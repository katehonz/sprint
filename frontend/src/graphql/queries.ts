import { gql } from '@apollo/client';

// Users
export const GET_USERS = gql`
  query GetUsers {
    users {
      id
      username
      email
      firstName
      lastName
      isActive
      group {
        id
        name
        description
      }
    }
  }
`;

export const GET_USER = gql`
  query GetUser($id: ID!) {
    user(id: $id) {
      id
      username
      email
      firstName
      lastName
      isActive
      group {
        id
        name
      }
    }
  }
`;

export const GET_USER_GROUPS = gql`
  query GetUserGroups {
    userGroups {
      id
      name
      description
    }
  }
`;

export const GET_CURRENT_USER = gql`
  query GetCurrentUser {
    currentUser {
      id
      username
      email
      firstName
      lastName
      group {
        id
        name
      }
    }
  }
`;

export const CREATE_USER = gql`
  mutation CreateUser($input: CreateUserInput!) {
    createUser(input: $input) {
      id
      username
      email
      firstName
      lastName
      isActive
      group {
        id
        name
      }
    }
  }
`;

export const UPDATE_USER = gql`
  mutation UpdateUser($id: ID!, $input: UpdateUserInput!) {
    updateUser(id: $id, input: $input) {
      id
      username
      email
      firstName
      lastName
      isActive
      group {
        id
        name
      }
    }
  }
`;

export const DELETE_USER = gql`
  mutation DeleteUser($id: ID!) {
    deleteUser(id: $id)
  }
`;

export const UPDATE_PROFILE = gql`
  mutation UpdateProfile($input: UpdateProfileInput!) {
    updateProfile(input: $input) {
      id
      username
      email
      firstName
      lastName
    }
  }
`;

export const CHANGE_PASSWORD = gql`
  mutation ChangePassword($input: ChangePasswordInput!) {
    changePassword(input: $input)
  }
`;

export const RESET_USER_PASSWORD = gql`
  mutation ResetUserPassword($id: ID!, $newPassword: String!) {
    resetUserPassword(id: $id, newPassword: $newPassword)
  }
`;

// Companies
export const GET_COMPANIES = gql`
  query GetCompanies {
    companies {
      id
      name
      eik
      vatNumber
      city
      country
      isActive
      napOffice
      preferredRateProvider
      baseCurrency {
        id
        code
        symbol
      }
      createdAt
    }
  }
`;

export const GET_COMPANY = gql`
  query GetCompany($id: ID!) {
    company(id: $id) {
      id
      name
      eik
      vatNumber
      address
      city
      country
      phone
      email
      contactPerson
      managerName
      managerEgn
      authorizedPerson
      authorizedPersonEgn
      isActive
      napOffice
      azureFormRecognizerEndpoint
      azureFormRecognizerKey
      enableViesValidation
      enableAiMapping
      preferredRateProvider
      baseCurrency {
        id
        code
        symbol
      }
      createdAt
      updatedAt
    }
  }
`;

export const CREATE_COMPANY = gql`
  mutation CreateCompany($input: CreateCompanyInput!) {
    createCompany(input: $input) {
      id
      name
      eik
    }
  }
`;

export const UPDATE_COMPANY = gql`
  mutation UpdateCompany($id: ID!, $input: UpdateCompanyInput!) {
    updateCompany(id: $id, input: $input) {
      id
      name
      eik
    }
  }
`;

export const DELETE_COMPANY = gql`
  mutation DeleteCompany($id: ID!) {
    deleteCompany(id: $id)
  }
`;

// Accounts
export const GET_ACCOUNTS = gql`
  query GetAccounts($companyId: ID!) {
    accounts(companyId: $companyId) {
      id
      code
      name
      description
      accountType
      accountClass
      level
      isAnalytical
      isActive
      supportsQuantities
      defaultUnit
      parent {
        id
        code
      }
    }
  }
`;

export const GET_ACCOUNT_HIERARCHY = gql`
  query GetAccountHierarchy($companyId: ID!) {
    accountHierarchy(companyId: $companyId) {
      id
      code
      name
      accountType
      accountClass
      level
      isAnalytical
      parent {
        id
      }
    }
  }
`;

export const CREATE_ACCOUNT = gql`
  mutation CreateAccount($input: CreateAccountInput!) {
    createAccount(input: $input) {
      id
      code
      name
    }
  }
`;

