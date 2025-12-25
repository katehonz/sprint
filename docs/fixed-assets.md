# Дълготрайни материални активи (ДМА)

Модул за управление на дълготрайни активи съгласно българското законодателство (ЗКПО).

## Функционалности

### 1. Регистър на активите
- Създаване и управление на ДМА
- Полета: инвентарен номер, наименование, категория, стойност
- Документ номер и дата (за ДДС формата)
- Дата на въвеждане в експлоатация
- Статуси: ACTIVE, DISPOSED, WRITTEN_OFF

### 2. Категории по ЗКПО
Категориите следват чл. 55 от ЗКПО:

| Категория | Описание | Макс. норма |
|-----------|----------|-------------|
| 1 | Масивни сгради, съоръжения | 4% |
| 2 | Машини, производствено оборудване | 30% |
| 3 | Транспортни средства без автомобили | 10% |
| 4 | Компютри, периферни устройства, софтуер | 50% |
| 5 | Автомобили | 25% |
| 6 | Други ДМА | 15% |
| 7 | Нематериални активи | 33.33% |

Всяка категория има настройки за:
- Сметка за актива (напр. 204)
- Сметка за натрупана амортизация (напр. 2414)
- Сметка за разход (напр. 603)
- Счетоводна норма на амортизация

### 3. Изчисляване на амортизация

#### Двойна амортизация
Системата изчислява паралелно:
- **Счетоводна амортизация** - по избран метод и норма
- **Данъчна амортизация** - винаги линеен метод, максимална норма по ЗКПО

#### Методи на амортизация
- LINEAR - линеен метод
- DECLINING_BALANCE - намаляващо салдо

#### Месечно изчисляване
```
POST /graphql
mutation {
  calculateMonthlyDepreciation(companyId: "1", year: 2024, month: 12) {
    calculated { fixedAssetName, accountingDepreciationAmount, taxDepreciationAmount }
    totalAccountingAmount
    totalTaxAmount
  }
}
```

### 4. Осчетоводяване
Създава автоматична счетоводна статия:
```
Дт 603 Разходи за амортизация
    Кт 241 Натрупана амортизация
```

```graphql
mutation {
  postDepreciation(companyId: "1", year: 2024, month: 12) {
    journalEntryId
    totalAmount
    assetsCount
  }
}
```

### 5. Дневник на амортизациите
Справка за всички изчислени амортизации с филтри по:
- Година
- Месец
- Статус (осчетоводени/неосчетоводени)

## Frontend структура

```
src/
├── pages/
│   └── FixedAssets.tsx              # Главна страница с табове
└── components/fixedAssets/
    ├── DepreciationCalculation.tsx  # Изчисляване на амортизация
    ├── DepreciationJournal.tsx      # Дневник на амортизациите
    └── AssetCategories.tsx          # Управление на категории
```

### Табове
1. **Регистър** - списък и създаване на активи
2. **Амортизация** - месечно изчисляване и осчетоводяване
3. **Дневник** - справка за амортизации
4. **Категории** - редактиране на сметки по категории

## GraphQL API

### Queries
```graphql
# Списък активи
fixedAssets(companyId: ID!): [FixedAsset]

# Категории
fixedAssetCategories(companyId: ID!): [FixedAssetCategory]

# Дневник амортизации
depreciationJournal(companyId: ID!, year: Int!, month: Int): [DepreciationJournal]

# Изчислени периоди
calculatedPeriods(companyId: ID!): [CalculatedPeriod]
```

### Mutations
```graphql
# Създаване на актив
createFixedAsset(input: CreateFixedAssetInput!): FixedAsset

# Изчисляване на амортизация
calculateMonthlyDepreciation(companyId: ID!, year: Int!, month: Int!): DepreciationCalculationResult

# Осчетоводяване
postDepreciation(companyId: ID!, year: Int!, month: Int!): DepreciationPostResult

# Обновяване на категория
updateFixedAssetCategory(id: ID!, input: UpdateFixedAssetCategoryInput!): FixedAssetCategory
```

## База данни

### Таблици
- `fixed_assets` - активи
- `fixed_asset_categories` - категории
- `depreciation_journal` - дневник на амортизациите

### Важни полета
```sql
-- fixed_assets
document_number VARCHAR(50)    -- Номер на документ
document_date DATE             -- Дата на документ
put_into_service_date DATE     -- Дата на въвеждане
accounting_book_value DECIMAL  -- Счетоводна балансова стойност
tax_book_value DECIMAL         -- Данъчна балансова стойност

-- fixed_asset_categories
asset_account_code VARCHAR     -- Сметка за актива
depreciation_account_code VARCHAR  -- Сметка за амортизация
expense_account_code VARCHAR   -- Сметка за разход
```

## Бележки

1. **Валута**: Всички стойности са в базова валута EUR
2. **ДДС**: ДМА се купуват чрез ДДС формата, модулът е самостоятелен
3. **Последователност**: Амортизациите се изчисляват последователно по месеци
4. **Разлики**: Разликата между счетоводна и данъчна амортизация създава временни разлики за отсрочени данъци
