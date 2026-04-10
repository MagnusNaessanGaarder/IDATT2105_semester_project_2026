#!/usr/bin/env bash
# seed-document.sh

# Uploads a sample PDF to the running backend so that the Documents view has
# at least one real, downloadable file after a fresh `make dev` run.
#
# Prerequisites: curl, python3
# Usage:
#   ./seed-document.sh
#   API_URL=http://localhost:8080/api/v1 \
#   EMAIL=admin@everest-sushi.no \
#   PASSWORD=Test1234! \
#   ORG_NUMBER=937219997 \
#   ./seed-document.sh

set -euo pipefail

API_URL="${API_URL:-http://localhost:8080/api/v1}"
EMAIL="${EMAIL:-admin@everest-sushi.no}"
PASSWORD="${PASSWORD:-Test1234!}"
ORG_NUMBER="${ORG_NUMBER:-937219997}"

TMPDIR_SEED="$(mktemp -d)"
trap 'rm -rf "$TMPDIR_SEED"' EXIT

PDF_PATH="$TMPDIR_SEED/HACCP-plan-2024.pdf"
LOG_PATH="$TMPDIR_SEED/response.json"

echo ""
echo "Document-seed for dev"
echo ""
echo "  API : $API_URL"
echo "  Org : $ORG_NUMBER"
echo "  User: $EMAIL"
echo ""

echo "[1/3] Genererer eksempel-PDF..."

python3 - "$PDF_PATH" << 'PYEOF'
import sys, textwrap, zlib, struct

out = sys.argv[1]


def pdf_string(s):
    return '(' + s.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)') + ')'

objects = []

def add_obj(content):
    objects.append(content)
    return len(objects)  # 1-based

# Object 1 — Catalog
catalog_id = add_obj(None)
# Object 2 — Pages
pages_id = add_obj(None)
# Object 3 — Font
font_id = add_obj(
    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
)
content_lines = [
    "BT",
    "/F1 18 Tf",
    "50 780 Td",
    "(HACCP-plan 2024) Tj",
    "/F1 12 Tf",
    "0 -30 Td",
    "(Everest Sushi & Fusion AS) Tj",
    "0 -20 Td",
    "(Dokument-type: POLICY) Tj",
    "0 -20 Td",
    "(Dette er et eksempeldokument for utvikling og testing.) Tj",
    "0 -20 Td",
    "(Kritiske kontrollpunkter:) Tj",
    "0 -18 Td",
    "(  CCP 1 - Kjoletemp fisk: 0-4 grader C, kontroll 2x daglig) Tj",
    "0 -18 Td",
    "(  CCP 2 - Kjoletemp kjott: 0-4 grader C, kontroll 2x daglig) Tj",
    "0 -18 Td",
    "(  CCP 3 - Serveringsbuffet: min 60 grader C, kontroll hver time) Tj",
    "0 -30 Td",
    "(Versjon: 2.1  |  Gyldighet: 2024-01-01 til 2024-12-31) Tj",
    "ET",
]
stream_data = "\n".join(content_lines).encode("latin-1")
content_id = add_obj(None)  # placeholder

page_id = add_obj(
    f"<< /Type /Page /Parent {pages_id} 0 R "
    f"/MediaBox [0 0 595 842] "
    f"/Contents {content_id} 0 R "
    f"/Resources << /Font << /F1 {font_id} 0 R >> >> >>"
)

objects[catalog_id - 1] = f"<< /Type /Catalog /Pages {pages_id} 0 R >>"
objects[pages_id - 1] = (
    f"<< /Type /Pages /Kids [{page_id} 0 R] /Count 1 >>"
)
objects[content_id - 1] = (
    f"<< /Length {len(stream_data)} >>\nstream\n"
    + stream_data.decode("latin-1")
    + "\nendstream"
)

lines = ["%PDF-1.4\n"]
offsets = []
for i, obj in enumerate(objects, 1):
    offsets.append(len("".join(lines).encode("latin-1")))
    lines.append(f"{i} 0 obj\n{obj}\nendobj\n")

xref_offset = len("".join(lines).encode("latin-1"))
lines.append("xref\n")
lines.append(f"0 {len(objects) + 1}\n")
lines.append("0000000000 65535 f \n")
for off in offsets:
    lines.append(f"{off:010d} 00000 n \n")
lines.append("trailer\n")
lines.append(f"<< /Size {len(objects) + 1} /Root {catalog_id} 0 R >>\n")
lines.append("startxref\n")
lines.append(f"{xref_offset}\n")
lines.append("%%EOF\n")

with open(out, "wb") as f:
    f.write("".join(lines).encode("latin-1"))

print(f"  PDF skrevet: {out} ({len(''.join(lines).encode('latin-1'))} bytes)")
PYEOF

echo "  ✓ PDF klar"
echo ""

echo "[2/3] Logger inn som $EMAIL..."

AUTH_RESPONSE=$(curl -sf -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  2>&1) || {
  echo "  ✗ Login failed. Is backend running? ($API_URL)"
  echo "    Error: $AUTH_RESPONSE"
  exit 1
}

ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])" 2>/dev/null) || {
  echo "  ✗ Could not retrieve ACCESS_TOKEN from reply:"
  echo "    $AUTH_RESPONSE"
  exit 1
}

echo "  ✓ Logged in (token: ${ACCESS_TOKEN:0:20}...)"
echo ""

echo "[3/3] Uploading document to $ORG_NUMBER..."

UPLOAD_RESPONSE=$(curl -sf -X POST "$API_URL/files/upload" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@$PDF_PATH;type=application/pdf" \
  -F "orgNumber=$ORG_NUMBER" \
  -F "documentType=POLICY" \
  -F "directory=documents" \
  2>&1) || {
  echo "  ✗ Upload failed:"
  echo "    $UPLOAD_RESPONSE"
  echo ""
  echo "  Is AZURE_STORAGE_CONNECTION_STRING set in .env"
  exit 1
}

DOC_ID=$(echo "$UPLOAD_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('documentId','?'))" 2>/dev/null || echo "?")

echo "  ✓ Dokument lastet opp!"
echo ""
echo "  document_id  : $DOC_ID"
echo "  org_number   : $ORG_NUMBER"
echo "  document_type: POLICY"
echo "  original_file: HACCP-plan-2024.pdf"
echo ""
echo "Full API-svar:"
echo "$UPLOAD_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$UPLOAD_RESPONSE"
echo ""
echo "Done!"
echo ""