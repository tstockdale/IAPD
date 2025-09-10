package com.iss.iapd.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iss.iapd.services.brochure.BrochureAnalysis;
import com.iss.iapd.services.brochure.BrochureProcessingStatistics;

/**
 * Test class for BrochureProcessingStatistics
 * Validates the comprehensive statistics collection and reporting functionality
 */
public class BrochureProcessingStatisticsTest {
    
    private BrochureProcessingStatistics statistics;
    
    @BeforeEach
    public void setUp() {
        statistics = new BrochureProcessingStatistics();
    }
    
    @Test
    public void testInitialStatistics() {
        // Test initial state
        assertEquals(0, statistics.getTotalBrochuresAttempted());
        assertEquals(0, statistics.getTotalBrochuresSuccessful());
        assertEquals(0, statistics.getTotalBrochuresFailed());
        assertEquals(0.0, statistics.getSuccessRate(), 0.01);
    }
    
    @Test
    public void testSuccessfulAnalysisRecording() {
        // Create a mock brochure analysis
        BrochureAnalysis analysis = new BrochureAnalysis();
        analysis.getProxyProvider().append("Glass Lewis");
        analysis.getClassActionProvider().append("ISS");
        analysis.getEsgProvider().append("MSCI");
        analysis.getEmailSet().add("test@example.com");
        analysis.getEmailSet().add("compliance@firm.com");
        
        // Record successful analysis
        statistics.recordSuccessfulAnalysis(analysis);
        
        // Verify statistics
        assertEquals(1, statistics.getTotalBrochuresAttempted());
        assertEquals(1, statistics.getTotalBrochuresSuccessful());
        assertEquals(0, statistics.getTotalBrochuresFailed());
        assertEquals(100.0, statistics.getSuccessRate(), 0.01);
        
        // Verify content analysis statistics
        assertEquals(1, statistics.getBrochuresWithProxyProviders());
        assertEquals(1, statistics.getBrochuresWithClassActionProviders());
        assertEquals(1, statistics.getBrochuresWithESGProviders());
        assertEquals(1, statistics.getBrochuresWithEmailAddresses());
        assertEquals(2, statistics.getTotalEmailAddressesFound());
    }
    
    @Test
    public void testFailedAnalysisRecording() {
        // Record failed analysis
        statistics.recordFailedAnalysis("PDF parsing error");
        
        // Verify statistics
        assertEquals(1, statistics.getTotalBrochuresAttempted());
        assertEquals(0, statistics.getTotalBrochuresSuccessful());
        assertEquals(1, statistics.getTotalBrochuresFailed());
        assertEquals(0.0, statistics.getSuccessRate(), 0.01);
    }
    
    @Test
    public void testMixedAnalysisRecording() {
        // Create successful analysis
        BrochureAnalysis analysis1 = new BrochureAnalysis();
        analysis1.getProxyProvider().append("Glass Lewis");
        analysis1.getEmailSet().add("test1@example.com");
        
        BrochureAnalysis analysis2 = new BrochureAnalysis();
        analysis2.getClassActionProvider().append("FRT");
        analysis2.getEmailSet().add("test2@example.com");
        analysis2.getEmailSet().add("test3@example.com");
        
        // Record mixed results
        statistics.recordSuccessfulAnalysis(analysis1);
        statistics.recordFailedAnalysis("Text extraction failed");
        statistics.recordSuccessfulAnalysis(analysis2);
        
        // Verify overall statistics
        assertEquals(3, statistics.getTotalBrochuresAttempted());
        assertEquals(2, statistics.getTotalBrochuresSuccessful());
        assertEquals(1, statistics.getTotalBrochuresFailed());
        assertEquals(66.67, statistics.getSuccessRate(), 0.01);
        
        // Verify content analysis statistics
        assertEquals(1, statistics.getBrochuresWithProxyProviders());
        assertEquals(1, statistics.getBrochuresWithClassActionProviders());
        assertEquals(0, statistics.getBrochuresWithESGProviders());
        assertEquals(2, statistics.getBrochuresWithEmailAddresses());
        assertEquals(3, statistics.getTotalEmailAddressesFound());
    }
    
