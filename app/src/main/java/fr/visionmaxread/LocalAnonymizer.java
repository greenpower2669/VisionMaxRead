package fr.visionmaxread;
import ai.onnxruntime.*;
import org.json.*;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import fr.visionmaxread.core.*;
/** Throws on any model/tokenization error: callers must never save the original as a fallback. */
final class LocalAnonymizer {
    static String mask(String text, ModelFiles files, BooleanSupplier cancelled) throws Exception {
        if(text.length()>50000)throw new IllegalArgumentException("Texte trop long pour l’historique local");
        WordPiece tokenizer=new WordPiece(Arrays.asList(files.readAsset("models/ner/vocab.txt").split("\n")));
        JSONObject labels=new JSONObject(files.readAsset("models/ner/config.json")).getJSONObject("id2label");
        List<WordPiece.Token> tokens=tokenizer.tokenize(text);
        List<PrivacyRules.Span> spans=new ArrayList<>();
        OrtEnvironment env=OrtEnvironment.getEnvironment();
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
            options.setIntraOpNumThreads(2);options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            try(OrtSession session=env.createSession(files.file("ner/onnx/model_quantized.onnx").getPath(),options)) {
                for(int start=0;start<tokens.size();start+=224) {
                    if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
                    int count=Math.min(254,tokens.size()-start),length=count+2;
                    long[] ids=new long[length],attention=new long[length],types=new long[length];
                    ids[0]=tokenizer.id("[CLS]");ids[length-1]=tokenizer.id("[SEP]");Arrays.fill(attention,1);
                    for(int i=0;i<count;i++)ids[i+1]=tokens.get(start+i).id;
                    long[] shape={1,length};
                    try(OnnxTensor input=OnnxTensor.createTensor(env,LongBuffer.wrap(ids),shape);
                        OnnxTensor mask=OnnxTensor.createTensor(env,LongBuffer.wrap(attention),shape);
                        OnnxTensor segments=OnnxTensor.createTensor(env,LongBuffer.wrap(types),shape)){
                        Map<String,OnnxTensor> inputs=new HashMap<>();inputs.put("input_ids",input);inputs.put("attention_mask",mask);
                        if(session.getInputNames().contains("token_type_ids"))inputs.put("token_type_ids",segments);
                        try(OrtSession.Result result=session.run(inputs)){
                            float[][][] logits=(float[][][])result.get(0).getValue();
                            if(logits.length!=1||logits[0].length!=length)throw new IllegalStateException("Sortie NER inattendue");
                            int entityStart=-1,entityEnd=-1;String entityLabel=null;
                            for(int i=0;i<count;i++) {
                                float[] scores=logits[0][i+1];int best=0;
                                for(int j=1;j<scores.length;j++)if(scores[j]>scores[best])best=j;
                                String tag=labels.getString(Integer.toString(best));
                                WordPiece.Token token=tokens.get(start+i);
                                String label=label(tag);
                                if(label==null){if(entityStart>=0)spans.add(new PrivacyRules.Span(entityStart,entityEnd,entityLabel));entityStart=-1;entityLabel=null;continue;}
                                boolean contiguous=entityStart>=0&&label.equals(entityLabel)&&
                                    (token.start<=entityEnd||(!tag.startsWith("B-")&&text.substring(entityEnd,token.start).trim().isEmpty()));
                                if(contiguous)entityEnd=Math.max(entityEnd,token.end);
                                else {if(entityStart>=0)spans.add(new PrivacyRules.Span(entityStart,entityEnd,entityLabel));entityStart=token.start;entityEnd=token.end;entityLabel=label;}
                            }
                            if(entityStart>=0)spans.add(new PrivacyRules.Span(entityStart,entityEnd,entityLabel));
                        }
                    }
                    if(start+count==tokens.size())break;
                }
            }
        }
        if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
        return PrivacyRules.mask(text,spans);
    }
    private static String label(String tag){
        if(tag.endsWith("-PER"))return "PERSONNE";if(tag.endsWith("-LOC"))return "LIEU";
        if(tag.endsWith("-ORG"))return "ORGANISATION";if(tag.endsWith("-DATE"))return "DATE";
        if(tag.equals("O"))return null;throw new IllegalArgumentException("Étiquette NER inconnue");
    }
}
