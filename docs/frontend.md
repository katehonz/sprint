# Frontend документация

React frontend за българска счетоводна система.

## Структура

```
frontend/
├── src/
│   ├── components/
│   │   └── layout/
│   │       ├── Layout.tsx      # Основен layout с Outlet
│   │       ├── Sidebar.tsx     # Странична лента с навигация
│   │       └── Header.tsx      # Горна лента
│   ├── graphql/
│   │   ├── client.ts           # Apollo Client конфигурация
│   │   └── queries.ts          # GraphQL queries и mutations
│   ├── pages/
│   │   ├── Dashboard.tsx       # Табло с обобщена информация
│   │   ├── Companies.tsx       # Управление на компании
│   │   ├── Accounts.tsx        # Сметкоплан (йерархично дърво)
│   │   ├── JournalEntries.tsx  # Списък журнални записи
│   │   ├── JournalEntryForm.tsx # Форма за журнален запис
│   │   ├── VATEntry.tsx        # ДДС операции (фактури)
│   │   ├── VatRates.tsx        # ДДС ставки
│   │   ├── VatReturns.tsx      # ДДС декларации
│   │   ├── Counterparts.tsx    # Контрагенти
│   │   ├── Currencies.tsx      # Валути и курсове
│   │   ├── FixedAssets.tsx     # Дълготрайни активи
│   │   └── Settings.tsx        # Настройки
│   ├── App.tsx                 # Routes
│   ├── main.tsx                # Entry point
│   └── index.css               # Tailwind CSS
├── postcss.config.js
├── tailwind.config.js
├── vite.config.ts
└── package.json
```

## Инсталация

```bash
npm install
```

## Стартиране

```bash
npm run dev      # Development
npm run build    # Production build
npm run preview  # Preview production build
```

## Tailwind CSS v4

Използва се новият синтаксис:

```css
/* index.css */
@import "tailwindcss";
```

PostCSS:
```javascript
// postcss.config.js
export default {
  plugins: {
    '@tailwindcss/postcss': {},
    autoprefixer: {},
  },
}
```

## Apollo Client v4

Импорти от `@apollo/client/react`:

```typescript
import { useQuery, useMutation } from '@apollo/client/react';
import { ApolloProvider } from '@apollo/client/react';
```

## Основни компоненти

### VATEntry - ДДС операции

Най-сложният компонент за въвеждане на ДДС фактури:

- Тип операция: Покупка / Продажба
- Документни типове (01-95)
- ДДС операции за дневници (пок09-41, про11-25)
- Тройна дата: Документ, ДДС, Счетоводна
- Контрагент с modal за избор
- Мултивалутност с курсове от ЕЦБ
- Счетоводни редове с баланс проверка
- Два таба: ДДС Операция и Плащане

### JournalEntryForm - Журнален запис

- Тройна дата
- Динамични редове
- Автоматична проверка за баланс
- Избор на валута

### Accounts - Сметкоплан

- Йерархично дърво
- Синтетични и аналитични сметки
- Търсене

### Counterparts - Контрагенти

Страница за управление на контрагенти с VIES валидация:

- VIES валидация на ДДС номер с бутон "Провери VIES"
- Автоматично попълване на име и адрес от VIES
- Адресът се записва в `longAddress` като един дълъг string (формат VIES)
- Типове: Клиент, Доставчик, Служител, Банка, Държавна институция, Друг

**Използване:**
1. Въведете ДДС номер (напр. BG123456789)
2. Натиснете "Провери VIES"
3. При валиден номер - данните се попълват автоматично
4. Адресът от VIES се записва в полето "Адрес от VIES"

## Локализация

- Всички labels на български
- Дати: `bg-BG` формат (31.12.2024)
- Валути: `bg-BG` формат (1 234,56 EUR)

## VatReturns - ДДС декларации

Страница за управление на ДДС декларации с експорт функционалност.

### Функции

| Бутон | Описание |
|-------|----------|
| DEKLAR | Експорт на DEKLAR.TXT (справка-декларация) |
| POKUPKI | Експорт на POKUPKI.TXT (дневник покупки) |
| PRODAGBI | Експорт на PRODAGBI.TXT (дневник продажби) |
| PDF Покупки | Експорт на PDF дневник за покупки |
| PDF Продажби | Експорт на PDF дневник за продажби |
| Подай | Маркира декларацията като подадена |

### Статуси

- **DRAFT** (Чернова) - показва бутон "Изчисли"
- **CALCULATED** (Изчислена) - показва всички експорт бутони
- **SUBMITTED** (Подадена) - без активни бутони
- **PAID** (Платена) - без активни бутони

### Примерен код за PDF експорт

```typescript
const handlePdfExport = async (
  type: 'pokupki' | 'prodajbi',
  returnId: string,
  year: number,
  month: number
) => {
  const response = await fetch(
    `http://localhost:8080/api/vat/${type}-pdf/${returnId}`
  );
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${type}_${year}_${month}.pdf`;
  link.click();
  URL.revokeObjectURL(url);
};
```

### Примерен код за TXT експорт

```typescript
const downloadFile = (filename: string, content: string) => {
  const blob = new Blob([content], {
    type: 'text/plain;charset=windows-1251'
  });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
};
```

## GraphQL Queries

```typescript
// queries.ts
export const GET_VAT_RETURNS = gql`
  query GetVatReturns($companyId: ID!) {
    vatReturns(companyId: $companyId) {
      id
      periodYear
      periodMonth
      periodFrom
      periodTo
      status
      outputVatAmount
      inputVatAmount
      vatToPay
      vatToRefund
      dueDate
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
```
