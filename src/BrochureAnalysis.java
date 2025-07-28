import java.util.HashSet;
import java.util.Set;

/**
 * Data class to hold brochure analysis results
 */
public class BrochureAnalysis {
    private StringBuilder proxyProvider = new StringBuilder();
    private StringBuilder classActionProvider = new StringBuilder();
    private StringBuilder esgProvider = new StringBuilder();
    private StringBuilder esgInvestmentLanguage = new StringBuilder();
    private StringBuilder noVoteString = new StringBuilder();
    private StringBuilder emailSentence = new StringBuilder();
    private StringBuilder emailComplianceSentence = new StringBuilder();
    private StringBuilder emailProxySentence = new StringBuilder();
    private StringBuilder emailBrochureSentence = new StringBuilder();
    private StringBuilder emailSetString = new StringBuilder();
    private Set<String> emailSet = new HashSet<>();
    
    // Getters
    public StringBuilder getProxyProvider() { return proxyProvider; }
    public StringBuilder getClassActionProvider() { return classActionProvider; }
    public StringBuilder getEsgProvider() { return esgProvider; }
    public StringBuilder getEsgInvestmentLanguage() { return esgInvestmentLanguage; }
    public StringBuilder getNoVoteString() { return noVoteString; }
    public StringBuilder getEmailSentence() { return emailSentence; }
    public StringBuilder getEmailComplianceSentence() { return emailComplianceSentence; }
    public StringBuilder getEmailProxySentence() { return emailProxySentence; }
    public StringBuilder getEmailBrochureSentence() { return emailBrochureSentence; }
    public StringBuilder getEmailSetString() { return emailSetString; }
    public Set<String> getEmailSet() { return emailSet; }
    
    /**
     * Builds the email set string from individual email addresses
     */
    public void buildEmailSetString() {
        emailSetString.setLength(0); // Clear existing content
        for (String email : emailSet) {
            emailSetString.append(email).append("|");
        }
    }
    
    /**
     * Gets the formatted email set string (without trailing pipe)
     */
    public String getFormattedEmailSetString() {
        if (emailSetString.length() == 0) {
            return "";
        }
        return emailSetString.toString().substring(0, emailSetString.length() - 1);
    }
    
    /**
     * Adds a provider to the specified provider list
     */
    public void addProvider(StringBuilder providerList, String providerName) {
        if (providerList.length() == 0) {
            providerList.append(providerName);
        } else {
            providerList.append("|").append(providerName);
        }
    }
    
    /**
     * Sanitizes text for CSV output by removing quotes
     */
    public String sanitizeForCSV(String text) {
        return text != null ? text.replaceAll("\"", "") : "";
    }
}
