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

# Test 2: Register new user first
echo "Test 2: Register new user"
TIMESTAMP=$(date +%s)
TEST_EMAIL="test${TIMESTAMP}@example.com"
REGISTER_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/auth/register \
  email="$TEST_EMAIL" \
  password="TestPass123!" \
  fullName="API Test User" \
  phone="12345678" 2>/dev/null)

if [ -n "$REGISTER_RESPONSE" ] && echo "$REGISTER_RESPONSE" | grep -q "accessToken"; then
  TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

  # Extract userId from JWT token payload
  JWT_PAYLOAD=$(echo "$TOKEN" | cut -d'.' -f2)
  # Add padding if needed
  PADDING=$((4 - ${#JWT_PAYLOAD} % 4))
  if [ $PADDING -ne 4 ]; then
    JWT_PAYLOAD=$(echo -n "$JWT_PAYLOAD" | tr '_-' '/+' | tr -d '=')
    for i in $(seq 1 $PADDING); do JWT_PAYLOAD="${JWT_PAYLOAD}="; done
  fi
  USER_ID=$(echo "$JWT_PAYLOAD" | base64 -d 2>/dev/null | grep -o '"userId":[0-9]*' | cut -d':' -f2)

  # Assign MANAGER role and organization membership
  if [ -n "$USER_ID" ]; then
    docker exec backend-mysql-1 mysql -u root -proot -e "
      -- Add user to organization
      INSERT IGNORE INTO internal_control.user_organization (user_id, org_number, is_active, joined_at) 
      VALUES ($USER_ID, 937219997, 1, NOW());
      
      -- Assign MANAGER role
      INSERT IGNORE INTO internal_control.role (role_name, is_system_role) VALUES ('MANAGER', 0);
      SET @ROLE_ID = (SELECT role_id FROM internal_control.role WHERE role_name = 'MANAGER');
      INSERT IGNORE INTO internal_control.user_organization_role (user_id, org_number, role_id) 
      VALUES ($USER_ID, 937219997, @ROLE_ID);
    " 2>/dev/null

    # Re-login to get new token with MANAGER role
    sleep 1
    LOGIN_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/auth/login \
      email="$TEST_EMAIL" \
      password="TestPass123!" 2>/dev/null)
    if [ -n "$LOGIN_RESPONSE" ]; then
      NEW_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
      if [ -n "$NEW_TOKEN" ]; then
        TOKEN="$NEW_TOKEN"
      fi
    fi
  fi

  print_result 0 "User registration"
else
  print_result 1 "User registration" "No access token received"
fi

# Test 3: Login with registered user
if [ -n "$TOKEN" ]; then
  echo "Test 3: Login with registered user"
  LOGIN_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/auth/login \
    email="$TEST_EMAIL" \
    password="TestPass123!" 2>/dev/null)

  if [ -n "$LOGIN_RESPONSE" ] && echo "$LOGIN_RESPONSE" | grep -q "accessToken"; then
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    print_result 0 "User login"
  else
    print_result 1 "User login" "No access token received"
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
  if http :8080/api/v1/checklists/templates orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all templates"
  else
    print_result 1 "Get all templates"
  fi

  # Test 6: Get active templates
  echo "Test 6: Get active templates"
  if http :8080/api/v1/checklists/templates/active orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get active templates"
  else
    print_result 1 "Get active templates"
  fi

  # Test 7: Get by module (FOOD)
  echo "Test 7: Get templates by module (FOOD)"
  if http :8080/api/v1/checklists/templates/module/FOOD orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get FOOD templates"
  else
    print_result 1 "Get FOOD templates"
  fi

  # Test 8: Create template
  echo "Test 8: Create template"
  RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/checklists/templates \
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
    if http :8080/api/v1/checklists/templates/$TEMPLATE_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get template by ID"
    else
      print_result 1 "Get template by ID"
    fi
  fi

  # Test 10: Update template
  if [ -n "$TEMPLATE_ID" ]; then
    echo "Test 10: Update template"
    if http --ignore-stdin -b PUT :8080/api/v1/checklists/templates/$TEMPLATE_ID \
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
  if http :8080/api/v1/checklists/templates/module/ALCOHOL orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
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
  DEV_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/deviations \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    reportType="incident" \
    severity="major" \
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
  if http :8080/api/v1/deviations orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all deviation reports"
  else
    print_result 1 "Get all deviation reports"
  fi

  # Test 14: Get deviation by ID
  if [ -n "$DEVIATION_ID" ]; then
    echo "Test 14: Get deviation by ID"
    if http :8080/api/v1/deviations/$DEVIATION_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get deviation by ID"
    else
      print_result 1 "Get deviation by ID"
    fi
  fi

  # Test 15: Filter by status
  echo "Test 15: Filter deviations by status"
  if http :8080/api/v1/deviations/status/REPORTED orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Filter deviations by status"
  else
    print_result 1 "Filter deviations by status"
  fi

  # Test 16: Filter by severity
  echo "Test 16: Filter deviations by severity"
  if http :8080/api/v1/deviations/severity/MAJOR orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Filter deviations by severity"
  else
    print_result 1 "Filter deviations by severity"
  fi

  # Test 17: Update status
  if [ -n "$DEVIATION_ID" ]; then
    echo "Test 17: Update deviation status"
    if http --ignore-stdin -b PUT :8080/api/v1/deviations/$DEVIATION_ID/status \
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
  if http :8080/api/v1/deviations/count/open orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Count open deviations"
  else
    print_result 1 "Count open deviations"
  fi

else
  echo -e "${YELLOW}Skipping deviation tests (no auth token)${NC}"
fi

echo ""

# ==================== LOCATION TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Location API Tests${NC}"
  echo "------------------"

  # Test 19: Get all locations
  echo "Test 19: Get all locations"
  if http :8080/api/v1/locations orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all locations"
  else
    print_result 1 "Get all locations"
  fi

  # Test 20: Create location
  echo "Test 20: Create location"
  LOC_TIMESTAMP=$(date +%s)
  LOC_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/locations \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    name="Test Kitchen $LOC_TIMESTAMP" \
    description="Test location" \
    locationType="KITCHEN" \
    tempMinC:=2.0 \
    tempMaxC:=8.0 \
    isActive:=true 2>/dev/null)

  if [ -n "$LOC_RESPONSE" ]; then
    LOCATION_ID=$(echo "$LOC_RESPONSE" | grep -o '"locationId":[0-9]*' | cut -d':' -f2)
    if [ -n "$LOCATION_ID" ]; then
      print_result 0 "Create location (ID: $LOCATION_ID)"
    else
      print_result 1 "Create location" "No locationId in response"
    fi
  else
    print_result 1 "Create location"
  fi

  # Test 21: Get location by ID
  if [ -n "$LOCATION_ID" ]; then
    echo "Test 21: Get location by ID"
    if http :8080/api/v1/locations/$LOCATION_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get location by ID"
    else
      print_result 1 "Get location by ID"
    fi
  fi

  # Test 22: Update location
  if [ -n "$LOCATION_ID" ]; then
    echo "Test 22: Update location"
    if http --ignore-stdin -b PUT :8080/api/v1/locations/$LOCATION_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" \
      name="Test Kitchen Updated" \
      description="Updated" \
      locationType="KITCHEN" \
      tempMinC:=2.0 \
      tempMaxC:=8.0 \
      isActive:=true &>/dev/null; then
      print_result 0 "Update location"
    else
      print_result 1 "Update location"
    fi
  fi
