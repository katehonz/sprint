import { useState } from 'react';
import { useMutation, useQuery } from '@apollo/client/react';
import {
  GET_CALCULATED_PERIODS,
  CALCULATE_MONTHLY_DEPRECIATION,
  POST_DEPRECIATION,
} from '../../graphql/queries';

interface CalculatedPeriod {
  year: number;
  month: number;
  periodDisplay: string;
  isPosted: boolean;
  totalAccountingAmount: number;
  totalTaxAmount: number;
  assetsCount: number;
}

interface DepreciationError {
  fixedAssetId: number;
  assetName: string;
  errorMessage: string;
}

interface CalculationResult {
  calculated: any[];
  errors: DepreciationError[];
  totalAccountingAmount: number;
  totalTaxAmount: number;
}

interface PostResult {
  journalEntryId: number;
  totalAmount: number;
  assetsCount: number;
}

interface Props {
  onRefreshAssets: () => void;
}

export default function DepreciationCalculation({ onRefreshAssets }: Props) {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;

  const [year, setYear] = useState(currentYear);
  const [month, setMonth] = useState(currentMonth);
  const [result, setResult] = useState<CalculationResult | null>(null);
  const [postResult, setPostResult] = useState<PostResult | null>(null);

  const { data: periodsData, refetch: refetchPeriods } = useQuery<any>(GET_CALCULATED_PERIODS, {
    variables: { companyId: '1' },
  });

  const [calculateDepreciation, { loading: calculating }] = useMutation<any>(CALCULATE_MONTHLY_DEPRECIATION);
  const [postDepreciation, { loading: posting }] = useMutation<any>(POST_DEPRECIATION);

  const calculatedPeriods: CalculatedPeriod[] = periodsData?.calculatedPeriods || [];

  const handleCalculate = async () => {
    try {
      setResult(null);
      setPostResult(null);

      const response = await calculateDepreciation({
        variables: {
          companyId: '1',
          year,
          month,
        },
      });

      setResult(response.data.calculateMonthlyDepreciation);
      refetchPeriods();
      onRefreshAssets();
    } catch (err) {
      alert('Грешка при изчисляване: ' + (err instanceof Error ? err.message : 'Неизвестна грешка'));
    }
  };

  const handlePost = async () => {
    if (!confirm('Сигурни ли сте, че искате да осчетоводите амортизацията?')) {
      return;
    }

    try {
      const response = await postDepreciation({
        variables: {
          companyId: '1',
          year,
          month,
        },
      });

      setPostResult(response.data.postDepreciation);
      refetchPeriods();
      onRefreshAssets();
    } catch (err) {
      alert('Грешка при осчетоводяване: ' + (err instanceof Error ? err.message : 'Неизвестна грешка'));
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('bg-BG', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2,
    }).format(amount || 0);
  };

  const monthNames = [
    'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
    'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември',
  ];

  return (
    <div className="space-y-6">
      {/* Calculated Periods Display */}
      {calculatedPeriods.length > 0 && (
        <div className="bg-blue-50 rounded-lg p-6">
          <h3 className="text-lg font-medium text-blue-900 mb-4">
            Изчислени и осчетоводени периоди
          </h3>
          <div className="flex flex-wrap gap-2">
            {calculatedPeriods.map((period, idx) => (
              <div
                key={idx}
                className={`px-3 py-1 rounded-md text-sm font-medium ${
                  period.isPosted
                    ? 'bg-green-100 text-green-800 border border-green-300'
                    : 'bg-yellow-100 text-yellow-800 border border-yellow-300'
                }`}
              >
                {period.periodDisplay}
                {period.isPosted && <span className="ml-1" title="Осчетоводен">&#10003;</span>}
              </div>
            ))}
          </div>
          <div className="mt-3 text-sm text-blue-700">
            <p>Зелени: Изчислени и осчетоводени</p>
            <p>Жълти: Изчислени, но неосчетоводени</p>
          </div>
        </div>
      )}

      {/* Period Selection */}
      <div className="bg-gray-50 rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">
          Изчисляване на месечна амортизация
        </h3>

        <div className="flex items-end gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Година</label>
            <select
              value={year}
              onChange={(e) => setYear(parseInt(e.target.value))}
              className="block w-32 px-3 py-2 border border-gray-300 rounded-md"
            >
              {[currentYear - 1, currentYear, currentYear + 1].map((y) => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Месец</label>
            <select
              value={month}
              onChange={(e) => setMonth(parseInt(e.target.value))}
              className="block w-40 px-3 py-2 border border-gray-300 rounded-md"
            >
              {monthNames.map((name, idx) => (
                <option key={idx + 1} value={idx + 1}>{name}</option>
              ))}
            </select>
          </div>

          <button
            onClick={handleCalculate}
            disabled={calculating}
            className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {calculating ? 'Изчисляване...' : 'Изчисли амортизация'}
          </button>
        </div>

        <div className="mt-4 text-sm text-gray-600">
          <p>Изчислява се месечна амортизация за всички активни ДМА</p>
          <p>Отделно се изчислява счетоводна и данъчна амортизация</p>
        </div>
      </div>

      {/* Calculation Result */}
      {result && (
        <div className={`rounded-lg p-6 ${result.calculated.length > 0 ? 'bg-green-50' : 'bg-yellow-50'}`}>
          <h3 className={`text-lg font-medium ${result.calculated.length > 0 ? 'text-green-900' : 'text-yellow-900'} mb-4`}>
            {result.calculated.length > 0 ? 'Успешно изчисление' : 'Няма активи за амортизация'}
          </h3>

          {result.calculated.length > 0 && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p className="text-sm text-gray-600">Обработени активи</p>
                  <p className="text-2xl font-semibold text-gray-900">{result.calculated.length}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Счетоводна амортизация</p>
                  <p className="text-xl font-semibold text-gray-900">
                    {formatCurrency(result.totalAccountingAmount)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Данъчна амортизация</p>
                  <p className="text-xl font-semibold text-gray-900">
                    {formatCurrency(result.totalTaxAmount)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Разлика</p>
                  <p className="text-xl font-semibold text-gray-900">
                    {formatCurrency(Math.abs(result.totalTaxAmount - result.totalAccountingAmount))}
                  </p>
                </div>
              </div>

              {result.errors.length > 0 && (
                <div className="mt-4 p-4 bg-yellow-100 rounded-md">
                  <p className="text-sm font-medium text-yellow-800">
                    Грешки при {result.errors.length} актива:
                  </p>
                  <ul className="mt-2 text-sm text-yellow-700">
                    {result.errors.map((error, idx) => (
                      <li key={idx}>{error.assetName}: {error.errorMessage}</li>
                    ))}
                  </ul>
                </div>
              )}

              {!postResult && (
                <div className="mt-6 flex items-center justify-between p-4 bg-white rounded-lg border border-green-200">
                  <div>
                    <p className="text-sm font-medium text-gray-900">Осчетоводяване</p>
                    <p className="text-sm text-gray-500 mt-1">
                      Създава журнален запис: Дт 603 Разходи за амортизация / Кт 241 Натрупана амортизация
                    </p>
                  </div>
                  <button
                    onClick={handlePost}
                    disabled={posting}
                    className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
                  >
                    {posting ? 'Осчетоводяване...' : 'Осчетоводи'}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Posting Result */}
      {postResult && (
        <div className="bg-blue-50 rounded-lg p-6">
          <h3 className="text-lg font-medium text-blue-900 mb-4">Успешно осчетоводяване</h3>
          <div className="space-y-3">
            <div className="flex items-center">
              <span className="text-sm text-gray-600 w-40">Журнален запис:</span>
              <span className="font-medium text-gray-900">#{postResult.journalEntryId}</span>
            </div>
            <div className="flex items-center">
              <span className="text-sm text-gray-600 w-40">Обща сума:</span>
              <span className="font-medium text-gray-900">{formatCurrency(postResult.totalAmount)}</span>
            </div>
            <div className="flex items-center">
              <span className="text-sm text-gray-600 w-40">Брой активи:</span>
              <span className="font-medium text-gray-900">{postResult.assetsCount}</span>
            </div>
          </div>
        </div>
      )}

      {/* Information Box */}
      <div className="bg-gray-50 rounded-lg p-6">
        <h4 className="text-sm font-medium text-gray-900 mb-3">Информация</h4>
        <div className="space-y-2 text-sm text-gray-600">
          <p><strong>Счетоводна амортизация:</strong> Изчислява се според счетоводната политика.</p>
          <p><strong>Данъчна амортизация:</strong> Изчислява се според максималните норми в ЗКПО.</p>
          <p><strong>Разликата</strong> създава временни разлики за отсрочени данъци.</p>
        </div>
      </div>
    </div>
  );
}
