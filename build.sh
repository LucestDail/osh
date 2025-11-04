#!/bin/bash

# Build Script for OSH Application
# This script pulls from git, updates gemini key, and builds the application

set -e

APP_DIR="/home/ubuntu/osh"
KEY_FILE="/home/ubuntu/key.txt"
PROPERTIES_FILE="$APP_DIR/src/main/resources/application.properties"

echo "=================================="
echo "OSH Application Build Script"
echo "=================================="
echo ""

# Navigate to application directory
cd $APP_DIR

# Check if key file exists
if [ ! -f "$KEY_FILE" ]; then
    echo "Error: Key file not found at $KEY_FILE"
    exit 1
fi

# Read gemini API key
GEMINI_KEY=$(cat $KEY_FILE | tr -d '\n\r')
echo "✓ Gemini API key loaded from $KEY_FILE"

# Pull latest code from git
echo ""
echo "Pulling latest code from git..."
git pull

# Update gemini API key in properties file
echo ""
echo "Updating Gemini API key in application.properties..."
sed -i "s|^gemini.api.key=.*|gemini.api.key=$GEMINI_KEY|g" $PROPERTIES_FILE
echo "✓ API key updated"

# Build with Maven
echo ""
echo "Building application with Maven..."
mvn clean install -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    JAR_FILE=$(find $APP_DIR/target -name "osh-*.jar" -type f | head -n 1)
    echo ""
    echo "=================================="
    echo "✓ Build completed successfully!"
    echo "JAR file: $JAR_FILE"
    echo "=================================="
else
    echo ""
    echo "=================================="
    echo "✗ Build failed!"
    echo "=================================="
    exit 1
fi

