#!/usr/bin/env python3
from model_pack import decoded_chunks
import hashlib,json,pathlib,subprocess,sys,zipfile,os,lzma
root=pathlib.Path(__file__).resolve().parent.parent
apk=pathlib.Path(sys.argv[1]);sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))
tools=sorted((sdk/'build-tools').glob('*'))[-1]
result=subprocess.check_output([str(tools/'aapt'),'dump','permissions',str(apk)],text=True)
if 'android.permission.INTERNET' in result:
 raise AssertionError('Offline privacy regression: INTERNET permission found in merged APK manifest:\\n'+result)
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
 liquid=json.loads(z.read('assets/liquid/manifest.json'))
 lm=z.read('assets/liquid/LFM2.5-230M-QAD-Q4_0.gguf')
 assert len(lm)==liquid['size'] and hashlib.sha256(lm).hexdigest()==liquid['sha256'], 'Liquid weight SHA mismatch'
 license=z.read('assets/liquid/LICENSE')
 assert len(license)==liquid['license_size'] and hashlib.sha256(license).hexdigest()==liquid['license_sha256'], 'LFM license missing or corrupt'
 assert len(z.read('assets/liquid/LLAMA_CPP_LICENSE'))>400, 'llama.cpp static library license missing'
 assert 'lib/arm64-v8a/libvmr_liquid.so' in z.namelist(), 'Missing native llama.cpp JNI'
 moe_manifest=json.loads(z.read('assets/moe/manifest.json'))
 assert 'lib/arm64-v8a/libvmr_moe.so' in z.namelist(), 'Missing true MoE Rust ARM64 library'
 model_hash=hashlib.sha256()
 model_size=0
 with z.open('assets/moe/hobbylm-1b-sft-3450-Q4_K_M.gguf') as reader:
  while True:
   block=reader.read(1024*1024)
   if not block:break
   model_hash.update(block);model_size+=len(block)
 assert model_size==moe_manifest['size'] and model_hash.hexdigest()==moe_manifest['sha256'], 'HobbyLM MoE GGUF mismatch'
 for license_file in ('LICENSE','NOTICE'):
  entry='assets/moe/'+license_file
  assert hashlib.sha256(z.read(entry)).hexdigest()==moe_manifest['files'][license_file]['sha256'], 'Missing model legal notices'
 assert b'Apache-2.0' in z.read('assets/moe/HOBBYLM_SOURCE_README.txt'), 'Missing source license grant'

 assert not any(p.startswith('assets/qwen/') for p in z.namelist()), 'Qwen assets must be absent'
 libs=[p for p in z.namelist() if p.startswith('lib/') and p.endswith('.so')]
 assert libs and all(p.startswith('lib/arm64-v8a/') for p in libs)
print('APK verified: signed, ARM64, all model hashes match (including Indra), no INTERNET permission')
