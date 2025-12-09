# FilesToDownload-Based Processing Implementation

## Overview

The `processBrochuresWithMerge` method has been updated to use the `filesToDownloadWithStatus` file as the primary data source, processing each record and outputting all fields from `filesToDownloadWithStatus` plus the brochure analysis fields.

## New Processing Approach

### Input Data Source
- **Primary Source**: `filesToDownloadWithStatus` CSV file
- **Format**: `FilesToDownload_YYYYMMDD_with_status.csv`
- **Fields**: `firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed,downloadStatus,fileName`

### Processing Logic
1. **Read FilesToDownload**: Process each record in the `filesToDownloadWithStatus` file
2. **Filter Success**: Only process records where `downloadStatus = "SUCCESS"` and `fileName` is not empty
3. **Analyze Brochures**: For each successful download, analyze the PDF brochure
4. **Merge Data**: Combine all FilesToDownload fields with brochure analysis results
5. **Output IAPD_Data**: Write to final output file in the Output directory

### Output Format

The IAPD_Data file now contains:

#### FilesToDownload Fields (from input):
- `dateAdded` (timestamp)
- `firmId`
- `firmName`
- `brochureVersionId`
- `brochureName`
- `dateSubmitted`
- `dateConfirmed`
- `downloadStatus`
- `fileName`

#### Brochure Analysis Fields (generated):
- `proxyProvider`
- `classActionProvider`
- `esgProvider`
- `esgInvestmentLanguage`
- `brochureURL`
- `complianceEmail`
- `proxyEmail`
- `brochureEmail`
- `generalEmail`
- `allEmails`
- `doesNotVote`

## Key Implementation Details

### Method Signature
```java
public void processBrochuresWithMerge(Path firmDataFile, Path filesToDownloadWithStatus, ProcessingContext context)
```

### Processing Flow
```java
// Read FilesToDownload with download status
for (CSVRecord csvRecord : records) {
    String downloadStatus = csvRecord.get("downloadStatus");
    String fileName = csvRecord.get("fileName");
    
    // Process only successfully downloaded brochures
    if ("SUCCESS".equals(downloadStatus) && fileName != null && !fileName.isEmpty()) {
        processSingleBrochureFromFilesToDownload(csvRecord, writer, context);
    }
}
```

### Output Record Structure
```java
// Add timestamp
record.append(Config.getCurrentDateString()).append(",");

// Add all FilesToDownload fields
record.append(csvEscape(csvRecord.get("firmId"))).append(",");
record.append(csvEscape(csvRecord.get("firmName"))).append(",");
// ... all other FilesToDownload fields

// Add brochure analysis fields
record.append(csvEscape(analysis.getProxyProvider().toString())).append(",");
record.append(csvEscape(analysis.getClassActionProvider().toString())).append(",");
// ... all other analysis fields
```

## Benefits of This Approach

### 1. Data Completeness
- **All FilesToDownload Data**: Every field from the FilesToDownload file is preserved
- **Rich Brochure Metadata**: Includes brochure names, submission dates, version IDs
- **Download Status**: Tracks which brochures were successfully downloaded

### 2. Processing Efficiency
- **Direct Processing**: No need to merge data from multiple sources
- **Clear Data Flow**: Single input file with all necessary information
- **Filtered Processing**: Only processes successfully downloaded brochures

### 3. Output Clarity
- **Comprehensive Records**: Each output record contains complete information
- **Traceable**: Easy to trace back to original brochure metadata
- **Self-Contained**: Output file contains all relevant data

### 4. Error Handling
- **Granular Logging**: Logs skipped records with reasons
- **Progress Tracking**: Shows total vs. processed counts
- **Graceful Degradation**: Continues processing if individual brochures fail

## Example Output Record

```csv
dateAdded,firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed,downloadStatus,fileName,proxyProvider,classActionProvider,esgProvider,esgInvestmentLanguage,brochureURL,complianceEmail,proxyEmail,brochureEmail,generalEmail,allEmails,doesNotVote
08/13/2025,7059,CITIGROUP GLOBAL MARKETS INC.,985284,CITIGROUP GLOBAL MARKETS INC. INVESTMENT ADVISORY PROGRAMS,7/20/2025,3/29/2021,SUCCESS,7059_985284.pdf,ISS|Glass Lewis,None,Sustainalytics,ESG factors are considered,https://adviserinfo.sec.gov/firm/brochure/985284,compliance@citi.com,proxy@citi.com,brochure@citi.com,info@citi.com,compliance@citi.com|proxy@citi.com|brochure@citi.com|info@citi.com,No
```

## Processing Statistics

The updated method provides detailed logging:

```
Brochure processing with merge completed.
Total records in FilesToDownload: 1500
Successfully processed brochures: 1200
Output file: ./Data/Output/IAPD_Data_20250813.csv
```

## Integration with 4-Step Workflow

This change completes the 4-step processing architecture:

1. **Parse XML** → Firm data (without brochure URLs)
2. **Extract Brochure URLs** → FilesToDownload
3. **Download Brochures** → FilesToDownload with download status
4. **Process Brochures** → **IAPD_Data with all FilesToDownload fields + analysis**

## Backward Compatibility

- The method signature remains unchanged
- The `firmDataFile` parameter is still accepted but not used in the current implementation
- All existing configuration and logging mechanisms continue to work
- Output file location and naming conventions remain the same
