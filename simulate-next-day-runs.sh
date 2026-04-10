#!/bin/bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
API_BASE="$BASE_URL/api/v1"
ORG_NUMBER="${ORG_NUMBER:-937219997}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@everest-sushi.no}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Test1234!}"
MODULE_TYPE="ALCOHOL"
DAY_OFFSET="1"
RESET_MODE="0"
POSITIONAL_INDEX=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --reset)
      RESET_MODE="1"
      ;;
    --help|-h)
      echo "Usage: $0 [MODULE_TYPE] [DAY_OFFSET] [--reset]"
      echo "Examples:"
      echo "  $0"
      echo "  $0 ALCOHOL 1"
      echo "  $0 FOOD 2 --reset"
      exit 0
      ;;
    *)
      if [ "$POSITIONAL_INDEX" -eq 0 ]; then
        MODULE_TYPE="$1"
      elif [ "$POSITIONAL_INDEX" -eq 1 ]; then
        DAY_OFFSET="$1"
      else
        echo "Unknown argument: $1" >&2
        exit 1
      fi
      POSITIONAL_INDEX=$((POSITIONAL_INDEX + 1))
      ;;
  esac
  shift
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd jq
require_cmd python3

if [ -f ".env" ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

TARGET_DATE="$(python3 - "$DAY_OFFSET" <<'PY'
from datetime import date, timedelta
import sys

offset = int(sys.argv[1])
print((date.today() + timedelta(days=offset)).isoformat())
PY
)"

LOGIN_RESPONSE="$(curl -sS -X POST "$API_BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")" || {
  echo "Backend is not reachable at $BASE_URL" >&2
  exit 1
}

TOKEN="$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')"

if [ -z "$TOKEN" ]; then
  echo "Login failed. No access token received." >&2
  exit 1
fi

AUTH_HEADER="Authorization: Bearer $TOKEN"

run_mysql_sql() {
  local sql="$1"

  if docker ps --format '{{.Names}}' | grep -qx 'backend-mysql-1'; then
    docker exec -i \
      -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-}" \
      backend-mysql-1 \
      mysql -uroot "${MYSQL_DATABASE:-internal_control}" -e "$sql"
    return
  fi

  if command -v mysql >/dev/null 2>&1; then
    MYSQL_PWD="${SPRING_DATASOURCE_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}" \
      mysql \
      -h "${MYSQL_HOST:-127.0.0.1}" \
      -P "${MYSQL_PORT:-3306}" \
      -u "${SPRING_DATASOURCE_USERNAME:-root}" \
      "${MYSQL_DATABASE:-internal_control}" \
      -e "$sql"
    return
  fi

  echo "Unable to reset runs: neither Docker container nor mysql client is available." >&2
  exit 1
}

mysql_query() {
  local sql="$1"

  if docker ps --format '{{.Names}}' | grep -qx 'backend-mysql-1'; then
    docker exec -i \
      -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-}" \
      backend-mysql-1 \
      mysql -N -B -uroot "${MYSQL_DATABASE:-internal_control}" -e "$sql"
    return
  fi

  if command -v mysql >/dev/null 2>&1; then
    MYSQL_PWD="${SPRING_DATASOURCE_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}" \
      mysql \
      -N -B \
      -h "${MYSQL_HOST:-127.0.0.1}" \
      -P "${MYSQL_PORT:-3306}" \
      -u "${SPRING_DATASOURCE_USERNAME:-root}" \
      "${MYSQL_DATABASE:-internal_control}" \
      -e "$sql"
    return
  fi

  echo "Unable to query mysql: neither Docker container nor mysql client is available." >&2
  exit 1
}

TEMPLATES="$(curl -sS "$API_BASE/checklists/templates/module/$MODULE_TYPE?orgNumber=$ORG_NUMBER" \
  -H "$AUTH_HEADER")"

RUNS="$(curl -sS "$API_BASE/checklists/runs?orgNumber=$ORG_NUMBER" \
  -H "$AUTH_HEADER")"

echo "Target date: $TARGET_DATE"
echo "Module: $MODULE_TYPE"
echo "Org: $ORG_NUMBER"
echo "Reset mode: $RESET_MODE"