export const DELETE_ACCOUNT = gql`
  mutation DeleteAccount($id: ID!) {
    deleteAccount(id: $id)
  }
`;

export const UPDATE_ACCOUNT = gql`
  mutation UpdateAccount($id: ID!, $input: UpdateAccountInput!) {
    updateAccount(id: $id, input: $input) {
      id
      code
      name
      description
      accountType
      supportsQuantities
      defaultUnit
      isActive
    }
  }
`;

// Journal Entries
export const GET_JOURNAL_ENTRIES = gql`
  query GetJournalEntries($filter: JournalEntryFilter!) {
    journalEntries(filter: $filter) {
      id
      entryNumber
      documentDate
      accountingDate
      documentNumber
      documentType
      description
      totalAmount
      isPosted
      counterpartId
      createdAt
    }
  }
`;

export const GET_JOURNAL_ENTRIES_PAGED = gql`
  query GetJournalEntriesPaged($filter: JournalEntryFilter!) {
    journalEntriesPaged(filter: $filter) {
      entries {
        id
        entryNumber
        documentDate
        accountingDate
        documentNumber
        documentType
        description
        totalAmount
        isPosted
        counterpartId
        createdAt
      }
      totalCount
      hasMore
    }
  }
`;

export const GET_JOURNAL_ENTRY = gql`
  query GetJournalEntry($id: ID!) {
    journalEntry(id: $id) {
      id
      entryNumber
      documentDate
      vatDate
      accountingDate
      documentNumber
      description
      totalAmount
      totalVatAmount
      isPosted
      postedAt
      documentType
      vatDocumentType
      vatPurchaseOperation
      vatSalesOperation
      counterpartId
      counterpart {
        id
        name
        eik
        vatNumber
        city
        country
        isVatRegistered
      }
      lines {
        id
        accountId
        account {
          id
          code
          name
          supportsQuantities
          defaultUnit
        }
        debitAmount
        creditAmount
        counterpartId
        currencyCode
        exchangeRate
        baseAmount
        vatAmount
        quantity
        unitOfMeasureCode
        description
        lineOrder
      }
      createdAt
    }
  }
`;

export const CREATE_JOURNAL_ENTRY = gql`
  mutation CreateJournalEntry($input: CreateJournalEntryInput!) {
    createJournalEntry(input: $input) {
      id
      entryNumber
      totalAmount
    }
  }
`;

export const POST_JOURNAL_ENTRY = gql`
  mutation PostJournalEntry($id: ID!) {
    postJournalEntry(id: $id) {
      id
      isPosted
      postedAt
    }
  }
`;

export const UNPOST_JOURNAL_ENTRY = gql`
  mutation UnpostJournalEntry($id: ID!) {
    unpostJournalEntry(id: $id) {
      id
      isPosted
    }
  }
`;

export const UPDATE_JOURNAL_ENTRY = gql`
  mutation UpdateJournalEntry($id: ID!, $input: UpdateJournalEntryInput!) {
    updateJournalEntry(id: $id, input: $input) {
      id
      entryNumber
    }
  }
`;

export const DELETE_JOURNAL_ENTRY = gql`
  mutation DeleteJournalEntry($id: ID!) {
    deleteJournalEntry(id: $id)
  }
`;

// Counterparts
export const GET_COUNTERPARTS = gql`
  query GetCounterparts($companyId: ID!) {
    counterparts(companyId: $companyId) {
      id
      name
      eik
      vatNumber
      address
      longAddress
      city
      country
      counterpartType
      isVatRegistered
      isActive
    }
  }
`;

export const CREATE_COUNTERPART = gql`
  mutation CreateCounterpart($input: CreateCounterpartInput!) {
    createCounterpart(input: $input) {
      id
      name
      eik
    }
  }
`;

export const DELETE_COUNTERPART = gql`
  mutation DeleteCounterpart($id: ID!) {
    deleteCounterpart(id: $id)
  }
`;

export const VALIDATE_VAT = gql`
  mutation ValidateVat($vatNumber: String!) {
    validateVat(vatNumber: $vatNumber) {
      valid
      vatNumber
      countryCode
      name
      longAddress
      errorMessage
      source
    }
  }
`;

