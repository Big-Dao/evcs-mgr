#!/bin/bash
set -e

IMAGE_DIR="build/images"
mkdir -p $IMAGE_DIR

echo "=== Exporting EVCS Images for K3s ==="

IMAGES=(
    "evcs/auth-service:dev"
    "evcs/config-server:dev"
    "evcs/eureka:dev"
    "evcs/gateway:dev"
    "evcs/monitoring-service:dev"
    "evcs/order-service:dev"
    "evcs/payment-service:dev"
    "evcs/protocol-service:dev"
    "evcs/station-service:dev"
    "evcs/tenant-service:dev"
)

for img in "${IMAGES[@]}"; do
    safe_name=$(echo $img | sed 's/\//_/g' | sed 's/:/_/g')
    tar_path="$IMAGE_DIR/$safe_name.tar"
    
    echo "Exporting $img to $tar_path..."
    # Check if image exists locally
    if docker image inspect $img >/dev/null 2>&1; then
        docker save -o $tar_path $img
    else
        echo "Warning: Image $img not found locally!"
    fi
done

echo ""
echo "=== Image Export Complete ==="
echo "To import these images into K3s (on each node):"
echo "1. Copy the 'build/images' directory to each node (Node A, Node B, Node C)."
echo "2. Run the following command on each node:"
echo "   sudo k3s ctr images import build/images/<image_name>.tar"
echo ""
echo "Example loop for importing all:"
echo "   for f in build/images/*.tar; do sudo k3s ctr images import \"\$f\"; done"
