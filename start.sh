#!/bin/bash
# MySQL i Docker + Backend/Frontend lokalt for utvikling
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

cd "$(dirname "$0")"

# Stopp eksisterende prosesser
stop_existing() {
    local found=0
    
    # Stopp eksisterende Spring Boot prosesser
    local backend_pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
    if [ -n "$backend_pids" ]; then
        echo -e "${YELLOW}  Stopper eksisterende backend prosesser${NC}"
        echo "$backend_pids" | xargs kill -9 2>/dev/null || true
        found=1
    fi
    
    # Stopp eksisterende Vite/npm prosesser
    local frontend_pids=$(pgrep -f "vite" 2>/dev/null || true)
    if [ -n "$frontend_pids" ]; then
        echo -e "${YELLOW}  Stopper eksisterende frontend prosesser${NC}"
        echo "$frontend_pids" | xargs kill -9 2>/dev/null || true
        found=1
    fi

    # stop mysql docker
    if docker ps | grep -q "ik-mysql-dev"; then 
        docker compose -f compose-dev.yml down -v
    fi

    if [ $found -eq 0 ]; then
        echo -e "${GREEN}  Ingen eksisterende prosesser funnet${NC}"
    fi
    
    

    # Vent litt så prosessene får tid til å stoppe
    sleep 2
}

echo -e "${YELLOW}[0/4] Sjekker for eksisterende prosesser${NC}"
stop_existing
echo ""

# 1. Start MySQL i Docker  
echo -e "${YELLOW}[1/5] Starter MySQL i Docker ${NC}"
if docker ps | grep -q "ik-mysql-dev"; then
    echo -e "${GREEN}  MySQL kjører allerede${NC}"
else
    docker compose -f compose-dev.yml up -d mysql
    
    echo -e "${YELLOW}    Venter på at MySQL blir klar${NC}"
    for i in {1..30}; do
        if docker compose -f compose-dev.yml ps mysql | grep -q "healthy"; then
            echo -e "${GREEN}MySQL er klar!${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done
fi
echo ""

# 2. Sjekk om backend allerede bygget
echo -e "${YELLOW}[2/5] Sjekker backend${NC}"
if [ -f "backend/target/InternalControl-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${GREEN}Backend er allerede bygget${NC}"
else
    echo -e "${YELLOW}    Bygger backend lokalt  ${NC}"
    cd backend
    ./mvnw clean package -DskipTests -q
    cd ..
    echo -e "${GREEN}Backend bygget!${NC}"
fi
echo ""

# 3. Start backend i bakgrunnen
echo -e "${YELLOW}[3/5] Starter backend lokalt${NC}"
cd backend
export $(grep -v '^#' ../.env | xargs)
./mvnw spring-boot:run -DskipTests -Djava.net.preferIPv4Stack=true &
BACKEND_PID=$!
cd ..
echo -e "${GREEN}Backend startet (PID: $BACKEND_PID)${NC}"
echo ""

# 4. Start frontend
echo -e "${YELLOW}[4/5] Starter frontend...${NC}"
cd frontend
npm install --silent 2>/dev/null || true
npm run dev &
FRONTEND_PID=$!
cd ..
echo -e "${GREEN}  Frontend startet (PID: $FRONTEND_PID)${NC}"
echo ""

# 5. Vent på at backend er klar
echo -e "${YELLOW}[5/5] Venter på at backend er klar...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP" || \
       curl -s http://localhost:8080/api/auth/login -X POST 2>/dev/null | grep -q "Bad credentials"; then
        echo -e "${GREEN}  Backend er klar!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done
echo ""

# Vis info
echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}  Alle tjenester er startet (5/5) ${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""
echo "Tjenester:"
echo "  Frontend:    http://localhost:5173"
echo "  Backend:     http://localhost:8080"
echo "  Swagger UI:  http://localhost:8080/swagger-ui"
echo "  MySQL:       localhost:3306 (Docker)"
echo ""
echo "Testbruker:"
echo "  admin@everest-sushi.no / Test1234!"
echo ""
echo "For å stoppe:"
echo "  kill $BACKEND_PID $FRONTEND_PID"
echo "  docker compose -f compose-dev.yml down"
echo ""

# Hold scriptet kjørende
wait $FRONTEND_PID