    @Test
    public void testFileProcessingStatistics() {
        // Record file processing
        statistics.recordFileProcessed();
        statistics.recordFileProcessed();
        statistics.recordFileSkipped();
        statistics.recordResumedProcessing();
        
        // Note: File processing counters are not exposed via getters in the current implementation
        // This test validates that the methods can be called without errors
        // In a real scenario, these would be verified through the comprehensive summary output
        
        // Verify that the statistics object is still functional
        assertEquals(0, statistics.getTotalBrochuresAttempted());
    }
    
    @Test
    public void testDetectionRates() {
        // Create analyses with different provider combinations
        BrochureAnalysis analysis1 = new BrochureAnalysis();
        analysis1.getProxyProvider().append("Glass Lewis");
        analysis1.getEmailSet().add("test1@example.com");
        
        BrochureAnalysis analysis2 = new BrochureAnalysis();
        analysis2.getClassActionProvider().append("ISS");
        
        BrochureAnalysis analysis3 = new BrochureAnalysis();
        analysis3.getProxyProvider().append("ISS");
        analysis3.getEsgProvider().append("Sustainalytics");
        analysis3.getEmailSet().add("test2@example.com");
        analysis3.getEmailSet().add("test3@example.com");
        
        // Record successful analyses
        statistics.recordSuccessfulAnalysis(analysis1);
        statistics.recordSuccessfulAnalysis(analysis2);
        statistics.recordSuccessfulAnalysis(analysis3);
        
        // Verify detection rates
        assertEquals(66.67, statistics.getProxyProviderDetectionRate(), 0.01); // 2 out of 3
        assertEquals(33.33, statistics.getClassActionProviderDetectionRate(), 0.01); // 1 out of 3
        assertEquals(33.33, statistics.getESGProviderDetectionRate(), 0.01); // 1 out of 3
        assertEquals(66.67, statistics.getEmailDetectionRate(), 0.01); // 2 out of 3
    }
    
    @Test
    public void testProviderCombinations() {
        // Create analysis with multiple providers
        BrochureAnalysis analysis = new BrochureAnalysis();
        analysis.getProxyProvider().append("Glass Lewis");
        analysis.getClassActionProvider().append("ISS");
        analysis.getEsgProvider().append("MSCI");
        
        statistics.recordSuccessfulAnalysis(analysis);
        
        // Verify provider combinations are tracked
        assertFalse(statistics.getProviderCombinations().isEmpty());
        
        // The combination should contain all three provider types
        String expectedCombination = "Proxy:Glass Lewis|ClassAction:ISS|ESG:MSCI";
        assertTrue(statistics.getProviderCombinations().containsKey(expectedCombination));
        assertEquals(1, statistics.getProviderCombinations().get(expectedCombination).get());
    }
    
    @Test
    public void testTimingCalculations() {
        // Start processing
        statistics.startProcessing();
        
        // Simulate some processing time
        try {
            Thread.sleep(10); // Small delay for timing test
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Record some successful analyses
        BrochureAnalysis analysis = new BrochureAnalysis();
        statistics.recordSuccessfulAnalysis(analysis);
        
        // End processing
        statistics.endProcessing();
        
        // Verify timing calculations
        assertTrue(statistics.getElapsedTimeMs() > 0);
        assertTrue(statistics.getAverageProcessingTimeMs() > 0);
        assertTrue(statistics.getThroughputPerMinute() >= 0);
    }
    
    @Test
    public void testComprehensiveSummaryExecution() {
        // Create some test data
        BrochureAnalysis analysis1 = new BrochureAnalysis();
        analysis1.getProxyProvider().append("Glass Lewis");
        analysis1.getEmailSet().add("test@example.com");
        
        statistics.recordSuccessfulAnalysis(analysis1);
        statistics.recordFailedAnalysis("Test failure");
        statistics.recordFileProcessed();
        statistics.recordFileSkipped();
        
        // This should execute without throwing exceptions
        assertDoesNotThrow(() -> {
            statistics.printComprehensiveSummary();
        });
        
        // This should also execute without throwing exceptions
        assertDoesNotThrow(() -> {
            statistics.printProgressSummary();
        });
    }
}