// Currencies
export const GET_CURRENCIES = gql`
  query GetCurrencies {
    currencies {
      id
      code
      name
      nameBg
      symbol
      decimalPlaces
      isActive
      isBaseCurrency
    }
  }
`;

export const GET_BASE_CURRENCY = gql`
  query GetBaseCurrency {
    baseCurrency {
      id
      code
      name
      symbol
    }
  }
`;

export const GET_LATEST_EXCHANGE_RATE = gql`
  query GetLatestExchangeRate($fromCode: String!, $toCode: String!) {
    latestExchangeRate(fromCode: $fromCode, toCode: $toCode) {
      rate
      reverseRate
      validDate
      rateSource
    }
  }
`;

export const FETCH_ECB_RATES = gql`
  mutation FetchEcbRates($date: Date) {
    fetchEcbRates(date: $date) {
      id
      rate
      validDate
    }
  }
`;

export const GET_EXCHANGE_RATES = gql`
  query GetExchangeRates($baseCurrency: String) {
    allExchangeRates(baseCurrency: $baseCurrency) {
      id
      fromCurrency {
        code
      }
      toCurrency {
        code
      }
      rate
      validDate
      rateSource
    }
  }
`;

export const CREATE_CURRENCY = gql`
  mutation CreateCurrency($input: CreateCurrencyInput!) {
    createCurrency(input: $input) {
      id
      code
      name
      symbol
      isActive
    }
  }
`;

export const UPDATE_CURRENCY = gql`
  mutation UpdateCurrency($id: ID!, $input: UpdateCurrencyInput!) {
    updateCurrency(id: $id, input: $input) {
      id
      code
      name
      symbol
      isActive
    }
  }
`;

// VAT
export const GET_VAT_RATES = gql`
  query GetVatRates($companyId: ID!) {
    vatRates(companyId: $companyId) {
      id
      code
      name
      rate
      vatDirection
      isActive
      validFrom
      validTo
    }
  }
`;

export const GET_VAT_RETURNS = gql`
  query GetVatReturns($companyId: ID!) {
    vatReturns(companyId: $companyId) {
      id
      periodYear
      periodMonth
      periodFrom
      periodTo
      outputVatAmount
      inputVatAmount
      vatToPay
      vatToRefund
      status
      dueDate
    }
  }
`;

export const GENERATE_VAT_RETURN = gql`
  mutation GenerateVatReturn($input: GenerateVatReturnInput!) {
    generateVatReturn(input: $input) {
      id
      periodYear
      periodMonth
      vatToPay
      vatToRefund
      status
    }
  }
`;

export const CREATE_VAT_RATE = gql`
  mutation CreateVatRate($input: CreateVatRateInput!) {
    createVatRate(input: $input) {
      id
      code
      name
      rate
    }
  }
`;

export const DELETE_VAT_RATE = gql`
  mutation DeleteVatRate($id: ID!) {
    deleteVatRate(id: $id)
  }
`;

export const CREATE_VAT_RETURN = gql`
  mutation CreateVatReturn($input: CreateVatReturnInput!) {
    createVatReturn(input: $input) {
      id
      periodYear
      periodMonth
      status
    }
  }
`;

export const SUBMIT_VAT_RETURN = gql`
  mutation SubmitVatReturn($id: ID!) {
    submitVatReturn(id: $id) {
      id
      status
      submittedAt
    }
  }
`;

export const CALCULATE_VAT_RETURN = gql`
    mutation CalculateVatReturn($input: GenerateVatReturnInput!) {
        calculateVatReturn(input: $input) {
            id
            status
            calculatedAt
        }
    }
`;

export const EXPORT_DEKLAR = gql`
    mutation ExportDeklar($id: ID!) {
        exportDeklar(id: $id)
    }
`;

export const EXPORT_POKUPKI = gql`
    mutation ExportPokupki($id: ID!) {
        exportPokupki(id: $id)
    }
`;

export const EXPORT_PRODAGBI = gql`
    mutation ExportProdajbi($id: ID!) {
        exportProdajbi(id: $id)
    }
`;

export const DELETE_VAT_RETURN = gql`
    mutation DeleteVatReturn($id: ID!) {
        deleteVatReturn(id: $id)
    }
`;

export const UPDATE_VAT_RETURN = gql`
    mutation UpdateVatReturn($id: ID!, $input: UpdateVatReturnInput!) {
        updateVatReturn(id: $id, input: $input) {
            id
            status
            vatToPay
            vatToRefund
        }
    }
`;

