import { useState } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_COUNTERPARTS, CREATE_COUNTERPART, DELETE_COUNTERPART, VALIDATE_VAT } from '../graphql/queries';
import { useCompany } from '../contexts/CompanyContext';

interface Counterpart {
  id: string;
  name: string;
  eik?: string;
  vatNumber?: string;
  address?: string;
  longAddress?: string;
  city?: string;
  country?: string;
  email?: string;
  phone?: string;
  counterpartType: string;
  isActive: boolean;
  createdAt: string;
}

interface ViesResult {
  valid: boolean;
  vatNumber?: string;
  countryCode?: string;
  name?: string;
  longAddress?: string;
  errorMessage?: string;
  source?: string;
}

export default function Counterparts() {
  const { companyId } = useCompany();
  const [showModal, setShowModal] = useState(false);
  const [filter, setFilter] = useState('ALL');
  const [formData, setFormData] = useState({
    name: '',
    eik: '',
    vatNumber: '',
    address: '',
    longAddress: '',
    city: '',
    country: 'България',
    email: '',
    phone: '',
    counterpartType: 'CUSTOMER',
  });
  const [error, setError] = useState('');
  const [viesLoading, setViesLoading] = useState(false);
  const [viesResult, setViesResult] = useState<ViesResult | null>(null);

  const { data, loading, refetch } = useQuery<any>(GET_COUNTERPARTS, {
    variables: { companyId },
    skip: !companyId,
  });
  const [createCounterpart, { loading: creating }] = useMutation<any>(CREATE_COUNTERPART);
  const [deleteCounterpart] = useMutation<any>(DELETE_COUNTERPART);
  const [validateVat] = useMutation<{ validateVat: ViesResult }>(VALIDATE_VAT);

  const counterparts: Counterpart[] = data?.counterparts || [];
  const filteredCounterparts = filter === 'ALL'
    ? counterparts
    : counterparts.filter(c => c.counterpartType === filter);

  const handleValidateVat = async () => {
    if (!formData.vatNumber || formData.vatNumber.length < 3) {
      setError('Въведете валиден ДДС номер (напр. BG123456789)');
      return;
    }

    setViesLoading(true);
    setError('');
    setViesResult(null);

    try {
      const { data: result } = await validateVat({
        variables: { vatNumber: formData.vatNumber }
      });

      if (result?.validateVat) {
        setViesResult(result.validateVat);

        if (result.validateVat.valid) {
          // Попълни данните от VIES
          setFormData(prev => ({
            ...prev,
            name: result.validateVat.name || prev.name,
            longAddress: result.validateVat.longAddress || prev.longAddress,
            country: result.validateVat.countryCode === 'BG' ? 'България' : result.validateVat.countryCode || prev.country,
          }));

          // Ако е български ДДС номер, извлечи ЕИК
          if (result.validateVat.countryCode === 'BG' && result.validateVat.vatNumber) {
            const eik = result.validateVat.vatNumber.replace(/^BG/i, '');
            setFormData(prev => ({ ...prev, eik }));
          }
        }
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при валидация');
    } finally {
      setViesLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name) {
      setError('Името е задължително');
      return;
    }

    try {
      await createCounterpart({
        variables: {
          input: {
            companyId,
            name: formData.name,
            eik: formData.eik || null,
            vatNumber: formData.vatNumber || null,
            address: formData.address || null,
            longAddress: formData.longAddress || null,
            city: formData.city || null,
            country: formData.country || null,
            counterpartType: formData.counterpartType,
            isVatRegistered: !!formData.vatNumber,
          },
        },
      });
      setShowModal(false);
      setFormData({
        name: '',
        eik: '',
        vatNumber: '',
        address: '',
        longAddress: '',
        city: '',
        country: 'България',
        email: '',
        phone: '',
        counterpartType: 'CUSTOMER',
      });
      setViesResult(null);
      refetch();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при създаване');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да изтриете този контрагент?')) return;

    try {
      await deleteCounterpart({ variables: { id } });
      refetch();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
  };

  const getTypeLabel = (counterpartType: string) => {
    const labels: Record<string, string> = {
      CUSTOMER: 'Клиент',
      SUPPLIER: 'Доставчик',
      EMPLOYEE: 'Служител',
      BANK: 'Банка',
      GOVERNMENT: 'Държавна институция',
      OTHER: 'Друг',
    };
    return labels[counterpartType] || counterpartType;
  };

  const getTypeColor = (counterpartType: string) => {
    const colors: Record<string, string> = {
      CUSTOMER: 'bg-green-100 text-green-700',
      SUPPLIER: 'bg-blue-100 text-blue-700',
      EMPLOYEE: 'bg-yellow-100 text-yellow-700',
      BANK: 'bg-indigo-100 text-indigo-700',
      GOVERNMENT: 'bg-red-100 text-red-700',
      OTHER: 'bg-gray-100 text-gray-700',
    };
    return colors[counterpartType] || 'bg-gray-100 text-gray-700';
  };

  if (!companyId) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-gray-500">Моля, изберете фирма от менюто</p>
      </div>
    );
  }

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
          <h1 className="text-2xl font-bold text-gray-900">Контрагенти</h1>
          <p className="mt-1 text-sm text-gray-500">
            Клиенти, доставчици и други контрагенти
          </p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
        >
          + Нов контрагент
        </button>
      </div>

      {/* Filters */}
      <div className="flex gap-2">
        {[
          { value: 'ALL', label: 'Всички' },
          { value: 'CUSTOMER', label: 'Клиенти' },
          { value: 'SUPPLIER', label: 'Доставчици' },
          { value: 'BOTH', label: 'Клиент и Доставчик' },
        ].map(({ value, label }) => (
          <button
            key={value}
            onClick={() => setFilter(value)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              filter === value
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Контрагент
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                ЕИК / ДДС
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Контакти
              </th>
              <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                Тип
              </th>
              <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                Статус
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Действия
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredCounterparts.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                  Няма контрагенти
                </td>
              </tr>
            ) : (
              filteredCounterparts.map((counterpart) => (
                <tr key={counterpart.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="w-10 h-10 flex-shrink-0 bg-gray-100 text-gray-600 rounded-full flex items-center justify-center font-semibold">
                        {counterpart.name.charAt(0)}
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">{counterpart.name}</div>
                        {counterpart.city && (
                          <div className="text-sm text-gray-500">{counterpart.city}, {counterpart.country}</div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {counterpart.eik && (
                      <div className="text-sm text-gray-900">ЕИК: {counterpart.eik}</div>
                    )}
                    {counterpart.vatNumber && (
                      <div className="text-sm text-gray-500">ДДС: {counterpart.vatNumber}</div>
                    )}
                    {!counterpart.eik && !counterpart.vatNumber && (
                      <span className="text-sm text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {counterpart.email && (
                      <div className="text-sm text-gray-900">{counterpart.email}</div>
                    )}
                    {counterpart.phone && (
                      <div className="text-sm text-gray-500">{counterpart.phone}</div>
                    )}
                    {!counterpart.email && !counterpart.phone && (
                      <span className="text-sm text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-center">
                    <span className={`px-2 py-1 text-xs rounded-full ${getTypeColor(counterpart.counterpartType)}`}>
                      {getTypeLabel(counterpart.counterpartType)}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-center">
                    <span className={`px-2 py-1 text-xs rounded-full ${
                      counterpart.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                    }`}>
                      {counterpart.isActive ? 'Активен' : 'Неактивен'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button className="text-blue-600 hover:text-blue-900 mr-4">
                      Редактирай
                    </button>
                    <button
                      onClick={() => handleDelete(counterpart.id)}
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
                <h2 className="text-xl font-semibold text-gray-900">Нов контрагент</h2>
                <button
                  onClick={() => setShowModal(false)}
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

              {/* VIES Validation Result */}
              {viesResult && (
                <div className={`rounded-lg p-4 text-sm ${
                  viesResult.valid
                    ? 'bg-green-50 border border-green-200 text-green-700'
                    : 'bg-yellow-50 border border-yellow-200 text-yellow-700'
                }`}>
                  {viesResult.valid ? (
                    <div>
                      <div className="font-medium">✓ ДДС номерът е валиден ({viesResult.source})</div>
                      {viesResult.name && <div className="mt-1">Име: {viesResult.name}</div>}
                      {viesResult.longAddress && <div>Адрес: {viesResult.longAddress}</div>}
                    </div>
                  ) : (
                    <div className="font-medium">✗ {viesResult.errorMessage || 'ДДС номерът не е валиден'}</div>
                  )}
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* ДДС номер с бутон за VIES валидация */}
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ДДС номер
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={formData.vatNumber}
                      onChange={(e) => setFormData({ ...formData, vatNumber: e.target.value.toUpperCase() })}
                      className="block flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="BG123456789"
                    />
                    <button
                      type="button"
                      onClick={handleValidateVat}
                      disabled={viesLoading || !formData.vatNumber}
                      className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                    >
                      {viesLoading ? 'Проверка...' : 'Провери VIES'}
                    </button>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    Въведете ДДС номер и натиснете "Провери VIES" за автоматично попълване
                  </p>
                </div>

                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Име *
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="Име на фирмата или лицето"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Тип
                  </label>
                  <select
                    value={formData.counterpartType}
                    onChange={(e) => setFormData({ ...formData, counterpartType: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  >
                    <option value="CUSTOMER">Клиент</option>
                    <option value="SUPPLIER">Доставчик</option>
                    <option value="EMPLOYEE">Служител</option>
                    <option value="BANK">Банка</option>
                    <option value="GOVERNMENT">Държавна институция</option>
                    <option value="OTHER">Друг</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ЕИК
                  </label>
                  <input
                    type="text"
                    value={formData.eik}
                    onChange={(e) => setFormData({ ...formData, eik: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="123456789"
                  />
                </div>

                {/* Адрес от VIES (longAddress) */}
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Адрес от VIES
                  </label>
                  <input
                    type="text"
                    value={formData.longAddress}
                    onChange={(e) => setFormData({ ...formData, longAddress: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm bg-gray-50"
                    placeholder="Попълва се автоматично от VIES"
                    readOnly={!!viesResult?.longAddress}
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

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Държава
                  </label>
                  <input
                    type="text"
                    value={formData.country}
                    onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="България"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                >
                  Отказ
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
                >
                  {creating ? 'Създаване...' : 'Създай контрагент'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
