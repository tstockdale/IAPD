@echo off
echo ================================================================================
echo DOWNLOADING JUNIT 5 DEPENDENCIES
echo ================================================================================
echo.

REM Set up Java 11 paths
set JAVA_HOME=C:\Users\stoctom\AppData\Local\Programs\Microsoft\jdk-11.0.26.4-hotspot
echo Using Java 11 from: %JAVA_HOME%
echo.

REM Create test-lib directory if it doesn't exist
if not exist "test-lib" (
    echo Creating test-lib directory...
    mkdir test-lib
)

echo Downloading JUnit 5 JAR files...
echo This may take a few minutes depending on your internet connection.
echo.

REM Download JUnit Jupiter API
echo [1/8] Downloading junit-jupiter-api-5.10.1.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.1/junit-jupiter-api-5.10.1.jar' -OutFile 'test-lib/junit-jupiter-api-5.10.1.jar' -ErrorAction Stop; Write-Host 'SUCCESS: junit-jupiter-api downloaded' } catch { Write-Host 'ERROR: Failed to download junit-jupiter-api' -ForegroundColor Red }"

REM Download JUnit Jupiter Engine
echo [2/8] Downloading junit-jupiter-engine-5.10.1.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.1/junit-jupiter-engine-5.10.1.jar' -OutFile 'test-lib/junit-jupiter-engine-5.10.1.jar' -ErrorAction Stop; Write-Host 'SUCCESS: junit-jupiter-engine downloaded' } catch { Write-Host 'ERROR: Failed to download junit-jupiter-engine' -ForegroundColor Red }"

REM Download JUnit Jupiter Params
echo [3/8] Downloading junit-jupiter-params-5.10.1.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-params/5.10.1/junit-jupiter-params-5.10.1.jar' -OutFile 'test-lib/junit-jupiter-params-5.10.1.jar' -ErrorAction Stop; Write-Host 'SUCCESS: junit-jupiter-params downloaded' } catch { Write-Host 'ERROR: Failed to download junit-jupiter-params' -ForegroundColor Red }"

REM Download JUnit Platform Launcher
echo [4/8] Downloading junit-platform-launcher-1.10.1.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.1/junit-platform-launcher-1.10.1.jar' -OutFile 'test-lib/junit-platform-launcher-1.10.1.jar' -ErrorAction Stop; Write-Host 'SUCCESS: junit-platform-launcher downloaded' } catch { Write-Host 'ERROR: Failed to download junit-platform-launcher' -ForegroundColor Red }"

REM Download JUnit Platform Engine
echo [5/8] Downloading junit-platform-engine-1.10.1.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-engine/1.10.1/junit-platform-engine-1.10.1.jar' -OutFile 'test-lib/junit-platform-engine-1.10.1.jar' -ErrorAction Stop; Write-Host 'SUCCESS: junit-platform-engine downloaded' } catch { Write-Host 'ERROR: Failed to download junit-platform-engine' -ForegroundColor Red }"

REM Download JUnit Platform Commons
echo [6/8] Downloading junit-platform-commons-1.10.1.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.10.1/junit-platform-commons-1.10.1.jar' -OutFile 'test-lib/junit-platform-commons-1.10.1.jar' -ErrorAction Stop; Write-Host 'SUCCESS: junit-platform-commons downloaded' } catch { Write-Host 'ERROR: Failed to download junit-platform-commons' -ForegroundColor Red }"

REM Download API Guardian
echo [7/8] Downloading apiguardian-api-1.1.2.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar' -OutFile 'test-lib/apiguardian-api-1.1.2.jar' -ErrorAction Stop; Write-Host 'SUCCESS: apiguardian-api downloaded' } catch { Write-Host 'ERROR: Failed to download apiguardian-api' -ForegroundColor Red }"

REM Download OpenTest4J
echo [8/8] Downloading opentest4j-1.3.0.jar...
powershell -Command "try { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar' -OutFile 'test-lib/opentest4j-1.3.0.jar' -ErrorAction Stop; Write-Host 'SUCCESS: opentest4j downloaded' } catch { Write-Host 'ERROR: Failed to download opentest4j' -ForegroundColor Red }"

echo.
echo ================================================================================
echo DOWNLOAD SUMMARY
echo ================================================================================

REM Check which files were downloaded successfully
echo Checking downloaded files:
if exist "test-lib\junit-jupiter-api-5.10.1.jar" (echo ✓ junit-jupiter-api-5.10.1.jar) else (echo ✗ junit-jupiter-api-5.10.1.jar - MISSING)
if exist "test-lib\junit-jupiter-engine-5.10.1.jar" (echo ✓ junit-jupiter-engine-5.10.1.jar) else (echo ✗ junit-jupiter-engine-5.10.1.jar - MISSING)
if exist "test-lib\junit-jupiter-params-5.10.1.jar" (echo ✓ junit-jupiter-params-5.10.1.jar) else (echo ✗ junit-jupiter-params-5.10.1.jar - MISSING)
if exist "test-lib\junit-platform-launcher-1.10.1.jar" (echo ✓ junit-platform-launcher-1.10.1.jar) else (echo ✗ junit-platform-launcher-1.10.1.jar - MISSING)
if exist "test-lib\junit-platform-engine-1.10.1.jar" (echo ✓ junit-platform-engine-1.10.1.jar) else (echo ✗ junit-platform-engine-1.10.1.jar - MISSING)
if exist "test-lib\junit-platform-commons-1.10.1.jar" (echo ✓ junit-platform-commons-1.10.1.jar) else (echo ✗ junit-platform-commons-1.10.1.jar - MISSING)
if exist "test-lib\apiguardian-api-1.1.2.jar" (echo ✓ apiguardian-api-1.1.2.jar) else (echo ✗ apiguardian-api-1.1.2.jar - MISSING)
if exist "test-lib\opentest4j-1.3.0.jar" (echo ✓ opentest4j-1.3.0.jar) else (echo ✗ opentest4j-1.3.0.jar - MISSING)

echo.
echo All JUnit 5 dependencies have been downloaded to the test-lib directory.
echo You can now run the tests using: run-tests.bat
echo.
echo ================================================================================
pause
