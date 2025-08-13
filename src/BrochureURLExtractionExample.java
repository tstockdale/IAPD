import java.io.File;
import java.nio.file.Path;

/**
 * Example demonstrating how to use the BrochureURLExtractionService
 * to extract brochure information from FIRM_API responses and create FilesToDownload output
 */
public class BrochureURLExtractionExample {
    
    public static void main(String[] args) {
        try {
            // Initialize ProcessingLogger
            ProcessingLogger.initialize();
            
            // Create processing context
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(10) // Process only 10 firms for demo
                    .xmlRatePerSecond(2) // 2 requests per second to be respectful to API
                    .verbose(true)
                    .configSource("example")
                    .build();
            
            // Create the service
            BrochureURLExtractionService service = new BrochureURLExtractionService();
            
            // Example input file path (this would be the output from XMLProcessingService)
            String inputFilePath = "./Data/Input/IA_FIRM_SEC_DATA_20250407.csv";
            File inputFile = new File(inputFilePath);
            
            if (!inputFile.exists()) {
                System.out.println("Input file not found: " + inputFilePath);
                System.out.println("This example expects a CSV file with firm data from the XML processing step.");
                System.out.println("The file should contain columns: FirmCrdNb, Business Name, etc.");
                return;
            }
            
            System.out.println("=== Brochure URL Extraction Example ===");
            System.out.println("Input file: " + inputFilePath);
            System.out.println("Processing context: " + context);
            System.out.println();
            
            // Process the file to extract brochure information
            Path outputFile = service.processFirmDataForBrochures(inputFile, context);
            
            System.out.println();
            System.out.println("=== Processing Complete ===");
            System.out.println("Output file created: " + outputFile);
            System.out.println("Processed firms: " + context.getProcessedFirms());
            System.out.println("Elapsed time: " + (context.getElapsedTimeMs() / 1000.0) + " seconds");
            
            if (context.getLastError() != null) {
                System.out.println("Last error: " + context.getLastError());
            }
            
        } catch (Exception e) {
            System.err.println("Error running brochure URL extraction example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates the expected JSON structure that the service can parse
     */
    public static void printExpectedJsonStructure() {
        System.out.println("=== Expected FIRM_API JSON Response Structure ===");
        System.out.println("{");
        System.out.println("  \"hits\": {");
        System.out.println("    \"total\": 1,");
        System.out.println("    \"hits\": [{");
        System.out.println("      \"_type\": \"_doc\",");
        System.out.println("      \"_source\": {");
        System.out.println("        \"iacontent\": \"{");
        System.out.println("          \\\"basicInformation\\\": {");
        System.out.println("            \\\"firmId\\\": 7059,");
        System.out.println("            \\\"firmName\\\": \\\"CITIGROUP GLOBAL MARKETS INC.\\\",");
        System.out.println("            ...");
        System.out.println("          },");
        System.out.println("          \\\"brochures\\\": {");
        System.out.println("            \\\"brochuredetails\\\": [{");
        System.out.println("              \\\"brochureVersionID\\\": 985284,");
        System.out.println("              \\\"brochureName\\\": \\\"CITIGROUP GLOBAL MARKETS INC. INVESTMENT ADVISORY PROGRAMS\\\",");
        System.out.println("              \\\"dateSubmitted\\\": \\\"7/20/2025\\\",");
        System.out.println("              \\\"lastConfirmed\\\": \\\"3/29/2021\\\"");
        System.out.println("            }]");
        System.out.println("          }");
        System.out.println("        }\"");
        System.out.println("      }");
        System.out.println("    }]");
        System.out.println("  }");
        System.out.println("}");
        System.out.println();
        System.out.println("=== Expected FilesToDownload Output ===");
        System.out.println("firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed");
        System.out.println("7059,CITIGROUP GLOBAL MARKETS INC.,985284,CITIGROUP GLOBAL MARKETS INC. INVESTMENT ADVISORY PROGRAMS,7/20/2025,3/29/2021");
    }
}
