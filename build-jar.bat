@echo off
echo Building IAPD JAR with proper resource structure...

REM Create build directory
if not exist "build" mkdir build
if not exist "build\classes" mkdir build\classes

REM Compile Java source files
echo Compiling Java source files...
javac -cp "lib/*" -d build/classes src/*.java src/test/java/*.java

REM Copy resources to build directory
echo Copying resources...
if not exist "build\classes" mkdir build\classes
xcopy "src\main\resources\*" "build\classes\" /E /Y

REM Create JAR file with resources included
echo Creating JAR file...
cd build\classes
jar -cfm ..\..\IAPD.jar ..\..\MANIFEST.MF . 
cd ..\..

REM Copy dependencies to lib folder in distribution
echo Copying dependencies...
if not exist "dist" mkdir dist
if not exist "dist\lib" mkdir dist\lib
copy "lib\*" "dist\lib\"
copy "IAPD.jar" "dist\"

echo Build complete! JAR file created: IAPD.jar
echo Distribution created in: dist/
echo.
echo To run: java -cp "dist/IAPD.jar;dist/lib/*" IAFirmSECParserRefactored
pause
