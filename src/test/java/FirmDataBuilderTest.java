import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test suite for FirmDataBuilder
 * Tests builder pattern implementation, data validation, and edge cases
 */
public class FirmDataBuilderTest {
    
    private FirmDataBuilder builder;
    
    @BeforeEach
    void setUp() {
        builder = new FirmDataBuilder();
    }
    
    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {
        
        @Test
        @DisplayName("Should build FirmData with all fields set")
        void testBuildWithAllFields() {
            FirmData firmData = builder
                .setSECRgnCD("SEC123")
                .setFirmCrdNb("12345")
                .setSECNb("SEC456")
                .setBusNm("Test Investment Advisors")
                .setLegalNm("Test Investment Advisors LLC")
                .setStreet1("123 Main Street")
                .setStreet2("Suite 100")
                .setCity("New York")
                .setState("NY")
                .setCountry("USA")
                .setPostalCode("10001")
                .setPhoneNumber("555-123-4567")
                .setFaxNumber("555-123-4568")
                .setFirmType("Investment Adviser")
                .setRegistrationState("NY")
                .setRegistrationDate("2020-01-01")
                .setFilingDate("2023-12-31")
                .setFormVersion("2.0")
                .setTotalEmployees("50")
                .setAUM("1000000000")
                .setTotalAccounts("500")
                .setBrochureURL("https://example.com/brochure.pdf")
                .build();
            
            assertNotNull(firmData);
            assertEquals("SEC123", firmData.getSECRgnCD());
            assertEquals("12345", firmData.getFirmCrdNb());
            assertEquals("SEC456", firmData.getSECNb());
            assertEquals("Test Investment Advisors", firmData.getBusNm());
            assertEquals("Test Investment Advisors LLC", firmData.getLegalNm());
            assertEquals("123 Main Street", firmData.getStreet1());
            assertEquals("Suite 100", firmData.getStreet2());
            assertEquals("New York", firmData.getCity());
            assertEquals("NY", firmData.getState());
            assertEquals("USA", firmData.getCountry());
            assertEquals("10001", firmData.getPostalCode());
            assertEquals("555-123-4567", firmData.getPhoneNumber());
            assertEquals("555-123-4568", firmData.getFaxNumber());
            assertEquals("Investment Adviser", firmData.getFirmType());
            assertEquals("NY", firmData.getRegistrationState());
            assertEquals("2020-01-01", firmData.getRegistrationDate());
            assertEquals("2023-12-31", firmData.getFilingDate());
            assertEquals("2.0", firmData.getFormVersion());
            assertEquals("50", firmData.getTotalEmployees());
            assertEquals("1000000000", firmData.getAUM());
            assertEquals("500", firmData.getTotalAccounts());
        }
        
        @Test
        @DisplayName("Should build FirmData with minimal required fields")
        void testBuildWithMinimalFields() {
            FirmData firmData = builder
                .setFirmCrdNb("12345")
                .setBusNm("Minimal Firm")
                .build();
            
            assertNotNull(firmData);
            assertEquals("12345", firmData.getFirmCrdNb());
            assertEquals("Minimal Firm", firmData.getBusNm());
        }
        
        @Test
        @DisplayName("Should allow method chaining")
        void testMethodChaining() {
            assertDoesNotThrow(() -> {
                FirmData firmData = builder
                    .setFirmCrdNb("12345")
                    .setBusNm("Chain Test Firm")
                    .setCity("Boston")
                    .setState("MA")
                    .build();
                
                assertNotNull(firmData);
                assertEquals("12345", firmData.getFirmCrdNb());
                assertEquals("Chain Test Firm", firmData.getBusNm());
                assertEquals("Boston", firmData.getCity());
                assertEquals("MA", firmData.getState());
            });
        }
        
        @Test
        @DisplayName("Should return same builder instance for chaining")
        void testBuilderInstanceChaining() {
            FirmDataBuilder result1 = builder.setFirmCrdNb("12345");
            FirmDataBuilder result2 = builder.setBusNm("Test Firm");
            
            assertSame(builder, result1);
            assertSame(builder, result2);
        }
    }
    
    @Nested
    @DisplayName("Individual Field Tests")
    class IndividualFieldTests {
        
        @Test
        @DisplayName("Should set and get SEC Registration Code")
        void testSECRgnCD() {
            builder.setSECRgnCD("SEC789");
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            assertEquals("SEC789", firmData.getSECRgnCD());
        }
        
        @Test
        @DisplayName("Should set and get Firm CRD Number")
        void testFirmCrdNb() {
            builder.setFirmCrdNb("98765");
            assertEquals("98765", builder.getFirmCrdNb());
            
            FirmData firmData = builder.setBusNm("Test").build();
            assertEquals("98765", firmData.getFirmCrdNb());
        }
        
        @Test
        @DisplayName("Should set and get SEC Number")
        void testSECNb() {
            builder.setSECNb("SEC999");
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            assertEquals("SEC999", firmData.getSECNb());
        }
        
