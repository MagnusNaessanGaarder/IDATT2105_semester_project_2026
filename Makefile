.DEFAULT_GOAL := dev

.PHONY: help dev stop restart status logs logs-backend logs-frontend test clean clean-db clean-full install backend-rebuild

UNAME_S := $(shell uname -s 2>/dev/null || echo Unknown)
IS_WINDOWS := 0
ifeq ($(OS),Windows_NT)
IS_WINDOWS := 1
endif
ifneq (,$(filter MINGW% MSYS% CYGWIN%,$(UNAME_S)))
IS_WINDOWS := 1
endif

IS_MACOS := 0
ifeq ($(UNAME_S),Darwin)
IS_MACOS := 1
endif

IS_LINUX := 0
ifeq ($(UNAME_S),Linux)
IS_LINUX := 1
endif

ifeq ($(IS_WINDOWS),1)
JAVA_HOME ?= $(shell powershell.exe -NoProfile -Command "$$java = (Get-Command java -ErrorAction SilentlyContinue).Source; if ($$java) { Split-Path -Parent (Split-Path -Parent $$java) }" 2>NUL)
BACKEND_LOG ?= $${TEMP:-.}/backend.log
FRONTEND_LOG ?= $${TEMP:-.}/frontend.log
else ifeq ($(IS_MACOS),1)
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null || dirname "$$(dirname "$$(command -v java)")")
BACKEND_LOG ?= /tmp/backend.log
FRONTEND_LOG ?= /tmp/frontend.log
else
JAVA_HOME ?= $(shell dirname "$$(dirname "$$(command -v java 2>/dev/null)")")
BACKEND_LOG ?= /tmp/backend.log
FRONTEND_LOG ?= /tmp/frontend.log
endif

export JAVA_HOME

DOCKER_COMPOSE := $(shell if docker compose version >/dev/null 2>&1; then echo "docker compose"; elif command -v docker-compose >/dev/null 2>&1; then echo "docker-compose"; else echo "docker compose"; fi)

help:
	@echo ""
	@echo "  make dev         - Start all services"
	@echo "  make stop        - Stop all services"
	@echo "  make restart     - Restart all services"
	@echo "  make status      - Check service status"
	@echo "  make logs        - Show all logs"
	@echo "  make install     - Install dependencies"
	@echo "  make test        - Run tests"
	@echo "  make clean       - Clean build files"
	@echo "  make clean-db    - Reset database"
	@echo "  make clean-full  - Full cleanup"
	@echo ""
	@echo "  OS detected: $(UNAME_S)"
	@echo "  JAVA_HOME: $(JAVA_HOME)"
	@echo ""

dev:
	@echo "========================================"
	@echo "  Starting dev environment"
	@echo "========================================"
	@$(MAKE) stop 2>/dev/null || true
	@if [ ! -x frontend/node_modules/.bin/vite ] && [ ! -f frontend/node_modules/.bin/vite.cmd ]; then \
		echo "Installing frontend dependencies..."; \
		cd frontend && npm install; \
	fi
	@echo ""
	@echo "[1/3] Starting MySQL (Docker)..."
	@$(DOCKER_COMPOSE) -f compose-dev.yaml up -d mysql 2>/dev/null || docker start backend-mysql-1 2>/dev/null || true
	@sleep 5
	@echo "  MySQL started"
	@echo ""
	@echo "[2/3] Starting backend..."
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$log = $$env:TEMP + '\backend.log'; Start-Process -FilePath 'cmd.exe' -WorkingDirectory '$(CURDIR)\backend' -ArgumentList '/c','mvnw.cmd spring-boot:run -DskipTests -Dcheckstyle.skip=true -Dspring-boot.run.jvmArguments=""-Dspring.docker.compose.enabled=false"" > ""' + $$log + '"" 2>&1' -WindowStyle Hidden"
else
	@(cd backend && nohup ./mvnw spring-boot:run -DskipTests -Dcheckstyle.skip=true -Dspring-boot.run.jvmArguments="-Dspring.docker.compose.enabled=false" > $(BACKEND_LOG) 2>&1 &)
endif
	@sleep 15
	@echo "  Backend started"
	@echo ""
	@echo "[3/3] Starting frontend..."
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$log = $$env:TEMP + '\frontend.log'; Start-Process -FilePath 'cmd.exe' -WorkingDirectory '$(CURDIR)\frontend' -ArgumentList '/c','npm run dev -- --host 127.0.0.1 > ""' + $$log + '"" 2>&1' -WindowStyle Hidden"
else
	@(cd frontend && nohup npm run dev -- --host 127.0.0.1 > $(FRONTEND_LOG) 2>&1 &)
endif
	@sleep 3
	@echo "  Frontend started"
	@echo ""
	@echo "========================================"
	@echo "  Started!"
	@echo "========================================"
	@echo ""
	@echo "  Backend:  http://localhost:8080"
	@echo "  Frontend: http://localhost:5173"
	@echo "  Swagger:  http://localhost:8080/swagger-ui/index.html"
	@echo ""
	@echo "  Logs:    make logs-backend"
	@echo "           make logs-frontend"
	@echo "  Stop:    make stop"
	@echo ""

stop:
	@echo "========================================"
	@echo "  Stopping dev environment"
	@echo "========================================"
	@echo ""
	@echo "Stopping services..."
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$ports = @(8080, 5173); foreach ($$port in $$ports) { $$pids = Get-NetTCPConnection -LocalPort $$port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique; foreach ($$pid in $$pids) { Stop-Process -Id $$pid -Force -ErrorAction SilentlyContinue } }"
	@powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $$_.CommandLine -match 'InternalControl|spring-boot|mvnw|vite' } | ForEach-Object { Stop-Process -Id $$_.ProcessId -Force -ErrorAction SilentlyContinue }"
