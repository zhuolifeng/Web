"""Start only; never install dependencies. Requires Java 21+ and cached Maven artifacts; Java 24 is supported."""
import argparse
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def java_environment():
    env = os.environ.copy()
    candidates = [Path(env['JAVA_HOME'])] if env.get('JAVA_HOME') else []
    candidates += [Path('/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home'),
                   Path('/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home'),
                   Path('/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'),
                   Path('/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home')]
    selected_java = shutil.which('java')
    if selected_java:
        candidates.append(Path(selected_java).resolve().parent.parent)
    for candidate in candidates:
        java = candidate / 'bin/java'
        if not java.is_file():
            continue
        info = subprocess.run([str(java), '-version'], capture_output=True, text=True)
        version = re.search(r'version "(\d+)', info.stderr + info.stdout)
        if version and int(version.group(1)) >= 21:
            env['JAVA_HOME'] = str(candidate)
            env['PATH'] = str(candidate / 'bin') + os.pathsep + env.get('PATH', '')
            return env
    raise SystemExit('Java 21+ not found. Install it yourself: brew install openjdk@21; then rerun.\nOn other systems set JAVA_HOME to your JDK 21+ directory.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', action='store_true', help='Disable virtual threads for a benchmark control')
    parser.add_argument('--port', type=int, default=8081)
    parser.add_argument('--allow-java24', action='store_true', help='Compatibility flag; Java 21+ (including 24) is accepted by default')
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        raise SystemExit('Port must be 1–65535')
    env = java_environment()
    if not shutil.which('mvn', path=env['PATH']):
        raise SystemExit('Maven not found. Install it yourself: brew install maven')
    if not env.get('JWT_SECRET'):
        env['JWT_SECRET'] = secrets.token_urlsafe(48)
        print('Using an ephemeral JWT signing key (not printed or saved). Tokens expire on restart.', flush=True)
    env['VIRTUAL_THREADS_ENABLED'] = 'false' if args.platform else 'true'
    env['SERVER_PORT'] = str(args.port)
    print(f'Starting Java 21-target API on 127.0.0.1:{args.port}, virtual threads={env["VIRTUAL_THREADS_ENABLED"]}', flush=True)
    os.chdir(ROOT)
    # Offline ensures this launcher never downloads packages on the user's behalf.
    os.execvpe('mvn', ['mvn', '-o', 'spring-boot:run'], env)
