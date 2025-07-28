/**
 * Example class demonstrating how to use the refactored IAPD services
 */
public class ExampleUsage {
    
    public static void main(String[] args) {
        demonstrateRefactoredArchitecture();
        demonstrateIndividualServices();
    }
    
    /**
     * Demonstrates using the main refactored parser
     */
    private static void demonstrateRefactoredArchitecture() {
        System.out.println("=== Using Refactored Architecture ===");
        
        // Create the main parser with all services properly injected
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        try {
            // Example 1: Process XML file only
            System.out.println("Processing XML file...");
            parser.processXMLFile();
            
            // Example 2: Process brochures only
            System.out.println("Processing brochures...");
            parser.processBrochures();
            
            // Example 3: Full workflow - download and process everything
            System.out.println("Full workflow...");
            parser.downloadAndProcessLatestIAPDData();
            
        } catch (Exception e) {
            System.err.println("Error in refactored architecture demo: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates using individual services directly
     */
    private static void demonstrateIndividualServices() {
        System.out.println("\n=== Using Individual Services ===");
        
        try {
            // Create services with dependency injection
            FileDownloadService downloadService = new FileDownloadService();
            XMLProcessingService xmlService = new XMLProcessingService(downloadService);
            
            BrochureAnalyzer analyzer = new BrochureAnalyzer();
            CSVWriterService csvWriter = new CSVWriterService();
            BrochureProcessingService brochureService = new BrochureProcessingService(analyzer, csvWriter);
            
            // Example: Process a specific XML file
            System.out.println("Processing specific XML file with XMLProcessingService...");
            String outputPath = xmlService.processDefaultXMLFile();
            System.out.println("XML processing completed. Output: " + outputPath);
            
            // Example: Process brochures with specific input
            if (outputPath != null) {
                System.out.println("Processing brochures with BrochureProcessingService...");
                brochureService.processBrochures(outputPath);
                System.out.println("Brochure processing completed.");
            }
            
        } catch (XMLProcessingException e) {
            System.err.println("XML Processing Error: " + e.getMessage());
        } catch (BrochureProcessingException e) {
            System.err.println("Brochure Processing Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General Error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates creating FirmData using the Builder pattern
     */
    private static void demonstrateBuilderPattern() {
        System.out.println("\n=== Builder Pattern Example ===");
        
        // Create FirmData using the builder pattern
        FirmData firm = new FirmDataBuilder()
            .setFirmCrdNb("12345")
            .setBusNm("Example Investment Advisers LLC")
            .setLegalNm("Example Investment Advisers, LLC")
            .setStreet1("123 Wall Street")
            .setCity("New York")
            .setState("NY")
            .setCountry("USA")
            .setPostalCode("10005")
            .setPhoneNumber("212-555-0123")
            .setAUM("1000000000")
            .setTotalEmployees("50")
            .build();
        
        System.out.println("Created firm: " + firm.getBusNm());
        System.out.println("CRD Number: " + firm.getFirmCrdNb());
        System.out.println("Location: " + firm.getCity() + ", " + firm.getState());
        System.out.println("AUM: $" + firm.getAUM());
    }
    
    /**
     * Demonstrates the improved BrochureAnalyzer with Strategy pattern
     */
    private static void demonstrateBrochureAnalysis() {
        System.out.println("\n=== Brochure Analysis Example ===");
        
        // Sample brochure text for analysis
        String sampleText = "We use Glass Lewis for proxy voting recommendations. " +
                           "Our ESG integration approach considers environmental factors. " +
                           "For compliance questions, contact compliance@example.com. " +
                           "We utilize ISS for class action monitoring services.";
        
        BrochureAnalyzer analyzer = new BrochureAnalyzer();
        BrochureAnalysis analysis = analyzer.analyzeBrochureContent(sampleText);
        
        System.out.println("Analysis Results:");
        System.out.println("- Proxy Providers: " + analysis.getProxyProvider().toString());
        System.out.println("- Class Action Providers: " + analysis.getClassActionProvider().toString());
        System.out.println("- ESG Providers: " + analysis.getEsgProvider().toString());
        System.out.println("- ESG Investment Language: " + analysis.getEsgInvestmentLanguage().toString());
        System.out.println("- Email Addresses Found: " + analysis.getEmailSet().size());
        System.out.println("- Formatted Email List: " + analysis.getFormattedEmailSetString());
    }
}
