import { useState } from 'react';
import { useQuery } from '@apollo/client/react';
import { GET_DEPRECIATION_JOURNAL } from '../../graphql/queries';

interface DepreciationEntry {
  id: string;
  fixedAssetId: number;
  fixedAssetName: string;
  fixedAssetInventoryNumber: string;
  period: string;
  accountingDepreciationAmount: number;
  accountingBookValueBefore: number;
  accountingBookValueAfter: number;
  taxDepreciationAmount: number;
  taxBookValueBefore: number;
  taxBookValueAfter: number;
  isPosted: boolean;
  journalEntryId: number | null;
  postedAt: string | null;
}

export default function DepreciationJournal() {
  const currentYear = new Date().getFullYear();

  const [year, setYear] = useState(currentYear);
  const [month, setMonth] = useState<number | null>(null);

  const { data, loading, refetch } = useQuery<any>(GET_DEPRECIATION_JOURNAL, {
    variables: {
      companyId: '1',
      year,
      month,
    },
  });

  const journal: DepreciationEntry[] = data?.depreciationJournal || [];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('bg-BG', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2,
    }).format(amount || 0);
  };

  const formatPeriod = (periodStr: string) => {
    const date = new Date(periodStr);
    const monthNames = [
      'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
      'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември',
    ];
    return `${monthNames[date.getMonth()]} ${date.getFullYear()}`;
  };

  const monthNames = [
    'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
    'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември',
  ];

  // Calculate totals
  const totals = journal.reduce(
    (acc, entry) => ({
      accountingAmount: acc.accountingAmount + (entry.accountingDepreciationAmount || 0),
      taxAmount: acc.taxAmount + (entry.taxDepreciationAmount || 0),
      count: acc.count + 1,
      posted: acc.posted + (entry.isPosted ? 1 : 0),
    }),
    { accountingAmount: 0, taxAmount: 0, count: 0, posted: 0 }
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Filters */}
      <div className="flex items-end gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Година</label>
          <select
            value={year}
            onChange={(e) => setYear(parseInt(e.target.value))}
            className="block w-32 px-3 py-2 border border-gray-300 rounded-md"
          >
            {[currentYear - 2, currentYear - 1, currentYear, currentYear + 1].map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Месец</label>
          <select
            value={month || ''}
            onChange={(e) => setMonth(e.target.value ? parseInt(e.target.value) : null)}
            className="block w-40 px-3 py-2 border border-gray-300 rounded-md"
          >
            <option value="">Всички месеци</option>
            {monthNames.map((name, idx) => (
              <option key={idx + 1} value={idx + 1}>{name}</option>
            ))}
          </select>
        </div>

        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200"
        >
          Обнови
        </button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500">Записи</p>
          <p className="text-2xl font-semibold text-gray-900">{totals.count}</p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500">Счетоводна амортизация</p>
          <p className="text-xl font-semibold text-gray-900">{formatCurrency(totals.accountingAmount)}</p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500">Данъчна амортизация</p>
          <p className="text-xl font-semibold text-gray-900">{formatCurrency(totals.taxAmount)}</p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <p className="text-sm text-gray-500">Осчетоводени</p>
          <p className="text-2xl font-semibold text-gray-900">{totals.posted} / {totals.count}</p>
        </div>
      </div>

      {/* Journal Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Период</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Актив</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Счет. амортизация</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Счет. балансова</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Данъчна амортизация</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Данъчна балансова</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Разлика</th>
              <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Статус</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {journal.map((entry) => {
              const difference = (entry.taxDepreciationAmount || 0) - (entry.accountingDepreciationAmount || 0);

              return (
                <tr key={entry.id} className="hover:bg-gray-50">
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatPeriod(entry.period)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{entry.fixedAssetInventoryNumber}</div>
                    <div className="text-xs text-gray-500">{entry.fixedAssetName}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm text-gray-900">
                    {formatCurrency(entry.accountingDepreciationAmount)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right">
                    <div className="text-xs text-gray-500">{formatCurrency(entry.accountingBookValueBefore)}</div>
                    <div className="text-sm text-gray-900">{formatCurrency(entry.accountingBookValueAfter)}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm text-gray-900">
                    {formatCurrency(entry.taxDepreciationAmount)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right">
                    <div className="text-xs text-gray-500">{formatCurrency(entry.taxBookValueBefore)}</div>
                    <div className="text-sm text-gray-900">{formatCurrency(entry.taxBookValueAfter)}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right">
                    <div className={`text-sm font-medium ${
                      difference > 0 ? 'text-green-600' : difference < 0 ? 'text-red-600' : 'text-gray-500'
                    }`}>
                      {difference !== 0 ? formatCurrency(Math.abs(difference)) : '-'}
                    </div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-center">
                    {entry.isPosted ? (
                      <div>
                        <span className="inline-flex px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-800">
                          Осчетоводен
                        </span>
                        {entry.journalEntryId && (
                          <div className="text-xs text-gray-500 mt-1">JE #{entry.journalEntryId}</div>
                        )}
                      </div>
                    ) : (
                      <span className="inline-flex px-2 py-1 text-xs font-medium rounded-full bg-yellow-100 text-yellow-800">
                        Неосчетоводен
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {journal.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-500">Няма записи за избрания период</p>
          </div>
        )}
      </div>

      {/* Information */}
      <div className="bg-blue-50 rounded-lg p-4">
        <h4 className="text-sm font-medium text-blue-900 mb-2">Информация</h4>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>Показва всички изчислени амортизации за периода</li>
          <li>Разликата = данъчна - счетоводна амортизация</li>
          <li>JE # показва номера на журналния запис</li>
        </ul>
      </div>
    </div>
  );
}
