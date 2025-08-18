package com.iss.iapd.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iss.iapd.core.IAFirmSECParserRefactored;
import com.iss.iapd.core.ProcessingContext;

/**
 * Test class for force restart functionality
 */
public class ForceRestartTest {
    
    private Path testDataDir;
    private Path testSubDir;
    private Path testFile;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create a test Data directory structure
        testDataDir = Paths.get("./Data");
        testSubDir = testDataDir.resolve("TestSubDir");
        testFile = testDataDir.resolve("test.txt");
        
        // Clean up any existing test directories
        cleanupTestDirectories();
        
        // Create test directory structure
        Files.createDirectories(testSubDir);
        Files.write(testFile, "Test content".getBytes());
        
        assertTrue(Files.exists(testDataDir), "Test Data directory should exist");
        assertTrue(Files.exists(testSubDir), "Test subdirectory should exist");
        assertTrue(Files.exists(testFile), "Test file should exist");
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        cleanupTestDirectories();
    }
    
    private void cleanupTestDirectories() throws Exception {
        // Clean up test directories (both Data and any Data_* backup directories)
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && (file.getName().equals("Data") || file.getName().startsWith("Data_"))) {
                    deleteDirectory(file.toPath());
                }
            }
        }
    }
    
    private void deleteDirectory(Path directory) throws Exception {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        // Ignore deletion errors in test cleanup
                    }
                });
        }
    }
    
    @Test
    public void testForceRestartWithExistingDataDirectory() throws Exception {
        // Arrange
        assertTrue(Files.exists(testDataDir), "Data directory should exist before test");
        
        // Create a processing context with force restart enabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(true)
                .build();
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Assert
        assertFalse(Files.exists(testDataDir), "Original Data directory should no longer exist");
        
        // Check that a backup directory was created
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        boolean backupFound = false;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    backupFound = true;
                    // Verify the backup contains our test content
                    Path backupTestFile = file.toPath().resolve("test.txt");
                    assertTrue(Files.exists(backupTestFile), "Backup should contain test file");
                    
                    String content = new String(Files.readAllBytes(backupTestFile));
                    assertEquals("Test content", content, "Backup file should contain original content");
                    break;
                }
            }
        }
        assertTrue(backupFound, "A backup directory with timestamp should have been created");
    }
    
    @Test
    public void testForceRestartWithoutExistingDataDirectory() throws Exception {
        // Arrange
        deleteDirectory(testDataDir); // Remove the test data directory
        
        // Wait a moment to ensure directory is fully deleted
        Thread.sleep(100);
        
        // Create a processing context with force restart enabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(true)
                .build();
        
        // Count existing backup directories before the test
        File currentDir = new File(".");
        File[] filesBefore = currentDir.listFiles();
        int backupCountBefore = 0;
        if (filesBefore != null) {
            for (File file : filesBefore) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    backupCountBefore++;
                }
            }
        }
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Assert
        // The logging system may create a Data directory, so we check that no NEW backup was created
        File[] filesAfter = currentDir.listFiles();
        int backupCountAfter = 0;
        if (filesAfter != null) {
            for (File file : filesAfter) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    backupCountAfter++;
                }
            }
        }
        
        // If there was no Data directory initially, no new backup should be created
        // (The logging system might create a Data directory, but that's expected)
        assertEquals(backupCountBefore, backupCountAfter, 
            "No new backup directory should have been created when Data directory doesn't exist initially");
    }
    
    @Test
    public void testNoForceRestartWhenDisabled() throws Exception {
        // Arrange
        assertTrue(Files.exists(testDataDir), "Data directory should exist before test");
        
        // Create a processing context with force restart disabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(false)
                .build();
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Assert
        assertTrue(Files.exists(testDataDir), "Original Data directory should still exist");
        assertTrue(Files.exists(testFile), "Test file should still exist");
        
        // Check that no backup directory was created
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        boolean backupFound = false;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    backupFound = true;
                    break;
                }
            }
        }
        assertFalse(backupFound, "No backup directory should have been created when force restart is disabled");
    }
}
