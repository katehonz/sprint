import { useState } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { useCompany } from '../contexts/CompanyContext';
import {
  GET_ACCOUNTING_PERIODS,
  CLOSE_ACCOUNTING_PERIOD,
  REOPEN_ACCOUNTING_PERIOD,
} from '../graphql/queries';
import type { AccountingPeriod, PeriodStatus } from '../types';

interface AccountingPeriodsData {
  accountingPeriods: AccountingPeriod[];
}

export default function AccountingPeriods() {
  const { companyId } = useCompany();
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [pendingAction, setPendingAction] = useState<{ year: number; month: number; action: 'close' | 'reopen' } | null>(null);

  const { data, loading, refetch } = useQuery<AccountingPeriodsData>(GET_ACCOUNTING_PERIODS, {
    variables: { companyId },
    skip: !companyId,
  });

  const [closePeriod, { loading: closing }] = useMutation(CLOSE_ACCOUNTING_PERIOD);
  const [reopenPeriod, { loading: reopening }] = useMutation(REOPEN_ACCOUNTING_PERIOD);

  const periods = data?.accountingPeriods || [];
  const monthNames = ['Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни', 'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

  const getPeriodStatus = (year: number, month: number): PeriodStatus => {
    const found = periods.find(p => p.year === year && p.month === month);
    return found?.status || 'OPEN';
  };

  const getPeriod = (year: number, month: number): AccountingPeriod | undefined => {
    return periods.find(p => p.year === year && p.month === month);
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('bg-BG', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleAction = (year: number, month: number, action: 'close' | 'reopen') => {
    setPendingAction({ year, month, action });
    setShowConfirmModal(true);
  };

  const confirmAction = async () => {
    if (!pendingAction || !companyId) return;

    try {
      const input = {
        companyId: parseInt(companyId),
        year: pendingAction.year,
        month: pendingAction.month,
      };

      if (pendingAction.action === 'close') {
        await closePeriod({ variables: { input } });
      } else {
        await reopenPeriod({ variables: { input } });
      }

      refetch();
      setShowConfirmModal(false);
      setPendingAction(null);
    } catch (err: unknown) {
      // Extract GraphQL error message
      let message = 'Грешка при операцията';
      if (err && typeof err === 'object' && 'graphQLErrors' in err) {
        const gqlErr = err as { graphQLErrors?: Array<{ message?: string }> };
        if (gqlErr.graphQLErrors && gqlErr.graphQLErrors.length > 0) {
          message = gqlErr.graphQLErrors[0].message || message;
        }
      } else if (err instanceof Error) {
        message = err.message;
      }
      alert(message);
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
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">Счетоводни периоди</h1>
            <p className="text-gray-500 mt-1">Заключване и отключване на счетоводни периоди</p>
          </div>
          <div className="flex items-center space-x-3">
            <span className="text-gray-600">Година:</span>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(parseInt(e.target.value))}
              className="border border-gray-300 rounded px-3 py-2"
            >
              {[...Array(5)].map((_, i) => {
                const year = new Date().getFullYear() - i;
                return <option key={year} value={year}>{year}</option>;
              })}
            </select>
          </div>
        </div>
      </div>

      {/* Info Box */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start">
          <svg className="h-5 w-5 text-blue-500 mt-0.5 mr-3" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="text-sm text-blue-800">
            <p className="font-medium mb-1">Какво означава заключване на период?</p>
            <p>След като приключите (заключите) даден счетоводен период, няма да можете да създавате, редактирате или изтривате счетоводни записи с дата в този период. Това е полезно след подаване на ДДС декларация или финансов отчет.</p>
          </div>
        </div>
      </div>

      {/* Periods Grid */}
      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        <div className="grid grid-cols-4 gap-px bg-gray-200">
          {monthNames.map((monthName, index) => {
            const month = index + 1;
            const status = getPeriodStatus(selectedYear, month);
            const period = getPeriod(selectedYear, month);
            const isClosed = status === 'CLOSED';

            return (
              <div
                key={month}
                className={`bg-white p-4 ${isClosed ? 'bg-red-50' : 'bg-green-50'}`}
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="font-medium text-gray-900">{monthName}</span>
                  <span className={`text-xs px-2 py-1 rounded-full ${
                    isClosed
                      ? 'bg-red-100 text-red-700'
                      : 'bg-green-100 text-green-700'
                  }`}>
                    {isClosed ? 'Приключен' : 'Отворен'}
                  </span>
                </div>

                {isClosed && period && (
                  <div className="text-xs text-gray-500 mb-3">
                    <p>Приключен на: {formatDate(period.closedAt)}</p>
                    {period.closedBy && (
                      <p>От: {period.closedBy.firstName} {period.closedBy.lastName}</p>
                    )}
                  </div>
                )}

                <button
                  onClick={() => handleAction(selectedYear, month, isClosed ? 'reopen' : 'close')}
                  className={`w-full text-sm px-3 py-2 rounded transition-colors ${
                    isClosed
                      ? 'border border-green-500 text-green-600 hover:bg-green-50'
                      : 'border border-red-500 text-red-600 hover:bg-red-50'
                  }`}
                >
                  {isClosed ? 'Отвори периода' : 'Приключи периода'}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center space-x-6 text-sm text-gray-600">
        <div className="flex items-center">
          <span className="w-4 h-4 bg-green-100 rounded mr-2"></span>
          <span>Отворен период - може да се редактира</span>
        </div>
        <div className="flex items-center">
          <span className="w-4 h-4 bg-red-100 rounded mr-2"></span>
          <span>Приключен период - заключен за редакция</span>
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && pendingAction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-md shadow-2xl">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-xl font-semibold">
                {pendingAction.action === 'close' ? 'Приключване на период' : 'Отваряне на период'}
              </h2>
            </div>
            <div className="p-6">
              <p className="text-gray-600 mb-4">
                {pendingAction.action === 'close' ? (
                  <>
                    Сигурни ли сте, че искате да приключите период{' '}
                    <span className="font-semibold">{monthNames[pendingAction.month - 1]} {pendingAction.year}</span>?
                    <br /><br />
                    След приключването няма да можете да създавате, редактирате или изтривате счетоводни записи за този период.
                  </>
                ) : (
                  <>
                    Сигурни ли сте, че искате да отворите отново период{' '}
                    <span className="font-semibold">{monthNames[pendingAction.month - 1]} {pendingAction.year}</span>?
                    <br /><br />
                    Това ще позволи редактирането на записи за този период.
                  </>
                )}
              </p>
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => {
                    setShowConfirmModal(false);
                    setPendingAction(null);
                  }}
                  className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                >
                  Отказ
                </button>
                <button
                  onClick={confirmAction}
                  disabled={closing || reopening}
                  className={`px-4 py-2 rounded text-white ${
                    pendingAction.action === 'close'
                      ? 'bg-red-600 hover:bg-red-700'
                      : 'bg-green-600 hover:bg-green-700'
                  } disabled:opacity-50`}
                >
                  {closing || reopening ? 'Изчакайте...' : 'Потвърди'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
