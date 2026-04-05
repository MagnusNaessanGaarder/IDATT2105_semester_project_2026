.PHONY: help dev stop restart status logs test clean clean-db clean-full install

# Java 21 Configuration
JAVA_HOME := /usr/lib/jvm/java-21-openjdk
PATH := $(JAVA_HOME)/bin:$(PATH)
export JAVA_HOME PATH

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
	@echo "[1/3] Starting MySQL (Docker)..."
	@docker compose -f compose-dev.yaml up -d mysql 2>/dev/null || docker start backend-mysql-1 2>/dev/null || true
	@sleep 5
	@echo "  MySQL started"
	@echo ""
	@echo "[2/3] Starting backend..."
	@(cd backend && ./mvnw spring-boot:run -DskipTests -Dcheckstyle.skip=true > /tmp/backend.log 2>&1 &)
	@sleep 15
	@echo "  Backend started"
	@echo ""
	@echo "[3/3] Starting frontend..."
	@(cd frontend && nohup npm run dev > /tmp/frontend.log 2>&1 &)
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
	@-docker compose -f compose-dev.yaml down -v 
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
	@if lsof -ti:3306 > /dev/null 2>&1; then echo "Running"; else echo "Stopped"; fi
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

clean-full: clean
	@docker compose down -v 2>/dev/null || true
	@docker system prune -af --volumes 2>/dev/null || true
	@find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null || true
	@echo "Full cleanup completed"
