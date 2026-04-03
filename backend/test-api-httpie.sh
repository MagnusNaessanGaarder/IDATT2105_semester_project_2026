#!/bin/bash
# API Test Script using HTTPie

BASE_URL="localhost:8080/api"
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
echo -e "${BLUE}  API Tests with HTTPie${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if httpie is installed
if ! command -v http &>/dev/null; then
  echo -e "${RED}ERROR: HTTPie is not installed!${NC}"
  echo ""
  echo "To install HTTPie:"
  echo "  Arch:    sudo pacman -S httpie"
  echo "  Ubuntu:  sudo apt-get install httpie"
  echo "  macOS:   brew install httpie"
  echo "  Python:  pip install httpie"
  echo ""
  echo "Or use the curl version: ./test-api-curl.sh"
  exit 1
fi

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

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"
echo ""

echo -n "  Checking backend... "
if ! http :8080/actuator/health &>/dev/null; then
  echo -e "${RED}✗ NOT RUNNING${NC}"
  echo ""
  echo -e "${RED}ERROR: Backend is not running!${NC}"
  echo "Run: ./start.sh"
  exit 1
fi
echo -e "${GREEN}✓ RUNNING${NC}"

echo -n "  Checking MySQL... "
if ! docker ps | grep -q "backend-mysql-1"; then
  echo -e "${RED}✗ NOT RUNNING${NC}"
  echo ""
  echo -e "${RED}ERROR: MySQL is not running!${NC}"
  exit 1
fi
echo -e "${GREEN}✓ RUNNING${NC}"

echo ""

# ==================== AUTH TESTS ====================
echo -e "${BLUE}Authentication Tests${NC}"
echo "--------------------"

# Test 1: Health check
echo "Test 1: Health check"
if http :8080/actuator/health &>/dev/null; then
  print_result 0 "Health check"
else
  print_result 1 "Health check"
fi

# Test 2: Login with admin
echo "Test 2: Login with admin user"
RESPONSE=$(http --ignore-stdin -b POST :8080/api/auth/login email="admin@everest-sushi.no" password="Test1234!" 2>/dev/null)
if [ -n "$RESPONSE" ] && echo "$RESPONSE" | grep -q "accessToken"; then
  TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
  print_result 0 "Admin login"
else
  print_result 1 "Admin login" "No access token received"
fi

# Test 3: Register new user
if [ -n "$TOKEN" ]; then
  TIMESTAMP=$(date +%s)
  NEW_EMAIL="test${TIMESTAMP}@example.com"

  echo "Test 3: Register new user"
  if http --ignore-stdin -b POST :8080/api/auth/register \
    email="$NEW_EMAIL" \
    password="TestPass123!" \
    fullName="API Test User" \
    phone="12345678" &>/dev/null; then
    print_result 0 "User registration"
  else
    print_result 1 "User registration"
  fi
fi

# Test 4: Swagger UI
echo "Test 4: Swagger UI accessibility"
if http :8080/swagger-ui/index.html &>/dev/null; then
  print_result 0 "Swagger UI accessible"
else
  print_result 1 "Swagger UI accessible"
fi

echo ""

