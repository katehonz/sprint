import { useState } from 'react';
import { useMutation } from '@apollo/client/react';
import { gql } from '@apollo/client';

const EXPORT_UNIVERSAL_JSON = gql`
  mutation ExportUniversalJson($input: UniversalExportInput!) {
    exportUniversalJson(input: $input)
  }
`;

const EXPORT_ACCOUNTS = gql`
  mutation ExportChartOfAccounts($companyId: ID!) {
    exportChartOfAccounts(companyId: $companyId)
  }
`;

const EXPORT_VAT_RATES = gql`
  mutation ExportVatRates($companyId: ID!) {
    exportVatRates(companyId: $companyId)
  }
`;

const EXPORT_COUNTERPARTS = gql`
  mutation ExportCounterparts($companyId: ID!) {
    exportCounterparts(companyId: $companyId)
  }
`;

export default function UniversalExport() {
  const [exporting, setExporting] = useState(false);

  const [exportUniversalJson] = useMutation<any>(EXPORT_UNIVERSAL_JSON);
  const [exportAccounts] = useMutation<any>(EXPORT_ACCOUNTS);
  const [exportVatRates] = useMutation<any>(EXPORT_VAT_RATES);
  const [exportCounterparts] = useMutation<any>(EXPORT_COUNTERPARTS);

  const downloadJsonFile = (jsonString: string, filename: string) => {
    const blob = new Blob([jsonString], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getTimestamp = () => {
    return new Date().toISOString().replace(/[:.]/g, '-').substring(0, 19);
  };

  const handleExportAll = async () => {
    setExporting(true);
    try {
      const { data } = await exportUniversalJson({
        variables: {
          input: {
            companyId: '1',
            includeDocuments: false,
            includeJournalEntries: false,
          },
        },
      });

      downloadJsonFile(data.exportUniversalJson, `universal-export-${getTimestamp()}.json`);
      alert('Експортът е завършен успешно!');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Грешка при експорт';
      alert(message);
    } finally {
      setExporting(false);
    }
  };

  const handleExportAccounts = async () => {
    setExporting(true);
    try {
      const { data } = await exportAccounts({
        variables: { companyId: '1' },
      });

      downloadJsonFile(data.exportChartOfAccounts, `chart-of-accounts-${getTimestamp()}.json`);
      alert('Сметкопланът е експортиран!');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Грешка';
      alert(message);
    } finally {
      setExporting(false);
    }
  };

  const handleExportVatRates = async () => {
    setExporting(true);
    try {
      const { data } = await exportVatRates({
        variables: { companyId: '1' },
      });

      downloadJsonFile(data.exportVatRates, `vat-rates-${getTimestamp()}.json`);
      alert('ДДС ставките са експортирани!');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Грешка';
      alert(message);
    } finally {
      setExporting(false);
    }
  };

  const handleExportCounterparts = async () => {
    setExporting(true);
    try {
      const { data } = await exportCounterparts({
        variables: { companyId: '1' },
      });

      downloadJsonFile(data.exportCounterparts, `counterparts-${getTimestamp()}.json`);
      alert('Контрагентите са експортирани!');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Грешка';
      alert(message);
    } finally {
      setExporting(false);
    }
  };

  const exportTypes = [
    {
      id: 'all',
      icon: '📦',
      title: 'Пълен експорт',
      description: 'Всички данни - сметкоплан, ДДС ставки, контрагенти',
      action: handleExportAll,
      color: 'blue',
    },
    {
      id: 'accounts',
      icon: '🗂️',
      title: 'Само сметкоплан',
      description: 'Експорт само на сметките',
      action: handleExportAccounts,
      color: 'green',
    },
    {
      id: 'vat',
      icon: '📊',
      title: 'Само ДДС ставки',
      description: 'Експорт само на ДДС ставки',
      action: handleExportVatRates,
      color: 'purple',
    },
    {
      id: 'counterparts',
      icon: '👥',
      title: 'Само контрагенти',
      description: 'Експорт само на контрагенти',
      action: handleExportCounterparts,
      color: 'orange',
    },
  ];

  const features = [
    'JSON формат v2.0',
    'Съвместимост между инсталации',
    'Частичен експорт',
    'Автоматично име с дата',
    'Валидна структура за импорт',
    'Само активни записи',
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start space-x-4">
        <div className="text-4xl">📤</div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">Универсален експорт - JSON v2.0</h3>
          <p className="text-gray-600 mb-3">
            Експорт на данни за прехвърляне в друга инсталация
          </p>
          <div className="grid grid-cols-2 gap-2">
            {features.map((feature, index) => (
              <div key={index} className="flex items-center space-x-2">
                <span className="w-2 h-2 bg-blue-400 rounded-full"></span>
                <span className="text-sm text-gray-700">{feature}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Export Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {exportTypes.map(type => (
          <div
            key={type.id}
            className="border-2 rounded-lg p-6 bg-white border-gray-200 hover:border-blue-400 hover:shadow-lg transition-all"
          >
            <div className="flex items-start space-x-4">
              <div className="text-4xl">{type.icon}</div>
              <div className="flex-1">
                <h4 className="text-lg font-semibold text-gray-900 mb-1">{type.title}</h4>
                <p className="text-sm text-gray-600 mb-4">{type.description}</p>
                <button
                  onClick={type.action}
                  disabled={exporting}
                  className="w-full px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {exporting ? 'Експортира се...' : 'Експортирай'}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Instructions */}
      <div className="bg-green-50 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <div className="text-green-500 text-xl">💡</div>
          <div className="text-sm text-green-800">
            <div className="font-medium mb-2">Как да използвате експорта:</div>
            <ol className="space-y-1 list-decimal list-inside">
              <li>Изберете тип експорт</li>
              <li>Изтеглете JSON файла</li>
              <li>Прехвърлете в друга инсталация</li>
              <li>Импортирайте чрез "Универсален импорт"</li>
            </ol>
          </div>
        </div>
      </div>

      {/* Format Info */}
      <div className="bg-yellow-50 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <div className="text-yellow-500 text-xl">ℹ️</div>
          <div className="text-sm text-yellow-800">
            <div className="font-medium mb-1">Структура на файла:</div>
            <ul className="mt-2 space-y-1">
              <li>
                • <code className="bg-yellow-100 px-1 rounded">companyInfo</code> - информация за фирмата
              </li>
              <li>
                • <code className="bg-yellow-100 px-1 rounded">chartOfAccounts</code> - сметки
              </li>
              <li>
                • <code className="bg-yellow-100 px-1 rounded">vatRates</code> - ДДС ставки
              </li>
              <li>
                • <code className="bg-yellow-100 px-1 rounded">counterparts</code> - контрагенти
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