export const GET_VAT_JOURNAL_ENTRIES = gql`
    query GetVatJournalEntries($filter: JournalEntryFilter!) {
        journalEntries(filter: $filter) {
            id
            entryNumber
            documentNumber
            documentDate
            vatDate
            description
            vatDocumentType
            vatPurchaseOperation
            vatSalesOperation
            totalAmount
            totalVatAmount
            counterpart {
                id
                name
                vatNumber
                eik
            }
            lines {
                id
                baseAmount
                vatAmount
            }
        }
    }
`;

export const GET_VAT_RETURN_DETAILS = gql`
  query GetVatReturnDetails($id: ID!) {
    vatReturn(id: $id) {
      id
      periodYear
      periodMonth
      periodFrom
      periodTo
      status
      dueDate
      outputVatAmount
      inputVatAmount
      vatToPay
      vatToRefund
      effectiveVatToPay
      vatForDeduction
      vatRefundArt92
      salesBase20
      salesVat20
      salesBase9
      salesVat9
      salesBaseVop
      salesVatVop
      salesBase0Export
      salesBase0Vod
      salesBase0Art3
      salesBaseArt21
      salesBaseArt69
      salesBaseExempt
      salesVatPersonalUse
      salesDocumentCount
      purchaseBaseFullCredit
      purchaseVatFullCredit
      purchaseBasePartialCredit
      purchaseVatPartialCredit
      purchaseBaseNoCredit
      purchaseDocumentCount
      totalDeductibleVat
      calculatedAt
      submittedAt
    }
  }
`;

// Accounting Periods (Приключване на периоди)
export const GET_ACCOUNTING_PERIODS = gql`
  query GetAccountingPeriods($companyId: ID!) {
    accountingPeriods(companyId: $companyId) {
      id
      companyId
      year
      month
      status
      closedBy {
        id
        username
        firstName
        lastName
      }
      closedAt
      createdAt
      updatedAt
    }
  }
`;

export const GET_ACCOUNTING_PERIOD = gql`
  query GetAccountingPeriod($companyId: ID!, $year: Int!, $month: Int!) {
    accountingPeriod(companyId: $companyId, year: $year, month: $month) {
      id
      companyId
      year
      month
      status
      closedBy {
        id
        username
        firstName
        lastName
      }
      closedAt
      createdAt
      updatedAt
    }
  }
`;

export const IS_PERIOD_OPEN = gql`
  query IsPeriodOpen($companyId: ID!, $year: Int!, $month: Int!) {
    isPeriodOpen(companyId: $companyId, year: $year, month: $month)
  }
`;

export const CLOSE_ACCOUNTING_PERIOD = gql`
  mutation CloseAccountingPeriod($input: CloseAccountingPeriodInput!) {
    closeAccountingPeriod(input: $input) {
      id
      companyId
      year
      month
      status
      closedBy {
        id
        username
        firstName
        lastName
      }
      closedAt
    }
  }
`;

export const REOPEN_ACCOUNTING_PERIOD = gql`
  mutation ReopenAccountingPeriod($input: CloseAccountingPeriodInput!) {
    reopenAccountingPeriod(input: $input) {
      id
      companyId
      year
      month
      status
      closedBy {
        id
        username
        firstName
        lastName
      }
      closedAt
    }
  }
`;

// Fixed Assets
export const GET_FIXED_ASSETS = gql`
  query GetFixedAssets($companyId: ID!) {
    fixedAssets(companyId: $companyId) {
      id
      inventoryNumber
      name
      acquisitionCost
      acquisitionDate
      documentNumber
      documentDate
      putIntoServiceDate
      accountingAccumulatedDepreciation
      accountingBookValue
      taxAccumulatedDepreciation
      taxBookValue
      status
      categoryId
    }
  }
`;

export const GET_FIXED_ASSET_CATEGORIES = gql`
  query GetFixedAssetCategories($companyId: ID!) {
    fixedAssetCategories(companyId: $companyId) {
      id
      code
      name
      description
      taxCategory
      maxTaxDepreciationRate
      defaultAccountingDepreciationRate
      assetAccountCode
      depreciationAccountCode
      expenseAccountCode
      isActive
    }
  }
`;

