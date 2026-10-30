#!/usr/bin/env python3
from model_pack import decoded_chunks
import hashlib,json,pathlib,subprocess,sys,zipfile,os,lzma
root=pathlib.Path(__file__).resolve().parent.parent
apk=pathlib.Path(sys.argv[1]);sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))
tools=sorted((sdk/'build-tools').glob('*'))[-1]
result=subprocess.check_output([str(tools/'aapt'),'dump','permissions',str(apk)],text=True)
if 'android.permission.INTERNET' not in result:
 raise AssertionError('Automatic model installation needs INTERNET permission')
if 'android.permission.ACCESS_NETWORK_STATE' not in result:
 raise AssertionError('Wi-Fi-only model installer needs ACCESS_NETWORK_STATE')
if 'android.permission.CAMERA' not in result:
 raise AssertionError('Missing CAMERA permission in merged APK manifest:\\n'+result)
subprocess.run([str(tools/'apksigner'),'verify',str(apk)],check=True)
with zipfile.ZipFile(apk) as z:
 entries=json.loads((root/'models.lock.json').read_text())['files']
 expected={'assets/models/'+e['path']+('.xz' if e.get('packaging')=='xz-shuffle4' else '') for e in entries}|{'assets/models/manifest.json'}
 actual={p for p in z.namelist() if p.startswith('assets/models/') and not p.endswith('/')}
 assert actual==expected,'Unexpected or missing model assets: '+str(actual^expected)
 for entry in entries:
  h=hashlib.sha256();count=0
  with z.open('assets/models/'+entry['path']+('.xz' if entry.get('packaging')=='xz-shuffle4' else '')) as raw:
   for b in decoded_chunks(raw,entry.get('packaging')=='xz-shuffle4'):h.update(b);count+=len(b)
  assert count==entry['size'] and h.hexdigest()==entry['sha256'],entry['path']
 indra_manifest=json.loads(z.read('assets/indra/manifest.json'))
 assert indra_manifest['revision']=='bd02167fdf2005a19962cc15188f8496a2c87d3b'
 for name in ('det.onnx','rec.onnx','order.onnx','lm.npz','meta.json','LICENSE'):
  raw=z.read('assets/indra/'+name)
  meta=indra_manifest[name]
  assert len(raw)==meta['size'] and hashlib.sha256(raw).hexdigest()==meta['sha256'], 'Indra '+name
 assert not any(p.startswith(('assets/liquid/','assets/moe/','assets/qwen/')) for p in z.namelist()), 'Generative LLM weights still bundled'
 assert not any(p.endswith(('libvmr_liquid.so','libvmr_moe.so')) for p in z.namelist()), 'Legacy generative LLM backend bundled'
 assert b'MIT License' in z.read('assets/licenses/llama.cpp-MIT.txt'), 'Pinned llama.cpp MIT notice is missing'
 executable='lib/arm64-v8a/libvmr_mtmd_cli.so'
 assert executable in z.namelist(), 'Missing actual ARM64 image OCR runtime'
 runtime=z.getinfo(executable)
 assert runtime.file_size>1024*1024, 'Native executable unexpectedly tiny'
 with z.open(executable) as elf:
  assert elf.read(4)==bytes([0x7f,0x45,0x4c,0x46]), 'Not a native ELF binary'
 libs=[p for p in z.namelist() if p.startswith('lib/') and p.endswith('.so')]
 assert libs and all(p.startswith('lib/arm64-v8a/') for p in libs)
print('APK verified: signed, ARM64, bundled Indra hashes, model-only Wi-Fi installer declared; no cleartext traffic')
