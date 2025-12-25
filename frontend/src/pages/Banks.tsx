import { useState, useMemo, useEffect } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { gql } from '@apollo/client';
import { GET_BANK_PROFILES, GET_ACCOUNTS, GET_CURRENCIES } from '../graphql/queries';

const CREATE_BANK_PROFILE = gql`
  mutation CreateBankProfile($input: CreateBankProfileInput!) {
    createBankProfile(input: $input) {
      id
      name
    }
  }
`;

const UPDATE_BANK_PROFILE = gql`
  mutation UpdateBankProfile($id: ID!, $input: UpdateBankProfileInput!) {
    updateBankProfile(id: $id, input: $input) {
      id
      name
    }
  }
`;

const DELETE_BANK_PROFILE = gql`
  mutation DeleteBankProfile($id: ID!) {
    deleteBankProfile(id: $id)
  }
`;

interface BankProfile {
  id: string;
  name: string;
  iban: string;
  accountId: string;
  bufferAccountId: string;
  currencyCode: string;
  connectionType: string;
  importFormat: string;
  saltEdgeProviderCode: string;
  saltEdgeProviderName: string;
  saltEdgeStatus: string;
  saltEdgeLastSyncAt: string;
  isActive: boolean;
}

interface Account {
  id: string;
  code: string;
  name: string;
  isAnalytical: boolean;
}

interface Currency {
  code: string;
  name: string;
  nameBg: string;
}

interface SaltEdgeProvider {
  id: string;
  code: string;
  name: string;
  countryCode: string;
  logoUrl: string;
}

interface FormState {
  name: string;
  iban: string;
  accountId: string;
  bufferAccountId: string;
  currencyCode: string;
  connectionType: 'FILE_IMPORT' | 'SALT_EDGE' | 'MANUAL';
  importFormat: string;
  saltEdgeProviderCode: string;
  isActive: boolean;
}

const importFormatOptions = [
  { value: 'UNICREDIT_MT940', label: 'UniCredit MT940 (SWIFT/TXT)' },
  { value: 'WISE_CAMT053', label: 'Wise CAMT.053 XML' },
  { value: 'REVOLUT_CAMT053', label: 'Revolut CAMT.053 XML' },
  { value: 'PAYSERA_CAMT053', label: 'Paysera CAMT.053 XML' },
  { value: 'POSTBANK_XML', label: 'Postbank XML' },
  { value: 'OBB_XML', label: 'OBB XML' },
  { value: 'CCB_CSV', label: 'ЦКБ CSV' },
];

const initialFormState: FormState = {
  name: '',
  iban: '',
  accountId: '',
  bufferAccountId: '',
  currencyCode: 'EUR',
  connectionType: 'FILE_IMPORT',
  importFormat: 'UNICREDIT_MT940',
  saltEdgeProviderCode: '',
  isActive: true,
};

