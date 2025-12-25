import { useState, useMemo } from 'react';
import { useLazyQuery, useQuery } from '@apollo/client/react';
import {
  GET_TRANSACTION_LOG,
  GET_GENERAL_LEDGER,
  GET_ACCOUNTS,
  GET_COUNTERPARTS,
} from '../graphql/queries';
import { useCompany } from '../contexts/CompanyContext';

interface Counterpart {
  id: string;
  name: string;
  eik: string;
  vatNumber: string;
}

interface TransactionEntry {
  date: string;
  entryNumber: string;
  documentNumber: string;
  description: string;
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
  counterpartName: string;
}


interface CounterpartBalance {
  name: string;
  openingDebit: number;
  openingCredit: number;
  periodDebit: number;
  periodCredit: number;
  closingDebit: number;
  closingCredit: number;
}

type ReportType = 'turnover' | 'chronological';

const PERIOD_PRESETS = [
  { id: 'currentMonth', name: 'Текущ месец' },
  { id: 'lastMonth', name: 'Предходен месец' },
  { id: 'currentQuarter', name: 'Текущо тримесечие' },
  { id: 'currentYear', name: 'Текуща година' },
  { id: 'custom', name: 'Избор на период' },
];

function getPresetDates(presetId: string): { startDate: string; endDate: string } {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth();

  switch (presetId) {
    case 'currentMonth':
      return {
        startDate: new Date(year, month, 1).toISOString().split('T')[0],
        endDate: new Date(year, month + 1, 0).toISOString().split('T')[0],
      };
    case 'lastMonth':
      return {
        startDate: new Date(year, month - 1, 1).toISOString().split('T')[0],
        endDate: new Date(year, month, 0).toISOString().split('T')[0],
      };
    case 'currentQuarter': {
      const quarterStart = Math.floor(month / 3) * 3;
      return {
        startDate: new Date(year, quarterStart, 1).toISOString().split('T')[0],
        endDate: new Date(year, quarterStart + 3, 0).toISOString().split('T')[0],
      };
    }
    case 'currentYear':
      return {
        startDate: new Date(year, 0, 1).toISOString().split('T')[0],
        endDate: new Date(year, 11, 31).toISOString().split('T')[0],
      };
    default:
      return {
        startDate: new Date(year, month, 1).toISOString().split('T')[0],
        endDate: new Date(year, month + 1, 0).toISOString().split('T')[0],
      };
  }
}

