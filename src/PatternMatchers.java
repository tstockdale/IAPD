import java.util.regex.Pattern;

/**
 * Pattern matchers for content analysis in IAPD brochures
 */
public final class PatternMatchers {
    
    // Regex patterns for various providers and services
    public static final String GLASS_LEWIS_MATCHER = "(?:Glass-Lewis|Glass Lewis|Glass, Lewis|Glass,Lewis|Glass,\\s+Lewis|Glass\\s+Lewis?)";
    public static final String BROADRIDGE_MATCHER = "Broadridge";
    public static final String SUSTAINALYTICS_MATCHER = "Sustainalytics";
    public static final String MSCI_MATCHER = "(?:ESG.{1,100}MSCI|MSCI.{1,100}ESG)";
    public static final String ESG_MATCHER = "(?:ESG integration|integration of ESG|integration of (?:e|E)nvironmental|integrates ESG| integrates (?:e|E)nvironmental|ESG [^.]{1,300}integra|integrat[^.]{1,300}ESG)";
    public static final String PROXYEDGE_MATCHER = "(?:Proxy Edge|ProxyEdge)";
    public static final String EGAN_JONES_MATCHER = "(?:Egan Jones|Egan-Jones) Proxy";
    public static final String ISS_PROXY_MATCHER = "(?:(?:[Pp]rox[iy]|recommendation)[^.]*?ISS|ISS[^.]*?(?:[Pp]rox[iy]|recommendation)|(?:[Pp]rox[iy]|recommendation)[^.]*?Institutional Shareholder Services|Institutional Shareholder Services[^.]*?(?:[Pp]rox[iy]|recommendation))";
    public static final String FRT_MATCHER = "(?: FRT[^.]*?[Cc]lass [Aa]ction|[Cc]lass action[^.]*? FRT|Financial Recovery Technologies[^.]*?[Cc]lass [Aa]ction |Financial Recovery Technologies[^.]*?[Cc]lass [Aa]ction)";
    public static final String BATTEA_MATCHER = "(?: Battea[^.]*?[Cc]lass [Aa]ction|[Cc]lass action[^.]*? Battea)";
    public static final String CCC_MATCHER = "(?:CCC[^.]*?[Cc]lass [Aa]ction|[Cc]lass action[^.]*?CCC)";
    public static final String THIRD_PARTY_PROXY_MATCHER = "(?:[Pp]rox[iy][^.]*?[Tt]hird(?:-| )[Pp]arty|[Tt]hird(?:-| )[Pp]arty[^.]*?[Pp]rox[iy])";
    public static final String ISS_CLASS_ACTION_MATCHER = "(?:[Cc]lass [Aa]ction[^.]*?ISS|ISS[^.]*?[Cc]lass [Aa]ction|[Cc]lass [Aa]ction[^.]*?Institutional Shareholder Services|Institutional Shareholder Services[^.]*?[Cc]lass [Aa]ction)";
    public static final String EMAIL_SENTENCE_MATCHER = "Item 17.{1,500}(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.).{1,500}Item 18";
    public static final String EMAIL_COMPLIANCE_SENTENCE_MATCHER = "(?:(.{10}\\.[^.]{1,300}[Cc]ompliance[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.)|(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}[Cc]ompliance[^.]{1,300}\\.))";
    public static final String EMAIL_PROXY_SENTENCE_MATCHER = "(?:(.{10}\\.[^.]{1,300}[Pp]roxy[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.)|(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}[Pp]roxy[^.]{1,300}\\.))";
    public static final String EMAIL_BROCHURE_SENTENCE_MATCHER = "(?:(.{10}\\.[^.]{1,300}(?:[Bb]rochure|[Qq]uestion)[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}\\.)|(.{10}\\.[^.]{1,300}[\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,}[^.]{1,300}(?:[Bb]rochure|[Qq]uestion)[^.]{1,300}\\.))";
    public static final String EMAIL_MATCHER = ".([\\p{L}\\p{N}\\._%+-]+@[\\p{L}\\p{N}\\.\\-]+\\.[\\p{L}]{2,})";
    public static final String NO_VOTE_MATCHER = "(?:VOTING CLIENT SECURITIES|Voting Client Securities)[^.]{1,300}not[^.]{1,30}(?:vot[ie][^.]{1,30}(?:prox[iy]|securit)|[^.]{1,30}(?:prox[iy]|securit)[^.]{1,50}vot[ie])";
    public static final String CUSTODIAL_SERVICES_MATCHER = ".{1,200}ustodial.{1,30}services.{1,200}";
    public static final String API_BRCHR_VERSION_ID_MATCHER = "brochureVersionID..: (\\d+),";
    public static final String BRCHR_VERSION_ID_MATCHER = "BRCHR_VRSN_ID=(\\d+)";
    
    // Compiled patterns
    public static final Pattern GLASS_LEWIS_PATTERN = Pattern.compile(GLASS_LEWIS_MATCHER);
    public static final Pattern BROADRIDGE_PATTERN = Pattern.compile(BROADRIDGE_MATCHER);
    public static final Pattern PROXYEDGE_PATTERN = Pattern.compile(PROXYEDGE_MATCHER);
    public static final Pattern EGAN_JONES_PATTERN = Pattern.compile(EGAN_JONES_MATCHER);
    public static final Pattern ISS_PROXY_PATTERN = Pattern.compile(ISS_PROXY_MATCHER);
    public static final Pattern FRT_PATTERN = Pattern.compile(FRT_MATCHER);
    public static final Pattern SUSTAINALYTICS_PATTERN = Pattern.compile(SUSTAINALYTICS_MATCHER);
    public static final Pattern MSCI_PATTERN = Pattern.compile(MSCI_MATCHER);
    public static final Pattern ESG_PATTERN = Pattern.compile(ESG_MATCHER);
    public static final Pattern BATTEA_CLASS_ACTION_PATTERN = Pattern.compile(BATTEA_MATCHER);
    public static final Pattern CCC_CLASS_ACTION_PATTERN = Pattern.compile(CCC_MATCHER);
    public static final Pattern THIRD_PARTY_PROXY_PATTERN = Pattern.compile(THIRD_PARTY_PROXY_MATCHER);
    public static final Pattern ISS_CLASS_ACTION_PATTERN = Pattern.compile(ISS_CLASS_ACTION_MATCHER);
    public static final Pattern EMAIL_SENTENCE_PATTERN = Pattern.compile(EMAIL_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_COMPLIANCE_SENTENCE_PATTERN = Pattern.compile(EMAIL_COMPLIANCE_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_PROXY_SENTENCE_PATTERN = Pattern.compile(EMAIL_PROXY_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_BROCHURE_SENTENCE_PATTERN = Pattern.compile(EMAIL_BROCHURE_SENTENCE_MATCHER, Pattern.CASE_INSENSITIVE);
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_MATCHER);
    public static final Pattern NO_VOTE_PATTERN = Pattern.compile(NO_VOTE_MATCHER);
    public static final Pattern CUSTODIAL_SERVICES_PATTERN = Pattern.compile(CUSTODIAL_SERVICES_MATCHER);
    public static final Pattern API_BRCHR_VERSION_ID_PATTERN = Pattern.compile(API_BRCHR_VERSION_ID_MATCHER);
    public static final Pattern BRCHR_VERSION_ID_PATTERN = Pattern.compile(BRCHR_VERSION_ID_MATCHER);
    
    // Private constructor to prevent instantiation
    private PatternMatchers() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
