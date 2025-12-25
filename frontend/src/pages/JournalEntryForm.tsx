import { useState, useEffect, useMemo } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { useNavigate, useParams } from 'react-router-dom';
import { useCompany } from '../contexts/CompanyContext';
import { GET_ACCOUNTS, GET_COMPANY, GET_JOURNAL_ENTRY, CREATE_JOURNAL_ENTRY, UPDATE_JOURNAL_ENTRY, GET_CURRENCIES, GET_COUNTERPARTS, CREATE_COUNTERPART, VALIDATE_VAT } from '../graphql/queries';

// --- Interfaces ---
interface Account {
  id: string;
  code: string;
  name: string;
  supportsQuantities: boolean;
  defaultUnit: string | null;
  isAnalytical: boolean;
}

interface Currency {
  id: string;
  code: string;
  name: string;
}

interface Counterpart {
  id: string;
  name: string;
  eik?: string;
  vatNumber?: string;
}

interface EntryLine {
  id?: string;
  accountId: string;
  description: string;
  side: 'debit' | 'credit';
  amount: string; // Стойност в базова валута
  currencyCode: string;
  currencyAmount: string; // Стойност в чуждата валута
  exchangeRate: string;
  materialQty: string; // Количество за материални сметки
  unitOfMeasure: string;
}

interface CompanyData {
  company: {
    id: string;
    baseCurrency: {
      id: string;
      code: string;
      symbol: string;
    };
  };
}

interface JournalEntryData {
  journalEntry: {
    documentDate: string;
    vatDate: string;
    accountingDate: string;
    description: string;
    documentNumber: string;
    documentType: string;
    vatDocumentType: string;
    vatPurchaseOperation: string;
    vatSalesOperation: string;
    counterpartId: number | null;
    counterpart: Counterpart | null;
    lines: {
        id: string;
        account: { id: string };
        counterpartId: number | null;
        description: string;
        debitAmount: number;
        creditAmount: number;
        currencyCode: string;
        quantity: number;
        exchangeRate: number;
    }[];
  };
}

// --- Helper Functions & Constants ---
const newEmptyLine = (baseCurrencyCode: string, side: 'debit' | 'credit' = 'debit'): EntryLine => ({
  accountId: '',
  description: '',
  side,
  amount: '',
  currencyCode: baseCurrencyCode,
  currencyAmount: '',
  exchangeRate: '1',
  materialQty: '',
  unitOfMeasure: '',
});

const DOCUMENT_TYPES = [
    { code: 'GENERIC', name: 'Общ документ' },
    { code: 'INVOICE', name: 'Фактура' },
    { code: 'DEBIT_NOTE', name: 'Дебитно известие' },
    { code: 'CREDIT_NOTE', name: 'Кредитно известие' },
    { code: 'CASH_PAYMENT_ORDER', name: 'Разходен касов ордер (РКО)' },
    { code: 'CASH_RECEIPT_ORDER', name: 'Приходен касов ордер (ПКО)' },
    { code: 'PAYROLL', name: 'Ведомост за заплати' },
    { code: 'BANK_STATEMENT', name: 'Банково бордеро' },
    { code: 'PROTOCOL', name: 'Протокол' },
    { code: 'OTHER', name: 'Други' },
];

// ДДС тип документ за НАП
const VAT_DOCUMENT_TYPES = [
    { code: '', name: '-- Без ДДС --' },
    { code: '01', name: '01 - Фактура' },
    { code: '02', name: '02 - Дебитно известие' },
    { code: '03', name: '03 - Кредитно известие' },
    { code: '07', name: '07 - Опростена фактура' },
    { code: '09', name: '09 - Протокол' },
    { code: '11', name: '11 - Митническа декларация' },
    { code: '12', name: '12 - Отчет продажби' },
    { code: '81', name: '81 - Документ внос услуги' },
    { code: '82', name: '82 - Документ износ услуги' },
];

// ДДС операции за покупки
const VAT_PURCHASE_OPERATIONS = [
    { code: '', name: '-- Без --' },
    { code: 'пок30', name: 'пок30 - Покупки с пълен ДК (20%)' },
    { code: 'пок32', name: 'пок32 - Покупки с пълен ДК (9%)' },
    { code: 'пок31', name: 'пок31 - Частичен ДК' },
    { code: 'пок40', name: 'пок40 - Без право на ДК' },
    { code: 'пок09', name: 'пок09 - ВОП стоки' },
    { code: 'пок10', name: 'пок10 - ВОП услуги' },
];

