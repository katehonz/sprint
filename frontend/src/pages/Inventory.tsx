import React, { useState } from 'react';
import { useQuery, useLazyQuery } from '@apollo/client/react';
import { GET_INVENTORY_BALANCES, GET_QUANTITY_TURNOVER } from '../graphql/queries';
import { useCompany } from '../contexts/CompanyContext';

interface Account {
    id: string;
    code: string;
    name: string;
}

interface InventoryBalance {
    id: string;
    currentQuantity: number;
    currentAmount: number;
    averageCost: number;
    unitOfMeasure: string;
    lastMovementDate: string;
    account: Account;
}

interface QuantityTurnover {
    accountId: number;
    accountCode: string;
    accountName: string;
    openingQuantity: number;
    openingAmount: number;
    receiptQuantity: number;
    receiptAmount: number;
    issueQuantity: number;
    issueAmount: number;
    closingQuantity: number;
    closingAmount: number;
}

interface InventoryBalancesData {
    inventoryBalances: InventoryBalance[];
}

interface QuantityTurnoverData {
    getQuantityTurnover: QuantityTurnover[];
}

type TabType = 'balances' | 'turnover';

const Inventory: React.FC = () => {
    const { companyId } = useCompany();
    const [activeTab, setActiveTab] = useState<TabType>('balances');

    // Периоди за количествена ведомост
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const [fromDate, setFromDate] = useState(firstDayOfMonth.toISOString().split('T')[0]);
    const [toDate, setToDate] = useState(today.toISOString().split('T')[0]);

    const { data: inventoryData, loading: inventoryLoading } = useQuery<InventoryBalancesData>(GET_INVENTORY_BALANCES, {
        variables: { companyId: companyId },
        skip: !companyId,
    });

    const [fetchTurnover, { data: turnoverData, loading: turnoverLoading }] = useLazyQuery<QuantityTurnoverData>(GET_QUANTITY_TURNOVER);

    const balances = inventoryData?.inventoryBalances || [];
    const turnoverRows = turnoverData?.getQuantityTurnover || [];

    const handleFetchTurnover = () => {
        if (companyId) {
            fetchTurnover({
                variables: {
                    input: {
                        companyId,
                        fromDate,
                        toDate
                    }
                }
            });
        }
    };

    // Изчисляваме общи суми за количествената ведомост
    const turnoverTotals = turnoverRows.reduce((acc, row) => ({
        openingAmount: acc.openingAmount + row.openingAmount,
        receiptAmount: acc.receiptAmount + row.receiptAmount,
        issueAmount: acc.issueAmount + row.issueAmount,
        closingAmount: acc.closingAmount + row.closingAmount,
    }), { openingAmount: 0, receiptAmount: 0, issueAmount: 0, closingAmount: 0 });

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Материални запаси</h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Преглед на складови наличности и справки
                    </p>
                </div>
            </div>

            {/* Табове */}
            <div className="border-b border-gray-200">
                <nav className="-mb-px flex space-x-8">
                    <button
                        onClick={() => setActiveTab('balances')}
                        className={`py-2 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'balances'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Наличности
                    </button>
                    <button
                        onClick={() => setActiveTab('turnover')}
                        className={`py-2 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'turnover'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Количествена оборотна ведомост
                    </button>
                </nav>
            </div>

            {/* Таб: Наличности */}
            {activeTab === 'balances' && (
                <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Материал</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Количество</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Мярка</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">СПЦ</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Стойност</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Последно движение</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {inventoryLoading ? (
                                <tr><td colSpan={6} className="px-6 py-12 text-center text-gray-500">Зареждане...</td></tr>
                            ) : balances.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                                        {companyId ? 'Няма данни за наличности.' : 'Моля, изберете компания от менюто горе.'}
                                    </td>
                                </tr>
                            ) : (
                                balances.map((balance) => (
                                    <tr key={balance.id} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <div className="text-sm font-medium text-gray-900">{balance.account.name}</div>
                                            <div className="text-sm text-gray-500">{balance.account.code}</div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-gray-700">{balance.currentQuantity.toFixed(2)}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{balance.unitOfMeasure || '-'}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-gray-700">{balance.averageCost.toFixed(4)}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-semibold text-gray-900">{balance.currentAmount.toFixed(2)}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{balance.lastMovementDate || '-'}</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                        {balances.length > 0 && (
                            <tfoot className="bg-gray-100">
                                <tr>
                                    <td colSpan={4} className="px-6 py-3 text-right text-sm font-semibold text-gray-900">
                                        Общо:
                                    </td>
                                    <td className="px-6 py-3 text-right text-sm font-bold text-gray-900">
                                        {balances.reduce((sum, b) => sum + b.currentAmount, 0).toFixed(2)}
                                    </td>
                                    <td></td>
                                </tr>
                            </tfoot>
                        )}
                    </table>
                </div>
            )}

            {/* Таб: Количествена оборотна ведомост */}
            {activeTab === 'turnover' && (
                <div className="space-y-4">
                    {/* Филтри */}
                    <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-4">
                        <div className="flex items-end gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">От дата</label>
                                <input
                                    type="date"
                                    value={fromDate}
                                    onChange={(e) => setFromDate(e.target.value)}
                                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">До дата</label>
                                <input
                                    type="date"
                                    value={toDate}
                                    onChange={(e) => setToDate(e.target.value)}
                                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                                />
                            </div>
                            <button
                                onClick={handleFetchTurnover}
                                disabled={!companyId || turnoverLoading}
                                className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {turnoverLoading ? 'Зареждане...' : 'Генерирай'}
                            </button>
                        </div>
                    </div>

                    {/* Таблица с резултати */}
                    <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th rowSpan={2} className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider border-r">
                                        Сметка
                                    </th>
                                    <th colSpan={2} className="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider border-r bg-blue-50">
                                        Начално салдо
                                    </th>
                                    <th colSpan={2} className="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider border-r bg-green-50">
                                        Приход
                                    </th>
                                    <th colSpan={2} className="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider border-r bg-red-50">
                                        Разход
                                    </th>
                                    <th colSpan={2} className="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider bg-yellow-50">
                                        Крайно салдо
                                    </th>
                                </tr>
                                <tr>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase border-r bg-blue-50">Кол.</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase border-r bg-blue-50">Ст-ст</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase border-r bg-green-50">Кол.</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase border-r bg-green-50">Ст-ст</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase border-r bg-red-50">Кол.</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase border-r bg-red-50">Ст-ст</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase bg-yellow-50">Кол.</th>
                                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase bg-yellow-50">Ст-ст</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {turnoverLoading ? (
                                    <tr><td colSpan={9} className="px-6 py-12 text-center text-gray-500">Зареждане...</td></tr>
                                ) : turnoverRows.length === 0 ? (
                                    <tr>
                                        <td colSpan={9} className="px-6 py-12 text-center text-gray-500">
                                            {companyId ? 'Натиснете "Генерирай" за да заредите данните.' : 'Моля, изберете компания.'}
                                        </td>
                                    </tr>
                                ) : (
                                    turnoverRows.map((row) => (
                                        <tr key={row.accountId} className="hover:bg-gray-50">
                                            <td className="px-3 py-2 whitespace-nowrap border-r">
                                                <div className="text-sm font-medium text-gray-900">{row.accountCode}</div>
                                                <div className="text-xs text-gray-500 truncate max-w-[150px]" title={row.accountName}>{row.accountName}</div>
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm text-gray-700 border-r bg-blue-50/30">
                                                {row.openingQuantity.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm text-gray-700 border-r bg-blue-50/30">
                                                {row.openingAmount.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm text-green-700 border-r bg-green-50/30">
                                                {row.receiptQuantity.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm text-green-700 border-r bg-green-50/30">
                                                {row.receiptAmount.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm text-red-700 border-r bg-red-50/30">
                                                {row.issueQuantity.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm text-red-700 border-r bg-red-50/30">
                                                {row.issueAmount.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm font-semibold text-gray-900 bg-yellow-50/30">
                                                {row.closingQuantity.toFixed(2)}
                                            </td>
                                            <td className="px-3 py-2 whitespace-nowrap text-right text-sm font-semibold text-gray-900 bg-yellow-50/30">
                                                {row.closingAmount.toFixed(2)}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                            {turnoverRows.length > 0 && (
                                <tfoot className="bg-gray-100">
                                    <tr className="font-semibold">
                                        <td className="px-3 py-3 text-right text-sm text-gray-900 border-r">Общо:</td>
                                        <td className="px-3 py-3 text-right text-sm text-gray-700 border-r">-</td>
                                        <td className="px-3 py-3 text-right text-sm text-gray-900 border-r">{turnoverTotals.openingAmount.toFixed(2)}</td>
                                        <td className="px-3 py-3 text-right text-sm text-gray-700 border-r">-</td>
                                        <td className="px-3 py-3 text-right text-sm text-green-700 border-r">{turnoverTotals.receiptAmount.toFixed(2)}</td>
                                        <td className="px-3 py-3 text-right text-sm text-gray-700 border-r">-</td>
                                        <td className="px-3 py-3 text-right text-sm text-red-700 border-r">{turnoverTotals.issueAmount.toFixed(2)}</td>
                                        <td className="px-3 py-3 text-right text-sm text-gray-700">-</td>
                                        <td className="px-3 py-3 text-right text-sm font-bold text-gray-900">{turnoverTotals.closingAmount.toFixed(2)}</td>
                                    </tr>
                                </tfoot>
                            )}
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Inventory;
