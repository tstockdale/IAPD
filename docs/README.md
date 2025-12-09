# IAPD - Investment Adviser Public Disclosure Parser

A Java application that downloads and processes SEC Investment Adviser Public Disclosure (IAPD) data, extracting firm information from XML feeds and analyzing PDF brochures to identify proxy providers, ESG services, class action providers, and other advisory firm characteristics.

## Overview

The IAPD Parser automates the collection and analysis of investment adviser data from the SEC's IAPD system through a four-step processing pipeline:

1. **Download & Parse XML** - Downloads latest IAPD XML data from SEC and extracts firm information
2. **Extract Brochure URLs** - Queries SEC FIRM API to collect brochure URLs for each firm  
3. **Download Brochures** - Downloads PDF brochure files from SEC website
4. **Analyze Brochures** - Extracts and analyzes content to identify services, providers, and contact information

## Key Features

### Data Processing
- **Four-Step Pipeline**: Modular processing with clear separation between XML parsing, URL extraction, downloading, and analysis
- **Incremental Updates**: Process only new or updated firms based on filing dates (95-98% time reduction for daily updates)
- **Force Restart**: Option to archive existing data and start fresh
- **Rate Limiting**: Configurable rate limits for API calls and downloads

### Content Analysis
- **Proxy Provider Detection**: Glass Lewis, ISS, Broadridge, ProxyEdge, Egan-Jones, and more
- **ESG Provider Identification**: Sustainalytics, MSCI, ISS ESG, Refinitiv, and others
- **Class Action Detection**: Securities litigation and claims filing services
- **Email Extraction**: Compliance, proxy voting, brochure, and other contact emails
- **Pattern Matching**: Comprehensive regex patterns for accurate identification

### Configuration & Control
- **Command Line Interface**: Simple argument parsing with help and validation
- **Flexible Processing**: Run complete pipeline with configurable options
- **Configurable Limits**: Control number of firms processed for testing
- **Rate Control**: Adjustable API and download rates

### Reliability
- **Robust Error Handling**: Graceful failure recovery with detailed logging
- **Retry Logic**: Automatic retry for transient network failures
- **Rate Limiting**: Respects SEC API rate limits
- **Thread-Safe Operations**: Atomic counters for concurrent access

## Technology Stack

- **Java 21** - Latest LTS Java version
- **Maven 3.x** - Dependency management and build automation
- **Google Guice 7.0** - Dependency injection framework
- **Apache Tika 2.9.2** - PDF text extraction
- **Apache Commons CSV 1.11.0** - CSV file handling
- **Jackson 2.17.2** - JSON parsing for API responses
- **Log4j 2.23.1** - Comprehensive logging
- **JUnit 5.11.0** - Testing framework

## Quick Start

### Prerequisites
- **Java 21** or later (JDK)
- **Maven 3.8+** for building
- **Internet connection** for downloading SEC data

### Build

```bash
# Clone the repository
git clone <repository-url>
cd IAPD

# Build the project (creates executable JAR)
mvn clean package

# Skip tests during build (faster)
mvn clean package -DskipTests
```

### Run

```bash
# Run with default settings (processes all firms)
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar

# Run with index limit (process first 100 firms)
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar --index-limit 100

# Run with verbose logging
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar --verbose --index-limit 50

# Show help
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar --help
```

### Install (Optional)

```bash
# Install to default directory (~/Work/IAPD)
mvn -P local-install -DskipTests package

# Install to custom directory  
mvn -P local-install -Dinstall.dir=/path/to/install -DskipTests package

# Run from install directory
cd ~/Work/IAPD
./run-iapd.bat --help          # Windows
```

## Command Line Options

### Basic Options

| Option | Short | Description | Example |
|--------|-------|-------------|---------|
| `--index-limit <n>` | `-l` | Limit number of firms to process | `--index-limit 100` |
| `--verbose` | `-v` | Enable detailed logging | `--verbose` |
| `--help` | `-h` | Show help message | `--help` |

### Processing Options

| Option | Description | Example |
|--------|-------------|---------|
| `--force-restart` | Archive existing Data directory and start fresh | `--force-restart` |
| `--incremental` | Process only new/updated firms (requires baseline) | `--incremental --baseline-file ./Data/Output/IAPD_Data.csv` |
| `--baseline-file <path>` | Specify baseline file for incremental comparison | `--baseline-file ./Data/Output/IAPD_Data.csv` |

