#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${MINEPAL_JAVA_HOME:-${MINEPAL_TOOLCHAINS:-/workspace/toolchains}/jdk-25.0.5.0.2}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
 echo 'Java 25 toolchain missing. Run scripts/bootstrap.sh first.' >&2
 exit 1
fi
# Gradle needs explicit Java proxy properties; it does not read HTTPS_PROXY.
# Values here contain a proxy hostname/port only, never proxy credentials.
mkdir -p "$GRADLE_USER_HOME"
python3 - <<'PY'
import os,pathlib,urllib.parse
p=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
f=pathlib.Path(os.environ['GRADLE_USER_HOME'])/'gradle.properties'
if not f.exists():
    lines=[]
    if p.hostname:
        lines += [f'systemProp.https.proxyHost={p.hostname}',f'systemProp.https.proxyPort={p.port or 80}',f'systemProp.http.proxyHost={p.hostname}',f'systemProp.http.proxyPort={p.port or 80}']
    trust='/etc/ssl/certs/java/cacerts'
    if pathlib.Path(trust).exists():lines.append(f'systemProp.javax.net.ssl.trustStore={trust}')
    f.write_text('\n'.join(lines)+'\n')
PY
cd "$root"
exec ./gradlew "$@"