export const UPDATE_FIXED_ASSET_CATEGORY = gql`
  mutation UpdateFixedAssetCategory($id: ID!, $input: UpdateFixedAssetCategoryInput!) {
    updateFixedAssetCategory(id: $id, input: $input) {
      id
      code
      name
      description
      taxCategory
      maxTaxDepreciationRate
      defaultAccountingDepreciationRate
      assetAccountCode
      depreciationAccountCode
      expenseAccountCode
      isActive
    }
  }
`;

export const CREATE_FIXED_ASSET = gql`
  mutation CreateFixedAsset($input: CreateFixedAssetInput!) {
    createFixedAsset(input: $input) {
      id
      inventoryNumber
      name
    }
  }
`;

export const DELETE_FIXED_ASSET = gql`
  mutation DeleteFixedAsset($id: ID!) {
    deleteFixedAsset(id: $id)
  }
`;

export const CALCULATE_DEPRECIATION = gql`
  mutation CalculateDepreciation($companyId: ID!, $toDate: Date!) {
    calculateDepreciation(companyId: $companyId, toDate: $toDate) {
      id
      inventoryNumber
      accountingAccumulatedDepreciation
      accountingBookValue
      taxAccumulatedDepreciation
      taxBookValue
    }
  }
`;

// Depreciation Journal queries
export const GET_DEPRECIATION_JOURNAL = gql`
  query GetDepreciationJournal($companyId: ID!, $year: Int!, $month: Int) {
    depreciationJournal(companyId: $companyId, year: $year, month: $month) {
      id
      fixedAssetId
      fixedAssetName
      fixedAssetInventoryNumber
      period
      companyId
      accountingDepreciationAmount
      accountingBookValueBefore
      accountingBookValueAfter
      taxDepreciationAmount
      taxBookValueBefore
      taxBookValueAfter
      isPosted
      journalEntryId
      postedAt
      createdAt
    }
  }
`;

export const GET_CALCULATED_PERIODS = gql`
  query GetCalculatedPeriods($companyId: ID!) {
    calculatedPeriods(companyId: $companyId) {
      year
      month
      periodDisplay
      isPosted
      totalAccountingAmount
      totalTaxAmount
      assetsCount
    }
  }
`;

export const GET_ASSETS_NEEDING_DEPRECIATION = gql`
  query GetAssetsNeedingDepreciation($companyId: ID!, $year: Int!, $month: Int!) {
    assetsNeedingDepreciation(companyId: $companyId, year: $year, month: $month) {
      id
      inventoryNumber
      name
      acquisitionCost
      accountingBookValue
      taxBookValue
    }
  }
`;

export const CALCULATE_MONTHLY_DEPRECIATION = gql`
  mutation CalculateMonthlyDepreciation($companyId: ID!, $year: Int!, $month: Int!) {
    calculateMonthlyDepreciation(companyId: $companyId, year: $year, month: $month) {
      calculated {
        id
        fixedAssetId
        fixedAssetName
        fixedAssetInventoryNumber
        period
        accountingDepreciationAmount
        accountingBookValueBefore
        accountingBookValueAfter
        taxDepreciationAmount
        taxBookValueBefore
        taxBookValueAfter
        isPosted
      }
      errors {
        fixedAssetId
        assetName
        errorMessage
      }
      totalAccountingAmount
      totalTaxAmount
    }
  }
`;

export const POST_DEPRECIATION = gql`
  mutation PostDepreciation($companyId: ID!, $year: Int!, $month: Int!) {
    postDepreciation(companyId: $companyId, year: $year, month: $month) {
      journalEntryId
      totalAmount
      assetsCount
    }
  }
`;

// Bank
export const GET_BANK_PROFILES = gql`
  query GetBankProfiles($companyId: ID!) {
    bankProfiles(companyId: $companyId) {
      id
      name
      iban
      currencyCode
      importFormat
      isActive
    }
  }
`;

export const GET_BANK_IMPORTS = gql`
  query GetBankImports($companyId: ID!) {
    bankImports(companyId: $companyId) {
      id
      fileName
      importedAt
      transactionsCount
      totalCredit
      totalDebit
      status
    }
  }
`;

// Document Scanning
export const RECOGNIZE_INVOICE = gql`
    mutation RecognizeInvoice($file: Upload!, $invoiceType: String!, $companyId: ID!) {
        recognizeInvoice(file: $file, invoiceType: $invoiceType, companyId: $companyId) {
            vendorName
            invoiceId
            invoiceDate
            invoiceTotal
            subtotal
            totalTax
            customerName
            dueDate
        }
    }
`;

