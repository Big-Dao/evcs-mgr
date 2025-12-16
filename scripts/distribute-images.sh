#!/bin/bash
set -e

# Configuration
IMAGE_DIR="build/images"
REMOTE_USER="andy" # Assuming same user, can be overridden
NODES=(
    "192.168.20.5"  # azlw (Node A)
    "192.168.2.2"   # leading-bull (Node B)
)

echo "=== Distributing Images to Worker Nodes ==="

for node in "${NODES[@]}"; do
    echo "--------------------------------------------------"
    echo "Processing Node: $node"
    
    # 1. Create remote directory
    echo "Creating remote directory..."
    ssh -o StrictHostKeyChecking=no "$REMOTE_USER@$node" "mkdir -p ~/images"
    
    # 2. Copy images
    echo "Copying images (this may take a while)..."
    scp -o StrictHostKeyChecking=no "$IMAGE_DIR"/*.tar "$REMOTE_USER@$node:~/images/"
    
    # 3. Import images
    echo "Importing images into K3s..."
    ssh -o StrictHostKeyChecking=no "$REMOTE_USER@$node" "for f in ~/images/*.tar; do echo \"Importing \$f...\"; sudo k3s ctr images import \"\$f\"; done"
    
    echo "Node $node complete."
done

echo "--------------------------------------------------"
echo "=== Distribution Complete ==="
