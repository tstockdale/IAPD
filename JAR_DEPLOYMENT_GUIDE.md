# JAR Deployment Guide - Log4j2 Configuration

## Overview
This guide explains how to properly deploy the IAPD application as a JAR file with the log4j2.xml configuration included using the standard Maven/Gradle resources directory structure.

## Directory Structure
The project now follows the standard Maven/Gradle directory structure:

```
IAPD/
├── src/
│   ├── main/
│   │   └── resources/
│   │       └── log4j2.xml          # Log4j2 configuration file
│   ├── *.java                      # Java source files
│   └── test/
├── lib/                            # External JAR dependencies
├── build/                          # Build output directory
│   └── classes/                    # Compiled classes + resources
├── dist/                           # Distribution directory
├── build-jar.bat                   # Windows build script
├── build-jar.sh                    # Linux/Unix build script
└── MANIFEST.MF                     # JAR manifest file
```

## Changes Made

### 1. Moved log4j2.xml to Resources Directory
- **Old location**: `src/log4j2.xml`
- **New location**: `src/main/resources/log4j2.xml`

This follows the standard Maven/Gradle convention where resources are placed in `src/main/resources/`.

### 2. Created Build Scripts
Two build scripts have been created for cross-platform compatibility:

#### Windows: `build-jar.bat`
- Compiles Java source files
- Copies resources to build directory
- Creates JAR with proper manifest
- Creates distribution with dependencies

#### Linux/Unix: `build-jar.sh`
- Same functionality as Windows script
- Uses Unix-style commands and paths

### 3. Created MANIFEST.MF
Defines the main class and classpath for the JAR file.

## Building the JAR

### Windows
```cmd
build-jar.bat
```

### Linux/Unix
```bash
chmod +x build-jar.sh
./build-jar.sh
```

## JAR Structure
When built, the JAR file will contain:
```
IAPD.jar
├── META-INF/
│   └── MANIFEST.MF
├── log4j2.xml                      # At root level for Log4j2 to find
├── *.class                         # All compiled Java classes
└── ... (other class files)
```

## Deployment
The build process creates a `dist/` directory with:
```
dist/
├── IAPD.jar                        # Main application JAR
└── lib/                            # Dependencies
    ├── log4j-api-2.20.0.jar
    ├── log4j-core-2.20.0.jar
    ├── commons-csv-1.9.0.jar
    ├── pdfbox-2.0.24.jar
    ├── fontbox-2.0.24.jar
    └── commons-logging-1.2.jar
```

## Running the Application

### Windows
```cmd
java -cp "dist/IAPD.jar;dist/lib/*" IAFirmSECParserRefactored
```

### Linux/Unix
```bash
java -cp "dist/IAPD.jar:dist/lib/*" IAFirmSECParserRefactored
```

## Log4j2 Configuration Location
The log4j2.xml file is now properly included in the JAR at the root level, which is where Log4j2 automatically looks for configuration files. This ensures:

1. **Automatic Discovery**: Log4j2 will automatically find and load the configuration
2. **No External Dependencies**: No need for external configuration files
3. **Portable Deployment**: The JAR is self-contained with its logging configuration

## Verification
To verify that log4j2.xml is properly included in the JAR:

```cmd
jar -tf IAPD.jar | findstr log4j2
```

This should output: `log4j2.xml`

## Benefits of This Approach
1. **Standard Structure**: Follows Maven/Gradle conventions
2. **Self-Contained**: All resources bundled in the JAR
3. **Portable**: No external configuration files needed
4. **Automatic Discovery**: Log4j2 finds configuration automatically
5. **Build Automation**: Repeatable build process with scripts

## Troubleshooting
If logging doesn't work after deployment:

1. Verify log4j2.xml is in the JAR: `jar -tf IAPD.jar | findstr log4j2`
2. Check that Log4j2 JARs are in the classpath
3. Ensure the log directory exists and is writable
4. Check for any Log4j2 initialization errors in the console output

## Migration Notes
- The log4j2.xml file has been moved from `src/` to `src/main/resources/`
- Build process now uses the new directory structure
- No changes needed to the log4j2.xml configuration itself
- The logging initialization code in the application remains the same