if [ "$RESET_MODE" = "1" ]; then
  echo "Resetting existing runs for $MODULE_TYPE on $TARGET_DATE..."
  run_mysql_sql "
    DELETE cri
    FROM checklist_run_item cri
    JOIN checklist_run cr ON cr.run_id = cri.run_id
    JOIN checklist_template ct ON ct.template_id = cr.template_id
    WHERE cr.org_number = $ORG_NUMBER
      AND cr.run_date = '$TARGET_DATE'
      AND ct.module_type = '$MODULE_TYPE';

    DELETE cr
    FROM checklist_run cr
    JOIN checklist_template ct ON ct.template_id = cr.template_id
    WHERE cr.org_number = $ORG_NUMBER
      AND cr.run_date = '$TARGET_DATE'
      AND ct.module_type = '$MODULE_TYPE';
  "

  RUNS="$(curl -sS "$API_BASE/checklists/runs?orgNumber=$ORG_NUMBER" \
    -H "$AUTH_HEADER")"
fi

CREATED=0

while IFS=$'\t' read -r template_id template_title; do
  [ -n "$template_id" ] || continue

  EXISTS="$(printf '%s' "$RUNS" | jq -r \
    --argjson templateId "$template_id" \
    --arg targetDate "$TARGET_DATE" \
    '[.[] | select(.templateId == $templateId and .runDate == $targetDate)] | length')"

  if [ "$EXISTS" != "0" ]; then
    echo "Existing run: templateId=$template_id title=$template_title"
    continue
  fi

  RESPONSE="$(curl -sS -X POST "$API_BASE/checklists/runs?orgNumber=$ORG_NUMBER" \
    -H "$AUTH_HEADER" \
    -H 'Content-Type: application/json' \
    -d "{\"templateId\":$template_id,\"runDate\":\"$TARGET_DATE\"}")"

  RUN_ID="$(printf '%s' "$RESPONSE" | jq -r '.runId // empty')"
  STATUS="$(printf '%s' "$RESPONSE" | jq -r '.status // empty')"

  if [ -z "$RUN_ID" ]; then
    echo "Failed to create run for templateId=$template_id" >&2
    echo "$RESPONSE" >&2
    exit 1
  fi

  SOURCE_RUN_ID="$(mysql_query "
    SELECT cr.run_id
    FROM checklist_run cr
    WHERE cr.org_number = $ORG_NUMBER
      AND cr.template_id = $template_id
      AND cr.run_date < '$TARGET_DATE'
    ORDER BY cr.run_date DESC, cr.run_id DESC
    LIMIT 1;
  ")"

  if [ -n "$SOURCE_RUN_ID" ]; then
    run_mysql_sql "
      UPDATE checklist_run target
      JOIN checklist_run source ON source.run_id = $SOURCE_RUN_ID
      SET target.status = source.status,
          target.completed_at = CASE
              WHEN source.completed_at IS NULL THEN NULL
              ELSE TIMESTAMP('$TARGET_DATE', TIME(source.completed_at))
          END,
          target.notes = source.notes,
          target.location_id = source.location_id,
          target.performed_by_user_id = source.performed_by_user_id,
          target.assigned_to_user_id = source.assigned_to_user_id,
          target.due_at = CASE
              WHEN source.due_at IS NULL THEN NULL
              ELSE TIMESTAMP('$TARGET_DATE', TIME(source.due_at))
          END
      WHERE target.run_id = $RUN_ID;

      UPDATE checklist_run_item target_item
      JOIN checklist_run_item source_item
        ON source_item.run_id = $SOURCE_RUN_ID
       AND source_item.template_item_id = target_item.template_item_id
      SET target_item.boolean_value = source_item.boolean_value,
          target_item.text_value = source_item.text_value,
          target_item.numeric_value = source_item.numeric_value,
          target_item.selected_choice = source_item.selected_choice,
          target_item.is_deviation = source_item.is_deviation,
          target_item.comment_text = source_item.comment_text
      WHERE target_item.run_id = $RUN_ID;
    "
  fi

  STATUS="$(mysql_query "SELECT status FROM checklist_run WHERE run_id = $RUN_ID;")"
  echo "Created run: templateId=$template_id runId=$RUN_ID status=$STATUS title=$template_title"
  CREATED=$((CREATED + 1))
done < <(
  printf '%s' "$TEMPLATES" | jq -r '.[] | select((.isActive // true) == true) | [.templateId, .title] | @tsv'
)

echo "Created total: $CREATED"
