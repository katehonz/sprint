import { useState } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_FIXED_ASSET_CATEGORIES, UPDATE_FIXED_ASSET_CATEGORY } from '../../graphql/queries';

interface Category {
  id: string;
  code: string;
  name: string;
  description: string;
  taxCategory: number;
  maxTaxDepreciationRate: number;
  defaultAccountingDepreciationRate: number;
  assetAccountCode: string;
  depreciationAccountCode: string;
  expenseAccountCode: string;
  isActive: boolean;
}

const TAX_CATEGORY_DESCRIPTIONS: Record<number, string> = {
  1: 'Масивни сгради, съоръжения, предавателни устройства',
  2: 'Машини, производствено оборудване, апаратура',
  3: 'Транспортни средства без автомобили',
  4: 'Компютри, периферни устройства, софтуер',
  5: 'Автомобили',
  6: 'Други ДМА',
  7: 'Нематериални активи',
};

const TAX_CATEGORY_ICONS: Record<number, string> = {
  1: '🏢',
  2: '⚙️',
  3: '🚂',
  4: '💻',
  5: '🚗',
  6: '📦',
  7: '📄',
};

export default function AssetCategories() {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState({
    assetAccountCode: '',
    depreciationAccountCode: '',
    expenseAccountCode: '',
    defaultAccountingDepreciationRate: '',
  });

  const { data, loading, refetch } = useQuery<any>(GET_FIXED_ASSET_CATEGORIES, {
    variables: { companyId: '1' },
  });

  const [updateCategory, { loading: updating }] = useMutation<any>(UPDATE_FIXED_ASSET_CATEGORY);

  const categories: Category[] = data?.fixedAssetCategories || [];

  const handleEdit = (category: Category) => {
    setEditingId(category.id);
    setEditForm({
      assetAccountCode: category.assetAccountCode || '',
      depreciationAccountCode: category.depreciationAccountCode || '',
      expenseAccountCode: category.expenseAccountCode || '',
      defaultAccountingDepreciationRate: String(category.defaultAccountingDepreciationRate || ''),
    });
  };

  const handleCancel = () => {
    setEditingId(null);
    setEditForm({
      assetAccountCode: '',
      depreciationAccountCode: '',
      expenseAccountCode: '',
      defaultAccountingDepreciationRate: '',
    });
  };

  const handleSave = async (categoryId: string) => {
    try {
      await updateCategory({
        variables: {
          id: categoryId,
          input: {
            assetAccountCode: editForm.assetAccountCode,
            depreciationAccountCode: editForm.depreciationAccountCode,
            expenseAccountCode: editForm.expenseAccountCode,
            defaultAccountingDepreciationRate: editForm.defaultAccountingDepreciationRate
              ? parseFloat(editForm.defaultAccountingDepreciationRate)
              : null,
          },
        },
      });
      setEditingId(null);
      refetch();
    } catch (err) {
      alert('Грешка при запис: ' + (err instanceof Error ? err.message : 'Неизвестна грешка'));
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Info Box */}
      <div className="bg-blue-50 rounded-lg p-4">
        <h4 className="text-sm font-medium text-blue-900 mb-2">Категории ДМА по ЗКПО</h4>
        <p className="text-sm text-blue-800">
          Категориите определят максималните данъчни норми на амортизация според чл. 55 от ЗКПО.
          Можете да редактирате счетоводните сметки за всяка категория.
        </p>
      </div>

      {/* Categories List */}
      <div className="space-y-4">
        {categories.map((category) => (
          <div
            key={category.id}
            className={`bg-white rounded-lg border p-4 ${
              category.isActive ? 'border-gray-200' : 'border-gray-100 opacity-60'
            }`}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{TAX_CATEGORY_ICONS[category.taxCategory] || '📁'}</span>
                <div>
                  <h3 className="text-sm font-medium text-gray-900">{category.name}</h3>
                  <p className="text-xs text-gray-500">
                    Код: {category.code} | Данъчна категория {category.taxCategory}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                  category.isActive
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-600'
                }`}>
                  {category.isActive ? 'Активна' : 'Неактивна'}
                </span>
                {editingId !== category.id && (
                  <button
                    onClick={() => handleEdit(category)}
                    className="px-3 py-1 text-xs text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded"
                  >
                    Редактирай
                  </button>
                )}
              </div>
            </div>

            <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Left column - Rates info */}
              <div className="space-y-2">
                <div className="text-xs text-gray-500 bg-gray-50 p-2 rounded">
                  {TAX_CATEGORY_DESCRIPTIONS[category.taxCategory] || 'Неопределена категория'}
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Макс. данъчна норма:</span>
                  <span className="font-medium text-gray-900">{category.maxTaxDepreciationRate}%</span>
                </div>
                {editingId !== category.id && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Счетоводна норма:</span>
                    <span className="font-medium text-gray-900">
                      {category.defaultAccountingDepreciationRate || '-'}%
                    </span>
                  </div>
                )}
              </div>

              {/* Right column - Account codes */}
              {editingId === category.id ? (
                <div className="space-y-3 bg-blue-50 p-3 rounded-lg">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Сметка за актива (напр. 204)
                    </label>
                    <input
                      type="text"
                      value={editForm.assetAccountCode}
                      onChange={(e) => setEditForm({ ...editForm, assetAccountCode: e.target.value })}
                      className="w-full px-2 py-1 text-sm border rounded"
                      placeholder="204"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Сметка за натрупана амортизация (напр. 2414)
                    </label>
                    <input
                      type="text"
                      value={editForm.depreciationAccountCode}
                      onChange={(e) => setEditForm({ ...editForm, depreciationAccountCode: e.target.value })}
                      className="w-full px-2 py-1 text-sm border rounded"
                      placeholder="2414"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Сметка за разход (напр. 603)
                    </label>
                    <input
                      type="text"
                      value={editForm.expenseAccountCode}
                      onChange={(e) => setEditForm({ ...editForm, expenseAccountCode: e.target.value })}
                      className="w-full px-2 py-1 text-sm border rounded"
                      placeholder="603"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Счетоводна норма (%)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={editForm.defaultAccountingDepreciationRate}
                      onChange={(e) => setEditForm({ ...editForm, defaultAccountingDepreciationRate: e.target.value })}
                      className="w-full px-2 py-1 text-sm border rounded"
                      placeholder="15.00"
                    />
                  </div>
                  <div className="flex gap-2 pt-2">
                    <button
                      onClick={() => handleSave(category.id)}
                      disabled={updating}
                      className="px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                    >
                      {updating ? 'Запис...' : 'Запази'}
                    </button>
                    <button
                      onClick={handleCancel}
                      className="px-3 py-1 text-xs bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
                    >
                      Отказ
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-2 bg-gray-50 p-3 rounded-lg">
                  <h4 className="text-xs font-medium text-gray-700 mb-2">Счетоводни сметки</h4>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Сметка актив:</span>
                    <span className="font-mono text-gray-900">{category.assetAccountCode || '-'}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Натр. амортизация:</span>
                    <span className="font-mono text-gray-900">{category.depreciationAccountCode || '-'}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Разход амортизация:</span>
                    <span className="font-mono text-gray-900">{category.expenseAccountCode || '-'}</span>
                  </div>
                </div>
              )}
            </div>

            {category.description && (
              <p className="mt-3 text-xs text-gray-500 border-t pt-2">
                {category.description}
              </p>
            )}
          </div>
        ))}
      </div>

      {categories.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500">Няма налични категории</p>
        </div>
      )}

      {/* Tax Categories Reference */}
      <div className="bg-gray-50 rounded-lg p-4">
        <h4 className="text-sm font-medium text-gray-900 mb-3">Справка за данъчните категории (ЗКПО)</h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
          {Object.entries(TAX_CATEGORY_DESCRIPTIONS).map(([key, desc]) => (
            <div key={key} className="flex items-center gap-2">
              <span className="font-medium text-gray-700">Кат. {key}:</span>
              <span className="text-gray-600">{desc}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Accounting Info */}
      <div className="bg-yellow-50 rounded-lg p-4">
        <h4 className="text-sm font-medium text-yellow-900 mb-2">Осчетоводяване на амортизация</h4>
        <p className="text-sm text-yellow-800">
          При осчетоводяване на амортизацията се създава статия:<br/>
          <span className="font-mono">Дт [Разход] / Кт [Натрупана амортизация]</span>
        </p>
      </div>
    </div>
  );
}
