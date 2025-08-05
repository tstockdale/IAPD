# JUnit Testing Setup Guide for IAPD Project

## Overview

This guide provides comprehensive instructions for setting up and running JUnit 5 tests in the IAPD project. The testing infrastructure follows Java best practices with proper directory structure, comprehensive test coverage, and automated test execution.

## Project Structure

```
IAPD/
├── src/
│   ├── main/java/          # Production source code
│   └── test/
│       ├── java/           # JUnit test classes
│       │   ├── ProcessingLoggerTest.java
│       │   ├── BrochureAnalyzerTest.java
│       │   ├── RetryUtilsTest.java
│       │   ├── FirmDataBuilderTest.java
│       │   └── TestRunner.java
│       └── resources/      # Test data and configuration files
│           ├── sample-brochure-content.txt
│           └── sample-firm-data.xml
├── lib/                    # Production dependencies
│   ├── log4j-api-2.20.0.jar
│   └── log4j-core-2.20.0.jar
└── test-lib/              # Test-specific dependencies (to be added)
```

## Required Dependencies

### JUnit 5 (Jupiter) Dependencies
Download and place in `test-lib/` directory:

1. **junit-jupiter-engine-5.10.1.jar**
   - Core JUnit 5 test engine
   - Download: https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.1/

2. **junit-jupiter-api-5.10.1.jar**
   - JUnit 5 API for writing tests
   - Download: https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.1/

3. **junit-jupiter-params-5.10.1.jar**
   - Support for parameterized tests
   - Download: https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-params/5.10.1/

4. **junit-platform-launcher-1.10.1.jar**
   - Platform launcher for programmatic test execution
   - Download: https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.1/

5. **junit-platform-engine-1.10.1.jar**
   - Platform engine API
   - Download: https://repo1.maven.org/maven2/org/junit/platform/junit-platform-engine/1.10.1/

6. **junit-platform-commons-1.10.1.jar**
   - Common utilities for JUnit Platform
   - Download: https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.10.1/

7. **apiguardian-api-1.1.2.jar**
   - API Guardian annotations
   - Download: https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/

8. **opentest4j-1.3.0.jar**
   - Open Test Alliance for the JVM
   - Download: https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.3.0/

### Optional: Mockito (for advanced mocking)
9. **mockito-core-5.7.0.jar**
   - Mocking framework for unit tests
   - Download: https://repo1.maven.org/maven2/org/mockito/mockito-core/5.7.0/

## Compilation Instructions

### Windows Command Line

```batch
# Compile production code
javac -cp "lib/*" -d bin src/*.java

# Compile test code
javac -cp "lib/*;test-lib/*;bin" -d bin src/test/java/*.java

# Run specific test class
java -cp "lib/*;test-lib/*;bin" org.junit.platform.console.ConsoleLauncher --class-path bin --select-class ProcessingLoggerTest

# Run all tests using TestRunner
java -cp "lib/*;test-lib/*;bin" TestRunner
```

### Linux/Mac Command Line

```bash
# Compile production code
javac -cp "lib/*" -d bin src/*.java

# Compile test code
javac -cp "lib/*:test-lib/*:bin" -d bin src/test/java/*.java

# Run specific test class
java -cp "lib/*:test-lib/*:bin" org.junit.platform.console.ConsoleLauncher --class-path bin --select-class ProcessingLoggerTest

# Run all tests using TestRunner
java -cp "lib/*:test-lib/*:bin" TestRunner
```

## Test Classes Overview

### 1. ProcessingLoggerTest
- **Purpose**: Tests logging functionality, counter operations, and thread safety
- **Coverage**: 
  - Counter increment/reset operations
  - Logging methods (info, warning, error)
  - Processing summary generation
  - Thread safety with concurrent operations
  - Edge cases and error handling

### 2. BrochureAnalyzerTest
- **Purpose**: Tests content analysis and pattern matching
- **Coverage**:
  - Proxy provider detection (ISS, Glass Lewis)
  - Class action provider detection (Robbins Geller)
  - ESG provider detection (MSCI, Sustainalytics)
  - Email extraction and sentence analysis
  - No-vote language detection
  - Edge cases and unicode handling

### 3. RetryUtilsTest
- **Purpose**: Tests retry logic and exception handling
- **Coverage**:
  - Basic retry operations
  - Exception classification (transient vs non-transient)
  - Retry delay and timing
  - Thread safety
  - Performance characteristics
  - Edge cases and error conditions

