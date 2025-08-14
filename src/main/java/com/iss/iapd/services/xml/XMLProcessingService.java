package com.iss.iapd.services.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.core.ProcessingContext;
import com.iss.iapd.exceptions.XMLProcessingException;
import com.iss.iapd.services.incremental.IncrementalUpdateManager;
import com.iss.iapd.model.FirmDataBuilder;
import com.iss.iapd.model.FirmData;
import com.iss.iapd.config.Config;

/**
 * Service class responsible for XML processing operations
 */
public class XMLProcessingService {

    public XMLProcessingService() {
      
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
        
        // Check if incremental processing is enabled
        if (context.isIncrementalUpdates() && context.getBaselineFilePath() != null) {
            return processXMLFileIncremental(xmlFile, context);
        } else {
            return processXMLFileStandard(xmlFile, context);
        }
    }
    
    /**
     * Processes XML file in standard mode (all firms)
     */
    private Path processXMLFileStandard(File xmlFile, ProcessingContext context) throws XMLProcessingException {
        String outputFileName = constructOutputFileName(xmlFile.getName());
        Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, outputFileName);
        
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(outputFilePath.toFile(), false), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(osw, CSVFormat.EXCEL
                     .builder()
                     .setQuoteMode(QuoteMode.MINIMAL)
                     .setRecordSeparator(System.lineSeparator())
                     .build())) {
            // Write header
            String[] headers = Config.FIRM_HEADER.split(",");
            printer.printRecord((Object[]) headers);
            
            parseXML(xmlFile, printer, context);
            
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
     * Processes XML file in incremental mode (only new/updated firms)
     */
    private Path processXMLFileIncremental(File xmlFile, ProcessingContext context) throws XMLProcessingException {
        IncrementalUpdateManager incrementalManager = new IncrementalUpdateManager();
        Path baselineFile = Paths.get(context.getBaselineFilePath());
        
        // Validate baseline file structure
        if (!incrementalManager.validateBaselineFileStructure(baselineFile)) {
            ProcessingLogger.logWarning("Baseline file structure invalid or missing. Falling back to standard processing.");
            return processXMLFileStandard(xmlFile, context);
        }
        
        try {
        	ProcessingLogger.logInfo("Incremental XML processing started.");
            // First pass: collect all firm data from XML
            List<FirmData> allFirms = collectAllFirmData(xmlFile, context);
            
            // Load historical filing dates
            Map<String, String> historicalDates = incrementalManager.getHistoricalFilingDates(baselineFile);
            
            // Calculate incremental statistics
            IncrementalUpdateManager.IncrementalStats stats = 
                    incrementalManager.calculateIncrementalStats(allFirms, historicalDates);
            
            // Log incremental statistics
            incrementalManager.logIncrementalStats(stats, baselineFile);
            
            // Filter firms for processing
            List<FirmData> firmsToProcess = incrementalManager.filterFirmsForProcessing(allFirms, historicalDates);
            
            // Generate incremental output file name
            String baseFileName = extractBaseFileName(xmlFile.getName());
            String dateString = extractDateString(xmlFile.getName());
            String incrementalFileName = incrementalManager.generateIncrementalFileName(baseFileName, dateString, ".csv");
            Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, incrementalFileName);
            
            // Write filtered firms to output file using Commons CSV
            try (OutputStreamWriter osw = new OutputStreamWriter(
                    new FileOutputStream(outputFilePath.toFile(), false), StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(osw, CSVFormat.EXCEL
                         .builder()
                         .setQuoteMode(QuoteMode.MINIMAL)
                         .setRecordSeparator(System.lineSeparator())
                         .build())) {
                // Header
                String[] headers = Config.FIRM_HEADER.split(",");
                printer.printRecord((Object[]) headers);

                for (FirmData firm : firmsToProcess) {
                    writeFirmRecord(printer, firm);
                }
            }
            
            ProcessingLogger.logInfo("Incremental XML processing completed. Output file: " + outputFilePath);
            ProcessingLogger.logInfo("Processed " + firmsToProcess.size() + " firms out of " + allFirms.size() + " total firms");
            
            return outputFilePath;
            
        } catch (Exception e) {
            context.setLastError("Critical error in incremental XML processing: " + xmlFile.getName() + " - " + e.getMessage());
            ProcessingLogger.logError("Critical error in incremental XML processing: " + xmlFile.getName(), e);
            throw new XMLProcessingException("Error in incremental XML processing: " + xmlFile.getName(), e);
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
    private void parseXML(File xmlFile, CSVPrinter printer, ProcessingContext context) throws Exception {
        try (InputStream in = new FileInputStream(xmlFile)) {
            ProcessingLogger.logInfo("Processing: " + xmlFile.getCanonicalPath());
            ProcessingLogger.logInfo("Index limit set to: " + (context.getIndexLimit() == Integer.MAX_VALUE ? "unlimited" : context.getIndexLimit()));
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in, Config.ENCODING);
            
        while (reader.hasNext() && !context.hasReachedIndexLimit()) {
                reader.next();
                if (reader.getEventType() == XMLStreamReader.START_ELEMENT && 
                    "Firm".equals(reader.getLocalName())) {
                    
                    if (!processNextFirm(reader, printer, context)) {
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
    private boolean processNextFirm(XMLStreamReader reader, CSVPrinter printer, ProcessingContext context) throws Exception {
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
                String brochureURL = null;
                firmBuilder.setBrochureURL(brochureURL);
                
                writeFirmRecord(printer, firmBuilder.build());
                
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
    private void writeFirmRecord(CSVPrinter printer, FirmData firmData) throws Exception {
        printer.printRecord(
            Config.getCurrentDateString(),
            firmData.getSECRgnCD(),
            firmData.getFirmCrdNb(),
            firmData.getSECNb(),
            sanitizeValue(firmData.getBusNm()),
            sanitizeValue(firmData.getLegalNm()),
            sanitizeValue(firmData.getStreet1()),
            sanitizeValue(firmData.getStreet2()),
            sanitizeValue(firmData.getCity()),
            firmData.getState(),
            firmData.getCountry(),
            firmData.getPostalCode(),
            firmData.getPhoneNumber(),
            firmData.getFaxNumber(),
            firmData.getFirmType(),
            firmData.getRegistrationState(),
            firmData.getRegistrationDate(),
            firmData.getFilingDate(),
            firmData.getFormVersion(),
            firmData.getTotalEmployees(),
            firmData.getAUM(),
            firmData.getTotalAccounts(),
            ""
        );
    }
    
    /**
     * Helper method to sanitize string values for CSV output
     */
    private String sanitizeValue(String value) {
        return value != null ? value.replaceAll("\"", "") : "";
    }
    
    /**
     * Collects all firm data from XML file without writing to output
     * Used for incremental processing to get complete firm list first
     */
    private List<FirmData> collectAllFirmData(File xmlFile, ProcessingContext context) throws Exception {
        List<FirmData> allFirms = new ArrayList<>();
        
        try (InputStream in = new FileInputStream(xmlFile)) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in, Config.ENCODING);
            
            while (reader.hasNext() && !context.hasReachedIndexLimit()) {
                reader.next();
                if (reader.getEventType() == XMLStreamReader.START_ELEMENT && 
                    "Firm".equals(reader.getLocalName())) {
                    
                    FirmData firmData = collectNextFirmData(reader, context);
                    if (firmData != null) {
                        allFirms.add(firmData);
                        context.incrementProcessedFirms();
                        
                        // Log progress periodically if verbose
                        if (context.isVerbose() && allFirms.size() % 100 == 0) {
                            ProcessingLogger.logInfo("Collected " + allFirms.size() + " firms for incremental analysis...");
                        }
                    }
                }
            }
        }
        
        ProcessingLogger.logInfo("Collected " + allFirms.size() + " total firms from XML for incremental processing");
        return allFirms;
    }
    
    /**
     * Collects a single firm's data from XML without writing to output
     */
    private FirmData collectNextFirmData(XMLStreamReader reader, ProcessingContext context) throws Exception {
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
                
                // Get brochure URL and build complete firm data
                String brochureURL = null;
                firmBuilder.setBrochureURL(brochureURL);
                
                return firmBuilder.build();
            }
            reader.next();
        }
        return null;
    }
    
    /**
     * Extracts base file name from input file name
     * e.g., "IA_FIRM_SEC_Feed_04_07_2025.xml" -> "IA_FIRM_SEC_DATA"
     */
    private String extractBaseFileName(String inputFileName) {
        return "IA_FIRM_SEC_DATA";
    }
    
    /**
     * Extracts date string from input file name
     * e.g., "IA_FIRM_SEC_Feed_04_07_2025.xml" -> "20250407"
     */
    private String extractDateString(String inputFileName) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("IA_FIRM_SEC_Feed_(\\d{2})_(\\d{2})_(\\d{4})\\.xml");
            java.util.regex.Matcher matcher = pattern.matcher(inputFileName);
            
            if (matcher.find()) {
                String month = matcher.group(1);
                String day = matcher.group(2);
                String year = matcher.group(3);
                return year + month + day;
            } else {
                return String.valueOf(System.currentTimeMillis());
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error extracting date from filename: " + inputFileName, e);
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
