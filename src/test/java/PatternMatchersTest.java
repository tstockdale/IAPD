import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive JUnit 5 test suite for PatternMatchers
 * Tests regex pattern validation and matching behavior
 */
public class PatternMatchersTest {
    
    @Nested
    @DisplayName("Proxy Provider Pattern Tests")
    class ProxyProviderPatternTests {
        
        @Test
        @DisplayName("Should match Glass Lewis variations")
        void testGlassLewisPattern() {
            Pattern pattern = PatternMatchers.GLASS_LEWIS_PATTERN;
            
            assertTrue(pattern.matcher("Glass Lewis").find());
            assertTrue(pattern.matcher("Glass-Lewis").find());
            assertTrue(pattern.matcher("Glass, Lewis").find());
            assertTrue(pattern.matcher("Glass Lewis?").find());
            assertTrue(pattern.matcher("glass lewis").find()); // Case insensitive
            assertTrue(pattern.matcher("GLASS LEWIS").find()); // Case insensitive
            
            assertFalse(pattern.matcher("Glass").find());
            assertFalse(pattern.matcher("Lewis").find());
            assertFalse(pattern.matcher("Other Provider").find());
        }
        
        @Test
        @DisplayName("Should match Broadridge")
        void testBroadridgePattern() {
            Pattern pattern = PatternMatchers.BROADRIDGE_PATTERN;
            
            assertTrue(pattern.matcher("Broadridge").find());
            assertTrue(pattern.matcher("broadridge").find()); // Case insensitive
            assertTrue(pattern.matcher("BROADRIDGE").find()); // Case insensitive
            assertTrue(pattern.matcher("We use Broadridge for proxy services").find());
            
            assertFalse(pattern.matcher("Broad").find());
            assertFalse(pattern.matcher("Ridge").find());
            assertFalse(pattern.matcher("Other Provider").find());
        }
        
        @Test
        @DisplayName("Should match ISS proxy patterns")
        void testISSProxyPattern() {
            Pattern pattern = PatternMatchers.ISS_PROXY_PATTERN;
            
            assertTrue(pattern.matcher("proxy services from ISS").find());
            assertTrue(pattern.matcher("ISS proxy recommendations").find());
            assertTrue(pattern.matcher("recommendation from ISS").find());
            assertTrue(pattern.matcher("proxy research by Institutional Shareholder Services").find());
            assertTrue(pattern.matcher("Institutional Shareholder Services recommendations").find());
            
            assertFalse(pattern.matcher("ISS").find()); // Just ISS without proxy/recommendation
            assertFalse(pattern.matcher("proxy services").find()); // Just proxy without ISS
            assertFalse(pattern.matcher("Other proxy provider").find());
        }
        
        @Test
        @DisplayName("Should match ProxyEdge")
        void testProxyEdgePattern() {
            Pattern pattern = PatternMatchers.PROXYEDGE_PATTERN;
            
            assertTrue(pattern.matcher("ProxyEdge").find());
            assertTrue(pattern.matcher("Proxy Edge").find());
            assertTrue(pattern.matcher("proxyedge").find()); // Case insensitive
            assertTrue(pattern.matcher("PROXY EDGE").find()); // Case insensitive
            
            assertFalse(pattern.matcher("Proxy").find());
            assertFalse(pattern.matcher("Edge").find());
            assertFalse(pattern.matcher("Other Provider").find());
        }
        
        @Test
        @DisplayName("Should match Egan-Jones Proxy")
        void testEganJonesPattern() {
            Pattern pattern = PatternMatchers.EGAN_JONES_PATTERN;
            
            assertTrue(pattern.matcher("Egan-Jones Proxy").find());
            assertTrue(pattern.matcher("Egan Jones Proxy").find());
            assertTrue(pattern.matcher("egan-jones proxy").find()); // Case insensitive
            assertTrue(pattern.matcher("EGAN JONES PROXY").find()); // Case insensitive
            
            assertFalse(pattern.matcher("Egan-Jones").find()); // Without "Proxy"
            assertFalse(pattern.matcher("Jones Proxy").find());
            assertFalse(pattern.matcher("Other Provider").find());
        }
        