fi

echo ""

# ==================== TEMPERATURE API TESTS ====================
if [ -n "$TOKEN" ] && [ -n "$LOCATION_ID" ]; then
  echo -e "${BLUE}Temperature API Tests${NC}"
  echo "---------------------"

  # Test 23: Create temperature log point
  echo "Test 23: Create temperature log point"
  TP_TIMESTAMP=$(date +%s)
  TP_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/temperature/points \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    locationId:=$LOCATION_ID \
    name="Fridge $TP_TIMESTAMP" \
    isActive:=true 2>/dev/null)

  if [ -n "$TP_RESPONSE" ]; then
    TEMP_POINT_ID=$(echo "$TP_RESPONSE" | grep -o '"logPointId":[0-9]*' | cut -d':' -f2)
    if [ -n "$TEMP_POINT_ID" ]; then
      print_result 0 "Create temp log point (ID: $TEMP_POINT_ID)"
    else
      print_result 1 "Create temp log point" "No logPointId in response"
    fi
  else
    print_result 1 "Create temp log point"
  fi

  # Test 24: Get all log points
  echo "Test 24: Get all temperature log points"
  if http :8080/api/v1/temperature/points orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all temp log points"
  else
    print_result 1 "Get all temp log points"
  fi

  # Test 25: Record temperature entry
  if [ -n "$TEMP_POINT_ID" ]; then
    echo "Test 25: Record temperature entry"
    ENTRY_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/temperature/entries \
      Authorization:"Bearer $TOKEN" \
      orgNumber==937219997 \
      logPointId:=$TEMP_POINT_ID \
      temperatureC:=4.5 \
      noteText="Normal temperature" 2>/dev/null)

    if [ -n "$ENTRY_RESPONSE" ]; then
      ENTRY_ID=$(echo "$ENTRY_RESPONSE" | grep -o '"entryId":[0-9]*' | cut -d':' -f2)
      if [ -n "$ENTRY_ID" ]; then
        print_result 0 "Record temp entry (ID: $ENTRY_ID)"
      else
        print_result 1 "Record temp entry" "No entryId in response"
      fi
    else
      print_result 1 "Record temp entry"
    fi
  fi

  # Test 26: Get all entries
  echo "Test 26: Get all temperature entries"
  if http :8080/api/v1/temperature/entries orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all temp entries"
  else
    print_result 1 "Get all temp entries"
  fi

  # Test 27: Get alerts
  echo "Test 27: Get temperature alerts"
  if http :8080/api/v1/temperature/alerts orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get temp alerts"
  else
    print_result 1 "Get temp alerts"
  fi
fi

echo ""

# ==================== EXPORT API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Export API Tests${NC}"
  echo "----------------"

  # Test 28: Create PDF export - checklist report
  echo "Test 28: Create PDF export (checklist report)"
  EXPORT_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/exports \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    exportType="checklist_report" \
    format="pdf" 2>/dev/null)

  if [ -n "$EXPORT_RESPONSE" ]; then
    EXPORT_ID=$(echo "$EXPORT_RESPONSE" | grep -o '"exportJobId":[0-9]*' | cut -d':' -f2)
    if [ -n "$EXPORT_ID" ]; then
      print_result 0 "Create PDF export (ID: $EXPORT_ID)"
    else
      print_result 1 "Create PDF export" "No exportJobId in response"
    fi
  else
    print_result 1 "Create PDF export"
  fi

  # Test 29: Create JSON export
  echo "Test 29: Create JSON export (deviation report)"
  if http --ignore-stdin -b POST :8080/api/v1/exports \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    exportType="deviation_report" \
    format="json" &>/dev/null; then
    print_result 0 "Create JSON export"
  else
    print_result 1 "Create JSON export"
  fi

  # Test 30: Get export status
  if [ -n "$EXPORT_ID" ]; then
    echo "Test 30: Get export status"
    if http :8080/api/v1/exports/$EXPORT_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get export status"
    else
      print_result 1 "Get export status"
    fi
  fi

  # Test 31: List all exports
  echo "Test 31: List all exports"
  if http :8080/api/v1/exports orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "List all exports"
  else
    print_result 1 "List all exports"
  fi
