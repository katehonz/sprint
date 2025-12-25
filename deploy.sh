#!/bin/bash

# SP-AC Deployment Script

set -e

echo "=== SP-AC Deployment ==="

# Check if .env exists
if [ ! -f .env ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo "Please edit .env with your settings before continuing."
    exit 1
fi

# Create external network if not exists
docker network create caddy-net 2>/dev/null || true

# Build and start services
echo "Building and starting services..."
docker compose up -d --build

# Wait for backend to be healthy
echo "Waiting for backend to start..."
sleep 10

# Check status
echo ""
echo "=== Service Status ==="
docker compose ps

echo ""
echo "=== Deployment Complete ==="
echo ""
echo "Don't forget to:"
echo "1. Add the Caddyfile.example configuration to your Caddy server"
echo "2. Reload Caddy: docker exec caddy caddy reload --config /etc/caddy/Caddyfile"
