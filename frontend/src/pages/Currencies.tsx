import { useState } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_CURRENCIES, GET_EXCHANGE_RATES, FETCH_ECB_RATES, CREATE_CURRENCY, UPDATE_CURRENCY } from '../graphql/queries';

interface Currency {
  id: string;
  code: string;
  name: string;
  symbol?: string;
  decimalPlaces: number;
  isBaseCurrency: boolean;
  isActive: boolean;
}

interface ExchangeRate {
  id: string;
  fromCurrency: { code: string };
  toCurrency: { code: string };
  rate: number;
  validDate: string;
  rateSource: string;
}

// Списък с популярни валути от ЕЦБ
const ECB_CURRENCIES = [
  { code: 'USD', name: 'US Dollar', nameBg: 'Щатски долар', symbol: '$' },
  { code: 'GBP', name: 'British Pound', nameBg: 'Британска лира', symbol: '£' },
  { code: 'CHF', name: 'Swiss Franc', nameBg: 'Швейцарски франк', symbol: 'Fr' },
  { code: 'JPY', name: 'Japanese Yen', nameBg: 'Японска йена', symbol: '¥' },
  { code: 'BGN', name: 'Bulgarian Lev', nameBg: 'Български лев', symbol: 'лв' },
  { code: 'CZK', name: 'Czech Koruna', nameBg: 'Чешка крона', symbol: 'Kč' },
  { code: 'DKK', name: 'Danish Krone', nameBg: 'Датска крона', symbol: 'kr' },
  { code: 'HUF', name: 'Hungarian Forint', nameBg: 'Унгарски форинт', symbol: 'Ft' },
  { code: 'PLN', name: 'Polish Zloty', nameBg: 'Полска злота', symbol: 'zł' },
  { code: 'RON', name: 'Romanian Leu', nameBg: 'Румънска лея', symbol: 'lei' },
  { code: 'SEK', name: 'Swedish Krona', nameBg: 'Шведска крона', symbol: 'kr' },
  { code: 'NOK', name: 'Norwegian Krone', nameBg: 'Норвежка крона', symbol: 'kr' },
  { code: 'TRY', name: 'Turkish Lira', nameBg: 'Турска лира', symbol: '₺' },
  { code: 'AUD', name: 'Australian Dollar', nameBg: 'Австралийски долар', symbol: 'A$' },
  { code: 'CAD', name: 'Canadian Dollar', nameBg: 'Канадски долар', symbol: 'C$' },
  { code: 'CNY', name: 'Chinese Yuan', nameBg: 'Китайски юан', symbol: '¥' },
  { code: 'HKD', name: 'Hong Kong Dollar', nameBg: 'Хонконгски долар', symbol: 'HK$' },
  { code: 'INR', name: 'Indian Rupee', nameBg: 'Индийска рупия', symbol: '₹' },
  { code: 'KRW', name: 'South Korean Won', nameBg: 'Южнокорейски вон', symbol: '₩' },
  { code: 'MXN', name: 'Mexican Peso', nameBg: 'Мексиканско песо', symbol: '$' },
  { code: 'NZD', name: 'New Zealand Dollar', nameBg: 'Новозеландски долар', symbol: 'NZ$' },
  { code: 'SGD', name: 'Singapore Dollar', nameBg: 'Сингапурски долар', symbol: 'S$' },
  { code: 'ZAR', name: 'South African Rand', nameBg: 'Южноафрикански ранд', symbol: 'R' },
  { code: 'RUB', name: 'Russian Ruble', nameBg: 'Руска рубла', symbol: '₽' },
  { code: 'BRL', name: 'Brazilian Real', nameBg: 'Бразилски реал', symbol: 'R$' },
  { code: 'ISK', name: 'Icelandic Krona', nameBg: 'Исландска крона', symbol: 'kr' },
  { code: 'IDR', name: 'Indonesian Rupiah', nameBg: 'Индонезийска рупия', symbol: 'Rp' },
  { code: 'ILS', name: 'Israeli Shekel', nameBg: 'Израелски шекел', symbol: '₪' },
  { code: 'MYR', name: 'Malaysian Ringgit', nameBg: 'Малайзийски рингит', symbol: 'RM' },
  { code: 'PHP', name: 'Philippine Peso', nameBg: 'Филипинско песо', symbol: '₱' },
  { code: 'THB', name: 'Thai Baht', nameBg: 'Тайландски бат', symbol: '฿' },
];