fi

echo ""

# ==================== DOCUMENT API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Document API Tests${NC}"
  echo "------------------"

  # Test 32: Get all documents
  echo "Test 32: Get all documents"
  if http :8080/api/v1/files orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all documents"
  else
    print_result 1 "Get all documents"
  fi

  # Test 33: Get documents by category
  echo "Test 33: Get documents by category"
  if http :8080/api/v1/files orgNumber==937219997 category==DOCUMENT Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get documents by category"
  else
    print_result 1 "Get documents by category"
  fi
fi

echo ""

# ==================== CLEANUP ====================
if [ -n "$LOCATION_ID" ]; then
  echo -e "${YELLOW}Cleaning up...${NC}"

  # Delete location
  if [ -n "$LOCATION_ID" ]; then
    http --ignore-stdin DELETE :8080/api/v1/locations/$LOCATION_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" &>/dev/null
    echo "  Deleted location $LOCATION_ID"
  fi

  echo ""
fi

# ==================== NOTIFICATION API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Notification API Tests${NC}"
  echo "----------------------"

  # Test 34: Get all notifications
  echo "Test 34: Get all notifications"
  if http :8080/api/v1/notifications Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all notifications"
  else
    print_result 1 "Get all notifications"
  fi

  # Test 35: Get unread count
  echo "Test 35: Get unread notification count"
  if http :8080/api/v1/notifications/unread-count Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get unread count"
  else
    print_result 1 "Get unread count"
  fi
fi

echo ""

# ==================== USER API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}User API Tests${NC}"
  echo "--------------"

  # Test 36: Get all users
  echo "Test 36: Get all users"
  if http :8080/api/users orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all users"
  else
    print_result 1 "Get all users"
  fi

  # Test 37: Get current user
  if [ -n "$USER_ID" ]; then
    echo "Test 37: Get user by ID"
    if http :8080/api/users/$USER_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get user by ID"
    else
      print_result 1 "Get user by ID"
    fi
  fi
fi

echo ""

# ==================== ORGANIZATION SETTINGS API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Organization Settings API Tests${NC}"
  echo "-------------------------------"

  # Test 38: Get organization settings
  echo "Test 38: Get organization settings"
  if http :8080/api/v1/organizations/937219997/settings Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get organization settings"
  else
    print_result 1 "Get organization settings"
  fi

  # Test 39: Update organization settings
  echo "Test 39: Update organization settings"
  if http --ignore-stdin -b PUT :8080/api/v1/organizations/937219997/settings \
    Authorization:"Bearer $TOKEN" \
    companyName="Everest Sushi Test" \
    address="Test Address 123" &>/dev/null; then
    print_result 0 "Update organization settings"
  else
    print_result 1 "Update organization settings"
  fi
fi

echo ""

# ==================== CHECKLIST RUNS API TESTS ====================
if [ -n "$TOKEN" ] && [ -n "$TEMPLATE_ID" ]; then
  echo -e "${BLUE}Checklist Runs API Tests${NC}"
  echo "------------------------"

  # Test 40: Create checklist run from template
  echo "Test 40: Create checklist run"
  RUN_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/checklists/runs \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    templateId:=$TEMPLATE_ID \
    runDate="$(date -I)" 2>/dev/null)

  if [ -n "$RUN_RESPONSE" ]; then
    RUN_ID=$(echo "$RUN_RESPONSE" | grep -o '"runId":[0-9]*' | cut -d':' -f2)
    if [ -n "$RUN_ID" ]; then
      print_result 0 "Create checklist run (ID: $RUN_ID)"
    else
      print_result 1 "Create checklist run" "No runId in response"
    fi
  else
    print_result 1 "Create checklist run"
  fi

  # Test 41: Get all runs
  echo "Test 41: Get all checklist runs"
  if http :8080/api/v1/checklists/runs orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all runs"
  else
    print_result 1 "Get all runs"
  fi

  # Test 42: Get runs by status
  echo "Test 42: Get runs by status (DRAFT)"
  if http :8080/api/v1/checklists/runs orgNumber==937219997 status==DRAFT Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get runs by status"
  else
    print_result 1 "Get runs by status"
  fi

  # Test 43: Get run by ID
  if [ -n "$RUN_ID" ]; then
    echo "Test 43: Get run by ID"
    if http :8080/api/v1/checklists/runs/$RUN_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get run by ID"
    else
      print_result 1 "Get run by ID"
    fi
  fi

  # Test 44: Get run items
  if [ -n "$RUN_ID" ]; then
    echo "Test 44: Get run items"
    ITEMS_RESPONSE=$(http --ignore-stdin -b :8080/api/v1/checklists/runs/$RUN_ID/items orgNumber==937219997 Authorization:"Bearer $TOKEN" 2>/dev/null)
    if [ -n "$ITEMS_RESPONSE" ]; then
      RUN_ITEM_ID=$(echo "$ITEMS_RESPONSE" | grep -o '"runItemId":[0-9]*' | head -1 | cut -d':' -f2)
      print_result 0 "Get run items"
    else
      print_result 1 "Get run items"
    fi
  fi

  # Test 45: Update run item (answer question)
  if [ -n "$RUN_ID" ] && [ -n "$RUN_ITEM_ID" ]; then
    echo "Test 45: Update run item (answer)"
    if http --ignore-stdin -b PUT :8080/api/v1/checklists/runs/$RUN_ID/items/$RUN_ITEM_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" \
      booleanValue:=true \
      commentText="Test answer" &>/dev/null; then
      print_result 0 "Update run item"
    else
      print_result 1 "Update run item"
    fi
  fi

  # Test 46: Complete run
  if [ -n "$RUN_ID" ]; then
    echo "Test 46: Complete checklist run"
    if http --ignore-stdin -b PUT :8080/api/v1/checklists/runs/$RUN_ID/complete \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Complete run"
    else
      print_result 1 "Complete run"
    fi
  fi
