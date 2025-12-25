#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Stopping SP-AC-BG Application..."

# Stop backend
if [ -f "$SCRIPT_DIR/backend.pid" ]; then
    BACKEND_PID=$(cat "$SCRIPT_DIR/backend.pid")
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo "Stopping backend (PID: $BACKEND_PID)..."
        kill $BACKEND_PID 2>/dev/null
        sleep 2
        kill -9 $BACKEND_PID 2>/dev/null
    fi
    rm -f "$SCRIPT_DIR/backend.pid"
fi

# Stop frontend
if [ -f "$SCRIPT_DIR/frontend.pid" ]; then
    FRONTEND_PID=$(cat "$SCRIPT_DIR/frontend.pid")
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        echo "Stopping frontend (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID 2>/dev/null
    fi
    rm -f "$SCRIPT_DIR/frontend.pid"
fi

# Kill any remaining processes on ports
fuser -k 8080/tcp 2>/dev/null
fuser -k 3000/tcp 2>/dev/null

echo "Application stopped."