// ДДС операции за продажби
const VAT_SALES_OPERATIONS = [
    { code: '', name: '-- Без --' },
    { code: 'про11', name: 'про11 - Облагаеми 20%' },
    { code: 'про12', name: 'про12 - Облагаеми 9%' },
    { code: 'про13', name: 'про13 - 0% по гл. 3' },
    { code: 'про15', name: 'про15 - Износ' },
    { code: 'про16', name: 'про16 - ВОД' },
    { code: 'про14', name: 'про14 - Услуги чл. 21' },
    { code: 'про17', name: 'про17 - Доставки чл. 69' },
    { code: 'про19', name: 'про19 - Освободени' },
    { code: 'про20', name: 'про20 - Лични нужди' },
];

// --- Component ---
export default function JournalEntryForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { companyId } = useCompany();
  const isEdit = !!id;

  // --- State ---
  const [formData, setFormData] = useState({
    documentDate: new Date().toISOString().split('T')[0],
    vatDate: '',
    accountingDate: new Date().toISOString().split('T')[0],
    description: '',
    documentNumber: '',
    documentType: 'GENERIC',
    vatDocumentType: '',
    vatPurchaseOperation: '',
    vatSalesOperation: '',
  });
  const [lines, setLines] = useState<EntryLine[]>([]);
  const [error, setError] = useState('');
  const [expandedLine, setExpandedLine] = useState<number | null>(null);
  const [selectedCounterpart, setSelectedCounterpart] = useState<Counterpart | null>(null);

  // Counterpart modal state
  const [showCounterpartModal, setShowCounterpartModal] = useState(false);
  const [counterpartSearch, setCounterpartSearch] = useState('');
  const [showNewCounterpartForm, setShowNewCounterpartForm] = useState(false);
  const [newCounterpart, setNewCounterpart] = useState({
    name: '',
    eik: '',
    vatNumber: '',
    longAddress: '',
    city: '',
    country: 'България',
    counterpartType: 'CUSTOMER',
  });
  const [viesLoading, setViesLoading] = useState(false);
  const [viesResult, setViesResult] = useState<{ valid: boolean; name?: string; longAddress?: string; countryCode?: string; vatNumber?: string; errorMessage?: string; source?: string } | null>(null);

  // Account search modal state
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [accountSearchLineIndex, setAccountSearchLineIndex] = useState<number | null>(null);
  const [accountSearch, setAccountSearch] = useState('');

  // --- Data Fetching ---
  const { data: companyData, loading: loadingCompany } = useQuery<CompanyData>(GET_COMPANY, { variables: { id: companyId }, skip: !companyId });
  const { data: accountsData } = useQuery<{ accounts: Account[] }>(GET_ACCOUNTS, { variables: { companyId }, skip: !companyId });
  const { data: counterpartsData, refetch: refetchCounterparts } = useQuery<{ counterparts: Counterpart[] }>(GET_COUNTERPARTS, { variables: { companyId }, skip: !companyId });
  const { data: currenciesData } = useQuery<{ currencies: Currency[] }>(GET_CURRENCIES);
  const { data: entryData, loading: loadingEntry } = useQuery<JournalEntryData>(GET_JOURNAL_ENTRY, { variables: { id }, skip: !id });

  const [createEntry, { loading: creating }] = useMutation(CREATE_JOURNAL_ENTRY);
  const [updateEntry, { loading: updating }] = useMutation(UPDATE_JOURNAL_ENTRY);
  const [createCounterpart, { loading: creatingCounterpart }] = useMutation(CREATE_COUNTERPART);
  const [validateVat] = useMutation<any>(VALIDATE_VAT);

  // --- Memos ---
  const baseCurrency = useMemo(() => companyData?.company?.baseCurrency, [companyData]);
  const accounts = useMemo(() => accountsData?.accounts || [], [accountsData]);
  const counterparts = useMemo(() => counterpartsData?.counterparts || [], [counterpartsData]);
  const currencies = useMemo(() => currenciesData?.currencies || [], [currenciesData]);

  const filteredCounterparts = useMemo(() => {
    if (!counterpartSearch.trim()) return counterparts;
    const search = counterpartSearch.toLowerCase();
    return counterparts.filter(c =>
      c.name.toLowerCase().includes(search) ||
      c.eik?.toLowerCase().includes(search) ||
      c.vatNumber?.toLowerCase().includes(search)
    );
  }, [counterparts, counterpartSearch]);

  const filteredAccounts = useMemo(() => {
    if (!accountSearch.trim()) return accounts;
    const search = accountSearch.toLowerCase();
    return accounts.filter(a =>
      a.code.toLowerCase().includes(search) ||
      a.name.toLowerCase().includes(search)
    );
  }, [accounts, accountSearch]);

  // --- Effects ---
  useEffect(() => {
    if (baseCurrency && lines.length === 0 && !isEdit) {
      setLines([
        newEmptyLine(baseCurrency.code, 'debit'),
        newEmptyLine(baseCurrency.code, 'credit')
      ]);
    }
  }, [baseCurrency, lines.length, isEdit]);

  useEffect(() => {
    if (entryData?.journalEntry && baseCurrency) {
      const entry = entryData.journalEntry;
      setFormData({
        documentDate: entry.documentDate,
        vatDate: entry.vatDate || '',
        accountingDate: entry.accountingDate || entry.vatDate || entry.documentDate,
        description: entry.description,
        documentNumber: entry.documentNumber || '',
        documentType: entry.documentType || 'GENERIC',
        vatDocumentType: entry.vatDocumentType || '',
        vatPurchaseOperation: entry.vatPurchaseOperation || '',
        vatSalesOperation: entry.vatSalesOperation || '',
      });
      // Set counterpart from entry level (not line level)
      if (entry.counterpart) {
        setSelectedCounterpart(entry.counterpart);
      } else if (entry.counterpartId) {
        const cp = counterparts.find(c => c.id === String(entry.counterpartId));
        if (cp) setSelectedCounterpart(cp);
      }
      setLines(entry.lines.map((line) => {
        const account = accounts.find(a => a.id === line.account.id);
        const isForeign = line.currencyCode && line.currencyCode !== baseCurrency.code;
        return {
          id: line.id,
          accountId: line.account.id,
          description: line.description || '',
          side: (line.debitAmount > 0 ? 'debit' : 'credit') as 'debit' | 'credit',
          amount: (line.debitAmount > 0 ? line.debitAmount : line.creditAmount)?.toString() || '',
          currencyCode: line.currencyCode || baseCurrency.code,
          currencyAmount: isForeign ? line.quantity?.toString() || '' : '',
          exchangeRate: line.exchangeRate?.toString() || '1',
          materialQty: account?.supportsQuantities ? (line.quantity?.toString() || '') : '',
          unitOfMeasure: account?.defaultUnit || '',
        };
      }));
    }
  }, [entryData, baseCurrency, counterparts, accounts]);

  // --- Handlers ---
  const addLine = () => {
    if (baseCurrency) {
      const lastSide = lines.length > 0 ? lines[lines.length - 1].side : 'credit';
      const newSide = lastSide === 'debit' ? 'credit' : 'debit';
      setLines([...lines, newEmptyLine(baseCurrency.code, newSide)]);
    }
  };

  const removeLine = (index: number) => {
    if (lines.length > 1) {
      setLines(lines.filter((_, i) => i !== index));
    }
  };

  const updateLine = (index: number, field: keyof EntryLine, value: string | null) => {
    const newLines = [...lines];
    const line: EntryLine = { ...newLines[index], [field]: value };

    const account = accounts.find(a => a.id === line.accountId);
    const isMaterial = account?.supportsQuantities || false;
    const isForeign = line.currencyCode !== baseCurrency?.code;

    // При смяна на сметка
    if (field === 'accountId') {
      line.materialQty = isMaterial ? '' : '';
      line.unitOfMeasure = account?.defaultUnit || '';
    }

    // При смяна на валута към базова
    if (field === 'currencyCode' && value === baseCurrency?.code) {
      line.exchangeRate = '1';
      line.currencyAmount = '';
    }

    // Изчисляване на сумата при чужда валута
    if (isForeign && (field === 'currencyAmount' || field === 'exchangeRate')) {
      const currAmt = parseFloat(line.currencyAmount) || 0;
      const rate = parseFloat(line.exchangeRate) || 1;
      line.amount = (currAmt * rate).toFixed(2);
    }

    newLines[index] = line;
    setLines(newLines);
  };

  const openAccountSearch = (lineIndex: number) => {
    setAccountSearchLineIndex(lineIndex);
    setAccountSearch('');
    setShowAccountModal(true);
  };

  const selectAccount = (account: Account) => {
    if (accountSearchLineIndex !== null) {
      const newLines = [...lines];
      newLines[accountSearchLineIndex] = {
        ...newLines[accountSearchLineIndex],
        accountId: account.id,
        unitOfMeasure: account.defaultUnit || '',
        materialQty: account.supportsQuantities ? '' : '',
      };
      setLines(newLines);
    }
    setShowAccountModal(false);
    setAccountSearchLineIndex(null);
  };

  const handleSelectCounterpart = (counterpart: Counterpart) => {
    setSelectedCounterpart(counterpart);
    setShowCounterpartModal(false);
    setCounterpartSearch('');
  };

  const handleClearCounterpart = () => {
    setSelectedCounterpart(null);
  };

  const handleValidateVat = async () => {
    if (!newCounterpart.vatNumber || newCounterpart.vatNumber.length < 3) {
      setError('Въведете валиден ДДС номер (напр. BG123456789)');
      return;
    }

    setViesLoading(true);
    setError('');
    setViesResult(null);

    try {
      const { data: result } = await validateVat({
        variables: { vatNumber: newCounterpart.vatNumber }
      });

      if (result?.validateVat) {
        setViesResult(result.validateVat);

        if (result.validateVat.valid) {
          setNewCounterpart(prev => ({
            ...prev,
            name: result.validateVat.name || prev.name,
            longAddress: result.validateVat.longAddress || prev.longAddress,
            country: result.validateVat.countryCode === 'BG' ? 'България' : result.validateVat.countryCode || prev.country,
          }));

          if (result.validateVat.countryCode === 'BG' && result.validateVat.vatNumber) {
            const eik = result.validateVat.vatNumber.replace(/^BG/i, '');
            setNewCounterpart(prev => ({ ...prev, eik }));
          }
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Грешка при валидация');
    } finally {
      setViesLoading(false);
    }
  };

  const handleCreateCounterpart = async () => {
    if (!newCounterpart.name.trim()) return;

    try {
      const result = await createCounterpart({
        variables: {
          input: {
            companyId,
            name: newCounterpart.name,
            eik: newCounterpart.eik || null,
            vatNumber: newCounterpart.vatNumber || null,
            longAddress: newCounterpart.longAddress || null,
            city: newCounterpart.city || null,
            country: newCounterpart.country || null,
            counterpartType: newCounterpart.counterpartType,
            isVatRegistered: !!newCounterpart.vatNumber,
          }
        }
      });

      await refetchCounterparts();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      setSelectedCounterpart((result.data as any).createCounterpart);
      setShowNewCounterpartForm(false);
      setShowCounterpartModal(false);
      setNewCounterpart({
        name: '',
        eik: '',
        vatNumber: '',
        longAddress: '',
        city: '',
        country: 'България',
        counterpartType: 'CUSTOMER',
      });
      setViesResult(null);
      setCounterpartSearch('');
    } catch (err) {
      console.error('Error creating counterpart:', err);
    }
  };

  // Calculate totals
  const totalDebit = lines
    .filter(l => l.side === 'debit')
    .reduce((sum, line) => sum + (parseFloat(line.amount) || 0), 0);
  const totalCredit = lines
    .filter(l => l.side === 'credit')
    .reduce((sum, line) => sum + (parseFloat(line.amount) || 0), 0);
  const isBalanced = Math.abs(totalDebit - totalCredit) < 0.01;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!companyId || !baseCurrency) { setError('Не е избрана компания или базова валута.'); return; }
    if (!formData.description) { setError('Описанието е задължително.'); return; }
    if (lines.some(l => !l.accountId)) { setError('Всички редове трябва да имат сметка.'); return; }
    if (!isBalanced) { setError('Дебит и кредит трябва да са равни.'); return; }

    const counterpartId = selectedCounterpart ? parseInt(selectedCounterpart.id, 10) : null;

    const input = {
      companyId: companyId,
      documentDate: formData.documentDate,
      vatDate: formData.accountingDate, // ДДС датата е равна на счетоводната дата за записи извън ДДС дневника
      accountingDate: formData.accountingDate,
      description: formData.description,
      documentNumber: formData.documentNumber || null,
      documentType: formData.documentType,
      vatDocumentType: formData.vatDocumentType || null,
      vatPurchaseOperation: formData.vatPurchaseOperation || null,
      vatSalesOperation: formData.vatSalesOperation || null,
      lines: lines.map(line => {
        const account = accounts.find(a => a.id === line.accountId);
        const isMaterial = account?.supportsQuantities || false;
        const isForeign = line.currencyCode !== baseCurrency.code;
        const amount = parseFloat(line.amount) || 0;

        let quantity = null;
        if (isMaterial) {
          quantity = parseFloat(line.materialQty) || null;
        } else if (isForeign) {
          quantity = parseFloat(line.currencyAmount) || null;
        }

        return {
          accountId: line.accountId,
          counterpartId: counterpartId,
          description: line.description || null,
          debitAmount: line.side === 'debit' ? amount : 0,
          creditAmount: line.side === 'credit' ? amount : 0,
          currencyCode: isForeign ? line.currencyCode : null,
          quantity: quantity,
          exchangeRate: isForeign ? parseFloat(line.exchangeRate) : null,
          unitOfMeasureCode: line.unitOfMeasure || null,
        }
      }),
    };

    try {
      if (isEdit) {
        await updateEntry({ variables: { id, input } });
      } else {
        await createEntry({ variables: { input } });
      }
      navigate('/journal-entries');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при запазване.');
    }
  };

  const formatBaseCurrency = (amount: number) => new Intl.NumberFormat('bg-BG', { style: 'currency', currency: baseCurrency?.code || 'BGN' }).format(amount);

  // Calculate unit price for material accounts
  const getUnitPrice = (line: EntryLine): string => {
    const account = accounts.find(a => a.id === line.accountId);
    if (!account?.supportsQuantities) return '';
    const qty = parseFloat(line.materialQty) || 0;
    const amt = parseFloat(line.amount) || 0;
    if (qty === 0) return '';
    return (amt / qty).toFixed(4);
  };

  if (loadingCompany || loadingEntry) {
    return <div className="flex items-center justify-center h-64"><div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" /></div>;
  }

  return (
    <div className="space-y-6 pb-12">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{isEdit ? 'Редактиране на запис' : 'Нов журнален запис'}</h1>
          <p className="mt-1 text-sm text-gray-500">Основна валута: <strong>{baseCurrency?.code}</strong></p>
        </div>
        <button onClick={() => navigate('/journal-entries')} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">Назад</button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {error && <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">{error}</div>}

        {/* Основни данни */}
        <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Основни данни</h3>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Дата на документ *</label>
              <input type="date" value={formData.documentDate} onChange={(e) => setFormData({ ...formData, documentDate: e.target.value })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Счетоводна дата *</label>
              <input type="date" value={formData.accountingDate} onChange={(e) => setFormData({ ...formData, accountingDate: e.target.value })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Тип на документа</label>
              <select value={formData.documentType} onChange={(e) => setFormData({ ...formData, documentType: e.target.value })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm">
                {DOCUMENT_TYPES.map(doc => <option key={doc.code} value={doc.code}>{doc.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Номер на документ</label>
              <input type="text" value={formData.documentNumber} onChange={(e) => setFormData({ ...formData, documentNumber: e.target.value })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm" placeholder="Номер..." />
            </div>
          </div>
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">Описание *</label>
            <input type="text" value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm" placeholder="Покупка на стоки, плащане..." />
          </div>

          {/* ДДС секция */}
          <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <h4 className="text-sm font-medium text-blue-900 mb-3">ДДС данни за дневника</h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Тип документ (НАП)</label>
                <select value={formData.vatDocumentType} onChange={(e) => setFormData({ ...formData, vatDocumentType: e.target.value })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm">
                  {VAT_DOCUMENT_TYPES.map(doc => <option key={doc.code} value={doc.code}>{doc.name}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Операция покупки</label>
                <select value={formData.vatPurchaseOperation} onChange={(e) => setFormData({ ...formData, vatPurchaseOperation: e.target.value, vatSalesOperation: '' })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm">
                  {VAT_PURCHASE_OPERATIONS.map(op => <option key={op.code} value={op.code}>{op.name}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Операция продажби</label>
                <select value={formData.vatSalesOperation} onChange={(e) => setFormData({ ...formData, vatSalesOperation: e.target.value, vatPurchaseOperation: '' })} className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm">
                  {VAT_SALES_OPERATIONS.map(op => <option key={op.code} value={op.code}>{op.name}</option>)}
                </select>
              </div>
            </div>
            <p className="mt-2 text-xs text-blue-700">Попълнете за документи, които трябва да влязат в ДДС дневника. Изберете или покупка, или продажба.</p>
          </div>
        </div>

        {/* Контрагент */}
        <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-medium text-gray-900">Контрагент</h3>
            {selectedCounterpart && (
              <button type="button" onClick={handleClearCounterpart} className="text-sm text-red-600 hover:text-red-700">Премахни</button>
            )}
          </div>

          {selectedCounterpart ? (
            <div className="mt-2 flex items-center gap-3 p-2 bg-blue-50 rounded-lg border border-blue-200">
              <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 font-semibold text-sm">
                {selectedCounterpart.name.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1">
                <div className="font-medium text-gray-900 text-sm">{selectedCounterpart.name}</div>
                {selectedCounterpart.eik && <span className="text-xs text-gray-500">ЕИК: {selectedCounterpart.eik}</span>}
              </div>
              <button type="button" onClick={() => setShowCounterpartModal(true)} className="text-xs text-blue-600 hover:text-blue-700">Промени</button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setShowCounterpartModal(true)}
              className="mt-2 w-full flex items-center justify-center gap-2 p-3 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-blue-400 hover:text-blue-600 transition-colors text-sm"
            >
              + Избери контрагент
            </button>
          )}
        </div>

        {/* Редове */}
        <div className="bg-white shadow-sm rounded-lg border border-gray-200">
          <div className="flex items-center justify-between p-3 border-b">
            <h3 className="text-base font-medium text-gray-900">Редове</h3>
            <button type="button" onClick={addLine} className="px-3 py-1 text-sm font-medium text-blue-600 hover:text-blue-700">+ Ред</button>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full text-xs">
              <thead className="bg-gray-50">
                <tr>
                  <th className="p-1.5 text-left font-medium text-gray-500 w-14">Д/К</th>
                  <th className="p-1.5 text-left font-medium text-gray-500 min-w-[180px]">Сметка</th>
                  <th className="p-1.5 text-left font-medium text-gray-500 w-16">МЗ Кол.</th>
                  <th className="p-1.5 text-left font-medium text-gray-500 w-16">Ед.цена</th>
                  <th className="p-1.5 text-left font-medium text-gray-500 w-16">Валута</th>
                  <th className="p-1.5 text-left font-medium text-gray-500 w-16">Вал.ст.</th>
                  <th className="p-1.5 text-left font-medium text-gray-500 w-14">Курс</th>
                  <th className="p-1.5 text-right font-medium text-gray-500 w-24">Стойност</th>
                  <th className="p-1.5 w-12"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {lines.map((line, index) => {
                  const account = accounts.find(a => a.id === line.accountId);
                  const isMaterial = account?.supportsQuantities || false;
                  const isForeign = line.currencyCode !== baseCurrency?.code;
                  const unitPrice = getUnitPrice(line);

                  return (
                    <tr key={index} className={line.side === 'debit' ? 'bg-blue-50/30' : 'bg-green-50/30'}>
                      <td className="p-1">
                        <select
                          value={line.side}
                          onChange={(e) => updateLine(index, 'side', e.target.value)}
                          className={`block w-full rounded border-gray-300 shadow-sm text-xs font-medium ${line.side === 'debit' ? 'text-blue-700' : 'text-green-700'}`}
                        >
                          <option value="debit">Дт</option>
                          <option value="credit">Кт</option>
                        </select>
                      </td>
                      <td className="p-1">
                        <button
                          type="button"
                          onClick={() => openAccountSearch(index)}
                          className="block w-full text-left rounded border border-gray-300 bg-white px-2 py-1.5 text-xs hover:border-blue-400 truncate"
                        >
                          {account ? (
                            <span>
                              <span className="font-mono font-medium">{account.code}</span>
                              <span className="text-gray-500 ml-1">{account.name}</span>
                              {isMaterial && <span className="text-amber-600 ml-1">[{account.defaultUnit}]</span>}
                            </span>
                          ) : (
                            <span className="text-gray-400">Търси сметка...</span>
                          )}
                        </button>
                      </td>
                      <td className="p-1">
                        <input
                          type="number"
                          step="any"
                          value={line.materialQty}
                          onChange={(e) => updateLine(index, 'materialQty', e.target.value)}
                          disabled={!isMaterial}
                          className={`block w-full rounded border-gray-300 shadow-sm text-xs text-right ${!isMaterial ? 'bg-gray-100 text-gray-400' : ''}`}
                          placeholder={isMaterial ? 'Кол.' : '-'}
                        />
                      </td>
                      <td className="p-1">
                        <input
                          type="text"
                          value={unitPrice}
                          readOnly
                          disabled
                          className="block w-full rounded border-gray-300 bg-gray-50 text-xs text-right text-gray-500"
                          placeholder="-"
                        />
                      </td>
                      <td className="p-1">
                        <select
                          value={line.currencyCode}
                          onChange={(e) => updateLine(index, 'currencyCode', e.target.value)}
                          className="block w-full rounded border-gray-300 shadow-sm text-xs"
                        >
                          {currencies.map(c => <option key={c.id} value={c.code}>{c.code}</option>)}
                        </select>
                      </td>
                      <td className="p-1">
                        <input
                          type="number"
                          step="any"
                          value={line.currencyAmount}
                          onChange={(e) => updateLine(index, 'currencyAmount', e.target.value)}
                          disabled={!isForeign}
                          className={`block w-full rounded border-gray-300 shadow-sm text-xs text-right ${!isForeign ? 'bg-gray-100 text-gray-400' : ''}`}
                          placeholder={isForeign ? 'Сума' : '-'}
                        />
                      </td>
                      <td className="p-1">
                        <input
                          type="number"
                          step="any"
                          value={line.exchangeRate}
                          onChange={(e) => updateLine(index, 'exchangeRate', e.target.value)}
                          disabled={!isForeign}
                          className={`block w-full rounded border-gray-300 shadow-sm text-xs text-right ${!isForeign ? 'bg-gray-100 text-gray-400' : ''}`}
                        />
                      </td>
                      <td className="p-1">
                        <input
                          type="number"
                          step="0.01"
                          value={line.amount}
                          onChange={(e) => updateLine(index, 'amount', e.target.value)}
                          className="block w-full rounded border-gray-300 shadow-sm text-xs text-right font-medium"
                          placeholder="0.00"
                        />
                      </td>
                      <td className="p-1 text-center">
                        <button
                          type="button"
                          onClick={() => setExpandedLine(expandedLine === index ? null : index)}
                          className="text-gray-400 hover:text-blue-500 p-0.5"
                          title="Описание"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                            <path fillRule="evenodd" d="M4 5a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zm0 4a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zm1 3a1 1 0 100 2h10a1 1 0 100-2H5z" clipRule="evenodd" />
                          </svg>
                        </button>
                        {lines.length > 1 && (
                          <button type="button" onClick={() => removeLine(index)} className="text-red-400 hover:text-red-600 p-0.5" title="Изтрий">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                            </svg>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {lines.map((line, index) => expandedLine === index && (
                  <tr key={`desc-${index}`} className="bg-gray-50">
                    <td colSpan={9} className="p-1.5">
                      <input
                        type="text"
                        value={line.description}
                        onChange={(e) => updateLine(index, 'description', e.target.value)}
                        className="block w-full rounded border-gray-300 shadow-sm text-xs"
                        placeholder="Описание на реда..."
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="bg-gray-100 font-medium">
                  <td colSpan={7} className="px-3 py-2 text-right text-xs text-gray-700">
                    <span className="text-blue-700">Дт:</span> {formatBaseCurrency(totalDebit)}
                    <span className="mx-3">|</span>
                    <span className="text-green-700">Кт:</span> {formatBaseCurrency(totalCredit)}
                  </td>
                  <td className="px-3 py-2 text-right text-xs">
                    <span className={`font-bold ${isBalanced ? 'text-green-700' : 'text-red-700'}`}>
                      {isBalanced ? '✓' : `${formatBaseCurrency(Math.abs(totalDebit - totalCredit))}`}
                    </span>
                  </td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end space-x-3">
          <button type="button" onClick={() => navigate('/journal-entries')} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">Отказ</button>
          <button type="submit" disabled={creating || updating || !isBalanced} className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50">{creating || updating ? 'Запазване...' : isEdit ? 'Обнови' : 'Създай'}</button>
        </div>
      </form>

      {/* Account Search Modal */}
      {showAccountModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-lg shadow-2xl max-h-[80vh] flex flex-col">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">Избери сметка</h2>
              <button onClick={() => setShowAccountModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>

            <div className="p-3 border-b">
              <input
                type="text"
                value={accountSearch}
                onChange={(e) => setAccountSearch(e.target.value)}
                className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                placeholder="Търси по код или име..."
                autoFocus
              />
            </div>

            <div className="flex-1 overflow-y-auto">
              {filteredAccounts.length === 0 ? (
                <div className="p-8 text-center text-gray-500">Няма намерени сметки</div>
              ) : (
                <div className="divide-y divide-gray-100">
                  {filteredAccounts.map(account => (
                    <button
                      key={account.id}
                      type="button"
                      onClick={() => selectAccount(account)}
                      className="w-full flex items-center gap-3 p-3 hover:bg-gray-50 text-left"
                    >
                      <div className="font-mono text-sm font-medium text-gray-700 w-20">{account.code}</div>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm text-gray-900 truncate">{account.name}</div>
                        <div className="flex gap-2 text-xs">
                          {account.isAnalytical && <span className="text-cyan-600">Аналитична</span>}
                          {account.supportsQuantities && <span className="text-amber-600">[{account.defaultUnit}]</span>}
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Counterpart Modal */}
      {showCounterpartModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-lg shadow-2xl max-h-[80vh] flex flex-col">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">
                {showNewCounterpartForm ? 'Нов контрагент' : 'Избери контрагент'}
              </h2>
              <button
                onClick={() => {
                  setShowCounterpartModal(false);
                  setShowNewCounterpartForm(false);
                  setCounterpartSearch('');
                  setNewCounterpart({
                    name: '',
                    eik: '',
                    vatNumber: '',
                    longAddress: '',
                    city: '',
                    country: 'България',
                    counterpartType: 'CUSTOMER',
                  });
                  setViesResult(null);
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            {showNewCounterpartForm ? (
              <div className="p-4 space-y-4 overflow-y-auto max-h-[60vh]">
                {/* VIES Validation Result */}
                {viesResult && (
                  <div className={`rounded-lg p-3 text-sm ${
                    viesResult.valid
                      ? 'bg-green-50 border border-green-200 text-green-700'
                      : 'bg-yellow-50 border border-yellow-200 text-yellow-700'
                  }`}>
                    {viesResult.valid ? (
                      <div>
                        <div className="font-medium">Валиден ДДС номер ({viesResult.source})</div>
                        {viesResult.name && <div className="mt-1">Име: {viesResult.name}</div>}
                        {viesResult.longAddress && <div>Адрес: {viesResult.longAddress}</div>}
                      </div>
                    ) : (
                      <div className="font-medium">{viesResult.errorMessage || 'ДДС номерът не е валиден'}</div>
                    )}
                  </div>
                )}

                {/* ДДС номер с VIES бутон */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">ДДС номер</label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newCounterpart.vatNumber}
                      onChange={(e) => setNewCounterpart({ ...newCounterpart, vatNumber: e.target.value.toUpperCase() })}
                      className="block flex-1 rounded-md border-gray-300 shadow-sm sm:text-sm"
                      placeholder="BG123456789"
                    />
                    <button
                      type="button"
                      onClick={handleValidateVat}
                      disabled={viesLoading || !newCounterpart.vatNumber}
                      className="px-3 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 disabled:opacity-50 whitespace-nowrap"
                    >
                      {viesLoading ? 'Проверка...' : 'Провери VIES'}
                    </button>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    Въведете ДДС номер и натиснете "Провери VIES" за автоматично попълване
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Име *</label>
                  <input
                    type="text"
                    value={newCounterpart.name}
                    onChange={(e) => setNewCounterpart({ ...newCounterpart, name: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                    placeholder="Име на контрагента..."
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Тип</label>
                    <select
                      value={newCounterpart.counterpartType}
                      onChange={(e) => setNewCounterpart({ ...newCounterpart, counterpartType: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                    >
                      <option value="CUSTOMER">Клиент</option>
                      <option value="SUPPLIER">Доставчик</option>
                      <option value="BOTH">Клиент и Доставчик</option>
                      <option value="EMPLOYEE">Служител</option>
                      <option value="BANK">Банка</option>
                      <option value="GOVERNMENT">Държавна институция</option>
                      <option value="OTHER">Друг</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">ЕИК</label>
                    <input
                      type="text"
                      value={newCounterpart.eik}
                      onChange={(e) => setNewCounterpart({ ...newCounterpart, eik: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                      placeholder="123456789"
                    />
                  </div>
                </div>

                {/* Адрес от VIES */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Адрес от VIES</label>
                  <input
                    type="text"
                    value={newCounterpart.longAddress}
                    onChange={(e) => setNewCounterpart({ ...newCounterpart, longAddress: e.target.value })}
                    className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm bg-gray-50"
                    placeholder="Попълва се автоматично от VIES"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Град</label>
                    <input
                      type="text"
                      value={newCounterpart.city}
                      onChange={(e) => setNewCounterpart({ ...newCounterpart, city: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                      placeholder="София"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Държава</label>
                    <input
                      type="text"
                      value={newCounterpart.country}
                      onChange={(e) => setNewCounterpart({ ...newCounterpart, country: e.target.value })}
                      className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                      placeholder="България"
                    />
                  </div>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t">
                  <button
                    type="button"
                    onClick={() => {
                      setShowNewCounterpartForm(false);
                      setNewCounterpart({
                        name: '',
                        eik: '',
                        vatNumber: '',
                        longAddress: '',
                        city: '',
                        country: 'България',
                        counterpartType: 'CUSTOMER',
                      });
                      setViesResult(null);
                    }}
                    className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                  >
                    Назад
                  </button>
                  <button
                    type="button"
                    onClick={handleCreateCounterpart}
                    disabled={!newCounterpart.name.trim() || creatingCounterpart}
                    className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50"
                  >
                    {creatingCounterpart ? 'Създаване...' : 'Създай контрагент'}
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="p-3 border-b">
                  <input
                    type="text"
                    value={counterpartSearch}
                    onChange={(e) => setCounterpartSearch(e.target.value)}
                    className="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm"
                    placeholder="Търси по име, ЕИК или ДДС..."
                    autoFocus
                  />
                </div>

                <div className="flex-1 overflow-y-auto">
                  {filteredCounterparts.length === 0 ? (
                    <div className="p-8 text-center text-gray-500">{counterpartSearch ? 'Няма намерени' : 'Няма контрагенти'}</div>
                  ) : (
                    <div className="divide-y divide-gray-100">
                      {filteredCounterparts.map(counterpart => (
                        <button
                          key={counterpart.id}
                          type="button"
                          onClick={() => handleSelectCounterpart(counterpart)}
                          className="w-full flex items-center gap-3 p-3 hover:bg-gray-50 text-left"
                        >
                          <div className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center text-gray-600 font-semibold text-sm flex-shrink-0">
                            {counterpart.name.charAt(0).toUpperCase()}
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="font-medium text-gray-900 text-sm truncate">{counterpart.name}</div>
                            {counterpart.eik && <div className="text-xs text-gray-500">ЕИК: {counterpart.eik}</div>}
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                <div className="p-3 border-t bg-gray-50">
                  <button
                    type="button"
                    onClick={() => {
                      setShowNewCounterpartForm(true);
                      setNewCounterpart({
                        name: counterpartSearch,
                        eik: '',
                        vatNumber: '',
                        longAddress: '',
                        city: '',
                        country: 'България',
                        counterpartType: 'CUSTOMER',
                      });
                    }}
                    className="w-full flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-md"
                  >
                    + Създай нов контрагент
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
