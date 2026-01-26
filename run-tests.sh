#!/bin/bash

# ScaleBiometrics Test Runner Script
# This script runs all unit and integration tests

set -e

echo "=========================================="
echo "ScaleBiometrics Test Suite"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven is not installed${NC}"
    echo "Please install Maven 3.9+ and Java 21+"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^"]*' | cut -d. -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${YELLOW}WARNING: Java 21+ is required (found Java $JAVA_VERSION)${NC}"
fi

echo -e "${YELLOW}Step 1: Building project...${NC}"
mvn clean install -DskipTests -q

echo -e "${YELLOW}Step 2: Running unit tests...${NC}"
mvn test -DskipIntegrationTests=false

echo -e "${YELLOW}Step 3: Running integration tests...${NC}"
mvn verify

echo -e "${YELLOW}Step 4: Generating coverage report...${NC}"
mvn jacoco:report

echo ""
echo -e "${GREEN}=========================================="
echo "Test Execution Complete!"
echo "==========================================${NC}"
echo ""

# Print coverage reports location
echo -e "${YELLOW}Coverage Reports:${NC}"
find . -name "index.html" -path "*/jacoco/*" | while read file; do
    echo "  - $file"
done

echo ""
echo -e "${YELLOW}Test Reports:${NC}"
find . -name "TEST-*.xml" | while read file; do
    echo "  - $file"
done

exit 0
