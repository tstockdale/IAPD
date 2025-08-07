# IAPD - Investment Adviser Public Disclosure Parser

This is a Java application that parses SEC Investment Adviser Public Disclosure (IAPD) data from XML files and processes PDF brochures to extract relevant information about investment advisory firms.

## Recent Major Updates

### 🚀 ProcessingContext Architecture (Latest)
The application now uses a unified ProcessingContext architecture that provides:
- **Multi-source Configuration**: Command line, config files, and database support
- **Runtime State Tracking**: Real-time processing metrics and progress monitoring
- **Thread-Safe Operations**: Atomic counters and volatile fields for concurrent access
- **Extensible Design**: Easy to add new configuration options and runtime state

### 🔄 Three-Step Processing Architecture (Latest)
Completely refactored processing flow into three distinct, independent steps:
1. **XML Processing**: Extract firm data with brochure URLs
2. **Brochure Downloading**: Download PDF files based on URLs from step 1
3. **Brochure Processing**: Analyze downloaded brochures

### 🔄 Resume Capability (Latest)
Intelligent resume functionality for robust processing:
- **Individual Firm-Level Resume**: Continue from exact interruption point
- **Automatic Failed Download Retry**: Smart retry of failed operations
- **PDF File Validation**: Verify integrity of existing files during resume
- **Enhanced Progress Reporting**: Clear visibility into remaining work

### 📈 Incremental Updates (Latest)
Date-based incremental processing for maximum efficiency:
- **Date-Based Intelligence**: Process only new or updated firms based on filing dates
- **Massive Efficiency Gains**: 95-98% reduction in processing time for daily updates
- **Flexible Processing**: Full incremental or selective step processing
- **Cumulative Results**: Append new results to existing IAPD_Data.csv

## Project Structure

```
IAPD/
├── src/                                    # Java source files
│   ├── IAFirmSECParserRefactored.java     # Main application class (recommended)
│   ├── ProcessingContext.java             # Unified context with config & runtime state
│   ├── ProcessingPhase.java               # Processing phase enumeration
│   ├── ConfigurationManager.java          # Multi-source configuration management
│   ├── CommandLineOptions.java            # Command line argument parsing
│   ├── ResumeStateManager.java            # Resume state management and validation
│   ├── IncrementalUpdateManager.java      # Incremental update logic and date comparison
│   ├── BrochureDownloadService.java       # Dedicated brochure downloading service
│   ├── XMLProcessingService.java          # XML parsing and firm data extraction
│   ├── BrochureProcessingService.java     # Brochure analysis and processing
│   ├── FileDownloadService.java           # File download operations
│   ├── CSVWriterService.java              # CSV output operations
│   ├── BrochureAnalyzer.java              # Brochure content analysis
│   ├── ProcessingLogger.java              # Logging utilities
│   ├── Config.java                        # Configuration constants
│   ├── PatternMatchers.java               # Regex patterns for analysis
│   ├── HttpUtils.java                     # HTTP operations
│   ├── PdfTextExtractor.java              # PDF text extraction
│   ├── RetryUtils.java                    # Retry logic utilities
│   ├── FirmData.java                      # Firm data model
│   ├── FirmDataBuilder.java               # Builder for FirmData
│   ├── BrochureAnalysis.java              # Analysis results model
│   ├── *Exception.java                    # Custom exception classes
│   └── test/                              # Test classes and resources
├── bin/                                   # Compiled class files
├── lib/                                   # External JAR dependencies
├── Data/                                  # Data directories
│   ├── Input/                            # Input files
│   ├── Output/                           # Output files
│   ├── Downloads/                        # Downloaded brochures
│   ├── FirmFiles/                        # XML firm files
│   └── Logs/                             # Log files
├── Documentation/
│   ├── PROCESSING_CONTEXT_ARCHITECTURE.md
│   ├── THREE_STEP_PROCESSING_ARCHITECTURE.md
│   ├── RESUME_CAPABILITY_IMPLEMENTATION.md
│   ├── INCREMENTAL_UPDATES_IMPLEMENTATION.md
│   ├── COMMAND_LINE_IMPLEMENTATION.md
│   └── REFACTORING_SUMMARY.md
├── IAPD.code-workspace                   # VSCode workspace file
└── README.md                             # This file
```

## Dependencies

The project requires the following external JAR files:
- `commons-csv-1.10.0.jar` - Apache Commons CSV library
- `tika-app-1.14.jar` - Apache Tika for PDF parsing
- `log4j-core-2.x.x.jar` - Apache Log4j for logging
- `log4j-api-2.x.x.jar` - Apache Log4j API

## Features