### Rate Limiting Options

| Option | Description | Example |
|--------|-------------|---------|
| `--url-rate <n>` | Limit API calls per second (overrides config) | `--url-rate 2` |
| `--download-rate <n>` | Limit downloads per second (overrides config) | `--download-rate 5` |

### Usage Examples

```bash
# Basic run with limit
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar --index-limit 1000

# Verbose logging for testing
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar -l 50 -v

# Incremental update (daily processing)
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar \
  --incremental \
  --baseline-file ./Data/Output/IAPD_Data.csv \
  --verbose

# Force restart with rate limits
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar \
  --force-restart \
  --index-limit 100 \
  --url-rate 2 \
  --download-rate 5 \
  --verbose

# Production run with custom rates
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar \
  --url-rate 3 \
  --download-rate 10
```

## Four-Step Processing Pipeline

### Step 1: Download & Parse XML

**Purpose**: Download latest IAPD XML data from SEC and extract firm information

**Input**: SEC IAPD XML feed (automatically downloaded)  
**Output**: `IA_FIRM_SEC_DATA_YYYYMMDD.csv`

**Process**:
- Downloads `IA_FIRM_SEC_Feed.zip` from SEC website
- Extracts and parses XML file
- Extracts firm data: name, CRD, SEC number, registration date, address, etc.
- Writes CSV with firm data (without brochure URLs yet)

**Processing Phase**: `DOWNLOADING_XML` → `PARSING_XML`

### Step 2: Extract Brochure URLs

**Purpose**: Query SEC FIRM API to collect brochure URLs for each firm

**Input**: `IA_FIRM_SEC_DATA_YYYYMMDD.csv` from Step 1  
**Output**: `FilesToDownload_YYYYMMDD.csv`

**Process**:
- Reads firm data from Step 1
- For each firm, queries SEC FIRM API: `https://api.adviserinfo.sec.gov/search/firm/{CRD}`
- Extracts brochure metadata: URLs, filing dates, document IDs, versions
- Creates FilesToDownload list with all brochure URLs
- Applies rate limiting for API calls (configurable via `--url-rate`)

**Processing Phase**: `EXTRACTING_BROCHURE_URLS`

### Step 3: Download Brochures

**Purpose**: Download PDF brochure files from SEC website

**Input**: `FilesToDownload_YYYYMMDD.csv` from Step 2  
**Output**: `FilesToDownload_YYYYMMDD_with_status.csv` + PDF files in `Data/Downloads/`

**Process**:
- Reads URLs from FilesToDownload
- Downloads each PDF brochure from SEC
- Validates downloaded files (file size, PDF signature)
- Updates status column (SUCCESS/FAILED)
- Implements retry logic for transient failures
- Applies rate limiting for downloads (configurable via `--download-rate`)

**Processing Phase**: `DOWNLOADING_BROCHURES`

### Step 4: Analyze Brochures

**Purpose**: Extract and analyze content from downloaded brochures

**Input**: 
- `IA_FIRM_SEC_DATA_YYYYMMDD.csv` (firm data)
- `FilesToDownload_YYYYMMDD_with_status.csv` (download status)
- Downloaded PDF files

**Output**: `IAPD_Data.csv` (comprehensive firm data with analysis)

**Process**:
- Extracts text from PDF brochures using Apache Tika
- Analyzes content using pattern matching:
  - **Proxy Providers**: Glass Lewis, ISS, Broadridge, ProxyEdge, Egan-Jones, etc.
  - **ESG Providers**: Sustainalytics, MSCI, ISS ESG, Refinitiv, etc.
  - **Class Action**: Securities litigation and claims filing services
  - **Emails**: Compliance, proxy voting, brochure contacts (Item 17), all emails
  - **No-Vote Language**: Abstention policy detection
- Merges analysis results with firm data
- Generates final comprehensive CSV output

**Processing Phase**: `PROCESSING_BROCHURES` → `GENERATING_OUTPUT`

## Output Files

### Primary Output Files