else
	@# Kill by port
	@-lsof -ti:8080 2>/dev/null | xargs -r kill -9 2>/dev/null || true
	@-lsof -ti:5173 2>/dev/null | xargs -r kill -9 2>/dev/null || true
	@# Kill all Java processes related to this project
	@-jps -l 2>/dev/null | grep -E "(InternalControl|spring-boot)" | awk '{print $$1}' | xargs -r kill -9 2>/dev/null || true
	@-pgrep -f "InternalControl" | xargs -r kill -9 2>/dev/null || true
	@-pgrep -f "spring-boot" | xargs -r kill -9 2>/dev/null || true
	@-pgrep -f "mvnw" | xargs -r kill -9 2>/dev/null || true
	@-pgrep -f "vite" | xargs -r kill -9 2>/dev/null || true
	@# Kill any hanging Java MySQL connections
	@-lsof -i:3306 2>/dev/null | grep java | awk '{print $$2}' | sort -u | xargs -r kill -9 2>/dev/null || true
endif
	@# Stop Docker
	@-docker stop backend-mysql-1 2>/dev/null || true
	@-docker rm backend-mysql-1 2>/dev/null || true
	@-$(DOCKER_COMPOSE) -f compose-dev.yaml down -v
	@echo "  All services stopped"
	@echo ""

backend-rebuild:
	@echo "Cleaning and rebuilding backend..."
	@cd backend && ./mvnw clean -DskipTests compile
	@echo "  Backend rebuild completed"

restart: stop backend-rebuild dev

status:
	@echo ""
	@echo "Service Status:"
	@echo ""
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$backend = if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { 'Running' } else { 'Stopped' }; $$frontend = if (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue) { 'Running' } else { 'Stopped' }; $$mysql = if (Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue) { 'Running' } else { 'Stopped' }; Write-Host ('Backend (port 8080):  ' + $$backend); Write-Host ('Frontend (port 5173): ' + $$frontend); Write-Host ('MySQL (port 3306):    ' + $$mysql)"
else
	@printf "Backend (port 8080):  "
	@if lsof -ti:8080 > /dev/null 2>&1; then echo "Running"; else echo "Stopped"; fi
	@printf "Frontend (port 5173): "
	@if lsof -ti:5173 > /dev/null 2>&1; then echo "Running"; else echo "Stopped"; fi
	@printf "MySQL (port 3306):    "
	@if lsof -ti:3306 > /dev/null 2>&1; then echo "Running"; else echo "Stopped"; fi
endif
	@echo ""
	@echo "URLs:"
	@echo "  Backend:  http://localhost:8080"
	@echo "  Frontend: http://localhost:5173"
	@echo "  Swagger:  http://localhost:8080/swagger-ui/index.html"
	@echo ""

logs:
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$backend = Join-Path $$env:TEMP 'backend.log'; $$frontend = Join-Path $$env:TEMP 'frontend.log'; if ((Test-Path $$backend) -or (Test-Path $$frontend)) { Get-Content $$backend, $$frontend -Wait -Tail 50 -ErrorAction SilentlyContinue } else { Write-Host 'No logs found' }"
else
	@tail -f $(BACKEND_LOG) $(FRONTEND_LOG) 2>/dev/null || echo "No logs found"
endif

logs-backend:
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$log = Join-Path $$env:TEMP 'backend.log'; if (Test-Path $$log) { Get-Content $$log -Wait -Tail 50 } else { Write-Host 'No backend log found' }"
else
	@tail -f $(BACKEND_LOG) 2>/dev/null || echo "No backend log found"
endif

logs-frontend:
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$log = Join-Path $$env:TEMP 'frontend.log'; if (Test-Path $$log) { Get-Content $$log -Wait -Tail 50 } else { Write-Host 'No frontend log found' }"
else
	@tail -f $(FRONTEND_LOG) 2>/dev/null || echo "No frontend log found"
endif

install:
	@echo "Installing backend dependencies..."
	@cd backend && ./mvnw dependency:resolve -q
	@echo "Installing frontend dependencies..."
	@cd frontend && npm install
	@echo "Done!"

test:
	@cd backend && ./mvnw test

clean:
	@echo "Cleaning build artifacts..."
	@find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "node_modules" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "dist" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.log" -delete 2>/dev/null || true
ifeq ($(IS_WINDOWS),1)
	@powershell.exe -NoProfile -Command "$$backend = Join-Path $$env:TEMP 'backend.log'; $$frontend = Join-Path $$env:TEMP 'frontend.log'; Remove-Item $$backend, $$frontend -Force -ErrorAction SilentlyContinue"
else
	@rm -f $(BACKEND_LOG) $(FRONTEND_LOG) 2>/dev/null || true
endif
	@echo "Cleaned"

clean-db:
	@$(MAKE) stop 2>/dev/null || true
	@docker stop backend-mysql-1 2>/dev/null || true
	@docker rm -f backend-mysql-1 2>/dev/null || true
	@docker volume rm -f backend_mysql-data 2>/dev/null || true
	@docker system prune -f 2>/dev/null || true
	@echo "Database reset - run 'make dev' to start fresh"

clean-full: clean
	@$(DOCKER_COMPOSE) down -v 2>/dev/null || true
	@docker system prune -af --volumes 2>/dev/null || true
	@find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null || true
	@echo "Full cleanup completed"
