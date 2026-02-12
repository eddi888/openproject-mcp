#!/bin/bash
#
# Start/Stop/Restart Script für OpenProject MCP-Server
# User: opnprj-mcp
# Port: 8787
#

APP_DIR="/home/opnprj-mcp"
APP_JAR="$APP_DIR/app.jar"
LOG_DIR="$APP_DIR/logs"
LOG_FILE="$LOG_DIR/app.log"
PID_FILE="$APP_DIR/app.pid"
ENV_FILE="$APP_DIR/.env"

start() {
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "MCP-Server läuft bereits (PID: $(cat "$PID_FILE"))"
        return 1
    fi

    if [ ! -f "$APP_JAR" ]; then
        echo "FEHLER: $APP_JAR nicht gefunden"
        return 1
    fi

    # Logs-Verzeichnis erstellen
    mkdir -p "$LOG_DIR"

    # Umgebungsvariablen aus .env laden (falls vorhanden)
    if [ -f "$ENV_FILE" ]; then
        echo "Lade Umgebungsvariablen aus $ENV_FILE"
        export $(grep -v '^#' "$ENV_FILE" | xargs)
    fi

    # Prüfe ob erforderliche Umgebungsvariablen gesetzt sind
    if [ -z "$OPENPROJECT_BASE_URL" ] || [ -z "$OPENPROJECT_API_KEY" ]; then
        echo "WARNUNG: OPENPROJECT_BASE_URL und/oder OPENPROJECT_API_KEY nicht gesetzt"
        echo "Setze Umgebungsvariablen oder erstelle $ENV_FILE mit:"
        echo "  OPENPROJECT_BASE_URL=https://your-instance.openproject.com/"
        echo "  OPENPROJECT_API_KEY=your-api-key"
    fi

    echo "Starte MCP-Server..."
    nohup java -jar "$APP_JAR" --server.port=8787 >> "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "MCP-Server gestartet (PID: $(cat "$PID_FILE"), Log: $LOG_FILE)"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "MCP-Server läuft nicht (kein PID-File)"
        return 1
    fi

    local pid
    pid=$(cat "$PID_FILE")

    if kill -0 "$pid" 2>/dev/null; then
        echo "Stoppe MCP-Server (PID: $pid)..."
        kill "$pid"
        # Warte max 10 Sekunden auf sauberes Beenden
        for i in $(seq 1 10); do
            if ! kill -0 "$pid" 2>/dev/null; then
                break
            fi
            sleep 1
        done
        # Falls noch aktiv: SIGKILL
        if kill -0 "$pid" 2>/dev/null; then
            echo "SIGKILL..."
            kill -9 "$pid"
        fi
        echo "MCP-Server gestoppt"
    else
        echo "Prozess $pid existiert nicht mehr"
    fi

    rm -f "$PID_FILE"
}

status() {
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "MCP-Server läuft (PID: $(cat "$PID_FILE"))"
    else
        echo "MCP-Server läuft nicht"
        rm -f "$PID_FILE"
    fi
}

case "${1:-start}" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; sleep 2; start ;;
    status)  status ;;
    *)       echo "Usage: $0 {start|stop|restart|status}" ;;
esac