// Reports
export const GET_TURNOVER_SHEET = gql`
  query GetTurnoverSheet($input: TurnoverReportInput!) {
    turnoverSheet(input: $input) {
      companyName
      periodStart
      periodEnd
      generatedAt
      entries {
        accountId
        accountCode
        accountName
        openingDebit
        openingCredit
        periodDebit
        periodCredit
        closingDebit
        closingCredit
      }
      totals {
        accountId
        accountCode
        accountName
        openingDebit
        openingCredit
        periodDebit
        periodCredit
        closingDebit
        closingCredit
      }
    }
  }
`;

export const GET_CHRONOLOGICAL_REPORT = gql`
  query GetChronologicalReport($input: ChronologicalReportInput!) {
    chronologicalReport(input: $input) {
      companyName
      periodStart
      periodEnd
      generatedAt
      totalAmount
      entries {
        date
        debitAccountCode
        debitAccountName
        creditAccountCode
        creditAccountName
        amount
        debitCurrencyAmount
        debitCurrencyCode
        creditCurrencyAmount
        creditCurrencyCode
        documentType
        documentDate
        description
      }
    }
  }
`;

export const GET_GENERAL_LEDGER = gql`
  query GetGeneralLedger($input: GeneralLedgerInput!) {
    generalLedger(input: $input) {
      companyName
      periodStart
      periodEnd
      generatedAt
      accounts {
        accountId
        accountCode
        accountName
        openingBalance
        closingBalance
        totalDebits
        totalCredits
        entries {
          date
          entryNumber
          documentNumber
          description
          debitAmount
          creditAmount
          balance
          counterpartName
        }
      }
    }
  }
`;

export const GET_TRANSACTION_LOG = gql`
  query GetTransactionLog($input: TransactionLogInput!) {
    transactionLog(input: $input) {
      companyName
      periodStart
      periodEnd
      generatedAt
      entries {
        date
        entryNumber
        documentNumber
        description
        accountCode
        accountName
        debitAmount
        creditAmount
        counterpartName
      }
    }
  }
`;

export const EXPORT_TURNOVER_SHEET = gql`
  mutation ExportTurnoverSheet($input: TurnoverReportInput!, $format: String!) {
    exportTurnoverSheet(input: $input, format: $format) {
      content
      filename
      mimeType
    }
  }
`;

export const EXPORT_CHRONOLOGICAL_REPORT = gql`
  mutation ExportChronologicalReport($input: ChronologicalReportInput!, $format: String!) {
    exportChronologicalReport(input: $input, format: $format) {
      content
      filename
      mimeType
    }
  }
`;

export const EXPORT_GENERAL_LEDGER = gql`
  mutation ExportGeneralLedger($input: GeneralLedgerInput!, $format: String!) {
    exportGeneralLedger(input: $input, format: $format) {
      content
      filename
      mimeType
    }
  }
`;

export const EXPORT_TRANSACTION_LOG = gql`
  mutation ExportTransactionLog($input: TransactionLogInput!, $format: String!) {
    exportTransactionLog(input: $input, format: $format) {
      content
      filename
      mimeType
    }
  }
`;

// Inventory
export const GET_INVENTORY_BALANCES = gql`
    query GetInventoryBalances($companyId: ID!) {
        inventoryBalances(companyId: $companyId) {
            id
            currentQuantity
            currentAmount
            averageCost
            unitOfMeasure
            lastMovementDate
            account {
                id
                code
                name
            }
        }
    }
`;

// Количествена оборотна ведомост
export const GET_QUANTITY_TURNOVER = gql`
    query GetQuantityTurnover($input: QuantityTurnoverInput!) {
        getQuantityTurnover(input: $input) {
            accountId
            accountCode
            accountName
            openingQuantity
            openingAmount
            receiptQuantity
            receiptAmount
            issueQuantity
            issueAmount
            closingQuantity
            closingAmount
        }
    }
`;

// Средно претеглена цена (СПЦ)
export const GET_AVERAGE_COST = gql`
    query GetAverageCost($companyId: ID!, $accountId: ID!, $asOfDate: Date) {
        getAverageCost(companyId: $companyId, accountId: $accountId, asOfDate: $asOfDate) {
            accountId
            currentQuantity
            currentAmount
            averageCost
        }
    }
`;

