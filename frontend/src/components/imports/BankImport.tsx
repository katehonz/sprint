import { useState, useEffect, useMemo, useCallback } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_BANK_PROFILES } from '../../graphql/queries';
import { gql } from '@apollo/client';

const IMPORT_BANK_STATEMENT = gql`
  mutation ImportBankStatement($input: ImportBankStatementInput!) {
    importBankStatement(input: $input) {
      bankImport {
        id
        fileName
        status
        transactionsCount
        totalDebit
        totalCredit
      }
      transactions
      totalDebit
      totalCredit
      journalEntryIds
    }
  }
`;

interface BankProfile {
  id: string;
  name: string;
  iban: string;
  currencyCode: string;
  importFormat: string;
  isActive: boolean;
}

interface UploadedFile {
  name: string;
  size: number;
  file: File;
  status: 'pending' | 'processing' | 'completed' | 'error';
  error: string | null;
  summary: {
    transactions: number;
    totalDebit: number;
    totalCredit: number;
    journalEntryIds: string[];
  } | null;
}

const importFormatConfig: Record<string, { label: string; extensions: string[]; icon: string; description: string }> = {
  UNICREDIT_MT940: {
    label: 'UniCredit MT940',
    extensions: ['.mt940', '.txt'],
    icon: '🏦',
    description: 'SWIFT MT940 извлечения от UniCredit',
  },
  WISE_CAMT053: {
    label: 'Wise CAMT.053 XML',
    extensions: ['.xml'],
    icon: '🌐',
    description: 'ISO 20022 CAMT.053 от Wise',
  },
  REVOLUT_CAMT053: {
    label: 'Revolut CAMT.053 XML',
    extensions: ['.xml'],
    icon: '💳',
    description: 'ISO 20022 CAMT.053 от Revolut Business',
  },
  PAYSERA_CAMT053: {
    label: 'Paysera CAMT.053 XML',
    extensions: ['.xml'],
    icon: '💼',
    description: 'ISO 20022 CAMT.053 от Paysera',
  },
  POSTBANK_XML: {
    label: 'Postbank XML',
    extensions: ['.xml'],
    icon: '🏛️',
    description: 'XML извлечения от Пощенска банка',
  },
  OBB_XML: {
    label: 'OBB XML',
    extensions: ['.xml'],
    icon: '🏦',
    description: 'XML извлечения от ОББ',
  },
  CCB_CSV: {
    label: 'ЦКБ CSV',
    extensions: ['.csv'],
    icon: '🏦',
    description: 'CSV извлечения от ЦКБ',
  },
};

const defaultFormatConfig = {
  label: 'Банков файл',
  extensions: ['.xml', '.mt940', '.txt', '.csv'],
  icon: '📄',
  description: 'Поддържани са MT940, CAMT.053 и банкови XML формати',
};

function formatBytes(bytes: number): string {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / Math.pow(1024, exponent);
  return `${value.toFixed(value >= 10 || exponent === 0 ? 0 : 1)} ${units[exponent]}`;
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (typeof result !== 'string') {
        reject(new Error('Неуспешно конвертиране на файла'));
        return;
      }
      const [, base64] = result.split(',');
      resolve(base64 || result);
    };
    reader.onerror = () => reject(new Error('Грешка при прочитане на файла'));
    reader.readAsDataURL(file);
  });
}

