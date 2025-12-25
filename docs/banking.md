# Банков модул

## Общ преглед

Банковият модул позволява импорт на банкови транзакции и автоматично генериране на счетоводни статии. Поддържа два режима на работа:

1. **Файлов импорт** - ръчно качване на банкови извлечения
2. **Open Banking** - автоматично теглене чрез Salt Edge API

## Банков профил

Всяка банкова сметка се конфигурира като отделен профил:

| Поле | Описание |
|------|----------|
| `name` | Име на банката |
| `iban` | IBAN на сметката |
| `account` | Аналитична сметка (напр. 5031 - Разплащателна сметка в EUR) |
| `bufferAccount` | Буферна сметка (484 - Неуточнени разчети) |
| `currencyCode` | Валута (EUR, BGN, USD) |
| `connectionType` | FILE_IMPORT, SALT_EDGE или MANUAL |
| `importFormat` | Формат за файлов импорт |

## Файлов импорт

### Поддържани формати

| Формат | Банки | Разширение |
|--------|-------|------------|
| `UNICREDIT_MT940` | UniCredit Bulbank | .txt, .sta |
| `WISE_CAMT053` | Wise | .xml |
| `REVOLUT_CAMT053` | Revolut | .xml |
| `PAYSERA_CAMT053` | Paysera | .xml |
| `POSTBANK_XML` | Postbank | .xml |
| `OBB_XML` | OBB | .xml |
| `CCB_CSV` | ЦКБ | .csv |

### Процес на импорт

```
1. Качване на файл в S3
2. Създаване на BankImport запис
3. Парсване на файла според формата
4. Създаване на Salt Edge транзакции (за унификация)
5. Генериране на счетоводни статии
```

### MT940 формат (SWIFT)

```
:20:STMT001
:25:BG80UNCR76301076117601
:28C:001/001
:60F:C230101EUR1000,00
:61:2301020102C500,00NTRF
:86:Получено плащане от Клиент ООД
:62F:C230102EUR1500,00
```

### CAMT.053 формат (ISO 20022)

```xml
<BkToCstmrStmt>
  <Stmt>
    <Acct><Id><IBAN>BG80UNCR...</IBAN></Id></Acct>
    <Ntry>
      <Amt Ccy="EUR">500.00</Amt>
      <CdtDbtInd>CRDT</CdtDbtInd>
      <BookgDt><Dt>2023-01-02</Dt></BookgDt>
    </Ntry>
  </Stmt>
</BkToCstmrStmt>
```

## Open Banking (Salt Edge)

Вижте [Open Banking документация](./open-banking.md) за детайли.

### Предимства

- Автоматично теглене на транзакции
- Real-time баланси
- Няма нужда от ръчно качване на файлове

### Недостатъци

- Не всички банки са налични
- Изисква периодичен reconnect (90 дни)
- Месечна такса към Salt Edge

## Счетоводни статии от банка

### Стандартна логика

При импорт на транзакция се създава статия с буферна сметка:

**Кредит (постъпление):**
```
Дт 503x Разплащателна сметка     1000.00
    Кт 484 Неуточнени разчети        1000.00
```

**Дебит (плащане):**
```
Дт 484 Неуточнени разчети         500.00
    Кт 503x Разплащателна сметка      500.00
```

### AI маппинг (опционално)

Системата може автоматично да разпознава контрагенти и сметки:

```sql
-- ai_bank_accounting_settings
counterpart_pattern = '%VIVACOM%'
default_account_id = 602  -- Разходи за телефон
default_counterpart_id = 123  -- Виваком
```

## REST API

### Bank Profiles

```
GET    /api/banks/profiles?companyId={id}
GET    /api/banks/profiles/{id}
POST   /api/banks/profiles
PUT    /api/banks/profiles/{id}
DELETE /api/banks/profiles/{id}
```

### Bank Imports

```
GET  /api/banks/imports?companyId={id}
GET  /api/banks/imports/profile/{profileId}
POST /api/banks/imports/file/{profileId}
POST /api/banks/imports/openbanking/{profileId}
```

## GraphQL API

### Queries

