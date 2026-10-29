#!/usr/bin/env python3
"""Build-only Indra Latin preview. No Android network permission.
The Hugging Face revision is fixed; SHA256 manifests are generated at build time.
Expected upstream SHA256 pinning is an explicit VMR-009 follow-up.
"""
import argparse,hashlib,json,pathlib,urllib.request
ROOT=pathlib.Path(__file__).resolve().parent.parent
ASSETS=ROOT/"app/src/main/assets/indra"
REVISION="bd02167fdf2005a19962cc15188f8496a2c87d3b"
BASE=f"https://huggingface.co/datalab-to/indra-ocr/resolve/{REVISION}"
FILES=("det.onnx","rec.onnx","meta.json","LICENSE")
def digest(path):
 h=hashlib.sha256()
 with path.open("rb") as stream:
  while part:=stream.read(1024*1024):h.update(part)
 return {"sha256":h.hexdigest(),"size":path.stat().st_size}
def main():
 parser=argparse.ArgumentParser();parser.add_argument("--verify",action="store_true");args=parser.parse_args()
 ASSETS.mkdir(parents=True,exist_ok=True)
 path=ASSETS/"manifest.json"
 if args.verify:
  if not path.is_file():raise SystemExit("Indra manifest absent")
  info=json.loads(path.read_text())
  if info.get("revision")!=REVISION:raise SystemExit("Bad Indra revision")
  for name in FILES:
   file=ASSETS/name
   if not file.is_file() or digest(file)!=info.get(name):raise SystemExit("Indra missing/corrupt: "+name)
  print("Indra Latin assets verified")
  return
 for name in FILES:
  file=ASSETS/name
  if file.is_file() and file.stat().st_size>100:continue
  tmp=ASSETS/(name+".part")
  req=urllib.request.Request(BASE+"/"+name+"?download=true",headers={"User-Agent":"VisionMaxRead-Indra-test"})
  try:
   with urllib.request.urlopen(req,timeout=180) as source,tmp.open("wb") as out:
    while buf:=source.read(1024*1024):out.write(buf)
   if tmp.stat().st_size<100:raise ValueError("Indra invalid file: "+name)
   tmp.replace(file)
  finally:tmp.unlink(missing_ok=True)
 info={"revision":REVISION,"origin":"datalab-to/indra-ocr","mode":"Latin-only prototype"}
 for name in FILES:info[name]=digest(ASSETS/name)
 path.write_text(json.dumps(info,indent=2)+"\n")
 print("Indra staged:",{name:info[name]["size"] for name in FILES})
if __name__=="__main__":main()
