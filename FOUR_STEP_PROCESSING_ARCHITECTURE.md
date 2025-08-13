# Four-Step Processing Architecture

## Overview

The IAPD processing system has been refactored from a 3-step to a 4-step architecture to provide better separation of concerns and improved modularity. The new architecture separates brochure URL extraction into its own processing phase.

## New 4-Step Workflow

### Step 1: Parse XML (XMLProcessingService)
- **Input**: Downloaded XML file from SEC
- **Process**: Extracts firm data from XML
- **Output**: CSV file with firm data (WITHOUT brochure URLs)
- **File**: `IA_FIRM_SEC_DATA_YYYYMMDD.csv`

### Step 2: Extract Brochure URLs (BrochureURLExtractionService) 
- **Input**: CSV file from Step 1 (firm data without brochure URLs)
- **Process**: Makes API calls to FIRM_API to get brochure information
- **Output**: `FilesToDownload.csv` with brochure details
- **File**: `FilesToDownload_YYYYMMDD.csv`

### Step 3: Download Brochures (BrochureDownloadService)
- **Input**: `FilesToDownload.csv` from Step 2
- **Process**: Downloads PDF brochures based on brochure version IDs
- **Output**: Updated `FilesToDownload.csv` with download status and filenames
- **File**: `FilesToDownload_YYYYMMDD_with_downloads.csv`

### Step 4: Process Brochures (BrochureProcessingService)
- **Input**: 
  - Original firm data CSV from Step 1
  - FilesToDownload with download status from Step 3
- **Process**: 
  - Analyzes downloaded PDF brochures
  - Merges firm data with brochure analysis results
- **Output**: Final `IAPD_Data.csv` in the Output directory
- **File**: `IAPD_Data_YYYYMMDD.csv`

## Key Changes Made

### 1. New Processing Phase
- Added `EXTRACTING_BROCHURE_URLS` to `ProcessingPhase` enum
- Integrated into main workflow in `IAFirmSECParserRefactored`

### 2. BrochureURLExtractionService
- **New Service**: Handles brochure URL extraction separately
- **JSON Parsing**: Uses Jackson ObjectMapper for robust JSON parsing
- **Input**: CSV file with firm data (no brochure URLs)
- **Output**: FilesToDownload CSV with brochure details

### 3. Enhanced BrochureDownloadService
- **New Method**: `downloadBrochuresFromFilesToDownload()`
- **Input**: FilesToDownload CSV format
- **Output**: FilesToDownload CSV with download status and filenames
- **Features**: Rate limiting, retry logic, progress tracking

### 4. Enhanced BrochureProcessingService
- **New Method**: `processBrochuresWithMerge()`
- **Data Merging**: Combines firm data with brochure analysis
- **Output Format**: IAPD_Data CSV in Output directory
- **Features**: Memory-efficient processing, comprehensive error handling

## File Flow Diagram

```
XML File (SEC)
     ↓
[Step 1: XMLProcessingService]
     ↓
IA_FIRM_SEC_DATA_YYYYMMDD.csv (firm data, no brochure URLs)
     ↓
[Step 2: BrochureURLExtractionService]
     ↓
FilesToDownload_YYYYMMDD.csv (brochure details)
     ↓
[Step 3: BrochureDownloadService]
     ↓
FilesToDownload_YYYYMMDD_with_downloads.csv (+ download status, filenames)
     ↓
[Step 4: BrochureProcessingService] ← IA_FIRM_SEC_DATA_YYYYMMDD.csv
     ↓
IAPD_Data_YYYYMMDD.csv (final merged output)
```

## Benefits of New Architecture

### 1. Separation of Concerns
- Each step has a single, well-defined responsibility
- Easier to debug and maintain individual components
- Better error isolation and recovery

### 2. Modularity
- Each service can be run independently
- Easier to test individual components
- Better code reusability

### 3. Error Recovery
- If brochure URL extraction fails, XML processing results are preserved
- If download fails, brochure URLs are preserved
- If processing fails, downloads are preserved

### 4. Performance Control
- Better rate limiting control for API requests
- More granular progress tracking
- Ability to resume at different stages

### 5. Data Integrity
- Clear data flow between steps
- Comprehensive logging at each stage
- Better validation and error handling

## Configuration

The new architecture uses existing configuration:

- **Rate Limiting**: `ProcessingContext.xmlRatePerSecond`
- **Index Limits**: `ProcessingContext.indexLimit`
- **File Paths**: Existing `Config` constants
- **Logging**: Existing `ProcessingLogger` infrastructure

## Backward Compatibility

- Existing command-line options remain unchanged
- Configuration files remain compatible
- Output format (IAPD_Data) remains the same
- Logging format remains consistent

## Technical Implementation Details

### JSON Parsing
- Uses Jackson ObjectMapper for robust JSON parsing
- Handles complex nested JSON structures from FIRM_API
- Proper error handling for malformed JSON

### Data Structures
- `FilesToDownload.csv` format:
  ```csv
  firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed
  ```
- `FilesToDownload_with_downloads.csv` format:
  ```csv
  firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed,downloadStatus,fileName
  ```

### Error Handling
- Comprehensive exception handling at each step
- Graceful degradation on individual firm failures
- Detailed error logging with context

### Memory Management
- Efficient CSV processing using streaming
- Memory-conscious data structures
- Proper resource cleanup

## Testing

- Unit tests for each new service
- Integration tests for the complete workflow
- Backward compatibility tests
- Performance regression tests

## Migration Notes

- No changes required for existing users
- New intermediate files will be created in processing directories
- Final output format remains unchanged
- All existing command-line options continue to work
