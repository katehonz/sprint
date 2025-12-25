package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.RateProvider;

public class CreateCompanyInput {
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
    private String napOffice;
    private Boolean enableViesValidation;
    private Boolean enableAiMapping;
    private Boolean autoValidateOnImport;
    private RateProvider preferredRateProvider;
    private String azureFormRecognizerEndpoint;
    private String azureFormRecognizerKey;
    private Integer baseCurrencyId;

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
}