// Проверка за корекции на СПЦ
export const CHECK_RETROACTIVE_CORRECTIONS = gql`
    query CheckRetroactiveCorrections($input: CheckCorrectionsInput!) {
        checkRetroactiveCorrections(input: $input) {
            movementId
            movementDate
            materialAccountId
            materialAccountCode
            materialAccountName
            expenseAccountId
            expenseAccountCode
            expenseAccountName
            quantity
            oldAverageCost
            newAverageCost
            correctionAmount
            description
        }
    }
`;

// Audit Logs
export const GET_AUDIT_LOGS = gql`
    query GetAuditLogs($filter: AuditLogFilter!) {
        auditLogs(filter: $filter) {
            logs {
                id
                companyId
                userId
                username
                userRole
                action
                entityType
                entityId
                details
                ipAddress
                userAgent
                success
                errorMessage
                createdAt
            }
            totalCount
            hasMore
        }
    }
`;

export const GET_AUDIT_LOG_STATS = gql`
    query GetAuditLogStats($companyId: ID, $days: Int) {
        auditLogStats(companyId: $companyId, days: $days) {
            action
            count
        }
    }
`;

// Production - Technology Cards
export const GET_TECHNOLOGY_CARDS = gql`
    query GetTechnologyCards($companyId: ID!) {
        technologyCards(companyId: $companyId) {
            id
            companyId
            code
            name
            description
            outputQuantity
            outputUnit
            isActive
            outputAccount {
                id
                code
                name
            }
            stages {
                id
                stageOrder
                name
                description
                inputQuantity
                inputUnit
                inputAccount {
                    id
                    code
                    name
                }
            }
            createdAt
            updatedAt
        }
    }
`;

export const GET_ACTIVE_TECHNOLOGY_CARDS = gql`
    query GetActiveTechnologyCards($companyId: ID!) {
        activeTechnologyCards(companyId: $companyId) {
            id
            code
            name
            outputQuantity
            outputUnit
            outputAccount {
                id
                code
                name
            }
            stages {
                id
                stageOrder
                name
                inputQuantity
                inputUnit
            }
        }
    }
`;

export const GET_TECHNOLOGY_CARD = gql`
    query GetTechnologyCard($id: ID!) {
        technologyCard(id: $id) {
            id
            companyId
            code
            name
            description
            outputQuantity
            outputUnit
            isActive
            outputAccount {
                id
                code
                name
            }
            stages {
                id
                stageOrder
                name
                description
                inputQuantity
                inputUnit
                inputAccount {
                    id
                    code
                    name
                }
            }
        }
    }
`;

export const CREATE_TECHNOLOGY_CARD = gql`
    mutation CreateTechnologyCard($input: CreateTechnologyCardInput!) {
        createTechnologyCard(input: $input) {
            id
            code
            name
        }
    }
`;

export const UPDATE_TECHNOLOGY_CARD = gql`
    mutation UpdateTechnologyCard($input: UpdateTechnologyCardInput!) {
        updateTechnologyCard(input: $input) {
            id
            code
            name
        }
    }
`;

export const DELETE_TECHNOLOGY_CARD = gql`
    mutation DeleteTechnologyCard($id: ID!) {
        deleteTechnologyCard(id: $id)
    }
`;

// Production - Batches
export const GET_PRODUCTION_BATCHES = gql`
    query GetProductionBatches($companyId: ID!) {
        productionBatches(companyId: $companyId) {
            id
            companyId
            batchNumber
            plannedQuantity
            actualQuantity
            startedAt
            completedAt
            status
            notes
            technologyCard {
                id
                code
                name
                outputUnit
            }
            createdAt
            updatedAt
        }
    }
`;

export const GET_PRODUCTION_BATCHES_BY_FILTER = gql`
    query GetProductionBatchesByFilter($filter: ProductionBatchFilterInput!) {
        productionBatchesByFilter(filter: $filter) {
            id
            batchNumber
            plannedQuantity
            actualQuantity
            startedAt
            completedAt
            status
            notes
            technologyCard {
                id
                code
                name
                outputUnit
            }
            createdAt
        }
    }
`;

