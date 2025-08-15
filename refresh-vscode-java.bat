@echo off
echo Refreshing VSCode Java Language Server...
echo.

echo Step 1: Cleaning Maven project...
call mvn clean compile test-compile
if %ERRORLEVEL% neq 0 (
    echo Maven build failed!
    pause
    exit /b 1
)

echo.
echo Step 2: VSCode Java workspace refresh instructions:
echo.
echo Please perform the following steps in VSCode:
echo 1. Press Ctrl+Shift+P to open Command Palette
echo 2. Type "Java: Reload Projects" and select it
echo 3. Wait for the reload to complete
echo 4. Press Ctrl+Shift+P again
echo 5. Type "Java: Rebuild Workspace" and select it
echo 6. Wait for the rebuild to complete
echo.
echo If issues persist:
echo 7. Press Ctrl+Shift+P
echo 8. Type "Java: Clean Workspace" and select it
echo 9. Restart VSCode completely
echo.
echo Maven compilation completed successfully!
echo Your project structure is now properly configured.
pause