        @Test
        @DisplayName("Should set and get Business Name")
        void testBusNm() {
            builder.setBusNm("Business Name Test");
            FirmData firmData = builder.setFirmCrdNb("12345").build();
            assertEquals("Business Name Test", firmData.getBusNm());
        }
        
        @Test
        @DisplayName("Should set and get Legal Name")
        void testLegalNm() {
            builder.setLegalNm("Legal Name Test LLC");
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            assertEquals("Legal Name Test LLC", firmData.getLegalNm());
        }
        
        @Test
        @DisplayName("Should set and get address fields")
        void testAddressFields() {
            builder.setStreet1("456 Oak Avenue")
                   .setStreet2("Floor 5")
                   .setCity("Chicago")
                   .setState("IL")
                   .setCountry("United States")
                   .setPostalCode("60601");
            
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            
            assertEquals("456 Oak Avenue", firmData.getStreet1());
            assertEquals("Floor 5", firmData.getStreet2());
            assertEquals("Chicago", firmData.getCity());
            assertEquals("IL", firmData.getState());
            assertEquals("United States", firmData.getCountry());
            assertEquals("60601", firmData.getPostalCode());
        }
        
        @Test
        @DisplayName("Should set and get contact information")
        void testContactInformation() {
            builder.setPhoneNumber("312-555-0123")
                   .setFaxNumber("312-555-0124");
            
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            
            assertEquals("312-555-0123", firmData.getPhoneNumber());
            assertEquals("312-555-0124", firmData.getFaxNumber());
        }
        
        @Test
        @DisplayName("Should set and get firm details")
        void testFirmDetails() {
            builder.setFirmType("Registered Investment Adviser")
                   .setRegistrationState("CA")
                   .setRegistrationDate("2019-06-15")
                   .setFilingDate("2023-03-31")
                   .setFormVersion("1.5");
            
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            
            assertEquals("Registered Investment Adviser", firmData.getFirmType());
            assertEquals("CA", firmData.getRegistrationState());
            assertEquals("2019-06-15", firmData.getRegistrationDate());
            assertEquals("2023-03-31", firmData.getFilingDate());
            assertEquals("1.5", firmData.getFormVersion());
        }
        
        @Test
        @DisplayName("Should set and get business metrics")
        void testBusinessMetrics() {
            builder.setTotalEmployees("75")
                   .setAUM("2500000000")
                   .setTotalAccounts("1200");
            
            FirmData firmData = builder.setFirmCrdNb("12345").setBusNm("Test").build();
            
            assertEquals("75", firmData.getTotalEmployees());
            assertEquals("2500000000", firmData.getAUM());
            assertEquals("1200", firmData.getTotalAccounts());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Null Handling Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle null values gracefully")
        void testNullValues() {
            assertDoesNotThrow(() -> {
                FirmData firmData = builder
                    .setSECRgnCD(null)
                    .setFirmCrdNb("12345")
                    .setSECNb(null)
                    .setBusNm("Test Firm")
                    .setLegalNm(null)
                    .setStreet1(null)
                    .setStreet2(null)
                    .setCity(null)
                    .setState(null)
                    .setCountry(null)
                    .setPostalCode(null)
                    .setPhoneNumber(null)
                    .setFaxNumber(null)
                    .setFirmType(null)
                    .setRegistrationState(null)
                    .setRegistrationDate(null)
                    .setFilingDate(null)
                    .setFormVersion(null)
                    .setTotalEmployees(null)
                    .setAUM(null)
                    .setTotalAccounts(null)
                    .setBrochureURL(null)
                    .build();
                
                assertNotNull(firmData);
                assertNull(firmData.getSECRgnCD());
                assertEquals("12345", firmData.getFirmCrdNb());
                assertNull(firmData.getSECNb());
                assertEquals("Test Firm", firmData.getBusNm());
                assertNull(firmData.getLegalNm());
            });
        }
        
        @Test
        @DisplayName("Should handle empty strings")
        void testEmptyStrings() {
            FirmData firmData = builder
                .setSECRgnCD("")
                .setFirmCrdNb("12345")
                .setBusNm("")
                .setCity("")
                .setState("")
                .build();
            
            assertNotNull(firmData);
            assertEquals("", firmData.getSECRgnCD());
            assertEquals("12345", firmData.getFirmCrdNb());
            assertEquals("", firmData.getBusNm());
            assertEquals("", firmData.getCity());
            assertEquals("", firmData.getState());
        }
        
        @Test
        @DisplayName("Should handle whitespace-only strings")
        void testWhitespaceStrings() {
            FirmData firmData = builder
                .setFirmCrdNb("12345")
                .setBusNm("   ")
                .setCity("\t\t")
                .setState("\n\n")
                .build();
            
            assertNotNull(firmData);
            assertEquals("   ", firmData.getBusNm());
            assertEquals("\t\t", firmData.getCity());
            assertEquals("\n\n", firmData.getState());
        }
        
        @Test
        @DisplayName("Should handle very long strings")
        void testVeryLongStrings() {
            StringBuilder longString = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longString.append("Very long string content ");
            }
            String longValue = longString.toString();
            
            FirmData firmData = builder
                .setFirmCrdNb("12345")
                .setBusNm(longValue)
                .setLegalNm(longValue)
                .build();
            
            assertNotNull(firmData);
            assertEquals(longValue, firmData.getBusNm());
            assertEquals(longValue, firmData.getLegalNm());
        }
        
