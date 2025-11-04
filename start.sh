#!/bin/bash

# Start Script for OSH Application with Auto-Restart
# This script starts the application with automatic restart capability

set -e

APP_DIR="/home/ubuntu/osh"
JAR_FILE="$APP_DIR/target/osh-0.0.1-SNAPSHOT.jar"
LOG_FILE="$APP_DIR/app.log"
PID_FILE="$APP_DIR/app.pid"
MONITOR_PID_FILE="$APP_DIR/monitor.pid"

# JVM Options
JVM_OPTS="-Xmx512M -Xms512M"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=$APP_DIR/heapdump.hprof"
JVM_OPTS="$JVM_OPTS -Djava.net.preferIPv4Stack=true"

echo "=================================="
echo "OSH Application Start Script"
echo "=================================="
echo ""

# Navigate to application directory
cd $APP_DIR

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run build.sh first"
    exit 1
fi

# Check if application is already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat $PID_FILE)
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "Application is already running (PID: $OLD_PID)"
        echo "Please run stop.sh first"
        exit 1
    else
        echo "Removing stale PID file..."
        rm -f $PID_FILE
    fi
fi

# Check if monitor is already running
if [ -f "$MONITOR_PID_FILE" ]; then
    OLD_MONITOR_PID=$(cat $MONITOR_PID_FILE)
    if ps -p $OLD_MONITOR_PID > /dev/null 2>&1; then
        echo "Monitor is already running (PID: $OLD_MONITOR_PID)"
        echo "Please run stop.sh first"
        exit 1
    else
        echo "Removing stale monitor PID file..."
        rm -f $MONITOR_PID_FILE
    fi
fi

# Function to start application
start_application() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Starting application..."
    nohup java $JVM_OPTS -jar $JAR_FILE > $LOG_FILE 2>&1 &
    local APP_PID=$!
    echo $APP_PID > $PID_FILE
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Application started (PID: $APP_PID)"
}

# Start the application
start_application

# Start monitoring process in background
(
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Monitor started"
    
    while true; do
        sleep 60  # Check every 60 seconds
        
        if [ -f "$PID_FILE" ]; then
            APP_PID=$(cat $PID_FILE)
            
            if ! ps -p $APP_PID > /dev/null 2>&1; then
                echo "$(date '+%Y-%m-%d %H:%M:%S') - Application is down. Restarting..." >> $LOG_FILE
                start_application
                echo "$(date '+%Y-%m-%d %H:%M:%S') - Application restarted" >> $LOG_FILE
            fi
        else
            echo "$(date '+%Y-%m-%d %H:%M:%S') - PID file not found. Restarting..." >> $LOG_FILE
            start_application
            echo "$(date '+%Y-%m-%d %H:%M:%S') - Application restarted" >> $LOG_FILE
        fi
    done
) &

MONITOR_PID=$!
echo $MONITOR_PID > $MONITOR_PID_FILE

echo ""
echo "=================================="
echo "✓ Application started successfully!"
echo "Application PID: $(cat $PID_FILE)"
echo "Monitor PID: $MONITOR_PID"
echo "Log file: $LOG_FILE"
echo ""
echo "Commands:"
echo "  - View logs: tail -f $LOG_FILE"
echo "  - Stop app: ./stop.sh"
echo "=================================="