export default function Banks() {
  const [formState, setFormState] = useState<FormState>(initialFormState);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [providers, setProviders] = useState<SaltEdgeProvider[]>([]);
  const [loadingProviders, setLoadingProviders] = useState(false);
  const [connectingBank, setConnectingBank] = useState(false);

  const { data: profilesData, loading, refetch } = useQuery<any>(GET_BANK_PROFILES, {
    variables: { companyId: '1' },
  });

  const { data: accountsData } = useQuery<any>(GET_ACCOUNTS, {
    variables: { companyId: '1' },
  });

  const { data: currenciesData } = useQuery<any>(GET_CURRENCIES);

  const [createBankProfile, { loading: creating }] = useMutation<any>(CREATE_BANK_PROFILE);
  const [updateBankProfile, { loading: updating }] = useMutation<any>(UPDATE_BANK_PROFILE);
  const [deleteBankProfile] = useMutation<any>(DELETE_BANK_PROFILE);

  const bankProfiles: BankProfile[] = profilesData?.bankProfiles || [];
  const accounts: Account[] = accountsData?.accounts || [];
  const currencies: Currency[] = currenciesData?.currencies || [];

  // Load Salt Edge providers when switching to Open Banking
  useEffect(() => {
    if (formState.connectionType === 'SALT_EDGE' && providers.length === 0) {
      loadProviders();
    }
  }, [formState.connectionType]);

  const loadProviders = async () => {
    setLoadingProviders(true);
    try {
      const response = await fetch('/api/saltedge/providers/bg');
      if (response.ok) {
        const data = await response.json();
        setProviders(data);
      }
    } catch (err) {
      console.error('Error loading providers:', err);
    } finally {
      setLoadingProviders(false);
    }
  };

  const analyticalAccounts = useMemo(
    () => accounts.filter(a => a.isAnalytical),
    [accounts]
  );

  const accountsMap = useMemo(() => {
    const map = new Map<string, Account>();
    accounts.forEach(acc => map.set(acc.id, acc));
    return map;
  }, [accounts]);

  const formatAccount = (accountId: string | null) => {
    if (!accountId) return '—';
    const account = accountsMap.get(accountId);
    return account ? `${account.code} · ${account.name}` : `ID ${accountId}`;
  };

  const sortedProfiles = useMemo(() => {
    return [...bankProfiles].sort((a, b) => {
      if (a.isActive === b.isActive) {
        return a.name.localeCompare(b.name);
      }
      return a.isActive ? -1 : 1;
    });
  }, [bankProfiles]);

  const resetForm = () => {
    setFormState(initialFormState);
    setEditingId(null);
    setError('');
  };

  const handleEdit = (profile: BankProfile) => {
    setEditingId(profile.id);
    setFormState({
      name: profile.name || '',
      iban: profile.iban || '',
      accountId: profile.accountId || '',
      bufferAccountId: profile.bufferAccountId || '',
      currencyCode: profile.currencyCode || 'EUR',
      connectionType: (profile.connectionType as FormState['connectionType']) || 'FILE_IMPORT',
      importFormat: profile.importFormat || 'UNICREDIT_MT940',
      saltEdgeProviderCode: profile.saltEdgeProviderCode || '',
      isActive: profile.isActive,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formState.name.trim()) {
      setError('Името на банката е задължително');
      return;
    }

    if (!formState.accountId) {
      setError('Изберете аналитична сметка');
      return;
    }

    if (!formState.bufferAccountId) {
      setError('Изберете буферна сметка');
      return;
    }

    if (formState.connectionType === 'FILE_IMPORT' && !formState.importFormat) {
      setError('Изберете формат за импорт');
      return;
    }

    try {
      const input: any = {
        companyId: '1',
        name: formState.name.trim(),
        iban: formState.iban.trim() || null,
        accountId: formState.accountId,
        bufferAccountId: formState.bufferAccountId,
        currencyCode: formState.currencyCode,
        connectionType: formState.connectionType,
        isActive: formState.isActive,
      };

      if (formState.connectionType === 'FILE_IMPORT') {
        input.importFormat = formState.importFormat;
      } else if (formState.connectionType === 'SALT_EDGE') {
        input.saltEdgeProviderCode = formState.saltEdgeProviderCode;
      }

      if (editingId) {
        await updateBankProfile({
          variables: { id: editingId, input },
        });
      } else {
        await createBankProfile({
          variables: { input },
        });
      }

      refetch();
      resetForm();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при запис');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да изтриете този профил?')) return;

    try {
      await deleteBankProfile({ variables: { id } });
      refetch();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
  };

  const handleConnectBank = async (profileId: string) => {
    setConnectingBank(true);
    try {
      const response = await fetch(`/api/saltedge/reconnect/${profileId}?returnUrl=${encodeURIComponent(window.location.href)}`, {
        method: 'POST',
      });

      if (response.ok) {
        const data = await response.json();
        if (data.connectUrl) {
          window.location.href = data.connectUrl;
        }
      } else {
        alert('Грешка при свързване с банката');
      }
    } catch (err) {
      alert('Грешка при свързване с банката');
    } finally {
      setConnectingBank(false);
    }
  };

  const handleSyncTransactions = async (profileId: string) => {
    try {
      const response = await fetch(`/api/saltedge/transactions/${profileId}/sync`, {
        method: 'POST',
      });

      if (response.ok) {
        const data = await response.json();
        alert(`Синхронизирани ${data.length} транзакции`);
        refetch();
      } else {
        alert('Грешка при синхронизиране');
      }
    } catch (err) {
      alert('Грешка при синхронизиране');
    }
  };

  const handleInitiateConnection = async () => {
    if (!formState.saltEdgeProviderCode) {
      setError('Изберете банка за Open Banking');
      return;
    }

    setConnectingBank(true);
    try {
      const response = await fetch(`/api/saltedge/connect?companyId=1&providerCode=${formState.saltEdgeProviderCode}&returnUrl=${encodeURIComponent(window.location.href)}`, {
        method: 'POST',
      });

      if (response.ok) {
        const data = await response.json();
        if (data.connectUrl) {
          window.location.href = data.connectUrl;
        }
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Грешка при свързване с банката');
      }
    } catch (err) {
      setError('Грешка при свързване с банката');
    } finally {
      setConnectingBank(false);
    }
  };

  const getConnectionTypeLabel = (type: string) => {
    switch (type) {
      case 'SALT_EDGE': return 'Open Banking';
      case 'FILE_IMPORT': return 'Файлов импорт';
      case 'MANUAL': return 'Ръчно';
      default: return type;
    }
  };

  const getStatusBadge = (profile: BankProfile) => {
    if (profile.connectionType === 'SALT_EDGE') {
      const status = profile.saltEdgeStatus;
      if (status === 'active') {
        return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">Свързана</span>;
      } else if (status === 'pending') {
        return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-700">Изчаква</span>;
      } else {
        return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700">Прекъсната</span>;
      }
    }
    return profile.isActive
      ? <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">Активна</span>
      : <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-200 text-gray-600">Неактивна</span>;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Банки</h1>
        <p className="mt-1 text-sm text-gray-500">
          Управление на банкови профили - файлов импорт или Open Banking (Salt Edge)
        </p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profiles List */}
        <div className="lg:col-span-2 space-y-4">
          <div className="bg-white shadow-sm rounded-lg border border-gray-200">
            <div className="px-6 py-4 border-b border-gray-100">
              <h2 className="text-lg font-semibold text-gray-900">Конфигурирани банки</h2>
              <p className="text-sm text-gray-500">
                Банкови профили с файлов импорт или Open Banking връзка
              </p>
            </div>

            <div className="divide-y divide-gray-100">
              {sortedProfiles.length === 0 ? (
                <div className="px-6 py-10 text-center text-gray-500 text-sm">
                  Няма добавени банкови профили. Създайте първия от формата вдясно.
                </div>
              ) : (
                sortedProfiles.map(profile => (
                  <div
                    key={profile.id}
                    className="px-6 py-5 flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-4 lg:space-y-0"
                  >
                    <div className="space-y-2">
                      <div className="flex items-center space-x-3">
                        <span className="text-xl">{profile.connectionType === 'SALT_EDGE' ? '🔗' : '🏦'}</span>
                        <div>
                          <div className="text-base font-semibold text-gray-900 flex items-center space-x-2">
                            <span>{profile.name}</span>
                            {getStatusBadge(profile)}
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                              profile.connectionType === 'SALT_EDGE'
                                ? 'bg-purple-100 text-purple-700'
                                : 'bg-blue-100 text-blue-700'
                            }`}>
                              {getConnectionTypeLabel(profile.connectionType)}
                            </span>
                          </div>
                          {profile.iban && (
                            <div className="text-sm text-gray-500">IBAN: {profile.iban}</div>
                          )}
                          {profile.saltEdgeProviderName && (
                            <div className="text-sm text-gray-500">Provider: {profile.saltEdgeProviderName}</div>
                          )}
                        </div>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm text-gray-600">
                        <div>
                          <div className="font-medium text-gray-700">Основна сметка</div>
                          <div>{formatAccount(profile.accountId)}</div>
                        </div>
                        <div>
                          <div className="font-medium text-gray-700">Буферна сметка</div>
                          <div>{formatAccount(profile.bufferAccountId)}</div>
                        </div>
                        <div>
                          <div className="font-medium text-gray-700">Валута</div>
                          <div>{profile.currencyCode}</div>
                        </div>
                        <div>
                          <div className="font-medium text-gray-700">
                            {profile.connectionType === 'FILE_IMPORT' ? 'Формат' : 'Последен sync'}
                          </div>
                          <div>
                            {profile.connectionType === 'FILE_IMPORT'
                              ? (importFormatOptions.find(o => o.value === profile.importFormat)?.label || profile.importFormat)
                              : (profile.saltEdgeLastSyncAt ? new Date(profile.saltEdgeLastSyncAt).toLocaleString('bg-BG') : 'Никога')
                            }
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2">
                      {profile.connectionType === 'SALT_EDGE' && (
                        <>
                          <button
                            onClick={() => handleSyncTransactions(profile.id)}
                            className="px-3 py-2 text-sm border border-green-300 text-green-700 rounded-md hover:bg-green-50"
                          >
                            Sync
                          </button>
                          <button
                            onClick={() => handleConnectBank(profile.id)}
                            disabled={connectingBank}
                            className="px-3 py-2 text-sm border border-purple-300 text-purple-700 rounded-md hover:bg-purple-50 disabled:opacity-50"
                          >
                            {profile.saltEdgeStatus === 'active' ? 'Reconnect' : 'Свържи'}
                          </button>
                        </>
                      )}
                      <button
                        onClick={() => handleEdit(profile)}
                        className="px-3 py-2 text-sm border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50"
                      >
                        Редакция
                      </button>
                      <button
                        onClick={() => handleDelete(profile.id)}
                        className="px-3 py-2 text-sm border border-red-300 text-red-700 rounded-md hover:bg-red-50"
                      >
                        Изтрий
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Form */}
        <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-1">
            {editingId ? 'Редакция на банков профил' : 'Нов банков профил'}
          </h2>
          <p className="text-sm text-gray-500 mb-4">
            Изберете начин на свързване и конфигурирайте сметките
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Connection Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Тип връзка
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setFormState(prev => ({ ...prev, connectionType: 'FILE_IMPORT' }))}
                  className={`px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors ${
                    formState.connectionType === 'FILE_IMPORT'
                      ? 'border-blue-500 bg-blue-50 text-blue-700'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="text-lg mb-1">📄</div>
                  Файлов импорт
                </button>
                <button
                  type="button"
                  onClick={() => setFormState(prev => ({ ...prev, connectionType: 'SALT_EDGE' }))}
                  className={`px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors ${
                    formState.connectionType === 'SALT_EDGE'
                      ? 'border-purple-500 bg-purple-50 text-purple-700'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="text-lg mb-1">🔗</div>
                  Open Banking
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Име на банката *
              </label>
              <input
                type="text"
                value={formState.name}
                onChange={e => setFormState(prev => ({ ...prev, name: e.target.value }))}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                placeholder="UniCredit Bulbank"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                IBAN
              </label>
              <input
                type="text"
                value={formState.iban}
                onChange={e => setFormState(prev => ({ ...prev, iban: e.target.value }))}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                placeholder="BGxx XXXX XXXX XXXX"
              />
            </div>

            {/* File Import Options */}
            {formState.connectionType === 'FILE_IMPORT' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Формат на файла *
                </label>
                <select
                  value={formState.importFormat}
                  onChange={e => setFormState(prev => ({ ...prev, importFormat: e.target.value }))}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                >
                  {importFormatOptions.map(opt => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Open Banking Options */}
            {formState.connectionType === 'SALT_EDGE' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Банка (Open Banking) *
                </label>
                {loadingProviders ? (
                  <div className="text-sm text-gray-500 py-2">Зареждане на банки...</div>
                ) : (
                  <select
                    value={formState.saltEdgeProviderCode}
                    onChange={e => setFormState(prev => ({ ...prev, saltEdgeProviderCode: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-purple-500 focus:ring-purple-500 sm:text-sm"
                  >
                    <option value="">Изберете банка...</option>
                    {providers.map(provider => (
                      <option key={provider.code} value={provider.code}>
                        {provider.name}
                      </option>
                    ))}
                  </select>
                )}
                <p className="mt-1 text-xs text-gray-500">
                  Salt Edge Open Banking - автоматично теглене на транзакции
                </p>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Аналитична сметка (банкова) *
              </label>
              <select
                value={formState.accountId}
                onChange={e => setFormState(prev => ({ ...prev, accountId: e.target.value }))}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              >
                <option value="">Изберете сметка...</option>
                {analyticalAccounts.map(acc => (
                  <option key={acc.id} value={acc.id}>
                    {acc.code} · {acc.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Буферна сметка (484) *
              </label>
              <select
                value={formState.bufferAccountId}
                onChange={e => setFormState(prev => ({ ...prev, bufferAccountId: e.target.value }))}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              >
                <option value="">Изберете сметка...</option>
                {analyticalAccounts.map(acc => (
                  <option key={acc.id} value={acc.id}>
                    {acc.code} · {acc.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Валута
              </label>
              <select
                value={formState.currencyCode}
                onChange={e => setFormState(prev => ({ ...prev, currencyCode: e.target.value }))}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              >
                {currencies.map(c => (
                  <option key={c.code} value={c.code}>
                    {c.code} - {c.nameBg || c.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                id="isActive"
                checked={formState.isActive}
                onChange={e => setFormState(prev => ({ ...prev, isActive: e.target.checked }))}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label htmlFor="isActive" className="ml-2 block text-sm text-gray-700">
                Профилът е активен
              </label>
            </div>

            <div className="flex flex-col space-y-3 pt-4 border-t border-gray-200">
              {/* Open Banking Connect Button */}
              {formState.connectionType === 'SALT_EDGE' && !editingId && formState.saltEdgeProviderCode && (
                <button
                  type="button"
                  onClick={handleInitiateConnection}
                  disabled={connectingBank}
                  className="w-full px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-purple-600 hover:bg-purple-700 disabled:opacity-50"
                >
                  {connectingBank ? 'Свързване...' : 'Свържи се с банката'}
                </button>
              )}

              <div className="flex justify-end space-x-3">
                {editingId && (
                  <button
                    type="button"
                    onClick={resetForm}
                    className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                  >
                    Отказ
                  </button>
                )}
                <button
                  type="submit"
                  disabled={creating || updating}
                  className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
                >
                  {creating || updating ? 'Запис...' : editingId ? 'Запази' : 'Създай профил'}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      {/* Info */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-blue-50 border border-blue-100 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <span className="text-xl">📄</span>
            <div className="text-sm text-blue-800">
              <div className="font-medium mb-1">Файлов импорт:</div>
              <ul className="space-y-1">
                <li>• <strong>MT940</strong> - UniCredit и други банки със SWIFT</li>
                <li>• <strong>CAMT.053</strong> - Wise, Revolut, Paysera</li>
                <li>• <strong>XML/CSV</strong> - Postbank, OBB, ЦКБ</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="bg-purple-50 border border-purple-100 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <span className="text-xl">🔗</span>
            <div className="text-sm text-purple-800">
              <div className="font-medium mb-1">Open Banking (Salt Edge):</div>
              <ul className="space-y-1">
                <li>• Автоматично теглене на транзакции</li>
                <li>• Поддържа повечето BG банки</li>
                <li>• Изисква Salt Edge акаунт</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
