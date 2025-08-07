import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

/**
 * Service class responsible for XML processing operations
 */
public class XMLProcessingService {
    
    private final FileDownloadService downloadService;
    
    public XMLProcessingService(FileDownloadService downloadService) {
        this.downloadService = downloadService;
    }
    
    /**
     * Processes the XML file containing firm data
     * @param xmlFile the XML file to process
     * @return the path of the output file that was written
     * @throws XMLProcessingException if processing fails
     */
   /**
 * Processes the XML file containing firm data
 * @param xmlFile the XML file to process
 * @param context processing context containing configuration and runtime state
 * @return the Path of the output file that was written
 * @throws XMLProcessingException if processing fails
 */
    public Path processXMLFile(File xmlFile, ProcessingContext context) throws XMLProcessingException {
        ProcessingLogger.logInfo("Starting XML processing for file: " + xmlFile.getName());
        ProcessingLogger.resetCounters();
        
        String outputFileName = constructOutputFileName(xmlFile.getName());
        Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, outputFileName);
        
        try (OutputStreamWriter firmWriter = new OutputStreamWriter(
                new FileOutputStream(outputFilePath.toFile(), false), StandardCharsets.UTF_8)) {
            firmWriter.write(Config.FIRM_HEADER + System.lineSeparator());
            parseXML(xmlFile, firmWriter, context);
            
            ProcessingLogger.logInfo("XML processing completed for file: " + xmlFile.getName());
            ProcessingLogger.printProcessingSummary();
            
            return outputFilePath;
        } catch (Exception e) {
            context.setLastError("Critical error processing XML file: " + xmlFile.getName() + " - " + e.getMessage());
            ProcessingLogger.logError("Critical error processing XML file: " + xmlFile.getName(), e);
            throw new XMLProcessingException("Error processing XML file: " + xmlFile.getName(), e);
        }
    }
    
  
    
    /**
     * Constructs the output file name based on the input file name
     */
    private String constructOutputFileName(String inputFileName) {
        try {
            // Extract date from input file name (e.g., "IA_FIRM_SEC_Feed_04_07_2025.xml")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("IA_FIRM_SEC_Feed_(\\d{2})_(\\d{2})_(\\d{4})\\.xml");
            java.util.regex.Matcher matcher = pattern.matcher(inputFileName);
            
            if (matcher.find()) {
                String month = matcher.group(1);
                String day = matcher.group(2);
                String year = matcher.group(3);
                
                String outputFileName = "IA_FIRM_SEC_DATA_" + year + month + day + ".csv";
                ProcessingLogger.logInfo("Successfully parsed date from filename: " + inputFileName + " -> " + outputFileName);
                return outputFileName;
            } else {
                ProcessingLogger.logWarning("Could not parse date from input file name: " + inputFileName + ". Using timestamp-based name.");
                ProcessingLogger.incrementFilenameParsingFailures();
                return "IA_FIRM_SEC_DATA_" + System.currentTimeMillis() + ".csv";
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error constructing output file name for: " + inputFileName, e);
            ProcessingLogger.incrementFilenameParsingFailures();
            return "IA_FIRM_SEC_DATA_" + System.currentTimeMillis() + ".csv";
        }
    }
    
    /**
     * Parses the XML file and extracts firm information
     * @param xmlFile the XML file to parse
     * @param firmWriter the writer to output firm data
     * @param context processing context containing configuration and runtime state
     */
    private void parseXML(File xmlFile, OutputStreamWriter firmWriter, ProcessingContext context) throws Exception {
        try (InputStream in = new FileInputStream(xmlFile)) {
            ProcessingLogger.logInfo("Processing: " + xmlFile.getCanonicalPath());
            ProcessingLogger.logInfo("Index limit set to: " + (context.getIndexLimit() == Integer.MAX_VALUE ? "unlimited" : context.getIndexLimit()));
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in, Config.ENCODING);
            
            while (reader.hasNext() && !context.hasReachedIndexLimit()) {
                reader.next();
                if (reader.getEventType() == XMLStreamReader.START_ELEMENT && 
                    "Firm".equals(reader.getLocalName())) {
                    
                    if (!processNextFirm(reader, firmWriter, context)) {
                        break;
                    }
                    
                    // Log progress periodically if verbose
                    if (context.isVerbose() && context.getProcessedFirms() % 100 == 0) {
                        context.logCurrentState();
                    }
                }
            }
            
            if (context.hasReachedIndexLimit() && context.getIndexLimit() != Integer.MAX_VALUE) {
                ProcessingLogger.logInfo("Reached index limit of " + context.getIndexLimit() + " firms. Processing stopped.");
            }
        }
    }
    
    /**
     * Processes a single firm record from the XML
     */
    private boolean processNextFirm(XMLStreamReader reader, OutputStreamWriter firmWriter, ProcessingContext context) throws Exception {
        FirmDataBuilder firmBuilder = new FirmDataBuilder();
        
        while (reader.hasNext()) {
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                String elementName = reader.getLocalName();
                
                switch (elementName) {
                    case "Info":
                        processInfoElement(reader, firmBuilder);
                        break;
                    case "Rgstn":
                        processRegistrationElement(reader, firmBuilder);
                        break;
                    case "Filing":
                        processFilingElement(reader, firmBuilder);
                        break;
                    case "MainAddr":
                        processAddressElement(reader, firmBuilder);
                        break;
                    case "Item5A":
                        processEmployeeElement(reader, firmBuilder);
                        break;
                    case "Item5F":
                        processAUMElement(reader, firmBuilder);
                        break;
                }
            } else if (reader.getEventType() == XMLStreamReader.END_ELEMENT && 
                      "Firm".equalsIgnoreCase(reader.getLocalName())) {
                
                // Increment firm counter in both ProcessingLogger and ProcessingContext
                ProcessingLogger.incrementTotalFirmsProcessed();
                context.incrementProcessedFirms();
                
                // Get brochure URL and write complete record
                String brochureURL = getBrochureURL(firmBuilder.getFirmCrdNb(), context);
                firmBuilder.setBrochureURL(brochureURL);
                
                writeFirmRecord(firmWriter, firmBuilder.build());
                
                Thread.sleep(1000); // Rate limiting
                return true;
            }
            reader.next();
        }
        return false;
    }
    
    private void processInfoElement(XMLStreamReader reader, FirmDataBuilder builder) {
        builder.setSECRgnCD(getAttributeValue(reader, "SECRgnCD"))
               .setFirmCrdNb(getAttributeValue(reader, "FirmCrdNb"))
               .setSECNb(getAttributeValue(reader, "SECNb"))
               .setBusNm(getAttributeValue(reader, "BusNm"))
               .setLegalNm(getAttributeValue(reader, "LegalNm"));
    }
    
    private void processRegistrationElement(XMLStreamReader reader, FirmDataBuilder builder) {
        builder.setFirmType(getAttributeValue(reader, "FirmType"))
               .setRegistrationState(getAttributeValue(reader, "St"))
               .setRegistrationDate(getAttributeValue(reader, "Dt"));
    }
    
    private void processFilingElement(XMLStreamReader reader, FirmDataBuilder builder) {
        String filingDate = getAttributeValue(reader, "Dt");
        builder.setFilingDate(filingDate)
               .setFormVersion(getAttributeValue(reader, "FormVrsn"));
    }
    
    private void processAddressElement(XMLStreamReader reader, FirmDataBuilder builder) {
        builder.setStreet1(getAttributeValue(reader, "Strt1"))
               .setStreet2(getAttributeValue(reader, "Strt2"))
               .setCity(getAttributeValue(reader, "City"))
               .setState(getAttributeValue(reader, "State"))
               .setCountry(getAttributeValue(reader, "Cntry"))
               .setPostalCode(getAttributeValue(reader, "PostlCd"))
               .setPhoneNumber(getAttributeValue(reader, "PhNb"))
               .setFaxNumber(getAttributeValue(reader, "FaxNb"));
    }
    
    private void processEmployeeElement(XMLStreamReader reader, FirmDataBuilder builder) {
        builder.setTotalEmployees(getAttributeValue(reader, "TtlEmp"));
    }
    
    private void processAUMElement(XMLStreamReader reader, FirmDataBuilder builder) {
        builder.setAUM(getAttributeValue(reader, "Q5F2C"))
               .setTotalAccounts(getAttributeValue(reader, "Q5F2F"));
    }
    
    /**
     * Helper method to get attribute value or empty string if null
     */
    private String getAttributeValue(XMLStreamReader reader, String attributeName) {
        String value = reader.getAttributeValue("", attributeName);
        return value != null ? value : "";
    }
    
    /**
     * Writes a complete firm record to CSV
     */
    private void writeFirmRecord(OutputStreamWriter writer, FirmData firmData) throws Exception {
        writer.write(firmData.getSECRgnCD() + ",");
        writer.write(firmData.getFirmCrdNb() + ",");
        writer.write(firmData.getSECNb() + ",");
        writer.write('"' + sanitizeValue(firmData.getBusNm()) + '"' + ",");
        writer.write('"' + sanitizeValue(firmData.getLegalNm()) + '"' + ",");
        writer.write('"' + sanitizeValue(firmData.getStreet1()) + '"' + ",");
        writer.write('"' + sanitizeValue(firmData.getStreet2()) + '"' + ",");
        writer.write('"' + sanitizeValue(firmData.getCity()) + '"' + ",");
        writer.write(firmData.getState() + ",");
        writer.write(firmData.getCountry() + ",");
        writer.write(firmData.getPostalCode() + ",");
        writer.write(firmData.getPhoneNumber() + ",");
        writer.write(firmData.getFaxNumber() + ",");
        writer.write(firmData.getFirmType() + ",");
        writer.write(firmData.getRegistrationState() + ",");
        writer.write(firmData.getRegistrationDate() + ",");
        writer.write(firmData.getFilingDate() + ",");
        writer.write(firmData.getFormVersion() + ",");
        writer.write(firmData.getTotalEmployees() + ",");
        writer.write(firmData.getAUM() + ",");
        writer.write(firmData.getTotalAccounts() + ",");
        
        if (firmData.getBrochureURL() != null) {
            writer.write(firmData.getBrochureURL());
        }
        writer.write(System.lineSeparator());
    }
    
    /**
     * Helper method to sanitize string values for CSV output
     */
    private String sanitizeValue(String value) {
        return value != null ? value.replaceAll("\"", "") : "";
    }
    
    /**
     * Retrieves the brochure URL for a given firm CRD number (URL only, no downloading)
     */
    private String getBrochureURL(String firmCrdNb, ProcessingContext context) {
        // Use retry logic for getting brochure URL
        String brochureURL = RetryUtils.executeWithRetry(() -> {
            try {
                String url = String.format(Config.FIRM_API_URL_FORMAT, firmCrdNb);
                String response = HttpUtils.getHTTPSResponse(url);
                
                if (response != null) {
                    Matcher matcher = PatternMatchers.API_BRCHR_VERSION_ID_PATTERN.matcher(response);
                    if (matcher.find()) {
                        String foundBrochureURL = Config.BROCHURE_URL_BASE + matcher.group(1);
                        return foundBrochureURL;
                    } else {
                        ProcessingLogger.logWarning("No brochure version ID found in API response for firm: " + firmCrdNb);
                        return null;
                    }
                } else {
                    throw new RuntimeException("No response received from API for firm: " + firmCrdNb);
                }
            } catch (Exception e) {
                // Check if this is a transient exception that should be retried
                if (RetryUtils.isTransientException(e)) {
                    throw new RuntimeException("Transient error getting brochure URL for firm " + firmCrdNb, e);
                } else {
                    // Non-transient error, don't retry
                    ProcessingLogger.logError("Non-transient error getting brochure URL for firm " + firmCrdNb, e);
                    return null;
                }
            }
        }, "Get brochure URL for firm " + firmCrdNb);
        
        if (brochureURL == null) {
            ProcessingLogger.incrementFirmsWithoutBrochures();
        }
        
        return brochureURL;
    }
}
