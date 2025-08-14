# VSCode Jackson Import Resolution Guide

This guide provides solutions for resolving Jackson databind import issues in VSCode for the IAPD Maven project.

## Problem Description

VSCode may show "The import com.fasterxml.jackson.databind cannot be resolved" errors even though:
- Jackson dependencies are properly configured in `pom.xml`
- Maven compilation succeeds without errors
- The project builds successfully from command line

## Root Cause

This is typically a VSCode Java Language Server issue where the IDE doesn't properly recognize Maven dependencies, even though they are correctly configured and available.

## Solutions

### Solution 1: Use VSCode Tasks (Recommended)

We've created custom VSCode tasks to resolve this issue:

1. **Open Command Palette**: `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
2. **Run Task**: Type "Tasks: Run Task"
3. **Select**: "Fix Jackson Import Issues"

This task will:
- Clean the Maven project
- Resolve all dependencies
- Recompile the project
- Force VSCode to refresh its dependency cache

### Solution 2: Manual Maven Commands

Run these commands in the VSCode terminal:

```bash
# Clean and resolve dependencies
mvn clean dependency:resolve

# Compile the project
mvn compile
```

### Solution 3: VSCode Java Extension Commands

1. **Open Command Palette**: `Ctrl+Shift+P`
2. **Run**: "Java: Reload Projects"
3. **If that doesn't work, try**: "Java: Clean Workspace"
4. **Then**: "Java: Reload Projects" again

### Solution 4: Restart Java Language Server

1. **Open Command Palette**: `Ctrl+Shift+P`
2. **Run**: "Java: Restart Language Server"
3. **Wait** for the language server to fully restart
4. **Check** if imports are now resolved

## Verification

After applying any solution, verify the fix by:

1. **Opening** `src/BrochureURLExtractionService.java`
2. **Checking** that these imports show no errors:
   ```java
   import com.fasterxml.jackson.databind.JsonNode;
   import com.fasterxml.jackson.databind.ObjectMapper;
   ```
3. **Confirming** that IntelliSense works for Jackson classes

## Configuration Details

### Maven Dependencies (Already Configured)

The following Jackson dependencies are properly configured in `pom.xml`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.17.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.1</version>
</dependency>
```

### VSCode Settings (Already Configured)

Key settings in `.vscode/settings.json`:

```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.maven.downloadSources": true,
    "java.maven.downloadJavadoc": true,
    "java.import.maven.enabled": true,
    "java.autobuild.enabled": true,
    "java.clean.workspace": false
}
```

## Prevention

To prevent future import resolution issues:

1. **Always use Maven** for dependency management
2. **Run "Maven: Resolve Dependencies"** task after adding new dependencies
3. **Restart Java Language Server** if you notice import issues
4. **Keep VSCode Java extensions updated**

## Troubleshooting

If the issue persists:

1. **Check Java Extension Pack**: Ensure "Extension Pack for Java" is installed and enabled
2. **Verify Java Version**: Ensure you're using Java 17 (as configured in pom.xml)
3. **Check Workspace**: Make sure you're opening the root folder containing `pom.xml`
4. **Clear Cache**: Delete `.vscode` folder and reopen the project (will reset all VSCode settings)

## Available VSCode Tasks

We've configured these tasks for easy access:

- **Maven: Clean and Compile** - Standard build process
- **Maven: Resolve Dependencies** - Download and resolve all dependencies
- **Fix Jackson Import Issues** - Comprehensive fix for import problems
- **Java: Clean Workspace and Reload Projects** - Reset Java workspace

Access these via `Ctrl+Shift+P` → "Tasks: Run Task"

## Success Indicators

The fix is successful when:

- ✅ No red underlines on Jackson imports
- ✅ IntelliSense works for `ObjectMapper` and `JsonNode`
- ✅ No compilation errors in VSCode Problems panel
- ✅ Maven compilation continues to work from command line

## Notes

- The Jackson dependencies are correctly configured and the project compiles successfully with Maven
- This is purely a VSCode IDE integration issue, not a project configuration problem
- The solutions above force VSCode to properly recognize the existing, working Maven configuration
