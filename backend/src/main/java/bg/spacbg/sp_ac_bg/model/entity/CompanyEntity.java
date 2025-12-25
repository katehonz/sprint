package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.RateProvider;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "companies")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String eik; // Единен идентификационен код (Unique Identification Code)

    private String vatNumber;

    private String address;

    private String city;

    private String country;

    private String phone;

    private String email;

    private String contactPerson;

    private String managerName;

    private String authorizedPerson;

    private String managerEgn;

    private String authorizedPersonEgn;

    @Column(nullable = false)
    private boolean isActive = true;

    private String contragentApiUrl;

    private String contragentApiKey;

    // TODO: Encrypt this key in the database
    private String azureFormRecognizerKey;

    private String azureFormRecognizerEndpoint;

    // Salt Edge Open Banking credentials (per company)
    // TODO: Encrypt these keys in the database
    @Column(name = "salt_edge_app_id")
    private String saltEdgeAppId;

    @Column(name = "salt_edge_secret")
    private String saltEdgeSecret;

    @Column(name = "salt_edge_enabled", nullable = false)
    private boolean saltEdgeEnabled = false;

    private String napOffice;

    @Column(nullable = false)
    private boolean enableViesValidation;

    @Column(nullable = false)
    private boolean enableAiMapping;

    @Column(nullable = false)
    private boolean autoValidateOnImport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id")
    private CurrencyEntity baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RateProvider preferredRateProvider;

    // ========== Default сметки за автоматични операции ==========

    // Каса - плащания в брой (обикновено 501)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_cash_account_id")
    private AccountEntity defaultCashAccount;

    // Клиенти (обикновено 411)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_customers_account_id")
    private AccountEntity defaultCustomersAccount;

    // Доставчици (обикновено 401)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_suppliers_account_id")
    private AccountEntity defaultSuppliersAccount;

    // Приходи от продажби - дефолт (обикновено 702 или 703)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_sales_revenue_account_id")
    private AccountEntity defaultSalesRevenueAccount;

    // ДДС на покупките (обикновено 4531)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_vat_purchase_account_id")
    private AccountEntity defaultVatPurchaseAccount;

    // ДДС на продажбите (обикновено 4532)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_vat_sales_account_id")
    private AccountEntity defaultVatSalesAccount;

    // Плащания с карта при покупки (POS терминал за плащане - обикновено 503)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_card_payment_purchase_account_id")
    private AccountEntity defaultCardPaymentPurchaseAccount;

    // Плащания с карта при продажби (POS терминал за приемане - обикновено 503 или отделна)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_card_payment_sales_account_id")
    private AccountEntity defaultCardPaymentSalesAccount;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountEntity> accounts;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryEntity> journalEntries;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