        @Test
        @DisplayName("Should match third party proxy patterns")
        void testThirdPartyProxyPattern() {
            Pattern pattern = PatternMatchers.THIRD_PARTY_PROXY_PATTERN;
            
            assertTrue(pattern.matcher("proxy services from third party").find());
            assertTrue(pattern.matcher("third-party proxy provider").find());
            assertTrue(pattern.matcher("third party proxy research").find());
            assertTrue(pattern.matcher("proxy voting third-party").find());
            assertTrue(pattern.matcher("THIRD-PARTY PROXY").find()); // Case insensitive
            
            assertFalse(pattern.matcher("third party").find()); // Without proxy
            assertFalse(pattern.matcher("proxy services").find()); // Without third party
            assertFalse(pattern.matcher("Other provider").find());
        }
    }
    
    @Nested
    @DisplayName("Class Action Provider Pattern Tests")
    class ClassActionProviderPatternTests {
        
        @Test
        @DisplayName("Should match FRT class action patterns")
        void testFRTPattern() {
            Pattern pattern = PatternMatchers.FRT_PATTERN;
            
            assertTrue(pattern.matcher("FRT class action monitoring").find());
            assertTrue(pattern.matcher("class action services from FRT").find());
            assertTrue(pattern.matcher("Financial Recovery Technologies class action").find());
            assertTrue(pattern.matcher("class action by Financial Recovery Technologies").find());
            assertTrue(pattern.matcher("frt class action").find()); // Case insensitive
            
            assertFalse(pattern.matcher("FRT").find()); // Without class action
            assertFalse(pattern.matcher("class action").find()); // Without FRT
            assertFalse(pattern.matcher("Other provider").find());
        }
        
        @Test
        @DisplayName("Should match Robbins Geller class action patterns")
        void testRobbinsGellerPattern() {
            Pattern pattern = PatternMatchers.ROBBINS_GELLER_CLASS_ACTION_PATTERN;
            
            assertTrue(pattern.matcher("Robbins Geller class action").find());
            assertTrue(pattern.matcher("class action by Robbins Geller").find());
            assertTrue(pattern.matcher("robbins geller class action").find()); // Case insensitive
            assertTrue(pattern.matcher("CLASS ACTION ROBBINS GELLER").find()); // Case insensitive
            
            assertFalse(pattern.matcher("Robbins Geller").find()); // Without class action
            assertFalse(pattern.matcher("class action").find()); // Without Robbins Geller
            assertFalse(pattern.matcher("Other law firm").find());
        }
        
        @Test
        @DisplayName("Should match Battea class action patterns")
        void testBatteaPattern() {
            Pattern pattern = PatternMatchers.BATTEA_CLASS_ACTION_PATTERN;
            
            assertTrue(pattern.matcher("Battea class action").find());
            assertTrue(pattern.matcher("class action services from Battea").find());
            assertTrue(pattern.matcher("battea class action").find()); // Case insensitive
            assertTrue(pattern.matcher("CLASS ACTION BATTEA").find()); // Case insensitive
            
            assertFalse(pattern.matcher("Battea").find()); // Without class action
            assertFalse(pattern.matcher("class action").find()); // Without Battea
            assertFalse(pattern.matcher("Other provider").find());
        }
        
        @Test
        @DisplayName("Should match CCC class action patterns")
        void testCCCPattern() {
            Pattern pattern = PatternMatchers.CCC_CLASS_ACTION_PATTERN;
            
            assertTrue(pattern.matcher("CCC class action").find());
            assertTrue(pattern.matcher("class action by CCC").find());
            // Note: CCC pattern is case sensitive based on the implementation
            
            assertFalse(pattern.matcher("CCC").find()); // Without class action
            assertFalse(pattern.matcher("class action").find()); // Without CCC
            assertFalse(pattern.matcher("ccc class action").find()); // Case sensitive
        }
        
        @Test
        @DisplayName("Should match ISS class action patterns")
        void testISSClassActionPattern() {
            Pattern pattern = PatternMatchers.ISS_CLASS_ACTION_PATTERN;
            
            assertTrue(pattern.matcher("ISS class action monitoring").find());
            assertTrue(pattern.matcher("class action services from ISS").find());
            assertTrue(pattern.matcher("Institutional Shareholder Services class action").find());
            assertTrue(pattern.matcher("class action by Institutional Shareholder Services").find());
            
            assertFalse(pattern.matcher("ISS").find()); // Without class action
            assertFalse(pattern.matcher("class action").find()); // Without ISS
            assertFalse(pattern.matcher("Other provider").find());
        }
    }
    
    @Nested
    @DisplayName("ESG Provider Pattern Tests")
    class ESGProviderPatternTests {
        