### Core Functionality
- Parse SEC IAPD XML feed files with configurable limits
- Extract firm information and registration details
- Download PDF brochures from SEC website with retry logic
- Analyze brochures for proxy providers, ESG services, class action providers
- Extract email addresses and contact information
- Generate comprehensive CSV reports with analysis results

### Advanced Features
- **Command Line Interface**: Full argument parsing with help and validation
- **Multi-Source Configuration**: Command line, properties files, database support
- **Real-Time Monitoring**: Processing rates, progress tracking, error reporting
- **Three-Step Processing**: Independent XML parsing, downloading, and analysis
- **Robust Error Handling**: Graceful failure recovery and detailed error reporting
- **Flexible Execution**: Can run steps independently or skip steps entirely

## Command Line Usage

### Basic Usage
```bash
# Run with default settings (no limit)
java IAFirmSECParserRefactored

# Set index limit for processing
java IAFirmSECParserRefactored --index-limit 1000
java IAFirmSECParserRefactored -l 500

# Enable verbose logging
java IAFirmSECParserRefactored --verbose
java IAFirmSECParserRefactored -v

# Combine options
java IAFirmSECParserRefactored --index-limit 100 --verbose

# Show help
java IAFirmSECParserRefactored --help
java IAFirmSECParserRefactored -h
```

### Resume Capability Usage
```bash
# Enable resume for both downloads and processing
java IAFirmSECParserRefactored --resume --index-limit 1000
java IAFirmSECParserRefactored -r

# Resume only brochure downloads
java IAFirmSECParserRefactored --resume-downloads --validate-pdfs

# Resume only brochure processing
java IAFirmSECParserRefactored --resume-processing

# Skip PDF validation during resume (faster)
java IAFirmSECParserRefactored --resume --no-validate-pdfs

# Force restart ignoring existing files
java IAFirmSECParserRefactored --force-restart

# Combine resume with other options
java IAFirmSECParserRefactored --resume --verbose --index-limit 500
```

### Incremental Updates Usage
```bash
# Enable incremental updates for both downloads and processing
java IAFirmSECParserRefactored --incremental --baseline-file ./Data/Output/IAPD_Data.csv
java IAFirmSECParserRefactored -i --baseline-file ./Data/Output/IAPD_Data.csv

# Incremental downloads only (skip processing)
java IAFirmSECParserRefactored --incremental-downloads --baseline-file ./Data/Output/IAPD_Data.csv

# Incremental processing only (skip downloads)
java IAFirmSECParserRefactored --incremental-processing --baseline-file ./Data/Output/IAPD_Data.csv

# Use custom baseline file for comparison
java IAFirmSECParserRefactored --incremental --baseline-file ./Data/Archive/IAPD_Data_20240101.csv

# Combine incremental with other options
java IAFirmSECParserRefactored --incremental --baseline-file ./Data/Output/IAPD_Data.csv --verbose --index-limit 1000
```

### Configuration File Support
Create `iapd.properties` in the project directory:
```properties
# IAPD Parser Configuration
index.limit=1000
verbose=true
output.format=CSV
retry.count=3
skip.brochure.download=false
```

## Three-Step Processing Flow

### Step 1: XML Processing
- **Input**: XML file from SEC
- **Output**: `IA_FIRM_SEC_DATA_YYYYMMDD.csv` (firm data + brochure URLs)
- **Purpose**: Extract firm information and brochure URLs without downloading

### Step 2: Brochure Downloading
- **Input**: CSV file from Step 1
- **Output**: `IA_FIRM_SEC_DATA_YYYYMMDD_with_downloads.csv` + PDF files
- **Purpose**: Download brochure PDFs and track download status

### Step 3: Brochure Processing
- **Input**: CSV file from Step 2 + downloaded PDFs
- **Output**: `IAPD_Found.csv` (analysis results)
- **Purpose**: Analyze downloaded brochures and extract information

## Architecture Overview

### ProcessingContext Architecture
The application uses a unified context pattern that provides:

#### Configuration Management
- **Command Line Arguments**: Highest priority configuration source
- **Properties Files**: Secondary configuration source
- **Database Configuration**: Placeholder for future database-driven config
- **Default Values**: Fallback configuration

#### Runtime State Tracking
- **Processing Metrics**: Firms processed, downloads, analysis results
- **Phase Tracking**: Current processing phase with descriptions
- **Performance Monitoring**: Processing rates, elapsed time, error counts
- **Thread-Safe Operations**: Atomic counters for concurrent access

