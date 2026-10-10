#!/usr/bin/env python3
"""Build-time download only. Immutable revisions and SHA256 integrity are in models.lock.json."""
import argparse,concurrent.futures,hashlib,json,pathlib,urllib.request,time
root=pathlib.Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser();parser.add_argument('--verify',action='store_true');args=parser.parse_args()
lock=json.loads((root/'models.lock.json').read_text());out=root/'app/src/main/assets/models'
def valid(p,entry):
 if not p.is_file() or p.stat().st_size!=entry['size']:return False
 h=hashlib.sha256()
 with p.open('rb') as r:
  while b:=r.read(1024*1024):h.update(b)
 return h.hexdigest()==entry['sha256']
def prepare(entry):
 p=out/entry['path']
 if valid(p,entry):return
 if args.verify:raise RuntimeError('Model missing or corrupt: '+entry['path']+'; run python3 scripts/prepare_models.py')
 p.parent.mkdir(parents=True,exist_ok=True);part=p.with_suffix(p.suffix+'.part')
 for attempt in range(4):
  try:
   with urllib.request.urlopen(entry['url'],timeout=120) as r,part.open('wb') as w:
    while b:=r.read(1024*1024):w.write(b)
   if not valid(part,entry):raise RuntimeError('Integrity mismatch: '+entry['path'])
   part.replace(p);return
  except Exception:
   part.unlink(missing_ok=True)
   if attempt==3:raise
   time.sleep(2)
with concurrent.futures.ThreadPoolExecutor(4) as pool:list(pool.map(prepare,lock['files']))
out.mkdir(parents=True,exist_ok=True);(out/'manifest.json').write_text(json.dumps(lock,indent=2)+'\n')
print('Models verified:',len(lock['files']),';',sum(x['size'] for x in lock['files']),'bytes')
