import { useState } from 'react';
import { useQuery, useMutation, useLazyQuery } from '@apollo/client/react';
import { GET_COMPANIES, GET_CURRENCIES, CREATE_COMPANY, UPDATE_COMPANY, DELETE_COMPANY, GET_COMPANY } from '../graphql/queries';

interface Currency {
  id: string;
  code: string;
  name: string;
  symbol: string;
}

interface Company {
  id: string;
  name: string;
  eik: string;
  vatNumber?: string;
  address?: string;
  city?: string;
  country?: string;
  phone?: string;
  email?: string;
  isActive: boolean;
  napOffice?: string;
  preferredRateProvider: string;
  baseCurrency?: Currency;
  createdAt: string;
}

export default function Companies() {
  const [showModal, setShowModal] = useState(false);
  const [editingCompany, setEditingCompany] = useState<Company | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    eik: '',
    vatNumber: '',
    address: '',
    city: '',
    country: 'Bulgaria',
    phone: '',
    email: '',
    managerName: '',
    managerEgn: '',
    authorizedPerson: '',
    authorizedPersonEgn: '',
    napOffice: '',
    azureFormRecognizerEndpoint: '',
    azureFormRecognizerKey: '',
    baseCurrencyId: '',
  });
  const [error, setError] = useState('');

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data, loading, refetch } = useQuery<any>(GET_COMPANIES);
  const { data: currenciesData } = useQuery<{ currencies: Currency[] }>(GET_CURRENCIES);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [createCompany, { loading: creating }] = useMutation<any>(CREATE_COMPANY);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [updateCompany, { loading: updating }] = useMutation<any>(UPDATE_COMPANY);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [deleteCompany] = useMutation<any>(DELETE_COMPANY);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [fetchCompany] = useLazyQuery<any>(GET_COMPANY);

  const companies: Company[] = data?.companies || [];
  const currencies: Currency[] = currenciesData?.currencies || [];

  const resetForm = () => {
    setFormData({
      name: '',
      eik: '',
      vatNumber: '',
      address: '',
      city: '',
      country: 'Bulgaria',
      phone: '',
      email: '',
      managerName: '',
      managerEgn: '',
      authorizedPerson: '',
      authorizedPersonEgn: '',
      napOffice: '',
      azureFormRecognizerEndpoint: '',
      azureFormRecognizerKey: '',
      baseCurrencyId: '',
    });
    setEditingCompany(null);
    setError('');
  };

  const handleEdit = async (company: Company) => {
    setEditingCompany(company);
    // Fetch full company details including baseCurrency
    const { data: companyData } = await fetchCompany({ variables: { id: company.id } });
    const fullCompany = companyData?.company;
    setFormData({
      name: company.name,
      eik: company.eik,
      vatNumber: company.vatNumber || '',
      address: company.address || '',
      city: company.city || '',
      country: company.country || 'Bulgaria',
      phone: company.phone || '',
      email: company.email || '',
      managerName: fullCompany?.managerName || '',
      managerEgn: fullCompany?.managerEgn || '',
      authorizedPerson: fullCompany?.authorizedPerson || '',
      authorizedPersonEgn: fullCompany?.authorizedPersonEgn || '',
      napOffice: fullCompany?.napOffice || '',
      azureFormRecognizerEndpoint: fullCompany?.azureFormRecognizerEndpoint || '',
      azureFormRecognizerKey: fullCompany?.azureFormRecognizerKey || '',
      baseCurrencyId: fullCompany?.baseCurrency?.id || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name || !formData.eik) {
      setError('Името и ЕИК са задължителни');
      return;
    }

    try {
      if (editingCompany) {
        await updateCompany({
          variables: {
            id: editingCompany.id,
            input: {
              name: formData.name,
              eik: formData.eik,
              vatNumber: formData.vatNumber || null,
              address: formData.address || null,
              city: formData.city || null,
              country: formData.country || null,
              phone: formData.phone || null,
              email: formData.email || null,
              managerName: formData.managerName || null,
              managerEgn: formData.managerEgn || null,
              authorizedPerson: formData.authorizedPerson || null,
              authorizedPersonEgn: formData.authorizedPersonEgn || null,
              napOffice: formData.napOffice || null,
              azureFormRecognizerEndpoint: formData.azureFormRecognizerEndpoint || null,
              azureFormRecognizerKey: formData.azureFormRecognizerKey || null,
              baseCurrencyId: formData.baseCurrencyId || null,
            },
          },
        });
      } else {
        await createCompany({
          variables: {
            input: {
              name: formData.name,
              eik: formData.eik,
              vatNumber: formData.vatNumber || null,
              address: formData.address || null,
              city: formData.city || null,
              country: formData.country || null,
              phone: formData.phone || null,
              email: formData.email || null,
              managerName: formData.managerName || null,
              managerEgn: formData.managerEgn || null,
              authorizedPerson: formData.authorizedPerson || null,
              authorizedPersonEgn: formData.authorizedPersonEgn || null,
              napOffice: formData.napOffice || null,
              azureFormRecognizerEndpoint: formData.azureFormRecognizerEndpoint || null,
              azureFormRecognizerKey: formData.azureFormRecognizerKey || null,
              baseCurrencyId: formData.baseCurrencyId || null,
            },
          },
        });
      }
      setShowModal(false);
      resetForm();
      refetch();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при запис на компания');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да изтриете тази компания?')) return;

    try {
      await deleteCompany({ variables: { id } });
      refetch();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Компании</h1>
          <p className="mt-1 text-sm text-gray-500">
            Управление на фирми и техните настройки
          </p>
        </div>
        <button
          onClick={() => { resetForm(); setShowModal(true); }}
          className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
        >
          + Нова компания
        </button>
      </div>

      {/* Companies Table */}
      <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Компания
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                ЕИК / ДДС
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Местоположение
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Валута
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Статус
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Действия
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {companies.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                  Няма създадени компании
                </td>
              </tr>
            ) : (
              companies.map((company) => (
                <tr key={company.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="w-10 h-10 flex-shrink-0 bg-blue-100 text-blue-700 rounded-full flex items-center justify-center font-semibold">
                        {company.name.charAt(0)}
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">{company.name}</div>
                        {company.email && (
                          <div className="text-sm text-gray-500">{company.email}</div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">ЕИК: {company.eik}</div>
                    {company.vatNumber && (
                      <div className="text-sm text-gray-500">ДДС: {company.vatNumber}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{company.city || '-'}</div>
                    <div className="text-sm text-gray-500">{company.country || '-'}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {company.baseCurrency ? (
                      <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-700 font-medium">
                        {company.baseCurrency.code}
                      </span>
                    ) : (
                      <span className="px-2 py-1 text-xs rounded-full bg-yellow-100 text-yellow-700">
                        Не е избрана
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs rounded-full ${
                      company.isActive
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-700'
                    }`}>
                      {company.isActive ? 'Активна' : 'Неактивна'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => handleEdit(company)}
                      className="text-blue-600 hover:text-blue-900 mr-4"
                    >
                      Редактирай
                    </button>
                    <button
                      onClick={() => handleDelete(company.id)}
                      className="text-red-600 hover:text-red-900"
                    >
                      Изтрий
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">
                  {editingCompany ? 'Редактиране на компания' : 'Нова компания'}
                </h2>
                <button
                  onClick={() => { setShowModal(false); resetForm(); }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Име на компанията *
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="Моята Фирма ЕООД"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ЕИК *
                  </label>
                  <input
                    type="text"
                    value={formData.eik}
                    onChange={(e) => setFormData({ ...formData, eik: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="123456789"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ДДС номер
                  </label>
                  <input
                    type="text"
                    value={formData.vatNumber}
                    onChange={(e) => setFormData({ ...formData, vatNumber: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="BG123456789"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Град
                  </label>
                  <input
                    type="text"
                    value={formData.city}
                    onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="София"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Адрес
                  </label>
                  <input
                    type="text"
                    value={formData.address}
                    onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="бул. Витоша 100"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Телефон
                  </label>
                  <input
                    type="text"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="+359 2 123 4567"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Email
                  </label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="office@firma.bg"
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <h3 className="text-lg font-medium text-gray-900">Представляващи лица (за ДДС декларация)</h3>
                <p className="text-sm text-gray-500 mb-4">Данни за управителя и пълномощника, които се използват при подаване на ДДС декларации</p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Управител (име)
                    </label>
                    <input
                      type="text"
                      value={formData.managerName}
                      onChange={(e) => setFormData({ ...formData, managerName: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="Иван Петров Иванов"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Управител (ЕГН)
                    </label>
                    <input
                      type="text"
                      value={formData.managerEgn}
                      onChange={(e) => setFormData({ ...formData, managerEgn: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="8001015678"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Пълномощник (име)
                    </label>
                    <input
                      type="text"
                      value={formData.authorizedPerson}
                      onChange={(e) => setFormData({ ...formData, authorizedPerson: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="Мария Георгиева Петрова"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Пълномощник (ЕГН)
                    </label>
                    <input
                      type="text"
                      value={formData.authorizedPersonEgn}
                      onChange={(e) => setFormData({ ...formData, authorizedPersonEgn: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="8512125678"
                    />
                  </div>
                   <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      ТД на НАП
                    </label>
                    <input
                      type="text"
                      value={formData.napOffice}
                      onChange={(e) => setFormData({ ...formData, napOffice: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="22"
                    />
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <h3 className="text-lg font-medium text-gray-900">Счетоводни настройки</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-2">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Базова валута *
                    </label>
                    <select
                      value={formData.baseCurrencyId}
                      onChange={(e) => setFormData({ ...formData, baseCurrencyId: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    >
                      <option value="">Изберете валута...</option>
                      {currencies.map(currency => (
                        <option key={currency.id} value={currency.id}>
                          {currency.code} - {currency.name}
                        </option>
                      ))}
                    </select>
                    <p className="mt-1 text-xs text-gray-500">
                      Валутата, в която се води счетоводството. Всички операции в други валути се преизчисляват.
                    </p>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <h3 className="text-lg font-medium text-gray-900">AI Settings</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-2">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Azure Form Recognizer Endpoint
                        </label>
                        <input
                            type="text"
                            value={formData.azureFormRecognizerEndpoint}
                            onChange={(e) => setFormData({ ...formData, azureFormRecognizerEndpoint: e.target.value })}
                            className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                            placeholder="https://YOUR_ENDPOINT.cognitiveservices.azure.com/"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Azure Form Recognizer Key
                        </label>
                        <input
                            type="password"
                            value={formData.azureFormRecognizerKey}
                            onChange={(e) => setFormData({ ...formData, azureFormRecognizerKey: e.target.value })}
                            className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                            placeholder="••••••••••••••••••••••••••••••••"
                        />
                    </div>
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
                <button
                  type="button"
                  onClick={() => { setShowModal(false); resetForm(); }}
                  className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                >
                  Отказ
                </button>
                <button
                  type="submit"
                  disabled={creating || updating}
                  className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
                >
                  {creating || updating ? 'Запис...' : (editingCompany ? 'Запази промените' : 'Създай компания')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
