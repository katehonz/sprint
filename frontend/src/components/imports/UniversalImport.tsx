import { useState, useCallback } from 'react';
import { useMutation } from '@apollo/client/react';
import { gql } from '@apollo/client';

const IMPORT_UNIVERSAL_JSON = gql`
  mutation ImportUniversalJson($input: UniversalImportInput!) {
    importUniversalJson(input: $input) {
      success
      accountsImported
      vatRatesImported
      counterpartsImported
      errors
      warnings
    }
  }
`;

interface UploadedFile {
  name: string;
  size: number;
  file: File;
  status: 'pending' | 'processing' | 'completed' | 'error';
}

interface ImportResult {
  fileName: string;
  success: boolean;
  accountsImported: number;
  vatRatesImported: number;
  counterpartsImported: number;
  errors: string[];
  warnings: string[];
}

export default function UniversalImport() {
  const [dragOver, setDragOver] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [importing, setImporting] = useState(false);
  const [importResults, setImportResults] = useState<ImportResult[] | null>(null);

  const [importUniversalJson] = useMutation<any>(IMPORT_UNIVERSAL_JSON);

  const features = [
    'JSON формат v2.0',
    'Импорт на сметкоплан',
    'Импорт на ДДС ставки',
    'Импорт на контрагенти',
    'Валидация за дубликати',
    'Детайлен отчет',
  ];

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);

    const files = Array.from(e.dataTransfer.files);
    const jsonFiles = files.filter(file => file.name.toLowerCase().endsWith('.json'));

    if (jsonFiles.length > 0) {
      setUploadedFiles(prev => [
        ...prev,
        ...jsonFiles.map(file => ({
          name: file.name,
          size: file.size,
          status: 'pending' as const,
          file,
        })),
      ]);
    } else {
      alert('Моля качете само JSON файлове');
    }
  }, []);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    setUploadedFiles(prev => [
      ...prev,
      ...files.map(file => ({
        name: file.name,
        size: file.size,
        status: 'pending' as const,
        file,
      })),
    ]);
  }, []);

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const removeFile = (index: number) => {
    setUploadedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const startImport = async () => {
    if (uploadedFiles.length === 0) {
      alert('Моля качете файл за импорт');
      return;
    }

    setImporting(true);
    setImportResults(null);
    setUploadedFiles(prev => prev.map(file => ({ ...file, status: 'processing' })));

    const results: ImportResult[] = [];

    for (const fileInfo of uploadedFiles) {
      try {
        const fileContent = await fileInfo.file.text();

        // Validate JSON
        try {
          JSON.parse(fileContent);
        } catch {
          throw new Error('Невалиден JSON формат');
        }

        const { data } = await importUniversalJson({
          variables: {
            input: {
              companyId: '1',
              jsonData: fileContent,
            },
          },
        });

        const result = data?.importUniversalJson;
        results.push({
          fileName: fileInfo.name,
          success: result?.success || false,
          accountsImported: result?.accountsImported || 0,
          vatRatesImported: result?.vatRatesImported || 0,
          counterpartsImported: result?.counterpartsImported || 0,
          errors: result?.errors || [],
          warnings: result?.warnings || [],
        });

        setUploadedFiles(prev =>
          prev.map(f =>
            f.name === fileInfo.name
              ? { ...f, status: result?.success ? 'completed' : 'error' }
              : f
          )
        );
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : 'Грешка при импорт';
        results.push({
          fileName: fileInfo.name,
          success: false,
          accountsImported: 0,
          vatRatesImported: 0,
          counterpartsImported: 0,
          errors: [message],
          warnings: [],
        });

        setUploadedFiles(prev =>
          prev.map(f => (f.name === fileInfo.name ? { ...f, status: 'error' } : f))
        );
      }
    }

    setImportResults(results);
    setImporting(false);
  };

  const downloadExampleJson = () => {
    const example = {
      version: '2.0',
      exportDate: new Date().toISOString(),
      companyInfo: {
        name: 'Примерна фирма ООД',
        eik: '123456789',
        vatNumber: 'BG123456789',
      },
      chartOfAccounts: [
        { code: '101', name: 'Каса в EUR', accountType: 'ASSET', isAnalytic: false },
        { code: '411', name: 'Клиенти', accountType: 'ASSET', isAnalytic: true },
      ],
      vatRates: [
        { code: '02', name: '20% ДДС продажби', rate: 20 },
        { code: '11', name: '20% ДДС покупки', rate: 20 },
      ],
      counterparts: [
        { name: 'Примерен доставчик', eik: '987654321', vatNumber: 'BG987654321' },
      ],
    };

    const blob = new Blob([JSON.stringify(example, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'universal-import-example.json';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start space-x-4">
        <div className="text-4xl">📊</div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">Универсален импорт - JSON v2.0</h3>
          <p className="text-gray-600 mb-3">
            Импорт на сметкоплан, ДДС ставки и контрагенти
          </p>
          <div className="grid grid-cols-2 gap-2">
            {features.map((feature, index) => (
              <div key={index} className="flex items-center space-x-2">
                <span className="w-2 h-2 bg-green-400 rounded-full"></span>
                <span className="text-sm text-gray-700">{feature}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Download Template */}
      <div className="bg-blue-50 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <div className="text-blue-500 text-xl">📥</div>
          <div>
            <div className="font-medium text-blue-900 mb-2">Шаблон:</div>
            <button
              onClick={downloadExampleJson}
              className="px-4 py-2 text-sm font-medium text-blue-700 bg-white border border-blue-300 rounded-md hover:bg-blue-50"
            >
              Изтегли примерен JSON
            </button>
          </div>
        </div>
      </div>

      {/* Upload Area */}
      <div
        className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
          dragOver ? 'border-green-500 bg-green-50' : 'border-gray-300 hover:border-gray-400'
        }`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <div className="space-y-4">
          <div className="text-4xl">📊</div>
          <div>
            <p className="text-lg font-medium text-gray-900">
              Пуснете JSON файл тук или{' '}
              <label className="cursor-pointer text-green-600 hover:text-green-700 font-medium">
                изберете файл
                <input
                  type="file"
                  accept=".json"
                  onChange={handleFileSelect}
                  className="hidden"
                  multiple
                />
              </label>
            </p>
          </div>
          <p className="text-sm text-gray-500">Формат: .json (Universal Schema v2.0)</p>
        </div>
      </div>

      {/* Uploaded Files */}
      {uploadedFiles.length > 0 && (
        <div className="space-y-4">
          <h4 className="text-sm font-medium text-gray-900">
            Качени файлове ({uploadedFiles.length})
          </h4>
          <div className="space-y-2">
            {uploadedFiles.map((file, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border">
                <div className="flex items-center space-x-3">
                  <div className="text-xl">📋</div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{file.name}</p>
                    <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
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
                    {file.status === 'processing' && 'Импортира се...'}
                    {file.status === 'completed' && 'Готов'}
                    {file.status === 'error' && 'Грешка'}
                  </span>
                  <button
                    onClick={() => removeFile(index)}
                    disabled={importing}
                    className="text-gray-400 hover:text-red-600 disabled:opacity-50"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="flex justify-end space-x-3">
            <button
              onClick={() => {
                setUploadedFiles([]);
                setImportResults(null);
              }}
              disabled={importing}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
            >
              Изчисти
            </button>
            <button
              onClick={startImport}
              disabled={importing}
              className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 disabled:opacity-50"
            >
              {importing ? 'Импортира се...' : 'Започни импорт'}
            </button>
          </div>
        </div>
      )}

      {/* Import Results */}
      {importResults && (
        <div className="space-y-4">
          <h4 className="text-sm font-medium text-gray-900">Резултати:</h4>
          {importResults.map((result, index) => (
            <div
              key={index}
              className={`rounded-lg p-4 border-2 ${
                result.success ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
              }`}
            >
              <div className="flex items-start space-x-3">
                <div className="text-2xl">{result.success ? '✅' : '❌'}</div>
                <div className="flex-1">
                  <p className="font-medium text-gray-900 mb-2">{result.fileName}</p>

                  {result.success && (
                    <div className="space-y-1 text-sm">
                      <p className="text-green-700">Сметки: {result.accountsImported}</p>
                      <p className="text-green-700">ДДС ставки: {result.vatRatesImported}</p>
                      <p className="text-green-700">Контрагенти: {result.counterpartsImported}</p>
                    </div>
                  )}

                  {result.warnings.length > 0 && (
                    <div className="mt-2">
                      <p className="font-medium text-yellow-800 text-sm">Предупреждения:</p>
                      <ul className="text-xs text-yellow-700">
                        {result.warnings.map((w, i) => (
                          <li key={i}>⚠ {w}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {result.errors.length > 0 && (
                    <div className="mt-2">
                      <p className="font-medium text-red-800 text-sm">Грешки:</p>
                      <ul className="text-xs text-red-700">
                        {result.errors.map((e, i) => (
                          <li key={i}>✗ {e}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Info */}
      <div className="bg-green-50 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <div className="text-green-500 text-xl">💡</div>
          <div className="text-sm text-green-800">
            <div className="font-medium mb-2">Формат на файла:</div>
            <ul className="space-y-1">
              <li>• <strong>chartOfAccounts</strong> - сметки</li>
              <li>• <strong>vatRates</strong> - ДДС ставки</li>
              <li>• <strong>counterparts</strong> - контрагенти</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
