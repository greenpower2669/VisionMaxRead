package fr.visionmaxread;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * Standalone reader for the original Datalab Indra lm.npz backoff n-gram tables.
 * No Python, network or extra dependency. Data format follows indra_ocr/lm.py.
 * Pure Java so CI can run synthetic oracles without Android.
 */
final class IndraNgramLM {
    private static final int MAX_ENTRY_BYTES=32*1024*1024;
    private static final int TAG_SHIFT=61;
    final int order,bits,vocabSize;
    final long[][] key,historyKey;
    final double[][] logProbs,backoff;
    private final Map<Long,Double> cache=new HashMap<>();
    private IndraNgramLM(int order,int bits,int vocabSize,long[][] k,double[][] v,long[][] hk,double[][] hb){
        this.order=order;this.bits=bits;this.vocabSize=vocabSize;
        this.key=k;this.logProbs=v;this.historyKey=hk;this.backoff=hb;
    }
    static IndraNgramLM load(InputStream file)throws IOException{
        Map<String,Entry> data=new HashMap<>();
        try(ZipInputStream zis=new ZipInputStream(new BufferedInputStream(file))){
            ZipEntry e;
            while((e=zis.getNextEntry())!=null){
                String name=e.getName();
                if(!name.matches("(order|bits|vocab_size|[kv][0-9]+|hk[0-9]+|hb[0-9]+)\\.npy")){zis.closeEntry();continue;}
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                byte[] buffer=new byte[8192];int n,total=0;
                while((n=zis.read(buffer))!=-1){
                    total+=n;
                    if(total>MAX_ENTRY_BYTES)throw new IOException("Indra LM array too large: "+name);
                    out.write(buffer,0,n);
                }
                data.put(name.substring(0,name.length()-4),Entry.read(out.toByteArray()));
                zis.closeEntry();
            }
        }
        int ord=scalar(data,"order"),bits=data.containsKey("bits")?scalar(data,"bits"):11, vocab=scalar(data,"vocab_size");
        if(ord<1||ord>5||bits<1||bits>16||ord*bits>TAG_SHIFT||vocab<1||vocab>100000)
            throw new IOException("Indra LM metadata incompatible");
        long[][] k=new long[ord][],hk=new long[ord][];
        double[][] v=new double[ord][],hb=new double[ord][];
        for(int i=0;i<ord;i++){
            k[i]=required(data,"k"+i).asLong();
            v[i]=required(data,"v"+i).asDouble();
            hk[i]=required(data,"hk"+i).asLong();
            hb[i]=required(data,"hb"+i).asDouble();
            if(k[i].length!=v[i].length||hk[i].length!=hb[i].length)
                throw new IOException("Indra LM table shape mismatch "+i);
        }
        return new IndraNgramLM(ord,bits,vocab,k,v,hk,hb);
    }
    private static Entry required(Map<String,Entry> d,String k)throws IOException{
        Entry e=d.get(k);if(e==null)throw new IOException("Indra LM missing "+k);return e;
    }
    private static int scalar(Map<String,Entry> d,String k)throws IOException{
        long[] v=required(d,k).asLong();if(v.length!=1||v[0]>Integer.MAX_VALUE)throw new IOException("Invalid LM scalar "+k);
        return (int)v[0];
    }
    private static long pack(int[] hist,int start,int len,int token,int bits){
        long value=0L;
        for(int i=0;i<len;i++)value=(value<<bits)|(hist[start+i]+1L);
        return (value<<bits)|(token+1L);
    }
    private static long histKey(int[] hist,int start,int len,int bits){
        long value=0L;for(int i=0;i<len;i++)value=(value<<bits)|(hist[start+i]+1L);return value;
    }
    /** Score log P(token | history) with longest existing history and backoff. */
    double logp(int[] tokens,int token){
        int m=Math.min(tokens.length,order-1),valid=0;
        for(int n=1;n<=m;n++){
            long hk=histKey(tokens,tokens.length-n,n,bits);
            if(unsignedFind(historyKey[n],hk)<0)break;
            valid=n;
        }
        double acc=0;
        for(int n=valid;n>=0;n--){
            int idx=unsignedFind(key[n],pack(tokens,tokens.length-n,n,token,bits));
            if(idx>=0)return acc+logProbs[n][idx];
            int hi=unsignedFind(historyKey[n],histKey(tokens,tokens.length-n,n,bits));
            if(hi>=0)acc+=backoff[n][hi];
        }
        return acc-Math.log(vocabSize);
    }
    /** The original tables are sorted unsigned uint64; Java long comparison is signed. */
    private static int unsignedFind(long[] sorted,long val){
        int low=0,hi=sorted.length-1;
        while(low<=hi){
            int mid=(low+hi)>>>1,cmp=Long.compareUnsigned(sorted[mid],val);
            if(cmp==0)return mid;
            if(cmp<0)low=mid+1;else hi=mid-1;
        }
        return -1;
    }
    static final class Entry {
        final String dtype;final byte[] bytes;final int count;
        Entry(String dtype,byte[] bytes,int count){this.dtype=dtype;this.bytes=bytes;this.count=count;}
        static Entry read(byte[] source)throws IOException{
            ByteBuffer b=ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN);
            if(source.length<12||b.get()!= (byte)0x93 || b.get()!='N'||b.get()!='U'||b.get()!='M'||b.get()!='P'||b.get()!='Y')
                throw new IOException("Not a numpy .npy asset");
            int version=b.get()&255;b.get();
            int headerBytes=version>=2?b.getInt():(b.getShort()&65535);
            if(headerBytes<10||headerBytes>b.remaining())throw new IOException("Corrupted numpy header");
            byte[] head=new byte[headerBytes];b.get(head);
            String str=new String(head,StandardCharsets.US_ASCII);
            java.util.regex.Matcher dtype=java.util.regex.Pattern.compile("['\\\"]descr['\\\"]\\s*:\\s*['\\\"]([^'\\\"]+)").matcher(str);
            java.util.regex.Matcher shape=java.util.regex.Pattern.compile("['\\\"]shape['\\\"]\\s*:\\s*\\(([^)]*)\\)").matcher(str);
            if(!dtype.find()||!shape.find()||str.contains("'fortran_order': True"))throw new IOException("Unsupported numpy header");
            String d=dtype.group(1);
            if(!d.matches("[<|][iuf][1248]"))throw new IOException("Unsupported numpy dtype "+d);
            String dimensions=shape.group(1).trim();
            int count;
            if(dimensions.isEmpty())count=1;
            else{
                String[] parts=dimensions.split(",");
                if(parts.length!=1)throw new IOException("Only 1D npz tables supported");
                try{count=Integer.parseInt(parts[0].trim());}catch(NumberFormatException e){throw new IOException("Invalid shape",e);}
            }
            int stride=Integer.parseInt(d.substring(2));
            if(count<0||((long)count*stride)!=b.remaining())throw new IOException("NPY length/dtype mismatch");
            byte[] arr=new byte[b.remaining()];b.get(arr);
            return new Entry(d,arr,count);
        }
        long[] asLong()throws IOException{
            long[] a=new long[count];ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(dtype.equals("<u8")||dtype.equals("<i8"))for(int i=0;i<count;i++)a[i]=b.getLong();
            else if(dtype.equals("<i4")||dtype.equals("<u4"))for(int i=0;i<count;i++)a[i]=Integer.toUnsignedLong(b.getInt());
            else if(dtype.equals("|u1")||dtype.equals("|i1"))for(int i=0;i<count;i++)a[i]=b.get()&255;
            else throw new IOException("Expected integer NPY but found "+dtype);
            return a;
        }
        double[] asDouble()throws IOException{
            double[] a=new double[count];ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(dtype.equals("<f8"))for(int i=0;i<count;i++)a[i]=b.getDouble();
            else if(dtype.equals("<f4"))for(int i=0;i<count;i++)a[i]=b.getFloat();
            else if(dtype.equals("<f2"))for(int i=0;i<count;i++)a[i]=half(b.getShort());
            else throw new IOException("Expected float NPY but found "+dtype);
            return a;
        }
        private static float half(short bits){
            int h=bits&65535,s=(h&0x8000)<<16,e=(h>>>10)&31,f=h&1023;
            if(e==0){if(f==0)return Float.intBitsToFloat(s);while((f&1024)==0){f<<=1;e--;}e++;f&=1023;}
            else if(e==31)return Float.intBitsToFloat(s|0x7f800000|(f<<13));
            int exp=e+112;return Float.intBitsToFloat(s|(exp<<23)|(f<<13));
        }
    }
}
