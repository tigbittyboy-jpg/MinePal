#!/usr/bin/env bash
set -euo pipefail
# Installs only local tooling; never changes system Java or requires root.
root="$(cd "$(dirname "$0")/.." && pwd)"
tools_dir="${MINEPAL_TOOLCHAINS:-/workspace/toolchains}"
mkdir -p "$tools_dir"
export MINEPAL_TOOLCHAINS="$tools_dir"
python3 - <<'PY'
import hashlib, os, pathlib, tarfile, tempfile, urllib.request
root=pathlib.Path(os.environ['MINEPAL_TOOLCHAINS'])
jdk=root/'jdk-25.0.5.0.2'
if not (jdk/'bin/java').exists():
    url='https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz'
    expected='d430d3f9e91435d2d58d366b1dabf14b23fcf8629ed4b041757c1be444055946'
    with tempfile.TemporaryDirectory() as temp:
        archive=pathlib.Path(temp)/'jdk.tar.gz'
        urllib.request.urlretrieve(url,archive)
        actual=hashlib.sha256(archive.read_bytes()).hexdigest()
        if actual!=expected:
            raise SystemExit('JDK download has changed. Obtain and verify the pinned Java 25.0.5.0.2 release; do not bypass checksum validation.')
        with tarfile.open(archive) as tf: tf.extractall(root,filter='data')
    if not (jdk/'bin/java').exists(): raise SystemExit('Unexpected JDK archive layout')
PY
"$root/scripts/dev.sh" --no-daemon build
