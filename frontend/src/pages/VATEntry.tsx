import { useState, useEffect, useMemo } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { useCompany } from '../contexts/CompanyContext';
import {
  GET_ACCOUNTS,
  GET_COUNTERPARTS,
  GET_CURRENCIES,
  GET_LATEST_EXCHANGE_RATE,
  GET_COMPANY,
  GET_JOURNAL_ENTRY,
  CREATE_JOURNAL_ENTRY,
  UPDATE_JOURNAL_ENTRY,
  CREATE_COUNTERPART,
  VALIDATE_VAT,
} from '../graphql/queries';

// Bulgarian VAT document types per ЗДДС
const VAT_DOCUMENT_TYPES = [
  { code: '01', name: 'Фактура' },
  { code: '02', name: 'Дебитно известие' },
  { code: '03', name: 'Кредитно известие' },
  { code: '04', name: 'Протокол по чл.117' },
  { code: '05', name: 'Известие към протокол по чл.117' },
  { code: '07', name: 'Митническа декларация' },
  { code: '08', name: 'Дебитно известие към МД' },
  { code: '09', name: 'Кредитно известие към МД' },
  { code: '11', name: 'Фактура - Loss' },
  { code: '12', name: 'Дебитно известие - Loss' },
  { code: '13', name: 'Кредитно известие - Loss' },
  { code: '81', name: 'Отчет за продажби' },
  { code: '82', name: 'Отчет за покупки' },
  { code: '91', name: 'Протокол за ВОП' },
  { code: '92', name: 'Протокол за услуга от ЕС' },
  { code: '93', name: 'Протокол за тристранна операция' },
  { code: '94', name: 'Протокол по чл.151в' },
  { code: '95', name: 'Документ по чл.112' },
];

// VAT purchase operations (покупки) - for VAT ledger
const VAT_PURCHASE_OPERATIONS = [
  { code: 'пок09', name: 'кл.09 - ВОП стоки', description: 'Вътреобщностни придобивания на стоки' },
  { code: 'пок10', name: 'кл.10 - ВОП 3-странни', description: 'ВОП като посредник в тристранна операция' },
  { code: 'пок12', name: 'кл.12 - Услуги от ЕС', description: 'Получени услуги от ЕС (чл.82)' },
  { code: 'пок14', name: 'кл.14 - Внос', description: 'Внос по чл.56' },
  { code: 'пок15', name: 'кл.15 - Стоки с монтаж', description: 'Стоки с монтаж по чл.13' },
  { code: 'пок20', name: 'кл.20 - Данъчна основа', description: 'Данъчна основа на получените доставки' },
  { code: 'пок30', name: 'кл.30 - 20%', description: 'Покупки с право на ДК 20%' },
  { code: 'пок31', name: 'кл.31 - ДДС 20%', description: 'ДДС с право на данъчен кредит 20%' },
  { code: 'пок32', name: 'кл.32 - 9%', description: 'Покупки с право на ДК 9%' },
  { code: 'пок33', name: 'кл.33 - ДДС 9%', description: 'ДДС с право на данъчен кредит 9%' },
  { code: 'пок40', name: 'кл.40 - Без право ДК', description: 'Покупки без право на данъчен кредит' },
  { code: 'пок41', name: 'кл.41 - ДДС без ДК', description: 'ДДС без право на данъчен кредит' },
];

// VAT sales operations (продажби) - for VAT ledger
const VAT_SALES_OPERATIONS = [
  { code: 'про11', name: 'кл.11 - ДО 20%', description: 'Данъчна основа на облагаемите доставки 20%' },
  { code: 'про12', name: 'кл.12 - ДО 9%', description: 'Данъчна основа на облагаемите доставки 9%' },
  { code: 'про13', name: 'кл.13 - ДО 0%', description: 'Данъчна основа на облагаемите доставки 0%' },
  { code: 'про14', name: 'кл.14 - Услуги чл.21', description: 'Доставки на услуги по чл.21' },
  { code: 'про15', name: 'кл.15 - Износ', description: 'Износ и доставки към трети страни' },
  { code: 'про16', name: 'кл.16 - ВОД', description: 'Вътреобщностни доставки' },
  { code: 'про17', name: 'кл.17 - 3-странни ВОД', description: 'ВОД като посредник в тристранна операция' },
  { code: 'про18', name: 'кл.18 - Дистанционни', description: 'Дистанционни продажби' },
  { code: 'про19', name: 'кл.19 - Освободени', description: 'Освободени доставки' },
  { code: 'про20', name: 'кл.20 - Начислен ДДС 20%', description: 'Начислен данък 20%' },
  { code: 'про21', name: 'кл.21 - Начислен ДДС 9%', description: 'Начислен данък 9%' },
  { code: 'про23', name: 'кл.23 - Лични нужди', description: 'Безвъзмездни доставки за лични нужди' },
  { code: 'про24', name: 'кл.24 - Режим марж', description: 'Доставки по режим на маржа' },
  { code: 'про25', name: 'кл.25 - Туристически', description: 'Туристически услуги' },
];

interface Account {
  id: string;
  code: string;
  name: string;
  accountType: string;
  isAnalytical: boolean;
  supportsQuantities?: boolean;
  defaultUnit?: string;
}

interface Counterpart {
  id: string;
  name: string;
  eik: string;
  vatNumber: string;
  city: string;
  country: string;
  isVatRegistered: boolean;
}

interface Currency {
  id: string;
  code: string;
  name: string;
  nameBg: string;
  symbol: string;
  isBaseCurrency: boolean;
}

interface EntryLine {
  id?: string;
  accountId: string;
  description: string;
  side: 'debit' | 'credit';
  amount: string;
  currencyCode: string;
  currencyAmount: string;
  exchangeRate: string;
  materialQty: string;
  unitOfMeasure: string;
}

