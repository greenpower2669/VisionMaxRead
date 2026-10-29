#!/usr/bin/env python3
"""Run JVM-only test of Indra NPZ backoff and CTC, no network nor numpy."""
import struct,subprocess,tempfile,zipfile
from pathlib import Path
root=Path(__file__).resolve().parent.parent
def npy(dtype,vals,kind='q'):
    raw=struct.pack('<'+str(len(vals))+kind,*vals)
    hdr="{'descr': '"+dtype+"', 'fortran_order': False, 'shape': ("+str(len(vals))+",), }"
    n=(16-((10+len(hdr)+1)%16))%16
    head=(hdr+' '*n+'\n').encode('ascii')
    return b'\x93NUMPY'+bytes((1,0))+struct.pack('<H',len(head))+head+raw
def fixture(f):
    with zipfile.ZipFile(f,'w',compression=zipfile.ZIP_DEFLATED) as z:
        for name,values,dtype,kind in [
          ('order',[2],'<i8','q'),('bits',[11],'<i8','q'),('vocab_size',[3],'<i8','q'),
          ('k0',[1,2,3],'<u8','Q'),('v0',[-2.0,-.4,-1.5],'<f4','f'),
          ('hk0',[0],'<u8','Q'),('hb0',[0.0],'<f4','f'),
          ('k1',[(1<<11)|2],'<u8','Q'),('v1',[-.01],'<f4','f'),
          ('hk1',[1],'<u8','Q'),('hb1',[-.15],'<f4','f')
        ]:z.writestr(name+'.npy',npy(dtype,values,kind))
with tempfile.TemporaryDirectory(prefix='vmr11-lm-') as tmp:
    f=Path(tmp)/'fake.npz'
    fixture(f)
    files=[root/'app/src/main/java/fr/visionmaxread'/n for n in ('IndraNgramLM.java','IndraCtcBeam.java')]
    files.append(root/'tests/java/fr/visionmaxread/IndraLmOracle.java')
    subprocess.run(['javac','-encoding','UTF-8','-d',tmp]+list(map(str,files)),check=True)
    subprocess.run(['java','-cp',tmp,'fr.visionmaxread.IndraLmOracle',str(f)],check=True)
print('VMR-011 NPZ source parser and language decoding: PASS')
