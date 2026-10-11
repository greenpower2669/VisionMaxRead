"""Lossless VMR1 64 KiB byte-lane packing for one large float32 ONNX asset.
ONNX bytes and SHA256 stay unchanged; XZ decoder dictionary stays below 32 MiB.
"""
import struct,lzma
MAGIC=b'VMR1';BLOCK=65536

def pack(source,destination):
 with open(source,'rb') as r,lzma.open(destination,'wb',preset=6) as w:
  w.write(MAGIC)
  while b:=r.read(BLOCK):
   w.write(struct.pack('>I',len(b)))
   for lane in range(4):w.write(b[lane::4])
  w.write(struct.pack('>I',0))

def decoded_chunks(raw,packed):
 if not packed:
  while b:=raw.read(1024*1024):yield b
  return
 with lzma.LZMAFile(raw) as r:
  if r.read(4)!=MAGIC:raise ValueError('Invalid model archive')
  while True:
   header=r.read(4)
   if len(header)!=4:raise EOFError('Truncated model block')
   length=struct.unpack('>I',header)[0]
   if length==0:
    if r.read(1):raise ValueError('Trailing model data')
    return
   if length>BLOCK:raise ValueError('Oversized model block')
   b=r.read(length)
   if len(b)!=length:raise EOFError('Truncated model data')
   restored=bytearray(length);offset=0
   for lane in range(4):
    count=(length+3-lane)//4;restored[lane::4]=b[offset:offset+count];offset+=count
   yield restored
