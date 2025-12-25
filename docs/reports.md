# Справки и отчети

## Основни справки

### 1. Оборотна ведомост
Обобщена справка за обороти и салда по сметки.

**Параметри:**
- Период (от-до дата)
- Сметка (опционално)
- Показване на нулеви салда
- Ниво на агрегация (1-4 символа от кода)

**Колони:**
| Начално салдо | Обороти | Крайно салдо |
|---------------|---------|--------------|
| Дебит/Кредит  | Дебит/Кредит | Дебит/Кредит |

### 2. Хронологичен регистър
Хронологичен списък на всички счетоводни операции.

**Колони:**
- Дата
- Дебит сметка
- Кредит сметка
- Сума
- Описание

### 3. Главна книга
Детайлна справка по сметки с всички движения.

**Структура:**
- Групиране по сметки
- Начално салдо
- Операции с баланс
- Крайно салдо

### 4. Дневник на операциите
Списък на всички счетоводни записвания.

**Колони:**
- Дата
- Номер на запис
- Документ
- Сметка
- Дебит/Кредит
- Контрагент

## Справки по контрагенти

Достъпни от `/reports/counterparty`

### Оборотна ведомост по контрагенти
Агрегирани обороти по контрагенти за избрана сметка.

**Изисква:** Избор на сметка

**Колони:**
| Контрагент | Начално салдо | Обороти | Крайно салдо |
|------------|---------------|---------|--------------|
| Име        | Дебит/Кредит  | Дебит/Кредит | Дебит/Кредит |

### Хронологична справка с контрагенти
Списък на операции с информация за контрагента.

**Филтри:**
- Период
- Сметка (опционално)
- Контрагент (опционално)

**Колони:**
- Дата
- Документ
- Контрагент
- Сметка
- Дебит/Кредит

## Експорт

Всички справки поддържат експорт в:
- PDF
- Excel (XLSX)
- CSV

## GraphQL API

### Queries
```graphql
# Оборотна ведомост
turnoverSheet(input: TurnoverReportInput!): TurnoverReport

# Хронологичен регистър
chronologicalReport(input: ChronologicalReportInput!): ChronologicalReport

# Главна книга
generalLedger(input: GeneralLedgerInput!): GeneralLedgerReport

# Дневник на операциите
transactionLog(input: TransactionLogInput!): TransactionLogReport
```

### Input типове
```graphql
input TurnoverReportInput {
  companyId: ID!
  startDate: Date!
  endDate: Date!
  accountId: ID
  showZeroBalances: Boolean
  accountCodeDepth: Int
}

input ChronologicalReportInput {
  companyId: ID!
  startDate: Date!
  endDate: Date!
  accountId: ID
}

input GeneralLedgerInput {
  companyId: ID!
  startDate: Date!
  endDate: Date!
  accountId: ID
}

input TransactionLogInput {
  companyId: ID!
  startDate: Date!
  endDate: Date!
  accountId: ID
}
```

### Export Mutations
```graphql
exportTurnoverSheet(input: TurnoverReportInput!, format: String!): ExportResult
exportChronologicalReport(input: ChronologicalReportInput!, format: String!): ExportResult
exportGeneralLedger(input: GeneralLedgerInput!, format: String!): ExportResult
exportTransactionLog(input: TransactionLogInput!, format: String!): ExportResult
```

## Frontend

### Страници
- `/reports` - Основни справки
- `/reports/counterparty` - Справки по контрагенти

### Компоненти
```
src/pages/
├── Reports.tsx              # Основни справки
└── CounterpartyReports.tsx  # Справки по контрагенти
```

### Предварителни периоди
- Текущ месец
- Предходен месец
- Текущо тримесечие
- Предходно тримесечие
- Текуща година
- Предходна година
- Произволен период

## Месечна статистика на транзакции

Справка за ценообразуване - показва обобщени данни по месеци.

**Достъп:** `/reports/monthly-stats`

**Параметри:**
- От година/месец
- До година/месец

**Колони:**
| Период | Документи (общо) | Документи (приключени) | Редове Дт/Кт (общо) | Редове Дт/Кт (приключени) | Оборот | ДДС |
|--------|------------------|------------------------|---------------------|---------------------------|--------|-----|

**Модел на ценообразуване:**
Базова цена (ангажимент) + допълнително заплащане по брой транзакции и счетоводни редове (Дт/Кт).
Важно: Отчитат се само приключени (posted) записи.

### GraphQL API

```graphql
# Query
monthlyTransactionStats(input: MonthlyStatsInput!): [MonthlyTransactionStats!]!

# Input
input MonthlyStatsInput {
  companyId: ID!
  fromYear: Int!
  fromMonth: Int!
  toYear: Int!
  toMonth: Int!
}

# Output
type MonthlyTransactionStats {
  year: Int!
  month: Int!
  monthName: String!
  totalEntries: Int!
  postedEntries: Int!
  totalEntryLines: Int!
  postedEntryLines: Int!
  totalAmount: BigDecimal!
  vatAmount: BigDecimal!
}

# Export
exportMonthlyStats(input: MonthlyStatsInput!, format: String!): ReportExport!
```

**Поддържани формати за експорт:** XLSX

## Бележки

1. **Контрагент**: Ако операцията няма контрагент, показва се "(Без контрагент)"
2. **counterpartName**: Полето е налично в TransactionLog и GeneralLedger entries
3. **Агрегация**: Справките по контрагенти агрегират данните от General Ledger
4. **Месечна статистика**: Полезна за фактуриране на счетоводни услуги базирани на обем
