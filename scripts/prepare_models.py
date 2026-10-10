#!/usr/bin/env python3
"""Build-time download only. Immutable revisions and SHA256 integrity are in models.lock.json."""
from model_pack import pack as pack_archive,decoded_chunks
import argparse,concurrent.futures,hashlib,json,pathlib,urllib.request,time,lzma,shutil
root=pathlib.Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser();parser.add_argument('--verify',action='store_true');args=parser.parse_args()
lock=json.loads((root/'models.lock.json').read_text());out=root/'app/src/main/assets/models'
allowed={entry['path'] for entry in lock['files']}|{entry['path']+'.xz' for entry in lock['files'] if entry.get('packaging')=='xz-shuffle4'}|{'manifest.json'}
if out.exists():
 for p in out.rglob('*'):
  if not p.is_file():continue
  if p.name.endswith('.part'):p.unlink() # Owned download staging, never an application asset.
  elif p.relative_to(out).as_posix() not in allowed:raise RuntimeError('Unexpected model asset: '+str(p))
def valid(p,entry):
 if not p.is_file():return False
 h=hashlib.sha256();size=0
 try:
  with p.open('rb') as r:
   for b in decoded_chunks(r,p.name.endswith('.xz')):h.update(b);size+=len(b)
 except (lzma.LZMAError,EOFError,ValueError):return False
 return size==entry['size'] and h.hexdigest()==entry['sha256']
def pack(p,entry):
 if entry.get('packaging')!='xz-shuffle4':return
 packed=p.with_suffix(p.suffix+'.xz');temp=packed.with_suffix(packed.suffix+'.part')
 if not valid(packed,entry):
  pack_archive(p,temp)
  temp.replace(packed)
  if not valid(packed,entry):raise RuntimeError('Packed model mismatch: '+entry['path'])
 p.unlink(missing_ok=True)
def prepare(entry):
 p=out/entry['path']
 if entry.get('packaging')=='xz-shuffle4' and valid(p.with_suffix(p.suffix+'.xz'),entry):p.unlink(missing_ok=True);return
 if valid(p,entry):pack(p,entry);return
 if args.verify:raise RuntimeError('Model missing or corrupt: '+entry['path']+'; run python3 scripts/prepare_models.py')
 p.parent.mkdir(parents=True,exist_ok=True);part=p.with_suffix(p.suffix+'.part')
 for attempt in range(4):
  try:
   with urllib.request.urlopen(entry['url'],timeout=120) as r,part.open('wb') as w:
    while b:=r.read(1024*1024):w.write(b)
   if not valid(part,entry):raise RuntimeError('Integrity mismatch: '+entry['path'])
   part.replace(p);pack(p,entry);return
  except Exception:
   part.unlink(missing_ok=True)
   if attempt==3:raise
   time.sleep(2)
with concurrent.futures.ThreadPoolExecutor(4) as pool:list(pool.map(prepare,lock['files']))
out.mkdir(parents=True,exist_ok=True);(out/'manifest.json').write_text(json.dumps(lock,indent=2)+'\n')
print('Models verified:',len(lock['files']),';',sum(x['size'] for x in lock['files']),'bytes')
