# Brochure URL Extraction Service

## Overview

The `BrochureURLExtractionService` is a new separate process step that extracts brochure URLs from FIRM_API JSON responses and creates a `FilesToDownload` output file. This service separates the brochure URL extraction logic from the main XML processing workflow.

## Purpose

- **Input**: CSV file containing firm data from `XMLProcessingService` (without brochureURL field)
- **Process**: Makes API calls to FIRM_API for each firm to get brochure information
- **Output**: Creates `FilesToDownload.csv` with detailed brochure information

## Key Features

### 1. Robust JSON Parsing with Jackson
- Uses Jackson ObjectMapper for reliable JSON parsing
- Handles complex nested JSON structures gracefully
- Properly processes escaped JSON strings within the API response
- More maintainable and less error-prone than regex-based parsing

### 2. Comprehensive Brochure Information Extraction
- Extracts `firmId`, `firmName` from `basicInformation` section
- Parses `brochuredetails` array to get:
  - `brochureVersionID`
  - `brochureName` 
  - `dateSubmitted`
  - `lastConfirmed` (optional)

### 3. Rate Limiting and Error Handling
- Respects API rate limits using `RateLimiter`
- Comprehensive error logging via `ProcessingLogger`
- Graceful handling of missing or malformed API responses

### 4. CSV Output Generation
- Creates properly formatted CSV with headers
- Sanitizes text values for CSV compatibility
- Generates timestamped output filenames

## Input Format

The service expects a CSV file with the following structure (output from `XMLProcessingService`):

```csv
dateAdded,SECRgmCD,FirmCrdNb,SECMb,Business Name,Legal Name,Street 1,Street 2,City,State,Country,Postal Code,Telephone #,Fax #,Registration Firm Type,Registration State,Registration Date,Filing Date,Filing Version,Total Employees,AUM,Total Accounts
08/13/2025,SEC,123456,789,Test Firm,Test Legal Name,123 Main St,,New York,NY,US,10001,555-1234,555-5678,Corporation,NY,01/01/2020,07/01/2025,1.0,50,1000000,100
```

## Output Format

The service creates a `FilesToDownload.csv` file with this structure:

```csv
firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed
7059,CITIGROUP GLOBAL MARKETS INC.,985284,CITIGROUP GLOBAL MARKETS INC. INVESTMENT ADVISORY PROGRAMS,7/20/2025,3/29/2021
7059,CITIGROUP GLOBAL MARKETS INC.,985058,CITIGROUP GLOBAL MARKETS INC. ALTERNATIVE INVESTMENTS PLATFORM,7/16/2025,
```

## API Response Structure

The service parses FIRM_API responses with this nested JSON structure:

```json
{
  "hits": {
    "total": 1,
    "hits": [{
      "_type": "_doc",
      "_source": {
        "iacontent": "{
          \"basicInformation\": {
            \"firmId\": 7059,
            \"firmName\": \"CITIGROUP GLOBAL MARKETS INC.\",
            ...
          },
          \"brochures\": {
            \"brochuredetails\": [{
              \"brochureVersionID\": 985284,
              \"brochureName\": \"CITIGROUP GLOBAL MARKETS INC. INVESTMENT ADVISORY PROGRAMS\",
              \"dateSubmitted\": \"7/20/2025\",
              \"lastConfirmed\": \"3/29/2021\"
            }]
          }
        }"
      }
    }]
  }
}
```

## Usage Example

```java
// Create processing context
ProcessingContext context = ProcessingContext.builder()
        .indexLimit(100)
        .xmlRatePerSecond(2)
        .verbose(true)
        .build();

// Create the service
BrochureURLExtractionService service = new BrochureURLExtractionService();

// Process the input file
File inputFile = new File("./Data/Input/IA_FIRM_SEC_DATA_20250407.csv");
Path outputFile = service.processFirmDataForBrochures(inputFile, context);

System.out.println("Output created: " + outputFile);
```

## Integration with Existing Workflow

### Before (Single Step):
1. `XMLProcessingService` processes XML → CSV with brochureURL field

### After (Two Steps):
1. `XMLProcessingService` processes XML → CSV without brochureURL field
2. `BrochureURLExtractionService` processes CSV → `FilesToDownload.csv`

## Benefits of Separation

1. **Modularity**: Each service has a single responsibility
2. **Reusability**: Brochure extraction can be run independently
3. **Error Recovery**: If brochure extraction fails, XML processing results are preserved
4. **Rate Limiting**: Better control over API request rates
5. **Debugging**: Easier to troubleshoot specific processing steps

## Files Created

### Core Service
- `src/BrochureURLExtractionService.java` - Main service class
- `src/BrochureURLExtractionExample.java` - Usage example

### Testing
- `src/test/java/BrochureURLExtractionServiceTest.java` - Unit tests

### Documentation
- `BROCHURE_URL_EXTRACTION_SERVICE.md` - This documentation

## Configuration

The service uses the existing configuration system:

- **Rate Limiting**: Configured via `ProcessingContext.xmlRatePerSecond`
- **Index Limits**: Controlled via `ProcessingContext.indexLimit`
- **Logging**: Uses existing `ProcessingLogger` infrastructure
- **File Paths**: Uses `Config.BROCHURE_INPUT_PATH` for output location

## Error Handling

- **Missing API Response**: Logs warning, continues processing
- **Malformed JSON**: Logs error, skips firm, continues processing
- **Network Errors**: Uses existing `RetryUtils` for transient failures
- **File I/O Errors**: Throws `BrochureProcessingException` with detailed message

## Performance Considerations

- **Rate Limiting**: Respects API limits to avoid being blocked
- **Memory Efficient**: Processes firms one at a time, doesn't load all into memory
- **Progress Logging**: Provides periodic progress updates for long-running operations
- **Graceful Shutdown**: Respects index limits for testing/debugging

## Future Enhancements

1. **Parallel Processing**: Could be enhanced to process multiple firms concurrently
2. **Caching**: Could cache API responses to avoid duplicate requests
3. **Resume Capability**: Could support resuming interrupted processing
4. **Filtering**: Could add options to filter brochures by date or type