### Service Layer Architecture
- **XMLProcessingService**: XML parsing and firm data extraction
- **BrochureDownloadService**: Bulk brochure downloading with status tracking
- **BrochureProcessingService**: PDF analysis and information extraction
- **FileDownloadService**: Core download operations with retry logic
- **ConfigurationManager**: Multi-source configuration management

### Key Design Patterns
- **Builder Pattern**: Flexible object construction (ProcessingContext, FirmData)
- **Strategy Pattern**: Extensible analysis framework
- **Service Layer Pattern**: Clear separation of business logic
- **Dependency Injection**: Loose coupling between components
- **Command Pattern**: Command line argument processing

## Benefits of Current Architecture

### 1. **Separation of Concerns**
- Each processing step has a single responsibility
- Clear data flow between steps
- Independent error handling per step

### 2. **Flexibility and Control**
- Can run individual steps for debugging
- Skip steps if data already exists
- Configure processing limits and behavior

### 3. **Robust Error Handling**
- Download failures don't stop XML processing
- Graceful continuation with partial failures
- Detailed error reporting and logging

### 4. **Real-Time Monitoring**
- Processing rates and progress tracking
- Phase-based status updates
- Comprehensive logging with configurable verbosity

### 5. **Extensibility**
- Easy to add new configuration options
- Simple to extend with new processing steps
- Pluggable analysis strategies

## Testing

The project includes comprehensive test classes:
- **ProcessingContextTest**: Tests the ProcessingContext architecture
- **ThreeStepProcessingTest**: Demonstrates the three-step processing flow
- **CommandLineTest**: Tests command line argument parsing
- **Unit Tests**: Individual service and component tests

## VSCode Setup

### Required Extensions
- Extension Pack for Java (vscjava.vscode-java-pack)
- Language Support for Java (redhat.java)
- Debugger for Java (vscjava.vscode-java-debug)

### Launch Configurations
- **Launch IAFirmSECParserRefactored**: Run the main application
- **Debug with Arguments**: Run with custom command line arguments
- **Test Runner**: Execute test classes

### Build Tasks
- **compile**: Compile all Java source files
- **run**: Compile and run the application
- **test**: Run test suite
- **clean**: Remove compiled class files

## Performance Considerations

- **Memory**: Configured for large datasets (2GB+ heap space)
- **Rate Limiting**: Built-in delays for API calls to respect SEC limits
- **Thread Safety**: All runtime state updates use atomic operations
- **Resource Management**: Proper cleanup with try-with-resources
- **Batch Processing**: Efficient handling of large firm datasets

## Error Handling Strategy

### Step 1 (XML Processing)
- Individual firm errors logged but processing continues
- Critical XML errors stop the process
- Output contains all successfully processed firms

### Step 2 (Brochure Downloading)
- Download failures recorded in status column
- Processing continues with remaining downloads
- Retry logic for transient failures

### Step 3 (Brochure Processing)
- Analysis errors logged per brochure
- Processing continues with remaining files
- Only successful analyses in final output

## Future Enhancements

### Planned Features
- **Parallel Processing**: Multi-threaded downloading and analysis
- **Distributed Processing**: Scale across multiple machines
- **Web Interface**: Browser-based monitoring and control
- **Advanced Incremental Features**: Time-based incremental processing, selective field updates
- **Enhanced Resume Features**: Time-based resume, selective validation, incremental resume

### Configuration Extensions
- **Database Configuration**: Store settings in database
- **Environment Variables**: Support for containerized deployments
- **Profile-Based Config**: Different settings for dev/test/prod

## Migration Notes

### From Legacy Architecture
- **Breaking Changes**: Method signatures updated to use ProcessingContext
- **New Files**: Additional CSV files with download status
- **Enhanced Logging**: More detailed progress and error reporting
- **Configuration**: New command line options and config file support

### Backward Compatibility
- Legacy classes maintained for reference
- Core functionality preserved
- Output formats remain consistent

## Troubleshooting

### Common Issues
1. **Missing Dependencies**: Ensure all JAR files are in the lib/ directory
2. **Memory Issues**: Increase JVM heap size for large datasets
3. **Network Errors**: Check internet connection and SEC website availability
4. **File Permissions**: Ensure write access to Data/ directories

### Debug Mode
```bash
# Run with verbose logging and debug information
java IAFirmSECParserRefactored --verbose --index-limit 10
```

### Log Files
Check the `Data/Logs/` directory for detailed processing logs and error information.

## Contributing

When contributing to this project:
1. Follow the established architecture patterns
2. Add appropriate unit tests for new functionality
3. Update documentation for any API changes
4. Use the ProcessingContext for all new configuration options
5. Maintain the three-step processing separation

## License

This project is for internal use and analysis of SEC IAPD data.
