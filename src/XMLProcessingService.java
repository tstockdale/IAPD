import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.regex.Matcher;

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
    public String processXMLFile(File xmlFile) throws XMLProcessingException {
        String outputFileName = constructOutputFileName(xmlFile.getName());
        String outputFilePath = Config.BROCHURE_INPUT_PATH + outputFileName;
        
        try (FileWriter firmWriter = new FileWriter(new File(outputFilePath), false)) {
            firmWriter.write(Config.FIRM_HEADER + System.lineSeparator());
            parseXML(xmlFile, firmWriter);
            return outputFilePath;
        } catch (Exception e) {
            throw new XMLProcessingException("Error processing XML file: " + xmlFile.getName(), e);
        }
    }
    
    /**
     * Processes the XML file using the default input file
     * @return the path of the output file that was written
     * @throws XMLProcessingException if processing fails
     */
    public String processDefaultXMLFile() throws XMLProcessingException {
        File xmlFile = new File(Config.INPUT_FILE);
        return processXMLFile(xmlFile);
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
                
                return "IA_FIRM_SEC_DATA" + year + month + day + ".csv";
            } else {
                System.err.println("Warning: Could not parse date from input file name: " + inputFileName);
                return "IA_FIRM_SEC_DATA_" + System.currentTimeMillis() + ".csv";
            }
        } catch (Exception e) {
            System.err.println("Error constructing output file name: " + e.getMessage());
            return "IA_FIRM_SEC_DATA_" + System.currentTimeMillis() + ".csv";
        }
    }
    
    /**
     * Parses the XML file and extracts firm information
     */
    private void parseXML(File xmlFile, FileWriter firmWriter) throws Exception {
        try (InputStream in = new FileInputStream(xmlFile)) {
            System.out.println("Processing: " + xmlFile.getCanonicalPath());
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in, Config.ENCODING);
            
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLStreamReader.START_ELEMENT && 
                    "Firm".equals(reader.getLocalName())) {
                    if (!processNextFirm(reader, firmWriter)) {
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Processes a single firm record from the XML
     */
    private boolean processNextFirm(XMLStreamReader reader, FileWriter firmWriter) throws Exception {
        FirmDataBuilder firmBuilder = new FirmDataBuilder();
        
        while (reader.hasNext()) {
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                String elementName = reader.getLocalName();
                System.out.println("Processing element: " + elementName);
                
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
                
                // Get brochure URL and write complete record
                String brochureURL = getBrochureURL(firmBuilder.getFirmCrdNb());
                firmBuilder.setBrochureURL(brochureURL);
                
                writeFirmRecord(firmWriter, firmBuilder.build());
                
                Thread.sleep(2000); // Rate limiting
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
    private void writeFirmRecord(FileWriter writer, FirmData firmData) throws Exception {
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
     * Retrieves the brochure URL for a given firm CRD number
     */
    private String getBrochureURL(String firmCrdNb) {
        try {
            String url = String.format(Config.FIRM_API_URL_FORMAT, firmCrdNb);
            String response = HttpUtils.getHTTPSResponse(url);
            
            if (response != null) {
                Matcher matcher = PatternMatchers.API_BRCHR_VERSION_ID_PATTERN.matcher(response);
                if (matcher.find()) {
                    String brochureURL = Config.BROCHURE_URL_BASE + matcher.group(1);
                    String fileName = firmCrdNb + "_" + matcher.group(1) + ".pdf";
                    downloadService.downloadBrochure(brochureURL, fileName);
                    return brochureURL;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting brochure URL for firm " + firmCrdNb + ": " + e.getMessage());
        }
        return null;
    }
}
