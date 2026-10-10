package fr.visionmaxread;
import java.io.*;
import org.tukaani.xz.XZInputStream;
/** Lossless VMR1 lane packing. Models are SHA256-checked after restoring their original bytes. */
final class ModelArchive {
    static InputStream open(InputStream raw,boolean packed) throws IOException {
        if(!packed)return raw;
        try{return new RestoredStream(new XZInputStream(raw,32768));}
        catch(IOException e){raw.close();throw e;}
    }
    private static final class RestoredStream extends InputStream {
        final DataInputStream input;final byte[] encoded=new byte[65536],decoded=new byte[65536];
        int pos,limit;boolean ended;
        RestoredStream(InputStream input)throws IOException {this.input=new DataInputStream(input);if(this.input.readInt()!=0x564d5231)throw new IOException("Invalid model archive");}
        boolean fill()throws IOException {
            if(ended)return false;int length=input.readInt();
            if(length==0){ended=true;if(input.read()!=-1)throw new IOException("Trailing model data");return false;}
            if(length<1||length>65536)throw new IOException("Invalid model block");
            input.readFully(encoded,0,length);int offset=0;
            for(int lane=0;lane<4;lane++)for(int i=lane;i<length;i+=4)decoded[i]=encoded[offset++];
            pos=0;limit=length;return true;
        }
        public int read()throws IOException {if(pos==limit&&!fill())return -1;return decoded[pos++]&255;}
        public int read(byte[] b,int off,int len)throws IOException {
            if(off<0||len<0||off>b.length-len)throw new IndexOutOfBoundsException();if(len==0)return 0;
            if(pos==limit&&!fill())return -1;int count=Math.min(len,limit-pos);System.arraycopy(decoded,pos,b,off,count);pos+=count;return count;
        }
        public void close()throws IOException {input.close();}
    }
}
