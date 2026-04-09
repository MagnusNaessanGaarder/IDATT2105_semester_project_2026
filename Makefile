.PHONY: help dev stop restart status logs logs-backend logs-frontend test clean clean-db clean-full install wait-mysql

# Java configuration (only force JAVA_HOME when this path exists)
ifneq ("$(wildcard /usr/lib/jvm/java-21-openjdk)","")
JAVA_HOME := /usr/lib/jvm/java-21-openjdk
PATH := $(JAVA_HOME)/bin:$(PATH)
export JAVA_HOME PATH
endif

# Startup wait windows (seconds)
MYSQL_STARTUP_TIMEOUT ?= 120
BACKEND_STARTUP_TIMEOUT ?= 180

help:
	@echo ""
	@echo "  make dev         - Start all services"
	@echo "  make stop        - Stop all services"
	@echo "  make restart     - Restart all services"
	@echo "  make status      - Check service status"
	@echo "  make wait-mysql  - Wait for MySQL to be ready (useful for VPN)"
	@echo "  make logs        - Show all logs"
	@echo "  make logs-backend - Show backend logs only"
	@echo "  make logs-frontend - Show frontend logs only"
	@echo "  make install     - Install dependencies"
	@echo "  make test        - Run tests"
	@echo "  make clean       - Clean build files"
	@echo "  make clean-db    - Reset database"
	@echo "  make clean-full  - Full cleanup"
	@echo ""

dev:
	@echo "========================================"
	@echo "  Starting dev environment"
	@echo "========================================"
	@$(MAKE) stop 2>/dev/null || true
	@if [ ! -d frontend/node_modules ]; then \
		echo "Installing frontend dependencies..."; \
		cd frontend && npm install; \
	fi
	@echo ""
	@echo "[1/4] Starting MySQL..."
	@if lsof -ti:3306 > /dev/null 2>&1; then \
		echo "  MySQL already reachable on port 3306"; \
	elif command -v docker > /dev/null 2>&1 && [ "$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' backend-mysql-1 2>/dev/null)" = "healthy" ]; then \
		echo "  MySQL container already healthy"; \
	elif command -v docker > /dev/null 2>&1; then \
		docker compose -f compose-dev.yaml up -d mysql 2>/dev/null || docker start backend-mysql-1 2>/dev/null || true; \
		mysql_started=0; \
		for i in $$(seq 1 60); do \
			if lsof -ti:3306 > /dev/null 2>&1 || (command -v docker > /dev/null 2>&1 && [ "$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' backend-mysql-1 2>/dev/null)" = "healthy" ]); then \
				mysql_started=1; \
				break; \
			fi; \
			sleep 1; \
		done; \
		if [ $$mysql_started -eq 1 ]; then \
			echo "  MySQL started"; \
		else \
			echo "  MySQL failed to start (check Docker and compose-dev.yaml)"; \
			exit 1; \
		fi; \
	else \
		echo "  Docker not found. Install Docker or start MySQL manually on port 3306."; \
		exit 1; \
	fi
	@echo ""
	@echo "[2/4] Starting backend..."
	@if ! (lsof -ti:3306 > /dev/null 2>&1 || (command -v docker > /dev/null 2>&1 && [ "$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' backend-mysql-1 2>/dev/null)" = "healthy" ])); then \
		echo "  Skipping backend (MySQL is not running on port 3306)"; \
	elif ! command -v java > /dev/null 2>&1; then \
		echo "  Skipping backend (Java not found in PATH)"; \
	else \
		(cd backend && ./mvnw spring-boot:run -DskipTests -Dcheckstyle.skip=true > /tmp/backend.log 2>&1 &); \
		backend_started=0; \
		for i in $$(seq 1 30); do \
			if lsof -ti:8080 > /dev/null 2>&1; then \
				backend_started=1; \
				break; \
			fi; \
			sleep 1; \
		done; \
		if [ $$backend_started -eq 1 ]; then \
			echo "  Backend started"; \
		else \
			echo "  Backend failed to start. Last backend log lines:"; \
			tail -n 20 /tmp/backend.log 2>/dev/null || echo "  No backend log found"; \
			exit 1; \
		fi; \
	fi
	@echo ""
	@echo "[3/3] Starting frontend..."
	@(cd frontend && nohup npm run dev > /tmp/frontend.log 2>&1 &)
	@frontend_started=0; \
	for i in $$(seq 1 30); do \
		if lsof -ti:5173 > /dev/null 2>&1; then \
			frontend_started=1; \
			break; \
		fi; \
		sleep 1; \
	done; \
	if [ $$frontend_started -eq 1 ]; then \
		echo "  Frontend started"; \
	else \
		echo "  Frontend failed to start. Last frontend log lines:"; \
		tail -n 20 /tmp/frontend.log 2>/dev/null || echo "  No frontend log found"; \
		exit 1; \
	fi
	@echo ""
	@echo "[4/4] Seeding example document..."
	@if lsof -ti:8080 > /dev/null 2>&1; then \
		bash seed-document.sh 2>&1 | sed 's/^/  /' || echo "  Seed skipped (check seed-document.sh or Azure config)"; \
	else \
		echo "  Seed skipped (backend is not running)"; \
	fi
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
	@# Stop Docker
	@-docker stop backend-mysql-1 2>/dev/null || true
	@-docker rm backend-mysql-1 2>/dev/null || true
	@-docker compose -f compose-dev.yaml down
	@echo "  All services stopped"
	@echo ""