```graphql
query GetBankProfiles($companyId: ID!) {
  bankProfiles(companyId: $companyId) {
    id
    name
    iban
    connectionType
    saltEdgeStatus
    saltEdgeLastSyncAt
    account { code name }
    bufferAccount { code name }
  }
}

query GetBankImports($profileId: ID!) {
  bankImports(bankProfileId: $profileId) {
    id
    fileName
    importedAt
    transactionsCount
    totalCredit
    totalDebit
    status
  }
}
```

### Mutations

```graphql
mutation CreateBankProfile($input: CreateBankProfileInput!) {
  createBankProfile(input: $input) {
    id
    name
  }
}

mutation ProcessFileImport($profileId: ID!, $fileKey: String!) {
  processFileImport(bankProfileId: $profileId, fileKey: $fileKey) {
    id
    status
    transactionsCount
  }
}
```

## Database схема

### bank_profiles

```sql
CREATE TABLE bank_profiles (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    iban VARCHAR(50),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    buffer_account_id INTEGER NOT NULL REFERENCES accounts(id),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'EUR',
    connection_type VARCHAR(20) NOT NULL DEFAULT 'FILE_IMPORT',
    import_format VARCHAR(30),
    -- Salt Edge fields
    salt_edge_connection_id VARCHAR(100),
    salt_edge_account_id VARCHAR(100),
    salt_edge_provider_code VARCHAR(100),
    salt_edge_provider_name VARCHAR(255),
    salt_edge_last_sync_at TIMESTAMPTZ,
    salt_edge_consent_expires_at TIMESTAMPTZ,
    salt_edge_status VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    settings JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

### bank_imports

```sql
CREATE TABLE bank_imports (
    id SERIAL PRIMARY KEY,
    bank_profile_id INTEGER NOT NULL REFERENCES bank_profiles(id),
    company_id INTEGER NOT NULL REFERENCES companies(id),
    file_name VARCHAR(500) NOT NULL,
    import_format VARCHAR(30) NOT NULL,
    import_source VARCHAR(20) NOT NULL DEFAULT 'FILE', -- FILE, OPEN_BANKING
    imported_at TIMESTAMPTZ NOT NULL,
    transactions_count INTEGER NOT NULL DEFAULT 0,
    total_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_debit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_journal_entries INTEGER NOT NULL DEFAULT 0,
    journal_entry_ids JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

## Frontend компоненти

### Banks.tsx

Основна страница за управление на банкови профили:
- Списък с конфигурирани банки
- Форма за създаване/редакция
- Избор между файлов импорт и Open Banking
- Бутони за Sync и Reconnect

### BankImport.tsx

Компонент за импорт на банкови извлечения:
- Drag & drop за файлове
- Преглед на транзакции преди импорт
- Mapping на контрагенти

## Примери

### Създаване на профил за файлов импорт

```typescript
const input = {
  companyId: '1',
  name: 'UniCredit EUR',
  iban: 'BG80UNCR76301076117601',
  accountId: '42', // 5031 Разплащателна сметка EUR
  bufferAccountId: '55', // 484 Неуточнени разчети
  currencyCode: 'EUR',
  connectionType: 'FILE_IMPORT',
  importFormat: 'UNICREDIT_MT940'
};

await createBankProfile({ variables: { input } });
```

### Създаване на профил за Open Banking

```typescript
const input = {
  companyId: '1',
  name: 'Revolut Business',
  accountId: '43', // 5032 Разплащателна сметка Revolut
  bufferAccountId: '55',
  currencyCode: 'EUR',
  connectionType: 'SALT_EDGE',
  saltEdgeProviderCode: 'revolut_eu'
};

await createBankProfile({ variables: { input } });

// След това инициирайте връзката
const { connectUrl } = await fetch('/api/saltedge/connect?companyId=1&providerCode=revolut_eu');
window.location.href = connectUrl;
```

## Бъдещи подобрения

- [ ] Автоматичен парсинг на MT940 и CAMT.053
- [ ] AI разпознаване на контрагенти от описанието
- [ ] Правила за автоматично осчетоводяване
- [ ] Scheduled sync за Open Banking (daily)
- [ ] Известия при нови транзакции
- [ ] Bank reconciliation модул
