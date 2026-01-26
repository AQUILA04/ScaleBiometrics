#!/bin/bash

# ScaleBiometrics Docker Test Runner
# This script runs all tests in Docker containers

set -e

echo "=========================================="
echo "ScaleBiometrics Docker Test Suite"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not installed${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}ERROR: Docker Compose is not installed${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Starting Docker services...${NC}"
docker-compose -f docker-compose.test.yml up -d

echo -e "${YELLOW}Step 2: Waiting for services to be ready...${NC}"
sleep 10

echo -e "${YELLOW}Step 3: Running tests in Docker...${NC}"
docker-compose -f docker-compose.test.yml run --rm test-runner

echo -e "${YELLOW}Step 4: Collecting test results...${NC}"
docker-compose -f docker-compose.test.yml logs test-runner > test-results.log

echo -e "${YELLOW}Step 5: Stopping Docker services...${NC}"
docker-compose -f docker-compose.test.yml down -v

echo ""
echo -e "${GREEN}=========================================="
echo "Docker Test Execution Complete!"
echo "==========================================${NC}"
echo ""
echo -e "${YELLOW}Test Results:${NC}"
echo "  - test-results.log"
echo ""
echo -e "${YELLOW}Coverage Reports:${NC}"
echo "  - ./coverage/index.html"
echo ""

exit 0
