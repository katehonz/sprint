# GraphQL API

## Queries

### Компании
```graphql
companies: [Company]
company(id: ID!): Company
```

### Сметки
```graphql
accounts(companyId: ID!): [Account]
account(id: ID!): Account
accountsByParent(companyId: ID!, parentId: ID): [Account]
accountHierarchy(companyId: ID!): [Account]
analyticalAccounts(companyId: ID!): [Account]
```

### Счетоводни статии
```graphql
journalEntries(filter: JournalEntryFilter!): [JournalEntry]
journalEntry(id: ID!): JournalEntryWithLines
unpostedEntries(companyId: ID!): [JournalEntry]
```

### Контрагенти
```graphql
counterparts(companyId: ID!): [Counterpart]
counterpart(id: ID!): Counterpart
searchCounterparts(companyId: ID!, query: String!): [Counterpart]
```

### Валути
```graphql
currencies: [Currency]
currency(id: ID!): Currency
currencyByCode(code: String!): Currency
baseCurrency: Currency
```

### Валутни курсове
```graphql
exchangeRates(fromCurrencyId: ID!, toCurrencyId: ID!): [ExchangeRate]
exchangeRate(fromCode: String!, toCode: String!, date: Date!): ExchangeRate
latestExchangeRate(fromCode: String!, toCode: String!): ExchangeRate
```

### Банкови профили
```graphql
bankProfiles(companyId: ID!): [BankProfile]
bankProfile(id: ID!): BankProfile
bankProfileByIban(iban: String!): BankProfile
```

### Банкови импорти
```graphql
bankImports(companyId: ID!): [BankImport]
bankImport(id: ID!): BankImport
bankImportsByProfile(bankProfileId: ID!): [BankImport]
```

### ДДС ставки
```graphql
vatRates(companyId: ID!): [VatRate]
vatRate(id: ID!): VatRate
vatRateByCode(code: String!): VatRate
activeVatRates(companyId: ID!): [VatRate]
```

### ДДС декларации
```graphql
vatReturns(companyId: ID!): [VatReturn]
vatReturn(id: ID!): VatReturn
vatReturnByPeriod(companyId: ID!, year: Int!, month: Int!): VatReturn
```

### Дълготрайни активи
```graphql
fixedAssets(companyId: ID!): [FixedAsset]
fixedAsset(id: ID!): FixedAsset
fixedAssetsByCategory(categoryId: ID!): [FixedAsset]
fixedAssetCategories(companyId: ID!): [FixedAssetCategory]
fixedAssetCategory(id: ID!): FixedAssetCategory
```

### Складови наличности
```graphql
inventoryMovements(filter: InventoryMovementFilter!): [InventoryMovement]
inventoryBalances(companyId: ID!): [InventoryBalance]
inventoryBalance(companyId: ID!, accountId: ID!): InventoryBalance
```

### Сканирани фактури
```graphql
scannedInvoices(companyId: ID!): [ScannedInvoice]
scannedInvoicesByDirection(companyId: ID!, direction: String!): [ScannedInvoice]
scannedInvoice(id: ID!): ScannedInvoice
```

## Mutations

### Автентикация
```graphql
login(loginInput: LoginInput!): AuthResponse!
```

### Компании
```graphql
createCompany(input: CreateCompanyInput!): Company!
updateCompany(id: ID!, input: UpdateCompanyInput!): Company!
deleteCompany(id: ID!): Boolean!
```

### Сметки
```graphql
createAccount(input: CreateAccountInput!): Account!
updateAccount(id: ID!, input: UpdateAccountInput!): Account!
deleteAccount(id: ID!): Boolean!
```

### Счетоводни статии
```graphql
createJournalEntry(input: CreateJournalEntryInput!): JournalEntryWithLines!
updateJournalEntry(id: ID!, input: UpdateJournalEntryInput!): JournalEntryWithLines!
deleteJournalEntry(id: ID!): Boolean!
postJournalEntry(id: ID!): JournalEntry!
unpostJournalEntry(id: ID!): JournalEntry!
```

### Контрагенти
```graphql
createCounterpart(input: CreateCounterpartInput!): Counterpart!
updateCounterpart(id: ID!, input: UpdateCounterpartInput!): Counterpart!
deleteCounterpart(id: ID!): Boolean!
validateVat(vatNumber: String!): ViesValidationResult!
```

### VIES Валидация
Mutation `validateVat` проверява ДДС номер срещу EU VIES системата и връща информация за фирмата.

```graphql
mutation {
  validateVat(vatNumber: "BG123456789") {
    valid
    vatNumber
    countryCode
    name
    longAddress
    errorMessage
    source
  }
}
```

**Резултат:**
- `valid` - дали номерът е валиден
- `name` - име на фирмата (ако е достъпно)
- `longAddress` - пълен адрес като един string (формат на VIES)
- `countryCode` - код на държавата (BG, DE, FR и др.)
- `source` - REST или SOAP (метод на валидация)
- `errorMessage` - съобщение за грешка при невалиден номер

### Валути
```graphql
createCurrency(input: CreateCurrencyInput!): Currency!
updateCurrency(id: ID!, input: UpdateCurrencyInput!): Currency!
```

### Валутни курсове
```graphql
createExchangeRate(input: CreateExchangeRateInput!): ExchangeRate!
fetchEcbRates(date: Date): [ExchangeRate!]!
```

### Банкови профили
```graphql
createBankProfile(input: CreateBankProfileInput!): BankProfile!
updateBankProfile(id: ID!, input: UpdateBankProfileInput!): BankProfile!
deleteBankProfile(id: ID!): Boolean!
```

