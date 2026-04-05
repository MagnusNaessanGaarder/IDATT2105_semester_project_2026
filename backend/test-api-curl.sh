#!/bin/bash
# Comprehensive API Test Script for Internal Control System
# Tests: Auth, Checklist Templates, and basic system endpoints

BASE_URL="http://localhost:8080/api"
PASS=0
FAIL=0
TOKEN=""
TEMPLATE_ID=""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    Internal Control API Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check prerequisites before starting
check_server

# Function to print test results
print_result() {
  if [ $1 -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC}: $2"
    ((PASS++))
  else
    echo -e "${RED}✗ FAIL${NC}: $2"
    if [ -n "$3" ]; then
      echo "  Error: $3"
    fi
    ((FAIL++))
  fi
}

# Function to check if server is running
check_server() {
  echo -e "${YELLOW}Checking prerequisites...${NC}"
  echo ""
  
  # Check if backend is running
  echo -n "  Checking backend... "
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null)
  if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${RED}✗ NOT RUNNING${NC}"
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  ERROR: Backend is not running!${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "To start the application, run:"
    echo "  ./start.sh"
    echo ""
    echo "Or start manually:"
    echo "  Terminal 1: cd backend && ./mvnw spring-boot:run -DskipTests"
    echo "  Terminal 2: cd frontend && npm run dev"
    echo ""
    exit 1
  fi
  echo -e "${GREEN}✓ RUNNING${NC}"
  
  # Check if MySQL is running
  echo -n "  Checking MySQL... "
  if ! docker ps | grep -q "backend-mysql-1"; then
    echo -e "${RED}✗ NOT RUNNING${NC}"
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  ERROR: MySQL database is not running!${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "MySQL should start automatically with Spring Boot."
    echo "If not running, try:"
    echo "  1. Stop everything: ./stop.sh"
    echo "  2. Start again: ./start.sh"
    echo ""
    echo "Or start MySQL manually:"
    echo "  docker compose -f compose-dev.yaml up -d"
    echo ""
    exit 1
  fi
  echo -e "${GREEN}✓ RUNNING${NC}"
  
  echo ""
  echo -e "${GREEN}✓ All prerequisites met${NC}"
  echo ""
}

# ==================== AUTH TESTS ====================
echo -e "${BLUE}Authentication Tests${NC}"
echo "--------------------"

# Test 1: Health check
echo "Test 1: Health check"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
  print_result 0 "Health check"
else
  print_result 1 "Health check" "HTTP $HTTP_CODE"
fi

# Test 2: Login with admin user
echo "Test 2: Login with admin user"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@everest-sushi.no", "password": "Test1234!"}' 2>/dev/null)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
  TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
  print_result 0 "Admin login"
else
  print_result 1 "Admin login" "HTTP $HTTP_CODE"
fi

# Test 3: Register new user (only if login worked)
if [ -n "$TOKEN" ]; then
  TIMESTAMP=$(date +%s)
  NEW_EMAIL="test${TIMESTAMP}@example.com"
  
  echo "Test 3: Register new user"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"${NEW_EMAIL}\", \"password\": \"TestPass123!\", \"fullName\": \"API Test User\", \"phone\": \"12345678\"}" 2>/dev/null)
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  
  if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "User registration"
  else
    print_result 1 "User registration" "HTTP $HTTP_CODE"
  fi
fi

# Test 4: Swagger UI
echo "Test 4: Swagger UI accessibility"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
  print_result 0 "Swagger UI accessible"
else
  print_result 1 "Swagger UI accessible" "HTTP $HTTP_CODE"
fi

echo ""

# ==================== CHECKLIST API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Checklist API Tests${NC}"
  echo "-------------------"
  
  # Test 5: Get all templates
  echo "Test 5: Get all checklist templates"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/checklists/templates?orgNumber=937219997" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  
  if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get all templates"
  else
    print_result 1 "Get all templates" "HTTP $HTTP_CODE"
  fi
  
  # Test 6: Get active templates
  echo "Test 6: Get active templates"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/checklists/templates/active?orgNumber=937219997" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  
  if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get active templates"
  else
    print_result 1 "Get active templates" "HTTP $HTTP_CODE"
  fi
  
  # Test 7: Get templates by module (FOOD)
  echo "Test 7: Get templates by module (FOOD)"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/checklists/templates/module/FOOD?orgNumber=937219997" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  
  if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get templates by module"
  else
    print_result 1 "Get templates by module" "HTTP $HTTP_CODE"
  fi
  
  # Test 8: Create template (orgNumber must be query param!)
  echo "Test 8: Create checklist template"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checklists/templates?orgNumber=937219997" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "moduleType": "FOOD",
      "title": "Daily Temperature Check",
      "description": "Check all fridge temperatures",
      "frequency": "DAILY"
    }' 2>/dev/null)
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  BODY=$(echo "$RESPONSE" | head -n-1)
  
  if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    TEMPLATE_ID=$(echo "$BODY" | grep -o '"templateId":[0-9]*' | cut -d':' -f2)
    print_result 0 "Create template (ID: $TEMPLATE_ID)"
  else
    print_result 1 "Create template" "HTTP $HTTP_CODE"
  fi
  
  # Test 9: Get template by ID (if created)
  if [ -n "$TEMPLATE_ID" ]; then
    echo "Test 9: Get template by ID"
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/checklists/templates/$TEMPLATE_ID?orgNumber=937219997" \
      -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" = "200" ]; then
      print_result 0 "Get template by ID"
    else
      print_result 1 "Get template by ID" "HTTP $HTTP_CODE"
    fi
  fi
  
  # Test 10: Update template
  if [ -n "$TEMPLATE_ID" ]; then
    echo "Test 10: Update template"
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/checklists/templates/$TEMPLATE_ID?orgNumber=937219997" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "title": "Daily Temperature Check - Updated",
        "description": "Updated description"
      }' 2>/dev/null)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" = "200" ]; then
      print_result 0 "Update template"
    else
      print_result 1 "Update template" "HTTP $HTTP_CODE"
    fi
  fi
  
  # Test 11: Get templates by module (ALCOHOL)
  echo "Test 11: Get templates by module (ALCOHOL)"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/checklists/templates/module/ALCOHOL?orgNumber=937219997" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  
  if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get ALCOHOL templates"
  else
    print_result 1 "Get ALCOHOL templates" "HTTP $HTTP_CODE"
  fi
  
else
  echo -e "${YELLOW}Skipping checklist tests (no auth token)${NC}"
fi

echo ""

# ==================== SUMMARY ====================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    Test Results${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Passed: $PASS${NC}"
echo -e "${RED}Failed: $FAIL${NC}"
echo -e "${BLUE}Total:  $((PASS + FAIL))${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}✓ All tests passed!${NC}"
  exit 0
else
  echo -e "${RED}✗ Some tests failed${NC}"
  exit 1
fi
