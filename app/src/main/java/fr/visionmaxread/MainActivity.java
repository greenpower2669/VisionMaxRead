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
import java.io.File;
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
    private final ExecutorService specialistWorker=Executors.newSingleThreadExecutor();
    // Model downloads live in a foreground Service, not this Activity.
    private SpecialistOcrModels.Spec pendingInstallOcr;
    private long pendingInstallTicket=-1;
    private boolean installReceiverRegistered;
    private final BroadcastReceiver installEvents=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){syncInstallState();}
    };
    private final SpecialistOcrProcess specialistOcr=new SpecialistOcrProcess();
    private final AtomicLong ocrChoice=new AtomicLong(),progressGeneration=new AtomicLong();
    private final ReaderSessions sessions=new ReaderSessions();
    private final SessionGate speech=new SessionGate();
    private final PiperSpeechEngine voice=new PiperSpeechEngine();
    private final IndraOcrService indraOcr=new IndraOcrService();
    private TextureView preview;private ImageView frozen;private FrameLayout imageArea;private ProgressBar ocrProgress;
    // Only an in-memory reference to the displayed photo, never copied to persistent storage.
    private Bitmap compareBitmap;
    private EditText text;private TextView status;private Button freeze,play,compare,indra,copyLogs,copyResults,specialistStop;private CameraController camera;
    private HistoryStore history;private ModelFiles models;private SharedPreferences prefs;
    private boolean changingText,destroyed,reading,cameraMode;private String maskedPreview="";private int textSp=24;
    private volatile OcrTrace lastTrace;
    private volatile IndraOcrService.Scan lastScan;
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
        content.addView(label("OCR et lecture hors ligne après installation automatique des modèles.",16));
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
        content.addView(button("OCR spécialisés · choisir un moteur",TEAL,this::showOcrEngines));
        content.addView(button("Préparer mes OCR automatiquement · Wi-Fi",TEAL,
            ()->requestAutoInstall(new SpecialistOcrModels.Spec[]{
                SpecialistOcrModels.LIGHTON,SpecialistOcrModels.SURYA},false,null)));
        specialistStop=button("■ Arrêter l’OCR spécialisé",RED,()->{
            if(SpecialistOcrInstallService.isWorking(this))SpecialistOcrInstallService.cancel(this);
            specialistOcr.stop();
            status.setText("Arrêt demandé. La copie partielle reste disponible pour une reprise ultérieure.");
        });
        specialistStop.setVisibility(View.GONE);content.addView(specialistStop);
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
    private long newDocument(){
        specialistOcr.stop();
        if(specialistStop!=null)specialistStop.setVisibility(
            SpecialistOcrInstallService.isWorking(this)?View.VISIBLE:View.GONE);
        stopReading();camera.stop();cameraMode=false;maskedPreview="";
        lastTrace=null;lastScan=null;compareBitmap=null;
        if(copyLogs!=null)copyLogs.setEnabled(true);
        if(copyResults!=null)copyResults.setEnabled(false);
        ocrChoice.incrementAndGet();progressGeneration.incrementAndGet();
        if(ocrProgress!=null)ocrProgress.setVisibility(View.GONE);
        if(compare!=null){setCompareAvailable(false);compare.setVisibility(View.GONE);}
        if(indra!=null){setIndraAvailable(false);indra.setVisibility(View.GONE);}
        return sessions.replaceSource();
    }
    private void startCamera(){
        newDocument();cameraMode=true;imageArea.setVisibility(View.VISIBLE);preview.setVisibility(View.VISIBLE);frozen.setVisibility(View.GONE);freeze.setVisibility(View.VISIBLE);freeze.setEnabled(true);setText("");status.setText("Vise le texte puis appuie sur Figer l’image.");
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
    }
    private void choosePhoto(){newDocument();Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,PHOTO);}catch(ActivityNotFoundException e){status.setText("Aucun sélecteur de photos disponible.");}}
    private void manualText(){newDocument();imageArea.setVisibility(View.GONE);freeze.setVisibility(View.GONE);status.setText("Colle ou écris ton texte, puis appuie sur Lire.");text.requestFocus();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA_PERMISSION&&cameraMode){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else{freeze.setEnabled(false);status.setText("Caméra refusée. Photo et Texte restent disponibles.");}}}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PHOTO && result==RESULT_OK && data!=null&&data.getData()!=null){
            Uri photo=data.getData();
            processImage(()->ImageLoader.fromUri(this,photo));
        }
    }
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
                        // These are *not* executed until native Android image backends pass validation.
                        for(OcrEngineCatalog.Entry entry:OcrEngineCatalog.PLANNED)
                            trace.add(this,"TARGET_"+entry.id,entry.name,"unavailable","",0,pss,
                                      "image-runtime-not-integrated");
                        lastScan=scan;lastTrace=trace;
                        finishOcrProgress(progress);
                        ui(()->{
                            if(!sessions.documentCurrent(ticket)||compareBitmap!=image)return;
                            setIndraAvailable(true);copyLogs.setEnabled(true);copyResults.setEnabled(true);
                            if(ocrChoice.get()==choice)setText(scan.lmOrdered);
                            status.setText("Indra + CTC : "+scan.lineCount+" lignes, "+elapsed+" ms. "+
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
    private void showOcrEngines(){
        String[] choices={
            "Indra OCR (actif) · PP-OCRv6-Small",
            "Surya OCR 2 · "+ocrAvailability(SpecialistOcrModels.SURYA),
            "LightOnOCR-3 0.8B · "+ocrAvailability(SpecialistOcrModels.LIGHTON),
            "Nemotron 3.5 Parse · portage ONNX spécifique nécessaire"
        };
        new AlertDialog.Builder(this).setTitle("OCR hors ligne · sélectionner un moteur")
            .setItems(choices,(dialog,which)->{
                if(which==0){status.setText("Indra OCR actif ; PP-OCRv6-Small disponible en comparaison.");return;}
                if(which==3){
                    new AlertDialog.Builder(this).setTitle("Nemotron 3.5 Parse")
                        .setMessage("Architecture C-RADIO + mBART : ne fonctionne pas avec le chargeur GGUF. "+
                            "Portage ONNX Android spécifique encore nécessaire. Aucun résultat simulé.")
                        .setPositiveButton("Fermer",null).show();return;
                }
                showSpecialistActions(which==1?SpecialistOcrModels.SURYA:SpecialistOcrModels.LIGHTON);
            }).setNegativeButton("Fermer",null).show();
    }
    private String ocrAvailability(SpecialistOcrModels.Spec spec){
        if(!specialistOcr.available(this))return "moteur natif non inclus";
        return SpecialistOcrModels.ready(this,spec)?"installé · lancement hors ligne":"installation automatique Wi-Fi";
    }
    private void showSpecialistActions(SpecialistOcrModels.Spec spec){
        new AlertDialog.Builder(this).setTitle(spec.title)
            .setMessage("L'application installe automatiquement les fichiers GGUF vérifiés sur Wi-Fi. "+
                "Aucune manipulation de fichiers n'est nécessaire. Environ "+
                (spec.engine==SpecialistOcrModels.Engine.SURYA2?"1,5 Go":"750 Mo")+
                " de stockage ; un seul moteur chargé en RAM à la fois. "+
                "Aucune photo ni transcription n'est envoyée. Tu peux lire avec Indra sans installer ce moteur.")
            .setItems(new String[]{
                "Reconnaître la photo (installer automatiquement si nécessaire)",
                "Installer maintenant sur Wi-Fi pour utilisation hors ligne",
                "Libérer le stockage occupé par ce moteur"
            },(d,index)->{
                if(index==0)runSpecialistOcr(spec);
                else if(index==1)requestAutoInstall(new SpecialistOcrModels.Spec[]{spec},false,null);
                else confirmDeleteModel(spec);
            }).setNegativeButton("Fermer",null).show();
    }
    private void confirmDeleteModel(SpecialistOcrModels.Spec spec){
        new AlertDialog.Builder(this).setTitle("Supprimer les poids locaux de "+spec.title+" ?")
            .setMessage("La reconnaissance Indra reste utilisable. Le modèle pourra être réinstallé automatiquement sur Wi-Fi.")
            .setPositiveButton("Libérer le stockage",(d,w)->{
                if(SpecialistOcrInstallService.isWorking(this)){status.setText("Arrête d'abord l'installation en cours.");return;}
                specialistOcr.stop();
                long removed=0;
                for(SpecialistOcrModels.Part part:SpecialistOcrModels.Part.values()){
                    File f=SpecialistOcrModels.path(this,spec,part);
                    if(f.isFile()&&f.delete())removed++;
                    File partial=new File(f.getParentFile(),f.getName()+".part");
                    if(partial.isFile())partial.delete();
                }
                status.setText("Stockage libéré pour "+spec.title+" ("+removed+" fichier(s)).");
            }).setNegativeButton("Annuler",null).show();
    }
    private void requestAutoInstall(SpecialistOcrModels.Spec[] targets,boolean runAfter,
                                    SpecialistOcrModels.Spec selected){
        if(SpecialistOcrInstallService.isWorking(this)){
            status.setText("Installation déjà en cours en arrière-plan ; voir la notification Android.");return;
        }
        String message=(targets.length==2?"Installer automatiquement LightOnOCR-3 et Surya OCR 2 (environ 2,2 Go) ?":
            "Installer automatiquement "+targets[0].title+" ?")+
            "\n\nConnexion Wi-Fi non facturée et espace libre nécessaires. "+
            "Une seule installation initiale ; les prochaines lectures fonctionnent sans réseau. "+
            "Aucun document n'est transféré. Les fichiers sont vérifiés par SHA-256. "+
            "Tu peux quitter cet écran : le service Android conserve le téléchargement et ses fichiers partiels.";
        new AlertDialog.Builder(this).setTitle("Préparer les modèles OCR")
            .setMessage(message)
            .setPositiveButton("Installer automatiquement",(d,w)->beginAutoInstall(targets,runAfter,selected))
            .setNegativeButton("Plus tard",null).show();
    }
    private void beginAutoInstall(SpecialistOcrModels.Spec[] targets,boolean runAfter,
                                  SpecialistOcrModels.Spec selected){
        pendingInstallOcr=runAfter?selected:null;
        pendingInstallTicket=runAfter?sessions.documentTicket():-1;
        specialistStop.setVisibility(View.VISIBLE);
        status.setText("Installation en arrière-plan sur Wi-Fi ; progression dans les notifications.");
        try {
            SpecialistOcrInstallService.install(this,targets);
            if(Build.VERSION.SDK_INT>=33 &&
               checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        } catch(RuntimeException e) {
            pendingInstallOcr=null;
            specialistStop.setVisibility(View.GONE);
            status.setText("Démarrage du service refusé par Android : réessaie depuis l'application.");
            DebugLog.append(this,"VMR021_INSTALL start failed="+e.getClass().getSimpleName());
        }
    }
    private void syncInstallState(){
        if(destroyed || status==null)return;
        SharedPreferences p=getSharedPreferences(SpecialistOcrInstallService.PREFS,MODE_PRIVATE);
        String state=p.getString(SpecialistOcrInstallService.KEY_STATE,"");
        String message=p.getString(SpecialistOcrInstallService.KEY_MESSAGE,"");
        if(state.isEmpty())return;
        boolean running="running".equals(state);
        if(running && !SpecialistOcrInstallService.isWorking(this)){
            // A process death may leave a persisted "running" UI state, but no
            // active worker. Preserve .part files and offer a new user-initiated start.
            running=false;
            state="interrupted";
            message="Installation interrompue. Tu peux reprendre sur Wi-Fi.";
            p.edit().putString(SpecialistOcrInstallService.KEY_STATE,state)
                .putString(SpecialistOcrInstallService.KEY_MESSAGE,message).apply();
        }
        specialistStop.setVisibility(running?View.VISIBLE:View.GONE);
        if(running || pendingInstallOcr!=null)status.setText(message);
        if("done".equals(state) && pendingInstallOcr!=null){
            SpecialistOcrModels.Spec spec=pendingInstallOcr;
            long ticket=pendingInstallTicket;
            pendingInstallOcr=null; pendingInstallTicket=-1;
            if(sessions.documentCurrent(ticket))runSpecialistOcr(spec);
            else status.setText("Modèles prêts hors ligne ; sélectionne à nouveau la photo.");
        }else if(!running && !"done".equals(state)){
            pendingInstallOcr=null; pendingInstallTicket=-1;
            status.setText(message);
        }
    }
    private void runSpecialistOcr(SpecialistOcrModels.Spec spec){
        final Bitmap snapshot=compareBitmap;
        final long ticket=sessions.documentTicket();
        if(snapshot==null||snapshot.isRecycled()||!sessions.documentCurrent(ticket)){
            status.setText("Choisis d'abord une photo. Aucun OCR spécialisé lancé.");return;
        }
        if(!specialistOcr.available(this)||!SpecialistOcrModels.ready(this,spec)){
            if(!specialistOcr.available(this)){status.setText("Moteur natif Android indisponible.");return;}
            requestAutoInstall(new SpecialistOcrModels.Spec[]{spec},true,spec);return;
        }
        status.setText(spec.title+" : authentification puis analyse réelle des pixels…");
        specialistStop.setVisibility(View.VISIBLE);
        final long progress=beginOcrProgress(-1);
        specialistWorker.execute(()->{
            try{
                SpecialistOcrProcess.Result result=specialistOcr.recognize(this,spec,snapshot);
                if(!sessions.documentCurrent(ticket)||destroyed)return;
                if(lastTrace!=null)lastTrace.add(this,"VMR019_"+spec.engine,spec.title,"executed",
                    result.text,result.elapsedMs,processPssMb(),"real-native-mtmd-image-ocr");
                finishOcrProgress(progress);
                ui(()->{
                    if(!sessions.documentCurrent(ticket))return;
                    specialistStop.setVisibility(View.GONE);
                    status.setText(spec.title+" : texte OCR obtenu en "+result.elapsedMs+
                        " ms. Compare avec la photo puis décide d'utiliser ou non ce texte.");
                    showComparisonText(spec.title+" · à vérifier",result.text,ticket);
                });
            }catch(Exception|OutOfMemoryError e){
                DebugLog.append(this,"VMR019_OCR engine="+spec.engine+" outcome=error type="+e.getClass().getSimpleName());
                finishOcrProgress(progress);
                ui(()->{if(sessions.documentCurrent(ticket)){
                    specialistStop.setVisibility(View.GONE);
                    status.setText(spec.title+" n'a pas produit de texte exploitable ("+
                        e.getClass().getSimpleName()+"). Indra conservé ; voir le journal.");
                }});
            }
        });
    }
    private void compareIndra(){
        final OcrTrace trace=lastTrace;final long ticket=sessions.documentTicket();
        if(trace==null||!sessions.documentCurrent(ticket))return;
        new AlertDialog.Builder(this).setTitle("Rapport des passes OCR")
            .setMessage(trace.summary()+"\nLes boutons sous le rapport copient les logs techniques ou le texte intégral. "+
                "Chaque passe est aussi copiable séparément. Les trois nouveaux moteurs sont indiqués indisponibles tant que leurs portages Android ne sont pas validés.")
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
        panel.addView(label("Indra OCR actif. PP-OCRv6-Small disponible comme secours.",18));
        panel.addView(label("Surya et LightOn : installation des poids automatiquement sur Wi-Fi, une seule fois. Nemotron : portage Android encore à réaliser.",16));
        panel.addView(button("Voir les trois moteurs OCR",INK,this::showOcrEngines));
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
    private void help(){
        new AlertDialog.Builder(this).setTitle("VisionMaxRead — OCR hors ligne")
            .setMessage("1. Caméra ou Photo : capture une image.\\n2. Indra reste automatique.\\n3. PP-OCR est un secours.\\n4. Surya et LightOn installent automatiquement leurs modèles sur Wi-Fi (puis hors ligne).\\n5. Nemotron attend son portage.\\n6. La voix Piper lit le texte choisi.\\n\\nLes résultats OCR spécialisés sont proposés, jamais appliqués automatiquement.")
            .setPositiveButton("Fermer",null).show();
    }
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
    @Override protected void onPause(){
        if(installReceiverRegistered){unregisterReceiver(installEvents);installReceiverRegistered=false;}
        if(camera!=null)camera.stop();
        if(freeze!=null)freeze.setEnabled(false);
        stopReading();
        super.onPause();
    }
    @Override protected void onResume(){super.onResume();
        if(!installReceiverRegistered){
            IntentFilter filter=new IntentFilter(SpecialistOcrInstallService.EVENT);
            if(Build.VERSION.SDK_INT>=33)registerReceiver(installEvents,filter,Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(installEvents,filter);
            installReceiverRegistered=true;
        }
        syncInstallState();
        if(cameraMode&&camera!=null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){camera.start(sessions.sourceTicket());freeze.setEnabled(true);}}
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)DebugLog.append(this,"MEMORY trim level="+level+" pssMb="+formatMb(processPssMb()));}
    @Override protected void onDestroy(){destroyed=true;specialistOcr.stop();DebugLog.append(this,"SESSION destroy");
        sessions.replaceSource();sessions.clearHistory();speech.next();voice.release();if(camera!=null)camera.destroy();
        // Keep the foreground installer alive when this Activity is destroyed.
        specialistWorker.shutdownNow();worker.shutdownNow();paddleWorker.shutdownNow();indraWorker.shutdownNow();super.onDestroy();}
}
