#!/usr/bin/env bash

echo "=== Book Review Service Setup ==="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven first."
    exit 1
fi

# Clean and build the project
echo "Building the project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed. Please check the errors above."
    exit 1
fi

echo ""
echo "Build successful!"
echo ""

# Check if Docker is installed
if command -v docker &> /dev/null; then
    echo "Starting services with Docker Compose..."
    docker compose up --build
else
    echo "Docker not found. Starting application with embedded H2 database..."
    java -jar target/*.jar
fi
