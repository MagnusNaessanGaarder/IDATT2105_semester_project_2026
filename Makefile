.PHONY: run run-backend run-frontend run-docker dev clean clean-backend clean-frontend clean-ide clean-logs clean-docker clean-tests clean-cache clean-all help

run: run-docker run-backend run-frontend
	@echo "All services started"

run-backend:
	@cd backend && ./run.sh
run-frontend:
	@cd frontend && npm run dev

run-docker:
	@docker compose up -d

dev: clean run-docker
	@cd backend && ./run.sh dev &
	@cd frontend && npm run dev &
	@echo "  Backend: http://localhost:8080"
	@echo "  Frontend: http://localhost:5173"
	@echo "  Swagger: http://localhost:8080/swagger-ui/index.html"


clean-all: clean clean-docker
	@echo "Full cleanup fullført (inkludert Docker)"

clean-backend:
	@find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.class" -delete 2>/dev/null || true
	@find . -type f -name "*.jar" -delete 2>/dev/null || true
	@find . -type f -name "*.war" -delete 2>/dev/null || true
	@find . -type f -name ".classpath" -delete 2>/dev/null || true
	@find . -type f -name ".project" -delete 2>/dev/null || true
	@find . -type f -name ".factorypath" -delete 2>/dev/null || true
	@find . -type d -name ".settings" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.iml" -delete 2>/dev/null || true
	@find . -type d -name ".apt_generated" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".apt_generated_tests" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "hs_err_pid*" -delete 2>/dev/null || true
	@echo "  Backend cleaned"

clean-frontend:
	@echo "Rydder frontend"
	@find . -type d -name "node_modules" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "dist" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "dist-ssr" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name ".eslintcache" -delete 2>/dev/null || true
	@find . -type f -name "*.timestamp-*-*.mjs" -delete 2>/dev/null || true
	@find . -type f -name "*.tsbuildinfo" -delete 2>/dev/null || true
	@find . -type d -name ".cache" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".turbo" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".parcel-cache" -exec rm -rf {} + 2>/dev/null || true
	@echo "  Frontend ryddet"

clean-ide:
	@echo "Rydder IDE-filer"
	@find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.suo" -delete 2>/dev/null || true
	@find . -type f -name "*.ntvs*" -delete 2>/dev/null || true
	@find . -type f -name "*.njsproj" -delete 2>/dev/null || true
	@find . -type f -name "*.sln" -delete 2>/dev/null || true
	@find . -type f -name ".editorconfig" -delete 2>/dev/null || true
	@echo "  IDE-filer ryddet"

clean-logs:
	@echo "  Rydder loggfiler..."
	@find . -type f -name "*.log" -delete 2>/dev/null || true
	@find . -type f -name "npm-debug.log*" -delete 2>/dev/null || true
	@find . -type f -name "yarn-debug.log*" -delete 2>/dev/null || true
	@find . -type f -name "yarn-error.log*" -delete 2>/dev/null || true
	@find . -type f -name "pnpm-debug.log*" -delete 2>/dev/null || true
	@find . -type f -name "lerna-debug.log*" -delete 2>/dev/null || true
	@find . -type d -name "logs" -exec rm -rf {} + 2>/dev/null || true
	@echo " Loggfiler ryddet"

clean-tests:
	@echo "Rydder test-artefakter"
	@find . -type d -name "coverage" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".nyc_output" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "cypress/videos" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "cypress/screenshots" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "__screenshots__" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "playwright-report" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name "test-results" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.spec.js.snap" -delete 2>/dev/null || true
	@echo "  Test-artefakter ryddet"

clean-cache:
	@echo "Rydder cache"
	@find . -type d -name ".cache" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".temp" -exec rm -rf {} + 2>/dev/null || true
	@find . -type d -name ".tmp" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.tmp" -delete 2>/dev/null || true
	@find . -type f -name "*.temp" -delete 2>/dev/null || true
	@find . -type d -name ".gradle" -exec rm -rf {} + 2>/dev/null || true
	@echo "  Cache ryddet"

clean-docker:
	@echo " Rydder Docker"
	@docker compose down -v 2>/dev/null || true
	@docker compose rm -f 2>/dev/null || true
	@docker system prune -f 2>/dev/null || true
	@echo "Docker ryddet"

clean-os:
	@echo "Rydder OS-spesifikke filer"
	@find . -type f -name ".DS_Store" -delete 2>/dev/null || true
	@find . -type f -name "Thumbs.db" -delete 2>/dev/null || true
	@find . -type f -name "desktop.ini" -delete 2>/dev/null || true
	@find . -type f -name ".directory" -delete 2>/dev/null || true
	@echo "  OS-filer ryddet"
