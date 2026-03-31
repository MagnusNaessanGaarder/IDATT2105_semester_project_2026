#!/bin/bash
# Start dev environment: MySQL (via Docker) + Backend + Frontend
# Spring Boot automatically starts compose file (defined in application.properties)

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

cd "$(dirname "$0")"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Starting dev environment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 1. Stop to be sure
echo -e "${YELLOW}[1/3] Stopping existing processes${NC}"
./stop.sh

# 2. Start backend (MySQL starts automatically via Docker Compose)
echo -e "${YELLOW}[2/3] Starting backend (MySQL starts automatically)...${NC}"
echo -e "${BLUE}  Running: cd backend && ./mvnw spring-boot:run -DskipTests${NC}"
cd backend
./mvnw spring-boot:run -DskipTests -Dcheckstyle.skip=true &
BACKEND_PID=$!
echo -e "${GREEN}  Backend started (PID: $BACKEND_PID)${NC}"
echo ""

# 3. Start frontend
echo -e "${YELLOW}[3/3] Starting frontend...${NC}"
echo -e "${BLUE}  Running: cd frontend && npm run dev${NC}"
cd ../frontend
npm run dev &
FRONTEND_PID=$!
echo -e "${GREEN}   Frontend started (PID: $FRONTEND_PID)${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Started!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "  Backend:  http://localhost:8080"
echo "  Frontend: http://localhost:5173"
echo ""
echo "  Backend log:  tail -f /tmp/backend.log"
echo "  Frontend log: tail -f /tmp/frontend.log"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all${NC}"
echo ""

# Function to stop when ctrl+c
cleanup() {
  echo ""
  echo -e "${YELLOW}Ctrl+C received, stopping...${NC}"
  ./stop.sh
  exit 0
}

trap cleanup SIGINT SIGTERM

# Wait for processes to complete
wait
