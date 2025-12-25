# Производство (Production)

Модул за производствено счетоводство, базиран на технологични карти и автоматично генериране на счетоводни операции.

## Общ преглед

Модулът позволява:
- Създаване на технологични карти с етапи за производство
- Управление на производствени партиди
- Автоматично изчисляване на консумирани материали
- Проследяване на статуса на производството

## Компоненти

### Технологични карти (Technology Cards)

Технологичната карта дефинира рецепта за производство на продукт.

#### Полета

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Integer | Уникален идентификатор |
| `companyId` | Integer | ID на компанията |
| `code` | String(50) | Уникален код (в рамките на компанията) |
| `name` | String | Наименование |
| `description` | Text | Описание |
| `outputAccountId` | Integer | Сметка за готова продукция (изход) |
| `outputQuantity` | BigDecimal | Количество готова продукция |
| `outputUnit` | String(20) | Мерна единица (кг, бр, л...) |
| `isActive` | Boolean | Активна/Неактивна |
| `stages` | List | Етапи (суровини/материали) |
| `createdAt` | DateTime | Дата на създаване |
| `updatedAt` | DateTime | Дата на промяна |

#### Етапи на технологична карта (Technology Card Stages)

Всеки етап представлява входящ материал/суровина.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Integer | Уникален идентификатор |
| `technologyCardId` | Integer | ID на технологичната карта |
| `stageOrder` | Integer | Пореден номер на етапа |
| `name` | String | Наименование на етапа |
| `description` | Text | Описание |
| `inputAccountId` | Integer | Сметка за суровина/материал (вход) |
| `inputQuantity` | BigDecimal | Необходимо количество |
| `inputUnit` | String(20) | Мерна единица |

### Производствени партиди (Production Batches)

Конкретна производствена поръчка по дадена технологична карта.

#### Полета

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Integer | Уникален идентификатор |
| `companyId` | Integer | ID на компанията |
| `technologyCardId` | Integer | ID на технологичната карта |
| `batchNumber` | String(50) | Номер на партида (уникален за компанията) |
| `plannedQuantity` | BigDecimal | Планирано количество |
| `actualQuantity` | BigDecimal | Реално произведено количество |
| `status` | Enum | Статус на партидата |
| `startedAt` | DateTime | Начало на производството |
| `completedAt` | DateTime | Край на производството |
| `notes` | Text | Бележки |
| `createdBy` | Integer | ID на потребителя създал партидата |
| `stages` | List | Етапи на партидата |
| `createdAt` | DateTime | Дата на създаване |
| `updatedAt` | DateTime | Дата на промяна |

#### Статуси на партида

| Статус | Описание |
|--------|----------|
| `PLANNED` | Планирана - очаква стартиране |
| `IN_PROGRESS` | В процес на изпълнение |
| `COMPLETED` | Завършена |
| `CANCELLED` | Отказана |

### Етапи на производствена партида (Production Batch Stages)

Конкретни етапи за изпълнение в рамките на партида.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | Integer | Уникален идентификатор |
| `productionBatchId` | Integer | ID на партидата |
| `technologyCardStageId` | Integer | ID на етапа от тех. карта |
| `plannedQuantity` | BigDecimal | Планирано количество |
| `actualQuantity` | BigDecimal | Реално изразходвано количество |
| `status` | Enum | Статус на етапа |
| `startedAt` | DateTime | Начало |
| `completedAt` | DateTime | Край |
| `journalEntryId` | Integer | ID на счетоводна статия |
| `notes` | Text | Бележки |

#### Статуси на етап

| Статус | Описание |
|--------|----------|
| `PENDING` | Очакващ |
| `IN_PROGRESS` | В процес |
| `COMPLETED` | Завършен |
| `CANCELLED` | Отказан |

## GraphQL API

### Queries

