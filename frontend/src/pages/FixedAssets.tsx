import { useState } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_FIXED_ASSETS, GET_FIXED_ASSET_CATEGORIES, CREATE_FIXED_ASSET, DELETE_FIXED_ASSET } from '../graphql/queries';
import DepreciationCalculation from '../components/fixedAssets/DepreciationCalculation';
import DepreciationJournal from '../components/fixedAssets/DepreciationJournal';
import AssetCategories from '../components/fixedAssets/AssetCategories';

interface FixedAssetCategory {
  id: string;
  code: string;
  name: string;
  maxTaxDepreciationRate: number;
  defaultAccountingDepreciationRate: number;
}

interface FixedAsset {
  id: string;
  name: string;
  inventoryNumber: string;
  categoryId: number;
  acquisitionDate: string;
  acquisitionCost: number;
  documentNumber: string;
  documentDate: string;
  putIntoServiceDate: string;
  status: string;
  accountingAccumulatedDepreciation: number;
  accountingBookValue: number;
  taxAccumulatedDepreciation: number;
  taxBookValue: number;
}

type TabType = 'assets' | 'depreciation' | 'journal' | 'categories';

export default function FixedAssets() {
  const [activeTab, setActiveTab] = useState<TabType>('assets');
  const [showModal, setShowModal] = useState(false);
  const [filter, setFilter] = useState('ALL');
  const [formData, setFormData] = useState({
    name: '',
    inventoryNumber: '',
    categoryId: '',
    acquisitionDate: new Date().toISOString().split('T')[0],
    acquisitionCost: '',
    documentNumber: '',
    documentDate: new Date().toISOString().split('T')[0],
    putIntoServiceDate: new Date().toISOString().split('T')[0],
    residualValue: '0',
  });
  const [error, setError] = useState('');

  const { data, loading, refetch } = useQuery<any>(GET_FIXED_ASSETS, {
    variables: { companyId: '1' },
  });
  const { data: categoriesData } = useQuery<any>(GET_FIXED_ASSET_CATEGORIES, {
    variables: { companyId: '1' },
  });
  const [createFixedAsset, { loading: creating }] = useMutation<any>(CREATE_FIXED_ASSET);
  const [deleteFixedAsset] = useMutation<any>(DELETE_FIXED_ASSET);

  const assets: FixedAsset[] = data?.fixedAssets || [];
  const categories: FixedAssetCategory[] = categoriesData?.fixedAssetCategories || [];

  const filteredAssets = filter === 'ALL'
    ? assets
    : assets.filter(a => a.status === filter);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name || !formData.inventoryNumber || !formData.categoryId || !formData.acquisitionCost) {
      setError('Всички полета с * са задължителни');
      return;
    }

    try {
      await createFixedAsset({
        variables: {
          input: {
            companyId: '1',
            name: formData.name,
            inventoryNumber: formData.inventoryNumber,
            categoryId: formData.categoryId,
            acquisitionDate: formData.acquisitionDate,
            acquisitionCost: parseFloat(formData.acquisitionCost),
            documentNumber: formData.documentNumber || null,
            documentDate: formData.documentDate || null,
            putIntoServiceDate: formData.putIntoServiceDate || null,
          },
        },
      });
      setShowModal(false);
      setFormData({
        name: '',
        inventoryNumber: '',
        categoryId: '',
        acquisitionDate: new Date().toISOString().split('T')[0],
        acquisitionCost: '',
        documentNumber: '',
        documentDate: new Date().toISOString().split('T')[0],
        putIntoServiceDate: new Date().toISOString().split('T')[0],
        residualValue: '0',
      });
      refetch();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при създаване');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Сигурни ли сте, че искате да изтриете този актив?')) return;

    try {
      await deleteFixedAsset({ variables: { id } });
      refetch();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('bg-BG', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('bg-BG');
  };

  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-700',
      DEPRECIATED: 'bg-yellow-100 text-yellow-700',
      DISPOSED: 'bg-gray-100 text-gray-700',
      SOLD: 'bg-blue-100 text-blue-700',
    };
    const labels: Record<string, string> = {
      ACTIVE: 'Активен',
      DEPRECIATED: 'Напълно амортизиран',
      DISPOSED: 'Бракуван',
      SOLD: 'Продаден',
    };
    return (
      <span className={`px-2 py-1 text-xs rounded-full ${styles[status] || styles.ACTIVE}`}>
        {labels[status] || status}
      </span>
    );
  };

  const tabs = [
    { id: 'assets' as TabType, label: 'Регистър', icon: '📋' },
    { id: 'depreciation' as TabType, label: 'Амортизация', icon: '📉' },
    { id: 'journal' as TabType, label: 'Дневник', icon: '📖' },
    { id: 'categories' as TabType, label: 'Категории', icon: '📁' },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Дълготрайни активи</h1>
          <p className="mt-1 text-sm text-gray-500">
            Управление на ДМА с двойна амортизация (данъчна и счетоводна)
          </p>
        </div>
        {activeTab === 'assets' && (
          <button
            onClick={() => setShowModal(true)}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
          >
            + Нов актив
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2 ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <span>{tab.icon}</span>
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'assets' && (
        <>
          {/* Filters */}
          <div className="flex gap-2">
            {[
              { value: 'ALL', label: 'Всички' },
              { value: 'ACTIVE', label: 'Активни' },
              { value: 'DEPRECIATED', label: 'Амортизирани' },
              { value: 'DISPOSED', label: 'Бракувани' },
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

          {/* Assets Table */}
          {loading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
            </div>
          ) : (
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Актив</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Категория</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Първ. стойност</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Данъчна БС</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Счетоводна БС</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Статус</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Действия</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredAssets.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                        Няма дълготрайни активи
                      </td>
                    </tr>
                  ) : (
                    filteredAssets.map((asset) => (
                      <tr key={asset.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <div className="w-10 h-10 flex-shrink-0 bg-purple-100 text-purple-700 rounded-full flex items-center justify-center">
                              <span className="text-lg">🏭</span>
                            </div>
                            <div className="ml-4">
                              <div className="text-sm font-medium text-gray-900">{asset.name}</div>
                              <div className="text-xs text-gray-500">
                                Инв.№ {asset.inventoryNumber} | {formatDate(asset.acquisitionDate)}
                              </div>
                              {asset.documentNumber && (
                                <div className="text-xs text-gray-400">Док: {asset.documentNumber}</div>
                              )}
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900">
                            {categories.find(c => c.id === String(asset.categoryId))?.name || '-'}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right">
                          <span className="text-sm font-medium text-gray-900">
                            {formatCurrency(asset.acquisitionCost)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right">
                          <div className="text-sm font-medium text-gray-900">
                            {formatCurrency(asset.taxBookValue)}
                          </div>
                          <div className="text-xs text-gray-500">
                            Амор: {formatCurrency(asset.taxAccumulatedDepreciation)}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right">
                          <div className="text-sm font-medium text-gray-900">
                            {formatCurrency(asset.accountingBookValue)}
                          </div>
                          <div className="text-xs text-gray-500">
                            Амор: {formatCurrency(asset.accountingAccumulatedDepreciation)}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-center">
                          {getStatusBadge(asset.status)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <button
                            onClick={() => handleDelete(asset.id)}
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
          )}

          {/* Summary */}
          {assets.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                <h3 className="text-sm font-medium text-gray-500">Обща първоначална стойност</h3>
                <p className="mt-2 text-2xl font-bold text-gray-900">
                  {formatCurrency(assets.reduce((sum, a) => sum + a.acquisitionCost, 0))}
                </p>
              </div>
              <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                <h3 className="text-sm font-medium text-gray-500">Данъчна балансова стойност</h3>
                <p className="mt-2 text-2xl font-bold text-purple-600">
                  {formatCurrency(assets.reduce((sum, a) => sum + a.taxBookValue, 0))}
                </p>
              </div>
              <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                <h3 className="text-sm font-medium text-gray-500">Счетоводна балансова стойност</h3>
                <p className="mt-2 text-2xl font-bold text-blue-600">
                  {formatCurrency(assets.reduce((sum, a) => sum + a.accountingBookValue, 0))}
                </p>
              </div>
              <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                <h3 className="text-sm font-medium text-gray-500">Активни активи</h3>
                <p className="mt-2 text-2xl font-bold text-green-600">
                  {assets.filter(a => a.status === 'ACTIVE').length}
                  <span className="text-sm font-normal text-gray-500 ml-2">от {assets.length}</span>
                </p>
              </div>
            </div>
          )}
        </>
      )}

      {activeTab === 'depreciation' && (
        <DepreciationCalculation onRefreshAssets={refetch} />
      )}

      {activeTab === 'journal' && (
        <DepreciationJournal />
      )}

      {activeTab === 'categories' && (
        <AssetCategories />
      )}

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-200 sticky top-0 bg-white">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">Нов дълготраен актив</h2>
                <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600">
                  &#10005;
                </button>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Наименование *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  placeholder="Компютър HP ProBook"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Инвентарен номер *</label>
                  <input
                    type="text"
                    value={formData.inventoryNumber}
                    onChange={(e) => setFormData({ ...formData, inventoryNumber: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="DMA-001"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Дата на придобиване *</label>
                  <input
                    type="date"
                    value={formData.acquisitionDate}
                    onChange={(e) => setFormData({ ...formData, acquisitionDate: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Категория по ЗКПО *</label>
                <select
                  value={formData.categoryId}
                  onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                >
                  <option value="">Изберете категория...</option>
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>
                      {cat.code} - {cat.name} (макс. {cat.maxTaxDepreciationRate}%)
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Първоначална стойност (EUR) *</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.acquisitionCost}
                    onChange={(e) => setFormData({ ...formData, acquisitionCost: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="1500.00"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Дата въвеждане в експлоатация</label>
                  <input
                    type="date"
                    value={formData.putIntoServiceDate}
                    onChange={(e) => setFormData({ ...formData, putIntoServiceDate: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Документ номер</label>
                  <input
                    type="text"
                    value={formData.documentNumber}
                    onChange={(e) => setFormData({ ...formData, documentNumber: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="0000000001"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Дата на документа</label>
                  <input
                    type="date"
                    value={formData.documentDate}
                    onChange={(e) => setFormData({ ...formData, documentDate: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
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
                  {creating ? 'Създаване...' : 'Създай актив'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
