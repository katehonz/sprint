// Build: 2025-12-13-v2 - Added journal entry display and duplicate prevention
import React, { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { gql } from '@apollo/client';
import { useNavigate } from 'react-router-dom';
import { useCompany } from '../contexts/CompanyContext';
import { GET_ACCOUNTS, GET_COUNTERPARTS, CREATE_COUNTERPART, VALIDATE_VAT } from '../graphql/queries';

const GET_SCANNED_INVOICES = gql`
    query ScannedInvoices($companyId: ID!) {
        scannedInvoices(companyId: $companyId) {
            id
            direction
            status
            invoiceNumber
            invoiceDate
            vendorName
            vendorVatNumber
            customerName
            customerVatNumber
            subtotal
            totalTax
            invoiceTotal
            viesStatus
            viesValidationMessage
            viesCompanyName
            viesCompanyAddress
            requiresManualReview
            manualReviewReason
            counterpartyAccount { id code name }
            vatAccount { id code name }
            expenseRevenueAccount { id code name }
            journalEntry { id entryNumber }
            createdAt
        }
    }
`;

const GET_SCANNED_INVOICES_BY_DIRECTION = gql`
    query ScannedInvoicesByDirection($companyId: ID!, $direction: InvoiceDirection!) {
        scannedInvoicesByDirection(companyId: $companyId, direction: $direction) {
            id
            direction
            status
            invoiceNumber
            invoiceDate
            vendorName
            vendorVatNumber
            customerName
            customerVatNumber
            subtotal
            totalTax
            invoiceTotal
            viesStatus
            viesValidationMessage
            viesCompanyName
            viesCompanyAddress
            requiresManualReview
            manualReviewReason
            counterpartyAccount { id code name }
            vatAccount { id code name }
            expenseRevenueAccount { id code name }
            journalEntry { id entryNumber }
            createdAt
        }
    }
`;

const DELETE_SCANNED_INVOICE = gql`
    mutation DeleteScannedInvoice($id: ID!) {
        deleteScannedInvoice(id: $id)
    }
`;

const VALIDATE_SCANNED_INVOICE_VIES = gql`
    mutation ValidateScannedInvoiceVies($id: ID!) {
        validateScannedInvoiceVies(id: $id) {
            id
            viesStatus
            viesValidationMessage
            viesCompanyName
            viesCompanyAddress
        }
    }
`;

const UPDATE_SCANNED_INVOICE_ACCOUNTS = gql`
    mutation UpdateScannedInvoiceAccounts($id: ID!, $input: UpdateScannedInvoiceAccountsInput!) {
        updateScannedInvoiceAccounts(id: $id, input: $input) {
            id
            counterpartyAccount { id code name }
            vatAccount { id code name }
            expenseRevenueAccount { id code name }
        }
    }
`;

interface Account {
    id: number;
    code: string;
    name: string;
    parentId: number | null;
}

interface Counterpart {
    id: number;
    name: string;
    eik: string;
    vatNumber: string;
    address: string;
    city: string;
    country: string;
}

interface ScannedInvoice {
    id: number;
    direction: 'PURCHASE' | 'SALE';
    status: 'PENDING' | 'VALIDATED' | 'PROCESSED' | 'REJECTED';
    invoiceNumber: string;
    invoiceDate: string;
    vendorName: string;
    vendorVatNumber: string;
    customerName: string;
    customerVatNumber: string;
    subtotal: number;
    totalTax: number;
    invoiceTotal: number;
    viesStatus: 'PENDING' | 'VALID' | 'INVALID' | 'NOT_APPLICABLE' | 'ERROR';
    viesValidationMessage: string;
    viesCompanyName: string;
    viesCompanyAddress: string;
    requiresManualReview: boolean;
    manualReviewReason: string;
    counterpartyAccount: { id: number; code: string; name: string } | null;
    vatAccount: { id: number; code: string; name: string } | null;
    expenseRevenueAccount: { id: number; code: string; name: string } | null;
    journalEntry: { id: number; entryNumber: string } | null;
    createdAt: string;
}

type TabType = 'ALL' | 'PURCHASE' | 'SALE';

const ScannedInvoices: React.FC = () => {
    const navigate = useNavigate();
    const { companyId } = useCompany();
    const [activeTab, setActiveTab] = useState<TabType>('ALL');
    const [selectedInvoice, setSelectedInvoice] = useState<ScannedInvoice | null>(null);
    const [isEditing, setIsEditing] = useState(false);
    const [showCounterpartModal, setShowCounterpartModal] = useState(false);
    const [counterpartMode, setCounterpartMode] = useState<'existing' | 'vies' | 'manual'>('existing');
    const [isNavigating, setIsNavigating] = useState(false);

    // Edit state for accounts
    const [editCounterpartyAccountId, setEditCounterpartyAccountId] = useState<number | null>(null);
    const [editVatAccountId, setEditVatAccountId] = useState<number | null>(null);
    const [editExpenseRevenueAccountId, setEditExpenseRevenueAccountId] = useState<number | null>(null);

    // Manual counterpart form
    const [manualName, setManualName] = useState('');
    const [manualEik, setManualEik] = useState('');
    const [manualVatNumber, setManualVatNumber] = useState('');
    const [manualAddress, setManualAddress] = useState('');
    const [manualCity, setManualCity] = useState('');
    const [manualCountry, setManualCountry] = useState('BG');

    // Queries
    const { data: allData, loading: loadingAll, refetch: refetchAll } = useQuery<{ scannedInvoices: ScannedInvoice[] }>(GET_SCANNED_INVOICES, {
        variables: { companyId },
        skip: !companyId || activeTab !== 'ALL',
    });

    const { data: filteredData, loading: loadingFiltered, refetch: refetchFiltered } = useQuery<{ scannedInvoicesByDirection: ScannedInvoice[] }>(GET_SCANNED_INVOICES_BY_DIRECTION, {
        variables: { companyId, direction: activeTab },
        skip: !companyId || activeTab === 'ALL',
    });

    const { data: accountsData } = useQuery<{ accounts: Account[] }>(GET_ACCOUNTS, {
        variables: { companyId },
        skip: !companyId,
    });

    const { data: counterpartsData, refetch: refetchCounterparts } = useQuery<{ counterparts: Counterpart[] }>(GET_COUNTERPARTS, {
        variables: { companyId },
        skip: !companyId,
    });

    // Mutations
    const [deleteInvoice] = useMutation<any>(DELETE_SCANNED_INVOICE);
    const [validateVies, { loading: validating }] = useMutation<any>(VALIDATE_SCANNED_INVOICE_VIES);
    const [updateAccounts, { loading: updatingAccounts }] = useMutation<any>(UPDATE_SCANNED_INVOICE_ACCOUNTS);
    const [validateVatNumber, { loading: validatingVat }] = useMutation<any>(VALIDATE_VAT);
    const [createCounterpart, { loading: creatingCounterpart }] = useMutation<any>(CREATE_COUNTERPART);

    const invoices: ScannedInvoice[] = activeTab === 'ALL'
        ? (allData?.scannedInvoices || [])
        : (filteredData?.scannedInvoicesByDirection || []);

    const accounts = accountsData?.accounts || [];
    const counterparts = counterpartsData?.counterparts || [];

    const loading = activeTab === 'ALL' ? loadingAll : loadingFiltered;

    // Flatten accounts for dropdown
    const flattenAccounts = (accs: Account[]): Account[] => {
        return accs.filter(a => a.code.length >= 3); // Only leaf accounts (3+ digits)
    };

    const flatAccounts = flattenAccounts(accounts);

    // Initialize edit state when selecting an invoice
    useEffect(() => {
        if (selectedInvoice) {
            setEditCounterpartyAccountId(selectedInvoice.counterpartyAccount?.id || null);
            setEditVatAccountId(selectedInvoice.vatAccount?.id || null);
            setEditExpenseRevenueAccountId(selectedInvoice.expenseRevenueAccount?.id || null);
            setIsNavigating(false); // Reset navigating state when selecting a new invoice
        }
    }, [selectedInvoice]);

    // Pre-fill manual form from invoice data
    const openCounterpartModal = () => {
        if (selectedInvoice) {
            const isVendor = selectedInvoice.direction === 'PURCHASE';
            setManualName(isVendor ? selectedInvoice.vendorName : selectedInvoice.customerName);
            setManualVatNumber(isVendor ? selectedInvoice.vendorVatNumber : selectedInvoice.customerVatNumber);
            setManualEik('');
            setManualAddress('');
            setManualCity('');
            setManualCountry('BG');
        }
        setShowCounterpartModal(true);
    };

    const refetchInvoices = () => {
        if (activeTab === 'ALL') {
            refetchAll();
        } else {
            refetchFiltered();
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Сигурни ли сте, че искате да изтриете тази фактура?')) return;
        try {
            await deleteInvoice({ variables: { id } });
            refetchInvoices();
            setSelectedInvoice(null);
        } catch (error) {
            console.error('Error deleting invoice:', error);
            alert('Грешка при изтриване');
        }
    };

    const handleValidateVies = async (id: number) => {
        try {
            const result = await validateVies({ variables: { id } });
            refetchInvoices();
            // Update selected invoice with new VIES data
            if (result.data?.validateScannedInvoiceVies) {
                setSelectedInvoice(prev => prev ? { ...prev, ...result.data.validateScannedInvoiceVies } : null);
            }
        } catch (error) {
            console.error('Error validating VIES:', error);
            alert('Грешка при VIES валидация');
        }
    };

    const handleSaveAccounts = async () => {
        if (!selectedInvoice) return;
        try {
            await updateAccounts({
                variables: {
                    id: selectedInvoice.id,
                    input: {
                        counterpartyAccountId: editCounterpartyAccountId,
                        vatAccountId: editVatAccountId,
                        expenseRevenueAccountId: editExpenseRevenueAccountId,
                    },
                },
            });
            refetchInvoices();
            setIsEditing(false);
            alert('Сметките са запазени успешно');
        } catch (error) {
            console.error('Error updating accounts:', error);
            alert('Грешка при запазване на сметки');
        }
    };

    const handleAddCounterpartVies = async () => {
        if (!selectedInvoice) return;
        const vatNumber = selectedInvoice.direction === 'PURCHASE'
            ? selectedInvoice.vendorVatNumber
            : selectedInvoice.customerVatNumber;

        if (!vatNumber) {
            alert('Няма ДДС номер за валидация');
            return;
        }

        try {
            // First validate with VIES
            const viesResult = await validateVatNumber({ variables: { vatNumber } });
            const viesData = viesResult.data?.validateVat;

            if (!viesData?.valid) {
                alert(`VIES валидацията неуспешна: ${viesData?.errorMessage || 'Невалиден номер'}`);
                return;
            }

            // Create counterpart from VIES data
            const result = await createCounterpart({
                variables: {
                    input: {
                        companyId: parseInt(companyId!),
                        name: viesData.name || (selectedInvoice.direction === 'PURCHASE' ? selectedInvoice.vendorName : selectedInvoice.customerName),
                        eik: vatNumber.replace(/^[A-Z]{2}/, ''),
                        vatNumber: vatNumber,
                        address: viesData.longAddress || '',
                        city: '',
                        country: viesData.countryCode || 'BG',
                        counterpartType: selectedInvoice.direction === 'PURCHASE' ? 'SUPPLIER' : 'CUSTOMER',
                        isVatRegistered: true,
                    },
                },
            });

            if (result.data?.createCounterpart) {
                alert(`Контрагент "${result.data.createCounterpart.name}" е добавен успешно`);
                refetchCounterparts();
                setShowCounterpartModal(false);
            }
        } catch (error: any) {
            console.error('Error adding counterpart via VIES:', error);
            alert(`Грешка: ${error.message || 'Неуспешно добавяне'}`);
        }
    };

    const handleAddCounterpartManual = async () => {
        if (!manualName.trim()) {
            alert('Моля, въведете име на контрагента');
            return;
        }

        try {
            const result = await createCounterpart({
                variables: {
                    input: {
                        companyId: parseInt(companyId!),
                        name: manualName,
                        eik: manualEik || null,
                        vatNumber: manualVatNumber || null,
                        address: manualAddress || null,
                        city: manualCity || null,
                        country: manualCountry || 'BG',
                        counterpartType: selectedInvoice?.direction === 'PURCHASE' ? 'SUPPLIER' : 'CUSTOMER',
                        isVatRegistered: !!manualVatNumber,
                    },
                },
            });

            if (result.data?.createCounterpart) {
                alert(`Контрагент "${result.data.createCounterpart.name}" е добавен успешно`);
                refetchCounterparts();
                setShowCounterpartModal(false);
                // Reset form
                setManualName('');
                setManualEik('');
                setManualVatNumber('');
                setManualAddress('');
                setManualCity('');
                setManualCountry('BG');
            }
        } catch (error: any) {
            console.error('Error adding counterpart manually:', error);
            alert(`Грешка: ${error.message || 'Неуспешно добавяне'}`);
        }
    };

    const handleProcessInvoice = () => {
        if (!selectedInvoice || isNavigating) return;

        // Prevent double clicks
        setIsNavigating(true);

        // Find existing counterpart by VAT number
        const existingCounterpart = findExistingCounterpart();

        // Prepare data for VAT Entry form
        const invoiceData = {
            scannedInvoiceId: selectedInvoice.id,
            direction: selectedInvoice.direction,
            documentNumber: selectedInvoice.invoiceNumber || '',
            documentDate: selectedInvoice.invoiceDate || new Date().toISOString().split('T')[0],
            baseAmount: selectedInvoice.subtotal?.toString() || '',
            vatAmount: selectedInvoice.totalTax?.toString() || '',
            totalAmount: selectedInvoice.invoiceTotal?.toString() || '',
            // Counterpart info
            counterpartId: existingCounterpart?.id || '',
            counterpartName: selectedInvoice.direction === 'PURCHASE'
                ? selectedInvoice.vendorName
                : selectedInvoice.customerName,
            counterpartVatNumber: selectedInvoice.direction === 'PURCHASE'
                ? selectedInvoice.vendorVatNumber
                : selectedInvoice.customerVatNumber,
            // Account suggestions
            counterpartyAccountId: selectedInvoice.counterpartyAccount?.id || '',
            vatAccountId: selectedInvoice.vatAccount?.id || '',
            expenseRevenueAccountId: selectedInvoice.expenseRevenueAccount?.id || '',
        };

        // Navigate to VAT Entry form with prefilled data
        navigate('/vat/entry', { state: { fromScannedInvoice: invoiceData } });
    };

    // Check if counterpart exists
    const findExistingCounterpart = (): Counterpart | null => {
        if (!selectedInvoice) return null;
        const vatNumber = selectedInvoice.direction === 'PURCHASE'
            ? selectedInvoice.vendorVatNumber
            : selectedInvoice.customerVatNumber;
        const name = selectedInvoice.direction === 'PURCHASE'
            ? selectedInvoice.vendorName
            : selectedInvoice.customerName;

        return counterparts.find(c =>
            (vatNumber && c.vatNumber === vatNumber) ||
            (name && c.name.toLowerCase() === name.toLowerCase())
        ) || null;
    };

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'PENDING':
                return <span className="px-2 py-1 text-xs rounded-full bg-yellow-100 text-yellow-800">Чакащ</span>;
            case 'VALIDATED':
                return <span className="px-2 py-1 text-xs rounded-full bg-green-100 text-green-800">Валидиран</span>;
            case 'PROCESSED':
                return <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">Осчетоводен</span>;
            case 'REJECTED':
                return <span className="px-2 py-1 text-xs rounded-full bg-red-100 text-red-800">Отхвърлен</span>;
            default:
                return <span className="px-2 py-1 text-xs rounded-full bg-gray-100 text-gray-800">{status}</span>;
        }
    };

    const getViesStatusBadge = (status: string) => {
        switch (status) {
            case 'VALID':
                return <span className="px-2 py-1 text-xs rounded-full bg-green-100 text-green-800">VIES ✓</span>;
            case 'INVALID':
                return <span className="px-2 py-1 text-xs rounded-full bg-red-100 text-red-800">VIES ✗</span>;
            case 'PENDING':
                return <span className="px-2 py-1 text-xs rounded-full bg-yellow-100 text-yellow-800">VIES ?</span>;
            case 'NOT_APPLICABLE':
                return <span className="px-2 py-1 text-xs rounded-full bg-gray-100 text-gray-600">N/A</span>;
            case 'ERROR':
                return <span className="px-2 py-1 text-xs rounded-full bg-orange-100 text-orange-800">Грешка</span>;
            default:
                return null;
        }
    };

    const getDirectionBadge = (direction: string) => {
        return direction === 'PURCHASE'
            ? <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">Покупка</span>
            : <span className="px-2 py-1 text-xs rounded-full bg-purple-100 text-purple-800">Продажба</span>;
    };

    const formatDate = (dateStr: string) => {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleDateString('bg-BG');
    };

    const formatCurrency = (amount: number) => {
        if (amount == null) return '-';
        return amount.toLocaleString('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' лв.';
    };

    if (!companyId) {
        return (
            <div className="p-6">
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                    <p className="text-yellow-800">Моля, изберете фирма</p>
                </div>
            </div>
        );
    }

    const existingCounterpart = findExistingCounterpart();

    return (
        <div className="p-6">
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-gray-900">Сканирани фактури</h1>
                <p className="text-gray-600">Преглед и управление на сканирани фактури за обработка</p>
            </div>

            {/* Tabs */}
            <div className="mb-6 border-b border-gray-200">
                <nav className="-mb-px flex space-x-8">
                    <button
                        onClick={() => setActiveTab('ALL')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'ALL'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Всички
                    </button>
                    <button
                        onClick={() => setActiveTab('PURCHASE')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'PURCHASE'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Покупки
                    </button>
                    <button
                        onClick={() => setActiveTab('SALE')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'SALE'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Продажби
                    </button>
                </nav>
            </div>

            {loading ? (
                <div className="text-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                    <p className="mt-4 text-gray-600">Зареждане...</p>
                </div>
            ) : invoices.length === 0 ? (
                <div className="text-center py-12 bg-gray-50 rounded-lg">
                    <p className="text-gray-600">Няма сканирани фактури</p>
                    <a href="/doc-scanner" className="mt-4 inline-block text-blue-600 hover:text-blue-800">
                        → Сканирай нова фактура
                    </a>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Invoice List */}
                    <div className="lg:col-span-2">
                        <div className="bg-white shadow rounded-lg overflow-hidden">
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Фактура</th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Контрагент</th>
                                        <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Сума</th>
                                        <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Статус</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {invoices.map((invoice) => (
                                        <tr
                                            key={invoice.id}
                                            onClick={() => { setSelectedInvoice(invoice); setIsEditing(false); }}
                                            className={`cursor-pointer hover:bg-gray-50 ${
                                                selectedInvoice?.id === invoice.id ? 'bg-blue-50' : ''
                                            }`}
                                        >
                                            <td className="px-4 py-3">
                                                <div className="flex items-center gap-2">
                                                    {getDirectionBadge(invoice.direction)}
                                                    <div>
                                                        <div className="font-medium text-gray-900">
                                                            {invoice.invoiceNumber || 'Без номер'}
                                                        </div>
                                                        <div className="text-sm text-gray-500">
                                                            {formatDate(invoice.invoiceDate)}
                                                        </div>
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="text-sm text-gray-900">
                                                    {invoice.direction === 'PURCHASE' ? invoice.vendorName : invoice.customerName}
                                                </div>
                                                <div className="text-xs text-gray-500">
                                                    {invoice.direction === 'PURCHASE' ? invoice.vendorVatNumber : invoice.customerVatNumber}
                                                </div>
                                            </td>
                                            <td className="px-4 py-3 text-right">
                                                <div className="font-medium text-gray-900">
                                                    {formatCurrency(invoice.invoiceTotal)}
                                                </div>
                                                <div className="text-xs text-gray-500">
                                                    ДДС: {formatCurrency(invoice.totalTax)}
                                                </div>
                                            </td>
                                            <td className="px-4 py-3 text-center">
                                                <div className="flex flex-col items-center gap-1">
                                                    {getStatusBadge(invoice.status)}
                                                    {getViesStatusBadge(invoice.viesStatus)}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Details Panel */}
                    <div className="lg:col-span-1">
                        {selectedInvoice ? (
                            <div className="bg-white shadow rounded-lg p-6 space-y-4">
                                <div className="flex items-center justify-between">
                                    <h3 className="text-lg font-medium text-gray-900">Детайли</h3>
                                    {!isEditing && (
                                        <button
                                            onClick={() => setIsEditing(true)}
                                            className="text-sm text-blue-600 hover:text-blue-800"
                                        >
                                            Редактирай
                                        </button>
                                    )}
                                </div>

                                {/* Header */}
                                <div className="flex items-center justify-between">
                                    {getDirectionBadge(selectedInvoice.direction)}
                                    {getStatusBadge(selectedInvoice.status)}
                                </div>

                                {selectedInvoice.requiresManualReview && (
                                    <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                                        <p className="text-sm text-yellow-800 font-medium">⚠️ Изисква ръчен преглед</p>
                                        <p className="text-xs text-yellow-700 mt-1">{selectedInvoice.manualReviewReason}</p>
                                    </div>
                                )}

                                {/* Invoice Info */}
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="text-xs text-gray-500 uppercase">Фактура №</label>
                                        <p className="font-medium">{selectedInvoice.invoiceNumber || '-'}</p>
                                    </div>
                                    <div>
                                        <label className="text-xs text-gray-500 uppercase">Дата</label>
                                        <p className="font-medium">{formatDate(selectedInvoice.invoiceDate)}</p>
                                    </div>
                                </div>

                                {/* Counterparty Section */}
                                <div className="border-t pt-4">
                                    <div className="flex items-center justify-between mb-2">
                                        <label className="text-xs text-gray-500 uppercase">
                                            {selectedInvoice.direction === 'PURCHASE' ? 'Доставчик' : 'Клиент'}
                                        </label>
                                        {existingCounterpart ? (
                                            <span className="text-xs text-green-600">✓ В базата</span>
                                        ) : (
                                            <button
                                                onClick={openCounterpartModal}
                                                className="text-xs text-blue-600 hover:text-blue-800"
                                            >
                                                + Добави контрагент
                                            </button>
                                        )}
                                    </div>
                                    <p className="font-medium">
                                        {selectedInvoice.direction === 'PURCHASE'
                                            ? selectedInvoice.vendorName
                                            : selectedInvoice.customerName}
                                    </p>
                                    <p className="text-sm text-gray-600">
                                        ДДС: {selectedInvoice.direction === 'PURCHASE'
                                            ? selectedInvoice.vendorVatNumber || '-'
                                            : selectedInvoice.customerVatNumber || '-'}
                                    </p>
                                    {existingCounterpart && (
                                        <p className="text-xs text-green-600 mt-1">
                                            ID: {existingCounterpart.id} | {existingCounterpart.vatNumber || existingCounterpart.eik}
                                        </p>
                                    )}
                                </div>

                                {/* VIES Status */}
                                <div className="border-t pt-4">
                                    <div className="flex items-center justify-between">
                                        <label className="text-xs text-gray-500 uppercase">VIES валидация</label>
                                        {getViesStatusBadge(selectedInvoice.viesStatus)}
                                    </div>
                                    {selectedInvoice.viesValidationMessage && (
                                        <p className="text-sm text-gray-600 mt-1">{selectedInvoice.viesValidationMessage}</p>
                                    )}
                                    {selectedInvoice.viesCompanyName && (
                                        <p className="text-xs text-gray-500 mt-1">
                                            VIES: {selectedInvoice.viesCompanyName}
                                        </p>
                                    )}
                                    {(selectedInvoice.viesStatus === 'PENDING' || selectedInvoice.viesStatus === 'ERROR') && (
                                        <button
                                            onClick={() => handleValidateVies(selectedInvoice.id)}
                                            disabled={validating}
                                            className="mt-2 text-sm text-blue-600 hover:text-blue-800 disabled:opacity-50"
                                        >
                                            {validating ? 'Валидиране...' : '→ Валидирай с VIES'}
                                        </button>
                                    )}
                                </div>

                                {/* Amounts */}
                                <div className="border-t pt-4">
                                    <label className="text-xs text-gray-500 uppercase">Суми</label>
                                    <div className="mt-2 space-y-1">
                                        <div className="flex justify-between text-sm">
                                            <span>Данъчна основа:</span>
                                            <span>{formatCurrency(selectedInvoice.subtotal)}</span>
                                        </div>
                                        <div className="flex justify-between text-sm">
                                            <span>ДДС:</span>
                                            <span>{formatCurrency(selectedInvoice.totalTax)}</span>
                                        </div>
                                        <div className="flex justify-between font-medium">
                                            <span>Общо:</span>
                                            <span>{formatCurrency(selectedInvoice.invoiceTotal)}</span>
                                        </div>
                                    </div>
                                </div>

                                {/* Accounts Section */}
                                <div className="border-t pt-4">
                                    <label className="text-xs text-gray-500 uppercase mb-2 block">Сметки</label>

                                    {isEditing ? (
                                        <div className="space-y-3">
                                            <div>
                                                <label className="text-xs text-gray-600">Контрагент (401/411)</label>
                                                <select
                                                    value={editCounterpartyAccountId || ''}
                                                    onChange={(e) => setEditCounterpartyAccountId(e.target.value ? parseInt(e.target.value) : null)}
                                                    className="mt-1 block w-full rounded border-gray-300 text-sm"
                                                >
                                                    <option value="">-- Избери --</option>
                                                    {flatAccounts.filter(a => a.code.startsWith('4')).map(acc => (
                                                        <option key={acc.id} value={acc.id}>
                                                            {acc.code} {acc.name}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div>
                                                <label className="text-xs text-gray-600">ДДС (4531/4532)</label>
                                                <select
                                                    value={editVatAccountId || ''}
                                                    onChange={(e) => setEditVatAccountId(e.target.value ? parseInt(e.target.value) : null)}
                                                    className="mt-1 block w-full rounded border-gray-300 text-sm"
                                                >
                                                    <option value="">-- Избери --</option>
                                                    {flatAccounts.filter(a => a.code.startsWith('453')).map(acc => (
                                                        <option key={acc.id} value={acc.id}>
                                                            {acc.code} {acc.name}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div>
                                                <label className="text-xs text-gray-600">Разход/Приход (6xx/7xx)</label>
                                                <select
                                                    value={editExpenseRevenueAccountId || ''}
                                                    onChange={(e) => setEditExpenseRevenueAccountId(e.target.value ? parseInt(e.target.value) : null)}
                                                    className="mt-1 block w-full rounded border-gray-300 text-sm"
                                                >
                                                    <option value="">-- Избери --</option>
                                                    {flatAccounts.filter(a => a.code.startsWith('6') || a.code.startsWith('7')).map(acc => (
                                                        <option key={acc.id} value={acc.id}>
                                                            {acc.code} {acc.name}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div className="flex gap-2 pt-2">
                                                <button
                                                    onClick={() => setIsEditing(false)}
                                                    className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded hover:bg-gray-50"
                                                >
                                                    Отказ
                                                </button>
                                                <button
                                                    onClick={handleSaveAccounts}
                                                    disabled={updatingAccounts}
                                                    className="flex-1 px-3 py-2 text-sm text-white bg-blue-600 rounded hover:bg-blue-700 disabled:opacity-50"
                                                >
                                                    {updatingAccounts ? 'Запазване...' : 'Запази'}
                                                </button>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="space-y-2 text-sm">
                                            <div>
                                                <span className="text-gray-500">Контрагент: </span>
                                                {selectedInvoice.counterpartyAccount
                                                    ? <span className="text-gray-900">{selectedInvoice.counterpartyAccount.code} {selectedInvoice.counterpartyAccount.name}</span>
                                                    : <span className="text-yellow-600">Не е избрана</span>}
                                            </div>
                                            <div>
                                                <span className="text-gray-500">ДДС: </span>
                                                {selectedInvoice.vatAccount
                                                    ? <span className="text-gray-900">{selectedInvoice.vatAccount.code} {selectedInvoice.vatAccount.name}</span>
                                                    : <span className="text-yellow-600">Не е избрана</span>}
                                            </div>
                                            <div>
                                                <span className="text-gray-500">Разход/Приход: </span>
                                                {selectedInvoice.expenseRevenueAccount
                                                    ? <span className="text-gray-900">{selectedInvoice.expenseRevenueAccount.code} {selectedInvoice.expenseRevenueAccount.name}</span>
                                                    : <span className="text-yellow-600">Не е избрана</span>}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Posting Status - show when processed */}
                                {selectedInvoice.status === 'PROCESSED' && selectedInvoice.journalEntry && (
                                    <div className="border-t pt-4">
                                        <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                                            <div className="flex items-center gap-2 text-green-800">
                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                </svg>
                                                <span className="font-medium">Осчетоводено</span>
                                            </div>
                                            <div className="mt-2 text-sm">
                                                <span className="text-gray-600">Транзакция: </span>
                                                <span className="font-mono font-medium text-green-700">{selectedInvoice.journalEntry.entryNumber}</span>
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {/* Actions */}
                                {!isEditing && (
                                    <div className="border-t pt-4 flex gap-2">
                                        <button
                                            onClick={() => handleDelete(selectedInvoice.id)}
                                            className="flex-1 px-4 py-2 text-sm text-red-600 border border-red-300 rounded hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                            disabled={selectedInvoice.status === 'PROCESSED'}
                                        >
                                            Изтрий
                                        </button>
                                        <button
                                            className="flex-1 px-4 py-2 text-sm text-white bg-blue-600 rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                            onClick={handleProcessInvoice}
                                            disabled={selectedInvoice.status === 'PROCESSED' || isNavigating}
                                        >
                                            {isNavigating ? (
                                                <span className="flex items-center justify-center gap-2">
                                                    <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                                    </svg>
                                                    Зареждане...
                                                </span>
                                            ) : selectedInvoice.status === 'PROCESSED' ? 'Осчетоводено' : 'Осчетоводи'}
                                        </button>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="bg-white shadow rounded-lg p-6 text-center text-gray-500">
                                <p>Изберете фактура за преглед на детайли</p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Counterpart Modal */}
            {showCounterpartModal && selectedInvoice && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-lg font-medium">Добави контрагент</h3>
                            <button
                                onClick={() => setShowCounterpartModal(false)}
                                className="text-gray-400 hover:text-gray-600"
                            >
                                ✕
                            </button>
                        </div>

                        {/* Mode tabs */}
                        <div className="flex border-b mb-4">
                            <button
                                onClick={() => setCounterpartMode('existing')}
                                className={`px-4 py-2 text-sm ${counterpartMode === 'existing' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
                            >
                                От базата
                            </button>
                            <button
                                onClick={() => setCounterpartMode('vies')}
                                className={`px-4 py-2 text-sm ${counterpartMode === 'vies' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
                            >
                                Чрез VIES
                            </button>
                            <button
                                onClick={() => setCounterpartMode('manual')}
                                className={`px-4 py-2 text-sm ${counterpartMode === 'manual' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
                            >
                                Ръчно
                            </button>
                        </div>

                        {counterpartMode === 'existing' && (
                            <div>
                                <p className="text-sm text-gray-600 mb-3">Изберете съществуващ контрагент:</p>
                                <div className="max-h-64 overflow-y-auto border rounded">
                                    {counterparts.length === 0 ? (
                                        <p className="p-4 text-gray-500 text-sm">Няма контрагенти</p>
                                    ) : (
                                        counterparts.map(cp => (
                                            <div
                                                key={cp.id}
                                                className="p-3 border-b hover:bg-gray-50 cursor-pointer"
                                                onClick={() => {
                                                    alert(`Избран контрагент: ${cp.name}`);
                                                    setShowCounterpartModal(false);
                                                }}
                                            >
                                                <div className="font-medium">{cp.name}</div>
                                                <div className="text-xs text-gray-500">
                                                    {cp.vatNumber || cp.eik} | {cp.city}, {cp.country}
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        )}

                        {counterpartMode === 'vies' && (
                            <div>
                                <p className="text-sm text-gray-600 mb-3">
                                    Добави контрагент чрез VIES валидация на ДДС номер:
                                </p>
                                <div className="bg-gray-50 p-3 rounded mb-4">
                                    <div className="text-sm">
                                        <strong>ДДС номер:</strong> {selectedInvoice.direction === 'PURCHASE'
                                            ? selectedInvoice.vendorVatNumber || 'Липсва'
                                            : selectedInvoice.customerVatNumber || 'Липсва'}
                                    </div>
                                    <div className="text-sm">
                                        <strong>Име:</strong> {selectedInvoice.direction === 'PURCHASE'
                                            ? selectedInvoice.vendorName
                                            : selectedInvoice.customerName}
                                    </div>
                                </div>
                                {!(selectedInvoice.direction === 'PURCHASE' ? selectedInvoice.vendorVatNumber : selectedInvoice.customerVatNumber) ? (
                                    <p className="text-red-600 text-sm">Няма ДДС номер - използвайте ръчно добавяне</p>
                                ) : (
                                    <button
                                        onClick={handleAddCounterpartVies}
                                        disabled={validatingVat || creatingCounterpart}
                                        className="w-full py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                                    >
                                        {validatingVat || creatingCounterpart ? 'Обработка...' : 'Валидирай и добави'}
                                    </button>
                                )}
                            </div>
                        )}

                        {counterpartMode === 'manual' && (
                            <div className="space-y-3">
                                <p className="text-sm text-gray-600">
                                    Ръчно добавяне (за нерегистрирани по ДДС или от трети страни):
                                </p>
                                <div>
                                    <label className="text-xs text-gray-600">Име *</label>
                                    <input
                                        type="text"
                                        value={manualName}
                                        onChange={(e) => setManualName(e.target.value)}
                                        className="mt-1 block w-full rounded border-gray-300 text-sm"
                                        placeholder="Име на фирмата"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div>
                                        <label className="text-xs text-gray-600">ЕИК</label>
                                        <input
                                            type="text"
                                            value={manualEik}
                                            onChange={(e) => setManualEik(e.target.value)}
                                            className="mt-1 block w-full rounded border-gray-300 text-sm"
                                            placeholder="123456789"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-xs text-gray-600">ДДС номер</label>
                                        <input
                                            type="text"
                                            value={manualVatNumber}
                                            onChange={(e) => setManualVatNumber(e.target.value)}
                                            className="mt-1 block w-full rounded border-gray-300 text-sm"
                                            placeholder="BG123456789"
                                        />
                                    </div>
                                </div>
                                <div>
                                    <label className="text-xs text-gray-600">Адрес</label>
                                    <input
                                        type="text"
                                        value={manualAddress}
                                        onChange={(e) => setManualAddress(e.target.value)}
                                        className="mt-1 block w-full rounded border-gray-300 text-sm"
                                        placeholder="ул. Примерна 1"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div>
                                        <label className="text-xs text-gray-600">Град</label>
                                        <input
                                            type="text"
                                            value={manualCity}
                                            onChange={(e) => setManualCity(e.target.value)}
                                            className="mt-1 block w-full rounded border-gray-300 text-sm"
                                            placeholder="София"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-xs text-gray-600">Държава</label>
                                        <input
                                            type="text"
                                            value={manualCountry}
                                            onChange={(e) => setManualCountry(e.target.value)}
                                            className="mt-1 block w-full rounded border-gray-300 text-sm"
                                            placeholder="BG"
                                        />
                                    </div>
                                </div>
                                <button
                                    onClick={handleAddCounterpartManual}
                                    disabled={creatingCounterpart || !manualName.trim()}
                                    className="w-full py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 mt-4"
                                >
                                    {creatingCounterpart ? 'Добавяне...' : 'Добави контрагент'}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default ScannedInvoices;
