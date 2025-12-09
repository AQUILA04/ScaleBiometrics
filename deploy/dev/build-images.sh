#!/bin/bash
# ScaleBiometrics - Build All Docker Images (Linux/Mac)
# This script builds all Docker images for the ScaleBiometrics platform

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo -e "${BLUE}========================================"
echo "ScaleBiometrics - Building Docker Images"
echo -e "========================================${NC}"
echo ""

# Function to build image
build_image() {
    local name=$1
    local context=$2
    local dockerfile=$3
    
    echo -e "${YELLOW}Building $name...${NC}"
    
    if docker build \
        -t "scalebiometrics/$name:latest" \
        -t "scalebiometrics/$name:$TIMESTAMP" \
        -f "$dockerfile" \
        "$context"; then
        echo -e "${GREEN}✓ $name image built successfully!${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}✗ ERROR: $name build failed!${NC}"
        return 1
    fi
}

# Build images
echo -e "${BLUE}[1/3] Building Keycloak Image...${NC}"
build_image "keycloak" \
    "$PROJECT_ROOT/infrastructure/keycloak" \
    "$PROJECT_ROOT/infrastructure/keycloak/Dockerfile"

echo -e "${BLUE}[2/3] Building Backend API Image...${NC}"
build_image "api" \
    "$PROJECT_ROOT/apps/api" \
    "$PROJECT_ROOT/apps/api/Dockerfile"

echo -e "${BLUE}[3/3] Building Frontend Web Image...${NC}"
build_image "web" \
    "$PROJECT_ROOT/apps/web" \
    "$PROJECT_ROOT/apps/web/Dockerfile"

# List built images
echo -e "${BLUE}[4/4] Listing built images...${NC}"
docker images | grep scalebiometrics || true
echo ""

# Summary
echo -e "${GREEN}========================================"
echo "✓ All images built successfully!"
echo "========================================${NC}"
echo "Timestamp: $TIMESTAMP"
echo ""
echo "Images:"
echo "  - scalebiometrics/keycloak:latest"
echo "  - scalebiometrics/api:latest"
echo "  - scalebiometrics/web:latest"
echo ""
echo -e "${YELLOW}To start the services, run:${NC}"
echo "  cd infrastructure/local"
echo "  docker-compose up -d"
echo ""
