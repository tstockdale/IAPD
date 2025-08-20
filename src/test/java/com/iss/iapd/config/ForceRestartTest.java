package com.iss.iapd.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.iss.iapd.core.IAFirmSECParserRefactored;
import com.iss.iapd.core.ProcessingContext;

/**
 * Test class for force restart functionality
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ForceRestartTest {
    
    private Path testDataDir;
    private Path testSubDir;
    private Path testFile;
    private String testId;
    
    @BeforeAll
    public void setUpClass() throws Exception {
        // Generate unique test ID to avoid conflicts between test runs
        testId = UUID.randomUUID().toString().substring(0, 8);
        
        // Clean up any existing test directories from previous runs
        cleanupAllTestDirectories();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create a test Data directory structure
        testDataDir = Paths.get("./Data");
        testSubDir = testDataDir.resolve("TestSubDir");
        testFile = testDataDir.resolve("test.txt");
        
        // Clean up any existing test directories
        cleanupAllTestDirectories();
        
        // Wait a moment to ensure cleanup is complete (Windows file system timing)
        Thread.sleep(100);
        
        // Create test directory structure
        Files.createDirectories(testSubDir);
        Files.write(testFile, "Test content".getBytes());
        
        assertTrue(Files.exists(testDataDir), "Test Data directory should exist");
        assertTrue(Files.exists(testSubDir), "Test subdirectory should exist");
        assertTrue(Files.exists(testFile), "Test file should exist");
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        // Wait a moment before cleanup to ensure all operations are complete
        Thread.sleep(100);
        cleanupAllTestDirectories();
    }
    
    @AfterAll
    public void tearDownClass() throws Exception {
        // Final cleanup to ensure no test artifacts remain
        cleanupAllTestDirectories();
    }
    
    private void cleanupAllTestDirectories() throws Exception {
        // Clean up test directories (both Data and any Data_* backup directories)
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && (file.getName().equals("Data") || file.getName().startsWith("Data_"))) {
                    deleteDirectoryRobust(file.toPath());
                }
            }
        }
    }
    
    private void deleteDirectoryRobust(Path directory) throws Exception {
        if (Files.exists(directory)) {
            // Try multiple times to handle Windows file system timing issues
            int maxAttempts = 3;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    Files.walk(directory)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                // Ignore deletion errors in test cleanup
                            }
                        });
                    
                    // If directory still exists after walking, try direct deletion
                    if (Files.exists(directory)) {
                        Files.delete(directory);
                    }
                    
                    // If we get here without exception, deletion succeeded
                    break;
                    
                } catch (Exception e) {
                    if (attempt == maxAttempts) {
                        // Last attempt failed, but don't throw exception to avoid test failures
                        System.err.println("Warning: Could not delete directory " + directory + " after " + maxAttempts + " attempts: " + e.getMessage());
                    } else {
                        // Wait before retry
                        Thread.sleep(50);
                    }
                }
            }
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
        
        // Count existing backup directories before the test
        int backupCountBefore = countBackupDirectories();
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Wait a moment for the operation to complete
        Thread.sleep(100);
        
        // Assert
        assertFalse(Files.exists(testDataDir), "Original Data directory should no longer exist");
        
        // Check that exactly one new backup directory was created
        int backupCountAfter = countBackupDirectories();
        assertEquals(backupCountBefore + 1, backupCountAfter, "Exactly one new backup directory should have been created");
        
        // Find and verify the new backup directory
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        boolean backupFound = false;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    // Verify the backup contains our test content
                    Path backupTestFile = file.toPath().resolve("test.txt");
                    if (Files.exists(backupTestFile)) {
                        String content = new String(Files.readAllBytes(backupTestFile));
                        if ("Test content".equals(content)) {
                            backupFound = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(backupFound, "A backup directory with the correct test content should have been created");
    }
    
    @Test
    public void testForceRestartWithoutExistingDataDirectory() throws Exception {
        // Arrange
        deleteDirectoryRobust(testDataDir); // Remove the test data directory
        
        // Wait a moment to ensure directory is fully deleted
        Thread.sleep(200);
        
        // Verify the directory is actually gone
        assertFalse(Files.exists(testDataDir), "Data directory should not exist before test");
        
        // Create a processing context with force restart enabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(true)
                .build();
        
        // Count existing backup directories before the test
        int backupCountBefore = countBackupDirectories();
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Wait a moment for the operation to complete
        Thread.sleep(100);
        
        // Assert
        // Count backup directories after the test
        int backupCountAfter = countBackupDirectories();
        
        // If there was no Data directory initially, no new backup should be created
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
        
        // Count existing backup directories before the test
        int backupCountBefore = countBackupDirectories();
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Wait a moment for the operation to complete
        Thread.sleep(100);
        
        // Assert
        assertTrue(Files.exists(testDataDir), "Original Data directory should still exist");
        assertTrue(Files.exists(testFile), "Test file should still exist");
        
        // Check that no new backup directory was created
        int backupCountAfter = countBackupDirectories();
        assertEquals(backupCountBefore, backupCountAfter, 
            "No new backup directory should have been created when force restart is disabled");
    }
    
    /**
     * Helper method to count backup directories
     */
    private int countBackupDirectories() {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    count++;
                }
            }
        }
        return count;
    }
}
