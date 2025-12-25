import { useState, useMemo } from 'react';
import { useLazyQuery, useMutation } from '@apollo/client/react';
import {
  GET_TURNOVER_SHEET,
  GET_CHRONOLOGICAL_REPORT,
  GET_GENERAL_LEDGER,
  GET_TRANSACTION_LOG,
  EXPORT_TURNOVER_SHEET,
  EXPORT_CHRONOLOGICAL_REPORT,
  EXPORT_GENERAL_LEDGER,
  EXPORT_TRANSACTION_LOG,
  GET_ACCOUNTS,
} from '../graphql/queries';
import { useQuery } from '@apollo/client/react';
import { useCompany } from '../contexts/CompanyContext';

type ReportType = 'turnover' | 'chronological' | 'generalLedger' | 'transactionLog';

interface ReportConfig {
  id: ReportType;
  name: string;
  description: string;
  icon: string;
}

const REPORT_TYPES: ReportConfig[] = [
  {
    id: 'turnover',
    name: 'Оборотна ведомост',
    description: 'Обобщена справка за обороти и салда по сметки',
    icon: '📊',
  },
  {
    id: 'chronological',
    name: 'Хронологичен регистър',
    description: 'Хронологичен списък на всички счетоводни операции',
    icon: '📅',
  },
  {
    id: 'generalLedger',
    name: 'Главна книга',
    description: 'Детайлна справка по сметки с всички движения',
    icon: '📖',
  },
  {
    id: 'transactionLog',
    name: 'Дневник на операциите',
    description: 'Списък на всички счетоводни записвания',
    icon: '📝',
  },
];

const PERIOD_PRESETS = [
  { id: 'currentMonth', name: 'Текущ месец' },
  { id: 'lastMonth', name: 'Предходен месец' },
  { id: 'currentQuarter', name: 'Текущо тримесечие' },
  { id: 'lastQuarter', name: 'Предходно тримесечие' },
  { id: 'currentYear', name: 'Текуща година' },
  { id: 'lastYear', name: 'Предходна година' },
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
    case 'lastQuarter': {
      const lastQuarterStart = Math.floor(month / 3) * 3 - 3;
      const lastQuarterYear = lastQuarterStart < 0 ? year - 1 : year;
      const adjustedStart = lastQuarterStart < 0 ? lastQuarterStart + 12 : lastQuarterStart;
      return {
        startDate: new Date(lastQuarterYear, adjustedStart, 1).toISOString().split('T')[0],
        endDate: new Date(lastQuarterYear, adjustedStart + 3, 0).toISOString().split('T')[0],
      };
    }
    case 'currentYear':
      return {
        startDate: new Date(year, 0, 1).toISOString().split('T')[0],
        endDate: new Date(year, 11, 31).toISOString().split('T')[0],
      };
    case 'lastYear':
      return {
        startDate: new Date(year - 1, 0, 1).toISOString().split('T')[0],
        endDate: new Date(year - 1, 11, 31).toISOString().split('T')[0],
      };
    default:
      return {
        startDate: new Date(year, month, 1).toISOString().split('T')[0],
        endDate: new Date(year, month + 1, 0).toISOString().split('T')[0],
      };
  }
}

