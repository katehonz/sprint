# Backend документация

Java Spring Boot backend с GraphQL API.

## Технологии

- Java 21
- Spring Boot 3.x
- Spring GraphQL
- PostgreSQL
- Flyway миграции
- Lombok

## Структура

```
backend/
├── src/main/java/bg/codix/spacbg/
│   ├── config/           # Конфигурации
│   ├── controller/       # GraphQL resolvers
│   ├── entity/           # JPA entities
│   ├── repository/       # Spring Data repositories
│   ├── service/          # Business logic
│   └── SpAcBgApplication.java
├── src/main/resources/
│   ├── graphql/
│   │   └── schema.graphqls  # GraphQL схема
│   ├── db/migration/        # Flyway миграции
│   └── application.yml      # Конфигурация
└── pom.xml
```

## Стартиране

```bash
./mvnw spring-boot:run
```

- API: http://localhost:8080/graphql
- GraphiQL: http://localhost:8080/graphiql

## GraphQL API

### Queries

```graphql
# Компании
companies: [Company!]!
company(id: ID!): Company

# Сметки
accounts(companyId: ID!): [Account!]!
accountHierarchy(companyId: ID!): [Account!]!

# Журнални записи
journalEntries(filter: JournalEntryFilter!): [JournalEntry!]!
journalEntry(id: ID!): JournalEntry

# Контрагенти
counterparts(companyId: ID!): [Counterpart!]!

# Валути
currencies: [Currency!]!
baseCurrency: Currency
latestExchangeRate(fromCode: String!, toCode: String!): ExchangeRate

# ДДС
vatRates(companyId: ID!): [VatRate!]!
vatReturns(companyId: ID!): [VatReturn!]!

# Дълготрайни активи
fixedAssets(companyId: ID!): [FixedAsset!]!
```

### Mutations

```graphql
# Компании
createCompany(input: CreateCompanyInput!): Company!
updateCompany(id: ID!, input: UpdateCompanyInput!): Company!
deleteCompany(id: ID!): Boolean!

# Журнални записи
createJournalEntry(input: CreateJournalEntryInput!): JournalEntry!
updateJournalEntry(id: ID!, input: UpdateJournalEntryInput!): JournalEntry!
postJournalEntry(id: ID!): JournalEntry!
unpostJournalEntry(id: ID!): JournalEntry!
deleteJournalEntry(id: ID!): Boolean!

# Валути
fetchEcbRates(date: Date): [ExchangeRate!]!

# ДДС
createVatRate(input: CreateVatRateInput!): VatRate!
generateVatReturn(input: GenerateVatReturnInput!): VatReturn!
submitVatReturn(id: ID!): VatReturn!
```

## База данни

PostgreSQL с Flyway миграции.

Връзка (application.yml):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/spacbg
    username: spacbg
    password: spacbg
```

## ЕЦБ курсове

Системата използва само курсове от Европейската централна банка:

```java
@Service
public class EcbExchangeRateService {
    private static final String ECB_URL =
        "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    public List<ExchangeRate> fetchRates() {
        // Парсира XML от ЕЦБ
    }
}
```

Фиксиран курс BGN/EUR: 1.95583

## REST API Endpoints

Освен GraphQL, системата има REST endpoints за бинарни файлове:

### ДДС PDF експорт

```
GET /api/vat/pokupki-pdf/{returnId}   - PDF дневник за покупки
GET /api/vat/prodajbi-pdf/{returnId}  - PDF дневник за продажби
```

Response: `application/pdf`

## Ключови сервизи

### ViesService

Валидира ДДС номера срещу EU VIES системата:

```java
@Service
public class ViesServiceImpl implements ViesService {

    // Валидира ДДС номер и връща данни за фирмата
    public ViesValidationResult validateVat(String vatNumber);
}
```

**Функционалност:**
- Проверява валидността на ДДС номера в EU VIES
- Извлича име и адрес на фирмата (ако са публични)
- Поддържа REST API с fallback на SOAP
- Адресът се записва в `longAddress` като един string

**GraphQL mutation:**
```graphql
mutation {
  validateVat(vatNumber: "BG123456789") {
    valid
    name
    longAddress
    countryCode
  }
}
```

**Важно:** Някои държави (напр. Германия) не предоставят публично име и адрес.

### VatPdfExportService

Генерира PDF дневници с динамични колони (само ненулевите):

```java
@Service
public class VatPdfExportService {

    // Генерира PDF за дневник покупки
    public byte[] exportPokupkiPdf(Integer returnId);

    // Генерира PDF за дневник продажби
    public byte[] exportProdajbiPdf(Integer returnId);
}
```

Използва OpenPDF библиотека за генериране на PDF с:
- Кирилица поддръжка (CP1251 encoding)
- Динамични колони
- Цветен header
- Тотали в края

### VatService

Управлява ДДС операции:

```java
@Service
public class VatServiceImpl implements VatService {

    // TXT експорт за НАП
    String exportDeklar(Integer returnId);   // DEKLAR.TXT
    String exportPokupki(Integer returnId);  // POKUPKI.TXT
    String exportProdajbi(Integer returnId); // PRODAGBI.TXT

    // ДДС декларации
    VatReturnEntity generateReturn(GenerateVatReturnInput input, Integer userId);
    VatReturnEntity submitReturn(Integer id, Integer userId);
}
```

## Зависимости (pom.xml)

```xml
<!-- PDF генериране -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>2.0.3</version>
</dependency>

<!-- Azure Form Recognizer (OCR) -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-ai-formrecognizer</artifactId>
</dependency>

<!-- GraphQL scalars -->
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-extended-scalars</artifactId>
    <version>21.0</version>
</dependency>
```