### Банкови импорти
```graphql
processBankImport(bankProfileId: ID!, fileKey: String!): BankImport!
deleteBankImport(id: ID!): Boolean!
```

### ДДС ставки
```graphql
createVatRate(input: CreateVatRateInput!): VatRate!
updateVatRate(id: ID!, input: UpdateVatRateInput!): VatRate!
deleteVatRate(id: ID!): Boolean!
```

### ДДС декларации
```graphql
generateVatReturn(input: GenerateVatReturnInput!): VatReturn!
submitVatReturn(id: ID!): VatReturn!
deleteVatReturn(id: ID!): Boolean!
```

### Дълготрайни активи
```graphql
createFixedAsset(input: CreateFixedAssetInput!): FixedAsset!
updateFixedAsset(id: ID!, input: UpdateFixedAssetInput!): FixedAsset!
disposeFixedAsset(id: ID!, disposalDate: Date!, disposalAmount: BigDecimal!): FixedAsset!
calculateDepreciation(companyId: ID!, toDate: Date!): [FixedAsset!]!
createFixedAssetCategory(input: CreateFixedAssetCategoryInput!): FixedAssetCategory!
updateFixedAssetCategory(id: ID!, input: UpdateFixedAssetCategoryInput!): FixedAssetCategory!
```

### Сканирани фактури
```graphql
saveScannedInvoice(companyId: ID!, recognized: RecognizedInvoiceInput!, fileName: String): ScannedInvoice!
updateScannedInvoiceAccounts(id: ID!, input: UpdateScannedInvoiceAccountsInput!): ScannedInvoice!
validateScannedInvoiceVies(id: ID!): ScannedInvoice!
deleteScannedInvoice(id: ID!): Boolean!
rejectScannedInvoice(id: ID!, reason: String!): ScannedInvoice!
```

#### Примери

**Запазване на сканирана фактура:**
```graphql
mutation {
  saveScannedInvoice(
    companyId: "1"
    recognized: {
      vendorName: "Фирма ЕООД"
      vendorVatNumber: "BG123456789"
      invoiceId: "0000001234"
      invoiceDate: "2024-01-15"
      subtotal: 1000.00
      totalTax: 200.00
      invoiceTotal: 1200.00
      direction: "PURCHASE"
    }
    fileName: "invoice.pdf"
  ) {
    id
    direction
    status
  }
}
```

**VIES валидация:**
```graphql
mutation {
  validateScannedInvoiceVies(id: "1") {
    id
    viesStatus
    viesValidationMessage
    viesCompanyName
    viesCompanyAddress
  }
}
```

**Промяна на сметки:**
```graphql
mutation {
  updateScannedInvoiceAccounts(
    id: "1"
    input: {
      counterpartyAccountId: 45
      vatAccountId: 67
      expenseRevenueAccountId: 89
    }
  ) {
    id
    counterpartyAccount { code name }
    vatAccount { code name }
    expenseRevenueAccount { code name }
  }
}
```

## Типове

### JournalEntryFilter
```graphql
input JournalEntryFilter {
    companyId: ID!
    fromDate: Date
    toDate: Date
    accountId: ID
    isPosted: Boolean
    documentNumber: String
}
```

### InventoryMovementFilter
```graphql
input InventoryMovementFilter {
    companyId: ID!
    accountId: ID
    fromDate: Date
    toDate: Date
    movementType: String
}
```

## Scalars

- `BigDecimal` - Числа с висока точност за финансови изчисления
- `Date` - Дата (YYYY-MM-DD)
- `DateTime` - Дата и час с timezone (ISO 8601)
- `Long` - 64-bit цяло число

## Сигурност

### GraphQL Security Interceptor

Всички GraphQL операции изискват автентикация, освен изрично дефинираните публични операции.

#### Публични операции (без автентикация)

```java
Set.of(
    "login",           // Вход в системата
    "recoverPassword", // Възстановяване на парола
    "resetPassword",   // Нулиране на парола
    "__schema",        // GraphQL introspection
    "__type"           // GraphQL type info
)
```

#### Как работи

1. **Interceptor** (`GraphQLSecurityInterceptor`) прихваща всяка GraphQL заявка
2. Проверява дали операцията е в списъка с публични операции
3. Ако не е публична - проверява `SecurityContextHolder` за валиден JWT token
4. При липса на автентикация връща грешка: `"Unauthorized: Authentication required"`

#### Имплементация

```java
@Component
public class GraphQLSecurityInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String operationName = request.getOperationName();
        String document = request.getDocument();

        boolean isPublicOperation = isPublicOperation(operationName, document);

        if (!isPublicOperation) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Mono.error(new AuthenticationCredentialsNotFoundException(
                    "Unauthorized: Authentication required"
                ));
            }
        }

        return chain.next(request);
    }
}
```

#### Грешки при неавторизиран достъп

При опит за достъп до защитена операция без валиден JWT token:

```json
{
  "errors": [
    {
      "message": "Unauthorized: Authentication required",
      "extensions": {
        "classification": "UNAUTHORIZED"
      }
    }
  ],
  "data": null
}
```

### JWT Token

Всички защитени заявки трябва да включват JWT token в header:

```
Authorization: Bearer <jwt-token>
```

Token се получава от `login` mutation:

```graphql
mutation {
  login(loginInput: { email: "user@example.com", password: "password" }) {
    token
    user {
      id
      email
    }
  }
}
```

### Добавяне на нови публични операции

За да добавите нова публична операция, редактирайте `GraphQLSecurityInterceptor.java`:

```java
private static final Set<String> PUBLIC_OPERATIONS = Set.of(
    "login",
    "recoverPassword",
    "resetPassword",
    "newPublicOperation",  // Добавете тук
    "__schema",
    "__type"
);
```
