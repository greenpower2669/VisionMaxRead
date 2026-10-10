#!/usr/bin/env python3
import hashlib,json,pathlib,subprocess,sys,zipfile,os
root=pathlib.Path(__file__).resolve().parent.parent
apk=pathlib.Path(sys.argv[1]);sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))
tools=sorted((sdk/'build-tools').glob('*'))[-1]
result=subprocess.check_output([str(tools/'aapt'),'dump','permissions',str(apk)],text=True)
assert 'android.permission.INTERNET' not in result and 'android.permission.CAMERA' in result
subprocess.run([str(tools/'apksigner'),'verify',str(apk)],check=True)
with zipfile.ZipFile(apk) as z:
 for entry in json.loads((root/'models.lock.json').read_text())['files']:
  h=hashlib.sha256();count=0
  with z.open('assets/models/'+entry['path']) as r:
   while b:=r.read(1024*1024):h.update(b);count+=len(b)
  assert count==entry['size'] and h.hexdigest()==entry['sha256'],entry['path']
 libs=[p for p in z.namelist() if p.startswith('lib/') and p.endswith('.so')]
 assert libs and all(p.startswith('lib/arm64-v8a/') for p in libs)
print('APK verified: signed, ARM64, all model hashes match, no INTERNET permission')
