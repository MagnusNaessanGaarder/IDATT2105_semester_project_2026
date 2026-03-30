#!/bin/bash
# Stopper alle tjenester startet av start.sh
# MySQL i Docker + Backend + Frontend
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

cd "$(dirname "$0")"

found=0

# 1. Stopp frontend (vite)
frontend_pids=$(pgrep -f "vite" 2>/dev/null || true)
if [ -n "$frontend_pids" ]; then
    echo -e "${YELLOW}  Stopper frontend (vite)${NC}"
    echo "$frontend_pids" | xargs kill -15 2>/dev/null || true
    sleep 2
    # Force kill hvis fortsatt kjører
    frontend_pids=$(pgrep -f "vite" 2>/dev/null || true)
    if [ -n "$frontend_pids" ]; then
        echo "$frontend_pids" | xargs kill -9 2>/dev/null || true
    fi
    echo -e "${GREEN} Frontend stoppet${NC}"
    found=1
else
    echo -e "  Frontend ikke kjørende"
fi

# 2. Stopp backend (spring-boot:run)
backend_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
if [ -n "$backend_pids" ]; then
    echo -e "${YELLOW}  Stopper backend (Spring Boot)${NC}"
    echo "$backend_pids" | xargs kill -15 2>/dev/null || true
    sleep 3
    # Force kill hvis fortsatt kjører
    backend_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
    if [ -n "$backend_pids" ]; then
        echo "$backend_pids" | xargs kill -9 2>/dev/null || true
    fi
    echo -e "${GREEN}  Backend stoppet${NC}"
    found=1
else
    echo -e "  Backend ikke kjørende"
fi

# 3. Stopp MySQL Docker
docker_ps=$(docker ps -q --filter "name=ik-mysql-dev" 2>/dev/null || true)
if [ -n "$docker_ps" ]; then
    echo -e "${YELLOW}  Stopper MySQL Docker container${NC}"
    docker compose -f compose-dev.yml down 2>/dev/null || true
    echo -e "${GREEN}   MySQL stoppet${NC}"
    found=1
else
    echo -e "  MySQL container ikke kjørende"
fi

echo ""
if [ $found -eq 1 ]; then
    echo -e "${GREEN}Alle tjenester stoppet ${NC}"
else
    echo -e "${YELLOW} Ingen tjenester var kjørende ${NC}"
fi
