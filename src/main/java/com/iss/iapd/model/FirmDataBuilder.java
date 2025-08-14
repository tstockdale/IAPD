package com.iss.iapd.model;

/**
 * Builder pattern implementation for FirmData
 */
public class FirmDataBuilder {
    private String SECRgnCD = "";
    private String firmCrdNb = "";
    private String SECNb = "";
    private String busNm = "";
    private String legalNm = "";
    private String street1 = "";
    private String street2 = "";
    private String city = "";
    private String state = "";
    private String country = "";
    private String postalCode = "";
    private String phoneNumber = "";
    private String faxNumber = "";
    private String firmType = "";
    private String registrationState = "";
    private String registrationDate = "";
    private String filingDate = "";
    private String formVersion = "";
    private String totalEmployees = "";
    private String AUM = "";
    private String totalAccounts = "";
    private String brochureURL = "";
    
    public FirmDataBuilder setSECRgnCD(String SECRgnCD) {
        this.SECRgnCD = SECRgnCD;
        return this;
    }
    
    public FirmDataBuilder setFirmCrdNb(String firmCrdNb) {
        this.firmCrdNb = firmCrdNb;
        return this;
    }
    
    public FirmDataBuilder setSECNb(String SECNb) {
        this.SECNb = SECNb;
        return this;
    }
    
    public FirmDataBuilder setBusNm(String busNm) {
        this.busNm = busNm;
        return this;
    }
    
    public FirmDataBuilder setLegalNm(String legalNm) {
        this.legalNm = legalNm;
        return this;
    }
    
    public FirmDataBuilder setStreet1(String street1) {
        this.street1 = street1 != null ? street1 : "";
        return this;
    }
    
    public FirmDataBuilder setStreet2(String street2) {
        this.street2 = street2 != null ? street2 : "";
        return this;
    }
    
    public FirmDataBuilder setCity(String city) {
        this.city = city != null ? city : "";
        return this;
    }
    
    public FirmDataBuilder setState(String state) {
        this.state = state != null ? state : "";
        return this;
    }
    
    public FirmDataBuilder setCountry(String country) {
        this.country = country != null ? country : "";
        return this;
    }
    
    public FirmDataBuilder setPostalCode(String postalCode) {
        this.postalCode = postalCode != null ? postalCode : "";
        return this;
    }
    
    public FirmDataBuilder setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        return this;
    }
    
    public FirmDataBuilder setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber != null ? faxNumber : "";
        return this;
    }
    
    public FirmDataBuilder setFirmType(String firmType) {
        this.firmType = firmType != null ? firmType : "";
        return this;
    }
    
    public FirmDataBuilder setRegistrationState(String registrationState) {
        this.registrationState = registrationState != null ? registrationState : "";
        return this;
    }
    
    public FirmDataBuilder setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate != null ? registrationDate : "";
        return this;
    }
    
    public FirmDataBuilder setFilingDate(String filingDate) {
        this.filingDate = filingDate != null ? filingDate : "";
        return this;
    }
    
    public FirmDataBuilder setFormVersion(String formVersion) {
        this.formVersion = formVersion != null ? formVersion : "";
        return this;
    }
    
    public FirmDataBuilder setTotalEmployees(String totalEmployees) {
        this.totalEmployees = totalEmployees != null ? totalEmployees : "";
        return this;
    }
    
    public FirmDataBuilder setAUM(String AUM) {
        this.AUM = AUM != null ? AUM : "";
        return this;
    }
    
    public FirmDataBuilder setTotalAccounts(String totalAccounts) {
        this.totalAccounts = totalAccounts != null ? totalAccounts : "";
        return this;
    }
    
    public FirmDataBuilder setBrochureURL(String brochureURL) {
        this.brochureURL = brochureURL != null ? brochureURL : "";
        return this;
    }
    
    // Getter for firmCrdNb (needed during processing)
    public String getFirmCrdNb() {
        return firmCrdNb;
    }
    
    public FirmData build() {
        return new FirmData(SECRgnCD, firmCrdNb, SECNb, busNm, legalNm, street1, street2,
                           city, state, country, postalCode, phoneNumber, faxNumber,
                           firmType, registrationState, registrationDate, filingDate,
                           formVersion, totalEmployees, AUM, totalAccounts, brochureURL);
    }
}