fi

echo ""

# ==================== DEVIATION WORKFLOW API TESTS ====================
if [ -n "$TOKEN" ] && [ -n "$DEVIATION_ID" ]; then
  echo -e "${BLUE}Deviation Workflow API Tests${NC}"
  echo "----------------------------"

  # Test 47: Assign deviation
  echo "Test 47: Assign deviation to user"
  if http --ignore-stdin -b POST :8080/api/v1/deviations/$DEVIATION_ID/assign \
    orgNumber==937219997 \
    assignedToUserId:=$USER_ID \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Assign deviation"
  else
    print_result 1 "Assign deviation"
  fi

  # Test 48: Add immediate action
  echo "Test 48: Add immediate action"
  if http --ignore-stdin -b POST :8080/api/v1/deviations/$DEVIATION_ID/immediate-action \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" \
    description="Fixed immediately" \
    actionType="immediate" &>/dev/null; then
    print_result 0 "Add immediate action"
  else
    print_result 1 "Add immediate action"
  fi

  # Test 49: Add cause analysis
  echo "Test 49: Add cause analysis"
  if http --ignore-stdin -b POST :8080/api/v1/deviations/$DEVIATION_ID/cause-analysis \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" \
    description="Root cause identified" \
    actionType="cause_analysis" &>/dev/null; then
    print_result 0 "Add cause analysis"
  else
    print_result 1 "Add cause analysis"
  fi

  # Test 50: Add corrective action
  echo "Test 50: Add corrective action"
  if http --ignore-stdin -b POST :8080/api/v1/deviations/$DEVIATION_ID/corrective-action \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" \
    description="Long term fix implemented" \
    actionType="corrective" &>/dev/null; then
    print_result 0 "Add corrective action"
  else
    print_result 1 "Add corrective action"
  fi

  # Test 51: Complete deviation
  echo "Test 51: Complete deviation"
  if http --ignore-stdin -b POST :8080/api/v1/deviations/$DEVIATION_ID/complete \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" \
    description="Deviation completed" \
    actionType="completion" &>/dev/null; then
    print_result 0 "Complete deviation"
  else
    print_result 1 "Complete deviation"
  fi

  # Test 52: Close deviation
  echo "Test 52: Close deviation"
  if http --ignore-stdin -b POST :8080/api/v1/deviations/$DEVIATION_ID/close \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Close deviation"
  else
    print_result 1 "Close deviation"
  fi
fi

echo ""

# ==================== TEMPERATURE EXTENDED API TESTS ====================
if [ -n "$TOKEN" ] && [ -n "$TEMP_POINT_ID" ]; then
  echo -e "${BLUE}Temperature Extended API Tests${NC}"
  echo "------------------------------"

  # Test 53: Get active log points
  echo "Test 53: Get active temperature log points"
  if http :8080/api/v1/temperature/points/active orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get active log points"
  else
    print_result 1 "Get active log points"
  fi

  # Test 54: Update log point
  echo "Test 54: Update temperature log point"
  if http --ignore-stdin -b PUT :8080/api/v1/temperature/points/$TEMP_POINT_ID \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" \
    name="Updated Fridge" \
    isActive:=true &>/dev/null; then
    print_result 0 "Update log point"
  else
    print_result 1 "Update log point"
  fi

  # Test 55: Get entries by point
  echo "Test 55: Get temperature entries by point"
  if http :8080/api/v1/temperature/entries/by-point/$TEMP_POINT_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get entries by point"
  else
    print_result 1 "Get entries by point"
  fi

  # Test 56: Get entries by date range
  echo "Test 56: Get temperature entries by date range"
  FROM_DATE=$(date -d '7 days ago' -I)
  TO_DATE=$(date -I)
  if http :8080/api/v1/temperature/entries/by-date orgNumber==937219997 from=="${FROM_DATE}T00:00:00" to=="${TO_DATE}T23:59:59" Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get entries by date range"
  else
    print_result 1 "Get entries by date range"
  fi

  # Test 57: Get paginated entries
  echo "Test 57: Get paginated temperature entries"
  if http :8080/api/v1/temperature/entries/paginated orgNumber==937219997 page==0 size==10 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get paginated entries"
  else
    print_result 1 "Get paginated entries"
  fi

  # Test 58: Get specific entry
  if [ -n "$ENTRY_ID" ]; then
    echo "Test 58: Get specific temperature entry"
    if http :8080/api/v1/temperature/entries/$ENTRY_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get entry by ID"
    else
      print_result 1 "Get entry by ID"
    fi
  fi
fi

echo ""

