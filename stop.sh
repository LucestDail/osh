#!/bin/bash

# Stop Script for OSH Application
# This script stops the running application and monitor process

APP_DIR="/home/ubuntu/osh"
PID_FILE="$APP_DIR/app.pid"
MONITOR_PID_FILE="$APP_DIR/monitor.pid"

echo "=================================="
echo "OSH Application Stop Script"
echo "=================================="
echo ""

# Function to stop a process
stop_process() {
    local PID=$1
    local NAME=$2
    
    if ps -p $PID > /dev/null 2>&1; then
        echo "Stopping $NAME (PID: $PID)..."
        kill $PID
        
        # Wait for process to stop (max 30 seconds)
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null 2>&1; then
                echo "✓ $NAME stopped successfully"
                return 0
            fi
            sleep 1
        done
        
        # Force kill if still running
        if ps -p $PID > /dev/null 2>&1; then
            echo "Force stopping $NAME..."
            kill -9 $PID
            sleep 1
            if ! ps -p $PID > /dev/null 2>&1; then
                echo "✓ $NAME force stopped"
                return 0
            else
                echo "✗ Failed to stop $NAME"
                return 1
            fi
        fi
    else
        echo "✓ $NAME is not running (PID: $PID)"
        return 0
    fi
}

# Stop monitor process first
if [ -f "$MONITOR_PID_FILE" ]; then
    MONITOR_PID=$(cat $MONITOR_PID_FILE)
    stop_process $MONITOR_PID "Monitor process"
    rm -f $MONITOR_PID_FILE
else
    echo "Monitor PID file not found"
fi

# Stop application
if [ -f "$PID_FILE" ]; then
    APP_PID=$(cat $PID_FILE)
    stop_process $APP_PID "Application"
    rm -f $PID_FILE
else
    echo "Application PID file not found"
fi

# Additional cleanup: kill any remaining processes
echo ""
echo "Checking for remaining processes..."
REMAINING_PIDS=$(pgrep -f "osh-.*\.jar" || true)
if [ ! -z "$REMAINING_PIDS" ]; then
    echo "Found remaining processes: $REMAINING_PIDS"
    echo "Killing remaining processes..."
    pkill -f "osh-.*\.jar"
    sleep 2
    echo "✓ Cleanup completed"
else
    echo "✓ No remaining processes found"
fi

echo ""
echo "=================================="
echo "✓ OSH Application stopped"
echo "=================================="

