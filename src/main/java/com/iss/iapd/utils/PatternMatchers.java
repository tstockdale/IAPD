package com.iss.iapd.utils;

import java.util.regex.Pattern;

/**
 * Pattern matchers for content analysis in IAPD brochures
 * Simplified and optimized regular expressions for better maintainability
 */
public final class PatternMatchers {
    
    // Common email pattern component (no spaces, no consecutive dots in local/domain, Unicode letters/numbers)
    // local: labels separated by single dots, allowed chars [_%+\-]
    // domain: labels separated by single dots, final TLD at least 2 letters
    private static final String EMAIL_PATTERN_BASE =
        "(?:[\\p{L}\\p{N}_%+\\-]+(?:\\.[\\p{L}\\p{N}_%+\\-]+)*)@(?:[\\p{L}\\p{N}\\-]+(?:\\.[\\p{L}\\p{N}\\-]+)+)";
    
    // Helper method to create class action patterns
    private static String createClassActionPattern(String company) {
        return String.format("(?:%s[^.]*?class action|class action[^.]*?%s)", company, company);
    }
    
    
    // Simplified regex patterns for various providers and services
    public static final String GLASS_LEWIS_MATCHER = "Glass[\\s,-]*Lewis\\??";
    public static final String BROADRIDGE_MATCHER = "Broadridge";
    public static final String SUSTAINALYTICS_MATCHER = "Sustainalytics";
    public static final String MSCI_MATCHER = "(?:ESG.{1,100}MSCI|MSCI.{1,100}ESG)";
    public static final String ESG_MATCHER = "(?:environmental, social, and governance|ESG|sustainable investing|ESG integration|integration of (?:ESG|environmental)|integrates (?:ESG|environmental)|ESG [^.]*?integra|integrat[^.]*?ESG)";
    public static final String PROXYEDGE_MATCHER = "Proxy\\s?Edge";
    public static final String EGAN_JONES_MATCHER = "Egan[\\s-]Jones Proxy";
    public static final String ISS_PROXY_MATCHER_OLD =  "(?:(?:[Pp]rox[iy]|recommendation)[^.]*?ISS|ISS[^.]*?(?:[Pp]rox[iy]|recommendation)|(?:[Pp]rox[iy]|recommendation)[^.]*?Institutional Shareholder Services|Institutional Shareholder Services[^.]*?(?:[Pp]rox[iy]|recommendation))";
    public static final String ISS_PROXY_MATCHER = "(?:(?:proxy|recommendation)[^.]*?(?:ISS|Institutional Shareholder Services)|(?:ISS|Institutional Shareholder Services)[^.]*?(?:proxy|recommendation))";
    public static final String FRT_MATCHER = createClassActionPattern("(?:FRT|Financial Recovery Technologies)");
    public static final String BATTEA_MATCHER = createClassActionPattern("Battea");
    public static final String CCC_MATCHER = createClassActionPattern("CCC");
    public static final String ROBBINS_GELLER_MATCHER = createClassActionPattern("Robbins Geller");
    public static final String THIRD_PARTY_PROXY_MATCHER = "(?:prox[iy][^.]*?third[\\s-]party|third[\\s-]party[^.]*?prox[iy])";
    public static final String ISS_CLASS_ACTION_MATCHER = createClassActionPattern("(?:ISS|Institutional Shareholder Services)");
    public static final String EMAIL_SENTENCE_MATCHER = "Item 17.{1,500}(.{10}\\.[^.]*?" + EMAIL_PATTERN_BASE + "[^.]*?\\.).{1,500}Item 18";
    public static final String EMAIL_COMPLIANCE_SENTENCE_MATCHER = "(?:(.{10}\\.[^.]{1,300}[Cc]ompliance[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.)|(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}[Cc]ompliance[^.]{1,300}\\.))";
    public static final String EMAIL_PROXY_SENTENCE_MATCHER = "(?:(.{10}\\.[^.]{1,300}[Pp]roxy[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.)|(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}[Pp]roxy[^.]{1,300}\\.))";
    public static final String EMAIL_BROCHURE_SENTENCE_MATCHER = "(?:(.{10}\\.[^.]{1,300}(?:[Bb]rochure|[Qq]uestion)[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.)|(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}(?:[Bb]rochure|[Qq]uestion)[^.]{1,300}\\.))";
    public static final String EMAIL_MATCHER = "(" + EMAIL_PATTERN_BASE + ")";
    public static final String NO_VOTE_MATCHER = "(Item 17.{1,500}(?:abstain|not vote|do not vote|will not vote|may not vote|not[^.]{1,15}vote).{1,500}Item 18)";
    public static final String CUSTODIAL_SERVICES_MATCHER = ".{0,200}ustodial.{0,30}services.{0,200}";
    public static final String BRCHR_VERSION_ID_MATCHER = "BRCHR_VRSN_ID=(\\d+)";
    
    // Compiled patterns with appropriate case sensitivity flags
    public static final Pattern GLASS_LEWIS_PATTERN = Pattern.compile(GLASS_LEWIS_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern BROADRIDGE_PATTERN = Pattern.compile(BROADRIDGE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern PROXYEDGE_PATTERN = Pattern.compile(PROXYEDGE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EGAN_JONES_PATTERN = Pattern.compile(EGAN_JONES_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern ISS_PROXY_PATTERN = Pattern.compile(ISS_PROXY_MATCHER);
    public static final Pattern FRT_PATTERN = Pattern.compile(FRT_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern SUSTAINALYTICS_PATTERN = Pattern.compile(SUSTAINALYTICS_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern MSCI_PATTERN = Pattern.compile(MSCI_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern ESG_PATTERN = Pattern.compile(ESG_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern BATTEA_CLASS_ACTION_PATTERN = Pattern.compile(BATTEA_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern CCC_CLASS_ACTION_PATTERN = Pattern.compile(CCC_MATCHER);
    public static final Pattern ROBBINS_GELLER_CLASS_ACTION_PATTERN = Pattern.compile(ROBBINS_GELLER_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern THIRD_PARTY_PROXY_PATTERN = Pattern.compile(THIRD_PARTY_PROXY_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern ISS_CLASS_ACTION_PATTERN = Pattern.compile(ISS_CLASS_ACTION_MATCHER);
    public static final Pattern EMAIL_SENTENCE_PATTERN = Pattern.compile(EMAIL_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_COMPLIANCE_SENTENCE_PATTERN = Pattern.compile(EMAIL_COMPLIANCE_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_PROXY_SENTENCE_PATTERN = Pattern.compile(EMAIL_PROXY_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_BROCHURE_SENTENCE_PATTERN = Pattern.compile(EMAIL_BROCHURE_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern NO_VOTE_PATTERN = Pattern.compile(NO_VOTE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern CUSTODIAL_SERVICES_PATTERN = Pattern.compile(CUSTODIAL_SERVICES_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern BRCHR_VERSION_ID_PATTERN = Pattern.compile(BRCHR_VERSION_ID_MATCHER);
    
    // Private constructor to prevent instantiation
    private PatternMatchers() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
