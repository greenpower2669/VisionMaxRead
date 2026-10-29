#!/usr/bin/env python3
"""Pinned GGUF weights of the real HobbyLM-1B sparse MoE; build-only network."""
import argparse,hashlib,json,pathlib,urllib.request
ROOT=pathlib.Path(__file__).resolve().parent.parent
DEST=ROOT/"app/src/main/assets/moe"
MODEL="hobbylm-1b-sft-3450-Q4_K_M.gguf"
REV="f8bc034403026b2706f5275702d3801753b87c34"
SHA="0a1d1d4a7e545c51e8fdaa61707b5bf7d09cb4e948fc9b09c3ec2789c493cdbb"
BASE=f"https://huggingface.co/harims95/hobbylm-1B-gguf/resolve/{REV}/"
MIN=700_000_000
MAX=765_000_000
def checksum(path):
 h=hashlib.sha256()
 with path.open("rb") as f:
  while chunk:=f.read(1<<20):h.update(chunk)
 return h.hexdigest()
def download(name,expected=None):
 path=DEST/name
 if path.is_file() and (not expected or checksum(path)==expected):return path
 part=DEST/(name+".part");part.unlink(missing_ok=True)
 req=urllib.request.Request(BASE+name+"?download=true",
                            headers={"User-Agent":"VisionMaxRead-experimental-HobbyMoE"})
 try:
  with urllib.request.urlopen(req,timeout=240) as reader,part.open("wb") as out:
   while chunk:=reader.read(1<<20):
    out.write(chunk)
    if name==MODEL and out.tell()>MAX:raise ValueError("MoE file too large")
  if expected and checksum(part)!=expected:raise ValueError("MoE model checksum mismatch")
  part.replace(path)
 finally:part.unlink(missing_ok=True)
 return path
def main():
 p=argparse.ArgumentParser();p.add_argument("--verify",action="store_true");args=p.parse_args()
 model=DEST/MODEL
 if args.verify:
  j=json.loads((DEST/"manifest.json").read_text())
  assert model.exists() and model.stat().st_size==j["size"] and checksum(model)==SHA
  for fn in ("LICENSE","NOTICE"):
   f=DEST/fn
   assert f.exists() and checksum(f)==j["files"][fn]["sha256"]
  print("MOE MODEL verified, pinned public revision, real sparse experts",model.stat().st_size)
  return
 DEST.mkdir(parents=True,exist_ok=True)
 pth=download(MODEL,SHA)
 if not MIN<=pth.stat().st_size<=MAX:raise RuntimeError("HobbyLM-Q4 size outside expected band")
 files={}
 for name in ("LICENSE","NOTICE"):
  f=download(name)
  files[name]={"size":f.stat().st_size,"sha256":checksum(f)}
 (DEST/"manifest.json").write_text(json.dumps({
  "model":"harims95/hobbylm-1B-gguf",
  "format":"GGUF-Q4_K_M","revision":REV,"filename":MODEL,
  "sha256":SHA,"size":pth.stat().st_size,"model_architecture":"hobbylm",
  "active_experts_top_k":8,"routed_experts":64,"license":"Apache-2.0",
  "files":files},indent=2)+"\n")
 print("HobbyLM MoE staged",pth.stat().st_size)
if __name__=="__main__":main()