| File | Description | Location |
|------|-------------|----------|
| `IAPD_Data.csv` | **Final comprehensive data with analysis** | `Data/Output/` |
| `IA_FIRM_SEC_DATA_YYYYMMDD.csv` | Firm data from XML (Step 1) | `Data/Output/` |
| `FilesToDownload_YYYYMMDD.csv` | Brochure URLs to download (Step 2) | `Data/Output/` |
| `FilesToDownload_YYYYMMDD_with_status.csv` | Download status tracking (Step 3) | `Data/Output/` |

### IAPD_Data.csv Structure

The final output file contains 35 columns:

**Firm Information** (from XML):
- dateAdded, SECRgmCD, FirmCrdNb, SECMb
- Business Name, Legal Name
- Street 1, Street 2, City, State, Country, Postal Code
- Telephone #, Fax #
- Registration Firm Type, Registration State, Registration Date
- Filing Date, Filing Version
- Total Employees, AUM, Total Accounts
- BrochureURL

**Brochure Metadata** (from API):
- brochureVersionId, brochureName
- dateSubmitted, dateConfirmed
- File Name

**Analysis Results** (from PDF analysis):
- Proxy Provider
- Class Action Provider
- ESG Provider
- ESG Investment Language
- Email -- Compliance
- Email -- Proxy
- Email -- Brochure
- Email -- Item 17
- Email -- All
- Does Not Vote String

## Incremental Processing

### Overview

Incremental processing allows you to process only new or updated firms since a previous run, dramatically reducing processing time for daily updates (95-98% reduction).

### How It Works

1. Compares current XML data with baseline `IAPD_Data.csv`
2. Identifies firms that are new or have updated filing dates
3. Processes only those firms
4. Appends results to existing `IAPD_Data.csv`

### Usage

```bash
# Enable incremental processing
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar \
  --incremental \
  --baseline-file ./Data/Output/IAPD_Data.csv
```

### Requirements

- Baseline file must exist (previous `IAPD_Data.csv`)
- Baseline file must contain `Filing Date` column
- Firms are compared by CRD number and filing date

### Benefits

- **Time Savings**: Process only 2-5% of firms on daily updates
- **Resource Efficiency**: Reduced API calls and downloads
- **Data Continuity**: Appends to existing data without duplication
- **Automation Ready**: Perfect for scheduled daily/weekly runs

## Recent Improvements (December 2025)

### ✅ Priority 1 Refactoring Complete: Incremental Processing Simplification

We successfully refactored the incremental processing architecture, resulting in:

**Key Improvements**:
- **Eliminated Code Duplication**: Date parsing logic consolidated into single `DateComparator` utility
- **Better Separation of Concerns**: Created focused services with single responsibilities
- **Enhanced Testability**: 52 comprehensive tests with 100% pass rate
- **10% Code Reduction**: Removed 63 lines while improving functionality

**New Components**:
- `DateComparator` - Centralized date parsing and comparison (21 tests)
- `BaselineDataReader` - Unified baseline file reading (16 tests)
- `IncrementalProcessingService` - Orchestrator for incremental operations (15 tests)

**Removed Legacy Code**:
- `IncrementalUpdateManager` - Replaced by new services
- `OutputDataReaderService` - Replaced by `BaselineDataReader`

For complete refactoring details, see **[REFACTORING_PRIORITY_1_COMPLETE.md](REFACTORING_PRIORITY_1_COMPLETE.md)**.

## Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PatternMatchersTest

# Run new refactored incremental processing tests
mvn test -Dtest=DateComparatorTest,BaselineDataReaderTest,IncrementalProcessingServiceTest

# Run with verbose output
mvn test -X

# Skip tests during build
mvn package -DskipTests
```

**Test Coverage**:
- **500+ total test methods** across the codebase
- Unit tests for all services and utilities
- Integration tests for complete workflows
- Pattern matching validation (150+ tests)
- Configuration and CLI argument tests
- **Incremental processing tests** (52 comprehensive tests - recently refactored)
  - Date parsing and comparison (21 tests)
  - Baseline file reading and validation (16 tests)
  - Incremental logic and statistics (15 tests)

**Recent Test Results**:
```
✅ All tests passing (52/52 incremental processing tests)
✅ Zero compilation errors or warnings
✅ 100% backward compatibility maintained
```

For complete testing documentation, see **[TESTING_GUIDE.md](TESTING_GUIDE.md)**.

## Configuration

### Directory Structure

The application automatically creates these directories:

```
Data/
├── Input/          # Reserved for input files
├── Output/         # CSV output files
├── Downloads/      # Downloaded PDF brochures
├── FirmFiles/      # Extracted XML files
└── Logs/           # Application log files
```

### Rate Limiting

Control API and download rates to respect SEC limits:

```bash
# Via command line
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar \
  --url-rate 10 \
  --download-rate 10

