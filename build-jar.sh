#!/bin/bash
echo "Building IAPD JAR with proper resource structure..."

# Create build directory
mkdir -p build/classes

# Compile Java source files
echo "Compiling Java source files..."
javac -cp "lib/*" -d build/classes src/*.java src/test/java/*.java

# Copy resources to build directory
echo "Copying resources..."
cp -r src/main/resources/* build/classes/

# Create JAR file with resources included
echo "Creating JAR file..."
cd build/classes
jar -cfm ../../IAPD.jar ../../MANIFEST.MF .
cd ../..

# Copy dependencies to lib folder in distribution
echo "Copying dependencies..."
mkdir -p dist/lib
cp lib/* dist/lib/
cp IAPD.jar dist/

echo "Build complete! JAR file created: IAPD.jar"
echo "Distribution created in: dist/"
echo ""
echo "To run: java -cp \"dist/IAPD.jar:dist/lib/*\" IAFirmSECParserRefactored"
