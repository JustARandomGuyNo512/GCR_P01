#!/usr/bin/env python3
from pathlib import Path
import json, sys, re
root = Path(__file__).resolve().parents[1]
java=list((root/'src/main/java').rglob('*.java'))
b=(root/'build.gradle').read_text()
m=(root/'src/main/resources/gcr.mixins.json').read_text()
assert len(java)==434, f'Unexpected Java count: {len(java)}'
assert 'lowdraglib2-unofficial-port-1641643:8945777' in b
assert 'kotlin-for-forge-351264:7291067' in b
assert 'reobf { shadowJar {} }' in b
assert 'IrisShaderCompatMixin' not in m
assert 'Ldlib2DebugMixin' not in m
assert 'oculus' not in b.lower().split('dependencies {',1)[1].split('jmeBundled(',1)[0].replace('no oculus','')
assert 'gradle --no-daemon --stacktrace clean build' in (root/'tools/check-prerequisites.sh').read_text()
print(json.dumps({'stage5_build_preflight':'PASS','java_files':len(java), 'full_gradle_build':'NOT_RUN'},indent=2))
