/**
 * Data Transfer Object for firm information
 */
public class FirmData {
    private final String SECRgnCD;
    private final String firmCrdNb;
    private final String SECNb;
    private final String busNm;
    private final String legalNm;
    private final String street1;
    private final String street2;
    private final String city;
    private final String state;
    private final String country;
    private final String postalCode;
    private final String phoneNumber;
    private final String faxNumber;
    private final String firmType;
    private final String registrationState;
    private final String registrationDate;
    private final String filingDate;
    private final String formVersion;
    private final String totalEmployees;
    private final String AUM;
    private final String totalAccounts;
    private final String brochureURL;
    
    public FirmData(String SECRgnCD, String firmCrdNb, String SECNb, String busNm, String legalNm,
                   String street1, String street2, String city, String state, String country,
                   String postalCode, String phoneNumber, String faxNumber, String firmType,
                   String registrationState, String registrationDate, String filingDate,
                   String formVersion, String totalEmployees, String AUM, String totalAccounts,
                   String brochureURL) {
        this.SECRgnCD = SECRgnCD;
        this.firmCrdNb = firmCrdNb;
        this.SECNb = SECNb;
        this.busNm = busNm;
        this.legalNm = legalNm;
        this.street1 = street1;
        this.street2 = street2;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
        this.phoneNumber = phoneNumber;
        this.faxNumber = faxNumber;
        this.firmType = firmType;
        this.registrationState = registrationState;
        this.registrationDate = registrationDate;
        this.filingDate = filingDate;
        this.formVersion = formVersion;
        this.totalEmployees = totalEmployees;
        this.AUM = AUM;
        this.totalAccounts = totalAccounts;
        this.brochureURL = brochureURL;
    }
    
    // Getters
    public String getSECRgnCD() { return SECRgnCD; }
    public String getFirmCrdNb() { return firmCrdNb; }
    public String getSECNb() { return SECNb; }
    public String getBusNm() { return busNm; }
    public String getLegalNm() { return legalNm; }
    public String getStreet1() { return street1; }
    public String getStreet2() { return street2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public String getPostalCode() { return postalCode; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getFaxNumber() { return faxNumber; }
    public String getFirmType() { return firmType; }
    public String getRegistrationState() { return registrationState; }
    public String getRegistrationDate() { return registrationDate; }
    public String getFilingDate() { return filingDate; }
    public String getFormVersion() { return formVersion; }
    public String getTotalEmployees() { return totalEmployees; }
    public String getAUM() { return AUM; }
    public String getTotalAccounts() { return totalAccounts; }
    public String getBrochureURL() { return brochureURL; }
}
