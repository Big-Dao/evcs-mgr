#!/bin/bash
# Docker Build Verification Script
# Tests the optimized Dockerfiles to ensure they build correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Services to test
SERVICES=(
    "evcs-eureka"
    "evcs-gateway"
    "evcs-auth"
)

echo "========================================="
echo "Docker Build Verification Test"
echo "========================================="
echo ""

# Enable BuildKit for better caching
export DOCKER_BUILDKIT=1

# Test each service
for SERVICE in "${SERVICES[@]}"; do
    echo -e "${YELLOW}Testing: ${SERVICE}${NC}"
    echo "Building Docker image for ${SERVICE}..."
    
    if docker build -t "${SERVICE}:test" -f "${SERVICE}/Dockerfile" . 2>&1 | tee "/tmp/${SERVICE}-build.log"; then
        echo -e "${GREEN}✓ ${SERVICE} build successful${NC}"
        
        # Get image size
        IMAGE_SIZE=$(docker images "${SERVICE}:test" --format "{{.Size}}")
        echo -e "${GREEN}  Image size: ${IMAGE_SIZE}${NC}"
        
        # Analyze image layers
        echo "  Analyzing image layers..."
        docker history "${SERVICE}:test" --no-trunc --format "table {{.Size}}\t{{.CreatedBy}}" | head -10
        
    else
        echo -e "${RED}✗ ${SERVICE} build failed${NC}"
        echo "Check logs at: /tmp/${SERVICE}-build.log"
        exit 1
    fi
    
    echo ""
done

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}All builds completed successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"