        @Test
        @DisplayName("Should match Sustainalytics")
        void testSustainalyticsPattern() {
            Pattern pattern = PatternMatchers.SUSTAINALYTICS_PATTERN;
            
            assertTrue(pattern.matcher("Sustainalytics").find());
            assertTrue(pattern.matcher("sustainalytics").find()); // Case insensitive
            assertTrue(pattern.matcher("SUSTAINALYTICS").find()); // Case insensitive
            assertTrue(pattern.matcher("We use Sustainalytics for ESG research").find());
            
            assertFalse(pattern.matcher("Sustain").find());
            assertFalse(pattern.matcher("Analytics").find());
            assertFalse(pattern.matcher("Other ESG provider").find());
        }
        
        @Test
        @DisplayName("Should match MSCI ESG patterns")
        void testMSCIPattern() {
            Pattern pattern = PatternMatchers.MSCI_PATTERN;
            
            assertTrue(pattern.matcher("ESG research from MSCI").find());
            assertTrue(pattern.matcher("MSCI ESG ratings").find());
            assertTrue(pattern.matcher("esg analysis by msci").find()); // Case insensitive
            assertTrue(pattern.matcher("MSCI for ESG data").find());
            
            assertFalse(pattern.matcher("MSCI").find()); // Without ESG
            assertFalse(pattern.matcher("ESG research").find()); // Without MSCI
            assertFalse(pattern.matcher("Other provider").find());
        }
        
        @Test
        @DisplayName("Should match ESG investment language")
        void testESGPattern() {
            Pattern pattern = PatternMatchers.ESG_PATTERN;
            
            assertTrue(pattern.matcher("environmental, social, and governance").find());
            assertTrue(pattern.matcher("ESG factors").find());
            assertTrue(pattern.matcher("sustainable investing").find());
            assertTrue(pattern.matcher("ESG integration").find());
            assertTrue(pattern.matcher("integration of ESG").find());
            assertTrue(pattern.matcher("integrates ESG factors").find());
            assertTrue(pattern.matcher("ESG criteria integration").find());
            assertTrue(pattern.matcher("integrating ESG").find());
            assertTrue(pattern.matcher("integration of environmental factors").find());
            assertTrue(pattern.matcher("integrates environmental considerations").find());
            
            assertFalse(pattern.matcher("environmental").find()); // Just environmental
            assertFalse(pattern.matcher("social").find()); // Just social
            assertFalse(pattern.matcher("governance").find()); // Just governance
            assertFalse(pattern.matcher("Other investment approach").find());
        }
    }
    
    @Nested
    @DisplayName("Email Pattern Tests")
    class EmailPatternTests {
        
        @ParameterizedTest
        @ValueSource(strings = {
            "test@example.com",
            "user.name@domain.org",
            "first_last@company.net",
            "info123@test-domain.co.uk",
            "contact+support@firm.gov",
            "andré@tëst.cöm" // Unicode support
        })
        @DisplayName("Should match valid email addresses")
        void testValidEmailAddresses(String email) {
            Pattern pattern = PatternMatchers.EMAIL_PATTERN;
            Matcher matcher = pattern.matcher("Contact us at " + email + " for more info");
            
            assertTrue(matcher.find());
            assertEquals(email, matcher.group(1));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "invalid.email",
            "@domain.com",
            "user@",
            "user@domain",
            "user name@domain.com", // Space in local part
            "user@domain..com" // Double dot
        })
        @DisplayName("Should not match invalid email addresses")
        void testInvalidEmailAddresses(String invalidEmail) {
            Pattern pattern = PatternMatchers.EMAIL_PATTERN;
            Matcher matcher = pattern.matcher("Contact us at " + invalidEmail + " for more info");
            
            assertFalse(matcher.find());
        }
        
        @Test
        @DisplayName("Should match email sentences with compliance keywords")
        void testEmailComplianceSentencePattern() {
            Pattern pattern = PatternMatchers.EMAIL_COMPLIANCE_SENTENCE_PATTERN;
            
            assertTrue(pattern.matcher("For compliance matters, contact compliance@firm.com").find());
            assertTrue(pattern.matcher("Compliance questions should be sent to info@company.org").find());
            assertTrue(pattern.matcher("Contact compliance@test.net for regulatory issues").find());
            
            assertFalse(pattern.matcher("General inquiries: info@firm.com").find()); // No compliance keyword
            assertFalse(pattern.matcher("Compliance department handles these matters").find()); // No email
        }
        