export default function Currencies() {
  const [selectedCurrency, setSelectedCurrency] = useState<string | null>(null);
  const [fetchingRates, setFetchingRates] = useState(false);
  const [activeTab, setActiveTab] = useState<'active' | 'available'>('active');
  const [showAddModal, setShowAddModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const { data: currenciesData, loading: loadingCurrencies, refetch: refetchCurrencies } = useQuery<any>(GET_CURRENCIES);
  const { data: ratesData, loading: loadingRates, refetch: refetchRates } = useQuery<any>(GET_EXCHANGE_RATES, {
    variables: { baseCurrency: 'EUR' },
  });

  const [fetchEcbRates] = useMutation<any>(FETCH_ECB_RATES);
  const [createCurrency] = useMutation<any>(CREATE_CURRENCY);
  const [updateCurrency] = useMutation<any>(UPDATE_CURRENCY);

  const currencies: Currency[] = currenciesData?.currencies || [];
  const rates: ExchangeRate[] = ratesData?.allExchangeRates || [];

  const activeCurrencies = currencies.filter(c => c.isActive);
  const inactiveCurrencies = currencies.filter(c => !c.isActive);

  // Валути които могат да се добавят (от ЕЦБ списъка, които не са вече добавени)
  const existingCodes = currencies.map(c => c.code);
  const availableCurrencies = ECB_CURRENCIES.filter(c => !existingCodes.includes(c.code));

  const filteredAvailable = searchTerm
    ? availableCurrencies.filter(c =>
        c.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.nameBg.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : availableCurrencies;

  const handleFetchRates = async () => {
    setFetchingRates(true);
    try {
      await fetchEcbRates();
      refetchRates();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при извличане на курсове');
    } finally {
      setFetchingRates(false);
    }
  };

  const handleAddCurrency = async (currency: typeof ECB_CURRENCIES[0]) => {
    try {
      await createCurrency({
        variables: {
          input: {
            code: currency.code,
            name: currency.name,
            nameBg: currency.nameBg,
            symbol: currency.symbol,
            decimalPlaces: 2,
            isBaseCurrency: false,
          },
        },
      });
      refetchCurrencies();
      setShowAddModal(false);
      // След добавяне, обнови курсовете
      await fetchEcbRates();
      refetchRates();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при добавяне на валута');
    }
  };

  const handleToggleActive = async (currency: Currency) => {
    try {
      await updateCurrency({
        variables: {
          id: currency.id,
          input: { isActive: !currency.isActive },
        },
      });
      refetchCurrencies();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при промяна на статус');
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('bg-BG', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const filteredRates = selectedCurrency
    ? rates.filter(r => r.fromCurrency.code === selectedCurrency || r.toCurrency.code === selectedCurrency)
    : rates;

  if (loadingCurrencies) {
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
          <h1 className="text-2xl font-bold text-gray-900">Валути и курсове</h1>
          <p className="mt-1 text-sm text-gray-500">
            Управление на валути и обменни курсове от ЕЦБ
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            + Добави валута
          </button>
          <button
            onClick={handleFetchRates}
            disabled={fetchingRates}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
          >
            {fetchingRates ? 'Зареждане...' : 'Обнови от ЕЦБ'}
          </button>
        </div>
      </div>

      {/* Info Banner */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start">
          <span className="text-2xl mr-3">💶</span>
          <div>
            <h3 className="text-sm font-medium text-blue-900">Базова валута: EUR</h3>
            <p className="mt-1 text-sm text-blue-700">
              Курсовете се извличат от Европейската централна банка (ЕЦБ) за активните валути.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Currencies List */}
        <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
          {/* Tabs */}
          <div className="border-b border-gray-200">
            <nav className="flex -mb-px">
              <button
                onClick={() => setActiveTab('active')}
                className={`flex-1 py-3 px-4 text-center text-sm font-medium border-b-2 ${
                  activeTab === 'active'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                Активни ({activeCurrencies.length})
              </button>
              <button
                onClick={() => setActiveTab('available')}
                className={`flex-1 py-3 px-4 text-center text-sm font-medium border-b-2 ${
                  activeTab === 'available'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                Неактивни ({inactiveCurrencies.length})
              </button>
            </nav>
          </div>

          <div className="p-4 space-y-2 max-h-[500px] overflow-y-auto">
            {activeTab === 'active' ? (
              activeCurrencies.length === 0 ? (
                <p className="text-center text-gray-500 py-4">Няма активни валути</p>
              ) : (
                activeCurrencies.map(currency => (
                  <div
                    key={currency.id}
                    className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                      selectedCurrency === currency.code
                        ? 'border-blue-300 bg-blue-50'
                        : currency.isBaseCurrency
                        ? 'border-green-200 bg-green-50'
                        : 'border-gray-100 hover:bg-gray-50'
                    }`}
                    onClick={() => setSelectedCurrency(
                      selectedCurrency === currency.code ? null : currency.code
                    )}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <span className="text-lg font-bold text-gray-700 w-10">
                          {currency.symbol || currency.code}
                        </span>
                        <div className="ml-2">
                          <div className="text-sm font-medium text-gray-900">{currency.code}</div>
                          <div className="text-xs text-gray-500">{currency.name}</div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {currency.isBaseCurrency && (
                          <span className="px-2 py-0.5 text-xs rounded-full bg-green-100 text-green-700">
                            Базова
                          </span>
                        )}
                        {!currency.isBaseCurrency && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleToggleActive(currency);
                            }}
                            className="px-2 py-0.5 text-xs rounded-full bg-red-100 text-red-600 hover:bg-red-200"
                          >
                            Деактивирай
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))
              )
            ) : (
              inactiveCurrencies.length === 0 ? (
                <p className="text-center text-gray-500 py-4">Няма неактивни валути</p>
              ) : (
                inactiveCurrencies.map(currency => (
                  <div
                    key={currency.id}
                    className="p-3 rounded-lg border border-gray-200 bg-gray-50"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <span className="text-lg font-bold text-gray-400 w-10">
                          {currency.symbol || currency.code}
                        </span>
                        <div className="ml-2">
                          <div className="text-sm font-medium text-gray-600">{currency.code}</div>
                          <div className="text-xs text-gray-400">{currency.name}</div>
                        </div>
                      </div>
                      <button
                        onClick={() => handleToggleActive(currency)}
                        className="px-2 py-0.5 text-xs rounded-full bg-green-100 text-green-600 hover:bg-green-200"
                      >
                        Активирай
                      </button>
                    </div>
                  </div>
                ))
              )
            )}
          </div>
        </div>

        {/* Exchange Rates */}
        <div className="xl:col-span-2 bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
          <div className="p-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">
                Обменни курсове
                {selectedCurrency && (
                  <span className="ml-2 text-sm font-normal text-gray-500">
                    (филтрирано по {selectedCurrency})
                  </span>
                )}
              </h3>
              {selectedCurrency && (
                <button
                  onClick={() => setSelectedCurrency(null)}
                  className="text-sm text-blue-600 hover:text-blue-700"
                >
                  Покажи всички
                </button>
              )}
            </div>
          </div>

          {loadingRates ? (
            <div className="flex items-center justify-center h-48">
              <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
            </div>
          ) : (
            <div className="overflow-x-auto max-h-[400px]">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50 sticky top-0">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">От</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Към</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Курс</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Дата</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Източник</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredRates.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                        Няма обменни курсове. Натиснете "Обнови от ЕЦБ".
                      </td>
                    </tr>
                  ) : (
                    filteredRates.map((rate) => (
                      <tr key={rate.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="px-2 py-1 text-sm font-medium bg-gray-100 rounded">
                            {rate.fromCurrency.code}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="px-2 py-1 text-sm font-medium bg-gray-100 rounded">
                            {rate.toCurrency.code}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-right">
                          <span className="text-sm font-mono font-medium text-gray-900">
                            {rate.rate.toFixed(4)}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="text-sm text-gray-500">{formatDate(rate.validDate)}</span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className={`px-2 py-1 text-xs rounded-full ${
                            rate.rateSource === 'ECB'
                              ? 'bg-green-100 text-green-700'
                              : rate.rateSource === 'FIXED'
                              ? 'bg-blue-100 text-blue-700'
                              : 'bg-gray-100 text-gray-700'
                          }`}>
                            {rate.rateSource}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Fixed Rate Info */}
      <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-lg p-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-green-900">Фиксиран курс BGN/EUR</h3>
            <p className="mt-1 text-sm text-green-700">
              Българският лев е фиксиран към еврото по силата на валутен борд
            </p>
          </div>
          <div className="text-right">
            <div className="text-3xl font-bold text-green-800">1.95583</div>
            <div className="text-sm text-green-600">BGN за 1 EUR</div>
          </div>
        </div>
      </div>

      {/* Add Currency Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 max-h-[80vh] overflow-hidden">
            <div className="p-4 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">Добави валута от ЕЦБ</h3>
                <button
                  onClick={() => setShowAddModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>
              <input
                type="text"
                placeholder="Търси по код или име..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="mt-3 w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              />
            </div>
            <div className="overflow-y-auto max-h-[400px]">
              {filteredAvailable.length === 0 ? (
                <p className="text-center text-gray-500 py-8">
                  {searchTerm ? 'Няма намерени валути' : 'Всички валути от ЕЦБ са добавени'}
                </p>
              ) : (
                <div className="divide-y divide-gray-100">
                  {filteredAvailable.map(currency => (
                    <div
                      key={currency.code}
                      className="p-3 hover:bg-gray-50 flex items-center justify-between"
                    >
                      <div className="flex items-center">
                        <span className="text-lg font-bold text-gray-600 w-10">
                          {currency.symbol}
                        </span>
                        <div className="ml-2">
                          <div className="text-sm font-medium text-gray-900">
                            {currency.code} - {currency.name}
                          </div>
                          <div className="text-xs text-gray-500">{currency.nameBg}</div>
                        </div>
                      </div>
                      <button
                        onClick={() => handleAddCurrency(currency)}
                        className="px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
                      >
                        Добави
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