```graphql
# Технологични карти
technologyCards(companyId: Int!): [TechnologyCard!]!
activeTechnologyCards(companyId: Int!): [TechnologyCard!]!
technologyCard(id: Int!): TechnologyCard
technologyCardWithStages(id: Int!): TechnologyCard

# Производствени партиди
productionBatches(companyId: Int!): [ProductionBatch!]!
productionBatchesByFilter(filter: ProductionBatchFilterInput!): [ProductionBatch!]!
productionBatch(id: Int!): ProductionBatch
productionBatchWithDetails(id: Int!): ProductionBatch

# Етапи
productionBatchStages(productionBatchId: Int!): [ProductionBatchStage!]!

# Статистики
technologyCardsCount(companyId: Int!): Int!
productionBatchesCount(companyId: Int!): Int!
activeProductionBatchesCount(companyId: Int!): Int!
```

### Mutations

```graphql
# Технологични карти
createTechnologyCard(input: CreateTechnologyCardInput!): TechnologyCard!
updateTechnologyCard(input: UpdateTechnologyCardInput!): TechnologyCard!
deleteTechnologyCard(id: Int!): Boolean!

# Производствени партиди
createProductionBatch(input: CreateProductionBatchInput!): ProductionBatch!
updateProductionBatch(input: UpdateProductionBatchInput!): ProductionBatch!
startProductionBatch(id: Int!): ProductionBatch!
completeProductionBatch(id: Int!): ProductionBatch!
cancelProductionBatch(id: Int!): ProductionBatch!
deleteProductionBatch(id: Int!): Boolean!

# Етапи
completeProductionStage(input: CompleteProductionStageInput!): ProductionBatchStage!
cancelProductionStage(stageId: Int!): ProductionBatchStage!
```

### Input типове

```graphql
input CreateTechnologyCardInput {
    companyId: Int!
    code: String!
    name: String!
    description: String
    outputAccountId: Int!
    outputQuantity: BigDecimal
    outputUnit: String
    stages: [TechnologyCardStageInput!]
}

input TechnologyCardStageInput {
    stageOrder: Int!
    name: String!
    description: String
    inputAccountId: Int!
    inputQuantity: BigDecimal!
    inputUnit: String
}

input CreateProductionBatchInput {
    companyId: Int!
    technologyCardId: Int!
    batchNumber: String!
    plannedQuantity: BigDecimal!
    notes: String
}

input ProductionBatchFilterInput {
    companyId: Int!
    status: ProductionBatchStatus
    technologyCardId: Int
    startDate: DateTime
    endDate: DateTime
}

input CompleteProductionStageInput {
    productionBatchStageId: Int!
    actualQuantity: BigDecimal!
    notes: String
}
```

## Работен процес

### 1. Създаване на технологична карта

```
1. Дефиниране на изходния продукт
   - Код и наименование
   - Изходна сметка (напр. 303 Продукция)
   - Количество и мярка

2. Добавяне на етапи (входове)
   - За всеки материал/суровина:
     - Входна сметка (напр. 302 Материали)
     - Количество за единица продукция
     - Мерна единица
```

### 2. Стартиране на производство

```
1. Създаване на производствена партида
   - Избор на технологична карта
   - Номер на партида (автоматичен или ръчен)
   - Планирано количество

2. Автоматично генериране на етапи
   - За всеки етап от тех. карта се създава етап на партидата
   - Количествата се умножават по планираното количество

3. Стартиране на партидата
   - Промяна на статус от PLANNED → IN_PROGRESS
   - Записва се startedAt
```

### 3. Изпълнение на етапи

```
1. Завършване на етап
   - Въвеждане на реално изразходвано количество
   - Опционални бележки
   - TODO: Генериране на счетоводна статия

2. При завършване на всички етапи
   - Партидата може да бъде завършена
```

### 4. Завършване на партида

```
1. Проверка за незавършени етапи
2. Промяна на статус IN_PROGRESS → COMPLETED
3. Записва се completedAt
4. TODO: Генериране на финална счетоводна статия
```

## Счетоводни операции (TODO)

