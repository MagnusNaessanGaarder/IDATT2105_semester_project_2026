#!/bin/bash

set -u

BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/v1"
ORG_NUMBER="937219997"
ADMIN_EMAIL="admin@everest-sushi.no"
ADMIN_PASSWORD="Test1234!"

PASS=0
FAIL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_result() {
  if [ "$1" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC}: $2"
    PASS=$((PASS + 1))
  else
    echo -e "${RED}FAIL${NC}: $2"
    if [ -n "${3:-}" ]; then
      echo "  Error: $3"
    fi
    FAIL=$((FAIL + 1))
  fi
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo -e "${RED}ERROR:${NC} Missing required command: $1"
    exit 1
  fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  API Tests with HTTPie${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

require_cmd http
require_cmd jq

echo -e "${YELLOW}Checking prerequisites...${NC}"
echo ""

echo -n "  Checking backend... "
if http --check-status --ignore-stdin GET "$BASE_URL/swagger-ui/index.html" >/dev/null 2>&1; then
  echo -e "${GREEN}RUNNING${NC}"
else
  echo -e "${RED}NOT RUNNING${NC}"
  echo ""
  echo -e "${RED}ERROR:${NC} Backend is not reachable at $BASE_URL"
  exit 1
fi

echo ""
echo -e "${BLUE}Authentication Tests${NC}"
echo "--------------------"

echo "Test 1: Login with admin user"
LOGIN_RESPONSE=$(http --ignore-stdin --check-status --body POST "$API_BASE/auth/login" \
  Content-Type:application/json \
  email="$ADMIN_EMAIL" \
  password="$ADMIN_PASSWORD" 2>/dev/null)

TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')

if [ -n "$TOKEN" ]; then
  print_result 0 "Admin login"
else
  print_result 1 "Admin login" "No access token received"
fi

echo "Test 2: Swagger UI accessibility"
if http --check-status --ignore-stdin GET "$BASE_URL/swagger-ui/index.html" >/dev/null 2>&1; then
  print_result 0 "Swagger UI accessible"
else
  print_result 1 "Swagger UI accessible"
fi

echo ""

if [ -n "$TOKEN" ]; then
  echo -e "${BLUE}Checklist API Tests${NC}"
  echo "-------------------"

  echo "Test 3: Get all templates"
  if http --check-status --ignore-stdin GET "$API_BASE/checklists/templates?orgNumber=$ORG_NUMBER" \
    Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
    print_result 0 "Get all templates"
  else
    print_result 1 "Get all templates"
  fi

  echo "Test 4: Get active templates"
  if http --check-status --ignore-stdin GET "$API_BASE/checklists/templates/active?orgNumber=$ORG_NUMBER" \
    Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
    print_result 0 "Get active templates"
  else
    print_result 1 "Get active templates"
  fi

  echo "Test 5: Get templates by module (FOOD)"
  if http --check-status --ignore-stdin GET "$API_BASE/checklists/templates/module/FOOD?orgNumber=$ORG_NUMBER" \
    Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
    print_result 0 "Get FOOD templates"
  else
    print_result 1 "Get FOOD templates"
  fi

  TEMPLATE_TITLE="HTTPie smoke $(date +%s)"
  echo "Test 6: Create template"
  TEMPLATE_RESPONSE=$(http --ignore-stdin --check-status --body POST "$API_BASE/checklists/templates?orgNumber=$ORG_NUMBER" \
    Authorization:"Bearer $TOKEN" \
    Content-Type:application/json \
    title="$TEMPLATE_TITLE" \
    description="HTTPie smoke test" \
    moduleType="FOOD" \
    frequency="DAILY" \
    items:='[]' 2>/dev/null)

  TEMPLATE_ID=$(printf '%s' "$TEMPLATE_RESPONSE" | jq -r '.templateId // empty')

  if [ -n "$TEMPLATE_ID" ]; then
    print_result 0 "Create template (ID: $TEMPLATE_ID)"
  else
    print_result 1 "Create template" "No templateId in response"
  fi

  if [ -n "$TEMPLATE_ID" ]; then
    echo "Test 7: Get template by ID"
    if http --check-status --ignore-stdin GET "$API_BASE/checklists/templates/$TEMPLATE_ID?orgNumber=$ORG_NUMBER" \
      Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
      print_result 0 "Get template by ID"
    else
      print_result 1 "Get template by ID"
    fi

    echo "Test 8: Update template"
    if http --check-status --ignore-stdin PUT "$API_BASE/checklists/templates/$TEMPLATE_ID?orgNumber=$ORG_NUMBER" \
      Authorization:"Bearer $TOKEN" \
      Content-Type:application/json \
      title="$TEMPLATE_TITLE updated" \
      description="Updated HTTPie smoke test" \
      moduleType="FOOD" \
      frequency="WEEKLY" \
      items:='[]' >/dev/null 2>&1; then
      print_result 0 "Update template"
    else
      print_result 1 "Update template"
    fi

    echo "Test 9: Delete template"
    if http --check-status --ignore-stdin DELETE "$API_BASE/checklists/templates/$TEMPLATE_ID?orgNumber=$ORG_NUMBER" \
      Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
      print_result 0 "Delete template"
    else
      print_result 1 "Delete template"
    fi
  fi

  echo ""
  echo -e "${BLUE}Deviation API Tests${NC}"
  echo "-------------------"

  echo "Test 10: Get all deviation reports"
  if http --check-status --ignore-stdin GET "$API_BASE/deviations?orgNumber=$ORG_NUMBER" \
    Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
    print_result 0 "Get all deviation reports"
  else
    print_result 1 "Get all deviation reports"
  fi

  DEVIATION_TITLE="HTTPie deviation $(date +%s)"
  echo "Test 11: Create deviation report"
  DEVIATION_RESPONSE=$(http --ignore-stdin --check-status --body POST "$API_BASE/deviations?orgNumber=$ORG_NUMBER" \
    Authorization:"Bearer $TOKEN" \
    Content-Type:application/json \
    reportType="INCIDENT" \
    severity="MAJOR" \
    title="$DEVIATION_TITLE" \
    description="HTTPie smoke test deviation" \
    locationText="Test location" \
    discoveredByName="HTTPie" \
    reportedToName="HTTPie" 2>/dev/null)

  DEVIATION_ID=$(printf '%s' "$DEVIATION_RESPONSE" | jq -r '.reportId // empty')

  if [ -n "$DEVIATION_ID" ]; then
    print_result 0 "Create deviation report (ID: $DEVIATION_ID)"
  else
    print_result 1 "Create deviation report" "No reportId in response"
  fi

  if [ -n "$DEVIATION_ID" ]; then
    echo "Test 12: Get deviation by ID"
    if http --check-status --ignore-stdin GET "$API_BASE/deviations/$DEVIATION_ID?orgNumber=$ORG_NUMBER" \
      Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
      print_result 0 "Get deviation by ID"
    else
      print_result 1 "Get deviation by ID"
    fi

    echo "Test 13: Delete deviation report"
    if http --check-status --ignore-stdin DELETE "$API_BASE/deviations/$DEVIATION_ID?orgNumber=$ORG_NUMBER" \
      Authorization:"Bearer $TOKEN" >/dev/null 2>&1; then
      print_result 0 "Delete deviation report"
    else
      print_result 1 "Delete deviation report"
    fi
  fi
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "Passed: ${GREEN}$PASS${NC}"
echo -e "Failed: ${RED}$FAIL${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
