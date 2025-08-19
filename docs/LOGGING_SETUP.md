# Log4j Logging Setup for IAPD Project

## Overview
The ProcessingLogger class has been updated to use Apache Log4j 2.20.0 for robust logging with file rotation capabilities.

## Features
- **File Logging**: Logs are written to `./Logs/processing.log`
- **Log Rotation**: Automatic rollover when log files reach 5MB
- **Compressed Archives**: Old log files are compressed as `.gz` files
- **Dual Output**: Logs appear both in console and file
- **Multiple Log Levels**: INFO, WARN, ERROR with proper formatting
- **Exception Logging**: Full stack traces for exceptions

## Files Added/Modified

### New Files:
- `lib/log4j-api-2.20.0.jar` - Log4j API library
- `lib/log4j-core-2.20.0.jar` - Log4j Core implementation
- `src/log4j2.xml` - Log4j configuration file
- `Logs/` - Directory for log files (created automatically)

### Modified Files:
- `src/ProcessingLogger.java` - Updated to use Log4j instead of System.out.println

## Configuration Details

### Log File Settings:
- **Primary Log File**: `./Logs/processing.log`
- **Rollover Size**: 5MB per file
- **Archive Pattern**: `./Logs/processing-%i.log.gz`
- **Max Archives**: 10 files (configurable in log4j2.xml)

### Log Format:
```
yyyy-MM-dd HH:mm:ss [LEVEL] LoggerName - Message
```

Example:
```
2025-08-05 13:08:40 [INFO] ProcessingLogger - Processing started
2025-08-05 13:08:40 [WARN] ProcessingLogger - Brochure URL failure count incremented to: 1
2025-08-05 13:08:40 [ERROR] ProcessingLogger - Processing failed
java.lang.RuntimeException: Test exception
    at LoggingTest.main(LoggingTest.java:16) [src/:?]
```

## Compilation and Execution

### Compiling with Log4j:
```bash
javac -cp "lib/log4j-api-2.20.0.jar;lib/log4j-core-2.20.0.jar" src/ProcessingLogger.java
```

### Running applications that use ProcessingLogger:
```bash
java -cp "lib/log4j-api-2.20.0.jar;lib/log4j-core-2.20.0.jar;src" YourMainClass
```

### Testing the logging system:
```bash
# Compile the test
javac -cp "lib/log4j-api-2.20.0.jar;lib/log4j-core-2.20.0.jar;src" src/LoggingTest.java

# Run the test
java -cp "lib/log4j-api-2.20.0.jar;lib/log4j-core-2.20.0.jar;src" LoggingTest
```

## ProcessingLogger API

### Basic Logging Methods:
```java
ProcessingLogger.logInfo("Information message");
ProcessingLogger.logWarning("Warning message");
ProcessingLogger.logError("Error message", exception); // exception can be null
```

### Counter Methods:
```java
ProcessingLogger.incrementTotalFirmsProcessed();
ProcessingLogger.incrementBrochureUrlFailures();
ProcessingLogger.incrementBrochureDownloadFailures();
ProcessingLogger.incrementFilenameParsingFailures();
```

### Summary and Reset:
```java
ProcessingLogger.printProcessingSummary(); // Logs processing statistics
ProcessingLogger.resetCounters(); // Resets all counters to 0
```

### Getter Methods:
```java
int total = ProcessingLogger.getTotalFirmsProcessed();
int urlFailures = ProcessingLogger.getBrochureUrlFailures();
int downloadFailures = ProcessingLogger.getBrochureDownloadFailures();
int parsingFailures = ProcessingLogger.getFilenameParsingFailures();
```

## Log Rotation Behavior

When the `processing.log` file reaches 5MB:
1. Current log is renamed to `processing-1.log.gz` (compressed)
2. Previous archives are shifted: `processing-1.log.gz` → `processing-2.log.gz`, etc.
3. A new `processing.log` file is created
4. Up to 10 archive files are kept (oldest are deleted)

## Directory Structure
```
IAPD/
├── lib/
│   ├── log4j-api-2.20.0.jar
│   └── log4j-core-2.20.0.jar
├── src/
│   ├── ProcessingLogger.java
│   ├── log4j2.xml
│   └── LoggingTest.java
├── Logs/
│   ├── processing.log
│   ├── processing-1.log.gz
│   └── processing-2.log.gz
└── Data/
```

## Troubleshooting

### Common Issues:
1. **ClassNotFoundException**: Ensure log4j jars are in classpath
2. **Log file not created**: Check that `Logs` directory exists
3. **Permission errors**: Ensure write permissions to `Logs` directory

### Verifying Setup:
Run the LoggingTest class to verify everything is working correctly:
```bash
java -cp "lib/log4j-api-2.20.0.jar;lib/log4j-core-2.20.0.jar;src" LoggingTest
```

This should create log entries both in console and in `Logs/processing.log`.
