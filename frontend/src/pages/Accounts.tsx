import { useState, useRef } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_ACCOUNTS, CREATE_ACCOUNT, DELETE_ACCOUNT, UPDATE_ACCOUNT } from '../graphql/queries';
import { useCompany } from '../contexts/CompanyContext';

interface Account {
  id: string;
  code: string;
  name: string;
  description: string | null;
  accountType: string;
  accountClass: number;
  level: number;
  isAnalytical: boolean;
  isActive: boolean;
  supportsQuantities: boolean;
  defaultUnit: string | null;
  parent?: { id: string; code: string };
  children?: Account[];
}

const UNITS_OF_MEASURE = [
  { code: 'PCS', name: 'Брой (бр.)' },
  { code: 'KG', name: 'Килограм (кг)' },
  { code: 'L', name: 'Литър (л)' },
  { code: 'M', name: 'Метър (м)' },
  { code: 'M2', name: 'Квадратен метър (м²)' },
  { code: 'M3', name: 'Кубичен метър (м³)' },
  { code: 'T', name: 'Тон (т)' },
  { code: 'HR', name: 'Час (ч)' },
];

export default function Accounts() {
  const { companyId } = useCompany();
  const [showModal, setShowModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [expandedAccounts, setExpandedAccounts] = useState<Set<string>>(new Set());
  const [selectedParent, setSelectedParent] = useState<Account | null>(null);
  const [editingAccount, setEditingAccount] = useState<Account | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    accountType: 'ASSET',
    accountClass: 1,
    parentId: '',
    isAnalytical: true,
    supportsQuantities: false,
    defaultUnit: '',
  });
  const [error, setError] = useState('');
  const [importStatus, setImportStatus] = useState<{ success?: boolean; message?: string } | null>(null);
  const [importing, setImporting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data, loading, error: queryError, refetch } = useQuery<any>(GET_ACCOUNTS, {
    variables: { companyId: companyId || '1' },
    skip: !companyId,
  });

  const [createAccount, { loading: creating }] = useMutation<any>(CREATE_ACCOUNT);
  const [updateAccount, { loading: updating }] = useMutation<any>(UPDATE_ACCOUNT);
  const [deleteAccount] = useMutation<any>(DELETE_ACCOUNT);

  const accounts: Account[] = data?.accounts || [];

  // Build tree structure
  const buildTree = (items: Account[], parentId?: string): Account[] => {
    return items
      .filter(item => (item.parent?.id || undefined) === parentId)
      .map(item => ({
        ...item,
        children: buildTree(items, item.id),
      }))
      .sort((a, b) => a.code.localeCompare(b.code));
  };

  const accountTree = buildTree(accounts, undefined);

  // Filter accounts based on search query
  const filterAccounts = (items: Account[], query: string): Account[] => {
    if (!query.trim()) return items;
    const lowerQuery = query.toLowerCase();

    const matchesSearch = (account: Account): boolean => {
      return account.code.toLowerCase().includes(lowerQuery) ||
             account.name.toLowerCase().includes(lowerQuery) ||
             (account.description?.toLowerCase().includes(lowerQuery) ?? false);
    };

    const filterTree = (accounts: Account[]): Account[] => {
      const result: Account[] = [];
      for (const account of accounts) {
        const filteredChildren = account.children ? filterTree(account.children) : [];
        const hasMatchingChildren = filteredChildren.length > 0;
        const selfMatches = matchesSearch(account);

        if (selfMatches || hasMatchingChildren) {
          result.push({
            ...account,
            children: hasMatchingChildren ? filteredChildren : account.children,
          });
        }
      }
      return result;
    };

    return filterTree(items);
  };

  const filteredTree = filterAccounts(accountTree, searchQuery);

  // Get all account IDs that should be expanded when searching
  const getExpandedIdsForSearch = (items: Account[]): Set<string> => {
    const ids = new Set<string>();
    const collectIds = (accounts: Account[]) => {
      accounts.forEach(account => {
        if (account.children && account.children.length > 0) {
          ids.add(account.id);
          collectIds(account.children);
        }
      });
    };
    collectIds(items);
    return ids;
  };

  // Auto-expand all when searching
  const effectiveExpandedAccounts = searchQuery.trim()
    ? getExpandedIdsForSearch(filteredTree)
    : expandedAccounts;

  const toggleExpand = (id: string) => {
    setExpandedAccounts(prev => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const openAddModal = (parent?: Account) => {
    setEditingAccount(null);
    setSelectedParent(parent || null);
    const parentCode = parent?.code || '';
    setFormData({
      code: parentCode ? parentCode + '.' : '',
      name: '',
      description: '',
      accountType: parent?.accountType || 'ASSET',
      accountClass: parent?.accountClass || 1,
      parentId: parent?.id || '',
      isAnalytical: true,
      supportsQuantities: false,
      defaultUnit: '',
    });
    setShowModal(true);
  };

  const openEditModal = (account: Account) => {
    setEditingAccount(account);
    setSelectedParent(null);
    setFormData({
      code: account.code,
      name: account.name,
      description: account.description || '',
      accountType: account.accountType,
      accountClass: account.accountClass,
      parentId: account.parent?.id || '',
      isAnalytical: account.isAnalytical,
      supportsQuantities: account.supportsQuantities,
      defaultUnit: account.defaultUnit || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.code || !formData.name) {
      setError('Кодът и името са задължителни');
      return;
    }

    if (formData.supportsQuantities && !formData.defaultUnit) {
      setError('Моля изберете мерна единица за материалната сметка');
      return;
    }

    try {
      if (editingAccount) {
        // Update existing account
        await updateAccount({
          variables: {
            id: editingAccount.id,
            input: {
              code: formData.code,
              name: formData.name,
              description: formData.description || null,
              accountType: formData.accountType,
              parentId: formData.parentId || null,
              supportsQuantities: formData.supportsQuantities,
              defaultUnit: formData.supportsQuantities ? formData.defaultUnit : null,
            },
          },
        });
      } else {
        // Create new account
        await createAccount({
          variables: {
            input: {
              companyId: companyId,
              code: formData.code,
              name: formData.name,
              description: formData.description || null,
              accountType: formData.accountType,
              accountClass: formData.accountClass,
              parentId: formData.parentId || null,
              supportsQuantities: formData.supportsQuantities,
              defaultUnit: formData.supportsQuantities ? formData.defaultUnit : null,
            },
          },
        });
      }
      setShowModal(false);
      setFormData({ code: '', name: '', description: '', accountType: 'ASSET', accountClass: 1, parentId: '', isAnalytical: true, supportsQuantities: false, defaultUnit: '' });
      setSelectedParent(null);
      setEditingAccount(null);
      refetch();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : editingAccount ? 'Грешка при редактиране на сметка' : 'Грешка при създаване на сметка');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да изтриете тази сметка?')) return;

    try {
      await deleteAccount({ variables: { id } });
      refetch();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
  };

  const getTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      ASSET: 'Актив',
      LIABILITY: 'Пасив',
      EQUITY: 'Капитал',
      REVENUE: 'Приход',
      EXPENSE: 'Разход',
    };
    return labels[type] || type;
  };

  const getTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      ASSET: 'bg-blue-100 text-blue-700',
      LIABILITY: 'bg-red-100 text-red-700',
      EQUITY: 'bg-purple-100 text-purple-700',
      REVENUE: 'bg-green-100 text-green-700',
      EXPENSE: 'bg-orange-100 text-orange-700',
    };
    return colors[type] || 'bg-gray-100 text-gray-700';
  };

  const handleExport = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/chart-of-accounts/export/${companyId}`, {
        headers: {
          'Authorization': token ? `Bearer ${token}` : '',
        },
      });
      if (!response.ok) throw new Error('Грешка при експорт');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `chart-of-accounts-${new Date().toISOString().slice(0, 10)}.json`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при експорт');
    }
  };

  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setImporting(true);
    setImportStatus(null);

    try {
      const formData = new FormData();
      formData.append('file', file);
      const token = localStorage.getItem('token');

      const response = await fetch(`/api/chart-of-accounts/import/${companyId}?replaceExisting=false`, {
        method: 'POST',
        headers: {
          'Authorization': token ? `Bearer ${token}` : '',
        },
        body: formData,
      });

      const result = await response.json();

      if (result.success) {
        setImportStatus({ success: true, message: result.message });
        refetch();
      } else {
        setImportStatus({ success: false, message: result.error || 'Грешка при импорт' });
      }
    } catch (err) {
      setImportStatus({ success: false, message: err instanceof Error ? err.message : 'Грешка при импорт' });
    } finally {
      setImporting(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const renderAccountRow = (account: Account, depth: number = 0) => {
    const hasChildren = account.children && account.children.length > 0;
    const isExpanded = effectiveExpandedAccounts.has(account.id);

    return (
      <div key={account.id}>
        <div
          className={`flex items-center py-2 px-4 border-b border-gray-100 hover:bg-gray-50 ${
            depth > 0 ? 'bg-gray-25' : ''
          }`}
          style={{ paddingLeft: `${16 + depth * 24}px` }}
        >
          <div className="flex items-center flex-1 min-w-0">
            {hasChildren ? (
              <button
                onClick={() => toggleExpand(account.id)}
                className="w-5 h-5 mr-2 text-gray-400 hover:text-gray-600 flex-shrink-0"
              >
                {isExpanded ? '▼' : '▶'}
              </button>
            ) : (
              <span className="w-5 h-5 mr-2 flex-shrink-0" />
            )}
            <span className="font-mono text-sm font-medium text-gray-700 w-28 flex-shrink-0">
              {account.code}
            </span>
            <span className="text-sm text-gray-900 flex-1 truncate">
              {account.name}
              {account.supportsQuantities && (
                <span className="ml-2 text-xs text-amber-600 font-medium">
                  [{account.defaultUnit || 'мат.'}]
                </span>
              )}
              {account.description && (
                <span className="ml-2 text-xs text-gray-500 italic">
                  — {account.description}
                </span>
              )}
            </span>
          </div>
          <div className="flex items-center gap-2 flex-shrink-0">
            <span className={`px-2 py-0.5 text-xs rounded-full ${getTypeColor(account.accountType)}`}>
              {getTypeLabel(account.accountType)}
            </span>
            {account.isAnalytical && (
              <span className="px-2 py-0.5 text-xs rounded-full bg-cyan-100 text-cyan-700">
                Анал.
              </span>
            )}
            <button
              onClick={() => openEditModal(account)}
              className="text-blue-600 hover:text-blue-800 text-sm px-2"
              title="Редактирай"
            >
              ✎
            </button>
            <button
              onClick={() => openAddModal(account)}
              className="text-green-600 hover:text-green-800 text-sm px-2"
              title="Добави подсметка"
            >
              +
            </button>
            <button
              onClick={() => handleDelete(account.id)}
              className="text-red-500 hover:text-red-700 text-sm px-2"
              title="Изтрий"
            >
              ✕
            </button>
          </div>
        </div>
        {hasChildren && isExpanded && (
          <div>
            {account.children!.map(child => renderAccountRow(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  if (!companyId) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center text-gray-500">
          <div className="text-4xl mb-4">🏢</div>
          <p>Моля изберете компания от горното меню</p>
        </div>
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

  if (queryError) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center text-red-500">
          <div className="text-4xl mb-4">⚠️</div>
          <p className="font-medium">Грешка при зареждане на сметкоплана</p>
          <p className="text-sm mt-2">{queryError.message}</p>
          <button
            onClick={() => refetch()}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Опитай отново
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Сметкоплан</h1>
          <p className="mt-1 text-sm text-gray-500">
            Йерархична структура на счетоводните сметки
          </p>
        </div>
        <div className="flex gap-2">
          {/* Search Input */}
          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Търси по код или име..."
              className="w-64 pl-10 pr-4 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
            />
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            )}
          </div>
          <button
            onClick={() => setExpandedAccounts(new Set(accounts.map(a => a.id)))}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            Разгъни
          </button>
          <button
            onClick={() => setExpandedAccounts(new Set())}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            Свий
          </button>
          <button
            onClick={() => openAddModal()}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
          >
            + Нова сметка
          </button>
          <button
            onClick={handleExport}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            Експорт
          </button>
          <button
            onClick={() => setShowImportModal(true)}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            Импорт
          </button>
        </div>
      </div>

      {/* Account Tree */}
      <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
        <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
          <div className="flex items-center text-xs font-medium text-gray-500 uppercase tracking-wider">
            <span className="w-5 mr-2" />
            <span className="w-28">Код</span>
            <span className="flex-1">Наименование</span>
            <span className="w-32 text-center">Тип</span>
            <span className="w-24 text-right">Действия</span>
          </div>
        </div>
        <div>
          {filteredTree.length === 0 ? (
            <div className="px-6 py-12 text-center text-gray-500">
              {searchQuery ? (
                <>
                  Няма намерени сметки за "<span className="font-medium">{searchQuery}</span>"
                </>
              ) : (
                'Няма създадени сметки'
              )}
            </div>
          ) : (
            <>
              {searchQuery && (
                <div className="px-4 py-2 bg-blue-50 border-b border-blue-100 text-sm text-blue-700">
                  Намерени {filteredTree.reduce((acc, a) => acc + 1 + (a.children?.length || 0), 0)} сметки за "{searchQuery}"
                </div>
              )}
              {filteredTree.map(account => renderAccountRow(account))}
            </>
          )}
        </div>
      </div>

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-lg shadow-2xl">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">
                  {editingAccount
                    ? `Редактиране на ${editingAccount.code}`
                    : selectedParent
                      ? `Нова подсметка на ${selectedParent.code}`
                      : 'Нова сметка'}
                </h2>
                <button
                  onClick={() => { setShowModal(false); setSelectedParent(null); setEditingAccount(null); }}
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

              {selectedParent && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm">
                  <span className="text-blue-700">Родителска сметка: </span>
                  <span className="font-medium text-blue-900">{selectedParent.code} - {selectedParent.name}</span>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Код *
                  </label>
                  <input
                    type="text"
                    value={formData.code}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="401.1"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Клас
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="9"
                    value={formData.accountClass}
                    onChange={(e) => setFormData({ ...formData, accountClass: parseInt(e.target.value) || 1 })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Наименование *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  placeholder="Доставчик АБВ"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Описание
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  placeholder="Допълнителна информация за сметката..."
                  rows={2}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Тип
                </label>
                <select
                  value={formData.accountType}
                  onChange={(e) => setFormData({ ...formData, accountType: e.target.value })}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  disabled={!!selectedParent}
                >
                  <option value="ASSET">Актив</option>
                  <option value="LIABILITY">Пасив</option>
                  <option value="EQUITY">Капитал</option>
                  <option value="REVENUE">Приход</option>
                  <option value="EXPENSE">Разход</option>
                </select>
                {selectedParent && (
                  <p className="mt-1 text-xs text-gray-500">Типът се наследява от родителската сметка</p>
                )}
              </div>

              {!selectedParent && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Родителска сметка
                  </label>
                  <select
                    value={formData.parentId}
                    onChange={(e) => setFormData({ ...formData, parentId: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  >
                    <option value="">Без родител (основна сметка)</option>
                    {accounts.map(acc => (
                      <option key={acc.id} value={acc.id}>
                        {acc.code} - {acc.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div className="pt-4 border-t border-gray-200">
                <h4 className="text-sm font-medium text-gray-900 mb-3">Материална сметка (с количества)</h4>

                <div className="flex items-center mb-3">
                  <input
                    type="checkbox"
                    id="supportsQuantities"
                    checked={formData.supportsQuantities}
                    onChange={(e) => setFormData({ ...formData, supportsQuantities: e.target.checked, defaultUnit: e.target.checked ? 'PCS' : '' })}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="supportsQuantities" className="ml-2 text-sm text-gray-700">
                    Поддържа количества (материална сметка)
                  </label>
                </div>

                {formData.supportsQuantities && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Мерна единица *
                    </label>
                    <select
                      value={formData.defaultUnit}
                      onChange={(e) => setFormData({ ...formData, defaultUnit: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    >
                      <option value="">Изберете мерна единица...</option>
                      {UNITS_OF_MEASURE.map(unit => (
                        <option key={unit.code} value={unit.code}>
                          {unit.name}
                        </option>
                      ))}
                    </select>
                    <p className="mt-1 text-xs text-gray-500">
                      За сметки като стоки, материали, продукция и др.
                    </p>
                  </div>
                )}
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
                <button
                  type="button"
                  onClick={() => { setShowModal(false); setSelectedParent(null); setEditingAccount(null); }}
                  className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                >
                  Отказ
                </button>
                <button
                  type="submit"
                  disabled={creating || updating}
                  className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
                >
                  {creating || updating
                    ? (editingAccount ? 'Запазване...' : 'Създаване...')
                    : (editingAccount ? 'Запази промените' : 'Създай сметка')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Import Modal */}
      {showImportModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-lg shadow-2xl">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">Импорт на сметкоплан</h2>
                <button
                  onClick={() => {
                    setShowImportModal(false);
                    setImportStatus(null);
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>
            </div>

            <div className="p-6 space-y-4">
              {importStatus && (
                <div className={`rounded-lg p-4 text-sm ${
                  importStatus.success
                    ? 'bg-green-50 border border-green-200 text-green-700'
                    : 'bg-red-50 border border-red-200 text-red-700'
                }`}>
                  {importStatus.message}
                </div>
              )}

              <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".json"
                  onChange={handleImport}
                  className="hidden"
                  id="chart-import-file"
                />
                <label
                  htmlFor="chart-import-file"
                  className="cursor-pointer"
                >
                  <div className="text-gray-400 text-4xl mb-2">📄</div>
                  <p className="text-sm text-gray-600 mb-2">
                    {importing ? 'Импортиране...' : 'Изберете JSON файл със сметкоплан'}
                  </p>
                  <p className="text-xs text-gray-400">
                    Поддържан формат: JSON експортиран от системата
                  </p>
                </label>
              </div>

              <div className="text-xs text-gray-500">
                <p className="font-medium mb-1">Забележка:</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>Съществуващи сметки със същия код няма да бъдат презаписани</li>
                  <li>Нови сметки ще бъдат добавени към съществуващия сметкоплан</li>
                  <li>Йерархията на сметките ще бъде възстановена автоматично</li>
                </ul>
              </div>

              <div className="flex justify-end pt-4 border-t border-gray-200">
                <button
                  onClick={() => {
                    setShowImportModal(false);
                    setImportStatus(null);
                  }}
                  className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                >
                  Затвори
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
