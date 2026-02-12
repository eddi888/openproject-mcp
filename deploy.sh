mvn clean install
ssh -i ~/.ssh/opnprj-mcp-deploy opnprj-mcp@192.168.77.4 "~/start.sh stop"
scp -i ~/.ssh/opnprj-mcp-deploy ./target/openproject-mcp-0.0.1-SNAPSHOT.jar opnprj-mcp@192.168.77.4:/home/opnprj-mcp/app.jar
scp -i ~/.ssh/opnprj-mcp-deploy ./opnprjmcp.sh opnprj-mcp@192.168.77.4:/home/opnprj-mcp/opnprjmcp.sh
ssh -i ~/.ssh/opnprj-mcp-deploy opnprj-mcp@192.168.77.4 "chmod +x ~/opnprjmcp.sh"
ssh -i ~/.ssh/opnprj-mcp-deploy opnprj-mcp@192.168.77.4 "~/opnprjmcp.sh start"
