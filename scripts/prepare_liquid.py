#!/usr/bin/env python3
"""Build-only pinned LiquidAI LFM2.5-230M QAD GGUF. No runtime network access."""
import argparse,hashlib,json,pathlib,urllib.request
ROOT=pathlib.Path(__file__).resolve().parent.parent
ASSETS=ROOT/"app/src/main/assets/liquid"
NAME="LFM2.5-230M-QAD-Q4_0.gguf"
SOURCE="https://huggingface.co/LiquidAI/LFM2.5-230M-GGUF/resolve/main/"+NAME+"?download=true"
EXPECTED_SHA="e75f83268de11b2a1bcfab5f3b5c5c0c97569ddbbc0990aad88437e45b8ba292"
MIN_BYTES=130_000_000
MAX_BYTES=175_000_000
def digest(path):
 h=hashlib.sha256()
 with path.open("rb") as f:
  while part:=f.read(1024*1024):h.update(part)
 return h.hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument("--verify",action="store_true");args=ap.parse_args()
 path=ASSETS/NAME;manifest=ASSETS/"manifest.json"
 if args.verify:
  if not path.is_file() or digest(path)!=EXPECTED_SHA:raise SystemExit("Liquid QAD GGUF missing or checksum mismatch")
  meta=json.loads(manifest.read_text())
  if meta.get("sha256")!=EXPECTED_SHA or meta.get("size")!=path.stat().st_size:raise SystemExit("Liquid manifest invalid")
  print("LFM2.5-230M QAD Q4_0 verified",path.stat().st_size)
  return
 ASSETS.mkdir(parents=True,exist_ok=True)
 if not path.is_file() or digest(path)!=EXPECTED_SHA:
  part=path.with_suffix(".part");part.unlink(missing_ok=True)
  req=urllib.request.Request(SOURCE,headers={"User-Agent":"VisionMaxRead-VMR012-offline-build"})
  try:
   with urllib.request.urlopen(req,timeout=240) as source,part.open("wb") as output:
    while block:=source.read(1024*1024):
     output.write(block)
     if output.tell()>MAX_BYTES:raise ValueError("Oversize model")
   if not MIN_BYTES<part.stat().st_size<MAX_BYTES or digest(part)!=EXPECTED_SHA:raise ValueError("Unexpected GGUF SHA/size")
   part.replace(path)
  finally:part.unlink(missing_ok=True)
 manifest.write_text(json.dumps({"model":"LiquidAI/LFM2.5-230M-GGUF",
   "filename":NAME,"sha256":EXPECTED_SHA,"size":path.stat().st_size,
   "format":"GGUF-QAD-Q4_0","source":SOURCE.split("?")[0]},indent=2)+"\n")
 print("LFM2.5-230M QAD staged:",path.stat().st_size)
if __name__=="__main__":main()
