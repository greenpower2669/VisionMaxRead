package fr.visionmaxread;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import fr.visionmaxread.core.SessionGate;
import fr.visionmaxread.core.ReaderSessions;

public final class MainActivity extends Activity {
    private static final int CAMERA_PERMISSION=10,PHOTO=20;
    private static final int INK=Color.rgb(16,44,60),PAPER=Color.rgb(255,249,238),TEAL=Color.rgb(0,105,92),RED=Color.rgb(179,38,32),DISABLED=Color.rgb(145,145,145);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService paddleWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService indraWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService semanticWorker=Executors.newSingleThreadExecutor();
    private final AtomicLong ocrChoice=new AtomicLong(),progressGeneration=new AtomicLong();
    private final ReaderSessions sessions=new ReaderSessions();
    private final SessionGate speech=new SessionGate();
    private final PiperSpeechEngine voice=new PiperSpeechEngine();
    private final IndraOcrService indraOcr=new IndraOcrService();
    private TextureView preview;private ImageView frozen;private FrameLayout imageArea;private ProgressBar ocrProgress;
    // Only an in-memory reference to the displayed photo, never copied to persistent storage.
    private Bitmap compareBitmap;
    private EditText text;private TextView status;private Button freeze,play,compare,indra,liquid,moe,copyLogs,copyResults;private CameraController camera;
    private HistoryStore history;private ModelFiles models;private SharedPreferences prefs;
    private boolean changingText,destroyed,reading,cameraMode;private String maskedPreview="";private int textSp=24;
    private volatile OcrTrace lastTrace;
    private volatile IndraOcrService.Scan lastScan;
    private volatile LiquidOcrReview.Result lastReview;
    private volatile HobbyMoeReview.Result lastMoeReview;
    private MoeStreamDialog activeMoeStream;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);DebugLog.session(this);prefs=getSharedPreferences("settings",MODE_PRIVATE);history=new HistoryStore(this);textSp=prefs.getInt("textSize",24);
        voice.selectVoice(prefs.getBoolean("voiceHD",true));buildUi();camera=new CameraController(this,preview,new CameraController.Listener(){
            public void captured(byte[] jpeg,long ticket){ui(()->{if(cameraMode&&sessions.sourceCurrent(ticket)){cameraMode=false;processImage(()->ImageLoader.fromJpeg(jpeg));}});}
            public void error(String message){ui(()->{if(cameraMode){status.setText(message);freeze.setEnabled(true);}});}
        });
        incoming(getIntent());
        // Warm Piper through the real muted audio route before the first user reading.
        worker.execute(()->{
            try{LegacyModelCleanup.run(this);}catch(Exception e){DebugLog.exception(this,"LEGACY cleanup",e);}
            try{
                voice.initialize(this,message->ui(()->{if(!reading&&text.getText().length()==0)status.setText(message);}));
                voice.warmUp(this,()->destroyed);
                ui(()->{if(!destroyed&&text.getText().length()==0)status.setText("Voix "+voice.voiceName()+" prête. Choisis Caméra, Photo ou colle un texte.");});
            }catch(Exception|OutOfMemoryError e){
                ui(()->{if(!destroyed&&text.getText().length()==0)status.setText("Voix Piper en attente : elle sera réessayée à la première lecture.");});
            }
        });
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(INK);return v;}
    private Button button(String value,int color,Runnable action){Button b=new Button(this);b.setText(value);b.setTextSize(18);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));b.setMinHeight(dp(56));b.setOnClickListener(v->action.run());return b;}
    private void row(LinearLayout root,Button... buttons){LinearLayout row=new LinearLayout(this);for(Button b:buttons){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);p.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(b,p);}root.addView(row);}
    private void buildUi(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(PAPER);outer.setPadding(dp(12),0,dp(12),0);
        outer.setOnApplyWindowInsetsListener((v,insets)->{outer.setPadding(dp(12),insets.getSystemWindowInsetTop()+dp(6),dp(12),insets.getSystemWindowInsetBottom()+dp(6));return insets;});
        setContentView(outer);outer.requestApplyInsets();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);
        TextView title=label("VisionMaxRead",28);title.setTypeface(null,Typeface.BOLD);if(Build.VERSION.SDK_INT>=28)title.setAccessibilityHeading(true);content.addView(title);
        content.addView(label("Voir le texte. L’écouter. Hors ligne.",16));
        row(content,button("Caméra",INK,this::startCamera),button("Photo",INK,this::choosePhoto),button("Texte",INK,this::manualText));
        imageArea=new FrameLayout(this);imageArea.setBackgroundColor(Color.BLACK);content.addView(imageArea,new LinearLayout.LayoutParams(-1,dp(270)));
        preview=new TextureView(this);preview.setContentDescription("Aperçu caméra pour viser uniquement");imageArea.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        frozen=new ImageView(this);frozen.setScaleType(ImageView.ScaleType.FIT_CENTER);frozen.setContentDescription("Image figée à reconnaître");frozen.setBackgroundColor(Color.BLACK);frozen.setVisibility(View.GONE);imageArea.addView(frozen,new FrameLayout.LayoutParams(-1,-1));
        freeze=button("● Figer l’image",RED,()->{freeze.setEnabled(false);status.setText("Capture de la photo…");camera.freeze();});content.addView(freeze);freeze.setEnabled(false);
        compare=button("PP-OCRv6-Small / Comparer",TEAL,this::compareOcr);compare.setVisibility(View.GONE);content.addView(compare);setCompareAvailable(false);
        indra=button("Passes Indra · rapport",TEAL,this::compareIndra);indra.setVisibility(View.GONE);content.addView(indra);setIndraAvailable(false);
        copyLogs=button("Copier logs techniques",INK,this::copyTechnicalLogs);
        copyResults=button("Copier résultats · toutes les passes",TEAL,this::confirmCopyAllPasses);
        row(content,copyLogs,copyResults);
        copyLogs.setEnabled(true);copyResults.setEnabled(false);
        liquid=button("Tester Liquid + experts (hors ligne)",INK,this::startLiquidReview);
        liquid.setVisibility(View.GONE);liquid.setEnabled(false);content.addView(liquid);
        moe=button("Tester vrai MoE · HobbyLM (hors ligne)",INK,this::startHobbyMoeReview);
        moe.setVisibility(View.GONE);moe.setEnabled(false);content.addView(moe);
        ocrProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);ocrProgress.setMax(100);ocrProgress.setVisibility(View.GONE);ocrProgress.setContentDescription("Progression de la reconnaissance de texte");content.addView(ocrProgress,new LinearLayout.LayoutParams(-1,dp(10)));
        status=label("Choisis Caméra, Photo ou colle un texte.",18);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);status.setPadding(0,dp(8),0,dp(8));content.addView(status);
        text=new EditText(this);text.setHint("Le texte apparaît ici. Tu peux le corriger ou coller un texte.");text.setTextSize(textSp);text.setTextColor(INK);text.setHintTextColor(Color.rgb(74,86,95));text.setGravity(Gravity.TOP);text.setMinLines(5);text.setSingleLine(false);text.setSelectAllOnFocus(false);text.setBackgroundTintList(android.content.res.ColorStateList.valueOf(TEAL));
        text.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50000)});text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);content.addView(text,new LinearLayout.LayoutParams(-1,-2));
        text.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){if(!changingText){sessions.editText();maskedPreview="";stopReading();}}public void afterTextChanged(Editable s){}});
        row(content,button("A−",INK,()->font(-2)),button("A+",INK,()->font(2)),button("Réglages",INK,this::settings));
        row(content,button("Garder masqué",INK,this::saveManual),button("Historique",INK,this::showHistory));
        content.addView(label("L’historique masque noms et coordonnées sur ce téléphone. Relis le résultat avant de partager : des éléments peuvent échapper au masquage.",16));
        row(content,button("Voir le masquage",INK,this::showMasked),button("Aide",INK,this::help));
        play=button("▶ Lire le texte",TEAL,()->{if(reading)stopReading();else readText();});outer.addView(play,new LinearLayout.LayoutParams(-1,dp(64)));
    }
    private void font(int delta){textSp=Math.max(18,Math.min(48,textSp+delta));text.setTextSize(textSp);prefs.edit().putInt("textSize",textSp).apply();}
    private void setCompareAvailable(boolean available){if(compare==null)return;compare.setEnabled(available);compare.setBackgroundTintList(android.content.res.ColorStateList.valueOf(available?TEAL:DISABLED));compare.setAlpha(available?1f:0.65f);}
    private void setIndraAvailable(boolean available){if(indra==null)return;indra.setEnabled(available);indra.setBackgroundTintList(android.content.res.ColorStateList.valueOf(available?TEAL:DISABLED));indra.setAlpha(available?1f:0.65f);}
    private long beginOcrProgress(int value){long id=progressGeneration.incrementAndGet();ui(()->{if(ocrProgress!=null&&progressGeneration.get()==id){ocrProgress.setVisibility(View.VISIBLE);ocrProgress.setIndeterminate(value<0);if(value>=0)ocrProgress.setProgress(value);}});return id;}
    private void updateOcrProgress(long id,int value){ui(()->{if(ocrProgress==null||progressGeneration.get()!=id)return;ocrProgress.setVisibility(View.VISIBLE);ocrProgress.setIndeterminate(value<0);if(value>=0)ocrProgress.setProgress(Math.max(0,Math.min(100,value)));});}
    private void finishOcrProgress(long id){ui(()->{if(ocrProgress==null||progressGeneration.get()!=id)return;ocrProgress.setIndeterminate(false);ocrProgress.setProgress(100);ocrProgress.postDelayed(()->{if(progressGeneration.get()==id)ocrProgress.setVisibility(View.GONE);},900);});}
    private long newDocument(){if(activeMoeStream!=null){activeMoeStream.cancelAndClose();activeMoeStream=null;}stopReading();camera.stop();cameraMode=false;maskedPreview="";lastTrace=null;lastScan=null;lastReview=null;lastMoeReview=null;compareBitmap=null;if(HobbyMoeNative.available())HobbyMoeNative.stop();if(copyLogs!=null)copyLogs.setEnabled(true);if(copyResults!=null)copyResults.setEnabled(false);if(LiquidNative.available())LiquidNative.stop();ocrChoice.incrementAndGet();progressGeneration.incrementAndGet();if(ocrProgress!=null)ocrProgress.setVisibility(View.GONE);if(compare!=null){setCompareAvailable(false);compare.setVisibility(View.GONE);}if(indra!=null){setIndraAvailable(false);indra.setVisibility(View.GONE);}if(liquid!=null){liquid.setEnabled(false);liquid.setVisibility(View.GONE);}if(moe!=null){moe.setEnabled(false);moe.setVisibility(View.GONE);}return sessions.replaceSource();}
    private void startCamera(){
        newDocument();cameraMode=true;imageArea.setVisibility(View.VISIBLE);preview.setVisibility(View.VISIBLE);frozen.setVisibility(View.GONE);freeze.setVisibility(View.VISIBLE);freeze.setEnabled(true);setText("");status.setText("Vise le texte puis appuie sur Figer l’image.");
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
    }
    private void choosePhoto(){newDocument();Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,PHOTO);}catch(ActivityNotFoundException e){status.setText("Aucun sélecteur de photos disponible.");}}
    private void manualText(){newDocument();imageArea.setVisibility(View.GONE);freeze.setVisibility(View.GONE);status.setText("Colle ou écris ton texte, puis appuie sur Lire.");text.requestFocus();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA_PERMISSION&&cameraMode){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else{freeze.setEnabled(false);status.setText("Caméra refusée. Photo et Texte restent disponibles.");}}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();if(request==PHOTO)processImage(()->ImageLoader.fromUri(this,uri));}}
    interface LoadImage{Bitmap get()throws Exception;}
    private void processImage(LoadImage source){
        long ticket=newDocument(),privacy=sessions.historyTicket(),choice=ocrChoice.get();
        imageArea.setVisibility(View.VISIBLE);freeze.setVisibility(View.GONE);preview.setVisibility(View.GONE);
        frozen.setVisibility(View.VISIBLE);frozen.setImageDrawable(null);setText("");
        status.setText("Préparation de l'image…");
        long loading=beginOcrProgress(2);
        worker.execute(()->{
            try {
                if(!sessions.documentCurrent(ticket))return;
                Bitmap image=source.get();
                if(!sessions.documentCurrent(ticket)){image.recycle();return;}
                DebugLog.append(this,"IMAGE ready "+image.getWidth()+"x"+image.getHeight()+" pssMb="+formatMb(processPssMb()));
                ui(()->{
                    if(!sessions.documentCurrent(ticket))return;
                    frozen.setImageBitmap(image);compareBitmap=image;
                    setCompareAvailable(true);compare.setVisibility(View.VISIBLE);
                    indra.setVisibility(View.VISIBLE);setIndraAvailable(false);
                    status.setText("Indra OCR lit l'image, puis vérifie l'ordre des colonnes…");
                });
                finishOcrProgress(loading);
                final long progress=beginOcrProgress(-1);
                indraWorker.execute(()->{
                    OcrTrace trace=new OcrTrace("doc-"+ticket);
                    long start=SystemClock.elapsedRealtime();
                    try {
                        IndraOcrService.Scan scan=indraOcr.scan(this,image);
                        if(!sessions.documentCurrent(ticket)||destroyed)return;
                        long elapsed=SystemClock.elapsedRealtime()-start;
                        float pss=processPssMb();
                        trace.add(this,"P1_INDRA_RAW","indra-det-rec","executed",scan.raw,
                                  scan.detectionMs+Math.max(0,scan.recognitionMs-scan.lmMs),pss,"lines="+scan.lineCount);
                        trace.add(this,"P2_ORDERNET","indra-order.onnx","executed",scan.ordered,
                                  scan.orderMs,pss,"method="+scan.orderMethod+" blocks="+scan.blockCount+" diff_positions="+scan.orderDisagreements+" geometry="+scan.layoutEvidence);
                        trace.add(this,"P2A_XYCUT","geometry-xycut","executed",scan.xycutOrdered,
                                  0,pss,"independent-order-comparison");
                        trace.add(this,"P2B_INDRA_LM","indra-lm.npz-ctc",scan.lmLoaded?"executed":"unavailable",scan.lmLoaded?scan.lmOrdered:"",
                                  scan.lmMs,pss,"uncertain_lines="+scan.lmRuns+" corrections="+scan.lmChanged+" fallback_errors="+scan.lmErrors);
                        trace.add(this,"P2C_XYCUT_LM","xycut-plus-IndraLM","executed",scan.xycutLm,
                                  0,pss,"same-CTC-hypotheses-different-line-order");
                        trace.add(this,"P3_LIQUID","LFM2.5-230M",LiquidNative.available()?"not_run":"unavailable","",0,pss,
                                  "tap-experimental-Liquid-button-to-run");
                        trace.add(this,"P4_MOBILE_MOE","MobileMoE-S-QAT","unavailable","",0,pss,
                                  "mobile-int4-runtime-not-validated");
                        // This is a verifiable no-hallucination fallback, NOT a
                        // neural MoE arbitration pass or a semantic correction.
                        trace.add(this,"P5_ARBITRAGE","no-neural-arbiter","executed",scan.lmOrdered,0,
                                  processPssMb(),"Indra-LM-CTC-evidence-only-no-MoE");
                        lastScan=scan;lastTrace=trace;
                        finishOcrProgress(progress);
                        ui(()->{
                            if(!sessions.documentCurrent(ticket)||compareBitmap!=image)return;
                            setIndraAvailable(true);copyLogs.setEnabled(true);copyResults.setEnabled(true);liquid.setVisibility(View.VISIBLE);liquid.setEnabled(LiquidNative.available());moe.setVisibility(View.VISIBLE);moe.setEnabled(HobbyMoeNative.available());
                            if(ocrChoice.get()==choice)setText(scan.lmOrdered);
                            status.setText("Indra + LM : "+scan.lineCount+" lignes, "+elapsed+" ms. "+
                                "Correction de "+scan.lmChanged+" ligne(s) ; OrderNet/XY-cut à comparer dans le rapport.");
                        });
                        if(!scan.lmOrdered.isBlank()&&prefs.getBoolean("history",true)&&ocrChoice.get()==choice)
                            maskAndSave(scan.lmOrdered,ticket,privacy,true);
                    }catch(Exception|OutOfMemoryError e){
                        DebugLog.exception(this,"INDRA pipeline",e);
                        trace.add(this,"P1_INDRA_RAW","indra-det-rec","error","",
                                  SystemClock.elapsedRealtime()-start,processPssMb(),e.getClass().getSimpleName());
                        lastTrace=trace;finishOcrProgress(progress);ui(()->{if(sessions.documentCurrent(ticket)){copyLogs.setEnabled(true);copyResults.setEnabled(true);}});
                        ui(()->{if(sessions.documentCurrent(ticket)){
                            setIndraAvailable(true);
                            status.setText("Indra indisponible. PP-OCRv6-Small reste utilisable ; consulte le journal.");
                        }});
                    }
                });
            }catch(Exception|OutOfMemoryError e){
                DebugLog.exception(this,"IMAGE load",e);finishOcrProgress(loading);
                ui(()->{if(sessions.documentCurrent(ticket))status.setText("Impossible de préparer cette image.");});
            }
        });
    }

    /** Independent PP-OCR fallback; comparison runs on the same pixels. */
    private void compareOcr(){
        final Bitmap image=compareBitmap;final long ticket=sessions.documentTicket();
        if(image==null||image.isRecycled()||!sessions.documentCurrent(ticket))return;
        setCompareAvailable(false);final long progress=beginOcrProgress(-1);
        status.setText("PP-OCRv6-Small analyse la même image…");
        paddleWorker.execute(()->{
            String value="",error=null;long start=SystemClock.elapsedRealtime();
            try{value=new OcrService().recognize(this,image);}
            catch(Exception|OutOfMemoryError e){error=e.getClass().getSimpleName();DebugLog.exception(this,"PADDLE",e);}
            long ms=SystemClock.elapsedRealtime()-start;
            DebugLog.append(this,"PADDLE done ms="+ms+" chars="+value.length()+" pssMb="+formatMb(processPssMb())+
                             " result="+(error==null?"ok":error));
            finishOcrProgress(progress);
            final String found=value,failure=error;
            ui(()->{
                if(!sessions.documentCurrent(ticket)||compareBitmap!=image)return;
                setCompareAvailable(true);
                if(failure==null)showComparisonText("PP-OCRv6-Small ("+ms+" ms)",found,ticket);
                else status.setText("PP-OCR a échoué : consulte le journal. Indra reste disponible.");
            });
        });
    }
    private void startLiquidReview(){
        final IndraOcrService.Scan scan=lastScan;
        final OcrTrace trace=lastTrace;
        final long ticket=sessions.documentTicket();
        if(scan==null||trace==null||!sessions.documentCurrent(ticket))return;
        if(!LiquidNative.available()){
            status.setText("Moteur Liquid natif indisponible sur cet appareil.");
            return;
        }
        if(!liquid.isEnabled())return;
        new AlertDialog.Builder(this).setTitle("Reconstruction sémantique expérimentale")
            .setMessage("LiquidAI tourne localement sur CPU. Pour limiter la mémoire, il examine les deux premiers blocs "+
                "de 650 caractères maximum, puis deux relectures spécialisées et un arbitrage. "+
                "Les autres blocs sont laissés intacts. Ne remplace jamais le texte automatiquement. "+
                "Cela peut durer plusieurs dizaines de secondes et chauffer le téléphone.")
            .setPositiveButton("Lancer",(d,w)->{
                liquid.setEnabled(false);
                status.setText("Liquid charge son modèle local puis examine les premières lignes…");
                final long progress=beginOcrProgress(-1);
                semanticWorker.execute(()->{
                    if(destroyed||!sessions.documentCurrent(ticket)){finishOcrProgress(progress);return;}
                    try{
                        LiquidOcrReview.Result result=LiquidOcrReview.run(this,scan.lmOrdered,trace);
                        if(!sessions.documentCurrent(ticket)||destroyed)return;
                        lastReview=result;finishOcrProgress(progress);
                        DebugLog.append(this,"LIQUID completed="+result.completed+" total="+result.total+
                           " conflicts="+result.criticalConflicts+" elapsedMs="+result.elapsedMs);
                        ui(()->{
                            if(!sessions.documentCurrent(ticket))return;
                            liquid.setEnabled(true);
                            status.setText("Liquid : "+result.completed+"/"+result.total+" blocs examinés, "+
                                result.elapsedMs+" ms. Texte OCR inchangé ; consulte et choisis.");
                            new AlertDialog.Builder(this).setTitle("Révision Liquid disponible")
                                .setMessage("Une proposition de texte a été générée. "+
                                    "La sélection de l'arbitre n'est pas une preuve indépendante : "+
                                    "vérifie la transcription face à l'image avant de l'utiliser.")
                                .setPositiveButton("Voir proposition",(a,b)->
                                    showComparisonText("Liquid — proposition expérimentale",result.proposal,ticket))
                                .setNeutralButton("Voir arbitrage",(a,b)->
                                    showComparisonText("Liquid — arbitrage (à vérifier)",result.selection,ticket))
                                .setNegativeButton("Fermer",null).show();
                        });
                    }catch(Exception|OutOfMemoryError e){
                        DebugLog.exception(this,"LIQUID review",e);
                        trace.add(this,"P3_LIQUID_ERROR","LiquidAI-LFM2.5-230M","error","",
                            0,processPssMb(),e.getClass().getSimpleName());
                        finishOcrProgress(progress);
                        ui(()->{if(sessions.documentCurrent(ticket)){
                            liquid.setEnabled(true);status.setText("Liquid : erreur de test, OCR Indra conservé. Consulte le rapport.");
                        }});
                    }
                });
            }).setNegativeButton("Annuler",null).show();
    }

    private void startHobbyMoeReview(){
        final IndraOcrService.Scan scan=lastScan;
        final OcrTrace trace=lastTrace;
        final long ticket=sessions.documentTicket();
        if(scan==null||trace==null||!sessions.documentCurrent(ticket))return;
        if(!HobbyMoeNative.available()){
            status.setText("Runtime HobbyLM MoE absent ; OCR Indra conservé.");
            return;
        }
        if(!moe.isEnabled())return;
        final int[] language={0};
        new AlertDialog.Builder(this).setTitle("Vrai MoE indépendant — mémoire et phrases")
            .setSingleChoiceItems(new String[]{"Français","English"},0,(d,w)->language[0]=w)
            .setMessage("HobbyLM-1B est un vrai réseau à experts routés (64 experts, 8 actifs par jeton), "+
                "différent de Liquid. Le format de conversation officiel est rétabli. "+
                "La suite du texte généré apparaîtra au fil des jetons, avec Arrêter et Copier flux. "+
                "Son poids quantifié fait environ 729 Mo et sera copié dans "+
                "le stockage privé au premier essai. La génération sur CPU peut être lente et "+
                "consommer beaucoup de mémoire. Pour ce premier test, seule la première zone "+
                "de trois lignes de 150 caractères maximum sont examinées dans une seule session. "+
                "La session sera déchargée après le lot. Choisis français ou anglais. "+
                "La qualité en français est inconnue ; vérifie tout résultat avant utilisation.")
            .setPositiveButton("Lancer le MoE",(d,w)->{
                moe.setEnabled(false);
                final MoeStreamDialog stream=new MoeStreamDialog(this);
                activeMoeStream=stream;stream.show();
                status.setText("HobbyLM prépare le modèle ; la sortie apparaît en temps réel dans la fenêtre…");
                long progress=beginOcrProgress(-1);
                semanticWorker.execute(()->{
                    if(destroyed||!sessions.documentCurrent(ticket)){finishOcrProgress(progress);return;}
                    long start=SystemClock.elapsedRealtime();
                    try{
                        DebugLog.append(this,"HOBBY_MOE begin engine=routed-hobbylm-1b inputChars="+scan.lmOrdered.length()+" mode=three_lines session=reused chat_template=hobbylm-v1 streamed=true pssMb="+formatMb(processPssMb()));
                        HobbyMoeReview.Result r=HobbyMoeReview.run(
                            this,scan.lmOrdered,trace,language[0]==0?"fr":"en",(delta,count)->{
                                if(!destroyed&&sessions.documentCurrent(ticket))stream.token(delta,count);
                                else HobbyMoeNative.stop();
                            },(index,total,lang,budget)->stream.segment(index,total,lang,budget));
                        if(!sessions.documentCurrent(ticket)||destroyed)return;
                        lastMoeReview=r;finishOcrProgress(progress);
                        ui(()->{
                            if(!sessions.documentCurrent(ticket))return;
                            moe.setEnabled(true);
                            status.setText("Vrai MoE : "+r.ms+" ms, chiffres préservés="+r.numbersKept+
                                ". Indra n'a pas été remplacé ; compare les versions.");
                            // A rejected/hallucinated MoE draft remains visible and
                            // copyable in the live window, but cannot be applied.
                            boolean approved="candidate".equals(r.status);
                            stream.finish("Terminé : "+r.ms+" ms. "+(approved?
                                "Candidat à comparer à la photo avant usage.":
                                "Proposition rejetée : texte Indra conservé."),
                                approved?()->showComparisonText("MoE — candidat à vérifier",r.preview,ticket):null);
                        });
                    }catch(Exception|OutOfMemoryError e){
                        boolean interrupted="Cancelled".equals(e.getMessage());
                        if(!interrupted)DebugLog.exception(this,"HOBBY_MOE",e);
                        // HobbyMoeReview already recorded the interrupted partial
                        // output or error, with a token count and duration.
                        stream.finish(interrupted?"Arrêt demandé. Flux partiel copiable.":
                            "MoE en erreur : copie les logs techniques pour le diagnostic.",null);
                        finishOcrProgress(progress);
                        ui(()->{if(sessions.documentCurrent(ticket)){
                            moe.setEnabled(true);
                            status.setText(interrupted?"MoE arrêté : texte Indra préservé.":
                                "MoE échoué : texte Indra préservé. Copie les logs techniques.");
                        }});
                    }
                });
            }).setNegativeButton("Annuler",null).show();
    }

    private void compareIndra(){
        final OcrTrace trace=lastTrace;final long ticket=sessions.documentTicket();
        if(trace==null||!sessions.documentCurrent(ticket))return;
        new AlertDialog.Builder(this).setTitle("Analyse multicouche — VMR-013")
            .setMessage(trace.summary()+"\nLes boutons sous le rapport copient les logs techniques ou le texte intégral. "+
                "Chaque passe est aussi copiable séparément. MobileMoE Meta est verrouillé ; les passes HobbyLM sont distinctes et vérifiables.")
            .setPositiveButton("Copier une passe",(d,w)->choosePassToCopy(trace,ticket))
            .setNeutralButton("Comparer les ordres",(d,w)->showReadingAlternatives(ticket))
            .setNegativeButton("Fermer",null).show();
    }
    /** One tap: anonymized technical diagnostics (never the OCR text). */
    private void copyTechnicalLogs(){
        final OcrTrace trace=lastTrace;final long ticket=sessions.documentTicket();
        String metrics=(trace!=null&&sessions.documentCurrent(ticket))?trace.metricsReport():
            "VISIONMAXREAD — JOURNAL TECHNIQUE APRÈS RELANCE\nAucun rapport OCR actif.\n";
        String message=metrics+"\n=== JOURNAL DEBUG PERSISTANT ===\n"+DebugLog.read(this);
        copyToClipboard("VisionMaxRead diagnostics sans texte",message,ticket,false);
    }
    /** Text exports require explicit user confirmation. No screenshots needed. */
    private void confirmCopyAllPasses(){
        final OcrTrace trace=lastTrace;final long ticket=sessions.documentTicket();
        if(trace==null||!sessions.documentCurrent(ticket))return;
        confirmSensitiveCopy("Copier TOUTES les passes",trace.fullReport(),ticket);
    }
    private void choosePassToCopy(OcrTrace trace,long ticket){
        if(!sessions.documentCurrent(ticket))return;
        List<OcrTrace.Pass> passes=trace.snapshot();
        if(passes.isEmpty()){status.setText("Aucune passe à copier.");return;}
        String[] labels=new String[passes.size()];
        for(int i=0;i<passes.size();i++){
            OcrTrace.Pass p=passes.get(i);
            labels[i]=p.key+" · "+p.status+" · "+p.text.length()+" caractères";
        }
        new AlertDialog.Builder(this).setTitle("Choisir une passe à copier")
            .setItems(labels,(d,which)->{
                if(!sessions.documentCurrent(ticket))return;
                OcrTrace.Pass pass=passes.get(which);
                confirmSensitiveCopy("Copier "+pass.key,trace.passReport(which),ticket);
            }).setNegativeButton("Fermer",null).show();
    }
    private void confirmSensitiveCopy(String title,String value,long ticket){
        if(!sessions.documentCurrent(ticket))return;
        new AlertDialog.Builder(this).setTitle(title)
            .setMessage("Cette copie peut contenir des textes privés, noms, dates ou références. "+
                "Le presse-papiers est accessible à d'autres applications. "+
                "Relis et anonymise avant de me l'envoyer.")
            .setPositiveButton("Copier",(d,w)->copyToClipboard(title,value,ticket,true))
            .setNegativeButton("Annuler",null).show();
    }
    private void copyToClipboard(String title,String value,long ticket,boolean sensitive){
        if(sensitive&&!sessions.documentCurrent(ticket))return;
        ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
            .setPrimaryClip(ClipData.newPlainText(title,value));
        status.setText(sensitive?"Résultats copiés. Pense à anonymiser avant partage.":
                  "Logs techniques copiés : colle-les directement dans notre discussion.");
    }

    private void showReadingAlternatives(long ticket){
        final IndraOcrService.Scan scan=lastScan;
        if(scan==null||!sessions.documentCurrent(ticket))return;
        String[] choices={"OrderNet + correction Indra LM","XY-cut + correction Indra LM",
                          "OrderNet sans correction","XY-cut sans correction"};
        new AlertDialog.Builder(this).setTitle("Choisir l'ordre de lecture")
            .setItems(choices,(d,which)->{
                String chosen=which==0?scan.lmOrdered:which==1?scan.xycutLm:
                              which==2?scan.ordered:scan.xycutOrdered;
                showComparisonText(choices[which],chosen,ticket);
            }).setNegativeButton("Fermer",null).show();
    }
    private void showSensitiveReport(OcrTrace trace,long ticket){
        if(!sessions.documentCurrent(ticket))return;
        ScrollView scroll=new ScrollView(this);
        TextView t=label(trace.fullReport(),15);t.setTextIsSelectable(true);scroll.addView(t);
        new AlertDialog.Builder(this).setTitle("Rapport AVEC textes : données sensibles")
            .setView(scroll)
            .setPositiveButton("Copier après vérification",(d,w)->{
                if(!sessions.documentCurrent(ticket))return;
                new AlertDialog.Builder(this).setTitle("Confirmer la copie")
                    .setMessage("Ce rapport contient les textes reconnus, éventuellement des noms et données privées. "+
                        "Le presse-papiers Android peut être lu par d'autres applications : partage uniquement après relecture.")
                    .setPositiveButton("Copier",(a,b)->{
                        ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
                            .setPrimaryClip(ClipData.newPlainText("VisionMaxRead étapes OCR — sensible",trace.fullReport()));
                        status.setText("Rapport copié : relis/anonymise avant de le transmettre.");
                    }).setNegativeButton("Annuler",null).show();
            }).setNegativeButton("Fermer",null).show();
    }
    private void showComparisonText(String name,String value,long ticket){
        if(!sessions.documentCurrent(ticket))return;
        ScrollView scroll=new ScrollView(this);
        scroll.setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView result=label(value.isEmpty()?"Aucun texte reconnu.":value,textSp);
        result.setTextIsSelectable(true);
        scroll.addView(result);
        new AlertDialog.Builder(this).setTitle(name+" — texte reconnu")
            .setView(scroll)
            .setPositiveButton("Utiliser ce texte",(d,w)->{
                if(!sessions.documentCurrent(ticket))return;
                ocrChoice.incrementAndGet();sessions.editText();maskedPreview="";stopReading();setText(value);
                DebugLog.append(this,"OCR selected engine="+name+" chars="+value.length());
                status.setText("Texte "+name+" sélectionné. Tu peux le corriger puis le faire lire.");
            })
            .setNegativeButton("Fermer",null).show();
    }

    private float processPssMb(){Debug.MemoryInfo info=new Debug.MemoryInfo();Debug.getMemoryInfo(info);return info.getTotalPss()/1024f;}
    private String formatMb(float mb){return mb<0?"n/d":String.format(Locale.FRANCE,"%.0f Mo",mb);}
    private void setText(String value){changingText=true;text.setText(value);changingText=false;}
    private ModelFiles models()throws Exception {if(models==null)models=new ModelFiles(this);return models;}
    private void saveManual(){String value=text.getText().toString();if(value.trim().isEmpty()){status.setText("Ajoute un texte à masquer.");return;}long ticket=sessions.documentTicket(),privacy=sessions.historyTicket();status.setText("Masquage local en préparation…");worker.execute(()->maskAndSave(value,ticket,privacy,true));}
    private void maskAndSave(String value,long ticket,long privacy,boolean save){
        try{
            if(!sessions.maskCurrent(ticket,privacy))return;
            HistoryStore.MaskedText masked=HistoryStore.MaskedText.analyze(value,models(),()->!sessions.maskCurrent(ticket,privacy)||destroyed);
            if(!sessions.maskCurrent(ticket,privacy))return;
            // This check and write execute under the same UI generation before a new document can arrive.
            ui(()->{
                if(!sessions.maskCurrent(ticket,privacy))return;maskedPreview=masked.value;
                try{if(save)history.add(masked);if(!reading)status.setText(save?"Texte masqué conservé dans l’historique local.":"Masquage prêt à relire.");}
                catch(Exception e){if(!reading)status.setText("Historique indisponible. Aucun texte brut n’a été conservé.");}
            });
        }catch(CancellationException ignored){}
        catch(Exception|OutOfMemoryError e){ui(()->{if(sessions.maskCurrent(ticket,privacy)&&!reading)status.setText("Masquage indisponible : rien n’est enregistré. Tu peux toujours lire le texte.");});}
    }
    private void readText(){
        String value=text.getText().toString();if(value.trim().isEmpty()){status.setText("Il n’y a pas encore de texte à lire.");return;}
        long ticket=speech.next();reading=true;play.setText("■ Arrêter la lecture");status.setText("Préparation de "+voice.voiceName()+" · Sherpa-ONNX/Piper…");
        float speed=prefs.getFloat("speed",1.0f);
        worker.execute(()->{
            try{
                java.util.function.BooleanSupplier cancelled=()->!speech.current(ticket)||destroyed;
                java.util.function.Consumer<String> report=message->ui(()->{if(speech.current(ticket))status.setText(message);});
                voice.speak(this,value,speed,cancelled,report);
                ui(()->{if(speech.current(ticket)){reading=false;play.setText("▶ Lire le texte");status.setText("Lecture terminée.");}});
            }catch(CancellationException ignored){ui(()->{if(speech.current(ticket)){reading=false;play.setText("▶ Lire le texte");status.setText("Lecture interrompue.");}});}
            catch(Exception|OutOfMemoryError e){ui(()->{if(speech.current(ticket)){reading=false;play.setText("▶ Lire le texte");status.setText("Voix Piper indisponible. Réessaie après fermeture des applications lourdes ou sélectionne Jessica.");}});}
        });
    }
    private void stopReading(){speech.next();voice.stop();if(reading){reading=false;play.setText("▶ Lire le texte");status.setText("Lecture arrêtée.");}}
    private void showMasked(){
        if(maskedPreview.isEmpty()){
            String value=text.getText().toString();if(value.trim().isEmpty()){status.setText("Ajoute un texte à masquer.");return;}
            long ticket=sessions.documentTicket(),privacy=sessions.historyTicket();status.setText("Masquage local en préparation…");worker.execute(()->maskAndSave(value,ticket,privacy,false));return;
        }
        EditText result=new EditText(this);result.setTextSize(textSp);result.setText(maskedPreview);result.setMinLines(5);
        ScrollView scroll=new ScrollView(this);scroll.setPadding(dp(16),dp(8),dp(16),dp(8));scroll.addView(result);
        new AlertDialog.Builder(this).setTitle("Relis le texte masqué").setMessage("Corrige les éléments encore identifiants avant de partager.").setView(scroll)
            .setPositiveButton("Partager",(d,w)->{String safe=result.getText().toString();Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,safe);startActivity(Intent.createChooser(send,"Partager le texte relu"));})
            .setNeutralButton("Copier",(d,w)->((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Texte masqué",result.getText().toString())))
            .setNegativeButton("Fermer",null).show();
    }
    private void showHistory(){
        stopReading();try{
            JSONArray entries=history.read();if(entries.length()==0){new AlertDialog.Builder(this).setMessage("L’historique masqué est vide.").setPositiveButton("Fermer",null).show();return;}
            String[] labels=new String[entries.length()];for(int i=0;i<entries.length();i++){JSONObject entry=entries.getJSONObject(i);String value=entry.getString("text");labels[i]=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,Locale.FRANCE).format(new Date(entry.getLong("time")))+"\n"+value.substring(0,Math.min(70,value.length()));}
            new AlertDialog.Builder(this).setTitle("Historique local · textes masqués").setItems(labels,(d,which)->{
                try{newDocument();imageArea.setVisibility(View.GONE);freeze.setVisibility(View.GONE);String value=entries.getJSONObject(which).getString("text");setText(value);maskedPreview=value;status.setText("Texte masqué chargé. Appuie sur Lire pour l’écouter.");}catch(JSONException e){status.setText("Entrée illisible.");}
            }).setNeutralButton("Tout effacer",(d,w)->confirmClear()).setNegativeButton("Fermer",null).show();
        }catch(Exception e){status.setText("Historique illisible. Tu peux l’effacer dans les réglages.");}
    }
    private void confirmClear(){new AlertDialog.Builder(this).setTitle("Effacer l’historique ?").setMessage("Les textes masqués conservés sur ce téléphone seront supprimés.").setPositiveButton("Effacer",(d,w)->{sessions.clearHistory();history.clear();maskedPreview="";status.setText("Historique effacé.");}).setNegativeButton("Annuler",null).show();}
    private void settings(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
        CheckBox keep=new CheckBox(this);keep.setText("Conserver les textes après masquage local");keep.setTextSize(18);keep.setChecked(prefs.getBoolean("history",true));panel.addView(keep);
        CheckBox hdVoice=new CheckBox(this);hdVoice.setText("Voix Siwis HD (français, articulation précise)");hdVoice.setTextSize(18);hdVoice.setChecked(prefs.getBoolean("voiceHD",true));panel.addView(hdVoice);
        panel.addView(label("Siwis HD ou Jessica (voix originale), moteur Sherpa-ONNX/Piper hors ligne ; une seule voix chargée.",16));
        TextView speedLabel=label("Vitesse de lecture",18);panel.addView(speedLabel);SeekBar speed=new SeekBar(this);speed.setMax(100);speed.setProgress(Math.round((prefs.getFloat("speed",1.0f)-0.75f)*100));panel.addView(speed);speed.setContentDescription("Vitesse de lecture de 0,75 à 1,75");
        panel.addView(button("Diagnostic 1re phrase",INK,()->worker.execute(()->{
            try{String diag=voice.diagnoseFirstPass(this);DebugLog.append(this,"PIPER diagnostic "+diag);ui(()->status.setText("Diagnostic Piper : "+diag));}
            catch(Exception e){DebugLog.exception(this,"PIPER diagnostic",e);ui(()->status.setText("Diagnostic Piper indisponible."));}
        })));
        panel.addView(button("Journal debug",INK,this::showDebugLog));
        panel.addView(button("Effacer l’historique",RED,this::confirmClear));
        ScrollView settingsScroll=new ScrollView(this);settingsScroll.addView(panel);
        new AlertDialog.Builder(this).setTitle("Réglages").setView(settingsScroll).setPositiveButton("Enregistrer",(d,w)->{stopReading();boolean hiFi=hdVoice.isChecked();prefs.edit().putBoolean("history",keep.isChecked()).putBoolean("voiceHD",hiFi).putFloat("speed",0.75f+speed.getProgress()/100f).apply();worker.execute(()->voice.selectVoice(hiFi));if(!keep.isChecked())sessions.clearHistory();}).setNegativeButton("Annuler",null).show();
    }
    private void showDebugLog(){
        String value=DebugLog.read(this);
        TextView log=label(value,14);log.setTextIsSelectable(true);log.setTypeface(Typeface.MONOSPACE);
        ScrollView scroll=new ScrollView(this);scroll.setPadding(dp(12),dp(8),dp(12),dp(8));scroll.addView(log);
        new AlertDialog.Builder(this).setTitle("Journal debug local").setMessage("Aucun texte OCR brut ni image n’est écrit dans ce journal.")
            .setView(scroll)
            .setPositiveButton("Copier",(d,w)->((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("VisionMaxRead debug",value)))
            .setNeutralButton("Effacer",(d,w)->{DebugLog.clear(this);status.setText("Journal debug effacé.");})
            .setNegativeButton("Fermer",null).show();
    }
    private void help(){new AlertDialog.Builder(this).setTitle("Lire avec VisionMaxRead").setMessage("1. Caméra ou Photo : choisir une image.\n2. Indra reconnaît le texte et OrderNet reconstruit les colonnes.\n3. Le bouton Rapport présente toutes les passes, y compris celles indisponibles.\n4. PP-OCRv6-Small sert de secours.\n5. Lire vocalement avec Siwis ou Jessica.\n\nLe journal standard contient des métriques, jamais les textes. L'export avec textes nécessite une confirmation.\n\nVMR-013 LAB · Indra + Liquid hors ligne, boutons Copier logs / Copier résultats et Copier une passe ; Meta MobileMoE non porté.").setPositiveButton("Fermer",null).show();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);incoming(intent);}
    private void incoming(Intent intent){
        if(intent==null)return;String action=intent.getAction();
        if(Intent.ACTION_PROCESS_TEXT.equals(action)||Intent.ACTION_SEND.equals(action)){
            CharSequence value=intent.getCharSequenceExtra(Intent.ACTION_PROCESS_TEXT.equals(action)?Intent.EXTRA_PROCESS_TEXT:Intent.EXTRA_TEXT);
            if(value!=null){manualText();String raw=value.toString();setText(raw.substring(0,Math.min(50000,raw.length())));if(prefs.getBoolean("history",true))saveManual();return;}
            if(Intent.ACTION_SEND.equals(action)&&intent.getType()!=null&&intent.getType().startsWith("image/")){Uri uri=intent.getParcelableExtra(Intent.EXTRA_STREAM);if(uri!=null)processImage(()->ImageLoader.fromUri(this,uri));}
        }
    }
    private void ui(Runnable action){runOnUiThread(()->{if(!destroyed)action.run();});}
    @Override protected void onPause(){super.onPause();if(camera!=null)camera.stop();if(freeze!=null)freeze.setEnabled(false);stopReading();}
    @Override protected void onResume(){super.onResume();if(cameraMode&&camera!=null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){camera.start(sessions.sourceTicket());freeze.setEnabled(true);}}
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)DebugLog.append(this,"MEMORY trim level="+level+" pssMb="+formatMb(processPssMb()));}
    @Override protected void onDestroy(){destroyed=true;DebugLog.append(this,"SESSION destroy");sessions.replaceSource();sessions.clearHistory();speech.next();voice.release();if(camera!=null)camera.destroy();if(LiquidNative.available())LiquidNative.stop();if(HobbyMoeNative.available())HobbyMoeNative.stop();if(activeMoeStream!=null)activeMoeStream.cancelAndClose();semanticWorker.shutdownNow();worker.shutdownNow();paddleWorker.shutdownNow();indraWorker.shutdownNow();super.onDestroy();}
}
