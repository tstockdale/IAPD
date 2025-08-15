# Maven Migration Complete

## Overview
The IAPD project has been successfully migrated from a custom build system to Maven. All legacy build files and directories have been removed, and the project now uses Maven exclusively for dependency management and building.

## Changes Made

### Removed Files/Directories
- ❌ `build-jar.bat` - Legacy custom build script
- ❌ `build-jar.sh` - Legacy Unix build script  
- ❌ `download-junit-dependencies.bat` - Legacy dependency download script
- ❌ `download-junit-dependencies.sh` - Legacy Unix dependency script
- ❌ `lib/` directory - Manual dependency storage
- ❌ `dist/` directory - Custom distribution output

### Added Files
- ✅ `build.bat` - Simple Maven wrapper script
- ✅ `run.bat` - Application runner script

### Maven Configuration
The project uses a comprehensive `pom.xml` with:
- **Dependencies**: All managed through Maven Central
- **Shade Plugin**: Creates fat JAR with all dependencies included
- **Compiler Plugin**: Java 17 compilation
- **Surefire Plugin**: JUnit 5 test execution
- **Local Install Profile**: Optional deployment to custom directory

## New Build Process

### Building the Application
```bash
# Option 1: Use Maven directly
mvn clean package -DskipTests

# Option 2: Use convenience script
build.bat
```

### Running the Application
```bash
# Option 1: Use Maven-generated JAR directly
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar [arguments]

# Option 2: Use convenience script
run.bat [arguments]
```

### Running Tests
```bash
# Run stable test subset (default)
mvn test

# Run all tests
mvn test -P all-tests
```

### Local Installation
```bash
# Install to default location (C:/Users/stoctom/Work/IAPD)
mvn package -P local-install

# Install to custom location
mvn package -P local-install -Dinstall.dir=C:/path/to/install
```

## Benefits of Maven Migration

### Dependency Management
- **Automatic Downloads**: Dependencies downloaded from Maven Central
- **Version Management**: Centralized version control in `pom.xml`
- **Transitive Dependencies**: Automatic resolution of sub-dependencies
- **Security Updates**: Easy to update to latest secure versions

### Build Process
- **Standardized**: Uses Maven standard directory layout
- **Reproducible**: Same build results across different environments
- **Fat JAR**: Single executable JAR with all dependencies
- **IDE Integration**: Better support in IDEs like IntelliJ, Eclipse, VS Code

### Development Workflow
- **Testing**: Integrated JUnit 5 support with Maven Surefire
- **Compilation**: Automatic compilation with proper classpath
- **Resources**: Automatic resource copying and processing
- **Profiles**: Different build configurations for different needs

## File Structure After Migration

```
IAPD/
├── pom.xml                    # Maven configuration
├── build.bat                  # Build convenience script
├── run.bat                    # Run convenience script
├── src/
│   ├── main/
│   │   ├── java/             # Source code
│   │   └── resources/        # Resources (log4j2.xml)
│   └── test/
│       ├── java/             # Test code
│       └── resources/        # Test resources
├── target/                   # Maven build output
│   ├── classes/              # Compiled classes
│   ├── test-classes/         # Compiled test classes
│   ├── iapd-1.0.0-SNAPSHOT.jar          # Thin JAR
│   └── iapd-1.0.0-SNAPSHOT-all.jar      # Fat JAR (executable)
├── Data/                     # Application data directories
└── docs/                     # Documentation
```

## Backward Compatibility

### Command Line
All existing command-line arguments and options continue to work exactly as before:
```bash
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar --help
java -jar target/iapd-1.0.0-SNAPSHOT-all.jar --incremental-processing
```

### Configuration
All existing configuration files and data directories remain unchanged:
- `Data/Input/`
- `Data/Output/`
- `Data/Downloads/`
- `Data/Logs/`

### Functionality
All application features work identically:
- XML processing
- Brochure URL extraction
- PDF download and analysis
- Incremental processing
- Resume capability

## Development Commands

### Common Maven Commands
```bash
# Clean build
mvn clean

# Compile only
mvn compile

# Run tests
mvn test

# Package (creates JAR)
mvn package

# Install to local repository
mvn install

# Skip tests during build
mvn package -DskipTests

# Run with specific profile
mvn package -P local-install
```

### IDE Integration
- **VS Code**: Use Java Extension Pack, Maven integration automatic
- **IntelliJ IDEA**: Import as Maven project
- **Eclipse**: Import as Existing Maven Project

## Migration Benefits Summary

1. ✅ **Simplified Build**: Single `mvn package` command
2. ✅ **Dependency Management**: Automatic and secure
3. ✅ **IDE Support**: Better integration with development tools
4. ✅ **Standardization**: Follows Maven conventions
5. ✅ **Maintainability**: Easier to update and maintain
6. ✅ **Fat JAR**: Single executable file for deployment
7. ✅ **Testing**: Integrated test framework support
8. ✅ **Documentation**: Standard Maven project structure

The migration is complete and the project is now fully Maven-based while maintaining all existing functionality and backward compatibility.