# ==================== CHECKLIST TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Checklist API Tests${NC}"
  echo "-------------------"

  # Test 5: Get all templates
  echo "Test 5: Get all templates"
  if http :8080/api/checklists/templates orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all templates"
  else
    print_result 1 "Get all templates"
  fi

  # Test 6: Get active templates
  echo "Test 6: Get active templates"
  if http :8080/api/checklists/templates/active orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get active templates"
  else
    print_result 1 "Get active templates"
  fi

  # Test 7: Get by module (FOOD)
  echo "Test 7: Get templates by module (FOOD)"
  if http :8080/api/checklists/templates/module/FOOD orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get FOOD templates"
  else
    print_result 1 "Get FOOD templates"
  fi

  # Test 8: Create template
  echo "Test 8: Create template"
  RESPONSE=$(http --ignore-stdin -b POST :8080/api/checklists/templates \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    moduleType="FOOD" \
    title="Daily Temperature Check" \
    description="Check all fridge temperatures" \
    frequency="DAILY" 2>/dev/null)

  if [ -n "$RESPONSE" ]; then
    TEMPLATE_ID=$(echo "$RESPONSE" | grep -o '"templateId":[0-9]*' | cut -d':' -f2)
    if [ -n "$TEMPLATE_ID" ]; then
      print_result 0 "Create template (ID: $TEMPLATE_ID)"
    else
      print_result 1 "Create template" "No templateId in response"
    fi
  else
    print_result 1 "Create template"
  fi

  # Test 9: Get by ID
  if [ -n "$TEMPLATE_ID" ]; then
    echo "Test 9: Get template by ID"
    if http :8080/api/checklists/templates/$TEMPLATE_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get template by ID"
    else
      print_result 1 "Get template by ID"
    fi
  fi

  # Test 10: Update template
  if [ -n "$TEMPLATE_ID" ]; then
    echo "Test 10: Update template"
    if http --ignore-stdin -b PUT :8080/api/checklists/templates/$TEMPLATE_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" \
      title="Daily Temperature Check - Updated" \
      moduleType="FOOD" \
      frequency="DAILY" \
      description="Updated description" &>/dev/null; then
      print_result 0 "Update template"
    else
      print_result 1 "Update template"
    fi
  fi

  # Test 11: Get by module (ALCOHOL)
  echo "Test 11: Get ALCOHOL templates"
  if http :8080/api/checklists/templates/module/ALCOHOL orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get ALCOHOL templates"
  else
    print_result 1 "Get ALCOHOL templates"
  fi

fi

echo ""

# ==================== DEVIATION API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Deviation API Tests${NC}"
  echo "-------------------"

  # Test 12: Create deviation report
  echo "Test 12: Create deviation report"
  DEV_RESPONSE=$(http --ignore-stdin -b POST :8080/api/deviations \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    reportType="INCIDENT" \
    severity="MAJOR" \
    title="Test Incident" \
    description="Test description for incident" 2>/dev/null)
  
  if [ -n "$DEV_RESPONSE" ]; then
    DEVIATION_ID=$(echo "$DEV_RESPONSE" | grep -o '"reportId":[0-9]*' | cut -d':' -f2)
    if [ -n "$DEVIATION_ID" ]; then
      print_result 0 "Create deviation report (ID: $DEVIATION_ID)"
    else
      print_result 1 "Create deviation report" "No reportId in response"
    fi
  else
    print_result 1 "Create deviation report"
  fi

  # Test 13: Get all deviations
  echo "Test 13: Get all deviation reports"
  if http :8080/api/deviations orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all deviation reports"
  else
    print_result 1 "Get all deviation reports"
  fi

  # Test 14: Get deviation by ID
  if [ -n "$DEVIATION_ID" ]; then
    echo "Test 14: Get deviation by ID"
    if http :8080/api/deviations/$DEVIATION_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get deviation by ID"
    else
      print_result 1 "Get deviation by ID"
    fi
  fi

  # Test 15: Filter by status
  echo "Test 15: Filter deviations by status"
  if http :8080/api/deviations/status/REPORTED orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Filter deviations by status"
  else
    print_result 1 "Filter deviations by status"
  fi

  # Test 16: Filter by severity
  echo "Test 16: Filter deviations by severity"
  if http :8080/api/deviations/severity/MAJOR orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Filter deviations by severity"
  else
    print_result 1 "Filter deviations by severity"
  fi

  # Test 17: Update status
  if [ -n "$DEVIATION_ID" ]; then
    echo "Test 17: Update deviation status"
    if http --ignore-stdin -b PUT :8080/api/deviations/$DEVIATION_ID/status \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" \
      status="under_investigation" &>/dev/null; then
      print_result 0 "Update deviation status"
    else
      print_result 1 "Update deviation status"
    fi
  fi

  # Test 18: Count open deviations
  echo "Test 18: Count open deviation reports"
  if http :8080/api/deviations/count/open orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Count open deviations"
  else
    print_result 1 "Count open deviations"
  fi

else
  echo -e "${YELLOW}Skipping deviation tests (no auth token)${NC}"
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
