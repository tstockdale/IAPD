# Command Line Argument Implementation

## Overview
This implementation adds command line argument support to the IAPD Parser application, specifically allowing users to set an index limit for XML processing with `Integer.MAX_VALUE` as the default.

## Files Modified/Created

### 1. CommandLineOptions.java (NEW)
- **Purpose**: Centralized command line argument parsing and validation
- **Features**:
  - Supports `--index-limit` / `-l` for setting processing limit
  - Supports `--verbose` / `-v` for verbose logging (extensible)
  - Supports `--help` / `-h` for usage information
  - Robust error handling with descriptive messages
  - Easy to extend for future command line options

### 2. IAFirmSECParserRefactored.java (MODIFIED)
- **Changes**:
  - Updated `main()` method to parse command line arguments
  - Added error handling for invalid arguments
  - Updated `downloadAndProcessLatestIAPDData()` to accept `CommandLineOptions`
  - Passes index limit to `XMLProcessingService`

### 3. XMLProcessingService.java (MODIFIED)
- **Changes**:
  - Updated `processXMLFile()` to accept `indexLimit` parameter
  - Updated `parseXML()` to use configurable index limit instead of hardcoded `Integer.MAX_VALUE`
  - Added logging to show current index limit setting
  - Added logging when index limit is reached

## Usage Examples

### Default Behavior (No Limit)
```bash
java IAFirmSECParserRefactored
# Uses Integer.MAX_VALUE as default (unlimited processing)
```

### Set Index Limit
```bash
java IAFirmSECParserRefactored --index-limit 1000
java IAFirmSECParserRefactored -l 500
```

### Multiple Options
```bash
java IAFirmSECParserRefactored --index-limit 1000 --verbose
java IAFirmSECParserRefactored -l 500 -v
```

### Help
```bash
java IAFirmSECParserRefactored --help
java IAFirmSECParserRefactored -h
```

## Key Features

### 1. Extensible Design
The `CommandLineOptions` class is designed to easily accommodate future command line arguments:
- Clean separation of parsing logic
- Consistent error handling
- Automatic help generation
- Type-safe parameter handling

### 2. Robust Error Handling
- Invalid arguments show helpful error messages
- Automatic usage display on errors
- Validation of numeric parameters
- Graceful handling of missing values

### 3. Backward Compatibility
- Default behavior unchanged (Integer.MAX_VALUE limit)
- No breaking changes to existing functionality
- Service layer properly updated to handle new parameter

### 4. Logging Integration
- Index limit setting is logged at startup
- Processing stops are logged when limit is reached
- Integrates with existing ProcessingLogger system

## Testing

A test class `CommandLineTest.java` was created to demonstrate functionality:

```bash
# Test default behavior
java -cp src CommandLineTest

# Test with index limit
java -cp src CommandLineTest --index-limit 3

# Test help
java -cp src CommandLineTest --help

# Test error handling
java -cp src CommandLineTest --invalid-option
```

## Future Extensibility

Adding new command line options is straightforward:

1. Add new field to `CommandLineOptions` class
2. Add case in `parseArgs()` switch statement
3. Add getter method
4. Update `printUsage()` method
5. Update `toString()` method

Example for adding a config file option:
```java
// In CommandLineOptions.java
private String configFile = null;

// In parseArgs() switch statement
case "--config":
case "-c":
    if (i + 1 >= args.length) {
        throw new IllegalArgumentException("Missing value for " + arg);
    }
    options.configFile = args[++i];
    break;
```

## Benefits

1. **User Control**: Users can now limit processing for testing or partial runs
2. **Performance**: Allows quick testing with small datasets
3. **Flexibility**: Easy to add more configuration options in the future
4. **Professional**: Standard command line interface with help and error handling
5. **Maintainable**: Clean, well-documented code structure

## Implementation Notes

- Uses `Integer.MAX_VALUE` (2,147,483,647) as the default, maintaining existing behavior
- Command line parsing is done before any heavy initialization
- Error messages are user-friendly and include usage information
- All changes maintain backward compatibility with existing code
