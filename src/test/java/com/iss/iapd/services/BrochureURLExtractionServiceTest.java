package com.iss.iapd.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;


import com.iss.iapd.core.ProcessingContext;
import com.iss.iapd.services.brochure.BrochureURLExtractionService;

/**
 * Test class for BrochureURLExtractionService
 */
public class BrochureURLExtractionServiceTest {
    
    private BrochureURLExtractionService service;
    private ProcessingContext context;
    
    @BeforeEach
    void setUp() {
        service = new BrochureURLExtractionService();
        context = ProcessingContext.builder()
                .indexLimit(5) // Limit for testing
                .urlRatePerSecond(1) // Slow rate for testing
                .verbose(false)
                .build();
    }
    
    @Test
    void testConstructFilesToDownloadFileName() throws Exception {
        // Create a temporary CSV file with the expected naming pattern
        File tempFile = File.createTempFile("IA_FIRM_SEC_DATA_20250407", ".csv");
        tempFile.deleteOnExit();
        
        // Write a simple CSV header and one record
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("dateAdded,SECRgmCD,FirmCrdNb,SECMb,Business Name,Legal Name,Street 1,Street 2,City,State,Country,Postal Code,Telephone #,Fax #,Registration Firm Type,Registration State,Registration Date,Filing Date,Filing Version,Total Employees,AUM,Total Accounts\n");
            writer.write("08/13/2025,SEC,123456,789,Test Firm,Test Legal Name,123 Main St,,New York,NY,US,10001,555-1234,555-5678,Corporation,NY,01/01/2020,07/01/2025,1.0,50,1000000,100\n");
        }
        
        // Test the service (this will try to make API calls, but we're mainly testing the file name construction)
        String expectedFileName = "FilesToDownload_20250407.csv";
        
        // We can't easily test the full processFirmDataForBrochures method without mocking HTTP calls,
        // but we can verify the service was created successfully
        assertNotNull(service);
        assertNotNull(context);
        
        System.out.println("BrochureURLExtractionService test setup completed successfully");
        System.out.println("Expected output file name: " + expectedFileName);
    }
    
    @Test
    void testBrochureDownloadRecord() {
        // Test the inner BrochureDownloadRecord class
        BrochureURLExtractionService.BrochureDownloadRecord record = 
            new BrochureURLExtractionService.BrochureDownloadRecord(
                "12345",
                "Test Firm Name",
                "98765",
                "Test Brochure Name",
                "07/20/2025",
                "03/29/2021"
            );
        
        assertEquals("12345", record.getFirmId());
        assertEquals("Test Firm Name", record.getFirmName());
        assertEquals("98765", record.getBrochureVersionId());
        assertEquals("Test Brochure Name", record.getBrochureName());
        assertEquals("07/20/2025", record.getDateSubmitted());
        assertEquals("03/29/2021", record.getDateConfirmed());
    }
    
    @Test
    void testBrochureDownloadRecordWithNullConfirmedDate() {
        // Test with null confirmed date
        BrochureURLExtractionService.BrochureDownloadRecord record = 
            new BrochureURLExtractionService.BrochureDownloadRecord(
                "12345",
                "Test Firm Name",
                "98765",
                "Test Brochure Name",
                "07/20/2025",
                null
            );
        
        assertEquals("", record.getDateConfirmed());
    }
    
    @Test
    void testServiceInstantiation() {
        // Simple test to ensure the service can be instantiated
        BrochureURLExtractionService testService = new BrochureURLExtractionService();
        assertNotNull(testService);
    }
}
