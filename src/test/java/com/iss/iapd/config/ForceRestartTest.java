package com.iss.iapd.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;

import com.iss.iapd.core.IAFirmSECParserRefactored;
import com.iss.iapd.core.ProcessingContext;

/**
 * Test class for force restart functionality with improved test isolation
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ForceRestartTest {
    
    private Path testRootDir;
    private Path testDataDir;
    private Path testSubDir;
    private Path testFile;
    private String originalUserDir;
    private String testId;
    private Path testXmlFile;
    
    @BeforeAll
    public void setUpClass() throws Exception {
        // Generate unique test ID to avoid conflicts
        testId = "ForceRestartTest_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Store original working directory
        originalUserDir = System.getProperty("user.dir");
        
        // Create a unique test directory in the system temp directory
        testRootDir = Files.createTempDirectory(testId);
        
        // Clean up any existing test artifacts from previous runs
        cleanupAllTestDirectories();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        // Set working directory to our test root directory
        System.setProperty("user.dir", testRootDir.toString());

    // Set up paths only; do not create Data directory or files here
    testDataDir = testRootDir.resolve("Data");
    testSubDir = testDataDir.resolve("TestSubDir");
    testFile = testDataDir.resolve("test.txt");
    testXmlFile = testRootDir.resolve("IA_FIRM_SEC_FEED.xml");
    // Ensure clean state
    deleteDirectoryIfExists(testDataDir);
    Files.deleteIfExists(testXmlFile);
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        try {
            // Clean up test directories in test root directory
            cleanupTestDirectories(testRootDir);
            // Clean up XML file if it was created
            if (Files.exists(testXmlFile)) {
                Files.deleteIfExists(testXmlFile);
            }
        } finally {
            // Always restore original working directory
            System.setProperty("user.dir", originalUserDir);
        }
    }
    // Helper to detect if the current test is the XML test
    private boolean isXmlTestRunning() {
        // Use stack trace to check if testXMLDataInclusionInIAPDSecData is running
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (ste.getMethodName().equals("testXMLDataInclusionInIAPDSecData")) {
                return true;
            }
        }
        return false;
    }
    
    @AfterAll
    public void tearDownClass() throws Exception {
        try {
            // Final cleanup
            cleanupAllTestDirectories();
            
            // Delete the test root directory
            if (Files.exists(testRootDir)) {
                deleteDirectoryIfExists(testRootDir);
            }
        } finally {
            // Always restore original working directory
            System.setProperty("user.dir", originalUserDir);
        }
    }
    
    /**
     * Robust directory deletion that handles Windows file system timing issues
     */
    private void deleteDirectoryIfExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        // Use Files.walkFileTree for reliable deletion
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    // If it's a log file that's locked, skip it for now
                    if (file.toString().contains(".log")) {
                        System.out.println("Skipping locked log file: " + file);
                        return FileVisitResult.CONTINUE;
                    }
                    throw e;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // If directory is not empty due to locked files, skip it
                    if (e instanceof java.nio.file.DirectoryNotEmptyException) {
                        System.out.println("Skipping non-empty directory: " + dir);
                        return FileVisitResult.CONTINUE;
                    }
                    throw e;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Clean up all test directories in the specified root directory
     */
    private void cleanupTestDirectories(Path rootDir) throws Exception {
        File[] files = rootDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && (file.getName().equals("Data") || file.getName().startsWith("Data_"))) {
                    deleteDirectoryIfExists(file.toPath());
                }
            }
        }
    }
    
    /**
     * Clean up all test directories from previous runs
     */
    private void cleanupAllTestDirectories() throws Exception {
        // Clean up test directories in the test root directory
        if (testRootDir != null && Files.exists(testRootDir)) {
            cleanupTestDirectories(testRootDir);
        }
    }
    
    @Test
    @org.junit.jupiter.api.Order(1)
    public void testForceRestartWithExistingDataDirectory() throws Exception {
        // Arrange
        // Ensure Data directory exists before calling handleForceRestart
        if (!Files.exists(testDataDir)) {
            Files.createDirectories(testSubDir);
            Files.write(testFile, "Test content".getBytes());
        }
        assertTrue(Files.exists(testDataDir), "Data directory should exist before test");

        // Create a processing context with force restart enabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(true)
                .build();

        // Count existing backup directories before the test
        int backupCountBefore = countBackupDirectories(testRootDir);

        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();

        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);

        // Assert
        assertFalse(Files.exists(testDataDir), 
            "Original Data directory should no longer exist. Current working dir: " + System.getProperty("user.dir"));

        // Check that exactly one new backup directory was created
        int backupCountAfter = countBackupDirectories(testRootDir);
        assertEquals(backupCountBefore + 1, backupCountAfter, 
            "Exactly one new backup directory should have been created. Before: " + backupCountBefore + ", After: " + backupCountAfter);

        // Find and verify the new backup directory
        boolean backupFound = verifyBackupDirectoryExists(testRootDir);
        assertTrue(backupFound, "A backup directory with the correct test content should have been created");
    }
    
    @Test
    @org.junit.jupiter.api.Order(2)
    public void testForceRestartWithoutExistingDataDirectory() throws Exception {
        // Arrange - explicitly remove the test data directory
        deleteDirectoryIfExists(testDataDir);

        // Ensure Data directory does not exist before calling handleForceRestart
        assertFalse(Files.exists(testDataDir), 
            "Data directory should not exist before test. Current working dir: " + System.getProperty("user.dir"));
        
        // Create a processing context with force restart enabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(true)
                .build();
        
        // Count existing backup directories before the test
        int backupCountBefore = countBackupDirectories(testRootDir);
        
        // Act
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        
        // Use reflection to call the private handleForceRestart method
        java.lang.reflect.Method handleForceRestartMethod = 
            IAFirmSECParserRefactored.class.getDeclaredMethod("handleForceRestart", ProcessingContext.class);
        handleForceRestartMethod.setAccessible(true);
        handleForceRestartMethod.invoke(parser, context);
        
        // Assert
        // Count backup directories after the test
        int backupCountAfter = countBackupDirectories(testRootDir);
        
        // If there was no Data directory initially, no new backup should be created
        assertEquals(backupCountBefore, backupCountAfter, 
            "No new backup directory should have been created when Data directory doesn't exist initially. " +
            "Before: " + backupCountBefore + ", After: " + backupCountAfter + 
            ", Working dir: " + System.getProperty("user.dir"));
        
        // Verify Data directory still doesn't exist
        assertFalse(Files.exists(testDataDir), 
            "Data directory should still not exist after force restart when it didn't exist initially");
    }
    
    @Test
    @org.junit.jupiter.api.Order(3)
    public void testNoForceRestartWhenDisabled() throws Exception {
        // Arrange
        // Create Data directory and contents for this test
        Files.createDirectories(testSubDir);
        Files.write(testFile, "Test content".getBytes());
        assertTrue(Files.exists(testDataDir), "Data directory should exist before test");

        // Create a processing context with force restart disabled
        ProcessingContext context = ProcessingContext.builder()
                .forceRestart(false)
                .build();

        // Count existing backup directories before the test
        int backupCountBefore = countBackupDirectories(testRootDir);

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

        // Check that no new backup directory was created
        int backupCountAfter = countBackupDirectories(testRootDir);
        assertEquals(backupCountBefore, backupCountAfter, 
            "No new backup directory should have been created when force restart is disabled");
    }
    
    /**
     * Helper method to count backup directories in the specified root directory
     */
    private int countBackupDirectories(Path rootDir) {
        File[] files = rootDir.toFile().listFiles();
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
    
    /**
     * Helper method to verify that a backup directory with correct content exists
     */
    private boolean verifyBackupDirectoryExists(Path rootDir) {
        File[] files = rootDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("Data_")) {
                    // Verify the backup contains our test content
                    Path backupTestFile = file.toPath().resolve("test.txt");
                    if (Files.exists(backupTestFile)) {
                        try {
                            String content = new String(Files.readAllBytes(backupTestFile));
                            if ("Test content".equals(content)) {
                                return true;
                            }
                        } catch (IOException e) {
                            // Continue checking other directories
                        }
                    }
                }
            }
        }
        return false;
    }
}
