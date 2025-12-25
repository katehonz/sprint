import React, { useState, useCallback, useRef } from 'react';
import { useCompany } from '../contexts/CompanyContext';
import { useMutation } from '@apollo/client/react';
import { gql } from '@apollo/client';

interface AccountInfo {
    id: number;
    code: string;
    name: string;
}

interface SuggestedAccounts {
    counterpartyAccount: AccountInfo | null;
    vatAccount: AccountInfo | null;
    expenseOrRevenueAccount: AccountInfo | null;
}

interface RecognizedInvoice {
    // Vendor info
    vendorName: string;
    vendorVatNumber: string;
    vendorAddress: string;
    // Customer info
    customerName: string;
    customerVatNumber: string;
    customerAddress: string;
    // Invoice details
    invoiceId: string;
    invoiceDate: string;
    dueDate: string;
    // Amounts
    subtotal: number;
    totalTax: number;
    invoiceTotal: number;
    // Auto-detected
    direction: 'PURCHASE' | 'SALE' | 'UNKNOWN';
    validationStatus: 'PENDING' | 'VALID' | 'INVALID' | 'NOT_APPLICABLE' | 'MANUAL_REVIEW';
    viesValidationMessage: string;
    // Suggested accounts
    suggestedAccounts: SuggestedAccounts;
    // Manual review
    requiresManualReview: boolean;
    manualReviewReason: string;
}

const SAVE_SCANNED_INVOICE = gql`
    mutation SaveScannedInvoice($companyId: ID!, $recognized: RecognizedInvoiceInput!, $fileName: String) {
        saveScannedInvoice(companyId: $companyId, recognized: $recognized, fileName: $fileName) {
            id
            direction
            status
        }
    }
`;

type InvoiceType = 'purchase' | 'sales';

const MAX_FILE_SIZE_MB = 50; // 50MB за многостранични PDF файлове