export default function CounterpartyReports() {
  const { companyId } = useCompany();
  const [reportType, setReportType] = useState<ReportType>('turnover');
  const [periodPreset, setPeriodPreset] = useState('currentMonth');
  const [startDate, setStartDate] = useState(() => getPresetDates('currentMonth').startDate);
  const [endDate, setEndDate] = useState(() => getPresetDates('currentMonth').endDate);
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const [selectedCounterpartId, setSelectedCounterpartId] = useState<string>('');
  const [counterpartSearch, setCounterpartSearch] = useState('');

  // Fetch accounts and counterparts
  const { data: accountsData } = useQuery<any>(GET_ACCOUNTS, {
    variables: { companyId },
    skip: !companyId,
  });
  const { data: counterpartsData } = useQuery<any>(GET_COUNTERPARTS, {
    variables: { companyId },
    skip: !companyId,
  });

  const accounts = accountsData?.accounts || [];
  const counterparts: Counterpart[] = counterpartsData?.counterparts || [];

  // Filter counterparts by search
  const filteredCounterparts = useMemo(() => {
    if (!counterpartSearch) return counterparts;
    const search = counterpartSearch.toLowerCase();
    return counterparts.filter(
      (c) =>
        c.name.toLowerCase().includes(search) ||
        c.eik?.toLowerCase().includes(search) ||
        c.vatNumber?.toLowerCase().includes(search)
    );
  }, [counterparts, counterpartSearch]);

  // Lazy queries
  const [fetchGeneralLedger, { data: ledgerData, loading: ledgerLoading }] =
    useLazyQuery<any>(GET_GENERAL_LEDGER);
  const [fetchTransactionLog, { data: logData, loading: logLoading }] =
    useLazyQuery<any>(GET_TRANSACTION_LOG);

  const isLoading = ledgerLoading || logLoading;

  const handlePresetChange = (preset: string) => {
    setPeriodPreset(preset);
    if (preset !== 'custom') {
      const { startDate: newStart, endDate: newEnd } = getPresetDates(preset);
      setStartDate(newStart);
      setEndDate(newEnd);
    }
  };

  const handleGenerateReport = () => {
    const input = {
      companyId,
      startDate,
      endDate,
      accountId: selectedAccountId || undefined,
    };

    if (reportType === 'turnover') {
      fetchGeneralLedger({ variables: { input } });
    } else {
      fetchTransactionLog({ variables: { input } });
    }
  };

  const formatAmount = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount;
    return num.toLocaleString('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  // Calculate counterpart-based turnover from general ledger data
  const counterpartBalances = useMemo((): CounterpartBalance[] => {
    if (!ledgerData?.generalLedger?.accounts) return [];

    const balanceMap = new Map<string, CounterpartBalance>();

    // Get all entries from all accounts
    for (const account of ledgerData.generalLedger.accounts) {
      for (const entry of account.entries || []) {
        const name = entry.counterpartName || '(Без контрагент)';

        // Skip if filtering by counterpart and doesn't match
        if (selectedCounterpartId) {
          const selectedCounterpart = counterparts.find(
            (c) => c.id === selectedCounterpartId
          );
          if (selectedCounterpart && name !== selectedCounterpart.name && name !== '(Без контрагент)') {
            continue;
          }
        }

        if (!balanceMap.has(name)) {
          balanceMap.set(name, {
            name,
            openingDebit: 0,
            openingCredit: 0,
            periodDebit: 0,
            periodCredit: 0,
            closingDebit: 0,
            closingCredit: 0,
          });
        }

        const balance = balanceMap.get(name)!;
        balance.periodDebit += parseFloat(entry.debitAmount) || 0;
        balance.periodCredit += parseFloat(entry.creditAmount) || 0;
      }
    }

    // Calculate closing balances
    for (const balance of balanceMap.values()) {
      const net =
        balance.openingDebit -
        balance.openingCredit +
        balance.periodDebit -
        balance.periodCredit;
      if (net >= 0) {
        balance.closingDebit = net;
        balance.closingCredit = 0;
      } else {
        balance.closingDebit = 0;
        balance.closingCredit = Math.abs(net);
      }
    }

    return Array.from(balanceMap.values()).sort((a, b) => a.name.localeCompare(b.name));
  }, [ledgerData, selectedCounterpartId, counterparts]);

  // Filter chronological entries by counterpart
  const filteredEntries = useMemo((): TransactionEntry[] => {
    if (!logData?.transactionLog?.entries) return [];

    let entries = logData.transactionLog.entries;

    if (selectedCounterpartId) {
      const selectedCounterpart = counterparts.find((c) => c.id === selectedCounterpartId);
      if (selectedCounterpart) {
        entries = entries.filter(
          (e: TransactionEntry) =>
            e.counterpartName === selectedCounterpart.name ||
            !e.counterpartName
        );
      }
    }

    return entries;
  }, [logData, selectedCounterpartId, counterparts]);

  // Totals for turnover report
  const totals = useMemo(() => {
    return counterpartBalances.reduce(
      (acc, b) => ({
        openingDebit: acc.openingDebit + b.openingDebit,
        openingCredit: acc.openingCredit + b.openingCredit,
        periodDebit: acc.periodDebit + b.periodDebit,
        periodCredit: acc.periodCredit + b.periodCredit,
        closingDebit: acc.closingDebit + b.closingDebit,
        closingCredit: acc.closingCredit + b.closingCredit,
      }),
      {
        openingDebit: 0,
        openingCredit: 0,
        periodDebit: 0,
        periodCredit: 0,
        closingDebit: 0,
        closingCredit: 0,
      }
    );
  }, [counterpartBalances]);

  const hasData =
    (reportType === 'turnover' && counterpartBalances.length > 0) ||
    (reportType === 'chronological' && filteredEntries.length > 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Справки по контрагенти</h1>
        <p className="mt-1 text-sm text-gray-500">
          Справки за обороти и операции по контрагенти
        </p>
      </div>

      {/* Report Type Selection */}
      <div className="flex gap-4">
        <button
          onClick={() => setReportType('turnover')}
          className={`px-6 py-3 rounded-lg border-2 transition-all ${
            reportType === 'turnover'
              ? 'border-blue-500 bg-blue-50 text-blue-700'
              : 'border-gray-200 bg-white hover:border-gray-300'
          }`}
        >
          <div className="text-xl mb-1">📊</div>
          <div className="font-medium">Оборотна ведомост</div>
          <div className="text-xs text-gray-500">по контрагенти</div>
        </button>
        <button
          onClick={() => setReportType('chronological')}
          className={`px-6 py-3 rounded-lg border-2 transition-all ${
            reportType === 'chronological'
              ? 'border-blue-500 bg-blue-50 text-blue-700'
              : 'border-gray-200 bg-white hover:border-gray-300'
          }`}
        >
          <div className="text-xl mb-1">📅</div>
          <div className="font-medium">Хронологична справка</div>
          <div className="text-xs text-gray-500">с контрагенти</div>
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Филтри</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Period Preset */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Период</label>
            <select
              value={periodPreset}
              onChange={(e) => handlePresetChange(e.target.value)}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            >
              {PERIOD_PRESETS.map((preset) => (
                <option key={preset.id} value={preset.id}>
                  {preset.name}
                </option>
              ))}
            </select>
          </div>

          {/* Start Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">От дата</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setPeriodPreset('custom');
              }}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>

          {/* End Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">До дата</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setPeriodPreset('custom');
              }}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>

          {/* Account Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Сметка {reportType === 'turnover' ? '(задължително)' : '(опционално)'}
            </label>
            <select
              value={selectedAccountId}
              onChange={(e) => setSelectedAccountId(e.target.value)}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            >
              <option value="">Всички сметки</option>
              {accounts.map((acc: any) => (
                <option key={acc.id} value={acc.id}>
                  {acc.code} - {acc.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Counterpart Filter */}
        <div className="mt-4 pt-4 border-t border-gray-200">
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Контрагент (опционално)
          </label>
          <div className="flex gap-4">
            <input
              type="text"
              placeholder="Търсене по име, ЕИК или ДДС номер..."
              value={counterpartSearch}
              onChange={(e) => setCounterpartSearch(e.target.value)}
              className="block w-64 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
            <select
              value={selectedCounterpartId}
              onChange={(e) => setSelectedCounterpartId(e.target.value)}
              className="block w-80 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            >
              <option value="">Всички контрагенти</option>
              {filteredCounterparts.map((cp) => (
                <option key={cp.id} value={cp.id}>
                  {cp.name} {cp.eik ? `(ЕИК: ${cp.eik})` : ''}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Actions */}
        <div className="mt-6 flex items-center gap-3">
          <button
            onClick={handleGenerateReport}
            disabled={isLoading || (reportType === 'turnover' && !selectedAccountId)}
            className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 font-medium"
          >
            {isLoading ? 'Зареждане...' : 'Генерирай справка'}
          </button>
          {reportType === 'turnover' && !selectedAccountId && (
            <span className="text-sm text-orange-600">
              Изберете сметка за оборотна ведомост
            </span>
          )}
        </div>
      </div>

      {/* Report Content */}
      {isLoading && (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
        </div>
      )}

      {/* Turnover Report by Counterpart */}
      {!isLoading && reportType === 'turnover' && counterpartBalances.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h3 className="text-lg font-semibold text-gray-900">
              Оборотна ведомост по контрагенти
            </h3>
            <p className="text-sm text-gray-500">
              Период: {startDate} - {endDate}
              {selectedAccountId &&
                ` | Сметка: ${
                  accounts.find((a: any) => a.id === selectedAccountId)?.code
                }`}
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Контрагент
                  </th>
                  <th
                    className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase"
                    colSpan={2}
                  >
                    Начално салдо
                  </th>
                  <th
                    className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase"
                    colSpan={2}
                  >
                    Обороти
                  </th>
                  <th
                    className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase"
                    colSpan={2}
                  >
                    Крайно салдо
                  </th>
                </tr>
                <tr className="bg-gray-100">
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500"></th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {counterpartBalances.map((balance, idx) => (
                  <tr key={idx} className="hover:bg-gray-50">
                    <td className="px-4 py-2 text-sm text-gray-900">{balance.name}</td>
                    <td className="px-4 py-2 text-sm text-right font-mono">
                      {formatAmount(balance.openingDebit)}
                    </td>
                    <td className="px-4 py-2 text-sm text-right font-mono">
                      {formatAmount(balance.openingCredit)}
                    </td>
                    <td className="px-4 py-2 text-sm text-right font-mono">
                      {formatAmount(balance.periodDebit)}
                    </td>
                    <td className="px-4 py-2 text-sm text-right font-mono">
                      {formatAmount(balance.periodCredit)}
                    </td>
                    <td className="px-4 py-2 text-sm text-right font-mono font-semibold">
                      {formatAmount(balance.closingDebit)}
                    </td>
                    <td className="px-4 py-2 text-sm text-right font-mono font-semibold">
                      {formatAmount(balance.closingCredit)}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="bg-gray-100 font-semibold">
                <tr>
                  <td className="px-4 py-3 text-sm">ОБЩО</td>
                  <td className="px-4 py-3 text-sm text-right font-mono">
                    {formatAmount(totals.openingDebit)}
                  </td>
                  <td className="px-4 py-3 text-sm text-right font-mono">
                    {formatAmount(totals.openingCredit)}
                  </td>
                  <td className="px-4 py-3 text-sm text-right font-mono">
                    {formatAmount(totals.periodDebit)}
                  </td>
                  <td className="px-4 py-3 text-sm text-right font-mono">
                    {formatAmount(totals.periodCredit)}
                  </td>
                  <td className="px-4 py-3 text-sm text-right font-mono">
                    {formatAmount(totals.closingDebit)}
                  </td>
                  <td className="px-4 py-3 text-sm text-right font-mono">
                    {formatAmount(totals.closingCredit)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}

      {/* Chronological Report with Counterparts */}
      {!isLoading && reportType === 'chronological' && filteredEntries.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h3 className="text-lg font-semibold text-gray-900">
              Хронологична справка с контрагенти
            </h3>
            <p className="text-sm text-gray-500">
              Период: {startDate} - {endDate} | {filteredEntries.length} записа
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Дата
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Документ
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Контрагент
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Сметка
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Описание
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Дебит
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Кредит
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredEntries.map((entry, idx) => (
                  <tr key={idx} className="hover:bg-gray-50">
                    <td className="px-4 py-2 text-sm text-gray-900">{entry.date}</td>
                    <td className="px-4 py-2 text-sm font-mono">
                      {entry.documentNumber || entry.entryNumber}
                    </td>
                    <td className="px-4 py-2 text-sm">
                      <span
                        className={
                          entry.counterpartName
                            ? 'text-gray-900'
                            : 'text-gray-400 italic'
                        }
                      >
                        {entry.counterpartName || '(Без контрагент)'}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-sm">
                      <span className="font-mono font-medium">{entry.accountCode}</span>
                      <span className="ml-1 text-gray-500 text-xs">{entry.accountName}</span>
                    </td>
                    <td className="px-4 py-2 text-sm text-gray-600">{entry.description}</td>
                    <td className="px-4 py-2 text-sm text-right font-mono">
                      {formatAmount(entry.debitAmount)}
                    </td>
                    <td className="px-4 py-2 text-sm text-right font-mono">
                      {formatAmount(entry.creditAmount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* No Data */}
      {!isLoading && !hasData && (
        <div className="bg-white rounded-lg shadow p-12 text-center text-gray-500">
          <div className="text-4xl mb-4">📊</div>
          <p>Изберете параметри и натиснете "Генерирай справка"</p>
        </div>
      )}
    </div>
  );
}
