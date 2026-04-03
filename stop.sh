#!/bin/bash
# Stopper alle tjenester: Backend, Frontend og MySQL

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

cd "$(dirname "$0")"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Stopper dev enviroment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

found=0

# 1. Stop frontend
echo -e "${YELLOW}[1/3] Stop frontend${NC}"
echo -e "${BLUE}  pkill -f 'vite'${NC}"
frontend_pids=$(pgrep -f "vite" 2>/dev/null || true)
if [ -n "$frontend_pids" ]; then
  echo "$frontend_pids" | xargs kill -15 2>/dev/null || true
  sleep 2
  # Force kill if still running
  frontend_pids=$(pgrep -f "vite" 2>/dev/null || true)
  if [ -n "$frontend_pids" ]; then
    echo -e "${BLUE}   force kill (kill -9)${NC}"
    echo "$frontend_pids" | xargs kill -9 2>/dev/null || true
  fi
  echo -e "${GREEN}  Frontend stoppet${NC}"
  found=1
else
  echo -e "${GREEN}   Frontend wasnt running${NC}"
fi

# 2. Stopp backend
echo ""
echo -e "${YELLOW}[2/3] Stop backend${NC}"
echo -e "${BLUE}    pkill -f 'spring-boot:run'${NC}"
backend_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
if [ -n "$backend_pids" ]; then
  echo "$backend_pids" | xargs kill -15 2>/dev/null || true
  sleep 3
  # Force kill hvis fortsatt kjører
  backend_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
  if [ -n "$backend_pids" ]; then
    echo -e "${BLUE}   force kill (kill -9)${NC}"
    echo "$backend_pids" | xargs kill -9 2>/dev/null || true
  fi
  echo -e "${GREEN}  Backend stopped${NC}"
  found=1
else
  echo -e "${GREEN}   Backend wasnt running${NC}"
fi

# 3. Stopp MySQL Docker
echo ""
echo -e "${YELLOW}[3/3] Stop MySQL Docker container${NC}"
echo -e "${BLUE}  docker stop backend-mysql-1${NC}"
if docker ps | grep -q "backend-mysql-1"; then
  docker stop backend-mysql-1 >/dev/null 2>&1 || true
  docker rm backend-mysql-1 >/dev/null 2>&1 || true
  echo -e "${GREEN}  MySQL container stopped${NC}"
  found=1
else
  echo -e "${GREEN}  MySQL wasnt runnign${NC}"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
if [ $found -eq 1 ]; then
  echo -e "${GREEN}  All services stopped  ${NC}"
else
  echo -e "${GREEN}  No services were running${NC}"
fi
echo -e "${GREEN}========================================${NC}"

# Optional: Clean database if --clean flag is passed
if [ "$1" = "--clean" ]; then
  echo ""
  echo -e "${YELLOW}Cleaning database...${NC}"
  docker stop backend-mysql-1 >/dev/null 2>&1 || true
  docker rm backend-mysql-1 >/dev/null 2>&1 || true
  docker volume prune -f >/dev/null 2>&1 || true
  echo -e "${GREEN}Database cleaned - run ./start.sh to start fresh${NC}"
fi