        @Test
        @DisplayName("Should match email sentences with proxy keywords")
        void testEmailProxySentencePattern() {
            Pattern pattern = PatternMatchers.EMAIL_PROXY_SENTENCE_PATTERN;
            
            assertTrue(pattern.matcher("Proxy voting questions: proxy@firm.com").find());
            assertTrue(pattern.matcher("For proxy matters, email voting@company.org").find());
            assertTrue(pattern.matcher("Contact proxy@test.net for voting information").find());
            
            assertFalse(pattern.matcher("General inquiries: info@firm.com").find()); // No proxy keyword
            assertFalse(pattern.matcher("Proxy voting is handled internally").find()); // No email
        }
        
        @Test
        @DisplayName("Should match email sentences with brochure keywords")
        void testEmailBrochureSentencePattern() {
            Pattern pattern = PatternMatchers.EMAIL_BROCHURE_SENTENCE_PATTERN;
            
            assertTrue(pattern.matcher("To request our brochure, email brochure@firm.com").find());
            assertTrue(pattern.matcher("Questions about our services: info@company.org").find());
            assertTrue(pattern.matcher("Contact brochure@test.net for more information").find());
            
            assertFalse(pattern.matcher("General inquiries: info@firm.com").find()); // No brochure/question keyword
            assertFalse(pattern.matcher("Our brochure is available online").find()); // No email
        }
    }
    
    @Nested
    @DisplayName("No Vote Pattern Tests")
    class NoVotePatternTests {
        
        @Test
        @DisplayName("Should match no vote language variations")
        void testNoVotePattern() {
            Pattern pattern = PatternMatchers.NO_VOTE_PATTERN;
            
            assertTrue(pattern.matcher("We may abstain from voting").find());
            assertTrue(pattern.matcher("We do not vote on certain proposals").find());
            assertTrue(pattern.matcher("We will not vote in some cases").find());
            assertTrue(pattern.matcher("We may not vote on proxy proposals").find());
            assertTrue(pattern.matcher("ABSTAIN FROM VOTING").find()); // Case insensitive
            assertTrue(pattern.matcher("DO NOT VOTE").find()); // Case insensitive
            
            assertFalse(pattern.matcher("We vote on all proposals").find());
            assertFalse(pattern.matcher("Voting is important").find());
            assertFalse(pattern.matcher("Other policy").find());
        }
    }
    
    @Nested
    @DisplayName("Custodial Services Pattern Tests")
    class CustodialServicesPatternTests {
        
        @Test
        @DisplayName("Should match custodial services patterns")
        void testCustodialServicesPattern() {
            Pattern pattern = PatternMatchers.CUSTODIAL_SERVICES_PATTERN;
            
            assertTrue(pattern.matcher("We provide custodial services to our clients").find());
            assertTrue(pattern.matcher("Our custodial services include safekeeping").find());
            assertTrue(pattern.matcher("CUSTODIAL SERVICES").find()); // Case insensitive
            
            // Pattern should match with context around "custodial services"
            String longText = "Our firm offers comprehensive investment management. " +
                             "We also provide custodial services for institutional clients. " +
                             "These services include asset safekeeping and reporting.";
            assertTrue(pattern.matcher(longText).find());
            
            assertFalse(pattern.matcher("custodial").find()); // Just custodial
            assertFalse(pattern.matcher("services").find()); // Just services
            assertFalse(pattern.matcher("Other services").find());
        }
    }
    
    @Nested
    @DisplayName("Version ID Pattern Tests")
    class VersionIDPatternTests {
        
        @Test
        @DisplayName("Should match API brochure version ID patterns")
        void testAPIBrochureVersionIDPattern() {
            Pattern pattern = PatternMatchers.API_BRCHR_VERSION_ID_PATTERN;
            
            Matcher matcher1 = pattern.matcher("brochureVersionID\": 12345,");
            assertTrue(matcher1.find());
            assertEquals("12345", matcher1.group(1));
            
            Matcher matcher2 = pattern.matcher("brochureVersionID\": 67890,");
            assertTrue(matcher2.find());
            assertEquals("67890", matcher2.group(1));
            
            assertFalse(pattern.matcher("brochureVersionID\": abc,").find()); // Non-numeric
            assertFalse(pattern.matcher("otherField\": 12345,").find()); // Wrong field name
        }
        
        @Test
        @DisplayName("Should match brochure version ID patterns")
        void testBrochureVersionIDPattern() {
            Pattern pattern = PatternMatchers.BRCHR_VERSION_ID_PATTERN;
            
            Matcher matcher1 = pattern.matcher("BRCHR_VRSN_ID=12345");
            assertTrue(matcher1.find());
            assertEquals("12345", matcher1.group(1));
            
            Matcher matcher2 = pattern.matcher("BRCHR_VRSN_ID=67890");
            assertTrue(matcher2.find());
            assertEquals("67890", matcher2.group(1));
            
            assertFalse(pattern.matcher("BRCHR_VRSN_ID=abc").find()); // Non-numeric
            assertFalse(pattern.matcher("OTHER_ID=12345").find()); // Wrong field name
        }
    }
    
    @Nested
    @DisplayName("Pattern Compilation Tests")
    class PatternCompilationTests {
        
        @Test
        @DisplayName("Should have all patterns properly compiled")
        void testAllPatternsCompiled() {
            // Test that all pattern constants are properly compiled and not null
            assertNotNull(PatternMatchers.GLASS_LEWIS_PATTERN);
            assertNotNull(PatternMatchers.BROADRIDGE_PATTERN);
            assertNotNull(PatternMatchers.SUSTAINALYTICS_PATTERN);
            assertNotNull(PatternMatchers.MSCI_PATTERN);
            assertNotNull(PatternMatchers.ESG_PATTERN);
            assertNotNull(PatternMatchers.PROXYEDGE_PATTERN);
            assertNotNull(PatternMatchers.EGAN_JONES_PATTERN);
            assertNotNull(PatternMatchers.ISS_PROXY_PATTERN);
            assertNotNull(PatternMatchers.FRT_PATTERN);
            assertNotNull(PatternMatchers.BATTEA_CLASS_ACTION_PATTERN);
            assertNotNull(PatternMatchers.CCC_CLASS_ACTION_PATTERN);
            assertNotNull(PatternMatchers.ROBBINS_GELLER_CLASS_ACTION_PATTERN);
            assertNotNull(PatternMatchers.THIRD_PARTY_PROXY_PATTERN);
            assertNotNull(PatternMatchers.ISS_CLASS_ACTION_PATTERN);
            assertNotNull(PatternMatchers.EMAIL_SENTENCE_PATTERN);
            assertNotNull(PatternMatchers.EMAIL_COMPLIANCE_SENTENCE_PATTERN);
            assertNotNull(PatternMatchers.EMAIL_PROXY_SENTENCE_PATTERN);
            assertNotNull(PatternMatchers.EMAIL_BROCHURE_SENTENCE_PATTERN);
            assertNotNull(PatternMatchers.EMAIL_PATTERN);
            assertNotNull(PatternMatchers.NO_VOTE_PATTERN);
            assertNotNull(PatternMatchers.CUSTODIAL_SERVICES_PATTERN);
            assertNotNull(PatternMatchers.API_BRCHR_VERSION_ID_PATTERN);
            assertNotNull(PatternMatchers.BRCHR_VERSION_ID_PATTERN);
        }
        
        @Test
        @DisplayName("Should have correct case sensitivity flags")
        void testCaseSensitivityFlags() {
            // Test patterns that should be case insensitive
            assertTrue(PatternMatchers.GLASS_LEWIS_PATTERN.matcher("glass lewis").find());
            assertTrue(PatternMatchers.BROADRIDGE_PATTERN.matcher("broadridge").find());
            assertTrue(PatternMatchers.SUSTAINALYTICS_PATTERN.matcher("sustainalytics").find());
            assertTrue(PatternMatchers.ESG_PATTERN.matcher("esg factors").find());
            assertTrue(PatternMatchers.NO_VOTE_PATTERN.matcher("abstain").find());
            
            // Test patterns that should be case sensitive (CCC)
            assertTrue(PatternMatchers.CCC_CLASS_ACTION_PATTERN.matcher("CCC class action").find());
            assertFalse(PatternMatchers.CCC_CLASS_ACTION_PATTERN.matcher("ccc class action").find());
        }
    }
    
    @Nested
    @DisplayName("Utility Class Tests")
    class UtilityClassTests {
        
        @Test
        @DisplayName("Should not be instantiable")
        void testNotInstantiable() {
            // PatternMatchers should be a utility class with private constructor
            assertThrows(UnsupportedOperationException.class, () -> {
                // Use reflection to try to instantiate
                java.lang.reflect.Constructor<PatternMatchers> constructor = 
                        PatternMatchers.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            });
        }
    }
}