export default function BankImport() {
  const [selectedBankId, setSelectedBankId] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [alert, setAlert] = useState<{ type: 'success' | 'warning' | 'error'; message: string } | null>(null);
  const [isImporting, setIsImporting] = useState(false);

  const { data: profilesData, loading: loadingProfiles } = useQuery<any>(GET_BANK_PROFILES, {
    variables: { companyId: '1' },
  });

  const [importBankStatement] = useMutation<any>(IMPORT_BANK_STATEMENT);

  const bankProfiles: BankProfile[] = profilesData?.bankProfiles || [];

  useEffect(() => {
    if (bankProfiles.length > 0 && !selectedBankId) {
      setSelectedBankId(bankProfiles[0].id);
    }
  }, [bankProfiles, selectedBankId]);

  const selectedProfile = useMemo(
    () => bankProfiles.find(p => p.id === selectedBankId) || null,
    [bankProfiles, selectedBankId]
  );

  const formatConfig = selectedProfile
    ? importFormatConfig[selectedProfile.importFormat] || defaultFormatConfig
    : defaultFormatConfig;

  const allowedExtensions = formatConfig.extensions.map(ext => ext.toLowerCase());

  const handleFilesAdded = useCallback((fileList: FileList | null) => {
    if (!fileList || !selectedProfile) {
      setAlert({ type: 'warning', message: 'Моля изберете банка преди да качите файл.' });
      return;
    }

    const filesArray = Array.from(fileList);
    const accepted: File[] = [];
    const rejected: string[] = [];

    filesArray.forEach(file => {
      const lowerName = file.name.toLowerCase();
      const isValid = allowedExtensions.some(ext => lowerName.endsWith(ext));
      if (isValid) {
        accepted.push(file);
      } else {
        rejected.push(file.name);
      }
    });

    if (rejected.length > 0) {
      setAlert({
        type: 'warning',
        message: `Неподдържан формат: ${rejected.join(', ')}`,
      });
    }

    if (accepted.length > 0) {
      setUploadedFiles(prev => [
        ...prev,
        ...accepted.map(file => ({
          name: file.name,
          size: file.size,
          file,
          status: 'pending' as const,
          error: null,
          summary: null,
        })),
      ]);
    }
  }, [selectedProfile, allowedExtensions]);

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault();
    setDragOver(false);
    handleFilesAdded(event.dataTransfer.files);
  };

  const removeFile = (index: number) => {
    setUploadedFiles(prev => prev.filter((_, idx) => idx !== index));
  };

  const clearFiles = () => {
    setUploadedFiles([]);
    setAlert(null);
  };

  const startImport = async () => {
    if (!selectedProfile) {
      setAlert({ type: 'warning', message: 'Изберете банкова сметка.' });
      return;
    }

    if (uploadedFiles.length === 0) {
      setAlert({ type: 'warning', message: 'Добавете поне един файл.' });
      return;
    }

    setIsImporting(true);
    setAlert(null);

    for (let i = 0; i < uploadedFiles.length; i++) {
      const entry = uploadedFiles[i];
      setUploadedFiles(prev =>
        prev.map((file, idx) => (idx === i ? { ...file, status: 'processing' } : file))
      );

      try {
        const base64content = await fileToBase64(entry.file);
        const { data } = await importBankStatement({
          variables: {
            input: {
              bankProfileId: selectedProfile.id,
              companyId: '1',
              fileName: entry.name,
              fileBase64: base64content,
            },
          },
        });

        const result = data?.importBankStatement;
        if (!result) throw new Error('Сървърът не върна резултат');

        setUploadedFiles(prev =>
          prev.map((file, idx) =>
            idx === i
              ? { ...file, status: 'completed', summary: result }
              : file
          )
        );
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Грешка при импорт';
        setUploadedFiles(prev =>
          prev.map((file, idx) =>
            idx === i ? { ...file, status: 'error', error: message } : file
          )
        );
        setAlert({ type: 'error', message });
      }
    }

    setIsImporting(false);
    setAlert(prev => prev || { type: 'success', message: 'Импортът приключи.' });
  };

  if (loadingProfiles) {
    return (
      <div className="flex items-center justify-center h-32">
        <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  if (bankProfiles.length === 0) {
    return (
      <div className="space-y-4">
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-start">
            <span className="text-2xl mr-3">⚠️</span>
            <div>
              <h3 className="text-sm font-medium text-yellow-900">Няма банкови профили</h3>
              <p className="mt-1 text-sm text-yellow-700">
                Добавете банкова сметка от настройките, за да започнете импорт.
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Bank Selection */}
      <div className="flex items-start space-x-4">
        <div className="text-4xl">{formatConfig.icon}</div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">Импорт на банкови извлечения</h3>
          <p className="text-gray-600 mb-3">{formatConfig.description}</p>
          <div className="flex items-center space-x-3">
            <label className="text-sm font-medium text-gray-700">Банков профил:</label>
            <select
              value={selectedBankId || ''}
              onChange={e => setSelectedBankId(e.target.value)}
              className="border border-gray-300 rounded-md px-3 py-1.5 text-sm focus:border-blue-500 focus:ring-blue-500"
            >
              {bankProfiles.map(profile => (
                <option key={profile.id} value={profile.id}>
                  {profile.name} ({profile.currencyCode})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Alert */}
      {alert && (
        <div
          className={`border px-4 py-3 rounded ${
            alert.type === 'success'
              ? 'bg-green-50 border-green-200 text-green-700'
              : alert.type === 'warning'
              ? 'bg-yellow-50 border-yellow-200 text-yellow-700'
              : 'bg-red-50 border-red-200 text-red-700'
          }`}
        >
          {alert.message}
        </div>
      )}

      {/* Upload Area */}
      <div
        className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
          dragOver ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
        }`}
        onDragOver={e => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={e => { e.preventDefault(); setDragOver(false); }}
        onDrop={handleDrop}
      >
        <div className="space-y-4">
          <div className="text-4xl">{formatConfig.icon}</div>
          <div>
            <p className="text-lg font-medium text-gray-900">
              Пуснете файла тук или{' '}
              <label className="cursor-pointer text-blue-600 hover:text-blue-700 font-medium">
                изберете файл
                <input
                  type="file"
                  multiple
                  accept={allowedExtensions.join(',')}
                  onChange={e => {
                    handleFilesAdded(e.target.files);
                    e.target.value = '';
                  }}
                  className="hidden"
                />
              </label>
            </p>
          </div>
          <p className="text-sm text-gray-500">
            Поддържани: {allowedExtensions.join(', ')}
          </p>
        </div>
      </div>

      {/* Uploaded Files */}
      {uploadedFiles.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-medium text-gray-900">
              Качени файлове ({uploadedFiles.length})
            </h4>
            <div className="flex items-center space-x-3">
              <button
                onClick={clearFiles}
                className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                Изчисти
              </button>
              <button
                onClick={startImport}
                disabled={isImporting}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
              >
                {isImporting ? 'Импортиране...' : 'Стартирай импорта'}
              </button>
            </div>
          </div>

          <div className="space-y-3">
            {uploadedFiles.map((file, index) => (
              <div key={`${file.name}-${index}`} className="border border-gray-200 rounded-lg p-4 bg-white">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="text-2xl">{formatConfig.icon}</div>
                    <div>
                      <div className="text-sm font-medium text-gray-900">{file.name}</div>
                      <div className="text-xs text-gray-500">{formatBytes(file.size)}</div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-3">
                    <span
                      className={`px-2 py-1 text-xs rounded-full font-medium ${
                        file.status === 'pending'
                          ? 'bg-yellow-100 text-yellow-800'
                          : file.status === 'processing'
                          ? 'bg-blue-100 text-blue-800'
                          : file.status === 'completed'
                          ? 'bg-green-100 text-green-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {file.status === 'pending' && 'Чака'}
                      {file.status === 'processing' && 'Обработва се'}
                      {file.status === 'completed' && 'Готов'}
                      {file.status === 'error' && 'Грешка'}
                    </span>
                    <button
                      onClick={() => removeFile(index)}
                      className="text-gray-400 hover:text-red-600"
                      disabled={file.status === 'processing'}
                    >
                      ✕
                    </button>
                  </div>
                </div>
                {file.error && (
                  <div className="mt-2 text-sm text-red-600">{file.error}</div>
                )}
                {file.summary && (
                  <div className="mt-3 bg-green-50 border border-green-100 rounded-md p-3 text-sm text-green-800">
                    <div className="font-medium text-green-900 mb-1">Импортът е успешен</div>
                    <div>Транзакции: {file.summary.transactions}</div>
                    <div>Дебит: {file.summary.totalDebit}</div>
                    <div>Кредит: {file.summary.totalCredit}</div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Info */}
      <div className="bg-blue-50 border border-blue-100 rounded-lg p-4 text-sm text-blue-800">
        <div className="font-medium text-blue-900 mb-2">Как работи импортът</div>
        <ul className="space-y-1">
          <li>Журналните записи се създават в чернова</li>
          <li>Буферната сметка се използва за разпределение</li>
          <li>Референцията се запазва от банковия файл</li>
        </ul>
      </div>
    </div>
  );
}
