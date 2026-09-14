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

# 🔴 2026-09-14: `stop_process` 는 실패 시 `return 1` 을 제대로 내는데
#    **호출부가 반환값을 버리고 있었다.** 강제 종료까지 실패해 `✗ Failed to stop` 을
#    찍고도 마지막 줄이 무조건 `✓ OSH Application stopped` 였다.
#    안 죽은 JVM 위에 start.sh 를 돌리면 포트 충돌로 이어진다.
#    ⚠️ 같은 커밋에서 start.sh 를 고치며 "형제 중 하나만 빠졌다" 고 적어 놓고
#       **같은 쌍의 다른 쪽(stop.sh)을 빠뜨렸다.** 한 곳 고치면 짝을 함께 본다.
STOP_FAIL=0

# Stop monitor process first
if [ -f "$MONITOR_PID_FILE" ]; then
    MONITOR_PID=$(cat $MONITOR_PID_FILE)
    stop_process $MONITOR_PID "Monitor process" || STOP_FAIL=$((STOP_FAIL + 1))
    rm -f $MONITOR_PID_FILE
else
    echo "Monitor PID file not found"
fi

# Stop application
if [ -f "$PID_FILE" ]; then
    APP_PID=$(cat $PID_FILE)
    stop_process $APP_PID "Application" || STOP_FAIL=$((STOP_FAIL + 1))
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
    # 🔴 pkill 뒤 결과를 보지 않으면 "정리했다" 가 거짓이 된다.
    STILL=$(pgrep -f "osh-.*\.jar" || true)
    if [ -z "$STILL" ]; then
        echo "✓ Cleanup completed"
    else
        echo "✗ Cleanup failed — 아직 살아 있는 PID: $STILL"
        STOP_FAIL=$((STOP_FAIL + 1))
    fi
else
    echo "✓ No remaining processes found"
fi

echo ""
echo "=================================="
if [ "$STOP_FAIL" -gt 0 ]; then
    echo "🔴 OSH 정지 실패 ${STOP_FAIL}건 — 프로세스가 남아 있다"
    echo "   이 상태로 start.sh 를 돌리면 포트 충돌이 난다"
    echo "=================================="
    exit 1
fi
echo "✓ OSH Application stopped"
echo "=================================="