const DocumentScanner: React.FC = () => {
    const { companyId } = useCompany();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const [data, setData] = useState<RecognizedInvoice | null>(null);

    const [file, setFile] = useState<File | null>(null);
    const [isDragOver, setIsDragOver] = useState(false);
    const [invoiceType, setInvoiceType] = useState<InvoiceType>('purchase');
    const [fileError, setFileError] = useState<string | null>(null);
    const [saveSuccess, setSaveSuccess] = useState(false);

    const [saveScannedInvoice, { loading: saving }] = useMutation(SAVE_SCANNED_INVOICE);

    const handleSave = async () => {
        if (!data || !companyId) return;

        try {
            await saveScannedInvoice({
                variables: {
                    companyId,
                    recognized: {
                        vendorName: data.vendorName,
                        vendorVatNumber: data.vendorVatNumber,
                        vendorAddress: data.vendorAddress,
                        customerName: data.customerName,
                        customerVatNumber: data.customerVatNumber,
                        customerAddress: data.customerAddress,
                        invoiceId: data.invoiceId,
                        invoiceDate: data.invoiceDate,
                        dueDate: data.dueDate,
                        subtotal: data.subtotal,
                        totalTax: data.totalTax,
                        invoiceTotal: data.invoiceTotal,
                        direction: data.direction,
                        validationStatus: data.validationStatus,
                        requiresManualReview: data.requiresManualReview,
                        manualReviewReason: data.manualReviewReason,
                        counterpartyAccountId: data.suggestedAccounts?.counterpartyAccount?.id,
                        vatAccountId: data.suggestedAccounts?.vatAccount?.id,
                        expenseRevenueAccountId: data.suggestedAccounts?.expenseOrRevenueAccount?.id,
                    },
                    fileName: file?.name,
                },
            });
            setSaveSuccess(true);
            setTimeout(() => setSaveSuccess(false), 3000);
        } catch (err) {
            setError(err instanceof Error ? err : new Error('Грешка при запазване'));
        }
    };

    const handleFileChange = async (selectedFile: File | null) => {
        setFileError(null);
        setError(null);
        setData(null);

        if (!companyId) {
            setFileError('Моля, изберете компания от менюто горе.');
            return;
        }

        if (!selectedFile) {
            return;
        }

        if (selectedFile.type !== 'application/pdf') {
            setFileError('Моля, изберете PDF файл.');
            return;
        }

        const fileSizeMB = selectedFile.size / (1024 * 1024);
        if (fileSizeMB > MAX_FILE_SIZE_MB) {
            setFileError(`Файлът е твърде голям (${fileSizeMB.toFixed(1)} MB). Максималният размер е ${MAX_FILE_SIZE_MB} MB.`);
            return;
        }

        setFile(selectedFile);
        setLoading(true);

        try {
            const formData = new FormData();
            formData.append('file', selectedFile);
            formData.append('invoiceType', invoiceType);
            formData.append('companyId', companyId);

            const token = localStorage.getItem('token');
            const response = await fetch('/api/scan-invoice', {
                method: 'POST',
                headers: {
                    'Authorization': token ? `Bearer ${token}` : '',
                },
                body: formData,
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || `HTTP error ${response.status}`);
            }

            const result: RecognizedInvoice = await response.json();
            setData(result);
        } catch (err) {
            setError(err instanceof Error ? err : new Error('Unknown error'));
        } finally {
            setLoading(false);
        }
    };

    const onDrop = useCallback((event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();
        setIsDragOver(false);
        const droppedFile = event.dataTransfer.files && event.dataTransfer.files[0];
        handleFileChange(droppedFile);
    }, [invoiceType, companyId]);

    const onDragOver = useCallback((event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();
        if (companyId) {
            setIsDragOver(true);
        }
    }, [companyId]);

    const onDragLeave = useCallback((event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();
        setIsDragOver(false);
    }, []);

    const onFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = event.target.files && event.target.files[0];
        handleFileChange(selectedFile);
    };

    const handleUploadClick = () => {
        if (companyId && fileInputRef.current) {
            fileInputRef.current.click();
        }
    };

    return (
        <div className="max-w-4xl mx-auto space-y-8">
            {/* Header */}
            <div className="text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 mb-4">
                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                </div>
                <h1 className="text-3xl font-bold text-gray-900">AI сканиране на фактури</h1>
                <p className="mt-2 text-gray-500 max-w-xl mx-auto">
                    Качете PDF фактури за автоматично извличане на данни чрез Azure Document Intelligence
                </p>
            </div>

            {/* Invoice Type Toggle */}
            <div className="flex justify-center">
                <div className="inline-flex p-1 rounded-xl bg-gray-100 shadow-inner">
                    <button
                        onClick={() => setInvoiceType('purchase')}
                        className={`px-6 py-2.5 text-sm font-semibold rounded-lg transition-all duration-200 ${
                            invoiceType === 'purchase'
                                ? 'bg-white text-blue-600 shadow-md'
                                : 'text-gray-600 hover:text-gray-900'
                        }`}
                    >
                        <span className="flex items-center gap-2">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                            </svg>
                            Фактури покупки
                        </span>
                    </button>
                    <button
                        onClick={() => setInvoiceType('sales')}
                        className={`px-6 py-2.5 text-sm font-semibold rounded-lg transition-all duration-200 ${
                            invoiceType === 'sales'
                                ? 'bg-white text-green-600 shadow-md'
                                : 'text-gray-600 hover:text-gray-900'
                        }`}
                    >
                        <span className="flex items-center gap-2">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            Фактури продажби
                        </span>
                    </button>
                </div>
            </div>

            {/* Upload Area */}
            <div
                onClick={handleUploadClick}
                className={`relative rounded-2xl p-8 text-center transition-all duration-300 cursor-pointer ${
                    !companyId
                        ? 'bg-gray-50 border-2 border-dashed border-gray-200 opacity-60 cursor-not-allowed'
                        : isDragOver
                            ? 'bg-blue-50 border-2 border-blue-400 shadow-lg shadow-blue-100'
                            : 'bg-gradient-to-b from-gray-50 to-white border-2 border-dashed border-gray-300 hover:border-blue-400 hover:shadow-lg hover:shadow-blue-50'
                }`}
                onDrop={onDrop}
                onDragOver={onDragOver}
                onDragLeave={onDragLeave}
            >
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="application/pdf"
                    onChange={onFileSelect}
                    className="hidden"
                    id="file-upload"
                    disabled={!companyId}
                />

                <div className="flex flex-col items-center justify-center py-6">
                    <div className={`w-20 h-20 rounded-full flex items-center justify-center mb-4 transition-all duration-300 ${
                        isDragOver
                            ? 'bg-blue-100 scale-110'
                            : 'bg-gray-100'
                    }`}>
                        <svg className={`w-10 h-10 transition-colors duration-300 ${isDragOver ? 'text-blue-500' : 'text-gray-400'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                        </svg>
                    </div>

                    {companyId ? (
                        <>
                            <p className="text-lg font-medium text-gray-700 mb-1">
                                Плъзнете файл тук или кликнете за избор
                            </p>
                            <p className="text-sm text-gray-500 mb-4">
                                Поддържа PDF файлове до {MAX_FILE_SIZE_MB} MB (до 10+ страници)
                            </p>
                            <button
                                type="button"
                                className="inline-flex items-center px-5 py-2.5 rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700 transition-colors shadow-md hover:shadow-lg"
                            >
                                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                                </svg>
                                Изберете PDF файл
                            </button>
                        </>
                    ) : (
                        <div className="text-center">
                            <p className="text-gray-500 mb-2">Моля, изберете компания от менюто горе</p>
                            <p className="text-xs text-gray-400">за да можете да качите файл</p>
                        </div>
                    )}

                    {file && !loading && (
                        <div className="mt-4 flex items-center gap-2 px-4 py-2 bg-green-50 rounded-lg border border-green-200">
                            <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span className="text-sm text-green-700 font-medium">{file.name}</span>
                            <span className="text-xs text-green-500">({(file.size / (1024 * 1024)).toFixed(2)} MB)</span>
                        </div>
                    )}
                </div>
            </div>

            {/* File Error */}
            {fileError && (
                <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-xl">
                    <svg className="w-5 h-5 text-red-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span className="text-sm text-red-700">{fileError}</span>
                </div>
            )}

            {/* Loading State */}
            {loading && (
                <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-8">
                    <div className="flex flex-col items-center justify-center">
                        <div className="relative w-16 h-16 mb-4">
                            <div className="absolute inset-0 rounded-full border-4 border-blue-100"></div>
                            <div className="absolute inset-0 rounded-full border-4 border-blue-500 border-t-transparent animate-spin"></div>
                        </div>
                        <p className="text-lg font-medium text-gray-700">Сканиране на документа...</p>
                        <p className="text-sm text-gray-500 mt-1">Извличане на данни чрез AI</p>
                    </div>
                </div>
            )}

            {/* API Error */}
            {error && (
                <div className="flex items-start gap-3 p-4 bg-red-50 border border-red-200 rounded-xl">
                    <svg className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <div>
                        <p className="font-medium text-red-700">Грешка при сканиране</p>
                        <p className="text-sm text-red-600 mt-1">{error.message}</p>
                    </div>
                </div>
            )}
            
            {/* Results */}
            {data && (
                <div className="bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden">
                    {/* Header with direction badge */}
                    <div className="p-6 border-b border-gray-100 bg-gradient-to-r from-green-50 to-emerald-50">
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
                                    <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>
                                <div>
                                    <h2 className="text-xl font-semibold text-gray-900">Извлечени данни</h2>
                                    <p className="text-sm text-gray-500">Успешно разпознати от AI</p>
                                </div>
                            </div>
                            {/* Direction badge */}
                            <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                                data.direction === 'PURCHASE' ? 'bg-blue-100 text-blue-700' :
                                data.direction === 'SALE' ? 'bg-green-100 text-green-700' :
                                'bg-yellow-100 text-yellow-700'
                            }`}>
                                {data.direction === 'PURCHASE' ? 'Покупка' :
                                 data.direction === 'SALE' ? 'Продажба' : 'Неопределено'}
                            </span>
                        </div>
                    </div>

                    {/* Manual review warning */}
                    {data.requiresManualReview && (
                        <div className="mx-6 mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                            <div className="flex gap-3">
                                <svg className="w-5 h-5 text-yellow-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                                <div>
                                    <p className="font-medium text-yellow-800">Необходима ръчна проверка</p>
                                    <p className="text-sm text-yellow-700">{data.manualReviewReason}</p>
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-5">
                        {/* Vendor section */}
                        <div className="md:col-span-2 pb-4 border-b border-gray-100">
                            <h3 className="text-sm font-semibold text-gray-700 mb-3">Доставчик (Продавач)</h3>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="space-y-1">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Име</label>
                                    <input type="text" readOnly value={data.vendorName || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm" />
                                </div>
                                <div className="space-y-1">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">ДДС номер</label>
                                    <input type="text" readOnly value={data.vendorVatNumber || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm font-mono" />
                                </div>
                                <div className="space-y-1">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Адрес</label>
                                    <input type="text" readOnly value={data.vendorAddress || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm" />
                                </div>
                            </div>
                        </div>

                        {/* Customer section */}
                        <div className="md:col-span-2 pb-4 border-b border-gray-100">
                            <h3 className="text-sm font-semibold text-gray-700 mb-3">Клиент (Купувач)</h3>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="space-y-1">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Име</label>
                                    <input type="text" readOnly value={data.customerName || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm" />
                                </div>
                                <div className="space-y-1">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">ДДС номер</label>
                                    <input type="text" readOnly value={data.customerVatNumber || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm font-mono" />
                                </div>
                                <div className="space-y-1">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Адрес</label>
                                    <input type="text" readOnly value={data.customerAddress || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm" />
                                </div>
                            </div>
                        </div>

                        {/* Invoice details */}
                        <div className="space-y-1">
                            <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Номер на фактура</label>
                            <input type="text" readOnly value={data.invoiceId || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm font-mono" />
                        </div>
                        <div className="space-y-1">
                            <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Дата на фактура</label>
                            <input type="text" readOnly value={data.invoiceDate || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm" />
                        </div>
                        <div className="space-y-1">
                            <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Краен срок</label>
                            <input type="text" readOnly value={data.dueDate || ''} className="block w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 sm:text-sm" />
                        </div>
                        <div className="space-y-1">
                            <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">VIES статус</label>
                            <div className={`px-4 py-2.5 rounded-lg border text-sm font-medium ${
                                data.validationStatus === 'VALID' ? 'bg-green-50 border-green-200 text-green-700' :
                                data.validationStatus === 'INVALID' ? 'bg-red-50 border-red-200 text-red-700' :
                                data.validationStatus === 'PENDING' ? 'bg-yellow-50 border-yellow-200 text-yellow-700' :
                                'bg-gray-50 border-gray-200 text-gray-700'
                            }`}>
                                {data.validationStatus === 'VALID' ? 'Валиден' :
                                 data.validationStatus === 'INVALID' ? 'Невалиден' :
                                 data.validationStatus === 'PENDING' ? 'Изчакващ валидация' :
                                 data.validationStatus === 'NOT_APPLICABLE' ? 'Неприложимо' : 'За проверка'}
                            </div>
                        </div>

                        {/* Amounts */}
                        <div className="md:col-span-2 pt-4 border-t border-gray-100">
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="bg-gray-50 rounded-xl p-4">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Данъчна основа</label>
                                    <p className="text-xl font-semibold text-gray-900">{data.subtotal?.toFixed(2) || '—'} лв.</p>
                                </div>
                                <div className="bg-gray-50 rounded-xl p-4">
                                    <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">ДДС</label>
                                    <p className="text-xl font-semibold text-gray-900">{data.totalTax?.toFixed(2) || '—'} лв.</p>
                                </div>
                                <div className="bg-blue-50 rounded-xl p-4 border border-blue-100">
                                    <label className="block text-xs font-medium text-blue-600 uppercase tracking-wide mb-1">Обща сума</label>
                                    <p className="text-2xl font-bold text-blue-700">{data.invoiceTotal?.toFixed(2) || '—'} лв.</p>
                                </div>
                            </div>
                        </div>

                        {/* Suggested accounts */}
                        {data.suggestedAccounts && (
                            <div className="md:col-span-2 pt-4 border-t border-gray-100">
                                <h3 className="text-sm font-semibold text-gray-700 mb-3">Предложени сметки</h3>
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                    {data.suggestedAccounts.counterpartyAccount && (
                                        <div className="bg-purple-50 rounded-xl p-4 border border-purple-100">
                                            <label className="block text-xs font-medium text-purple-600 uppercase tracking-wide mb-1">
                                                {data.direction === 'PURCHASE' ? 'Доставчици' : 'Клиенти'}
                                            </label>
                                            <p className="text-sm font-semibold text-purple-900">
                                                {data.suggestedAccounts.counterpartyAccount.code} - {data.suggestedAccounts.counterpartyAccount.name}
                                            </p>
                                        </div>
                                    )}
                                    {data.suggestedAccounts.vatAccount && (
                                        <div className="bg-orange-50 rounded-xl p-4 border border-orange-100">
                                            <label className="block text-xs font-medium text-orange-600 uppercase tracking-wide mb-1">ДДС сметка</label>
                                            <p className="text-sm font-semibold text-orange-900">
                                                {data.suggestedAccounts.vatAccount.code} - {data.suggestedAccounts.vatAccount.name}
                                            </p>
                                        </div>
                                    )}
                                    {data.suggestedAccounts.expenseOrRevenueAccount && (
                                        <div className="bg-teal-50 rounded-xl p-4 border border-teal-100">
                                            <label className="block text-xs font-medium text-teal-600 uppercase tracking-wide mb-1">
                                                {data.direction === 'PURCHASE' ? 'Разход' : 'Приход'}
                                            </label>
                                            <p className="text-sm font-semibold text-teal-900">
                                                {data.suggestedAccounts.expenseOrRevenueAccount.code} - {data.suggestedAccounts.expenseOrRevenueAccount.name}
                                            </p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Save success message */}
                    {saveSuccess && (
                        <div className="mx-6 mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2">
                            <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span className="text-green-700 font-medium">Фактурата е запазена успешно!</span>
                        </div>
                    )}

                    <div className="p-6 bg-gray-50 border-t border-gray-100 flex justify-end gap-3">
                        <button
                            onClick={handleSave}
                            disabled={saving}
                            className="inline-flex items-center px-5 py-2.5 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 transition-colors disabled:opacity-50"
                        >
                            {saving ? (
                                <>
                                    <svg className="animate-spin w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Запазване...
                                </>
                            ) : (
                                <>
                                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                                    </svg>
                                    Запази за обработка
                                </>
                            )}
                        </button>
                    </div>
                </div>
            )}

            {/* Azure Setup Instructions */}
            <div className="bg-gradient-to-br from-slate-50 to-blue-50 rounded-2xl border border-slate-200 overflow-hidden">
                <div className="p-6 border-b border-slate-200">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center">
                            <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M13.05 4.24L6.56 18.05c-.18.39-.56.63-.98.63h-.02c-.61-.01-1.09-.55-1.03-1.16l.66-6.94L.65 9.91a.993.993 0 0 1-.38-1.22c.13-.31.41-.54.75-.6l5.82-.99L10.78.87a1.01 1.01 0 0 1 1.37-.35c.35.2.56.57.56.97v2.75h.34zm9.64 6.74l-4.16 8.09c-.35.68-1.26.81-1.78.26l-5.31-5.61 3.78-7.35 7.47 4.61z"/>
                            </svg>
                        </div>
                        <div>
                            <h3 className="text-lg font-semibold text-gray-900">Настройка на Azure Document Intelligence</h3>
                            <p className="text-sm text-gray-500">Инструкции за конфигуриране на услугата</p>
                        </div>
                    </div>
                </div>
                <div className="p-6 space-y-6">
                    {/* Quick Setup Notice */}
                    <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                        <div className="flex gap-3">
                            <svg className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <div>
                                <p className="font-medium text-blue-800">Къде да въведете ключовете?</p>
                                <p className="text-sm text-blue-700 mt-1">
                                    Azure ключовете се въвеждат в <strong>Компании → AI Settings</strong> при създаване или редактиране на компания.
                                </p>
                                <a href="/companies" className="inline-flex items-center gap-1 mt-2 text-sm font-medium text-blue-600 hover:text-blue-800">
                                    Отиди към Компании
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                    </svg>
                                </a>
                            </div>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <div className="flex gap-4">
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-semibold text-sm">1</div>
                            <div>
                                <h4 className="font-medium text-gray-900">Създайте Azure акаунт</h4>
                                <p className="text-sm text-gray-600 mt-1">
                                    Отидете на <a href="https://portal.azure.com" target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline">portal.azure.com</a> и създайте безплатен акаунт, ако нямате такъв.
                                </p>
                            </div>
                        </div>
                        <div className="flex gap-4">
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-semibold text-sm">2</div>
                            <div>
                                <h4 className="font-medium text-gray-900">Създайте Document Intelligence ресурс</h4>
                                <p className="text-sm text-gray-600 mt-1">
                                    В Azure портала: <code className="bg-gray-100 px-1.5 py-0.5 rounded text-xs">Create a resource</code> → търсете "Document Intelligence" → <code className="bg-gray-100 px-1.5 py-0.5 rounded text-xs">Create</code>
                                </p>
                            </div>
                        </div>
                        <div className="flex gap-4">
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-semibold text-sm">3</div>
                            <div>
                                <h4 className="font-medium text-gray-900">Изберете ценови план</h4>
                                <p className="text-sm text-gray-600 mt-1">
                                    <strong>Free (F0)</strong>: 500 страници/месец безплатно<br/>
                                    <strong>Standard (S0)</strong>: $1.50 за 1000 страници (препоръчително за продукция)
                                </p>
                            </div>
                        </div>
                        <div className="flex gap-4">
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-semibold text-sm">4</div>
                            <div>
                                <h4 className="font-medium text-gray-900">Вземете ключовете</h4>
                                <p className="text-sm text-gray-600 mt-1">
                                    След създаването отидете на <code className="bg-gray-100 px-1.5 py-0.5 rounded text-xs">Keys and Endpoint</code> и копирайте:
                                </p>
                                <ul className="mt-2 text-sm text-gray-600 space-y-1">
                                    <li>• <strong>Endpoint</strong>: напр. <code className="bg-gray-100 px-1.5 py-0.5 rounded text-xs">https://your-resource.cognitiveservices.azure.com/</code></li>
                                    <li>• <strong>Key 1</strong> или <strong>Key 2</strong>: 32-символен API ключ</li>
                                </ul>
                            </div>
                        </div>
                        <div className="flex gap-4">
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-semibold text-sm">5</div>
                            <div>
                                <h4 className="font-medium text-gray-900">Въведете в настройките на компанията</h4>
                                <p className="text-sm text-gray-600 mt-1">
                                    Отидете в <strong>Компании</strong>, изберете вашата компания и въведете:
                                </p>
                                <div className="mt-3 bg-white rounded-lg border border-gray-200 p-4 space-y-3">
                                    <div>
                                        <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Azure Form Recognizer Endpoint</label>
                                        <code className="block mt-1 text-sm text-gray-700 bg-gray-50 px-3 py-2 rounded">https://your-resource.cognitiveservices.azure.com/</code>
                                    </div>
                                    <div>
                                        <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide">Azure Form Recognizer Key</label>
                                        <code className="block mt-1 text-sm text-gray-700 bg-gray-50 px-3 py-2 rounded">your-32-character-api-key-here</code>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
                        <div className="flex gap-3">
                            <svg className="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                            <div>
                                <p className="font-medium text-amber-800">Важни лимити на Azure</p>
                                <ul className="mt-1 text-sm text-amber-700 space-y-0.5">
                                    <li>• Максимален размер на файл: <strong>500 MB</strong></li>
                                    <li>• Максимален брой страници за един документ: <strong>2000</strong></li>
                                    <li>• Поддържани формати: PDF, JPEG, PNG, BMP, TIFF, HEIF</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DocumentScanner;
