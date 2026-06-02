#!/bin/bash

# Build Script for OSH Application
# Gemini 키는 osh-ai-gateway 가 보관. osh 는 AI_GATEWAY_TOKEN 만 /etc/osh.env 로 주입.

set -e

APP_DIR="${OSH_APP_DIR:-/home/seunghyun/Workspace/osh}"

echo "=================================="
echo "OSH Application Build Script"
echo "=================================="
echo ""

cd "$APP_DIR"

echo "Resetting local changes..."
git reset --hard HEAD
git clean -fd

echo ""
echo "Pulling latest code from git..."
git pull

echo ""
echo "Building application with Maven..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    JAR_FILE=$(find "$APP_DIR/target" -name "osh-*.jar" -type f | head -n 1)
    echo ""
    echo "=================================="
    echo "✓ Build completed successfully!"
    echo "JAR file: $JAR_FILE"
    echo "  Gateway: set AI_GATEWAY_TOKEN + GEMINI_API_URL in /etc/osh.env"
    echo "=================================="
else
    echo ""
    echo "=================================="
    echo "✗ Build failed!"
    echo "=================================="
    exit 1
fi
