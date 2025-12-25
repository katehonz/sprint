# Система за права и роли

## Архитектура

Системата за управление на права е базирана на RBAC (Role-Based Access Control) модел с три нива:

```
Потребител (User) → Компания (Company) → Роля (Role) → Права (Permissions)
```

### Ентитети

#### 1. Permission (Право)
Атомарно право за извършване на конкретно действие.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Уникален идентификатор |
| name | String | Име на правото (уникално) |

#### 2. Role (Роля)
Колекция от права, която се присвоява на потребители.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Уникален идентификатор |
| name | String | Име на ролята (уникално) |
| permissions | Set<Permission> | Списък с права |

#### 3. UserCompany (Потребител-Компания)
Връзка между потребител и компания с определена роля.

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Уникален идентификатор |
| user | User | Потребител |
| company | Company | Компания |
| role | Role | Роля в тази компания |

## Стандартни права

### Управление на компании
- `COMPANY_CREATE` - Създаване на нова компания
- `COMPANY_EDIT` - Редактиране на компания
- `COMPANY_DELETE` - Изтриване на компания
- `COMPANY_VIEW` - Преглед на компания

### Управление на потребители
- `USER_CREATE` - Създаване на потребител
- `USER_EDIT` - Редактиране на потребител
- `USER_DELETE` - Изтриване на потребител
- `USER_VIEW` - Преглед на потребители
- `USER_MANAGE_ROLES` - Управление на роли на потребители

### Счетоводни операции
- `JOURNAL_CREATE` - Създаване на счетоводен запис
- `JOURNAL_EDIT` - Редактиране на счетоводен запис
- `JOURNAL_DELETE` - Изтриване на счетоводен запис
- `JOURNAL_POST` - Осчетоводяване (контиране)
- `JOURNAL_UNPOST` - Отмяна на осчетоводяване

### Сметкоплан
- `ACCOUNT_CREATE` - Създаване на сметка
- `ACCOUNT_EDIT` - Редактиране на сметка
- `ACCOUNT_DELETE` - Изтриване на сметка
- `ACCOUNT_VIEW` - Преглед на сметкоплан

### Контрагенти
- `COUNTERPART_CREATE` - Създаване на контрагент
- `COUNTERPART_EDIT` - Редактиране на контрагент
- `COUNTERPART_DELETE` - Изтриване на контрагент
- `COUNTERPART_VIEW` - Преглед на контрагенти

### ДДС
- `VAT_RETURN_CREATE` - Създаване на ДДС декларация
- `VAT_RETURN_EDIT` - Редактиране на ДДС декларация
- `VAT_RETURN_SUBMIT` - Подаване на ДДС декларация
- `VAT_RETURN_VIEW` - Преглед на ДДС декларации

### Дълготрайни активи
- `FIXED_ASSET_CREATE` - Създаване на ДМА
- `FIXED_ASSET_EDIT` - Редактиране на ДМА
- `FIXED_ASSET_DELETE` - Изтриване на ДМА
- `FIXED_ASSET_DEPRECIATE` - Начисляване на амортизации
- `FIXED_ASSET_DISPOSE` - Бракуване/продажба на ДМА

### Банкови операции
- `BANK_IMPORT` - Импорт на банкови извлечения
- `BANK_PROFILE_MANAGE` - Управление на банкови профили

### Справки
- `REPORT_VIEW` - Преглед на справки
- `REPORT_EXPORT` - Експорт на справки

### Системни
- `SYSTEM_ADMIN` - Пълен достъп до системата
- `SETTINGS_MANAGE` - Управление на настройки

## Стандартни роли

### SUPERADMIN
Пълен достъп до цялата система. Има всички права.

### ADMIN
Администратор на компания. Има права за управление на потребители и настройки в рамките на компанията.

Права:
- Всички права за компанията с изключение на `SYSTEM_ADMIN`

### ACCOUNTANT
Счетоводител. Може да извършва всички счетоводни операции.

Права:
- `JOURNAL_*` - Всички права за журнални записи
- `ACCOUNT_VIEW` - Преглед на сметкоплан
- `COUNTERPART_*` - Всички права за контрагенти
- `VAT_RETURN_*` - Всички права за ДДС
- `FIXED_ASSET_*` - Всички права за ДМА
- `BANK_IMPORT` - Импорт на банкови извлечения
- `REPORT_*` - Всички права за справки

### OPERATOR
Оператор. Може да въвежда документи, но не може да осчетоводява.

Права:
- `JOURNAL_CREATE`, `JOURNAL_EDIT`, `JOURNAL_VIEW`
- `COUNTERPART_CREATE`, `COUNTERPART_EDIT`, `COUNTERPART_VIEW`
- `REPORT_VIEW`

### VIEWER
Преглед само. Не може да променя данни.

Права:
- `COMPANY_VIEW`
- `ACCOUNT_VIEW`
- `COUNTERPART_VIEW`
- `JOURNAL_VIEW` (без редакция)
- `REPORT_VIEW`

## API Endpoints

### Права
```
GET    /api/permissions           - Списък с всички права
POST   /api/permissions           - Създаване на право (SYSTEM_ADMIN)
DELETE /api/permissions/{id}      - Изтриване на право (SYSTEM_ADMIN)
```

### Роли
```
GET    /api/roles                 - Списък с всички роли
GET    /api/roles/{id}            - Детайли за роля
POST   /api/roles                 - Създаване на роля (ADMIN)
PUT    /api/roles/{id}            - Редактиране на роля (ADMIN)
DELETE /api/roles/{id}            - Изтриване на роля (ADMIN)
```

### Потребители в компания
```
GET    /api/companies/{id}/users              - Списък с потребители
POST   /api/companies/{id}/users              - Добавяне на потребител
PUT    /api/companies/{id}/users/{userId}     - Промяна на роля
DELETE /api/companies/{id}/users/{userId}     - Премахване на потребител
```

## Първоначални данни

### Superadmin потребител

При инсталация на системата се създава superadmin потребител:

| Поле | Стойност |
|------|----------|
| Потребителско име | `superadmin` |
| Email | `admin@example.com` |
| Парола | *(вижте .env.example за инструкции)* |

**ВАЖНО:** Сменете паролата веднага след първия вход!

## Проверка на права в код

### Backend (Java)

```java
@PreAuthorize("hasAuthority('JOURNAL_POST')")
public void postJournalEntry(Long entryId) {
    // ...
}
```

### Frontend (React)

```typescript
import { useAuth } from '../contexts/AuthContext';

const MyComponent = () => {
    const { hasPermission } = useAuth();

    return (
        <>
            {hasPermission('JOURNAL_POST') && (
                <button onClick={handlePost}>Осчетоводи</button>
            )}
        </>
    );
};
```