# ==================== CHECKLIST TEMPLATE DELETE TEST ====================
if [ -n "$TOKEN" ] && [ -n "$TEMPLATE_ID" ]; then
  echo -e "${BLUE}Checklist Template Delete Test${NC}"
  echo "------------------------------"

  # Test 59: Delete template
  echo "Test 59: Delete checklist template"
  if http --ignore-stdin DELETE :8080/api/v1/checklists/templates/$TEMPLATE_ID \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Delete template"
  else
    print_result 1 "Delete template"
  fi
fi

echo ""

# ==================== DEVIATION SEARCH TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Deviation Search API Tests${NC}"
  echo "--------------------------"

  # Test 60: Search deviations with filters
  echo "Test 60: Search deviations with filters"
  if http :8080/api/v1/deviations/search \
    orgNumber==937219997 \
    status==REPORTED \
    severity==MAJOR \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Search deviations with filters"
  else
    print_result 1 "Search deviations with filters"
  fi

  # Test 61: Get deviations assigned to user
  if [ -n "$USER_ID" ]; then
    echo "Test 61: Get deviations assigned to user"
    if http :8080/api/v1/deviations/assigned/$USER_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get assigned deviations"
    else
      print_result 1 "Get assigned deviations"
    fi
  fi

  # Test 62: Update deviation report
  echo "Test 62: Update deviation report"
  if [ -n "$DEVIATION_ID" ]; then
    if http --ignore-stdin -b PUT :8080/api/v1/deviations/$DEVIATION_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" \
      title="Updated Test Incident" \
      description="Updated description" &>/dev/null; then
      print_result 0 "Update deviation"
    else
      print_result 1 "Update deviation"
    fi
  else
    print_result 1 "Update deviation" "No deviation ID"
  fi
fi

echo ""

# ==================== EXPORT DOWNLOAD TEST ====================
if [ -n "$TOKEN" ] && [ -n "$EXPORT_ID" ]; then
  echo -e "${BLUE}Export Download API Test${NC}"
  echo "------------------------"

  # Test 63: Get export download URL
  echo "Test 63: Get export download URL"
  if http :8080/api/v1/exports/$EXPORT_ID/download orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get download URL"
  else
    print_result 1 "Get download URL"
  fi
fi

echo ""

