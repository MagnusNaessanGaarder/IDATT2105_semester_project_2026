#!/bin/bash

# Backend startup script
# usage: cd backend && ./run run
set -e

MODE=${1:-dev}

if [ ! -f .env ]; then
  echo "Error: .env file not found "
  echo "create .env from .env.example. ask Tri for .env var"
  exit 1
fi

# Export environment variables from .env
export $(grep -v '^#' .env | xargs)

echo "./mvnw spring-boot run"
./mvnw spring-boot:run
