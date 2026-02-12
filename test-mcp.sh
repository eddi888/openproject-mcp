#!/bin/bash
# MCP Server Test Script (Streamable HTTP Transport)
# Simuliert Claude.AI Calls gegen den MCP Server

BASE_URL="${1:-http://localhost:8787}"
MCP_ENDPOINT="$BASE_URL/mcp"
SESSION_ID=""

echo "=== OpenProject MCP Server Test ==="
echo "Server: $BASE_URL"
echo "Endpoint: $MCP_ENDPOINT"
echo ""

send_request() {
    local name="$1"
    local body="$2"
    echo "--- $name ---"

    local session_header=""
    if [ -n "$SESSION_ID" ]; then
        session_header="-H Mcp-Session-Id:$SESSION_ID"
    fi

    local response
    response=$(curl -s -D /dev/stderr -X POST "$MCP_ENDPOINT" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json, text/event-stream" \
        $session_header \
        -d "$body" 2>/tmp/mcp-headers.txt)

    # Session-ID aus Response-Header extrahieren
    local new_session
    new_session=$(grep -i "^Mcp-Session-Id:" /tmp/mcp-headers.txt | tr -d '\r' | awk '{print $2}')
    if [ -n "$new_session" ]; then
        SESSION_ID="$new_session"
    fi

    # HTTP Status prüfen
    local status
    status=$(grep "^HTTP/" /tmp/mcp-headers.txt | tail -1 | awk '{print $2}')
    if [ "$status" != "200" ]; then
        echo "ERROR: HTTP $status"
        cat /tmp/mcp-headers.txt
        echo ""
        return 1
    fi

    # JSON Response ausgeben
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    echo ""
}

# 1. Initialize
send_request "Initialize" '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
        "protocolVersion": "2025-03-26",
        "capabilities": {},
        "clientInfo": {
            "name": "test-client",
            "version": "1.0.0"
        }
    }
}'

# 2. Initialized Notification
send_request "Initialized" '{
    "jsonrpc": "2.0",
    "method": "notifications/initialized"
}'

# 3. List Tools
send_request "List Tools" '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
}'

# 4. Call listProjects
send_request "Call listProjects" '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
        "name": "listProjects",
        "arguments": {}
    }
}'

# 5. Call listWorkPackages
send_request "Call listWorkPackages" '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
        "name": "listWorkPackages",
        "arguments": {
            "projectId": "demo-project"
        }
    }
}'

# Cleanup
rm -f /tmp/mcp-headers.txt
echo "=== Test abgeschlossen ==="
