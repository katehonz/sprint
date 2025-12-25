import { useState, useEffect, useCallback, useRef } from 'react';
import { useQuery } from '@apollo/client/react';
import { GET_AUDIT_LOGS, GET_AUDIT_LOG_STATS } from '../graphql/queries';
import { useCompany } from '../contexts/CompanyContext';

interface AuditLog {
  id: string;
  companyId?: number;
  userId?: number;
  username?: string;
  userRole?: string;
  action: string;
  entityType?: string;
  entityId?: string;
  details?: string;
  ipAddress?: string;
  userAgent?: string;
  success: boolean;
  errorMessage?: string;
  createdAt: string;
}

interface AuditLogsPageData {
  auditLogs: {
    logs: AuditLog[];
    totalCount: number;
    hasMore: boolean;
  };
}

interface AuditLogStat {
  action: string;
  count: number;
}

interface AuditLogStatsData {
  auditLogStats: AuditLogStat[];
}

const PAGE_SIZE = 50;

const ACTION_LABELS: Record<string, string> = {
  LOGIN: 'Вход',
  LOGOUT: 'Изход',
  VIEW_REPORT: 'Преглед справка',
  GENERATE_REPORT: 'Генериране справка',
  EXPORT: 'Експорт',
  CREATE: 'Създаване',
  UPDATE: 'Редакция',
  DELETE: 'Изтриване',
};

const ACTION_COLORS: Record<string, string> = {
  LOGIN: 'bg-green-100 text-green-800',
  LOGOUT: 'bg-gray-100 text-gray-800',
  VIEW_REPORT: 'bg-blue-100 text-blue-800',
  GENERATE_REPORT: 'bg-indigo-100 text-indigo-800',
  EXPORT: 'bg-purple-100 text-purple-800',
  CREATE: 'bg-emerald-100 text-emerald-800',
  UPDATE: 'bg-yellow-100 text-yellow-800',
  DELETE: 'bg-red-100 text-red-800',
};