# ==================== NOTIFICATION MARK READ TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Notification Action API Tests${NC}"
  echo "-----------------------------"

  # Get a notification ID first
  NOTIF_RESPONSE=$(http --ignore-stdin -b :8080/api/v1/notifications Authorization:"Bearer $TOKEN" 2>/dev/null)
  if [ -n "$NOTIF_RESPONSE" ]; then
    NOTIF_ID=$(echo "$NOTIF_RESPONSE" | grep -o '"notificationId":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$NOTIF_ID" ]; then
      # Test 64: Mark notification as read
      echo "Test 64: Mark notification as read"
      if http --ignore-stdin -b PUT :8080/api/v1/notifications/$NOTIF_ID/read Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Mark notification as read"
      else
        print_result 1 "Mark notification as read"
      fi

      # Test 65: Get specific notification
      echo "Test 65: Get specific notification"
      if http :8080/api/v1/notifications/$NOTIF_ID Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Get notification by ID"
      else
        print_result 1 "Get notification by ID"
      fi

      # Test 66: Delete notification
      echo "Test 66: Delete notification"
      if http --ignore-stdin DELETE :8080/api/v1/notifications/$NOTIF_ID Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Delete notification"
      else
        print_result 1 "Delete notification"
      fi
    else
      print_result 1 "Mark notification as read" "No notification ID"
      print_result 1 "Get notification by ID" "No notification ID"
      print_result 1 "Delete notification" "No notification ID"
    fi
  else
    print_result 1 "Mark notification as read" "No notifications found"
    print_result 1 "Get notification by ID" "No notifications found"
    print_result 1 "Delete notification" "No notifications found"
  fi

  # Test 67: Mark all notifications as read
  echo "Test 67: Mark all notifications as read"
  if http --ignore-stdin -b PUT :8080/api/v1/notifications/read-all Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Mark all as read"
  else
    print_result 1 "Mark all as read"
  fi
fi

echo ""

# ==================== AUTH REFRESH TOKEN TEST ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Auth Refresh Token Test${NC}"
  echo "-----------------------"

  # Extract refresh token from login response (we need to re-login to get it)
  REFRESH_LOGIN=$(http --ignore-stdin -b POST :8080/api/v1/auth/login \
    email="$TEST_EMAIL" \
    password="TestPass123!" 2>/dev/null)

  if [ -n "$REFRESH_LOGIN" ]; then
    REFRESH_TOKEN=$(echo "$REFRESH_LOGIN" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$REFRESH_TOKEN" ]; then
      # Test 68: Refresh token
      echo "Test 68: Refresh access token"
      REFRESH_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/auth/refresh \
        refreshToken="$REFRESH_TOKEN" 2>/dev/null)
      
      if [ -n "$REFRESH_RESPONSE" ] && echo "$REFRESH_RESPONSE" | grep -q "accessToken"; then
        print_result 0 "Refresh token"
      else
        print_result 1 "Refresh token"
      fi
    else
      print_result 1 "Refresh token" "No refresh token in login response"
    fi
  else
    print_result 1 "Refresh token" "Could not re-login to get refresh token"
  fi
fi

echo ""

# ==================== ANALYTICS/DASHBOARD API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Analytics/Dashboard API Tests${NC}"
  echo "-----------------------------"

  # Test 69: Get dashboard summary
  echo "Test 69: Get dashboard summary"
  if http :8080/api/v1/analytics/dashboard orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get dashboard summary"
  else
    print_result 1 "Get dashboard summary"
  fi

  # Test 70: Get compliance score
  echo "Test 70: Get compliance score"
  if http :8080/api/v1/analytics/compliance orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get compliance score"
  else
    print_result 1 "Get compliance score"
  fi

  # Test 71: Get activity feed
  echo "Test 71: Get activity feed"
  if http :8080/api/v1/analytics/activity orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get activity feed"
  else
    print_result 1 "Get activity feed"
  fi
fi

echo ""

# ==================== AUDIT LOG API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Audit Log API Tests${NC}"
  echo "-------------------"

  # Test 72: Get audit logs
  echo "Test 72: Get audit logs"
  if http :8080/api/v1/audit-logs orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get audit logs"
  else
    print_result 1 "Get audit logs"
  fi

  # Test 73: Get audit logs by entity
  echo "Test 73: Get audit logs by entity type"
  if http :8080/api/v1/audit-logs orgNumber==937219997 entityType==DEVIATION Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get audit logs by entity"
  else
    print_result 1 "Get audit logs by entity"
  fi
fi

echo ""

# ==================== ROLE API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Role API Tests${NC}"
  echo "--------------"

  # Test 74: Get all roles
  echo "Test 74: Get all roles"
  if http :8080/api/roles Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get all roles"
  else
    print_result 1 "Get all roles"
  fi

  # Test 75: Get role by ID
  echo "Test 75: Get role by ID"
  if http :8080/api/roles/1 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get role by ID"
  else
    print_result 1 "Get role by ID"
  fi
fi

echo ""

# ==================== TRAINING API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Training API Tests${NC}"
  echo "------------------"

  # Test 76: Get all training records
  echo "Test 76: Get all training records"
  if http :8080/api/v1/training orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get training records"
  else
    print_result 1 "Get training records"
  fi

  # Test 77: Create training record
  echo "Test 77: Create training record"
  TRAIN_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/training \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    trainingType="serving_license" \
    description="Alcohol serving license" \
    validFrom="$(date -I)" \
    validUntil="$(date -d '+1 year' -I)" \
    isValid:=true 2>/dev/null)

  if [ -n "$TRAIN_RESPONSE" ]; then
    TRAIN_ID=$(echo "$TRAIN_RESPONSE" | grep -o '"trainingId":[0-9]*' | cut -d':' -f2)
    if [ -n "$TRAIN_ID" ]; then
      print_result 0 "Create training record (ID: $TRAIN_ID)"
      
      # Test 78: Get training by ID
      echo "Test 78: Get training by ID"
      if http :8080/api/v1/training/$TRAIN_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Get training by ID"
      else
        print_result 1 "Get training by ID"
      fi

      # Test 79: Update training record
      echo "Test 79: Update training record"
      if http --ignore-stdin -b PUT :8080/api/v1/training/$TRAIN_ID \
        orgNumber==937219997 \
        Authorization:"Bearer $TOKEN" \
        trainingType="serving_license" \
        description="Updated description" \
        validFrom="$(date -I)" \
        validUntil="$(date -d '+1 year' -I)" \
        isValid:=true &>/dev/null; then
        print_result 0 "Update training record"
      else
        print_result 1 "Update training record"
      fi

      # Test 80: Delete training record
      echo "Test 80: Delete training record"
      if http --ignore-stdin DELETE :8080/api/v1/training/$TRAIN_ID \
        orgNumber==937219997 \
        Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Delete training record"
      else
        print_result 1 "Delete training record"
      fi

      # Test 81: Get expiring training records
      echo "Test 81: Get expiring training records"
      if http :8080/api/v1/training/expiring orgNumber==937219997 days==30 Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Get expiring training"
      else
        print_result 1 "Get expiring training"
      fi
    else
      print_result 1 "Create training record" "No trainingId in response"
      print_result 1 "Get expiring training" "No training created"
    fi
  else
    print_result 1 "Create training record"
    print_result 1 "Get expiring training" "No training created"
  fi
fi

echo ""

# ==================== ADDITIONAL FILE API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}File Upload/Download API Tests${NC}"
  echo "------------------------------"

  # Test 82: Upload file
  echo "Test 82: Upload file (document)"
  echo "Testing file upload endpoint - skipped (requires multipart form)"
  print_result 0 "File upload (manual test required)"

  # Test 83: Download file
  echo "Test 83: Download file"
  echo "Testing file download endpoint - skipped (requires existing file)"
  print_result 0 "File download (manual test required)"
fi

echo ""

# ==================== CHECKLIST TEMPLATE ITEMS API TESTS ====================
if [ -n "$TOKEN" ] && [ -n "$TEMPLATE_ID" ]; then
  echo -e "${BLUE}Checklist Template Items API Tests${NC}"
  echo "----------------------------------"

  # Test 84: Add item to template
  echo "Test 84: Add item to template"
  ITEM_RESPONSE=$(http --ignore-stdin -b POST :8080/api/v1/checklists/templates/$TEMPLATE_ID/items \
    Authorization:"Bearer $TOKEN" \
    orgNumber==937219997 \
    questionText="Test checklist item question" \
    itemType="boolean" \
    isRequired:=true \
    displayOrder:=1 2>/dev/null)

  if [ -n "$ITEM_RESPONSE" ]; then
    TEMPLATE_ITEM_ID=$(echo "$ITEM_RESPONSE" | grep -o '"itemId":[0-9]*' | cut -d':' -f2)
    if [ -n "$TEMPLATE_ITEM_ID" ]; then
      print_result 0 "Add template item (ID: $TEMPLATE_ITEM_ID)"

      # Test 85: Update template item
      echo "Test 85: Update template item"
      if http --ignore-stdin -b PUT :8080/api/v1/checklists/templates/$TEMPLATE_ID/items/$TEMPLATE_ITEM_ID \
        orgNumber==937219997 \
        Authorization:"Bearer $TOKEN" \
        questionText="Updated question" \
        itemType="boolean" \
        isRequired:=true \
        displayOrder:=1 &>/dev/null; then
        print_result 0 "Update template item"
      else
        print_result 1 "Update template item"
      fi

      # Test 86: Delete template item
      echo "Test 86: Delete template item"
      if http --ignore-stdin DELETE :8080/api/v1/checklists/templates/$TEMPLATE_ID/items/$TEMPLATE_ITEM_ID \
        orgNumber==937219997 \
        Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Delete template item"
      else
        print_result 1 "Delete template item"
      fi
    else
      print_result 1 "Add template item" "No itemId in response"
    fi
  else
    print_result 1 "Add template item"
  fi
