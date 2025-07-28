# IAPD - Investment Adviser Public Disclosure Parser

This is a Java application that parses SEC Investment Adviser Public Disclosure (IAPD) data from XML files and processes PDF brochures to extract relevant information about investment advisory firms.

## Project Structure

```
IAPD/
├── .vscode/                    # VSCode configuration files
│   ├── settings.json          # Project settings
│   ├── launch.json            # Debug configurations
│   └── tasks.json             # Build and run tasks
├── src/                       # Java source files
│   ├── IAFirmSECParser.java   # Main application class
│   ├── Config.java            # Configuration constants
│   ├── PatternMatchers.java   # Regex patterns for content analysis
│   ├── BrochureAnalysis.java  # Data class for analysis results
│   ├── BrochureAnalyzer.java  # Brochure content analysis logic
│   ├── HttpUtils.java         # HTTP operations and file downloads
│   ├── PdfTextExtractor.java  # PDF text extraction utilities
│   └── IAFirmSECParser_original.java  # Original backup file
├── bin/                       # Compiled class files
├── .classpath                 # Eclipse classpath configuration
├── .project                   # Eclipse project configuration
├── IAPD.code-workspace        # VSCode workspace file
└── README.md                  # This file
```

## Dependencies

The project requires the following external JAR files:
- `commons-csv-1.10.0.jar` - Apache Commons CSV library
- `tika-app-1.14.jar` - Apache Tika for PDF parsing

These JAR files should be located at:
`C:/Users/stoctom/OneDrive - ISS/Work/lib/`

## Features

The application provides functionality to:
- Parse SEC IAPD XML feed files
- Extract firm information and registration details
- Download and process PDF brochures from SEC website
- Search for specific patterns in brochures (proxy providers, ESG services, etc.)
- Extract email addresses and contact information
- Generate CSV reports with analyzed data

## Architecture

The application has been comprehensively refactored into a service-oriented architecture with the following components:

### Refactored Architecture (Recommended)
- **`IAFirmSECParserRefactored`** - Main orchestrator using service layer pattern
- **`XMLProcessingService`** - Handles XML parsing and firm data extraction
- **`BrochureProcessingService`** - Manages brochure analysis and processing
- **`FileDownloadService`** - Centralizes all download operations
- **`CSVWriterService`** - Handles all CSV output operations

### Data Transfer Objects
- **`FirmData`** - Immutable data class for firm information
- **`FirmDataBuilder`** - Builder pattern for constructing FirmData objects
- **`BrochureAnalysis`** - Data class holding analysis results

### Analysis Components
- **`BrochureAnalyzer`** - Improved analyzer using Strategy pattern
  - `ProxyProviderAnalysisStrategy`
  - `ClassActionProviderAnalysisStrategy`
  - `ESGProviderAnalysisStrategy`
  - `EmailAnalysisStrategy`
  - `NoVoteAnalysisStrategy`

### Exception Handling
- **`XMLProcessingException`** - For XML parsing errors
- **`BrochureProcessingException`** - For brochure analysis errors
- **`FileDownloadException`** - For download-related errors

### Configuration and Utilities
- **`Config`** - Centralized configuration constants
- **`PatternMatchers`** - Regex patterns for content analysis
- **`HttpUtils`** - HTTP operations and file downloads
- **`PdfTextExtractor`** - PDF text extraction using Apache Tika

### Legacy Classes (Maintained for Compatibility)
- **`IAFirmSECParser`** - Original monolithic implementation

### Benefits of Refactored Architecture
- **Service Layer Pattern** - Clear separation of business logic into focused services
- **SOLID Principles** - Each class follows Single Responsibility Principle
- **Strategy Pattern** - Extensible analysis framework for adding new analysis types
- **Builder Pattern** - Clean object construction with fluent interface
- **Dependency Injection** - Loose coupling between components
- **Custom Exceptions** - Better error handling and debugging
- **Resource Management** - Proper cleanup with try-with-resources
- **Testability** - Each service can be unit tested independently
- **Maintainability** - Changes isolated to specific service classes
- **Extensibility** - Easy to add new features without modifying existing code

## VSCode Setup

This project is configured for VSCode with the Java Extension Pack. The configuration includes:

### Settings (`.vscode/settings.json`)
- Source path: `src/`
- Output path: `bin/`
- Referenced libraries: External JAR dependencies
- Java compilation and debugging settings

### Launch Configurations (`.vscode/launch.json`)
- **Launch IAFirmSECParser**: Run the application normally
- **Debug IAFirmSECParser**: Run with debugging enabled

### Build Tasks (`.vscode/tasks.json`)
- **compile**: Compile the Java source files
- **run**: Compile and run the application
- **clean**: Remove compiled class files

## Usage

### Using VSCode
1. Open the workspace file: `IAPD.code-workspace`
2. Use Ctrl+Shift+P and run "Java: Compile Workspace"
3. Use F5 to run with debugging or Ctrl+F5 to run without debugging
4. Or use the Terminal menu > Run Task to execute build tasks

### Using Command Line
```bash
# Compile
javac -cp "C:/Users/stoctom/OneDrive - ISS/Work/lib/commons-csv-1.10.0.jar;C:/Users/stoctom/OneDrive - ISS/Work/lib/tika-app-1.14.jar" -d bin src/IAFirmSECParser.java

# Run
java -cp "bin;C:/Users/stoctom/OneDrive - ISS/Work/lib/commons-csv-1.10.0.jar;C:/Users/stoctom/OneDrive - ISS/Work/lib/tika-app-1.14.jar" IAFirmSECParser
```

## Configuration

The application uses several hardcoded paths and constants that may need to be updated:
- Input XML file path
- Output directory paths
- Brochure download locations
- API URLs and endpoints

Check the constants at the top of `IAFirmSECParser.java` for configuration options.

## Required VSCode Extensions

The following extensions are recommended for optimal development experience:
- Extension Pack for Java (vscjava.vscode-java-pack)
- Language Support for Java (redhat.java)
- Debugger for Java (vscjava.vscode-java-debug)
- Test Runner for Java (vscjava.vscode-java-test)
- Maven for Java (vscjava.vscode-maven)

## Notes

- The application includes SSL certificate bypass for HTTPS connections
- Large memory allocation is configured (2GB) for processing large datasets
- The application includes rate limiting (2-second delays) for API calls
- PDF processing uses Apache Tika with specific configuration for inline images