# Via properties file (future)
rate.limit.url.per.second=2
rate.limit.download.per.second=5
```

### Logging

Logs are written to `Logs/iapd-YYYYMMDD.log` with:
- INFO level: Progress and completion messages
- WARN level: Non-fatal issues
- ERROR level: Failures and exceptions
- Verbose mode: Additional debug information

## Project Structure

```
IAPD/
├── src/main/java/com/iss/iapd/
│   ├── core/
│   │   ├── IAFirmSECParserRefactored.java    # Main application entry point
│   │   └── ProcessingContext.java            # Central configuration & state
│   ├── config/
│   │   ├── CommandLineOptions.java           # CLI argument parsing
│   │   ├── ConfigurationManager.java         # Multi-source configuration
│   │   ├── ProcessingLogger.java             # Logging utilities
│   │   ├── Config.java                       # Configuration constants
│   │   └── IapdModule.java                   # Guice DI configuration
│   ├── services/
│   │   ├── xml/XMLProcessingService.java     # XML parsing & firm extraction
│   │   ├── brochure/
│   │   │   ├── BrochureURLExtractionService.java  # URL extraction from API
│   │   │   ├── BrochureDownloadService.java       # Bulk PDF downloading
│   │   │   ├── BrochureProcessingService.java     # PDF analysis pipeline
│   │   │   ├── BrochureAnalyzer.java             # Content analysis
│   │   │   └── BrochureProcessingStatistics.java # Analysis metrics
│   │   ├── download/FileDownloadService.java # Core download operations
│   │   ├── csv/                              # CSV services
│   │   │   └── CSVWriterService.java        # CSV file writing operations
│   │   └── incremental/                      # Incremental processing (refactored Dec 2025)
│   │       ├── IncrementalProcessingService.java  # Unified orchestrator
│   │       └── BaselineDataReader.java            # Baseline file reading
│   ├── model/                                # Data models
│   │   ├── FirmData.java                    # Immutable firm data DTO
│   │   ├── FirmDataBuilder.java             # Builder for FirmData
│   │   ├── BrochureAnalysis.java            # Brochure analysis results
│   │   └── ProcessingPhase.java             # Pipeline phase enumeration
│   ├── utils/                                # Utility classes
│   │   ├── DateComparator.java              # Date parsing & comparison (new Dec 2025)
│   │   ├── CsvUtils.java                    # CSV utility functions (new Dec 2025)
│   │   ├── PatternMatchers.java             # Regex patterns for analysis
│   │   ├── HttpUtils.java                   # HTTP operations with retry
│   │   ├── RetryUtils.java                  # Retry logic utilities
│   │   ├── RateLimiter.java                 # Token-bucket rate limiting
│   │   └── PdfTextExtractor.java            # PDF text extraction
│   └── exceptions/                           # Custom exceptions
│       ├── XMLProcessingException.java      # XML processing errors
│       ├── BrochureProcessingException.java # Brochure processing errors
│       └── FileDownloadException.java       # File download errors
├── src/test/java/                            # 500+ test methods
├── docs/                                     # Documentation
├── scripts/                                  # Build & test scripts
├── Data/                                     # Runtime data directories
└── pom.xml                                   # Maven configuration
```

## Architecture

### Design Patterns

**Dependency Injection** (Google Guice):
- Loose coupling between components
- Easy testing with mocked dependencies
- Centralized configuration

**Service Layer Pattern**:
- Clear separation of concerns
- Each service handles one aspect of processing
- Easy to test and maintain

**Builder Pattern**:
- Flexible object construction (FirmData, ProcessingContext)
- Fluent API for readable code

**Strategy Pattern**:
- Pluggable analysis strategies
- Easy to add new detection patterns

### Key Components

**Core**:
- `IAFirmSECParserRefactored` - Main application controller orchestrating four-step pipeline
- `ProcessingContext` - Central configuration and runtime state tracking
- `ProcessingPhase` - Enumeration of processing stages

**Services**:
- `XMLProcessingService` - Parses XML and extracts firm data
- `BrochureURLExtractionService` - Queries SEC API for brochure URLs
- `BrochureDownloadService` - Downloads PDF files with retry logic
- `BrochureProcessingService` - Analyzes PDF content and merges data
- `FileDownloadService` - Core HTTP download operations

**Utilities**:
- `DateComparator` - Date parsing and comparison for incremental processing
- `PatternMatchers` - Regex patterns for content analysis
- `HttpUtils` - HTTP operations with retry logic
- `RetryUtils` - Configurable retry mechanisms
- `RateLimiter` - API and download rate limiting
- `CsvUtils` - CSV file utility functions

## Performance

### Typical Processing Times

| Mode | Firms | Time | Notes |
|------|-------|------|-------|
| Full Run | 15,000 | 8-12 hours | Complete pipeline all firms |
| Incremental | 300 | 20-40 minutes | Daily updates (~2% of firms) |
| Test Run | 100 | 5-10 minutes | With `--index-limit 100` |

### Optimization Features

- **Incremental Processing**: 95-98% time reduction for updates
- **Rate Limiting**: Prevents API throttling
- **Efficient Parsing**: Apache Tika with optimized extraction
- **Retry Logic**: Automatic recovery from transient failures

### Resource Requirements

- **Memory**: 2-4 GB heap recommended
- **Disk**: ~10-20 GB for brochure PDFs
- **Network**: Stable internet connection
- **CPU**: Single-threaded (future: parallel processing)

## Troubleshooting

### Common Issues

**OutOfMemoryError**:
```bash
# Increase heap size
java -Xmx4g -jar target/iapd-1.0.0-SNAPSHOT-all.jar
```

**Network Timeouts**:
- Check internet connection
- Verify SEC website availability
- Reduce rate limits: `--url-rate 1 --download-rate 2`

**PDF Extraction Errors**:
- Some PDFs may be corrupted or password-protected
- Processing continues with remaining files
- Check logs for specific errors

**Build Issues**:
```bash
# Clean rebuild
mvn clean install -U

