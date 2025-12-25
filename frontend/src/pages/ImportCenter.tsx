import { useState } from 'react';
import BankImport from '../components/imports/BankImport';
import UniversalImport from '../components/imports/UniversalImport';
import UniversalExport from '../components/imports/UniversalExport';

type ImportTab = 'bank' | 'universal' | 'export';

interface ImportSource {
  name: string;
  icon: string;
  description: string;
  acceptedFormats: string[];
  features: string[];
}

const importSources: Record<ImportTab, ImportSource> = {
  bank: {
    name: 'Банкови извлечения',
    icon: '🏦',
    description: 'Импорт на банкови извлечения и операции',
    acceptedFormats: ['.xml', '.csv', '.txt', '.mt940'],
    features: [
      'MT940 формат (UniCredit)',
      'CAMT.053 XML (Wise, Revolut, Paysera)',
      'Postbank, OBB XML',
      'ЦКБ CSV',
      'Автоматично мапиране на сметки',
    ],
  },
  universal: {
    name: 'Универсален импорт',
    icon: '📊',
    description: 'Импорт от JSON формат',
    acceptedFormats: ['.json'],
    features: [
      'JSON формат v2.0',
      'Сметкоплан и ДДС ставки',
      'Контрагенти',
      'Валидация на данните',
    ],
  },
  export: {
    name: 'Универсален експорт',
    icon: '📤',
    description: 'Експорт на данни в JSON формат',
    acceptedFormats: ['.json'],
    features: [
      'Пълен или частичен експорт',
      'Съвместимост между инсталации',
      'JSON формат v2.0',
    ],
  },
};

export default function ImportCenter() {
  const [activeTab, setActiveTab] = useState<ImportTab>('bank');

  const renderImportComponent = () => {
    switch (activeTab) {
      case 'bank':
        return <BankImport />;
      case 'universal':
        return <UniversalImport />;
      case 'export':
        return <UniversalExport />;
      default:
        return <BankImport />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Център за импорти</h1>
        <p className="mt-1 text-sm text-gray-500">
          Импорт на счетоводни данни от различни източници
        </p>
      </div>

      {/* Source Selection Tabs */}
      <div className="bg-white shadow-sm rounded-lg border border-gray-200">
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex space-x-8 px-6">
            {(Object.entries(importSources) as [ImportTab, ImportSource][]).map(([key, source]) => (
              <button
                key={key}
                onClick={() => setActiveTab(key)}
                className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center space-x-2 ${
                  activeTab === key
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <span className="text-lg">{source.icon}</span>
                <span>{source.name}</span>
              </button>
            ))}
          </nav>
        </div>

        <div className="p-6">
          {renderImportComponent()}
        </div>
      </div>
    </div>
  );
}
