import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test suite for BrochureAnalyzer
 * Tests pattern matching, content analysis, and edge cases
 */
public class BrochureAnalyzerTest {
    
    private BrochureAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new BrochureAnalyzer();
    }
    
    @Nested
    @DisplayName("Proxy Provider Analysis Tests")
    class ProxyProviderAnalysisTests {
        
        @Test
        @DisplayName("Should detect ISS proxy provider")
        void testDetectISSProxyProvider() {
            String content = "We use Institutional Shareholder Services (ISS) for proxy voting services.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_001");
            
            assertTrue(result.getProxyProvider().toString().contains("ISS"));
        }
        
        @Test
        @DisplayName("Should detect Glass Lewis proxy provider")
        void testDetectGlassLewisProxyProvider() {
            String content = "Our proxy voting is handled by Glass Lewis & Co.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_002");
            
            assertTrue(result.getProxyProvider().toString().contains("Glass Lewis"));
        }
        
        @Test
        @DisplayName("Should detect multiple proxy providers")
        void testDetectMultipleProxyProviders() {
            String content = "We utilize both ISS and Glass Lewis for comprehensive proxy research and voting recommendations.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_003");
            
            String providers = result.getProxyProvider().toString();
            assertTrue(providers.contains("ISS"));
            assertTrue(providers.contains("Glass Lewis"));
        }
        
        @Test
        @DisplayName("Should handle case insensitive proxy provider detection")
        void testCaseInsensitiveProxyProviderDetection() {
            String content = "We work with iss for proxy voting services.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_004");
            
            assertTrue(result.getProxyProvider().toString().contains("ISS"));
        }
        
        @Test
        @DisplayName("Should not detect proxy providers when none mentioned")
        void testNoProxyProviderDetection() {
            String content = "We handle all investment decisions internally without external assistance.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_005");
            
            assertTrue(result.getProxyProvider().toString().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Class Action Provider Analysis Tests")
    class ClassActionProviderAnalysisTests {
        
        @Test
        @DisplayName("Should detect Robbins Geller class action provider")
        void testDetectRobbinsGellerClassActionProvider() {
            String content = "Class action settlements are monitored through Robbins Geller Rudman & Dowd LLP.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_006");
            
            assertTrue(result.getClassActionProvider().toString().contains("Robbins Geller"));
        }
        
        @Test
        @DisplayName("Should detect multiple class action providers")
        void testDetectMultipleClassActionProviders() {
            String content = "We work with both Robbins Geller and other law firms for class action monitoring.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_007");
            
            assertTrue(result.getClassActionProvider().toString().contains("Robbins Geller"));
        }
        
        @Test
        @DisplayName("Should not detect class action providers when none mentioned")
        void testNoClassActionProviderDetection() {
            String content = "We do not participate in class action settlements.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_008");
            
            assertTrue(result.getClassActionProvider().toString().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("ESG Provider Analysis Tests")
    class ESGProviderAnalysisTests {
        
        @Test
        @DisplayName("Should detect MSCI ESG provider")
        void testDetectMSCIESGProvider() {
            String content = "Our ESG analysis is supported by MSCI ESG Research.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_009");
            
            assertTrue(result.getEsgProvider().toString().contains("MSCI"));
        }
        
        @Test
        @DisplayName("Should detect Sustainalytics ESG provider")
        void testDetectSustainalyticsESGProvider() {
            String content = "We utilize Sustainalytics for ESG ratings and research.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_010");
            
            assertTrue(result.getEsgProvider().toString().contains("Sustainalytics"));
        }
        
        @Test
        @DisplayName("Should detect ESG investment language")
        void testDetectESGInvestmentLanguage() {
            String content = "We integrate environmental, social, and governance factors into our investment process.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_011");
            
            assertFalse(result.getEsgInvestmentLanguage().toString().isEmpty());
        }
        
        @Test
        @DisplayName("Should detect sustainable investing language")
        void testDetectSustainableInvestingLanguage() {
            String content = "Our sustainable investing approach considers ESG criteria.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_012");
            
            assertFalse(result.getEsgInvestmentLanguage().toString().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Email Analysis Tests")
    class EmailAnalysisTests {
        
        @Test
        @DisplayName("Should extract email addresses")
        void testExtractEmailAddresses() {
            String content = "Contact us at info@example.com or support@testfirm.org for more information.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_013");
            
            assertTrue(result.getEmailSet().contains("info@example.com"));
            assertTrue(result.getEmailSet().contains("support@testfirm.org"));
        }
        
        @Test
        @DisplayName("Should extract compliance email sentences")
        void testExtractComplianceEmailSentences() {
            String content = "For compliance matters, please contact compliance@firm.com immediately.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_014");
            
            assertFalse(result.getEmailComplianceSentence().toString().isEmpty());
        }
        
        @Test
        @DisplayName("Should extract proxy email sentences")
        void testExtractProxyEmailSentences() {
            String content = "Proxy voting questions should be directed to proxy@firm.com.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_015");
            
            assertFalse(result.getEmailProxySentence().toString().isEmpty());
        }
        
        @Test
        @DisplayName("Should extract brochure email sentences")
        void testExtractBrochureEmailSentences() {
            String content = "To request our brochure, email brochure@firm.com.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_016");
            
            assertFalse(result.getEmailBrochureSentence().toString().isEmpty());
        }
        
        @Test
        @DisplayName("Should handle multiple email formats")
        void testMultipleEmailFormats() {
            String content = "Contact: john.doe@firm.com, jane_smith@company.org, or info123@test-firm.net";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_017");
            
            assertTrue(result.getEmailSet().contains("john.doe@firm.com"));
            assertTrue(result.getEmailSet().contains("jane_smith@company.org"));
            assertTrue(result.getEmailSet().contains("info123@test-firm.net"));
        }
    }
    
    @Nested
    @DisplayName("No Vote Analysis Tests")
    class NoVoteAnalysisTests {
        
        @Test
        @DisplayName("Should detect no vote language")
        void testDetectNoVoteLanguage() {
            String content = "We may abstain from voting on certain proposals.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_018");
            
            assertFalse(result.getNoVoteString().toString().isEmpty());
        }
        
        @Test
        @DisplayName("Should detect abstain language")
        void testDetectAbstainLanguage() {
            String content = "In some cases, we will not vote on proxy proposals.";
            BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_019");
            
            assertFalse(result.getNoVoteString().toString().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle null input gracefully")
        void testHandleNullInput() {
            assertDoesNotThrow(() -> {
                BrochureAnalysis result = analyzer.analyzeBrochureContent(null, "TEST_FIRM_020");
                assertNotNull(result);
            });
        }
        
        @Test
        @DisplayName("Should handle empty string input")
        void testHandleEmptyStringInput() {
            BrochureAnalysis result = analyzer.analyzeBrochureContent("", "TEST_FIRM_021");
            
            assertNotNull(result);
            assertTrue(result.getProxyProvider().toString().isEmpty());
            assertTrue(result.getClassActionProvider().toString().isEmpty());
            assertTrue(result.getEsgProvider().toString().isEmpty());
            assertTrue(result.getEmailSet().isEmpty());
        }
        
        @Test
        @DisplayName("Should handle very long content")
        void testHandleVeryLongContent() {
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longContent.append("This is a very long document with lots of text. ");
            }
            longContent.append("We use ISS for proxy voting. Contact us at info@firm.com.");
            
            assertDoesNotThrow(() -> {
                BrochureAnalysis result = analyzer.analyzeBrochureContent(longContent.toString(), "TEST_FIRM_022");
                assertNotNull(result);
                assertTrue(result.getProxyProvider().toString().contains("ISS"));
                assertTrue(result.getEmailSet().contains("info@firm.com"));
            });
        }
        
        @Test
        @DisplayName("Should handle special characters and unicode")
        void testHandleSpecialCharacters() {
            String content = "Côntact ús at tëst@fïrm.cöm för ESG ïnförmätïön. We use ISS® for proxy voting.";
            
            assertDoesNotThrow(() -> {
                BrochureAnalysis result = analyzer.analyzeBrochureContent(content, "TEST_FIRM_023");
                assertNotNull(result);
            });
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "   ", // whitespace only
            "\n\n\n", // newlines only
            "\t\t\t", // tabs only
            "123456789", // numbers only
            "!@#$%^&*()", // special characters only
        })
        @DisplayName("Should handle various edge case inputs")
        void testVariousEdgeCaseInputs(String input) {
            assertDoesNotThrow(() -> {
                BrochureAnalysis result = analyzer.analyzeBrochureContent(input, "TEST_FIRM_024");
                assertNotNull(result);
            });
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should analyze comprehensive brochure content")
        void testComprehensiveBrochureAnalysis() {
            String comprehensiveContent = "Investment Advisory Services\n\n" +
                "We provide comprehensive investment management services to institutional and individual clients.\n" +
                "Our investment process integrates environmental, social, and governance (ESG) factors.\n\n" +
                "Proxy Voting\n" +
                "We utilize Institutional Shareholder Services (ISS) for proxy research and voting recommendations.\n" +
                "Glass Lewis also provides additional research when needed.\n\n" +
                "Class Action Monitoring\n" +
                "Class action settlements are monitored through Robbins Geller Rudman & Dowd LLP.\n\n" +
                "ESG Research\n" +
                "Our ESG analysis is supported by MSCI ESG Research and Sustainalytics ratings.\n" +
                "We believe sustainable investing is crucial for long-term returns.\n\n" +
                "Contact Information\n" +
                "General inquiries: info@firm.com\n" +
                "Compliance matters: compliance@firm.com\n" +
                "Proxy voting questions: proxy@firm.com\n" +
                "Brochure requests: brochure@firm.com\n\n" +
                "In certain circumstances, we may abstain from voting on proxy proposals.";
            
            BrochureAnalysis result = analyzer.analyzeBrochureContent(comprehensiveContent, "TEST_FIRM_025");
            
            // Verify proxy providers
            String proxyProviders = result.getProxyProvider().toString();
            assertTrue(proxyProviders.contains("ISS"));
            assertTrue(proxyProviders.contains("Glass Lewis"));
            
            // Verify class action providers
            assertTrue(result.getClassActionProvider().toString().contains("Robbins Geller"));
            
            // Verify ESG providers
            String esgProviders = result.getEsgProvider().toString();
            assertTrue(esgProviders.contains("MSCI"));
            assertTrue(esgProviders.contains("Sustainalytics"));
            
            // Verify ESG investment language
            assertFalse(result.getEsgInvestmentLanguage().toString().isEmpty());
            
            // Verify email extraction
            assertTrue(result.getEmailSet().contains("info@firm.com"));
            assertTrue(result.getEmailSet().contains("compliance@firm.com"));
            assertTrue(result.getEmailSet().contains("proxy@firm.com"));
            assertTrue(result.getEmailSet().contains("brochure@firm.com"));
            
            // Verify email sentences
            assertFalse(result.getEmailComplianceSentence().toString().isEmpty());
            assertFalse(result.getEmailProxySentence().toString().isEmpty());
            assertFalse(result.getEmailBrochureSentence().toString().isEmpty());
            
            // Verify no vote language
            assertFalse(result.getNoVoteString().toString().isEmpty());
        }
    }
}