# Force update dependencies
mvn clean install -U -DskipTests
```

### Debug Mode

```bash
# Enable verbose logging and limit processing
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar \
  --verbose \
  --index-limit 10 \
  2>&1 | tee debug.log
```

### Log Files

Check `Logs/iapd-YYYYMMDD.log` for:
- Detailed processing information
- Error messages with stack traces
- Progress tracking and timing
- API call details (in verbose mode)

## Documentation

### Quick Links
- **[Quick Start Guide](QUICK_START.md)** - Get up and running quickly
- **[Testing Guide](TESTING_GUIDE.md)** - Comprehensive testing documentation
- **[Deployment Guide](JAR_DEPLOYMENT_GUIDE.md)** - Production deployment
- **[Refactoring Complete](REFACTORING_PRIORITY_1_COMPLETE.md)** - Recent improvements (Dec 2025)

### Architecture Documentation
- **[ProcessingContext Architecture](PROCESSING_CONTEXT_ARCHITECTURE.md)** - Core patterns
- **[Command Line Implementation](COMMAND_LINE_IMPLEMENTATION.md)** - CLI details
- **[Incremental Processing](INCREMENTAL_PROCESSING.md)** - Incremental updates

### Feature Documentation
- **[Brochure Processing Statistics](BROCHURE_PROCESSING_STATISTICS_IMPLEMENTATION.md)**
- **[Brochure URL Extraction](BROCHURE_URL_EXTRACTION_SERVICE.md)**
- **[HTTP Utils Enhancement](HTTP_UTILS_ENHANCEMENT_SUMMARY.md)**

### Configuration & Setup
- **[Logging Setup](LOGGING_SETUP.md)** - Log4j2 configuration
- **[Tika Upgrade Notes](TIKA_UPGRADE_NOTES.md)** - Apache Tika information

## License

This project is for internal use and analysis of SEC IAPD data.

---

**Version**: 1.0.0-SNAPSHOT  
**Java Version**: 21  
**Last Updated**: December 8, 2025
