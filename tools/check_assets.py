"""Check publication hashes, mechanical integrity and APK hash without rendering."""
from pathlib import Path
import hashlib,json,sys
ROOT=Path(__file__).resolve().parents[1];errors=[];count=0
def check(path,want):
 global count
 count+=1;p=ROOT/path
 if not p.is_file():errors.append('Missing '+path);return
 if hashlib.sha256(p.read_bytes()).hexdigest().lower()!=want.lower():errors.append('Hash mismatch '+path)
for item in json.loads((ROOT/'mechanical/source-map.json').read_text(encoding='utf-8')):check('mechanical/'+item['file'],item['sha256'])
for item in json.loads((ROOT/'hardware/source-manifest.json').read_text(encoding='utf-8')):check('hardware/'+item['file'],item['sha256'])
for item in json.loads((ROOT/'media/manifest.json').read_text(encoding='utf-8'))['assets']:check(item['file'],item['sha256'])
for p in (ROOT/'releases/android').glob('*.apk.sha256'):
 values=p.read_text().split();check('releases/android/'+values[1],values[0])
for e in errors:print(e)
print(f'{count} asset checks; {len(errors)} errors');sys.exit(bool(errors))