        @Test
        @DisplayName("Should handle special characters")
        void testSpecialCharacters() {
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            
            FirmData firmData = builder
                .setFirmCrdNb("12345")
                .setBusNm("Firm " + specialChars)
                .setCity("City " + specialChars)
                .build();
            
            assertNotNull(firmData);
            assertEquals("Firm " + specialChars, firmData.getBusNm());
            assertEquals("City " + specialChars, firmData.getCity());
        }
        
        @Test
        @DisplayName("Should handle unicode characters")
        void testUnicodeCharacters() {
            String unicode = "Fïrm Nämé with Ünïcödé 中文 العربية";
            
            FirmData firmData = builder
                .setFirmCrdNb("12345")
                .setBusNm(unicode)
                .setLegalNm(unicode)
                .build();
            
            assertNotNull(firmData);
            assertEquals(unicode, firmData.getBusNm());
            assertEquals(unicode, firmData.getLegalNm());
        }
    }
    
    @Nested
    @DisplayName("Builder Reuse Tests")
    class BuilderReuseTests {
        
        @Test
        @DisplayName("Should allow building multiple FirmData objects")
        void testMultipleBuildCalls() {
            // Build first FirmData
            FirmData firmData1 = builder
                .setFirmCrdNb("11111")
                .setBusNm("First Firm")
                .setCity("Boston")
                .build();
            
            // Build second FirmData with same builder
            FirmData firmData2 = builder
                .setFirmCrdNb("22222")
                .setBusNm("Second Firm")
                .setCity("Seattle")
                .build();
            
            assertNotNull(firmData1);
            assertNotNull(firmData2);
            assertNotSame(firmData1, firmData2);
            
            assertEquals("11111", firmData1.getFirmCrdNb());
            assertEquals("First Firm", firmData1.getBusNm());
            assertEquals("Boston", firmData1.getCity());
            
            assertEquals("22222", firmData2.getFirmCrdNb());
            assertEquals("Second Firm", firmData2.getBusNm());
            assertEquals("Seattle", firmData2.getCity());
        }
        
        @Test
        @DisplayName("Should maintain state between builds")
        void testStateMaintenanceBetweenBuilds() {
            // Set some common fields
            builder.setCountry("USA")
                   .setFirmType("Investment Adviser");
            
            // Build first FirmData
            FirmData firmData1 = builder
                .setFirmCrdNb("11111")
                .setBusNm("First Firm")
                .build();
            
            // Build second FirmData (should retain country and firm type)
            FirmData firmData2 = builder
                .setFirmCrdNb("22222")
                .setBusNm("Second Firm")
                .build();
            
            assertEquals("USA", firmData1.getCountry());
            assertEquals("Investment Adviser", firmData1.getFirmType());
            assertEquals("USA", firmData2.getCountry());
            assertEquals("Investment Adviser", firmData2.getFirmType());
        }
    }
    
    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {
        
        @Test
        @DisplayName("Should preserve data integrity after build")
        void testDataIntegrityAfterBuild() {
            String originalCrdNb = "12345";
            String originalBusNm = "Original Business Name";
            
            FirmData firmData = builder
                .setFirmCrdNb(originalCrdNb)
                .setBusNm(originalBusNm)
                .build();
            
            // Verify data is preserved
            assertEquals(originalCrdNb, firmData.getFirmCrdNb());
            assertEquals(originalBusNm, firmData.getBusNm());
            
            // Modify builder after build
            builder.setFirmCrdNb("99999")
                   .setBusNm("Modified Name");
            
            // Original FirmData should be unchanged
            assertEquals(originalCrdNb, firmData.getFirmCrdNb());
            assertEquals(originalBusNm, firmData.getBusNm());
        }
        
        @Test
        @DisplayName("Should handle numeric string values correctly")
        void testNumericStringValues() {
            FirmData firmData = builder
                .setFirmCrdNb("123456")
                .setBusNm("Test Firm")
                .setTotalEmployees("0")
                .setAUM("0")
                .setTotalAccounts("0")
                .setPostalCode("00000")
                .build();
            
            assertEquals("123456", firmData.getFirmCrdNb());
            assertEquals("0", firmData.getTotalEmployees());
            assertEquals("0", firmData.getAUM());
            assertEquals("0", firmData.getTotalAccounts());
            assertEquals("00000", firmData.getPostalCode());
        }
    }
}
