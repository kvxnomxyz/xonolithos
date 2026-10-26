#!/usr/bin/env python3
"""Publish a small, reviewable build-status snapshot without committing ROMs."""
from datetime import datetime, timezone
from pathlib import Path
import hashlib
import json

root = Path(__file__).resolve().parents[1]
state = root / '.state'
lines = [
    '# Automated build status',
    '',
    f'Updated: {datetime.now(timezone.utc).isoformat()}',
    '',
    'ROM binaries stay in local `artifacts/` storage; this page publishes results and checksums only.',
    '',
]
for name, label in (('samsung', 'Samsung SM-T725 (`gts4lv`)'), ('lavender', 'Xiaomi Redmi Note 7 (`lavender`)')):
    exit_file = state / f'unattended-{name}.exit'
    if not exit_file.exists() and name == 'lavender':
        exit_file = state / 'lavender-build.exit'
    result = exit_file.read_text().strip() if exit_file.exists() else 'running'
    status_file = state / ('status.json' if name == 'samsung' else 'lavender-status.json')
    detail = ''
    if status_file.exists():
        try:
            data = json.loads(status_file.read_text())
            detail = f" — {data.get('stage', '')}: {data.get('detail', '')}"
        except Exception:
            pass
    lines.append(f'- **{label}:** exit `{result}`{detail}')

for path in sorted((root / 'artifacts').glob('*.sha256')):
    digest = path.read_text().strip()
    lines.append(f'- `{path.name}`: `{digest}`')

(root / 'docs/BUILD-STATUS.md').write_text('\n'.join(lines) + '\n')
