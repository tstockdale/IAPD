package com.iss.iapd.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iss.iapd.config.Config;
import com.iss.iapd.core.ProcessingContext;
import com.iss.iapd.services.brochure.BrochureAnalysis;
import com.iss.iapd.services.brochure.BrochureAnalyzer;
import com.iss.iapd.services.csv.CSVWriterService;
import com.iss.iapd.services.xml.XMLProcessingService;

/**
 * Integration test to verify that all XML data is properly included in output files
 */
public class XMLDataInclusionTest {
    
    @TempDir
    Path tempDir;
    
    private XMLProcessingService xmlProcessingService;
    private CSVWriterService csvWriterService;
    private ProcessingContext context;
    
    @BeforeEach
    void setUp() {
        xmlProcessingService = new XMLProcessingService();
        csvWriterService = new CSVWriterService();
        context = ProcessingContext.builder()
                .indexLimit(10) // Limit for testing
                .verbose(false)
                .build();
    }
    
    @Test
    void testXMLDataInclusionInIAPDSecData() throws Exception {
        // Create a sample XML file with comprehensive firm data
        String sampleXML = createSampleXMLContent();
        File xmlFile = createTempXMLFile(sampleXML);
        
        // Process the XML file
        Path outputFile = xmlProcessingService.processXMLFile(xmlFile, context);
        
        assertNotNull(outputFile, "Output file should be created");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        // Read the output file and verify all XML fields are included
        List<String> lines = Files.readAllLines(outputFile);
        assertTrue(lines.size() >= 2, "Should have header and at least one data row");
        
        // Verify header contains all expected fields
        String header = lines.get(0);
        String[] expectedFields = Config.FIRM_HEADER.split(",");
        
        for (String field : expectedFields) {
            assertTrue(header.contains(field.trim()), 
                "Header should contain field: " + field);
        }
        
        // Verify data row contains all fields (no empty trailing fields)
        if (lines.size() > 1) {
            String dataRow = lines.get(1);
            String[] dataFields = dataRow.split(",", -1); // -1 to include empty trailing fields
            
            assertEquals(expectedFields.length, dataFields.length, 
                "Data row should have same number of fields as header");
            
            // Verify key XML fields are populated (not empty)
            assertFalse(dataFields[1].isEmpty(), "SECRgnCD should not be empty");
            assertFalse(dataFields[2].isEmpty(), "FirmCrdNb should not be empty");
            assertFalse(dataFields[4].isEmpty(), "Business Name should not be empty");
            assertFalse(dataFields[5].isEmpty(), "Legal Name should not be empty");
        }
    }
    
    @Test
    void testCSVWriterServiceIncludesAllXMLFields() throws Exception {
        // Create a sample record map with all XML fields
        java.util.Map<String, String> recordMap = createSampleRecordMap();
        
        // Create a mock brochure analysis
        BrochureAnalyzer analyzer = new BrochureAnalyzer();
        BrochureAnalysis analysis = analyzer.analyzeBrochureContent("Sample brochure text", "12345");
        
        // Write to a temporary file
        Path outputFile = tempDir.resolve("test_output.csv");
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            // Write header first
            writer.write(Config.IAPD_DATA_HEADER + System.lineSeparator());
            
            // Write brochure analysis record
            csvWriterService.writeBrochureAnalysis(writer, recordMap, analysis, 
                "test_brochure.pdf", "https://example.com/brochure");
        }
        
        // Verify the output
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(2, lines.size(), "Should have header and one data row");
        
        String header = lines.get(0);
        String dataRow = lines.get(1);
        
        // Count fields in header and data row
        String[] headerFields = header.split(",");
        String[] dataFields = dataRow.split(",", -1); // -1 to include empty trailing fields
        
        assertEquals(headerFields.length, dataFields.length, 
            "Data row should have same number of fields as header");
        
