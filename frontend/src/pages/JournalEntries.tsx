import { useState, useEffect, useCallback, useRef } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import { GET_JOURNAL_ENTRIES_PAGED, POST_JOURNAL_ENTRY, UNPOST_JOURNAL_ENTRY, DELETE_JOURNAL_ENTRY } from '../graphql/queries';
import { useCompany } from '../contexts/CompanyContext';

interface JournalEntry {
  id: string;
  entryNumber: string;
  documentDate: string;
  accountingDate: string;
  documentNumber?: string;
  documentType?: string;
  description: string;
  totalAmount: number;
  isPosted: boolean;
  counterpartId?: string;
  createdAt: string;
}

interface JournalEntriesPageData {
  journalEntriesPaged: {
    entries: JournalEntry[];
    totalCount: number;
    hasMore: boolean;
  };
}

const PAGE_SIZE = 30;

// Check if entry is from VAT form (has documentType like '01', '02', etc.)
const isVatEntry = (entry: JournalEntry): boolean => {
  return !!entry.documentType && /^\d{2}$/.test(entry.documentType);
};

export default function JournalEntries() {
  const { companyId } = useCompany();
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'POSTED' | 'DRAFT'>('ALL');
  const [search, setSearch] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalCount, setTotalCount] = useState(0);
  const loadingRef = useRef<HTMLDivElement>(null);
  const [debouncedSearch, setDebouncedSearch] = useState('');

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
      companyId,
      offset: currentOffset,
      limit: PAGE_SIZE,
    };

    if (fromDate) filter.fromDate = fromDate;
    if (toDate) filter.toDate = toDate;
    if (debouncedSearch) filter.search = debouncedSearch;
    if (statusFilter === 'POSTED') filter.isPosted = true;
    if (statusFilter === 'DRAFT') filter.isPosted = false;

    return filter;
  }, [companyId, fromDate, toDate, debouncedSearch, statusFilter]);

  const { data, loading, fetchMore, refetch } = useQuery<JournalEntriesPageData>(GET_JOURNAL_ENTRIES_PAGED, {
    variables: { filter: buildFilter(0) },
    skip: !companyId,
    notifyOnNetworkStatusChange: true,
  });

  // Update state when data changes
  useEffect(() => {
    if (data?.journalEntriesPaged) {
      setEntries(data.journalEntriesPaged.entries);
      setTotalCount(data.journalEntriesPaged.totalCount);
      setHasMore(data.journalEntriesPaged.hasMore);
      setOffset(PAGE_SIZE);
    }
  }, [data]);

  // Reset and refetch when filters change
  useEffect(() => {
    if (companyId) {
      setEntries([]);
      setOffset(0);
      setHasMore(true);
      refetch({ filter: buildFilter(0) });
    }
  }, [companyId, fromDate, toDate, debouncedSearch, statusFilter, refetch, buildFilter]);

  const [postEntry] = useMutation(POST_JOURNAL_ENTRY);
  const [unpostEntry] = useMutation(UNPOST_JOURNAL_ENTRY);
  const [deleteEntry] = useMutation(DELETE_JOURNAL_ENTRY);

  // Load more entries (infinite scroll)
  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;

    const { data } = await fetchMore({
      variables: { filter: buildFilter(offset) },
    });

    if (data?.journalEntriesPaged) {
      setEntries(prev => [...prev, ...data.journalEntriesPaged.entries]);
      setHasMore(data.journalEntriesPaged.hasMore);
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

  const handlePost = async (id: string) => {
    try {
      await postEntry({ variables: { id } });
      // Update local state
      setEntries(prev => prev.map(e => e.id === id ? { ...e, isPosted: true } : e));
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при осчетоводяване');
    }
  };

  const handleUnpost = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да разосчетоводите този запис?')) return;
    try {
      await unpostEntry({ variables: { id } });
      setEntries(prev => prev.map(e => e.id === id ? { ...e, isPosted: false } : e));
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при разосчетоводяване');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да изтриете този запис?')) return;
    try {
      await deleteEntry({ variables: { id } });
      setEntries(prev => prev.filter(e => e.id !== id));
      setTotalCount(prev => prev - 1);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
  };

  const formatCurrency = (amount: number, currency = 'EUR') => {
    return new Intl.NumberFormat('bg-BG', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('bg-BG');
  };

  const getStatusBadge = (isPosted: boolean) => {
    return (
      <span className={`px-2 py-1 text-xs rounded-full ${isPosted ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>
        {isPosted ? 'Осчетоводен' : 'Чернова'}
      </span>
    );
  };

  const getEntryTypeDot = (entry: JournalEntry) => {
    const isVat = isVatEntry(entry);
    return (
      <span
        className={`inline-block w-2.5 h-2.5 rounded-full mr-2 ${isVat ? 'bg-yellow-400' : 'bg-green-400'}`}
        title={isVat ? 'ДДС документ' : 'Обикновен запис'}
      />
    );
  };

  const getEditUrl = (entry: JournalEntry) => {
    return isVatEntry(entry) ? `/vat/entry/${entry.id}` : `/journal-entry/${entry.id}`;
  };

  const clearFilters = () => {
    setSearch('');
    setFromDate('');
    setToDate('');
    setStatusFilter('ALL');
  };

  if (!companyId) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-gray-500">Моля, изберете фирма от менюто</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Журнални записи</h1>
          <p className="mt-1 text-sm text-gray-500">
            {totalCount > 0 ? `${totalCount.toLocaleString('bg-BG')} записа` : 'Зареждане...'}
          </p>
        </div>
        <Link
          to="/journal-entry/new"
          className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
        >
          + Нов запис
        </Link>
      </div>

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
              placeholder="Търси по описание, номер..."
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

        {/* Status Filters and Clear */}
        <div className="flex items-center justify-between">
          <div className="flex gap-2">
            {[
              { value: 'ALL', label: 'Всички' },
              { value: 'DRAFT', label: 'Чернови' },
              { value: 'POSTED', label: 'Осчетоводени' },
            ].map(({ value, label }) => (
              <button
                key={value}
                onClick={() => setStatusFilter(value as typeof statusFilter)}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  statusFilter === value
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {(search || fromDate || toDate || statusFilter !== 'ALL') && (
            <button
              onClick={clearFilters}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              Изчисти филтрите
            </button>
          )}
        </div>
      </div>

      {/* Entries Table */}
      <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Номер
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Дата
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Описание
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Сума
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
            {entries.length === 0 && !loading ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                  Няма журнални записи
                </td>
              </tr>
            ) : (
              entries.map((entry) => (
                <tr key={entry.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-blue-600 flex items-center">
                      {getEntryTypeDot(entry)}
                      <Link to={getEditUrl(entry)}>
                        {entry.entryNumber}
                      </Link>
                    </div>
                    {entry.documentNumber && (
                      <div className="text-xs text-gray-500 ml-4">
                        Док: {entry.documentNumber}
                      </div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {formatDate(entry.documentDate)}
                    </div>
                    <div className="text-xs text-gray-500">
                      Сч: {formatDate(entry.accountingDate)}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-900 max-w-xs truncate">
                      {entry.description}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right">
                    <span className="text-sm font-medium text-gray-900">
                      {formatCurrency(entry.totalAmount)}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-center">
                    {getStatusBadge(entry.isPosted)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    {!entry.isPosted ? (
                      <>
                        <button
                          onClick={() => handlePost(entry.id)}
                          className="text-green-600 hover:text-green-900 mr-3"
                        >
                          Осчетоводи
                        </button>
                        <Link
                          to={getEditUrl(entry)}
                          className="text-blue-600 hover:text-blue-900 mr-3"
                        >
                          Редактирай
                        </Link>
                        <button
                          onClick={() => handleDelete(entry.id)}
                          className="text-red-600 hover:text-red-900"
                        >
                          Изтрий
                        </button>
                      </>
                    ) : (
                      <>
                        <Link
                          to={getEditUrl(entry)}
                          className="text-blue-600 hover:text-blue-900 mr-3"
                        >
                          Преглед
                        </Link>
                        <button
                          onClick={() => handleUnpost(entry.id)}
                          className="text-orange-600 hover:text-orange-900"
                        >
                          Разосчетоводи
                        </button>
                      </>
                    )}
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
          {!loading && hasMore && entries.length > 0 && (
            <span className="text-sm text-gray-400">Скролирай за още</span>
          )}
          {!loading && !hasMore && entries.length > 0 && (
            <span className="text-sm text-gray-400">Показани са всички {totalCount} записа</span>
          )}
        </div>
      </div>

      {/* Legend */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-6">
            <span className="text-sm font-medium text-blue-900">
              Показани: {entries.length} от {totalCount}
            </span>
            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span className="flex items-center">
                <span className="inline-block w-2.5 h-2.5 rounded-full bg-green-400 mr-1.5" />
                Обикновен
              </span>
              <span className="flex items-center">
                <span className="inline-block w-2.5 h-2.5 rounded-full bg-yellow-400 mr-1.5" />
                ДДС документ
              </span>
            </div>
          </div>
          <span className="text-sm text-blue-700">
            Обща сума: {formatCurrency(entries.reduce((sum, e) => sum + e.totalAmount, 0))}
          </span>
        </div>
      </div>
    </div>
  );
}