restart: stop dev

status:
	@echo ""
	@echo "Service Status:"
	@echo ""
	@printf "Backend (port 8080):  "
	@if lsof -ti:8080 > /dev/null 2>&1; then echo "Running"; else echo "Stopped"; fi
	@printf "Frontend (port 5173): "
	@if lsof -ti:5173 > /dev/null 2>&1; then echo "Running"; else echo "Stopped"; fi
	@printf "MySQL (port 3306):    "
	@if lsof -ti:3306 > /dev/null 2>&1 || (command -v docker > /dev/null 2>&1 && [ "$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' backend-mysql-1 2>/dev/null)" = "healthy" ]); then echo "Running"; else echo "Stopped"; fi
	@echo ""
	@echo "URLs:"
	@echo "  Backend:  http://localhost:8080"
	@echo "  Frontend: http://localhost:5173"
	@echo "  Swagger:  http://localhost:8080/swagger-ui/index.html"
	@echo ""

logs:
	@tail -f /tmp/backend.log /tmp/frontend.log 2>/dev/null || echo "No logs found"

logs-backend:
	@tail -f /tmp/backend.log 2>/dev/null || echo "No backend log found"

logs-frontend:
	@tail -f /tmp/frontend.log 2>/dev/null || echo "No frontend log found"

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
	@rm -f /tmp/backend.log /tmp/frontend.log 2>/dev/null || true
	@echo "Cleaned"

clean-db:
	@$(MAKE) stop 2>/dev/null || true
	@docker stop backend-mysql-1 2>/dev/null || true
	@docker rm -f backend-mysql-1 2>/dev/null || true
	@docker volume rm -f backend_mysql-data 2>/dev/null || true
	@docker system prune -f 2>/dev/null || true
	@echo "Database reset - run 'make dev' to start fresh"

clean-full: clean clean-db
	@docker compose down -v 2>/dev/null || true
	@docker system prune -af --volumes 2>/dev/null || true
	@find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null || true
	@echo "Full cleanup completed"

wait-mysql:
	@echo "Waiting for MySQL to be ready..."
	@for i in 1 2 3 4 5 6 7 8 9 10; do \
		if nc -z localhost 3306 2>/dev/null; then \
			echo "MySQL is ready!"; \
			exit 0; \
		fi; \
		echo "Attempt $$i: MySQL not ready yet..."; \
		sleep 5; \
	done; \
	echo "MySQL failed to start within 50 seconds"; \
	exit 1
