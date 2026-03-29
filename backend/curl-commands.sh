#!/bin/bash
# script som brukes til å teste api med curl commandoer - kan like gjerne gjøres i postman
BASE_URL="http://localhost:8080/api"
PASS=0
FAIL=0

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "    JWT Authentication API Tests"
echo "========================================"
echo ""

print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
        ((PASS++))
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
        echo "  Error: $3"
        ((FAIL++))
    fi
}

TIMESTAMP=$(date +%s)
NEW_EMAIL="test${TIMESTAMP}@example.com"
NEW_PASSWORD="TestPass123!"
echo "Test 1: Register new user"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"${NEW_EMAIL}\", \"password\": \"${NEW_PASSWORD}\", \"fullName\": \"Test User\"}" 2>/dev/null)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "User registered: $NEW_EMAIL"
else
    print_result 1 "Registration failed" "HTTP $HTTP_CODE"
fi
echo ""

echo "Test 2: Login with registered user"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"${NEW_EMAIL}\", \"password\": \"${NEW_PASSWORD}\"}" 2>/dev/null)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Login successful"
else
    print_result 1 "Login failed" "HTTP $HTTP_CODE"
fi
echo ""

echo "Test 3: Swagger UI"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Swagger UI accessible"
else
    print_result 1 "Swagger not accessible" "HTTP $HTTP_CODE"
fi
echo ""

echo "========================================"
echo -e "${GREEN}Passed: $PASS${NC}"
echo -e "${RED}Failed: $FAIL${NC}"
echo "========================================"