export default function AuditLogs() {
  const { companyId } = useCompany();
  const [actionFilter, setActionFilter] = useState<string>('');
  const [search, setSearch] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalCount, setTotalCount] = useState(0);
  const loadingRef = useRef<HTMLDivElement>(null);
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Build filter object
  const buildFilter = useCallback((currentOffset: number) => {
    const filter: Record<string, unknown> = {
      offset: currentOffset,
      limit: PAGE_SIZE,
    };

    if (companyId) filter.companyId = companyId;
    if (fromDate) filter.fromDate = fromDate;
    if (toDate) filter.toDate = toDate;
    if (debouncedSearch) filter.search = debouncedSearch;
    if (actionFilter) filter.action = actionFilter;

    return filter;
  }, [companyId, fromDate, toDate, debouncedSearch, actionFilter]);

  const { data, loading, fetchMore, refetch } = useQuery<AuditLogsPageData>(GET_AUDIT_LOGS, {
    variables: { filter: buildFilter(0) },
    notifyOnNetworkStatusChange: true,
  });

  const { data: statsData } = useQuery<AuditLogStatsData>(GET_AUDIT_LOG_STATS, {
    variables: { companyId, days: 30 },
  });

  // Update state when data changes
  useEffect(() => {
    if (data?.auditLogs) {
      setLogs(data.auditLogs.logs);
      setTotalCount(data.auditLogs.totalCount);
      setHasMore(data.auditLogs.hasMore);
      setOffset(PAGE_SIZE);
    }
  }, [data]);

  // Reset and refetch when filters change
  useEffect(() => {
    setLogs([]);
    setOffset(0);
    setHasMore(true);
    refetch({ filter: buildFilter(0) });
  }, [companyId, fromDate, toDate, debouncedSearch, actionFilter, refetch, buildFilter]);

  // Load more logs (infinite scroll)
  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;

    const { data: moreData } = await fetchMore({
      variables: { filter: buildFilter(offset) },
    });

    if (moreData?.auditLogs) {
      setLogs(prev => [...prev, ...moreData.auditLogs.logs]);
      setHasMore(moreData.auditLogs.hasMore);
      setOffset(prev => prev + PAGE_SIZE);
    }
  }, [loading, hasMore, fetchMore, buildFilter, offset]);

  // Intersection Observer for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loading) {
          loadMore();
        }
      },
      { threshold: 0.1 }
    );

    if (loadingRef.current) {
      observer.observe(loadingRef.current);
    }

    return () => observer.disconnect();
  }, [hasMore, loading, loadMore]);

  const formatDateTime = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString('bg-BG', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const getActionBadge = (action: string, success: boolean) => {
    const baseColor = ACTION_COLORS[action] || 'bg-gray-100 text-gray-800';
    const color = success ? baseColor : 'bg-red-100 text-red-800';
    const label = ACTION_LABELS[action] || action;
    return (
      <span className={`px-2 py-1 text-xs rounded-full ${color}`}>
        {label} {!success && '✗'}
      </span>
    );
  };

  const parseDetails = (detailsStr: string | undefined): Record<string, unknown> => {
    if (!detailsStr) return {};
    try {
      return JSON.parse(detailsStr);
    } catch {
      return {};
    }
  };

  const clearFilters = () => {
    setSearch('');
    setFromDate('');
    setToDate('');
    setActionFilter('');
  };

  const stats = statsData?.auditLogStats || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Одит логове</h1>
          <p className="mt-1 text-sm text-gray-500">
            Проследяване на действията на потребителите
          </p>
        </div>
        <div className="text-sm text-gray-500">
          {totalCount > 0 ? `${totalCount.toLocaleString('bg-BG')} записа` : ''}
        </div>
      </div>

      {/* Statistics */}
      {stats.length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <h3 className="text-sm font-medium text-gray-700 mb-3">Последните 30 дни</h3>
          <div className="flex flex-wrap gap-2">
            {stats.map((stat) => (
              <button
                key={stat.action}
                onClick={() => setActionFilter(actionFilter === stat.action ? '' : stat.action)}
                className={`px-3 py-1.5 text-sm rounded-lg border transition-colors ${
                  actionFilter === stat.action
                    ? 'bg-blue-50 border-blue-300 text-blue-700'
                    : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
                }`}
              >
                {ACTION_LABELS[stat.action] || stat.action}: {stat.count}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Search and Filters */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Търсене</label>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Потребител, действие..."
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>

          {/* From Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">От дата</label>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>

          {/* To Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">До дата</label>
            <input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        </div>

        {(search || fromDate || toDate || actionFilter) && (
          <div className="flex justify-end">
            <button
              onClick={clearFilters}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              Изчисти филтрите
            </button>
          </div>
        )}
      </div>

      {/* Logs Table */}
      <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Време
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Потребител
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Действие
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Детайли
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                IP адрес
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {logs.length === 0 && !loading ? (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                  Няма записи в одит лога
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr
                  key={log.id}
                  className={`hover:bg-gray-50 cursor-pointer ${!log.success ? 'bg-red-50' : ''}`}
                  onClick={() => setSelectedLog(log)}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatDateTime(log.createdAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{log.username || 'Анонимен'}</div>
                    <div className="text-xs text-gray-500">{log.userRole}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {getActionBadge(log.action, log.success)}
                    {log.entityType && (
                      <span className="ml-2 text-xs text-gray-500">{log.entityType}</span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-900 max-w-xs truncate">
                      {log.errorMessage || (log.entityId ? `ID: ${log.entityId}` : '-')}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {log.ipAddress || '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Loading indicator / Infinite scroll trigger */}
        <div ref={loadingRef} className="py-4 flex justify-center">
          {loading && (
            <div className="flex items-center gap-2 text-gray-500">
              <div className="animate-spin h-5 w-5 border-2 border-blue-500 border-t-transparent rounded-full" />
              <span>Зареждане...</span>
            </div>
          )}
          {!loading && hasMore && logs.length > 0 && (
            <span className="text-sm text-gray-400">Скролирай за още</span>
          )}
          {!loading && !hasMore && logs.length > 0 && (
            <span className="text-sm text-gray-400">Показани са всички {totalCount} записа</span>
          )}
        </div>
      </div>

      {/* Detail Modal */}
      {selectedLog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl">
            <div className="p-6 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-gray-900">Детайли на лога</h2>
              <button
                onClick={() => setSelectedLog(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-500">Време</label>
                  <div className="mt-1 text-sm text-gray-900">{formatDateTime(selectedLog.createdAt)}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">Статус</label>
                  <div className="mt-1">{getActionBadge(selectedLog.action, selectedLog.success)}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">Потребител</label>
                  <div className="mt-1 text-sm text-gray-900">{selectedLog.username || 'Анонимен'}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">Роля</label>
                  <div className="mt-1 text-sm text-gray-900">{selectedLog.userRole || '-'}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">Обект</label>
                  <div className="mt-1 text-sm text-gray-900">{selectedLog.entityType || '-'}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">ID на обект</label>
                  <div className="mt-1 text-sm text-gray-900">{selectedLog.entityId || '-'}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">IP адрес</label>
                  <div className="mt-1 text-sm text-gray-900">{selectedLog.ipAddress || '-'}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500">Компания ID</label>
                  <div className="mt-1 text-sm text-gray-900">{selectedLog.companyId || '-'}</div>
                </div>
              </div>

              {selectedLog.errorMessage && (
                <div>
                  <label className="block text-sm font-medium text-gray-500">Грешка</label>
                  <div className="mt-1 text-sm text-red-600 bg-red-50 p-3 rounded-md">
                    {selectedLog.errorMessage}
                  </div>
                </div>
              )}

              {selectedLog.details && (
                <div>
                  <label className="block text-sm font-medium text-gray-500">Детайли (JSON)</label>
                  <pre className="mt-1 text-xs text-gray-700 bg-gray-50 p-3 rounded-md overflow-x-auto">
                    {JSON.stringify(parseDetails(selectedLog.details), null, 2)}
                  </pre>
                </div>
              )}

              {selectedLog.userAgent && (
                <div>
                  <label className="block text-sm font-medium text-gray-500">User Agent</label>
                  <div className="mt-1 text-xs text-gray-600 break-all">{selectedLog.userAgent}</div>
                </div>
              )}
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200"
              >
                Затвори
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
