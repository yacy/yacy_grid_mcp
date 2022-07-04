echo mcp:
curl -s http://localhost:8100/yacy/grid/mcp/info/threaddump.txt | head -6 | grep Memory

echo crawler:
curl -s http://localhost:8300/yacy/grid/mcp/info/threaddump.txt | head -6 | grep Memory

echo loader:
curl -s http://localhost:8200/yacy/grid/mcp/info/threaddump.txt | head -6 | grep Memory

echo parser:
curl -s http://localhost:8500/yacy/grid/mcp/info/threaddump.txt | head -6 | grep Memory

echo search:
curl -s http://localhost:8800/yacy/grid/mcp/info/threaddump.txt | head -6 | grep Memory

echo searchlab:
curl -s http://localhost:8400/en/api/threaddump.txt | head -6 | grep Memory