interface VatOperation {
  documentType: string;
  documentNumber: string;
  documentDate: string;
  vatDate: string;
  accountingDate: string;
  counterpartId: string;
  vatDirection: 'PURCHASE' | 'SALE';
  vatOperationCode: string;
  currency: string;
  exchangeRate: string;
  baseAmount: string;
  vatRate: string;
  vatAmount: string;
  totalAmount: string;
  description: string;
  reference: string;
}

const newEmptyLine = (currencyCode: string, side: 'debit' | 'credit' = 'debit'): EntryLine => ({
  accountId: '',
  description: '',
  side,
  amount: '',
  currencyCode,
  currencyAmount: '',
  exchangeRate: '1',
  materialQty: '',
  unitOfMeasure: '',
});

// Interface for scanned invoice data passed via navigation state
interface ScannedInvoiceData {
  scannedInvoiceId: number;
  direction: 'PURCHASE' | 'SALE';
  documentNumber: string;
  documentDate: string;
  baseAmount: string;
  vatAmount: string;
  totalAmount: string;
  counterpartId: string;
  counterpartName: string;
  counterpartVatNumber: string;
  counterpartyAccountId: string;
  vatAccountId: string;
  expenseRevenueAccountId: string;
}

export default function VATEntry() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  const { companyId } = useCompany();
  const isEdit = !!id;

  // Get scanned invoice data if navigated from ScannedInvoices
  const scannedInvoiceData = location.state?.fromScannedInvoice as ScannedInvoiceData | undefined;

  const [activeTab, setActiveTab] = useState<'vat' | 'payment'>('vat');
  const [showCounterpartModal, setShowCounterpartModal] = useState(false);
  const [searchCounterpart, setSearchCounterpart] = useState('');
  const [loadedFromScannedInvoice, setLoadedFromScannedInvoice] = useState(false);
  const [error, setError] = useState('');
  const [expandedLine, setExpandedLine] = useState<number | null>(null);

  // New counterpart form state
  const [showNewCounterpartForm, setShowNewCounterpartForm] = useState(false);
  const [newCounterpart, setNewCounterpart] = useState({
    name: '',
    eik: '',
    vatNumber: '',
    longAddress: '',
    city: '',
    country: 'България',
    counterpartType: 'SUPPLIER',
  });
  const [viesLoading, setViesLoading] = useState(false);
  const [viesResult, setViesResult] = useState<{ valid: boolean; name?: string; longAddress?: string; countryCode?: string; vatNumber?: string; errorMessage?: string; source?: string } | null>(null);

  // Account search modal state
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [accountSearchLineIndex, setAccountSearchLineIndex] = useState<number | null>(null);
  const [accountSearch, setAccountSearch] = useState('');
  const [isPaymentAccountSearch, setIsPaymentAccountSearch] = useState(false);

  // VAT Operation state
  const [vatOperation, setVatOperation] = useState<VatOperation>({
    documentType: '01',
    documentNumber: '',
    documentDate: new Date().toISOString().split('T')[0],
    vatDate: new Date().toISOString().split('T')[0],
    accountingDate: new Date().toISOString().split('T')[0],
    counterpartId: '',
    vatDirection: 'PURCHASE',
    vatOperationCode: 'пок30',
    currency: 'EUR',
    exchangeRate: '1',
    baseAmount: '',
    vatRate: '20',
    vatAmount: '',
    totalAmount: '',
    description: '',
    reference: '',
  });

  // Selected counterpart display
  const [selectedCounterpart, setSelectedCounterpart] = useState<Counterpart | null>(null);

  // Accounting lines
  const [lines, setLines] = useState<EntryLine[]>([
    newEmptyLine('EUR', 'debit'),
    newEmptyLine('EUR', 'credit'),
  ]);

  // Payment lines (for payment tab)
  const [paymentLines, setPaymentLines] = useState<EntryLine[]>([
    newEmptyLine('EUR', 'debit'),
    newEmptyLine('EUR', 'credit'),
  ]);

  // Queries
  const { data: companyData } = useQuery<any>(GET_COMPANY, {
    variables: { id: companyId },
    skip: !companyId,
  });

  const { data: accountsData } = useQuery<any>(GET_ACCOUNTS, {
    variables: { companyId },
    skip: !companyId,
  });

  const { data: counterpartsData, refetch: refetchCounterparts } = useQuery<any>(GET_COUNTERPARTS, {
    variables: { companyId },
    skip: !companyId,
  });

  const { data: currenciesData } = useQuery<any>(GET_CURRENCIES);

  // Load existing entry for edit mode
  const { data: entryData, loading: loadingEntry } = useQuery<any>(GET_JOURNAL_ENTRY, {
    variables: { id },
    skip: !id,
  });

  const baseCurrency = companyData?.company?.baseCurrency?.code || 'EUR';

  const { data: exchangeRateData, refetch: refetchRate } = useQuery<any>(GET_LATEST_EXCHANGE_RATE, {
    variables: { fromCode: vatOperation.currency, toCode: baseCurrency },
    skip: vatOperation.currency === baseCurrency,
  });

  const [createEntry, { loading: creating }] = useMutation<any>(CREATE_JOURNAL_ENTRY);
  const [updateEntry, { loading: updating }] = useMutation<any>(UPDATE_JOURNAL_ENTRY);
  const [createCounterpart, { loading: creatingCounterpart }] = useMutation<any>(CREATE_COUNTERPART);
  const [validateVat] = useMutation<any>(VALIDATE_VAT);

  const accounts: Account[] = accountsData?.accounts || [];
  const counterparts: Counterpart[] = counterpartsData?.counterparts || [];
  const currencies: Currency[] = currenciesData?.currencies || [];

  // Filter accounts for search modal
  const filteredAccounts = useMemo(() => {
    if (!accountSearch.trim()) return accounts;
    const search = accountSearch.toLowerCase();
    return accounts.filter(a =>
      a.code.toLowerCase().includes(search) ||
      a.name.toLowerCase().includes(search)
    );
  }, [accounts, accountSearch]);

  // Update exchange rate when currency changes
  useEffect(() => {
    if (vatOperation.currency !== baseCurrency && exchangeRateData?.latestExchangeRate) {
      setVatOperation(prev => ({
        ...prev,
        exchangeRate: exchangeRateData.latestExchangeRate.rate.toString(),
      }));
    } else if (vatOperation.currency === baseCurrency) {
      setVatOperation(prev => ({ ...prev, exchangeRate: '1' }));
    }
  }, [vatOperation.currency, exchangeRateData, baseCurrency]);

  // Calculate VAT amount when base amount or rate changes
  // Skip initial calculation if data came from scanned invoice
  useEffect(() => {
    // Skip if just loaded from scanned invoice (one-time skip)
    if (loadedFromScannedInvoice) {
      setLoadedFromScannedInvoice(false);
      return;
    }

    const base = parseFloat(vatOperation.baseAmount) || 0;
    const rate = parseFloat(vatOperation.vatRate) || 0;
    const vat = (base * rate) / 100;
    const total = base + vat;

    setVatOperation(prev => ({
      ...prev,
      vatAmount: vat.toFixed(2),
      totalAmount: total.toFixed(2),
    }));
  }, [vatOperation.baseAmount, vatOperation.vatRate]);

  // Load existing entry data for edit mode
  useEffect(() => {
    if (entryData?.journalEntry && isEdit) {
      const entry = entryData.journalEntry;

      // Set counterpart
      if (entry.counterpart) {
        setSelectedCounterpart(entry.counterpart);
      }

      // Set VAT operation data
      setVatOperation({
        documentType: entry.documentType || '01',
        documentNumber: entry.documentNumber || '',
        documentDate: entry.documentDate,
        vatDate: entry.vatDate || entry.documentDate,
        accountingDate: entry.accountingDate,
        counterpartId: entry.counterpartId?.toString() || '',
        vatDirection: 'PURCHASE', // Default, could be determined from vatPurchaseOperation/vatSalesOperation
        vatOperationCode: 'пок30',
        currency: baseCurrency,
        exchangeRate: '1',
        baseAmount: '',
        vatRate: '20',
        vatAmount: entry.totalVatAmount?.toString() || '0',
        totalAmount: entry.totalAmount?.toString() || '0',
        description: entry.description || '',
        reference: '',
      });

      // Convert lines
      if (entry.lines && entry.lines.length > 0) {
        const convertedLines: EntryLine[] = entry.lines.map((line: any) => {
          const debit = parseFloat(line.debitAmount) || 0;
          const credit = parseFloat(line.creditAmount) || 0;
          const isForeign = line.currencyCode && line.currencyCode !== baseCurrency;

          return {
            id: line.id,
            accountId: line.accountId?.toString() || '',
            description: line.description || '',
            side: debit > 0 ? 'debit' as const : 'credit' as const,
            amount: (debit > 0 ? debit : credit).toFixed(2),
            currencyCode: line.currencyCode || baseCurrency,
            currencyAmount: isForeign && line.quantity ? line.quantity.toString() : '',
            exchangeRate: line.exchangeRate?.toString() || '1',
            materialQty: line.account?.supportsQuantities && line.quantity ? line.quantity.toString() : '',
            unitOfMeasure: line.unitOfMeasureCode || line.account?.defaultUnit || '',
          };
        });
        setLines(convertedLines);
      }
    }
  }, [entryData, isEdit, baseCurrency]);

  // Pre-fill VAT operation data from scanned invoice (runs immediately when data is available)
  useEffect(() => {
    if (scannedInvoiceData && !isEdit) {
      const direction = scannedInvoiceData.direction === 'PURCHASE' ? 'PURCHASE' : 'SALE';
      const defaultOperationCode = direction === 'PURCHASE' ? 'пок30' : 'про11';

      // Mark as loaded from scanned invoice to skip auto-calculation
      setLoadedFromScannedInvoice(true);

      // Set VAT operation data
      setVatOperation(prev => ({
        ...prev,
        vatDirection: direction,
        vatOperationCode: defaultOperationCode,
        documentNumber: scannedInvoiceData.documentNumber || '',
        documentDate: scannedInvoiceData.documentDate || prev.documentDate,
        vatDate: scannedInvoiceData.documentDate || prev.vatDate,
        accountingDate: scannedInvoiceData.documentDate || prev.accountingDate,
        baseAmount: scannedInvoiceData.baseAmount || '',
        vatAmount: scannedInvoiceData.vatAmount || '',
        totalAmount: scannedInvoiceData.totalAmount || '',
        description: `Фактура ${scannedInvoiceData.documentNumber} от ${scannedInvoiceData.counterpartName || 'контрагент'}`,
      }));

      // Generate accounting lines directly with pre-selected accounts
      const base = parseFloat(scannedInvoiceData.baseAmount) || 0;
      const vat = parseFloat(scannedInvoiceData.vatAmount) || 0;
      const total = parseFloat(scannedInvoiceData.totalAmount) || 0;

      // Get account IDs from scanned invoice
      const expenseRevenueAccId = scannedInvoiceData.expenseRevenueAccountId || '';
      const vatAccId = scannedInvoiceData.vatAccountId || '';
      const counterpartyAccId = scannedInvoiceData.counterpartyAccountId || '';

      if (direction === 'PURCHASE') {
        setLines([
          { accountId: expenseRevenueAccId, description: 'Разход по фактура', side: 'debit' as const, amount: base.toFixed(2), currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
          { accountId: vatAccId, description: 'ДДС за възстановяване', side: 'debit' as const, amount: vat.toFixed(2), currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
          { accountId: counterpartyAccId, description: 'Задължение към доставчик', side: 'credit' as const, amount: total.toFixed(2), currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
        ]);
      } else {
        setLines([
          { accountId: counterpartyAccId, description: 'Вземане от клиент', side: 'debit' as const, amount: total.toFixed(2), currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
          { accountId: expenseRevenueAccId, description: 'Приход от продажби', side: 'credit' as const, amount: base.toFixed(2), currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
          { accountId: vatAccId, description: 'ДДС за внасяне', side: 'credit' as const, amount: vat.toFixed(2), currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
        ]);
      }
    }
  }, [scannedInvoiceData, isEdit, baseCurrency]);

  // Set counterpart when counterparts are loaded (separate effect)
  useEffect(() => {
    if (scannedInvoiceData && !isEdit && counterparts.length > 0) {
      let counterpart: Counterpart | null = null;
      if (scannedInvoiceData.counterpartId) {
        counterpart = counterparts.find(c => c.id === scannedInvoiceData.counterpartId) || null;
      }
      if (!counterpart && scannedInvoiceData.counterpartVatNumber) {
        counterpart = counterparts.find(c => c.vatNumber === scannedInvoiceData.counterpartVatNumber) || null;
      }

      if (counterpart) {
        setSelectedCounterpart(counterpart);
        setVatOperation(prev => ({ ...prev, counterpartId: counterpart!.id }));
      }
    }
  }, [scannedInvoiceData, isEdit, counterparts]);

  // Get VAT operations based on direction
  const vatOperations = vatOperation.vatDirection === 'PURCHASE'
    ? VAT_PURCHASE_OPERATIONS
    : VAT_SALES_OPERATIONS;

  // Filter counterparts for modal
  const filteredCounterparts = useMemo(() => {
    if (!searchCounterpart) return counterparts;
    const search = searchCounterpart.toLowerCase();
    return counterparts.filter(c =>
      c.name.toLowerCase().includes(search) ||
      c.eik?.toLowerCase().includes(search) ||
      c.vatNumber?.toLowerCase().includes(search)
    );
  }, [counterparts, searchCounterpart]);

  const selectCounterpart = (counterpart: Counterpart) => {
    setSelectedCounterpart(counterpart);
    setVatOperation(prev => ({ ...prev, counterpartId: counterpart.id }));
    setShowCounterpartModal(false);
    setSearchCounterpart('');
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
          },
        },
      });
      if (result.data?.createCounterpart) {
        const created = result.data.createCounterpart;
        setSelectedCounterpart(created);
        setVatOperation(prev => ({ ...prev, counterpartId: created.id }));
        setShowNewCounterpartForm(false);
        setNewCounterpart({
          name: '',
          eik: '',
          vatNumber: '',
          longAddress: '',
          city: '',
          country: 'България',
          counterpartType: 'SUPPLIER',
        });
        setViesResult(null);
        setShowCounterpartModal(false);
        refetchCounterparts();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Грешка при създаване на контрагент');
    }
  };

  const updateLine = (index: number, field: keyof EntryLine, value: string, isPayment = false) => {
    const targetLines = isPayment ? paymentLines : lines;
    const setTargetLines = isPayment ? setPaymentLines : setLines;

    const newLines = [...targetLines];
    const line: EntryLine = { ...newLines[index], [field]: value };

    const isForeign = line.currencyCode !== baseCurrency;

    // При смяна на валута към базова
    if (field === 'currencyCode' && value === baseCurrency) {
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
    setTargetLines(newLines);
  };

  const addLine = (isPayment = false) => {
    const targetLines = isPayment ? paymentLines : lines;
    const setTargetLines = isPayment ? setPaymentLines : setLines;
    const lastSide = targetLines.length > 0 ? targetLines[targetLines.length - 1].side : 'credit';
    const newSide = lastSide === 'debit' ? 'credit' : 'debit';
    setTargetLines(prev => [...prev, newEmptyLine(baseCurrency, newSide)]);
  };

  const removeLine = (index: number, isPayment = false) => {
    const targetLines = isPayment ? paymentLines : lines;
    const setTargetLines = isPayment ? setPaymentLines : setLines;
    if (targetLines.length > 1) {
      setTargetLines(targetLines.filter((_, i) => i !== index));
    }
  };

  const openAccountSearch = (lineIndex: number, isPayment = false) => {
    setAccountSearchLineIndex(lineIndex);
    setIsPaymentAccountSearch(isPayment);
    setAccountSearch('');
    setShowAccountModal(true);
  };

  const selectAccount = (account: Account) => {
    if (accountSearchLineIndex !== null) {
      const targetLines = isPaymentAccountSearch ? paymentLines : lines;
      const setTargetLines = isPaymentAccountSearch ? setPaymentLines : setLines;
      const newLines = [...targetLines];
      newLines[accountSearchLineIndex] = {
        ...newLines[accountSearchLineIndex],
        accountId: account.id,
        unitOfMeasure: account.defaultUnit || '',
        materialQty: '',
      };
      setTargetLines(newLines);
    }
    setShowAccountModal(false);
    setAccountSearchLineIndex(null);
  };

  // Calculate unit price for material accounts
  const getUnitPrice = (line: EntryLine): string => {
    const account = accounts.find(a => a.id === line.accountId);
    if (!account?.supportsQuantities) return '';
    const qty = parseFloat(line.materialQty) || 0;
    const amt = parseFloat(line.amount) || 0;
    if (qty === 0) return '';
    return (amt / qty).toFixed(4);
  };

  // Calculate totals
  const calculateTotals = (targetLines: EntryLine[]) => {
    const totalDebit = targetLines.reduce((sum, line) => sum + (line.side === 'debit' ? (parseFloat(line.amount) || 0) : 0), 0);
    const totalCredit = targetLines.reduce((sum, line) => sum + (line.side === 'credit' ? (parseFloat(line.amount) || 0) : 0), 0);
    return { totalDebit, totalCredit, isBalanced: Math.abs(totalDebit - totalCredit) < 0.01 };
  };

  const vatTotals = calculateTotals(lines);
  const paymentTotals = calculateTotals(paymentLines);

  const formatCurrency = (amount: number, currency = 'EUR') => {
    return new Intl.NumberFormat('bg-BG', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!vatOperation.documentNumber) {
      setError('Номерът на документа е задължителен');
      return;
    }

    if (!isEdit && !vatOperation.counterpartId) {
      setError('Контрагентът е задължителен');
      return;
    }

    if (!isEdit && (!vatOperation.baseAmount || parseFloat(vatOperation.baseAmount) <= 0)) {
      setError('Данъчната основа е задължителна');
      return;
    }

    const validLines = lines.filter(l => l.accountId && l.amount);
    if (validLines.length < 2) {
      setError('Необходими са поне два счетоводни реда');
      return;
    }

    if (!vatTotals.isBalanced) {
      setError('Дебит и кредит трябва да са равни');
      return;
    }

    try {
      const docRate = parseFloat(vatOperation.exchangeRate) || 1;
      const input = {
        companyId,
        documentDate: vatOperation.documentDate,
        vatDate: vatOperation.vatDate,
        accountingDate: vatOperation.accountingDate,
        documentNumber: vatOperation.documentNumber,
        description: vatOperation.description || `${VAT_DOCUMENT_TYPES.find(t => t.code === vatOperation.documentType)?.name} ${vatOperation.documentNumber}`,
        vatDocumentType: vatOperation.documentType,
        counterpartId: vatOperation.counterpartId,
        totalAmount: parseFloat(vatOperation.totalAmount) * docRate,
        totalVatAmount: parseFloat(vatOperation.vatAmount) * docRate,
        // VAT operation codes for VAT ledgers
        vatPurchaseOperation: vatOperation.vatDirection === 'PURCHASE' ? vatOperation.vatOperationCode : null,
        vatSalesOperation: vatOperation.vatDirection === 'SALE' ? vatOperation.vatOperationCode : null,
        // Link to scanned invoice if coming from scanner
        scannedInvoiceId: scannedInvoiceData?.scannedInvoiceId || null,
        lines: validLines.map((line, idx) => {
          const amount = parseFloat(line.amount) || 0;
          const account = accounts.find(a => a.id === line.accountId);
          const isMaterial = account?.supportsQuantities || false;
          const isForeign = line.currencyCode !== baseCurrency;
          return {
            accountId: line.accountId,
            description: line.description || null,
            debitAmount: line.side === 'debit' ? amount : 0,
            creditAmount: line.side === 'credit' ? amount : 0,
            currencyCode: isForeign ? line.currencyCode : null,
            exchangeRate: isForeign ? parseFloat(line.exchangeRate) : null,
            quantity: isMaterial ? parseFloat(line.materialQty) || null : (isForeign ? parseFloat(line.currencyAmount) || null : null),
            unitOfMeasureCode: isMaterial ? line.unitOfMeasure || null : null,
            lineOrder: idx + 1,
          };
        }),
      };

      if (isEdit) {
        await updateEntry({ variables: { id, input } });
      } else {
        await createEntry({ variables: { input } });
      }
      navigate('/journal-entries');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Грешка при запазване');
    }
  };

  const isSaving = creating || updating;

  // Auto-generate accounting lines based on VAT operation
  const generateAccountingLines = () => {
    const base = parseFloat(vatOperation.baseAmount) || 0;
    const vat = parseFloat(vatOperation.vatAmount) || 0;
    const total = parseFloat(vatOperation.totalAmount) || 0;
    const rate = parseFloat(vatOperation.exchangeRate) || 1;
    const isForeign = vatOperation.currency !== baseCurrency;

    // Convert to base currency
    const baseInBase = (base * rate).toFixed(2);
    const vatInBase = (vat * rate).toFixed(2);
    const totalInBase = (total * rate).toFixed(2);

    if (vatOperation.vatDirection === 'PURCHASE') {
      // Purchase: Debit expense/asset + Debit VAT, Credit payable
      setLines([
        { accountId: '', description: 'Разход/Актив', side: 'debit' as const, amount: baseInBase, currencyCode: isForeign ? vatOperation.currency : baseCurrency, currencyAmount: isForeign ? base.toFixed(2) : '', exchangeRate: rate.toString(), materialQty: '', unitOfMeasure: '' },
        { accountId: '', description: 'ДДС за възстановяване', side: 'debit' as const, amount: vatInBase, currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
        { accountId: '', description: 'Задължение към доставчик', side: 'credit' as const, amount: totalInBase, currencyCode: isForeign ? vatOperation.currency : baseCurrency, currencyAmount: isForeign ? total.toFixed(2) : '', exchangeRate: rate.toString(), materialQty: '', unitOfMeasure: '' },
      ]);
    } else {
      // Sale: Debit receivable, Credit revenue + Credit VAT
      setLines([
        { accountId: '', description: 'Вземане от клиент', side: 'debit' as const, amount: totalInBase, currencyCode: isForeign ? vatOperation.currency : baseCurrency, currencyAmount: isForeign ? total.toFixed(2) : '', exchangeRate: rate.toString(), materialQty: '', unitOfMeasure: '' },
        { accountId: '', description: 'Приход от продажби', side: 'credit' as const, amount: baseInBase, currencyCode: isForeign ? vatOperation.currency : baseCurrency, currencyAmount: isForeign ? base.toFixed(2) : '', exchangeRate: rate.toString(), materialQty: '', unitOfMeasure: '' },
        { accountId: '', description: 'ДДС за внасяне', side: 'credit' as const, amount: vatInBase, currencyCode: baseCurrency, currencyAmount: '', exchangeRate: '1', materialQty: '', unitOfMeasure: '' },
      ]);
    }
  };

  // Loading state for edit mode
  if (isEdit && loadingEntry) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isEdit ? 'Редактиране на ДДС операция' : 'Нова ДДС операция'}
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Въвеждане на фактура с ДДС и автоматично осчетоводяване
          </p>
        </div>
        <button
          onClick={() => navigate('/journal-entries')}
          className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
        >
          Назад
        </button>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('vat')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'vat'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            ДДС Операция
          </button>
          <button
            onClick={() => setActiveTab('payment')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'payment'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Плащане
          </button>
        </nav>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
            {error}
          </div>
        )}

        {activeTab === 'vat' && (
          <>
            {/* VAT Direction Toggle */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <div className="flex items-center gap-4">
                <span className="text-sm font-medium text-gray-700">Тип операция:</span>
                <div className="flex rounded-md shadow-sm">
                  <button
                    type="button"
                    onClick={() => {
                      setVatOperation(prev => ({ ...prev, vatDirection: 'PURCHASE', vatOperationCode: 'пок30' }));
                    }}
                    className={`px-4 py-2 text-sm font-medium rounded-l-md border ${
                      vatOperation.vatDirection === 'PURCHASE'
                        ? 'bg-blue-600 text-white border-blue-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    Покупка
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setVatOperation(prev => ({ ...prev, vatDirection: 'SALE', vatOperationCode: 'про11' }));
                    }}
                    className={`px-4 py-2 text-sm font-medium rounded-r-md border-t border-r border-b ${
                      vatOperation.vatDirection === 'SALE'
                        ? 'bg-green-600 text-white border-green-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    Продажба
                  </button>
                </div>
              </div>
            </div>

            {/* Document Info */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Документ</h3>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Тип документ
                  </label>
                  <select
                    value={vatOperation.documentType}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, documentType: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  >
                    {VAT_DOCUMENT_TYPES.map(type => (
                      <option key={type.code} value={type.code}>
                        {type.code} - {type.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Номер на документ *
                  </label>
                  <input
                    type="text"
                    value={vatOperation.documentNumber}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, documentNumber: e.target.value }))}
                    onBlur={(e) => {
                      const val = e.target.value.replace(/\D/g, '');
                      if (val) {
                        setVatOperation(prev => ({ ...prev, documentNumber: val.padStart(10, '0') }));
                      }
                    }}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="0000000001"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ДДС операция
                  </label>
                  <select
                    value={vatOperation.vatOperationCode}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, vatOperationCode: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  >
                    {vatOperations.map(op => (
                      <option key={op.code} value={op.code} title={op.description}>
                        {op.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Референция
                  </label>
                  <input
                    type="text"
                    value={vatOperation.reference}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, reference: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    placeholder="Допълнителна референция"
                  />
                </div>
              </div>
            </div>

            {/* Triple Date */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Тройна дата</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Дата на документ *
                  </label>
                  <input
                    type="date"
                    value={vatOperation.documentDate}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, documentDate: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Дата за ДДС *
                  </label>
                  <input
                    type="date"
                    value={vatOperation.vatDate}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, vatDate: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  />
                  <p className="mt-1 text-xs text-gray-500">За ДДС дневници</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Счетоводна дата *
                  </label>
                  <input
                    type="date"
                    value={vatOperation.accountingDate}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, accountingDate: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Counterpart */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                {vatOperation.vatDirection === 'PURCHASE' ? 'Доставчик' : 'Клиент'}
              </h3>
              <div className="flex items-start gap-4">
                <div className="flex-1">
                  {selectedCounterpart ? (
                    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                      <div className="flex items-start justify-between">
                        <div>
                          <h4 className="font-medium text-gray-900">{selectedCounterpart.name}</h4>
                          <div className="mt-1 text-sm text-gray-600">
                            {selectedCounterpart.eik && <span className="mr-4">ЕИК: {selectedCounterpart.eik}</span>}
                            {selectedCounterpart.vatNumber && (
                              <span className="text-green-600">ДДС №: {selectedCounterpart.vatNumber}</span>
                            )}
                          </div>
                          <div className="text-sm text-gray-500">
                            {selectedCounterpart.city}, {selectedCounterpart.country}
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedCounterpart(null);
                            setVatOperation(prev => ({ ...prev, counterpartId: '' }));
                          }}
                          className="text-gray-400 hover:text-gray-600"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setShowCounterpartModal(true)}
                      className="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-blue-400 hover:text-blue-500 transition-colors"
                    >
                      + Избери контрагент
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Amounts */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Суми</h3>
                <button
                  type="button"
                  onClick={generateAccountingLines}
                  className="text-sm text-blue-600 hover:text-blue-700"
                >
                  Генерирай счетоводни редове
                </button>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Валута на документа
                  </label>
                  <select
                    value={vatOperation.currency}
                    onChange={(e) => {
                      setVatOperation(prev => ({ ...prev, currency: e.target.value }));
                      if (e.target.value !== baseCurrency) {
                        refetchRate({ fromCode: e.target.value, toCode: baseCurrency });
                      }
                    }}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  >
                    {currencies.map(c => (
                      <option key={c.code} value={c.code}>
                        {c.code} - {c.nameBg || c.name}
                      </option>
                    ))}
                  </select>
                </div>
                {vatOperation.currency !== baseCurrency && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Курс към {baseCurrency}
                    </label>
                    <input
                      type="number"
                      step="0.000001"
                      value={vatOperation.exchangeRate}
                      onChange={(e) => setVatOperation(prev => ({ ...prev, exchangeRate: e.target.value }))}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    />
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Данъчна основа *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={vatOperation.baseAmount}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, baseAmount: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm text-right"
                    placeholder="0.00"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ДДС ставка (%)
                  </label>
                  <select
                    value={vatOperation.vatRate}
                    onChange={(e) => setVatOperation(prev => ({ ...prev, vatRate: e.target.value }))}
                    className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  >
                    <option value="20">20%</option>
                    <option value="9">9%</option>
                    <option value="0">0%</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    ДДС сума
                  </label>
                  <input
                    type="text"
                    value={vatOperation.vatAmount}
                    readOnly
                    className="block w-full rounded-md border-gray-200 bg-gray-50 shadow-sm sm:text-sm text-right"
                  />
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-gray-200">
                <div className="flex justify-end items-center gap-4">
                  <span className="text-sm font-medium text-gray-500">Общо:</span>
                  <span className="text-2xl font-bold text-gray-900">
                    {formatCurrency(parseFloat(vatOperation.totalAmount) || 0, vatOperation.currency)}
                  </span>
                  {vatOperation.currency !== baseCurrency && (
                    <span className="text-lg text-gray-500">
                      = {formatCurrency((parseFloat(vatOperation.totalAmount) || 0) * (parseFloat(vatOperation.exchangeRate) || 1), baseCurrency)}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Description */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Описание
              </label>
              <input
                type="text"
                value={vatOperation.description}
                onChange={(e) => setVatOperation(prev => ({ ...prev, description: e.target.value }))}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                placeholder="Описание на операцията..."
              />
            </div>

            {/* Accounting Lines */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200">
              <div className="flex items-center justify-between p-3 border-b">
                <h3 className="text-base font-medium text-gray-900">Счетоводни редове</h3>
                <button type="button" onClick={() => addLine(false)} className="px-3 py-1 text-sm font-medium text-blue-600 hover:text-blue-700">+ Ред</button>
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
                      const isForeign = line.currencyCode !== baseCurrency;
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
                              onClick={() => openAccountSearch(index, false)}
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
                        <span className="text-blue-700">Дт:</span> {formatCurrency(vatTotals.totalDebit, baseCurrency)}
                        <span className="mx-3">|</span>
                        <span className="text-green-700">Кт:</span> {formatCurrency(vatTotals.totalCredit, baseCurrency)}
                      </td>
                      <td className="px-3 py-2 text-right text-xs">
                        <span className={`font-bold ${vatTotals.isBalanced ? 'text-green-700' : 'text-red-700'}`}>
                          {vatTotals.isBalanced ? '✓' : formatCurrency(Math.abs(vatTotals.totalDebit - vatTotals.totalCredit), baseCurrency)}
                        </span>
                      </td>
                      <td></td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </>
        )}

        {activeTab === 'payment' && (
          <>
            {/* Payment Info */}
            <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
                <div className="flex items-start">
                  <span className="text-2xl mr-3">💡</span>
                  <div>
                    <h3 className="text-sm font-medium text-yellow-900">Плащане</h3>
                    <p className="mt-1 text-sm text-yellow-700">
                      Тук можете да въведете плащането по фактурата. Плащането се записва като отделен журнален запис.
                    </p>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Редове за плащане</h3>
                <button
                  type="button"
                  onClick={() => addLine(true)}
                  className="px-3 py-1 text-sm font-medium text-blue-600 hover:text-blue-700"
                >
                  + Добави ред
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="min-w-full text-xs">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="p-1.5 text-left font-medium text-gray-500 w-14">Д/К</th>
                      <th className="p-1.5 text-left font-medium text-gray-500 min-w-[180px]">Сметка</th>
                      <th className="p-1.5 text-right font-medium text-gray-500 w-24">Стойност ({baseCurrency})</th>
                      <th className="p-1.5 w-12"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {paymentLines.map((line, index) => {
                      const account = accounts.find(a => a.id === line.accountId);

                      return (
                        <tr key={index} className={line.side === 'debit' ? 'bg-blue-50/30' : 'bg-green-50/30'}>
                          <td className="p-1">
                            <select
                              value={line.side}
                              onChange={(e) => updateLine(index, 'side', e.target.value, true)}
                              className={`block w-full rounded border-gray-300 shadow-sm text-xs font-medium ${line.side === 'debit' ? 'text-blue-700' : 'text-green-700'}`}
                            >
                              <option value="debit">Дт</option>
                              <option value="credit">Кт</option>
                            </select>
                          </td>
                          <td className="p-1">
                            <button
                              type="button"
                              onClick={() => openAccountSearch(index, true)}
                              className="block w-full text-left rounded border border-gray-300 bg-white px-2 py-1.5 text-xs hover:border-blue-400 truncate"
                            >
                              {account ? (
                                <span>
                                  <span className="font-mono font-medium">{account.code}</span>
                                  <span className="text-gray-500 ml-1">{account.name}</span>
                                </span>
                              ) : (
                                <span className="text-gray-400">Търси сметка...</span>
                              )}
                            </button>
                          </td>
                          <td className="p-1">
                            <input
                              type="number"
                              step="0.01"
                              value={line.amount}
                              onChange={(e) => updateLine(index, 'amount', e.target.value, true)}
                              className="block w-full rounded border-gray-300 shadow-sm text-xs text-right font-medium"
                              placeholder="0.00"
                            />
                          </td>
                          <td className="p-1 text-center">
                            {paymentLines.length > 1 && (
                              <button type="button" onClick={() => removeLine(index, true)} className="text-red-400 hover:text-red-600 p-0.5" title="Изтрий">
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                                </svg>
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                  <tfoot>
                    <tr className="bg-gray-100 font-medium">
                      <td colSpan={2} className="px-3 py-2 text-right text-xs text-gray-700">
                        <span className="text-blue-700">Дт:</span> {formatCurrency(paymentTotals.totalDebit, baseCurrency)}
                        <span className="mx-3">|</span>
                        <span className="text-green-700">Кт:</span> {formatCurrency(paymentTotals.totalCredit, baseCurrency)}
                      </td>
                      <td className="px-3 py-2 text-right text-xs">
                        <span className={`font-bold ${paymentTotals.isBalanced ? 'text-green-700' : 'text-red-700'}`}>
                          {paymentTotals.isBalanced ? '✓' : formatCurrency(Math.abs(paymentTotals.totalDebit - paymentTotals.totalCredit), baseCurrency)}
                        </span>
                      </td>
                      <td></td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </>
        )}

        {/* Actions */}
        <div className="flex justify-end space-x-3">
          <button
            type="button"
            onClick={() => navigate('/journal-entries')}
            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            Отказ
          </button>
          <button
            type="submit"
            disabled={isSaving || !vatTotals.isBalanced}
            className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
          >
            {isSaving ? 'Запазване...' : isEdit ? 'Обнови' : 'Създай операция'}
          </button>
        </div>
      </form>

      {/* Counterpart Selection Modal */}
      {showCounterpartModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-2xl max-h-[80vh] flex flex-col shadow-2xl">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900">Избор на контрагент</h2>
                <button
                  onClick={() => {
                    setShowCounterpartModal(false);
                    setSearchCounterpart('');
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>
              <div className="mt-4">
                <input
                  type="text"
                  value={searchCounterpart}
                  onChange={(e) => setSearchCounterpart(e.target.value)}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  placeholder="Търсене по име, ЕИК или ДДС номер..."
                  autoFocus
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {showNewCounterpartForm ? (
                <div className="space-y-4">
                  <h3 className="font-medium text-gray-900">Нов контрагент</h3>

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
                        onChange={(e) => setNewCounterpart(prev => ({ ...prev, vatNumber: e.target.value.toUpperCase() }))}
                        className="block flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
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
                      onChange={(e) => setNewCounterpart(prev => ({ ...prev, name: e.target.value }))}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      placeholder="Име на контрагента"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Тип</label>
                      <select
                        value={newCounterpart.counterpartType}
                        onChange={(e) => setNewCounterpart(prev => ({ ...prev, counterpartType: e.target.value }))}
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                      >
                        <option value="SUPPLIER">Доставчик</option>
                        <option value="CUSTOMER">Клиент</option>
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
                        onChange={(e) => setNewCounterpart(prev => ({ ...prev, eik: e.target.value }))}
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
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
                      onChange={(e) => setNewCounterpart(prev => ({ ...prev, longAddress: e.target.value }))}
                      className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm bg-gray-50"
                      placeholder="Попълва се автоматично от VIES"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Град</label>
                      <input
                        type="text"
                        value={newCounterpart.city}
                        onChange={(e) => setNewCounterpart(prev => ({ ...prev, city: e.target.value }))}
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                        placeholder="София"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Държава</label>
                      <input
                        type="text"
                        value={newCounterpart.country}
                        onChange={(e) => setNewCounterpart(prev => ({ ...prev, country: e.target.value }))}
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                        placeholder="България"
                      />
                    </div>
                  </div>

                  <div className="flex gap-3 pt-2 border-t">
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
                          counterpartType: 'SUPPLIER',
                        });
                        setViesResult(null);
                      }}
                      className="px-4 py-2 border border-gray-300 rounded-md text-sm text-gray-700 hover:bg-gray-50"
                    >
                      Отказ
                    </button>
                    <button
                      type="button"
                      onClick={handleCreateCounterpart}
                      disabled={!newCounterpart.name.trim() || creatingCounterpart}
                      className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
                    >
                      {creatingCounterpart ? 'Създаване...' : 'Създай контрагент'}
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  {filteredCounterparts.length === 0 ? (
                    <div className="text-center text-gray-500 py-8">
                      Няма намерени контрагенти
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {filteredCounterparts.map(counterpart => (
                        <button
                          key={counterpart.id}
                          type="button"
                          onClick={() => selectCounterpart(counterpart)}
                          className="w-full text-left p-4 rounded-lg border border-gray-200 hover:border-blue-300 hover:bg-blue-50 transition-colors"
                        >
                          <div className="flex items-start justify-between">
                            <div>
                              <h4 className="font-medium text-gray-900">{counterpart.name}</h4>
                              <div className="mt-1 text-sm text-gray-600">
                                {counterpart.eik && <span className="mr-4">ЕИК: {counterpart.eik}</span>}
                                {counterpart.vatNumber && (
                                  <span className="text-green-600">ДДС: {counterpart.vatNumber}</span>
                                )}
                              </div>
                              <div className="text-sm text-gray-500">
                                {counterpart.city}, {counterpart.country}
                              </div>
                            </div>
                            {counterpart.isVatRegistered && (
                              <span className="px-2 py-1 text-xs rounded-full bg-green-100 text-green-700">
                                Рег. по ДДС
                              </span>
                            )}
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>

            {/* Footer with New Counterpart button */}
            {!showNewCounterpartForm && (
              <div className="p-4 border-t border-gray-200 bg-gray-50">
                <button
                  type="button"
                  onClick={() => setShowNewCounterpartForm(true)}
                  className="w-full px-4 py-2 border-2 border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-blue-400 hover:text-blue-600 transition-colors"
                >
                  + Създай нов контрагент
                </button>
              </div>
            )}
          </div>
        </div>
      )}

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
    </div>
  );
}
