#!/bin/bash
set -e

echo "=== Deploying EVCS to K8s (Internal Test Environment) ==="

# 1. Apply Base (Namespace, Common Config, Secrets)
echo "Applying Base Configuration..."
kubectl apply -f k8s/deployments/00-base.yaml

# 2. Create ConfigMap for Config Repo
echo "Creating Config Repo ConfigMap..."
# Check if config-repo exists
if [ -d "config-repo" ]; then
    kubectl create configmap evcs-config-repo --from-file=config-repo/ -n evcs --dry-run=client -o yaml | kubectl apply -f -
else
    echo "Error: config-repo directory not found!"
    exit 1
fi

# 3. Apply Infrastructure
echo "Applying Infrastructure (DB, Redis, RabbitMQ)..."
kubectl apply -f k8s/deployments/01-infrastructure.yaml

# 4. Apply Discovery & Config
echo "Applying Discovery & Config Server..."
kubectl apply -f k8s/deployments/02-discovery-config.yaml

# 5. Apply Gateway
echo "Applying Gateway..."
kubectl apply -f k8s/deployments/03-gateway.yaml

# 6. Apply Microservices
echo "Applying Microservices..."
kubectl apply -f k8s/deployments/04-services.yaml

echo "=== Deployment Submitted ==="
echo "Check status with: kubectl get pods -n evcs"
