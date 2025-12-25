package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.RateProvider;

public class UpdateCompanyInput {
    private String name;
    private String eik;
    private String vatNumber;
    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;
    private String contactPerson;
    private String managerName;
    private String managerEgn;
    private String authorizedPerson;
    private String authorizedPersonEgn;
    private Boolean isActive;
    private String napOffice;
    private Boolean enableViesValidation;
    private Boolean enableAiMapping;
    private Boolean autoValidateOnImport;
    private RateProvider preferredRateProvider;
    private String azureFormRecognizerEndpoint;
    private String azureFormRecognizerKey;
    private Integer baseCurrencyId;
    private Integer defaultCashAccountId;
    private Integer defaultCustomersAccountId;
    private Integer defaultSuppliersAccountId;
    private Integer defaultSalesRevenueAccountId;
    private Integer defaultVatPurchaseAccountId;
    private Integer defaultVatSalesAccountId;
    private Integer defaultCardPaymentPurchaseAccountId;
    private Integer defaultCardPaymentSalesAccountId;
    private String saltEdgeAppId;
    private String saltEdgeSecret;
    private Boolean saltEdgeEnabled;

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEik() {
        return eik;
    }

    public void setEik(String eik) {
        this.eik = eik;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getManagerEgn() {
        return managerEgn;
    }

    public void setManagerEgn(String managerEgn) {
        this.managerEgn = managerEgn;
    }

    public String getAuthorizedPerson() {
        return authorizedPerson;
    }

    public void setAuthorizedPerson(String authorizedPerson) {
        this.authorizedPerson = authorizedPerson;
    }

    public String getAuthorizedPersonEgn() {
        return authorizedPersonEgn;
    }

    public void setAuthorizedPersonEgn(String authorizedPersonEgn) {
        this.authorizedPersonEgn = authorizedPersonEgn;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getNapOffice() {
        return napOffice;
    }

    public void setNapOffice(String napOffice) {
        this.napOffice = napOffice;
    }

    public Boolean getEnableViesValidation() {
        return enableViesValidation;
    }

    public void setEnableViesValidation(Boolean enableViesValidation) {
        this.enableViesValidation = enableViesValidation;
    }

    public Boolean getEnableAiMapping() {
        return enableAiMapping;
    }

    public void setEnableAiMapping(Boolean enableAiMapping) {
        this.enableAiMapping = enableAiMapping;
    }

    public Boolean getAutoValidateOnImport() {
        return autoValidateOnImport;
    }

    public void setAutoValidateOnImport(Boolean autoValidateOnImport) {
        this.autoValidateOnImport = autoValidateOnImport;
    }

    public RateProvider getPreferredRateProvider() {
        return preferredRateProvider;
    }

    public void setPreferredRateProvider(RateProvider preferredRateProvider) {
        this.preferredRateProvider = preferredRateProvider;
    }

    public String getAzureFormRecognizerEndpoint() {
        return azureFormRecognizerEndpoint;
    }

    public void setAzureFormRecognizerEndpoint(String azureFormRecognizerEndpoint) {
        this.azureFormRecognizerEndpoint = azureFormRecognizerEndpoint;
    }

    public String getAzureFormRecognizerKey() {
        return azureFormRecognizerKey;
    }

    public void setAzureFormRecognizerKey(String azureFormRecognizerKey) {
        this.azureFormRecognizerKey = azureFormRecognizerKey;
    }

    public Integer getBaseCurrencyId() {
        return baseCurrencyId;
    }

    public void setBaseCurrencyId(Integer baseCurrencyId) {
        this.baseCurrencyId = baseCurrencyId;
    }

    public Integer getDefaultCashAccountId() {
        return defaultCashAccountId;
    }

    public void setDefaultCashAccountId(Integer defaultCashAccountId) {
        this.defaultCashAccountId = defaultCashAccountId;
    }

    public Integer getDefaultCustomersAccountId() {
        return defaultCustomersAccountId;
    }

    public void setDefaultCustomersAccountId(Integer defaultCustomersAccountId) {
        this.defaultCustomersAccountId = defaultCustomersAccountId;
    }

    public Integer getDefaultSuppliersAccountId() {
        return defaultSuppliersAccountId;
    }

    public void setDefaultSuppliersAccountId(Integer defaultSuppliersAccountId) {
        this.defaultSuppliersAccountId = defaultSuppliersAccountId;
    }

    public Integer getDefaultSalesRevenueAccountId() {
        return defaultSalesRevenueAccountId;
    }

    public void setDefaultSalesRevenueAccountId(Integer defaultSalesRevenueAccountId) {
        this.defaultSalesRevenueAccountId = defaultSalesRevenueAccountId;
    }

    public Integer getDefaultVatPurchaseAccountId() {
        return defaultVatPurchaseAccountId;
    }

    public void setDefaultVatPurchaseAccountId(Integer defaultVatPurchaseAccountId) {
        this.defaultVatPurchaseAccountId = defaultVatPurchaseAccountId;
    }

    public Integer getDefaultVatSalesAccountId() {
        return defaultVatSalesAccountId;
    }

    public void setDefaultVatSalesAccountId(Integer defaultVatSalesAccountId) {
        this.defaultVatSalesAccountId = defaultVatSalesAccountId;
    }

    public Integer getDefaultCardPaymentPurchaseAccountId() {
        return defaultCardPaymentPurchaseAccountId;
    }

    public void setDefaultCardPaymentPurchaseAccountId(Integer defaultCardPaymentPurchaseAccountId) {
        this.defaultCardPaymentPurchaseAccountId = defaultCardPaymentPurchaseAccountId;
    }

    public Integer getDefaultCardPaymentSalesAccountId() {
        return defaultCardPaymentSalesAccountId;
    }

    public void setDefaultCardPaymentSalesAccountId(Integer defaultCardPaymentSalesAccountId) {
        this.defaultCardPaymentSalesAccountId = defaultCardPaymentSalesAccountId;
    }

    public String getSaltEdgeAppId() {
        return saltEdgeAppId;
    }

    public void setSaltEdgeAppId(String saltEdgeAppId) {
        this.saltEdgeAppId = saltEdgeAppId;
    }

    public String getSaltEdgeSecret() {
        return saltEdgeSecret;
    }

    public void setSaltEdgeSecret(String saltEdgeSecret) {
        this.saltEdgeSecret = saltEdgeSecret;
    }

    public Boolean getSaltEdgeEnabled() {
        return saltEdgeEnabled;
    }

    public void setSaltEdgeEnabled(Boolean saltEdgeEnabled) {
        this.saltEdgeEnabled = saltEdgeEnabled;
    }
}