        // Verify key XML fields are present in the data row
        assertTrue(dataRow.contains("TEST_SEC_RGN"), "Should contain SECRgnCD");
        assertTrue(dataRow.contains("12345"), "Should contain FirmCrdNb");
        assertTrue(dataRow.contains("Test Investment Advisors"), "Should contain Business Name");
        assertTrue(dataRow.contains("Test Legal Name"), "Should contain Legal Name");
        assertTrue(dataRow.contains("555-123-4567"), "Should contain phone number");
        assertTrue(dataRow.contains("555-123-4568"), "Should contain fax number");
    }
    
    @Test
    void testHeaderConsistencyBetweenFirmAndIAPDData() {
        // Verify that FIRM_HEADER and IAPD_DATA_HEADER have consistent base fields
        String[] firmFields = Config.FIRM_HEADER.split(",");
        String[] iapdFields = Config.IAPD_DATA_HEADER.split(",");
        
        // The first 23 fields should be identical (all XML fields)
        int xmlFieldCount = 23; // Based on FIRM_HEADER structure
        
        assertTrue(firmFields.length >= xmlFieldCount, 
            "FIRM_HEADER should have at least " + xmlFieldCount + " fields");
        assertTrue(iapdFields.length >= xmlFieldCount, 
            "IAPD_DATA_HEADER should have at least " + xmlFieldCount + " fields");
        
        // Compare the first xmlFieldCount fields
        for (int i = 0; i < xmlFieldCount; i++) {
            assertEquals(firmFields[i].trim(), iapdFields[i].trim(), 
                "Field " + i + " should be identical in both headers");
        }
    }
    
    private String createSampleXMLContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<IAData>\n" +
               "    <Firm>\n" +
               "        <Info SECRgnCD=\"TEST_SEC_RGN\" FirmCrdNb=\"12345\" SECNb=\"SEC123\" " +
               "              BusNm=\"Test Investment Advisors\" LegalNm=\"Test Legal Name\"/>\n" +
               "        <Rgstn FirmType=\"Investment Adviser\" St=\"NY\" Dt=\"2020-01-01\"/>\n" +
               "        <Filing Dt=\"2023-12-31\" FormVrsn=\"2.0\"/>\n" +
               "        <MainAddr Strt1=\"123 Main Street\" Strt2=\"Suite 100\" City=\"New York\" " +
               "                  State=\"NY\" Cntry=\"USA\" PostlCd=\"10001\" PhNb=\"555-123-4567\" FaxNb=\"555-123-4568\"/>\n" +
               "        <Item5A TtlEmp=\"50\"/>\n" +
               "        <Item5F Q5F2C=\"1000000000\" Q5F2F=\"500\"/>\n" +
               "    </Firm>\n" +
               "</IAData>";
    }
    
    private File createTempXMLFile(String content) throws IOException {
        File xmlFile = tempDir.resolve("test_input.xml").toFile();
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(content);
        }
        return xmlFile;
    }
    
    private java.util.Map<String, String> createSampleRecordMap() {
        java.util.Map<String, String> recordMap = new java.util.HashMap<>();
        
        // Add all XML fields that should be included
        recordMap.put("SECRgmCD", "TEST_SEC_RGN");
        recordMap.put("FirmCrdNb", "12345");
        recordMap.put("SECMb", "SEC123");
        recordMap.put("Business Name", "Test Investment Advisors");
        recordMap.put("Legal Name", "Test Legal Name");
        recordMap.put("Street 1", "123 Main Street");
        recordMap.put("Street 2", "Suite 100");
        recordMap.put("City", "New York");
        recordMap.put("State", "NY");
        recordMap.put("Country", "USA");
        recordMap.put("Postal Code", "10001");
        recordMap.put("Telephone #", "555-123-4567");
        recordMap.put("Fax #", "555-123-4568");
        recordMap.put("Registration Firm Type", "Investment Adviser");
        recordMap.put("Registration State", "NY");
        recordMap.put("Registration Date", "2020-01-01");
        recordMap.put("Filing Date", "2023-12-31");
        recordMap.put("Filing Version", "2.0");
        recordMap.put("Total Employees", "50");
        recordMap.put("AUM", "1000000000");
        recordMap.put("Total Accounts", "500");
        recordMap.put("BrochureURL", "");
        
        return recordMap;
    }
}