### 4. FirmDataBuilderTest
- **Purpose**: Tests builder pattern implementation
- **Coverage**:
  - Builder method chaining
  - Individual field setting/getting
  - Null and empty value handling
  - Data integrity preservation
  - Builder reuse scenarios
  - Special character and unicode support

## Running Tests

### Option 1: Using TestRunner (Recommended)
```bash
java -cp "lib/*:test-lib/*:bin" TestRunner
```

This will:
- Execute all test classes
- Provide detailed summary with pass/fail counts
- Show execution time
- Display failure details if any tests fail

### Option 2: Using JUnit Platform Console Launcher
```bash
# Run all tests
java -cp "lib/*:test-lib/*:bin" org.junit.platform.console.ConsoleLauncher --class-path bin --scan-classpath

# Run specific test class
java -cp "lib/*:test-lib/*:bin" org.junit.platform.console.ConsoleLauncher --class-path bin --select-class ProcessingLoggerTest

# Run tests with specific tag
java -cp "lib/*:test-lib/*:bin" org.junit.platform.console.ConsoleLauncher --class-path bin --include-tag "integration"
```

### Option 3: IDE Integration
Most modern IDEs (IntelliJ IDEA, Eclipse, VS Code) can automatically detect and run JUnit 5 tests:

1. **IntelliJ IDEA**: Right-click on test class → "Run Tests"
2. **Eclipse**: Right-click on test class → "Run As" → "JUnit Test"
3. **VS Code**: Use Java Test Runner extension

## Test Features

### Advanced JUnit 5 Features Used

1. **@Nested Classes**: Organize related tests into logical groups
2. **@DisplayName**: Provide descriptive test names
3. **@ParameterizedTest**: Test multiple input scenarios
4. **@BeforeEach/@AfterEach**: Setup and cleanup for each test
5. **Assertions**: Comprehensive assertion methods
6. **Exception Testing**: Test expected exceptions and error conditions

### Test Categories

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Edge Case Tests**: Test boundary conditions and error scenarios
4. **Performance Tests**: Test timing and resource usage
5. **Thread Safety Tests**: Test concurrent access scenarios

## Best Practices Implemented

1. **Descriptive Test Names**: Each test clearly describes what it's testing
2. **Arrange-Act-Assert Pattern**: Tests follow clear structure
3. **Test Isolation**: Each test is independent and can run in any order
4. **Comprehensive Coverage**: Tests cover happy path, edge cases, and error conditions
5. **Resource Management**: Proper setup and cleanup in test methods
6. **Readable Assertions**: Clear assertion messages for test failures

## Troubleshooting

### Common Issues

1. **ClassNotFoundException**: Ensure all JUnit 5 JARs are in test-lib directory
2. **NoClassDefFoundError**: Check classpath includes both lib and test-lib directories
3. **Test Not Found**: Verify test classes are compiled to bin directory
4. **Logging Conflicts**: Ensure Log4j configuration doesn't interfere with tests

### Debugging Tips

1. **Verbose Output**: Add `-Djunit.platform.output.capture.stdout=true` for detailed output
2. **Test Discovery**: Use `--details=verbose` with Console Launcher
3. **Classpath Issues**: Print classpath with `System.getProperty("java.class.path")`

## Integration with CI/CD

The test infrastructure is designed to integrate with continuous integration systems:

```bash
# Example CI script
#!/bin/bash
set -e

echo "Compiling production code..."
javac -cp "lib/*" -d bin src/*.java

echo "Compiling test code..."
javac -cp "lib/*:test-lib/*:bin" -d bin src/test/java/*.java

echo "Running tests..."
java -cp "lib/*:test-lib/*:bin" TestRunner

echo "Tests completed successfully!"
```

## Future Enhancements

1. **Test Coverage Reports**: Add JaCoCo for coverage analysis
2. **Performance Benchmarks**: Add JMH for performance testing
3. **Mock Integration**: Add Mockito for external dependency mocking
4. **Test Data Builders**: Create builders for complex test data
5. **Custom Assertions**: Create domain-specific assertion methods

## Conclusion

This JUnit testing infrastructure provides comprehensive test coverage for the IAPD project following Java best practices. The tests are designed to be maintainable, readable, and provide confidence in code quality and reliability.

For questions or issues with the testing setup, refer to the JUnit 5 documentation: https://junit.org/junit5/docs/current/user-guide/
