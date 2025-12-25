#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Starting SP-AC-BG Application..."

# Start backend
echo "Starting backend..."
cd "$SCRIPT_DIR/backend"
nohup ./mvnw spring-boot:run > "$SCRIPT_DIR/backend.log" 2>&1 &
echo $! > "$SCRIPT_DIR/backend.pid"

# Wait for backend to start
echo "Waiting for backend..."
sleep 10

# Start frontend
echo "Starting frontend..."
cd "$SCRIPT_DIR/frontend"
nohup npm run dev > "$SCRIPT_DIR/frontend.log" 2>&1 &
echo $! > "$SCRIPT_DIR/frontend.pid"

sleep 2

echo ""
echo "Application started!"
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:3000"
echo ""
echo "Logs: backend.log, frontend.log"
echo "Use ./stop.sh to stop the application"
