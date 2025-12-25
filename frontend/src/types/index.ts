export type RateProvider = 'ECB' | 'MANUAL' | 'API';
export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
export type VatDirection = 'NONE' | 'INPUT' | 'OUTPUT' | 'BOTH';
export type CounterpartType = 'CUSTOMER' | 'SUPPLIER' | 'EMPLOYEE' | 'BANK' | 'GOVERNMENT' | 'OTHER';
export type VatReturnStatus = 'DRAFT' | 'CALCULATED' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED';
export type BankImportFormat = 'UNICREDIT_MT940' | 'WISE_CAMT053' | 'REVOLUT_CAMT053' | 'PAYSERA_CAMT053' | 'POSTBANK_XML' | 'OBB_XML' | 'CCB_CSV';
export type BankImportStatus = 'COMPLETED' | 'FAILED' | 'IN_PROGRESS';
export type RateSource = 'ECB' | 'MANUAL' | 'API';
export type PeriodStatus = 'OPEN' | 'CLOSED';

export interface Company {
  id: string;
  name: string;
  eik: string;
  vatNumber?: string;
  address?: string;
  city?: string;
  country?: string;
  phone?: string;
  email?: string;
  contactPerson?: string;
  managerName?: string;
  authorizedPerson?: string;
  isActive: boolean;
  enableViesValidation: boolean;
  enableAiMapping: boolean;
  preferredRateProvider: RateProvider;
  createdAt: string;
  updatedAt: string;
}

export interface Account {
  id: string;
  code: string;
  name: string;
  accountType: AccountType;
  accountClass: number;
  parent?: Account;
  level: number;
  isVatApplicable: boolean;
  vatDirection: VatDirection;
  isActive: boolean;
  isAnalytical: boolean;
  companyId: number;
  supportsQuantities: boolean;
  defaultUnit?: string;
}

export interface EntryLine {
  id: string;
  journalEntryId: number;
  accountId: number;
  account?: Account;
  debitAmount: number;
  creditAmount: number;
  counterpartId?: number;
  currencyCode?: string;
  exchangeRate?: number;
  baseAmount: number;
  vatAmount: number;
  quantity?: number;
  unitOfMeasureCode?: string;
  description?: string;
  lineOrder: number;
}

export interface JournalEntry {
  id: string;
  entryNumber: string;
  documentDate: string;
  vatDate?: string;
  accountingDate: string;
  documentNumber?: string;
  description: string;
  totalAmount: number;
  totalVatAmount: number;
  isPosted: boolean;
  postedAt?: string;
  companyId: number;
  documentType?: string;
  lines?: EntryLine[];
  createdAt: string;
  updatedAt: string;
}

export interface Counterpart {
  id: string;
  name: string;
  eik?: string;
  vatNumber?: string;
  address?: string;
  longAddress?: string;
  city?: string;
  country?: string;
  counterpartType: CounterpartType;
  isVatRegistered: boolean;
  companyId: number;
  isActive: boolean;
}

export interface ViesValidationResult {
  valid: boolean;
  vatNumber?: string;
  countryCode?: string;
  name?: string;
  longAddress?: string;
  errorMessage?: string;
  source?: string;
}

export interface Currency {
  id: string;
  code: string;
  name: string;
  nameBg: string;
  symbol?: string;
  decimalPlaces: number;
  isActive: boolean;
  isBaseCurrency: boolean;
}

export interface ExchangeRate {
  id: string;
  fromCurrency: Currency;
  toCurrency: Currency;
  rate: number;
  reverseRate: number;
  validDate: string;
  rateSource: RateSource;
  isActive: boolean;
}

export interface VatRate {
  id: string;
  code: string;
  name: string;
  rate: number;
  vatDirection: VatDirection;
  isActive: boolean;
  validFrom: string;
  validTo?: string;
  companyId: number;
}

export interface VatReturn {
  id: string;
  periodYear: number;
  periodMonth: number;
  periodFrom: string;
  periodTo: string;
  outputVatAmount: number;
  inputVatAmount: number;
  vatToPay: number;
  vatToRefund: number;
  salesDocumentCount: number;
  purchaseDocumentCount: number;
  status: VatReturnStatus;
  submittedAt?: string;
  dueDate: string;
  companyId: number;
}

export interface AccountingPeriod {
  id: string;
  companyId: number;
  year: number;
  month: number;
  status: PeriodStatus;
  closedBy?: {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
  };
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FixedAssetCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
  taxCategory: number;
  maxTaxDepreciationRate: number;
  defaultAccountingDepreciationRate?: number;
  minUsefulLife?: number;
  maxUsefulLife?: number;
  isActive: boolean;
}

export interface FixedAsset {
  id: string;
  inventoryNumber: string;
  name: string;
  description?: string;
  categoryId: number;
  companyId: number;
  acquisitionCost: number;
  acquisitionDate: string;
  putIntoServiceDate?: string;
  accountingUsefulLife: number;
  accountingDepreciationRate: number;
  accountingAccumulatedDepreciation: number;
  accountingBookValue: number;
  taxDepreciationRate: number;
  taxAccumulatedDepreciation: number;
  taxBookValue: number;
  status: string;
  disposalDate?: string;
  disposalAmount?: number;
}

export interface BankProfile {
  id: string;
  companyId: number;
  name: string;
  iban?: string;
  accountId: number;
  bufferAccountId: number;
  currencyCode: string;
  importFormat: BankImportFormat;
  isActive: boolean;
}

export interface BankImport {
  id: string;
  bankProfileId: number;
  companyId: number;
  fileName: string;
  importedAt: string;
  transactionsCount: number;
  totalCredit: number;
  totalDebit: number;
  status: BankImportStatus;
  errorMessage?: string;
}
