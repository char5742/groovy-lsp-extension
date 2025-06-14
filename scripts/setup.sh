#!/bin/bash
# Setup script for Groovy LSP Extension development environment

echo "Setting up Groovy LSP Extension development environment..."

# Check Java version
echo "Checking Java version..."
java -version

# Check Gradle
echo "Checking Gradle..."
./gradlew --version

# Build the project
echo "Building the project..."
./gradlew clean build

echo "Setup complete!"