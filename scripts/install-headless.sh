#!/usr/bin/env bash
set -euo pipefail
# Optional Debian x86_64 graphical test tooling, extracted locally without root.
base="${MINEPAL_TOOLCHAINS:-/workspace/toolchains}"
apt_dir="$base/apt"
target="$base/xvfb"
mkdir -p "$apt_dir/lists/partial" "$apt_dir/cache/archives/partial" "$target"
printf '%s\n' 'deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://deb.debian.org/debian trixie main' > "$apt_dir/sources.list"
apt_options=(-o Dir::Etc::Parts=- -o "Dir::Etc::sourcelist=$apt_dir/sources.list" -o Dir::Etc::sourceparts=- -o "Dir::State::Lists=$apt_dir/lists" -o "Dir::Cache=$apt_dir/cache")
/usr/bin/apt-get "${apt_options[@]}" update
cd "$target"
/usr/bin/apt-get "${apt_options[@]}" download xvfb mesa-vulkan-drivers mesa-utils-bin
for package in ./*.deb; do dpkg-deb -x "$package" "$target"; done
export MINEPAL_HEADLESS_ROOT="$target"
python3 - <<'PY'
import pathlib,os,json
p=pathlib.Path(os.environ['MINEPAL_HEADLESS_ROOT'])
d=json.loads((p/'usr/share/vulkan/icd.d/lvp_icd.json').read_text())
d['ICD']['library_path']=str(p/'usr/lib/x86_64-linux-gnu/libvulkan_lvp.so')
(p/'lvp-local.json').write_text(json.dumps(d))
PY
