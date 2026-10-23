#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
base="${MINEPAL_TOOLCHAINS:-/workspace/toolchains}"
if [[ ! -x "$base/xvfb/usr/bin/Xvfb" || ! -f "$base/xvfb/lvp-local.json" ]]; then
 echo 'Run scripts/install-headless.sh first.' >&2; exit 1
fi
export DISPLAY="${DISPLAY:-:99}"
export XDG_RUNTIME_DIR="$base/runtime"
export MESA_SHADER_CACHE_DIR="$base/mesa-cache"
export VK_DRIVER_FILES="$base/xvfb/lvp-local.json"
export LIBGL_ALWAYS_SOFTWARE=1
# Audio hardware is optional in graphical automated tests.
export ALSOFT_DRIVERS=null
mkdir -p "$XDG_RUNTIME_DIR" "$MESA_SHADER_CACHE_DIR"
chmod 700 "$XDG_RUNTIME_DIR"
x_pid=''
cleanup() { if [[ -n "$x_pid" ]]; then kill "$x_pid" 2>/dev/null || true; fi; }
trap cleanup EXIT
if [[ ! -S "/tmp/.X11-unix/X${DISPLAY#:}" ]]; then
 "$base/xvfb/usr/bin/Xvfb" "$DISPLAY" -screen 0 1280x720x24 -nolisten tcp -ac > "$base/xvfb/server.log" 2>&1 &
 x_pid=$!
 for attempt in {1..50}; do
  [[ -S "/tmp/.X11-unix/X${DISPLAY#:}" ]] && break
  sleep 0.1
 done
fi
if [[ $# -eq 0 ]]; then set -- --no-daemon runClientGameTest; fi
"$root/scripts/dev.sh" "$@"
if [[ -d "$root/build/run/clientGameTest/screenshots" ]]; then
 mkdir -p "$root/build/reports/companion-client"
 cp "$root"/build/run/clientGameTest/screenshots/*.png "$root/build/reports/companion-client/"
fi
