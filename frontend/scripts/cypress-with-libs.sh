#!/usr/bin/env bash
set -euo pipefail

PACK_ROOT="${HOME}/.local/cypress-libs"
UNPACKED_ROOT="${PACK_ROOT}/unpacked"
LIB_PATH="${UNPACKED_ROOT}/usr/lib/x86_64-linux-gnu"
SHARE_PATH="${UNPACKED_ROOT}/usr/share"

bootstrap_local_libs() {
  local required_lib="${LIB_PATH}/libwayland-egl.so.1"

  if [[ -f "${required_lib}" ]]; then
    return
  fi

  mkdir -p "${PACK_ROOT}" "${UNPACKED_ROOT}"

  # No-root bootstrap for WSL/CI machines where sudo apt install is unavailable.
  apt download \
    libgtk-3-0 \
    libgtk-3-common \
    libcolord2 \
    libgdk-pixbuf2.0-0 \
    libgtk2.0-0 \
    libgbm1 \
    libnss3 \
    libasound2 \
    libxss1 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxshmfence1 \
    libxrandr2 \
    libcups2 \
    libxkbcommon0 \
    libepoxy0 \
    libwayland-server0 \
    libwayland-client0 \
    libwayland-cursor0 \
    libwayland-egl1 \
    >/tmp/cypress-local-libs-bootstrap.log 2>&1

  local pkg
  for pkg in "${PACK_ROOT}"/*.deb; do
    dpkg-deb -x "${pkg}" "${UNPACKED_ROOT}"
  done
}

bootstrap_local_libs

if [[ -d "${LIB_PATH}" ]]; then
  export LD_LIBRARY_PATH="${LIB_PATH}:${LD_LIBRARY_PATH:-}"
fi

if [[ -d "${SHARE_PATH}" ]]; then
  export XDG_DATA_DIRS="${SHARE_PATH}:${XDG_DATA_DIRS:-/usr/local/share:/usr/share}"
fi

exec npx cypress "$@"