При завършване на производствен етап се генерира следната операция:

```
Дт 611 Разходи за основна дейност
    Кт 302 Материали (входна сметка)
```

При завършване на партидата:

```
Дт 303 Продукция (изходна сметка)
    Кт 611 Разходи за основна дейност
```

## Пример

### Технологична карта: Хляб "Добруджа"

```
Код: BREAD-001
Наименование: Хляб "Добруджа"
Изходна сметка: 303 Продукция
Изходно количество: 100 бр

Етапи:
1. Брашно
   - Сметка: 302.01 Брашно
   - Количество: 50 кг

2. Мая
   - Сметка: 302.02 Мая
   - Количество: 2 кг

3. Сол
   - Сметка: 302.03 Сол
   - Количество: 1 кг
```

### Производствена партида

```
Номер: BATCH-20251201-0001
Технологична карта: BREAD-001
Планирано количество: 200 бр (2x базово)

Автоматично генерирани етапи:
1. Брашно: 100 кг (50 × 2)
2. Мая: 4 кг (2 × 2)
3. Сол: 2 кг (1 × 2)
```

## База данни

### Таблици

```sql
-- Технологични карти
CREATE TABLE technology_cards (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    output_account_id INTEGER NOT NULL REFERENCES accounts(id),
    output_quantity NUMERIC(19,4) DEFAULT 1,
    output_unit VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(company_id, code)
);

-- Етапи на технологични карти
CREATE TABLE technology_card_stages (
    id SERIAL PRIMARY KEY,
    technology_card_id INTEGER NOT NULL REFERENCES technology_cards(id) ON DELETE CASCADE,
    stage_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    input_account_id INTEGER NOT NULL REFERENCES accounts(id),
    input_quantity NUMERIC(19,4) NOT NULL,
    input_unit VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Производствени партиди
CREATE TABLE production_batches (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    technology_card_id INTEGER NOT NULL REFERENCES technology_cards(id),
    batch_number VARCHAR(50) NOT NULL,
    planned_quantity NUMERIC(19,4) NOT NULL,
    actual_quantity NUMERIC(19,4),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    notes TEXT,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(company_id, batch_number)
);

-- Етапи на производствени партиди
CREATE TABLE production_batch_stages (
    id SERIAL PRIMARY KEY,
    production_batch_id INTEGER NOT NULL REFERENCES production_batches(id) ON DELETE CASCADE,
    technology_card_stage_id INTEGER NOT NULL REFERENCES technology_card_stages(id),
    planned_quantity NUMERIC(19,4) NOT NULL,
    actual_quantity NUMERIC(19,4),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

## Валидации

### Технологични карти

- `code` е уникален в рамките на компанията
- `outputAccountId` трябва да съществува
- При изтриване: не може да се изтрие карта с производствени партиди

### Производствени партиди

- `batchNumber` е уникален в рамките на компанията
- Само партиди със статус `PLANNED` могат да бъдат стартирани
- Само партиди със статус `IN_PROGRESS` могат да бъдат завършени
- Партида не може да бъде завършена преди всички етапи да са `COMPLETED` или `CANCELLED`
- Партиди със статус `COMPLETED` не могат да бъдат отказани
- Само партиди със статус `PLANNED` или `CANCELLED` могат да бъдат изтрити

### Етапи на партида

- Само етапи със статус `PENDING` или `IN_PROGRESS` могат да бъдат завършени
- Етапи със статус `COMPLETED` не могат да бъдат отказани

## Frontend

Страницата Production (`/production`) съдържа три раздела:

1. **Технологични карти** - списък и управление
2. **Производствени партиди** - списък със статуси и действия
3. **Справки** - (TODO) производствени справки

### Действия в интерфейса

| Статус | Достъпни действия |
|--------|-------------------|
| PLANNED | Започни, Изтрий |
| IN_PROGRESS | Завърши, Отмени |
| COMPLETED | - |
| CANCELLED | - |