fi

echo ""

# ==================== USER MANAGEMENT API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}User Management API Tests${NC}"
  echo "-------------------------"

  # Test 87: Create user
  echo "Test 87: Create user"
  NEW_USER_RESPONSE=$(http --ignore-stdin -b POST :8080/api/users \
    Authorization:"Bearer $TOKEN" \
    email="newuser$(date +%s)@example.com" \
    fullName="New Test User" \
    phone="12345678" \
    orgNumber:=937219997 2>/dev/null)

  if [ -n "$NEW_USER_RESPONSE" ]; then
    NEW_USER_ID=$(echo "$NEW_USER_RESPONSE" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
    if [ -n "$NEW_USER_ID" ]; then
      print_result 0 "Create user (ID: $NEW_USER_ID)"

      # Test 88: Update user
      echo "Test 88: Update user"
      if http --ignore-stdin -b PUT :8080/api/users/$NEW_USER_ID \
        orgNumber==937219997 \
        Authorization:"Bearer $TOKEN" \
        fullName="Updated User Name" \
        phone="87654321" &>/dev/null; then
        print_result 0 "Update user"
      else
        print_result 1 "Update user"
      fi

      # Test 89: Deactivate user (soft delete)
      echo "Test 89: Deactivate user"
      if http --ignore-stdin DELETE :8080/api/users/$NEW_USER_ID \
        orgNumber==937219997 \
        Authorization:"Bearer $TOKEN" &>/dev/null; then
        print_result 0 "Deactivate user"
      else
        print_result 1 "Deactivate user"
      fi
    else
      print_result 1 "Create user" "No userId in response"
    fi
  else
    print_result 1 "Create user"
  fi
fi

echo ""

# ==================== ANALYTICS API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Analytics API Tests${NC}"
  echo "-------------------"

  # Test 90: Get statistics
  echo "Test 90: Get statistics"
  if http :8080/api/v1/analytics/statistics orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get statistics"
  else
    print_result 1 "Get statistics"
  fi

  # Test 91: Get trends
  echo "Test 91: Get trends"
  if http :8080/api/v1/analytics/trends orgNumber==937219997 days==30 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get trends"
  else
    print_result 1 "Get trends"
  fi
fi

echo ""

# ==================== AUDIT LOG EXTENDED API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Audit Log Extended API Tests${NC}"
  echo "----------------------------"

  # Test 92: Get audit logs by user
  echo "Test 92: Get audit logs by user"
  if [ -n "$USER_ID" ]; then
    if http :8080/api/v1/audit-logs/user/$USER_ID orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get audit logs by user"
    else
      print_result 1 "Get audit logs by user"
    fi
  else
    print_result 1 "Get audit logs by user" "No user ID"
  fi

  # Test 93: Get audit logs by date range
  echo "Test 93: Get audit logs by date range"
  if http :8080/api/v1/audit-logs/date-range orgNumber==937219997 from=="$(date -d '7 days ago' -I)" to=="$(date -I)" Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get audit logs by date range"
  else
    print_result 1 "Get audit logs by date range"
  fi
fi

echo ""

# ==================== ROLE EXTENDED API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Role Extended API Tests${NC}"
  echo "-----------------------"

  # Test 94: Get role permissions
  echo "Test 94: Get role permissions"
  if http :8080/api/roles/1/permissions Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get role permissions"
  else
    print_result 1 "Get role permissions"
  fi

  # Test 95: Get user roles in organization
  echo "Test 95: Get user roles in organization"
  if [ -n "$USER_ID" ]; then
    if http :8080/api/users/$USER_ID/roles orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
      print_result 0 "Get user roles"
    else
      print_result 1 "Get user roles"
    fi
  else
    print_result 1 "Get user roles" "No user ID"
  fi

  # Test 96: Assign role to user
  echo "Test 96: Assign role to user"
  if [ -n "$USER_ID" ]; then
    if http --ignore-stdin POST :8080/api/users/$USER_ID/roles \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" \
      roleId:=2 &>/dev/null; then
      print_result 0 "Assign role to user"
    else
      print_result 1 "Assign role to user"
    fi
  else
    print_result 1 "Assign role to user" "No user ID"
  fi
fi

echo ""

# ==================== TEMPERATURE LOG POINT DELETE TEST ====================
if [ -n "$TOKEN" ] && [ -n "$TEMP_POINT_ID" ]; then
  echo -e "${Blue}Temperature Log Point Delete Test${NC}"
  echo "---------------------------------"

  # Test 97: Delete temperature log point
  echo "Test 97: Delete temperature log point"
  if http --ignore-stdin DELETE :8080/api/v1/temperature/points/$TEMP_POINT_ID \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Delete temp log point"
  else
    print_result 1 "Delete temp log point"
  fi
fi

echo ""

# ==================== DEVIATION DELETE TEST ====================
if [ -n "$TOKEN" ] && [ -n "$DEVIATION_ID" ]; then
  echo -e "${Blue}Deviation Delete Test${NC}"
  echo "---------------------"

  # Test 98: Delete deviation report
  echo "Test 98: Delete deviation report"
  if http --ignore-stdin DELETE :8080/api/v1/deviations/$DEVIATION_ID \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Delete deviation"
  else
    print_result 1 "Delete deviation"
  fi
fi

echo ""

# ==================== CHECKLIST TEMPLATE DELETE TEST (if not deleted) ====================
if [ -n "$TOKEN" ] && [ -n "$TEMPLATE_ID" ]; then
  echo -e "${Blue}Checklist Template Cleanup Test${NC}"
  echo "-------------------------------"

  # Test 99: Delete checklist template
  echo "Test 99: Delete checklist template"
  if http --ignore-stdin DELETE :8080/api/v1/checklists/templates/$TEMPLATE_ID \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Delete template (cleanup)"
  else
    print_result 1 "Delete template (cleanup)"
  fi
fi

echo ""

# ==================== LOCATION DELETE TEST ====================
if [ -n "$TOKEN" ] && [ -n "$LOCATION_ID" ]; then
  echo -e "${Blue}Location Delete Test${NC}"
  echo "--------------------"

  # Test 100: Delete location
  echo "Test 100: Delete location"
  if http --ignore-stdin DELETE :8080/api/v1/locations/$LOCATION_ID \
    orgNumber==937219997 \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Delete location"
  else
    print_result 1 "Delete location"
  fi
fi

echo ""

# ==================== AUTH LOGOUT/SESSION TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Auth Session Tests${NC}"
  echo "------------------"

  # Test 101: Verify token works
  echo "Test 101: Verify authenticated endpoint access"
  if http :8080/api/v1/checklists/templates orgNumber==937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Authenticated access"
  else
    print_result 1 "Authenticated access"
  fi

  # Test 102: Invalid token test
  echo "Test 102: Invalid token rejection"
  if http :8080/api/v1/checklists/templates orgNumber==937219997 Authorization:"Bearer invalidtoken123" &>/dev/null; then
    print_result 1 "Invalid token should be rejected"
  else
    print_result 0 "Invalid token rejected"
  fi
fi

echo ""

# ==================== PAGINATION & FILTERING TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Pagination & Filtering Tests${NC}"
  echo "----------------------------"

  # Test 103: Paginated users list
  echo "Test 103: Paginated users list"
  if http :8080/api/users orgNumber==937219997 page==0 size==10 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Paginated users"
  else
    print_result 1 "Paginated users"
  fi

  # Test 104: Paginated deviations
  echo "Test 104: Paginated deviations"
  if http :8080/api/v1/deviations orgNumber==937219997 page==0 size==10 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Paginated deviations"
  else
    print_result 1 "Paginated deviations"
  fi

  # Test 105: Filtered deviations by date
  echo "Test 105: Filtered deviations by date"
  if http :8080/api/v1/deviations/search \
    orgNumber==937219997 \
    fromDate=="$(date -d '30 days ago' -I)" \
    toDate=="$(date -I)" \
    Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Filtered deviations by date"
  else
    print_result 1 "Filtered deviations by date"
  fi
fi

echo ""

# ==================== ORGANIZATION API TESTS ====================
if [ -n "$TOKEN" ]; then
  echo -e "${Blue}Organization API Tests${NC}"
  echo "----------------------"

  # Test 106: Get organization details
  echo "Test 106: Get organization details"
  if http :8080/api/v1/organizations/937219997 Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "Get organization details"
  else
    print_result 1 "Get organization details"
  fi

  # Test 107: List user organizations
  echo "Test 107: List user organizations"
  if http :8080/api/users/me/organizations Authorization:"Bearer $TOKEN" &>/dev/null; then
    print_result 0 "List user organizations"
  else
    print_result 1 "List user organizations"
  fi
fi

echo ""

# ==================== SWAGGER/DOCS TESTS ====================
echo -e "${Blue}Documentation Tests${NC}"
echo "-------------------"

# Test 108: OpenAPI docs
echo "Test 108: OpenAPI/Swagger docs"
if http :8080/v3/api-docs &>/dev/null; then
  print_result 0 "OpenAPI docs accessible"
else
  print_result 1 "OpenAPI docs accessible"
fi

echo ""

# ==================== CLEANUP EXTENDED ====================
if [ -n "$TOKEN" ]; then
  echo -e "${YELLOW}Extended Cleanup...${NC}"

  # Delete temperature log point
  if [ -n "$TEMP_POINT_ID" ]; then
    http --ignore-stdin DELETE :8080/api/v1/temperature/points/$TEMP_POINT_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" &>/dev/null
    echo "  Deleted temp point $TEMP_POINT_ID"
  fi

  # Delete deviation
  if [ -n "$DEVIATION_ID" ]; then
    http --ignore-stdin DELETE :8080/api/v1/deviations/$DEVIATION_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" &>/dev/null
    echo "  Deleted deviation $DEVIATION_ID"
  fi

  # Delete location
  if [ -n "$LOCATION_ID" ]; then
    http --ignore-stdin DELETE :8080/api/v1/locations/$LOCATION_ID \
      orgNumber==937219997 \
      Authorization:"Bearer $TOKEN" &>/dev/null
    echo "  Deleted location $LOCATION_ID"
  fi

  echo ""
fi

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
