#!/usr/bin/env python3
"""CPU smoke tests using synthetic French text only, never user documents.
Requires pip install -r scripts/smoke-requirements.txt
This validates model contracts on desktop, not Android camera/audio or handwriting quality.
"""
import pathlib,json,unicodedata,gc,tempfile
from model_pack import decoded_chunks
import numpy as np
import onnxruntime as ort
from PIL import Image,ImageDraw,ImageFont
import yaml
from transformers import BertTokenizer
root=pathlib.Path(__file__).resolve().parent.parent/'app/src/main/assets/models'
options=ort.SessionOptions();options.intra_op_num_threads=2

temporary=tempfile.TemporaryDirectory()
def session(path):
 p=root/path
 if not p.exists() and p.with_suffix(p.suffix+'.xz').exists():
  target=pathlib.Path(temporary.name)/p.name
  with p.with_suffix(p.suffix+'.xz').open('rb') as r,target.open('wb') as w:
   for block in decoded_chunks(r,True):w.write(block)
  p=target
 return ort.InferenceSession(str(p),sess_options=options,providers=['CPUExecutionProvider'])
# Printed French recognition and detector activation on a clean synthetic page.
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',40)
line='Bonjour, été en français.'
box=font.getbbox(line);im=Image.new('RGB',(box[2]+16,58),'white');ImageDraw.Draw(im).text((8,0),line,font=font,fill='black')
rec=session('rec/inference.onnx');scaled=im.resize((int(np.ceil(48*im.width/im.height)),48))
x=(np.asarray(scaled,dtype=np.float32)/127.5-1).transpose(2,0,1)[None]
probs=rec.run(None,{'x':x})[0][0];chars=yaml.safe_load((root/'rec/inference.yml').read_text())['PostProcess']['character_dict'];chars=chars+([' '] if chars[-1]!=' ' else [])
indices=probs.argmax(-1);out='';prev=-1
for idx in indices:
 if idx and idx!=prev:out+=chars[idx-1]
 prev=idx
assert out==line,(out,line)
print('PASS OCR recognizer: exact French printed line with accents',flush=True);del rec;gc.collect()
page=Image.new('RGB',(960,384),'white');ImageDraw.Draw(page).text((80,100),line,font=font,fill='black')
det=session('det/inference.onnx');x=np.asarray(page,dtype=np.float32)[:,:,::-1]/255
x=(x-np.array([.485,.456,.406],np.float32))/np.array([.229,.224,.225],np.float32)
heat=det.run(None,{'x':x.transpose(2,0,1)[None]})[0]
assert float(heat.max())>.8 and int((heat>.6).sum())>100
print('PASS OCR detector: text activates on synthetic page',flush=True);del det;gc.collect()
# BERT vocabulary and French entity classification.
tokenizer=BertTokenizer(vocab_file=str(root/'ner/vocab.txt'),do_lower_case=False)
sentence='Jean Dupont habite à Paris.';inputs=tokenizer(sentence,return_tensors='np');ner=session('ner/onnx/model_quantized.onnx')
logits=ner.run(None,{k:v.astype(np.int64) for k,v in inputs.items()})[0][0];labels=json.loads((root/'ner/config.json').read_text())['id2label']
tags=[labels[str(i)] for i in logits.argmax(-1)];assert any(t.endswith('-PER') for t in tags) and any(t.endswith('-LOC') for t in tags),tags
print('PASS NER: French person and place classified',flush=True);del ner;gc.collect()
# Same tensor pipeline as Android Supertonic, French tag, 8 steps, deterministic initial noise.
config=json.loads((root/'tts/onnx/tts.json').read_text());styles=json.loads((root/'tts/voice_styles/F1.json').read_text());index=json.loads((root/'tts/onnx/unicode_indexer.json').read_text())
text=unicodedata.normalize('NFKD','<fr>Bonjour, ceci est un test.</fr>');ids=np.array([[index[ord(c)] for c in text]],np.int64);mask=np.ones((1,1,len(text)),np.float32)
style_dp=np.array(styles['style_dp']['data'],np.float32);style_ttl=np.array(styles['style_ttl']['data'],np.float32)
sessions=[session('tts/onnx/'+name+'.onnx') for name in ['duration_predictor','text_encoder','vector_estimator','vocoder']]
duration=float(sessions[0].run(None,{'text_ids':ids,'style_dp':style_dp,'text_mask':mask})[0][0])/1.05
encoded=sessions[1].run(None,{'text_ids':ids,'style_ttl':style_ttl,'text_mask':mask})[0]
rate=config['ae']['sample_rate'];chunk=config['ae']['base_chunk_size']*config['ttl']['chunk_compress_factor'];dims=config['ttl']['latent_dim']*config['ttl']['chunk_compress_factor'];samples=int(duration*rate);length=(samples+chunk-1)//chunk
latent=np.random.default_rng(2026).standard_normal((1,dims,length)).astype(np.float32);lm=np.ones((1,1,length),np.float32)
for step in range(8):latent=sessions[2].run(None,{'noisy_latent':latent,'text_emb':encoded,'style_ttl':style_ttl,'latent_mask':lm,'text_mask':mask,'current_step':np.array([step],np.float32),'total_step':np.array([8],np.float32)})[0]
audio=sessions[3].run(None,{'latent':latent})[0].flatten()[:samples]
assert 0.3<duration<30 and np.isfinite(audio).all() and np.std(audio)>.01
print('PASS Supertonic 3: French waveform, duration %.2fs, RMS %.4f'%(duration,float(np.sqrt(np.mean(audio**2)))),flush=True)
