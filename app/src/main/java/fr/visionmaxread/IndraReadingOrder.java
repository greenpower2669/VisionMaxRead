package fr.visionmaxread;

import ai.onnxruntime.*;
import java.util.*;

/** Indra OrderNet (official 16 input features, pairwise+layout output) with XY-cut fallback. */
final class IndraReadingOrder {
    static final class Box {
        final float x0,y0,x1,y1;
        Box(float x0,float y0,float x1,float y1){this.x0=x0;this.y0=y0;this.x1=x1;this.y1=y1;}
    }
    static final class Result {
        final int[] indexes; final String method;final int blocks;
        Result(int[] indexes,String method,int blocks){this.indexes=indexes;this.method=method;this.blocks=blocks;}
    }
    private IndraReadingOrder(){}
    static Result sort(OrtEnvironment env,OrtSession session,List<Box> boxes,List<String> text,int width,int height) {
        int n=boxes.size();if(n==0)return new Result(new int[0],"empty",0);
        if(n==1)return new Result(new int[]{0},"single",1);
        // Model is trained on page layout with a fixed N x N pairwise output; cap
        // memory/complexity for very long pages and use the official XY-cut baseline.
        if(session==null||n>140)return new Result(xycut(boxes),session==null?"xycut-no-model":"xycut-long-page",1);
        try {
            float[] h=new float[n];for(int i=0;i<n;i++)h[i]=Math.max(1,boxes.get(i).y1-boxes.get(i).y0);
            float[] sortedH=h.clone();Arrays.sort(sortedH);
            float medH=sortedH[n/2];
            float[][][] feats=new float[1][n][16];
            for(int i=0;i<n;i++){
                Box b=boxes.get(i);String t=text.get(i).trim();float w=Math.max(0.001f,b.x1-b.x0),hh=h[i];
                float[] f=feats[0][i];
                f[0]=b.x0/width;f[1]=b.y0/height;f[2]=b.x1/width;f[3]=b.y1/height;
                f[4]=(b.x0+b.x1)/(2f*width);f[5]=(b.y0+b.y1)/(2f*height);
                f[6]=w/width;f[7]=(float)Math.log(hh/medH);
                f[8]=(float)Math.log(w/hh)/4f;f[9]=medH/height*20f;
                if(!t.isEmpty()){
                    f[10]=(float)Math.log1p(t.codePointCount(0,t.length()))/5;
                    int digits=0;
                    for(int p=0;p<t.length();p++)if(Character.isDigit(t.charAt(p)))digits++;
                    f[11]=digits/(float)t.length();
                    f[12]=("•-–▪◦*·→".indexOf(t.charAt(0))>=0||(t.length()>3&&Character.isDigit(t.charAt(0))))?1:0;
                    f[13]=t.endsWith("-")?1:0;
                    f[14]=".!?:;。！？：；．".indexOf(t.charAt(t.length()-1))>=0?1:0;
                    f[15]=Character.isUpperCase(t.charAt(0))?1:0;
                }
            }
            boolean[][] mask=new boolean[1][n];Arrays.fill(mask[0],true);
            try(OnnxTensor inp=OnnxTensor.createTensor(env,feats);
                OnnxTensor m=OnnxTensor.createTensor(env,mask);
                OrtSession.Result results=session.run(Map.of("feats",inp,"mask",m))) {
                float[][] pair=((float[][][])results.get(0).getValue())[0];
                if(pair.length!=n||pair[0].length!=n)throw new IllegalStateException("OrderNet pair dimensions");
                if(results.size()>=3){
                    float[][] cls=((float[][][])results.get(1).getValue())[0];
                    float[][] same=((float[][][])results.get(2).getValue())[0];
                    List<List<Integer>> groups=cluster(same,n);
                    // The official decoder groups blocks, XY-cut inside a paragraph
                    // and rows in tables; block order comes from mean pair logits.
                    List<int[]> inner=new ArrayList<>();
                    for(List<Integer> g:groups){
                        List<Box> subset=new ArrayList<>();
                        for(int idx:g)subset.add(boxes.get(idx));
                        boolean grid=false;
                        if(cls.length==n&&cls[0].length>=10){
                            float[] score=new float[cls[0].length];
                            for(int idx:g)for(int k=0;k<score.length;k++)score[k]+=cls[idx][k];
                            int best=0;for(int k=1;k<score.length;k++)if(score[k]>score[best])best=k;
                            grid=best==5||best==8||best==9;
                        }
                        int[] local=grid?topDown(subset):xycut(subset);
                        int[] original=new int[local.length];
                        for(int k=0;k<local.length;k++)original[k]=g.get(local[k]);
                        inner.add(original);
                    }
                    float[][] groupPairs=new float[groups.size()][groups.size()];
                    for(int a=0;a<groups.size();a++)for(int b=0;b<groups.size();b++){
                        if(a==b)continue;
                        double sum=0;
                        for(int i:groups.get(a))for(int j:groups.get(b))sum+=pair[i][j];
                        groupPairs[a][b]=(float)(sum/(groups.get(a).size()*groups.get(b).size()));
                    }
                    int[] rank=rank(groupPairs);
                    int[] order=new int[n];int pos=0;
                    for(int idx:rank)for(int v:inner.get(idx))order[pos++]=v;
                    if(!permutation(order,n))throw new IllegalStateException("OrderNet invalid permutation");
                    return new Result(order,"ordernet-layout",groups.size());
                }
                int[] order=rank(pair);
                if(!permutation(order,n))throw new IllegalStateException("OrderNet invalid pair order");
                return new Result(order,"ordernet-pair",n);
            }
        }catch(Exception|LinkageError ex){
            // A model incompatibility must never prevent reading the page.
            return new Result(xycut(boxes),"xycut-model-fallback-"+ex.getClass().getSimpleName(),1);
        }
    }
    private static List<List<Integer>> cluster(float[][] same,int n){
        List<List<Integer>> groups=new ArrayList<>();
        for(int i=0;i<n;i++)groups.add(new ArrayList<>(Collections.singletonList(i)));
        while(groups.size()>1){
            float best=0;int ba=-1,bb=-1;
            for(int a=0;a<groups.size();a++)for(int b=a+1;b<groups.size();b++){
                double sum=0;
                for(int i:groups.get(a))for(int j:groups.get(b))sum+=same[i][j];
                float score=(float)(sum/(groups.get(a).size()*groups.get(b).size()));
                if(score>best){best=score;ba=a;bb=b;}
            }
            if(ba<0)break;
            groups.get(ba).addAll(groups.get(bb));groups.remove(bb);
        }
        return groups;
    }
    /** Faithful Java translation of Indra's pairwise decode_order insertion search.
     * Initial ranking uses log probabilities (rather than raw score sums).
     * Then move each group to the position maximizing all pairwise relations.
     * This especially avoids splitting the final short line of a column.
     */
    private static int[] rank(float[][] matrix){
        int n=matrix.length;if(n<2){int[] one=new int[n];if(n==1)one[0]=0;return one;}
        double[][] logp=new double[n][n];
        double[] totals=new double[n];
        Integer[] order=new Integer[n];
        for(int i=0;i<n;i++){
            order[i]=i;
            for(int j=0;j<n;j++){
                logp[i][j]=logSigmoid(matrix[i][j]);
                totals[i]+=logp[i][j]-logSigmoid(matrix[j][i]);
            }
        }
        Arrays.sort(order,(a,b)->Double.compare(totals[b],totals[a]));
        for(int sweep=0;sweep<4;sweep++){
            boolean changed=false;
            Integer[] snapshot=order.clone();
            for(int moved:snapshot){
                int[] others=new int[n-1];int current=-1,p=0;
                for(int i=0;i<n;i++){
                    if(order[i]==moved){current=i;continue;}
                    others[p++]=order[i];
                }
                if(current<0)throw new IllegalStateException("OrderNet decoding lost block");
                double[] gains=new double[n];
                double after=0;
                for(int i=0;i<others.length;i++)after+=logp[moved][others[i]];
                gains[0]=after;
                double before=0;
                for(int k=0;k<others.length;k++){
                    before+=logp[others[k]][moved];
                    after-=logp[moved][others[k]];
                    gains[k+1]=before+after;
                }
                int selected=current;
                for(int k=0;k<n;k++)if(gains[k]>gains[selected]+1e-6)selected=k;
                if(selected!=current){
                    Integer[] updated=new Integer[n];
                    int j=0;
                    for(int i=0;i<n;i++)updated[i]=i==selected?moved:others[j++];
                    order=updated;changed=true;
                }
            }
            if(!changed)break;
        }
        int[] out=new int[n];for(int i=0;i<n;i++)out[i]=order[i];
        return out;
    }
    private static double logSigmoid(double x){
        if(x>=0)return -Math.log1p(Math.exp(-x));
        return x-Math.log1p(Math.exp(x));
    }
    private static boolean permutation(int[] indices,int n){
        if(indices.length!=n)return false;
        boolean[] seen=new boolean[n];for(int i:indices){if(i<0||i>=n||seen[i])return false;seen[i]=true;}return true;
    }
    private static int[] topDown(List<Box> boxes){
        Integer[] order=new Integer[boxes.size()];
        for(int i=0;i<order.length;i++)order[i]=i;
        Arrays.sort(order,Comparator.comparingDouble((Integer i)->boxes.get(i).y0).thenComparingDouble(i->boxes.get(i).x0));
        int[] res=new int[order.length];for(int i=0;i<res.length;i++)res[i]=order[i];return res;
    }
    static int[] xycut(List<Box> boxes){
        List<Integer> ids=new ArrayList<>();for(int i=0;i<boxes.size();i++)ids.add(i);
        List<Integer> out=new ArrayList<>();split(boxes,ids,out,0);
        int[] result=new int[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);
        return result;
    }
    private static void split(List<Box> boxes,List<Integer> ids,List<Integer> out,int depth){
        if(ids.size()<=1){out.addAll(ids);return;}
        if(depth>16){ids.sort(Comparator.comparingDouble((Integer i)->boxes.get(i).y0).thenComparingDouble(i->boxes.get(i).x0));out.addAll(ids);return;}
        Gap y=gap(boxes,ids,true),x=gap(boxes,ids,false);
        Gap selected=y.gap>=x.gap?y:x;
        if(selected.index<=0){
            ids.sort(Comparator.comparingDouble((Integer i)->boxes.get(i).y0).thenComparingDouble(i->boxes.get(i).x0));
            out.addAll(ids);return;
        }
        split(boxes,new ArrayList<>(selected.sorted.subList(0,selected.index)),out,depth+1);
        split(boxes,new ArrayList<>(selected.sorted.subList(selected.index,selected.sorted.size())),out,depth+1);
    }
    private static final class Gap{
        final double gap;final int index;final List<Integer> sorted;
        Gap(double gap,int index,List<Integer> sorted){this.gap=gap;this.index=index;this.sorted=sorted;}
    }
    private static Gap gap(List<Box> boxes,List<Integer> ids,boolean vertical){
        List<Integer> sorted=new ArrayList<>(ids);
        sorted.sort(Comparator.comparingDouble(i->vertical?boxes.get(i).y0:boxes.get(i).x0));
        double end=vertical?boxes.get(sorted.get(0)).y1:boxes.get(sorted.get(0)).x1;
        double best=0;int at=-1;
        for(int k=1;k<sorted.size();k++){
            Box b=boxes.get(sorted.get(k));
            double start=vertical?b.y0:b.x0;
            double d=start-end;
            if(d>best){best=d;at=k;}
            end=Math.max(end,vertical?b.y1:b.x1);
        }
        return new Gap(best,at,sorted);
    }
}
