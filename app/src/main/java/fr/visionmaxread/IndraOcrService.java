package fr.visionmaxread;
import android.content.Context;
import android.graphics.Bitmap;
import ai.onnxruntime.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.*;
import java.security.MessageDigest;
import java.util.*;

/** Experimental Indra native Latin-only port. No text persistence, no network. */
final class IndraOcrService {
  private static final int MAX_LINES=180;
  private static final class Box {
    float x0,y0,x1,y1;
    Box(float a,float b,float c,float d){x0=a;y0=b;x1=c;y1=d;}
  }
  String recognize(Context context,Bitmap bitmap)throws Exception{
    if(bitmap==null||bitmap.isRecycled())throw new IOException("Image absente");
    JSONObject manifest=manifest(context);
    JSONObject meta=meta(context,manifest);
    File detector=install(context,"det.onnx",manifest.getJSONObject("det.onnx"));
    File recognizer=install(context,"rec.onnx",manifest.getJSONObject("rec.onnx"));
    int w=bitmap.getWidth(),h=bitmap.getHeight();
    if((long)w*h>20_000_000)throw new IOException("Image trop grande");
    int[] argb=new int[w*h];bitmap.getPixels(argb,0,w,0,0,w,h);
    byte[] gray=new byte[argb.length];
    for(int i=0;i<argb.length;i++){
      int c=argb[i];gray[i]=(byte)((77*((c>>16)&255)+150*((c>>8)&255)+29*(c&255)+128)>>8);
    }
    int recH=meta.optInt("rec_height",32),win=meta.optInt("rec_window",1);
    if(recH<16||recH>128||win<1||win>5)throw new IOException("Métadonnées Indra incompatibles");
    JSONObject vocab=new JSONObject(meta.getString("vocab"));
    JSONArray chars=vocab.getJSONArray("chars");
    OrtEnvironment env=OrtEnvironment.getEnvironment();
    OrtSession.SessionOptions opts=new OrtSession.SessionOptions();
    opts.setIntraOpNumThreads(2);
    try(OrtSession det=env.createSession(detector.getAbsolutePath(),opts);
        OrtSession rec=env.createSession(recognizer.getAbsolutePath(),opts)){
      List<Box> boxes=detect(env,det,gray,w,h,meta.optInt("det_long_side",1024));
      StringBuilder text=new StringBuilder();
      for(Box b:boxes){
        String line=recognizeLine(env,rec,gray,w,h,b,recH,win,chars);
        if(!line.isEmpty()){if(text.length()>0)text.append('\n');text.append(line);}
      }
      return text.toString();
    }finally{opts.close();}
  }
  private static List<Box> detect(OrtEnvironment env,OrtSession net,byte[] gray,int sw,int sh,int side)throws Exception{
    if(side<256||side>2048)throw new IOException("Taille detector non prise en charge");
    float scale=side/(float)Math.max(sw,sh);
    int iw=Math.max(1,Math.round(sw*scale)),ih=Math.max(1,Math.round(sh*scale));
    int pw=(iw+31)/32*32,ph=(ih+31)/32*32;
    float[] data=new float[ph*pw];
    for(int y=0;y<ih;y++)for(int x=0;x<iw;x++){
      int ox=Math.min(sw-1,(int)(x/scale)),oy=Math.min(sh-1,(int)(y/scale));
      data[y*pw+x]=1-(gray[oy*sw+ox]&255)/255f;
    }
    float[][][] out;
    try(OnnxTensor t=OnnxTensor.createTensor(env,FloatBuffer.wrap(data),new long[]{1,1,ph,pw});
        OrtSession.Result r=net.run(Collections.singletonMap("x",t))){
      out=((float[][][][])r.get(0).getValue())[0];
    }
    if(out.length<3)throw new IOException("Sortie détecteur incompatible");
    float[][] core=out[0],top=out[1],bottom=out[2];
    int h=core.length,w=core[0].length;boolean[] seen=new boolean[w*h];
    int[] queue=new int[w*h];List<Box> boxes=new ArrayList<>();
    for(int y=0;y<h;y++)for(int x=0;x<w;x++){
      int at=y*w+x;if(seen[at]||core[y][x]<=0)continue;
      int head=0,tail=0,count=0,xmin=x,xmax=x;double upper=0,lower=0;
      queue[tail++]=at;seen[at]=true;
      while(head<tail){
        int p=queue[head++],cy=p/w,cx=p%w;
        count++;xmin=Math.min(xmin,cx);xmax=Math.max(xmax,cx);
        upper+=(cy+.5)*2-top[cy][cx];lower+=(cy+.5)*2+bottom[cy][cx];
        if(cx>0)tail=visit(cx-1,cy,w,core,seen,queue,tail);
        if(cx+1<w)tail=visit(cx+1,cy,w,core,seen,queue,tail);
        if(cy>0)tail=visit(cx,cy-1,w,core,seen,queue,tail);
        if(cy+1<h)tail=visit(cx,cy+1,w,core,seen,queue,tail);
      }
      if(count<2)continue;
      float y0=(float)(upper/count),y1=(float)(lower/count),pad=.15f*Math.max(1,y1-y0);
      float a=Math.max(0,(xmin*2-pad)/scale),b=Math.max(0,y0/scale);
      float c=Math.min(sw,((xmax+1)*2+pad)/scale),d=Math.min(sh,y1/scale);
      if(Float.isFinite(a)&&Float.isFinite(b)&&Float.isFinite(c)&&Float.isFinite(d)&&c>a+2&&d>b+1)boxes.add(new Box(a,b,c,d));
    }
    boxes.sort(Comparator.comparingDouble((Box b)->(b.y0+b.y1)*.5).thenComparingDouble(b->b.x0));
    if(boxes.size()>MAX_LINES)return new ArrayList<>(boxes.subList(0,MAX_LINES));
    return boxes;
  }
  private static int visit(int x,int y,int w,float[][] core,boolean[] seen,int[] queue,int tail){
    int index=y*w+x;
    if(!seen[index]&&core[y][x]>0){seen[index]=true;queue[tail++]=index;}
    return tail;
  }
  private static String recognizeLine(OrtEnvironment env,OrtSession rec,byte[] gray,int iw,int ih,
      Box box,int recH,int window,JSONArray chars)throws Exception{
    float bh=Math.max(1,box.y1-box.y0);double py=(window*1.4-1)/2;
    int x0=(int)Math.floor(box.x0-.3*bh),x1=(int)Math.ceil(box.x1+.3*bh);
    int y0=(int)Math.floor(box.y0-py*bh),y1=(int)Math.ceil(box.y1+py*bh);
    int cw=Math.max(1,x1-x0),ch=Math.max(1,y1-y0),outH=recH*window;
    int outW=Math.max(8,Math.min(4096,Math.round(cw*outH/(float)ch)));
    int padded=(outW+7)/8*8;float[] input=new float[outH*padded];
    for(int y=0;y<outH;y++)for(int x=0;x<outW;x++){
      int px=Math.round(x0+(x+.5f)*cw/outW-.5f),qy=Math.round(y0+(y+.5f)*ch/outH-.5f);
      if(px>=0&&px<iw&&qy>=0&&qy<ih)input[y*padded+x]=1-(gray[qy*iw+px]&255)/255f;
    }
    try(OnnxTensor t=OnnxTensor.createTensor(env,FloatBuffer.wrap(input),new long[]{1,1,outH,padded});
        OnnxTensor wd=OnnxTensor.createTensor(env,LongBuffer.wrap(new long[]{outW}),new long[]{1});
        OrtSession.Result r=rec.run(Map.of("x",t,"widths",wd))){
      Object seq=Array.get(r.get("ids").orElseThrow(()->new IOException("ids absents")).getValue(),0);
      Object probs=Array.get(r.get("conf").orElseThrow(()->new IOException("conf absente")).getValue(),0);
      Object lens=r.get("lengths").orElseThrow(()->new IOException("lengths absents")).getValue();
      int len=Math.min(Array.getLength(seq),((Number)Array.get(lens,0)).intValue());
      StringBuilder result=new StringBuilder();double conf=0;int count=0;long prev=-1;
      for(int i=0;i<len;i++){
        long id=((Number)Array.get(seq,i)).longValue();
        if(id>0&&id!=prev&&id<=chars.length()){
          result.append(chars.getString((int)id-1));conf+=((Number)Array.get(probs,i)).doubleValue();count++;
        }
        prev=id;
      }
      return count>0&&conf/count>=.5?result.toString():"";
    }
  }
  private static JSONObject manifest(Context ctx)throws Exception{
    try(InputStream in=ctx.getAssets().open("indra/manifest.json")){return new JSONObject(read(in));}
  }
  private static JSONObject meta(Context ctx,JSONObject manifest)throws Exception{
    try(InputStream in=ctx.getAssets().open("indra/meta.json")){
      byte[] bytes=readBytes(in);
      if(!hash(bytes).equals(manifest.getJSONObject("meta.json").getString("sha256")))throw new IOException("Indra meta corrompu");
      return new JSONObject(new String(bytes,"UTF-8"));
    }
  }
  private static File install(Context ctx,String name,JSONObject expected)throws Exception{
    File folder=new File(ctx.getNoBackupFilesDir(),"indra");
    if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Stockage Indra indisponible");
    File file=new File(folder,name);
    if(file.isFile()&&file.length()==expected.getLong("size"))return file;
    File part=new File(folder,name+".part");part.delete();
    MessageDigest sha=MessageDigest.getInstance("SHA-256");
    try(InputStream in=ctx.getAssets().open("indra/"+name);FileOutputStream out=new FileOutputStream(part)){
      byte[] buf=new byte[65536];int n;
      while((n=in.read(buf))!=-1){out.write(buf,0,n);sha.update(buf,0,n);}
      out.getFD().sync();
    }catch(Exception e){part.delete();throw e;}
    if(part.length()!=expected.getLong("size")||!hex(sha.digest()).equals(expected.getString("sha256"))){
      part.delete();throw new IOException("Intégrité du modèle Indra invalide");
    }
    if(!part.renameTo(file)){part.delete();throw new IOException("Installation modèle Indra échouée");}
    return file;
  }
  private static String read(InputStream in)throws Exception{return new String(readBytes(in),"UTF-8");}
  private static byte[] readBytes(InputStream in)throws Exception{
    ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
    while((n=in.read(buf))!=-1)b.write(buf,0,n);
    return b.toByteArray();
  }
  private static String hash(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
  private static String hex(byte[] bytes){
    StringBuilder b=new StringBuilder();
    for(byte value:bytes)b.append(String.format(Locale.ROOT,"%02x",value&255));
    return b.toString();
  }
}
