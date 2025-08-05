import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * Test runner for executing all JUnit tests in the project
 * This class provides a programmatic way to run all tests and generate a summary
 */
public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("IAPD PROJECT - JUNIT TEST EXECUTION");
        System.out.println("=".repeat(80));
        
        // Create launcher discovery request
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("")) // Select all tests in default package
                .build();
        
        // Create launcher and summary listener
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        
        // Execute tests
        System.out.println("Executing all JUnit tests...\n");
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        // Print summary
        TestExecutionSummary summary = listener.getSummary();
        printTestSummary(summary);
    }
    
    private static void printTestSummary(TestExecutionSummary summary) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println("=".repeat(80));
        
        System.out.printf("Tests found: %d%n", summary.getTestsFoundCount());
        System.out.printf("Tests started: %d%n", summary.getTestsStartedCount());
        System.out.printf("Tests successful: %d%n", summary.getTestsSucceededCount());
        System.out.printf("Tests skipped: %d%n", summary.getTestsSkippedCount());
        System.out.printf("Tests aborted: %d%n", summary.getTestsAbortedCount());
        System.out.printf("Tests failed: %d%n", summary.getTestsFailedCount());
        
        System.out.printf("Total time: %d ms%n", summary.getTotalTime());
        
        // Print failure details if any
        if (summary.getTestsFailedCount() > 0) {
            System.out.println("\nFAILED TESTS:");
            System.out.println("-".repeat(40));
            summary.getFailures().forEach(failure -> {
                System.out.printf("❌ %s%n", failure.getTestIdentifier().getDisplayName());
                System.out.printf("   Exception: %s%n", failure.getException().getClass().getSimpleName());
                System.out.printf("   Message: %s%n", failure.getException().getMessage());
                System.out.println();
            });
        }
        
        // Print overall result
        System.out.println("=".repeat(80));
        if (summary.getTestsFailedCount() == 0) {
            System.out.println("✅ ALL TESTS PASSED!");
        } else {
            System.out.printf("❌ %d TEST(S) FAILED%n", summary.getTestsFailedCount());
        }
        System.out.println("=".repeat(80));
    }
}
