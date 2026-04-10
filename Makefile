JAVA_HOME := $(shell dirname $$(dirname $$(readlink -f $$(command -v javac))))
PATH := $(JAVA_HOME)/bin:$(PATH)
export JAVA_HOME PATH

.PHONY: help dev stop status logs test clean prod-build prod-up prod-down prod-logs

help:
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Development:"
	@echo "  dev       Start all services (MySQL, backend, frontend)"
	@echo "  stop      Stop all running services"
	@echo "  status    Show which services are running"
	@echo "  logs      Show logs from all services"
	@echo ""
	@echo "Testing:"
	@echo "  test      Run backend tests"
	@echo ""
	@echo "Maintenance:"
	@echo "  clean     Remove build artifacts and reset database"
	@echo "  install   Install dependencies without starting services"
	@echo ""
	@echo "Production:"
	@echo "  prod-build  Build production Docker images"
	@echo "  prod-up     Start production services"
	@echo "  prod-down   Stop production services"
	@echo "  prod-logs   View production logs"
	@echo ""

# Start all services for local development
dev:
	@echo "Starting development environment..."
	@$(MAKE) stop >/dev/null 2>&1 || true
	@echo ""

	@echo "[1/3] Starting MySQL..."
	@docker compose -f compose-dev.yaml up -d mysql 2>/dev/null || docker start backend-mysql-1 2>/dev/null || true
	@for i in $$(seq 1 30); do \
		docker exec backend-mysql-1 mysqladmin ping -h 127.0.0.1 -uik_root -pikroot >/dev/null 2>&1 && break; \
		if [ $$i -eq 30 ]; then \
			echo "  ERROR: MySQL did not become ready"; \
			exit 1; \
		fi; \
		sleep 1; \
	done
	@echo "  MySQL ready"
	@echo ""

	@echo "[2/3] Starting backend..."
	@cd backend && nohup ./mvnw -Plocal-run spring-boot:run -Dmaven.test.skip=true -Dcheckstyle.skip=true > /tmp/backend.log 2>&1 &
	@for i in $$(seq 1 120); do \
		lsof -ti:8080 >/dev/null 2>&1 && break; \
		if [ $$i -eq 120 ]; then \
			echo "  ERROR: Backend failed to start (see /tmp/backend.log)"; \
			exit 1; \
		fi; \
		sleep 1; \
	done
	@echo "  Backend started on http://localhost:8080"
	@echo ""

	@echo "[3/3] Starting frontend..."
	@(cd frontend && nohup npm run dev -- --host 127.0.0.1 > /tmp/frontend.log 2>&1 &)
	@for i in $$(seq 1 30); do \
		lsof -ti:5173 >/dev/null 2>&1 && break; \
		if [ $$i -eq 30 ]; then \
			echo "  ERROR: Frontend failed to start (see /tmp/frontend.log)"; \
			exit 1; \
		fi; \
		sleep 1; \
	done
	@echo "  Frontend started on http://localhost:5173"
	@echo ""

	@echo "All services started!"
	@echo "  Backend:  http://localhost:8080"
	@echo "  Frontend: http://localhost:5173"
	@echo "  API docs: http://localhost:8080/swagger-ui.html"
	@echo ""
	@echo "Logs: make logs"
	@echo "Stop: make stop"

# Stop all services
stop:
	@echo "Stopping services..."
	@-lsof -ti:8080 2>/dev/null | xargs -r kill -9 2>/dev/null || true
	@-lsof -ti:5173 2>/dev/null | xargs -r kill -9 2>/dev/null || true
	@-jps -l 2>/dev/null | grep -E "(InternalControl|spring-boot)" | awk '{print $$1}' | xargs -r kill -9 2>/dev/null || true
	@-pgrep -f "mvnw" | xargs -r kill -9 2>/dev/null || true
	@-docker compose -f compose-dev.yaml down 2>/dev/null || true
	@-docker stop backend-mysql-1 2>/dev/null || true
	@-docker rm backend-mysql-1 2>/dev/null || true
	@echo "  Done"

# Show service status
status:
	@echo ""
	@echo "Service Status:"
	@echo ""
	@printf "  MySQL:    "
	@if docker ps 2>/dev/null | grep -q backend-mysql-1; then echo "running"; else echo "stopped"; fi
	@printf "  Backend:  "
	@if lsof -ti:8080 >/dev/null 2>&1; then echo "running (port 8080)"; else echo "stopped"; fi
	@printf "  Frontend: "
	@if lsof -ti:5173 >/dev/null 2>&1; then echo "running (port 5173)"; else echo "stopped"; fi
	@echo ""

# Show logs
logs:
	@echo "=== Backend log ==="
	@tail -50 /tmp/backend.log 2>/dev/null || echo "No backend log"
	@echo ""
	@echo "=== Frontend log ==="
	@tail -20 /tmp/frontend.log 2>/dev/null || echo "No frontend log"

# Run backend tests
test:
	@cd backend && ./mvnw test

# Install dependencies without starting
install:
	@echo "Installing backend dependencies..."
	@cd backend && ./mvnw dependency:resolve -q
	@echo "Installing frontend dependencies..."
	@cd frontend && npm install
	@echo "Done"

# Clean everything
clean:
	@echo "Cleaning up..."
	@$(MAKE) stop >/dev/null 2>&1 || true
	@rm -rf backend/target frontend/dist 2>/dev/null || true
	@rm -f /tmp/backend.log /tmp/frontend.log 2>/dev/null || true
	@docker compose -f compose-dev.yaml down -v 2>/dev/null || true
	@docker volume rm -f backend_mysql-data 2>/dev/null || true
	@echo "  Done"

# Production commands
prod-build:
	@docker compose build

prod-up:
	@docker compose up -d

prod-down:
	@docker compose down

prod-logs:
	@docker compose logs -f