export default function Reports() {
  const { companyId } = useCompany();
  const [selectedReport, setSelectedReport] = useState<ReportType>('turnover');
  const [periodPreset, setPeriodPreset] = useState('currentMonth');
  const [startDate, setStartDate] = useState(() => getPresetDates('currentMonth').startDate);
  const [endDate, setEndDate] = useState(() => getPresetDates('currentMonth').endDate);
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const [showZeroBalances, setShowZeroBalances] = useState(false);
  const [accountCodeDepth, setAccountCodeDepth] = useState<number | null>(null);
  const [expandedAccounts, setExpandedAccounts] = useState<Set<number>>(new Set());

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: accountsData } = useQuery<any>(GET_ACCOUNTS, {
    variables: { companyId },
    skip: !companyId,
  });

  const accounts = accountsData?.accounts || [];

  // Lazy queries for reports
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [fetchTurnover, { data: turnoverData, loading: turnoverLoading }] = useLazyQuery<any>(GET_TURNOVER_SHEET);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [fetchChronological, { data: chronoData, loading: chronoLoading }] = useLazyQuery<any>(GET_CHRONOLOGICAL_REPORT);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [fetchGeneralLedger, { data: ledgerData, loading: ledgerLoading }] = useLazyQuery<any>(GET_GENERAL_LEDGER);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [fetchTransactionLog, { data: logData, loading: logLoading }] = useLazyQuery<any>(GET_TRANSACTION_LOG);

  // Export mutations
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [exportTurnover] = useMutation<any>(EXPORT_TURNOVER_SHEET);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [exportChronological] = useMutation<any>(EXPORT_CHRONOLOGICAL_REPORT);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [exportGeneralLedger] = useMutation<any>(EXPORT_GENERAL_LEDGER);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [exportTransactionLog] = useMutation<any>(EXPORT_TRANSACTION_LOG);

  const isLoading = turnoverLoading || chronoLoading || ledgerLoading || logLoading;

  const handlePresetChange = (preset: string) => {
    setPeriodPreset(preset);
    if (preset !== 'custom') {
      const { startDate: newStart, endDate: newEnd } = getPresetDates(preset);
      setStartDate(newStart);
      setEndDate(newEnd);
    }
  };

  const handleGenerateReport = () => {
    const baseInput = {
      companyId,
      startDate,
      endDate,
      accountId: selectedAccountId || undefined,
    };

    switch (selectedReport) {
      case 'turnover':
        fetchTurnover({
          variables: {
            input: {
              ...baseInput,
              showZeroBalances,
              accountCodeDepth: accountCodeDepth || undefined,
            },
          },
        });
        break;
      case 'chronological':
        fetchChronological({ variables: { input: baseInput } });
        break;
      case 'generalLedger':
        fetchGeneralLedger({ variables: { input: baseInput } });
        break;
      case 'transactionLog':
        fetchTransactionLog({ variables: { input: baseInput } });
        break;
    }
  };

  const handleExport = async (format: 'pdf' | 'xlsx' | 'csv') => {
    const baseInput = {
      companyId,
      startDate,
      endDate,
      accountId: selectedAccountId || undefined,
    };

    try {
      let result;
      switch (selectedReport) {
        case 'turnover':
          result = await exportTurnover({
            variables: {
              input: { ...baseInput, showZeroBalances, accountCodeDepth: accountCodeDepth || undefined },
              format,
            },
          });
          break;
        case 'chronological':
          result = await exportChronological({ variables: { input: baseInput, format } });
          break;
        case 'generalLedger':
          result = await exportGeneralLedger({ variables: { input: baseInput, format } });
          break;
        case 'transactionLog':
          result = await exportTransactionLog({ variables: { input: baseInput, format } });
          break;
      }

      if (result?.data) {
        const exportData = Object.values(result.data)[0] as { content: string; filename: string; mimeType: string };
        // Decode base64 to binary correctly
        const binaryString = atob(exportData.content);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i);
        }
        const blob = new Blob([bytes], { type: exportData.mimeType });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = exportData.filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при експорт');
    }
  };

  const formatAmount = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount;
    return num.toLocaleString('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  const toggleAccountExpand = (accountId: number) => {
    setExpandedAccounts(prev => {
      const newSet = new Set(prev);
      if (newSet.has(accountId)) {
        newSet.delete(accountId);
      } else {
        newSet.add(accountId);
      }
      return newSet;
    });
  };

  // Render report content based on selected type
  const renderReportContent = () => {
    switch (selectedReport) {
      case 'turnover':
        if (!turnoverData?.turnoverSheet) return null;
        const ts = turnoverData.turnoverSheet;
        return (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
              <h3 className="text-lg font-semibold text-gray-900">Оборотна ведомост</h3>
              <p className="text-sm text-gray-500">
                {ts.companyName} | Период: {ts.periodStart} - {ts.periodEnd}
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Сметка</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase" colSpan={2}>Начално салдо</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase" colSpan={2}>Обороти</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase" colSpan={2}>Крайно салдо</th>
                  </tr>
                  <tr className="bg-gray-100">
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Код / Наименование</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {ts.entries.map((entry: any, idx: number) => (
                    <tr key={idx} className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm">
                        <span className="font-mono font-medium text-gray-700">{entry.accountCode}</span>
                        <span className="ml-2 text-gray-600">{entry.accountName}</span>
                      </td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.openingDebit)}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.openingCredit)}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.periodDebit)}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.periodCredit)}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.closingDebit)}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.closingCredit)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-gray-100 font-semibold">
                  <tr>
                    <td className="px-4 py-3 text-sm">ОБЩО</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(ts.totals.openingDebit)}</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(ts.totals.openingCredit)}</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(ts.totals.periodDebit)}</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(ts.totals.periodCredit)}</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(ts.totals.closingDebit)}</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(ts.totals.closingCredit)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        );

      case 'chronological':
        if (!chronoData?.chronologicalReport) return null;
        const cr = chronoData.chronologicalReport;
        return (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
              <h3 className="text-lg font-semibold text-gray-900">Хронологичен регистър</h3>
              <p className="text-sm text-gray-500">
                {cr.companyName} | Период: {cr.periodStart} - {cr.periodEnd}
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Дата</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Дебит сметка</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Кредит сметка</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Сума</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Описание</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {cr.entries.map((entry: any, idx: number) => (
                    <tr key={idx} className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-gray-900">{entry.date}</td>
                      <td className="px-4 py-2 text-sm">
                        <span className="font-mono font-medium">{entry.debitAccountCode}</span>
                        <span className="ml-1 text-gray-500 text-xs">{entry.debitAccountName}</span>
                      </td>
                      <td className="px-4 py-2 text-sm">
                        <span className="font-mono font-medium">{entry.creditAccountCode}</span>
                        <span className="ml-1 text-gray-500 text-xs">{entry.creditAccountName}</span>
                      </td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.amount)}</td>
                      <td className="px-4 py-2 text-sm text-gray-600">{entry.description}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-gray-100 font-semibold">
                  <tr>
                    <td colSpan={3} className="px-4 py-3 text-sm">ОБЩО</td>
                    <td className="px-4 py-3 text-sm text-right font-mono">{formatAmount(cr.totalAmount)}</td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        );

      case 'generalLedger':
        if (!ledgerData?.generalLedger) return null;
        const gl = ledgerData.generalLedger;
        return (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
              <h3 className="text-lg font-semibold text-gray-900">Главна книга</h3>
              <p className="text-sm text-gray-500">
                {gl.companyName} | Период: {gl.periodStart} - {gl.periodEnd}
              </p>
            </div>
            <div className="divide-y divide-gray-200">
              {gl.accounts.map((account: any) => (
                <div key={account.accountId} className="bg-white">
                  <div
                    className="px-6 py-3 bg-blue-50 flex items-center justify-between cursor-pointer hover:bg-blue-100"
                    onClick={() => toggleAccountExpand(account.accountId)}
                  >
                    <div className="flex items-center">
                      <span className="mr-2">{expandedAccounts.has(account.accountId) ? '▼' : '▶'}</span>
                      <span className="font-mono font-semibold text-blue-800">{account.accountCode}</span>
                      <span className="ml-2 text-gray-700">{account.accountName}</span>
                    </div>
                    <div className="text-sm text-gray-600">
                      Начално: <span className="font-mono">{formatAmount(account.openingBalance)}</span> |
                      Крайно: <span className="font-mono font-semibold">{formatAmount(account.closingBalance)}</span>
                    </div>
                  </div>
                  {expandedAccounts.has(account.accountId) && (
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Дата</th>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Документ</th>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Описание</th>
                          <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Дебит</th>
                          <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Кредит</th>
                          <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">Салдо</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {account.entries.map((entry: any, idx: number) => (
                          <tr key={idx} className="hover:bg-gray-50">
                            <td className="px-4 py-2 text-sm">{entry.date}</td>
                            <td className="px-4 py-2 text-sm">{entry.documentNumber || entry.entryNumber}</td>
                            <td className="px-4 py-2 text-sm text-gray-600">{entry.description}</td>
                            <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.debitAmount)}</td>
                            <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.creditAmount)}</td>
                            <td className="px-4 py-2 text-sm text-right font-mono font-semibold">{formatAmount(entry.balance)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              ))}
            </div>
          </div>
        );

      case 'transactionLog':
        if (!logData?.transactionLog) return null;
        const tl = logData.transactionLog;
        return (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
              <h3 className="text-lg font-semibold text-gray-900">Дневник на операциите</h3>
              <p className="text-sm text-gray-500">
                {tl.companyName} | Период: {tl.periodStart} - {tl.periodEnd}
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Дата</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">No</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Документ</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Сметка</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Описание</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Дебит</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Кредит</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {tl.entries.map((entry: any, idx: number) => (
                    <tr key={idx} className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm">{entry.date}</td>
                      <td className="px-4 py-2 text-sm font-mono">{entry.entryNumber}</td>
                      <td className="px-4 py-2 text-sm">{entry.documentNumber}</td>
                      <td className="px-4 py-2 text-sm">
                        <span className="font-mono font-medium">{entry.accountCode}</span>
                        <span className="ml-1 text-gray-500 text-xs">{entry.accountName}</span>
                      </td>
                      <td className="px-4 py-2 text-sm text-gray-600">{entry.description}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.debitAmount)}</td>
                      <td className="px-4 py-2 text-sm text-right font-mono">{formatAmount(entry.creditAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  const hasReportData = useMemo(() => {
    switch (selectedReport) {
      case 'turnover': return !!turnoverData?.turnoverSheet;
      case 'chronological': return !!chronoData?.chronologicalReport;
      case 'generalLedger': return !!ledgerData?.generalLedger;
      case 'transactionLog': return !!logData?.transactionLog;
      default: return false;
    }
  }, [selectedReport, turnoverData, chronoData, ledgerData, logData]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Отчети</h1>
          <p className="mt-1 text-sm text-gray-500">
            Генериране на счетоводни справки и отчети
          </p>
        </div>
      </div>

      {/* Report Selection */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {REPORT_TYPES.map((report) => (
          <button
            key={report.id}
            onClick={() => setSelectedReport(report.id)}
            className={`p-4 rounded-lg border-2 text-left transition-all ${
              selectedReport === report.id
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 bg-white hover:border-gray-300'
            }`}
          >
            <div className="text-2xl mb-2">{report.icon}</div>
            <h3 className="font-semibold text-gray-900">{report.name}</h3>
            <p className="text-sm text-gray-500 mt-1">{report.description}</p>
          </button>
        ))}
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Параметри на отчета</h3>

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
                <option key={preset.id} value={preset.id}>{preset.name}</option>
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
            <label className="block text-sm font-medium text-gray-700 mb-1">Сметка (опционално)</label>
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

        {/* Additional options for Turnover Sheet */}
        {selectedReport === 'turnover' && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="flex items-center gap-6">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={showZeroBalances}
                  onChange={(e) => setShowZeroBalances(e.target.checked)}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="ml-2 text-sm text-gray-700">Показвай нулеви салда</span>
              </label>

              <div className="flex items-center gap-2">
                <label className="text-sm text-gray-700">Ниво на агрегация:</label>
                <select
                  value={accountCodeDepth || ''}
                  onChange={(e) => setAccountCodeDepth(e.target.value ? parseInt(e.target.value) : null)}
                  className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                >
                  <option value="">Всички нива</option>
                  <option value="1">1 символ</option>
                  <option value="2">2 символа</option>
                  <option value="3">3 символа</option>
                  <option value="4">4 символа</option>
                </select>
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="mt-6 flex items-center gap-3">
          <button
            onClick={handleGenerateReport}
            disabled={isLoading}
            className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 font-medium"
          >
            {isLoading ? 'Зареждане...' : 'Генерирай отчет'}
          </button>

          {hasReportData && (
            <>
              <span className="text-gray-400">|</span>
              <span className="text-sm text-gray-500">Експорт:</span>
              <button
                onClick={() => handleExport('pdf')}
                className="px-3 py-1.5 border border-gray-300 rounded-md text-sm hover:bg-gray-50"
              >
                PDF
              </button>
              <button
                onClick={() => handleExport('xlsx')}
                className="px-3 py-1.5 border border-gray-300 rounded-md text-sm hover:bg-gray-50"
              >
                Excel
              </button>
            </>
          )}
        </div>
      </div>

      {/* Report Content */}
      {isLoading && (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
        </div>
      )}

      {!isLoading && renderReportContent()}

      {!isLoading && !hasReportData && (
        <div className="bg-white rounded-lg shadow p-12 text-center text-gray-500">
          <div className="text-4xl mb-4">📊</div>
          <p>Изберете параметри и натиснете "Генерирай отчет"</p>
        </div>
      )}
    </div>
  );
}
