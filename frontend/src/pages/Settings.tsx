import { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { gql } from '@apollo/client';
import { GET_COMPANIES, GET_CURRENCIES, GET_ACCOUNTS, UPDATE_COMPANY, GET_SYSTEM_SETTINGS, UPDATE_SMTP_SETTINGS, TEST_SMTP_CONNECTION } from '../graphql/queries';
import { Link } from 'react-router-dom';
import { useCompany } from '../contexts/CompanyContext';

// Extended query to include default accounts and Salt Edge settings
const GET_COMPANY_WITH_DEFAULTS = gql`
  query GetCompanyWithDefaults($id: ID!) {
    company(id: $id) {
      id
      name
      eik
      defaultCashAccount { id code name }
      defaultCustomersAccount { id code name }
      defaultSuppliersAccount { id code name }
      defaultSalesRevenueAccount { id code name }
      defaultVatPurchaseAccount { id code name }
      defaultVatSalesAccount { id code name }
      defaultCardPaymentPurchaseAccount { id code name }
      defaultCardPaymentSalesAccount { id code name }
      saltEdgeAppId
      saltEdgeEnabled
    }
  }
`;

interface Account {
  id: string;
  code: string;
  name: string;
}

export default function Settings() {
  const [activeTab, setActiveTab] = useState('general');
  const { companyId } = useCompany();

  const { data: companiesData } = useQuery<any>(GET_COMPANIES);
  const { data: currenciesData } = useQuery<any>(GET_CURRENCIES);
  const { data: accountsData } = useQuery<any>(GET_ACCOUNTS, {
    variables: { companyId },
    skip: !companyId,
  });
  const { data: companyData, refetch: refetchCompany } = useQuery<any>(GET_COMPANY_WITH_DEFAULTS, {
    variables: { id: companyId },
    skip: !companyId,
  });

  const [updateCompany, { loading: savingDefaults }] = useMutation(UPDATE_COMPANY);

  // Default account state
  const [defaultAccounts, setDefaultAccounts] = useState({
    defaultCashAccountId: '',
    defaultCustomersAccountId: '',
    defaultSuppliersAccountId: '',
    defaultSalesRevenueAccountId: '',
    defaultVatPurchaseAccountId: '',
    defaultVatSalesAccountId: '',
    defaultCardPaymentPurchaseAccountId: '',
    defaultCardPaymentSalesAccountId: '',
  });

  // Load current default accounts and Salt Edge settings when company data is fetched
  useEffect(() => {
    if (companyData?.company) {
      const c = companyData.company;
      setDefaultAccounts({
        defaultCashAccountId: c.defaultCashAccount?.id || '',
        defaultCustomersAccountId: c.defaultCustomersAccount?.id || '',
        defaultSuppliersAccountId: c.defaultSuppliersAccount?.id || '',
        defaultSalesRevenueAccountId: c.defaultSalesRevenueAccount?.id || '',
        defaultVatPurchaseAccountId: c.defaultVatPurchaseAccount?.id || '',
        defaultVatSalesAccountId: c.defaultVatSalesAccount?.id || '',
        defaultCardPaymentPurchaseAccountId: c.defaultCardPaymentPurchaseAccount?.id || '',
        defaultCardPaymentSalesAccountId: c.defaultCardPaymentSalesAccount?.id || '',
      });
      // Load Salt Edge settings
      setSaltEdgeSettings({
        saltEdgeAppId: c.saltEdgeAppId || '',
        saltEdgeSecret: '', // Never load secret from server
        saltEdgeEnabled: c.saltEdgeEnabled ?? false,
      });
    }
  }, [companyData]);

  const companies = companiesData?.companies || [];
  const currencies = currenciesData?.currencies || [];
  const accounts: Account[] = accountsData?.accounts || [];
  const baseCurrency = currencies.find((c: { isBaseCurrency: boolean }) => c.isBaseCurrency);

  // Filter accounts by code prefix
  const filterAccountsByCode = (prefix: string) => {
    return accounts.filter((acc: Account) => acc.code.startsWith(prefix));
  };

  const handleSaveDefaultAccounts = async () => {
    if (!companyId) return;

    try {
      await updateCompany({
        variables: {
          id: companyId,
          input: {
            defaultCashAccountId: defaultAccounts.defaultCashAccountId ? parseInt(defaultAccounts.defaultCashAccountId) : 0,
            defaultCustomersAccountId: defaultAccounts.defaultCustomersAccountId ? parseInt(defaultAccounts.defaultCustomersAccountId) : 0,
            defaultSuppliersAccountId: defaultAccounts.defaultSuppliersAccountId ? parseInt(defaultAccounts.defaultSuppliersAccountId) : 0,
            defaultSalesRevenueAccountId: defaultAccounts.defaultSalesRevenueAccountId ? parseInt(defaultAccounts.defaultSalesRevenueAccountId) : 0,
            defaultVatPurchaseAccountId: defaultAccounts.defaultVatPurchaseAccountId ? parseInt(defaultAccounts.defaultVatPurchaseAccountId) : 0,
            defaultVatSalesAccountId: defaultAccounts.defaultVatSalesAccountId ? parseInt(defaultAccounts.defaultVatSalesAccountId) : 0,
            defaultCardPaymentPurchaseAccountId: defaultAccounts.defaultCardPaymentPurchaseAccountId ? parseInt(defaultAccounts.defaultCardPaymentPurchaseAccountId) : 0,
            defaultCardPaymentSalesAccountId: defaultAccounts.defaultCardPaymentSalesAccountId ? parseInt(defaultAccounts.defaultCardPaymentSalesAccountId) : 0,
          }
        }
      });
      refetchCompany();
      alert('Настройките са запазени успешно!');
    } catch (error) {
      console.error('Error saving default accounts:', error);
      alert('Грешка при запазване на настройките');
    }
  };

  const handleSaveSaltEdgeSettings = async () => {
    if (!companyId) return;

    setSavingSaltEdge(true);
    try {
      const input: any = {
        saltEdgeEnabled: saltEdgeSettings.saltEdgeEnabled,
      };
      // Only send appId if it changed
      if (saltEdgeSettings.saltEdgeAppId) {
        input.saltEdgeAppId = saltEdgeSettings.saltEdgeAppId;
      }
      // Only send secret if user entered a new one
      if (saltEdgeSettings.saltEdgeSecret) {
        input.saltEdgeSecret = saltEdgeSettings.saltEdgeSecret;
      }

      await updateCompany({
        variables: {
          id: companyId,
          input
        }
      });
      refetchCompany();
      // Clear the secret field after save
      setSaltEdgeSettings(prev => ({ ...prev, saltEdgeSecret: '' }));
      alert('Salt Edge настройките са запазени успешно!');
    } catch (error: any) {
      console.error('Error saving Salt Edge settings:', error);
      alert('Грешка при запазване: ' + (error.message || 'Неизвестна грешка'));
    } finally {
      setSavingSaltEdge(false);
    }
  };

  // SMTP Settings
  const { data: systemSettingsData, refetch: refetchSystemSettings } = useQuery<any>(GET_SYSTEM_SETTINGS);
  const [updateSmtpSettings, { loading: savingSmtp }] = useMutation(UPDATE_SMTP_SETTINGS);
  const [testSmtp, { loading: testingSmtp }] = useMutation<{ testSmtpConnection: boolean }>(TEST_SMTP_CONNECTION);

  const [smtpSettings, setSmtpSettings] = useState({
    smtpHost: '',
    smtpPort: 587,
    smtpUsername: '',
    smtpPassword: '',
    smtpFromEmail: '',
    smtpFromName: '',
    smtpUseTls: true,
    smtpUseSsl: false,
    smtpEnabled: false,
  });
  const [testEmail, setTestEmail] = useState('');

  // Salt Edge Open Banking state
  const [saltEdgeSettings, setSaltEdgeSettings] = useState({
    saltEdgeAppId: '',
    saltEdgeSecret: '',
    saltEdgeEnabled: false,
  });
  const [savingSaltEdge, setSavingSaltEdge] = useState(false);

  // Load SMTP settings when data is fetched
  useEffect(() => {
    if (systemSettingsData?.systemSettings) {
      const s = systemSettingsData.systemSettings;
      setSmtpSettings({
        smtpHost: s.smtpHost || '',
        smtpPort: s.smtpPort || 587,
        smtpUsername: s.smtpUsername || '',
        smtpPassword: '', // Never load password from server
        smtpFromEmail: s.smtpFromEmail || '',
        smtpFromName: s.smtpFromName || '',
        smtpUseTls: s.smtpUseTls ?? true,
        smtpUseSsl: s.smtpUseSsl ?? false,
        smtpEnabled: s.smtpEnabled ?? false,
      });
    }
  }, [systemSettingsData]);

  const handleSaveSmtpSettings = async () => {
    try {
      await updateSmtpSettings({
        variables: {
          input: {
            smtpHost: smtpSettings.smtpHost || null,
            smtpPort: smtpSettings.smtpPort || null,
            smtpUsername: smtpSettings.smtpUsername || null,
            smtpPassword: smtpSettings.smtpPassword || null, // Only send if changed
            smtpFromEmail: smtpSettings.smtpFromEmail || null,
            smtpFromName: smtpSettings.smtpFromName || null,
            smtpUseTls: smtpSettings.smtpUseTls,
            smtpUseSsl: smtpSettings.smtpUseSsl,
            smtpEnabled: smtpSettings.smtpEnabled,
          }
        }
      });
      refetchSystemSettings();
      alert('SMTP настройките са запазени успешно!');
    } catch (error: any) {
      console.error('Error saving SMTP settings:', error);
      alert('Грешка при запазване: ' + (error.message || 'Неизвестна грешка'));
    }
  };

  const handleTestSmtp = async () => {
    if (!testEmail) {
      alert('Моля, въведете имейл адрес за тест');
      return;
    }
    try {
      const result = await testSmtp({ variables: { testEmail } });
      if (result.data?.testSmtpConnection) {
        alert('Тестовият имейл беше изпратен успешно на ' + testEmail);
      }
    } catch (error: any) {
      console.error('Error testing SMTP:', error);
      alert('Грешка при изпращане: ' + (error.message || 'Неизвестна грешка'));
    }
  };

  const tabs = [
    { id: 'general', label: 'Общи', icon: '⚙️' },
    { id: 'company', label: 'Компания', icon: '🏢' },
    { id: 'accounting', label: 'Счетоводство', icon: '📚' },
    { id: 'periods', label: 'Счетоводни периоди', icon: '📅' },
    { id: 'automation', label: 'Автоматизации', icon: '🤖' },
    { id: 'smtp', label: 'SMTP / Email', icon: '📧' },
    { id: 'vat', label: 'ДДС', icon: '💰' },
    { id: 'integrations', label: 'Интеграции', icon: '🔗' },
    { id: 'permissions', label: 'Потребители и права', icon: '👥' },
    { id: 'audit', label: 'Одит логове', icon: '📋' },
  ];

  const AccountSelect = ({
    label,
    value,
    onChange,
    filterPrefix,
    hint
  }: {
    label: string;
    value: string;
    onChange: (value: string) => void;
    filterPrefix?: string;
    hint?: string;
  }) => {
    const filteredAccounts = filterPrefix ? filterAccountsByCode(filterPrefix) : accounts;
    return (
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
        >
          <option value="">-- Изберете сметка --</option>
          {filteredAccounts.map((acc: Account) => (
            <option key={acc.id} value={acc.id}>
              {acc.code} - {acc.name}
            </option>
          ))}
        </select>
        {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Настройки</h1>
        <p className="mt-1 text-sm text-gray-500">
          Конфигурация на системата и предпочитания
        </p>
      </div>

      <div className="flex gap-6">
        {/* Sidebar Tabs */}
        <div className="w-48 flex-shrink-0">
          <nav className="space-y-1">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`w-full flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                  activeTab === tab.id
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`}
              >
                <span className="mr-3">{tab.icon}</span>
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        <div className="flex-1">
          {activeTab === 'general' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">Общи настройки</h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Език на интерфейса
                  </label>
                  <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                    <option value="bg">Български</option>
                    <option value="en">English</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Формат на дата
                  </label>
                  <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                    <option value="dd.MM.yyyy">DD.MM.YYYY</option>
                    <option value="yyyy-MM-dd">YYYY-MM-DD</option>
                    <option value="MM/dd/yyyy">MM/DD/YYYY</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Формат на числа
                  </label>
                  <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                    <option value="bg-BG">1 234,56 (БГ)</option>
                    <option value="en-US">1,234.56 (US)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Часова зона
                  </label>
                  <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                    <option value="Europe/Sofia">Europe/Sofia (EET)</option>
                    <option value="UTC">UTC</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'company' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">Настройки на компанията</h2>

              {companies.length > 0 ? (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Активна компания
                    </label>
                    <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                      {companies.map((company: { id: string; name: string; eik: string }) => (
                        <option key={company.id} value={company.id}>
                          {company.name} (ЕИК: {company.eik})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="bg-gray-50 rounded-lg p-4">
                    <h3 className="text-sm font-medium text-gray-900 mb-2">Данни за компанията</h3>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-gray-500">Име:</span>
                        <span className="ml-2 text-gray-900">{companies[0]?.name}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">ЕИК:</span>
                        <span className="ml-2 text-gray-900">{companies[0]?.eik}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  Няма създадени компании
                </div>
              )}
            </div>
          )}

          {activeTab === 'accounting' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">Счетоводни настройки</h2>

              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Базова валута
                    </label>
                    <div className="flex items-center p-3 bg-green-50 border border-green-200 rounded-md">
                      <span className="text-2xl font-bold text-green-700 mr-3">€</span>
                      <div>
                        <p className="text-sm font-medium text-green-900">{baseCurrency?.code || 'EUR'}</p>
                        <p className="text-xs text-green-700">Фиксирана базова валута</p>
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Източници на курсове
                    </label>
                    <div className="flex items-center p-3 bg-blue-50 border border-blue-200 rounded-md">
                      <span className="text-2xl mr-3">🏛️</span>
                      <div>
                        <p className="text-sm font-medium text-blue-900">ЕЦБ</p>
                        <p className="text-xs text-blue-700">Европейска централна банка</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-yellow-900 mb-2">Фиксиран курс BGN/EUR</h3>
                  <p className="text-sm text-yellow-700">
                    По силата на валутен борд курсът е фиксиран: <strong>1 EUR = 1.95583 BGN</strong>
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Счетоводен период
                  </label>
                  <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                    <option value="monthly">Месечен</option>
                    <option value="quarterly">Тримесечен</option>
                    <option value="yearly">Годишен</option>
                  </select>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="autoPost"
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    defaultChecked
                  />
                  <label htmlFor="autoPost" className="ml-2 block text-sm text-gray-700">
                    Автоматично осчетоводяване на записи
                  </label>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'periods' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <h2 className="text-lg font-semibold text-gray-900">Счетоводни периоди</h2>
              <p className="mt-1 text-sm text-gray-600">
                Заключвайте счетоводни периоди след подаване на ДДС декларация, за да предотвратите нежелани промени в счетоводните записи.
              </p>
              <div className="mt-4">
                <Link
                  to="/settings/periods"
                  className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
                >
                  Управление на периоди
                </Link>
              </div>
            </div>
          )}

          {activeTab === 'automation' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">Автоматизации</h2>
                <p className="mt-1 text-sm text-gray-500">
                  Настройте default сметки за автоматични плащания и AI обработка на фактури
                </p>
              </div>

              {!companyId ? (
                <div className="text-center py-8 text-gray-500">
                  Моля, изберете компания от менюто горе.
                </div>
              ) : (
                <div className="space-y-6">
                  {/* Разплащания */}
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                      Сметки за разплащания
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <AccountSelect
                        label="Каса (плащания в брой)"
                        value={defaultAccounts.defaultCashAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultCashAccountId: v }))}
                        filterPrefix="50"
                        hint="Обикновено 501"
                      />
                      <AccountSelect
                        label="Плащания с карта (покупки)"
                        value={defaultAccounts.defaultCardPaymentPurchaseAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultCardPaymentPurchaseAccountId: v }))}
                        filterPrefix="50"
                        hint="POS терминал за плащане"
                      />
                      <AccountSelect
                        label="Плащания с карта (продажби)"
                        value={defaultAccounts.defaultCardPaymentSalesAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultCardPaymentSalesAccountId: v }))}
                        filterPrefix="50"
                        hint="POS терминал за приемане"
                      />
                    </div>
                  </div>

                  {/* Контрагенти */}
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                      Сметки на контрагенти
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <AccountSelect
                        label="Клиенти"
                        value={defaultAccounts.defaultCustomersAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultCustomersAccountId: v }))}
                        filterPrefix="41"
                        hint="Обикновено 411"
                      />
                      <AccountSelect
                        label="Доставчици"
                        value={defaultAccounts.defaultSuppliersAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultSuppliersAccountId: v }))}
                        filterPrefix="40"
                        hint="Обикновено 401"
                      />
                    </div>
                  </div>

                  {/* Приходи и ДДС */}
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                      Приходи и ДДС
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <AccountSelect
                        label="Приходи от продажби (default)"
                        value={defaultAccounts.defaultSalesRevenueAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultSalesRevenueAccountId: v }))}
                        filterPrefix="70"
                        hint="Обикновено 702 или 703"
                      />
                      <AccountSelect
                        label="ДДС на покупките"
                        value={defaultAccounts.defaultVatPurchaseAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultVatPurchaseAccountId: v }))}
                        filterPrefix="453"
                        hint="Обикновено 4531"
                      />
                      <AccountSelect
                        label="ДДС на продажбите"
                        value={defaultAccounts.defaultVatSalesAccountId}
                        onChange={(v) => setDefaultAccounts(prev => ({ ...prev, defaultVatSalesAccountId: v }))}
                        filterPrefix="453"
                        hint="Обикновено 4532"
                      />
                    </div>
                  </div>

                  {/* Info box */}
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <h4 className="text-sm font-medium text-blue-900 mb-2">Как работят автоматизациите?</h4>
                    <ul className="text-sm text-blue-700 space-y-1">
                      <li>• AI разпознаване на фактури автоматично ще попълва тези сметки</li>
                      <li>• При плащане по документ системата ще предлага съответната сметка</li>
                      <li>• Банковите импорти ще използват тези default стойности</li>
                    </ul>
                  </div>

                  <div className="flex justify-end pt-4 border-t">
                    <button
                      onClick={handleSaveDefaultAccounts}
                      disabled={savingDefaults}
                      className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50"
                    >
                      {savingDefaults ? 'Запазване...' : 'Запази настройките'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'smtp' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">SMTP / Email настройки</h2>
                <p className="mt-1 text-sm text-gray-500">
                  Глобални настройки за изпращане на имейли (за всички компании)
                </p>
              </div>

              <div className="space-y-6">
                {/* Enable/Disable */}
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div>
                    <p className="text-sm font-medium text-gray-900">Активиране на SMTP</p>
                    <p className="text-xs text-gray-500">Позволява изпращане на имейли от системата</p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={smtpSettings.smtpEnabled}
                      onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpEnabled: e.target.checked }))}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>

                {/* Server settings */}
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                    Сървър настройки
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">SMTP Хост</label>
                      <input
                        type="text"
                        value={smtpSettings.smtpHost}
                        onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpHost: e.target.value }))}
                        placeholder="smtp.example.com"
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      />
                      <p className="mt-1 text-xs text-gray-500">Direct Mail Alibaba: smtpdm.aliyun.com</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Порт</label>
                      <select
                        value={smtpSettings.smtpPort}
                        onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpPort: parseInt(e.target.value) }))}
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      >
                        <option value={25}>25 (SMTP)</option>
                        <option value={80}>80 (HTTP/Контейнери)</option>
                        <option value={465}>465 (SSL)</option>
                        <option value={587}>587 (TLS/STARTTLS)</option>
                        <option value={2525}>2525 (Алтернативен)</option>
                      </select>
                      <p className="mt-1 text-xs text-gray-500">Порт 80 работи добре в контейнери</p>
                    </div>
                  </div>
                </div>

                {/* Authentication */}
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                    Автентикация
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Потребител</label>
                      <input
                        type="text"
                        value={smtpSettings.smtpUsername}
                        onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpUsername: e.target.value }))}
                        placeholder="user@example.com"
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Парола</label>
                      <input
                        type="password"
                        value={smtpSettings.smtpPassword}
                        onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpPassword: e.target.value }))}
                        placeholder="••••••••"
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      />
                      <p className="mt-1 text-xs text-gray-500">Оставете празно за да запазите текущата</p>
                    </div>
                  </div>
                </div>

                {/* Sender info */}
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                    Данни за изпращача
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Имейл адрес</label>
                      <input
                        type="email"
                        value={smtpSettings.smtpFromEmail}
                        onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpFromEmail: e.target.value }))}
                        placeholder="noreply@example.com"
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Име на изпращача</label>
                      <input
                        type="text"
                        value={smtpSettings.smtpFromName}
                        onChange={(e) => setSmtpSettings(prev => ({ ...prev, smtpFromName: e.target.value }))}
                        placeholder="SP-AC Accounting"
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      />
                    </div>
                  </div>
                </div>

                {/* Security */}
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 mb-4 pb-2 border-b">
                    Сигурност
                  </h3>
                  <div className="flex gap-6">
                    <label className="flex items-center">
                      <input
                        type="radio"
                        name="smtpSecurity"
                        checked={smtpSettings.smtpUseTls && !smtpSettings.smtpUseSsl}
                        onChange={() => setSmtpSettings(prev => ({ ...prev, smtpUseTls: true, smtpUseSsl: false }))}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                      />
                      <span className="ml-2 text-sm text-gray-700">STARTTLS (порт 587)</span>
                    </label>
                    <label className="flex items-center">
                      <input
                        type="radio"
                        name="smtpSecurity"
                        checked={smtpSettings.smtpUseSsl}
                        onChange={() => setSmtpSettings(prev => ({ ...prev, smtpUseTls: false, smtpUseSsl: true }))}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                      />
                      <span className="ml-2 text-sm text-gray-700">SSL/TLS (порт 465)</span>
                    </label>
                    <label className="flex items-center">
                      <input
                        type="radio"
                        name="smtpSecurity"
                        checked={!smtpSettings.smtpUseTls && !smtpSettings.smtpUseSsl}
                        onChange={() => setSmtpSettings(prev => ({ ...prev, smtpUseTls: false, smtpUseSsl: false }))}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                      />
                      <span className="ml-2 text-sm text-gray-700">Без криптиране</span>
                    </label>
                  </div>
                </div>

                {/* Test connection */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h4 className="text-sm font-medium text-blue-900 mb-3">Тест на връзката</h4>
                  <div className="flex gap-3">
                    <input
                      type="email"
                      value={testEmail}
                      onChange={(e) => setTestEmail(e.target.value)}
                      placeholder="test@example.com"
                      className="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    />
                    <button
                      onClick={handleTestSmtp}
                      disabled={testingSmtp || !smtpSettings.smtpHost}
                      className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50"
                    >
                      {testingSmtp ? 'Изпращане...' : 'Изпрати тест'}
                    </button>
                  </div>
                </div>

                {/* Save button */}
                <div className="flex justify-end pt-4 border-t">
                  <button
                    onClick={handleSaveSmtpSettings}
                    disabled={savingSmtp}
                    className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50"
                  >
                    {savingSmtp ? 'Запазване...' : 'Запази SMTP настройките'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'vat' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">ДДС настройки</h2>

              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      ДДС период
                    </label>
                    <select className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm">
                      <option value="monthly">Месечен</option>
                      <option value="quarterly">Тримесечен</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Стандартна ставка
                    </label>
                    <div className="flex items-center p-3 bg-gray-50 border border-gray-200 rounded-md">
                      <span className="text-2xl font-bold text-gray-700 mr-3">20%</span>
                      <span className="text-sm text-gray-500">По ЗДДС</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="vatRegistered"
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    defaultChecked
                  />
                  <label htmlFor="vatRegistered" className="ml-2 block text-sm text-gray-700">
                    Регистрация по ЗДДС
                  </label>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="intraCommunity"
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="intraCommunity" className="ml-2 block text-sm text-gray-700">
                    Вътреобщностни доставки
                  </label>
                </div>

                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-gray-900 mb-2">Срокове</h3>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• Справка-декларация: до 14-то число</li>
                    <li>• Дневници за покупки и продажби: до 14-то число</li>
                    <li>• VIES декларация: до 14-то число</li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'integrations' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6 space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">Интеграции</h2>

              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                  <div className="flex items-center">
                    <span className="text-2xl mr-3">🏛️</span>
                    <div>
                      <p className="text-sm font-medium text-gray-900">Европейска централна банка (ЕЦБ)</p>
                      <p className="text-xs text-gray-500">Обменни курсове</p>
                    </div>
                  </div>
                  <div className="flex items-center">
                    <span className="w-2 h-2 bg-green-500 rounded-full mr-2"></span>
                    <span className="text-sm text-green-700">Активна</span>
                  </div>
                </div>

                <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                  <div className="flex items-center">
                    <span className="text-2xl mr-3">🏦</span>
                    <div>
                      <p className="text-sm font-medium text-gray-900">Банкови импорти</p>
                      <p className="text-xs text-gray-500">MT940 / ISO 20022</p>
                    </div>
                  </div>
                  <button className="text-sm text-blue-600 hover:text-blue-700">
                    Конфигурирай
                  </button>
                </div>

                <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                  <div className="flex items-center">
                    <span className="text-2xl mr-3">📄</span>
                    <div>
                      <p className="text-sm font-medium text-gray-900">НАП</p>
                      <p className="text-xs text-gray-500">Електронно подаване</p>
                    </div>
                  </div>
                  <span className="text-sm text-gray-500">Скоро</span>
                </div>
              </div>

              {/* Salt Edge Open Banking Section */}
              <div className="mt-8 pt-6 border-t border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Salt Edge Open Banking</h3>
                <p className="text-sm text-gray-500 mb-4">
                  Свържете банковите сметки на компанията чрез Salt Edge API за автоматичен импорт на транзакции.
                </p>

                {!companyId ? (
                  <div className="text-center py-4 text-gray-500">
                    Моля, изберете компания от менюто горе.
                  </div>
                ) : (
                  <div className="space-y-4">
                    {/* Enable/Disable Toggle */}
                    <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                      <div>
                        <p className="text-sm font-medium text-gray-900">Активиране на Salt Edge</p>
                        <p className="text-xs text-gray-500">Позволява свързване на банкови сметки</p>
                      </div>
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          checked={saltEdgeSettings.saltEdgeEnabled}
                          onChange={(e) => setSaltEdgeSettings(prev => ({ ...prev, saltEdgeEnabled: e.target.checked }))}
                          className="sr-only peer"
                        />
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                      </label>
                    </div>

                    {/* API Credentials */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">App ID</label>
                        <input
                          type="text"
                          value={saltEdgeSettings.saltEdgeAppId}
                          onChange={(e) => setSaltEdgeSettings(prev => ({ ...prev, saltEdgeAppId: e.target.value }))}
                          placeholder="Вашият Salt Edge App ID"
                          className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Secret</label>
                        <input
                          type="password"
                          value={saltEdgeSettings.saltEdgeSecret}
                          onChange={(e) => setSaltEdgeSettings(prev => ({ ...prev, saltEdgeSecret: e.target.value }))}
                          placeholder="••••••••"
                          className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                        />
                        <p className="mt-1 text-xs text-gray-500">Оставете празно за да запазите текущия</p>
                      </div>
                    </div>

                    {/* Info box */}
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                      <h4 className="text-sm font-medium text-blue-900 mb-2">Как да получите API ключове?</h4>
                      <ol className="text-sm text-blue-700 space-y-1 list-decimal list-inside">
                        <li>Регистрирайте се на <a href="https://www.saltedge.com/" target="_blank" rel="noopener noreferrer" className="underline">saltedge.com</a></li>
                        <li>Създайте ново приложение в Client Dashboard</li>
                        <li>Копирайте App ID и Secret от настройките на приложението</li>
                      </ol>
                    </div>

                    {/* Save button */}
                    <div className="flex justify-end pt-4">
                      <button
                        onClick={handleSaveSaltEdgeSettings}
                        disabled={savingSaltEdge}
                        className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50"
                      >
                        {savingSaltEdge ? 'Запазване...' : 'Запази Salt Edge настройките'}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === 'permissions' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900">Потребители и права</h2>
                <p className="mt-1 text-sm text-gray-600">
                Управление на потребителски роли и техните достъпи до различните модули на системата.
                </p>
                <div className="mt-4">
                <Link
                    to="/settings/permissions"
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
                >
                    Управление на права
                </Link>
                </div>
            </div>
          )}

          {activeTab === 'audit' && (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900">Одит логове</h2>
                <p className="mt-1 text-sm text-gray-600">
                Преглед на потребителската активност - логини, генериране на справки, експорти.
                Логовете се пазят 6 месеца и са достъпни само за супер администратори.
                </p>
                <div className="mt-4">
                <Link
                    to="/settings/audit-logs"
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
                >
                    Преглед на логове
                </Link>
                </div>
            </div>
          )}

          {/* Save Button - only for tabs that need it */}
          {['general', 'accounting', 'vat'].includes(activeTab) && (
            <div className="mt-6 flex justify-end">
              <button className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700">
                Запази настройките
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