export const GET_PRODUCTION_BATCH = gql`
    query GetProductionBatch($id: ID!) {
        productionBatch(id: $id) {
            id
            companyId
            batchNumber
            plannedQuantity
            actualQuantity
            startedAt
            completedAt
            status
            notes
            technologyCard {
                id
                code
                name
                outputUnit
                stages {
                    id
                    stageOrder
                    name
                    inputQuantity
                    inputUnit
                }
            }
            stages {
                id
                plannedQuantity
                actualQuantity
                startedAt
                completedAt
                status
                notes
                technologyCardStage {
                    id
                    stageOrder
                    name
                    inputQuantity
                    inputUnit
                    inputAccount {
                        id
                        code
                        name
                    }
                }
                journalEntry {
                    id
                    entryNumber
                }
            }
            createdAt
            updatedAt
        }
    }
`;

export const CREATE_PRODUCTION_BATCH = gql`
    mutation CreateProductionBatch($input: CreateProductionBatchInput!) {
        createProductionBatch(input: $input) {
            id
            batchNumber
            status
        }
    }
`;

export const UPDATE_PRODUCTION_BATCH = gql`
    mutation UpdateProductionBatch($input: UpdateProductionBatchInput!) {
        updateProductionBatch(input: $input) {
            id
            batchNumber
            status
        }
    }
`;

export const START_PRODUCTION_BATCH = gql`
    mutation StartProductionBatch($id: ID!) {
        startProductionBatch(id: $id) {
            id
            status
            startedAt
        }
    }
`;

export const COMPLETE_PRODUCTION_BATCH = gql`
    mutation CompleteProductionBatch($id: ID!) {
        completeProductionBatch(id: $id) {
            id
            status
            completedAt
        }
    }
`;

export const CANCEL_PRODUCTION_BATCH = gql`
    mutation CancelProductionBatch($id: ID!) {
        cancelProductionBatch(id: $id) {
            id
            status
        }
    }
`;

export const DELETE_PRODUCTION_BATCH = gql`
    mutation DeleteProductionBatch($id: ID!) {
        deleteProductionBatch(id: $id)
    }
`;

export const COMPLETE_PRODUCTION_STAGE = gql`
    mutation CompleteProductionStage($input: CompleteProductionStageInput!) {
        completeProductionStage(input: $input) {
            id
            status
            actualQuantity
            completedAt
            journalEntry {
                id
                entryNumber
            }
        }
    }
`;

export const CANCEL_PRODUCTION_STAGE = gql`
    mutation CancelProductionStage($id: ID!) {
        cancelProductionStage(id: $id) {
            id
            status
        }
    }
`;

// System Settings
export const GET_SYSTEM_SETTINGS = gql`
    query GetSystemSettings {
        systemSettings {
            id
            smtpHost
            smtpPort
            smtpUsername
            smtpFromEmail
            smtpFromName
            smtpUseTls
            smtpUseSsl
            smtpEnabled
            appName
            defaultLanguage
            defaultTimezone
            updatedAt
            updatedBy
        }
    }
`;

export const UPDATE_SMTP_SETTINGS = gql`
    mutation UpdateSmtpSettings($input: UpdateSmtpSettingsInput!) {
        updateSmtpSettings(input: $input) {
            id
            smtpHost
            smtpPort
            smtpUsername
            smtpFromEmail
            smtpFromName
            smtpUseTls
            smtpUseSsl
            smtpEnabled
            updatedAt
            updatedBy
        }
    }
`;

export const TEST_SMTP_CONNECTION = gql`
    mutation TestSmtpConnection($testEmail: String!) {
        testSmtpConnection(testEmail: $testEmail)
    }
`;

// Monthly Transaction Stats (Месечна статистика за ценообразуване)
export const GET_MONTHLY_TRANSACTION_STATS = gql`
    query GetMonthlyTransactionStats($input: MonthlyStatsInput!) {
        monthlyTransactionStats(input: $input) {
            year
            month
            monthName
            totalEntries
            postedEntries
            totalEntryLines
            postedEntryLines
            totalAmount
            vatAmount
        }
    }
`;

export const EXPORT_MONTHLY_STATS = gql`
    mutation ExportMonthlyStats($input: MonthlyStatsInput!, $format: String!) {
        exportMonthlyStats(input: $input, format: $format) {
            content
            filename
            mimeType
        }
    }
`;
