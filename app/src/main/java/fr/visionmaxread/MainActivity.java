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
    private static final int CAMERA_PERMISSION=10,PHOTO=20,SPECIALIST_MODEL=30;
    private static final int INK=Color.rgb(16,44,60),PAPER=Color.rgb(255,249,238),TEAL=Color.rgb(0,105,92),RED=Color.rgb(179,38,32),DISABLED=Color.rgb(145,145,145);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService qwenWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService paddleWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService modelWorker=Executors.newSingleThreadExecutor();
    private final AtomicLong ocrChoice=new AtomicLong(),progressGeneration=new AtomicLong();
    private final ReaderSessions sessions=new ReaderSessions();
    private final SessionGate speech=new SessionGate();
    private final PiperSpeechEngine voice=new PiperSpeechEngine();
    private final QwenOcrService qwen=new QwenOcrService();
    private final SpecializedOcrService specialistOcr=new SpecializedOcrService();
    private TextureView preview;private ImageView frozen;private FrameLayout imageArea;private ProgressBar ocrProgress;
    // Only an in-memory reference to the displayed photo, never copied to persistent storage.
    private Bitmap compareBitmap;
    private EditText text;private TextView status;private Button freeze,play,compare,specialist;private CameraController camera;
    private HistoryStore history;private ModelFiles models;private SharedPreferences prefs;
    private boolean changingText,destroyed,reading,cameraMode;private String maskedPreview="",lastQwenText="",lastQwenError="";private long lastQwenMs=-1;private float lastQwenPssMb=-1f;private int textSp=24;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);DebugLog.session(this);prefs=getSharedPreferences("settings",MODE_PRIVATE);history=new HistoryStore(this);textSp=prefs.getInt("textSize",24);
        voice.selectVoice(prefs.getBoolean("voiceHD",true));buildUi();camera=new CameraController(this,preview,new CameraController.Listener(){
            public void captured(byte[] jpeg,long ticket){ui(()->{if(cameraMode&&sessions.sourceCurrent(ticket)){cameraMode=false;processImage(()->ImageLoader.fromJpeg(jpeg));}});}
            public void error(String message){ui(()->{if(cameraMode){status.setText(message);freeze.setEnabled(true);}});}
        });
        incoming(getIntent());
        // Warm Piper through the real muted audio route before the first user reading.
        worker.execute(()->{
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
        specialist=button("🔎 PaddleOCR-VL-1.6 / Comparer",TEAL,this::compareSpecialist);
        specialist.setVisibility(View.GONE);content.addView(specialist);setSpecialistAvailable(false);
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
    private void setSpecialistAvailable(boolean available){if(specialist==null)return;specialist.setEnabled(available);specialist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(available?TEAL:DISABLED));specialist.setAlpha(available?1f:0.65f);}
    private long beginOcrProgress(int value){long id=progressGeneration.incrementAndGet();ui(()->{if(ocrProgress!=null&&progressGeneration.get()==id){ocrProgress.setVisibility(View.VISIBLE);ocrProgress.setIndeterminate(value<0);if(value>=0)ocrProgress.setProgress(value);}});return id;}
    private void updateOcrProgress(long id,int value){ui(()->{if(ocrProgress==null||progressGeneration.get()!=id)return;ocrProgress.setVisibility(View.VISIBLE);ocrProgress.setIndeterminate(value<0);if(value>=0)ocrProgress.setProgress(Math.max(0,Math.min(100,value)));});}
    private void finishOcrProgress(long id){ui(()->{if(ocrProgress==null||progressGeneration.get()!=id)return;ocrProgress.setIndeterminate(false);ocrProgress.setProgress(100);ocrProgress.postDelayed(()->{if(progressGeneration.get()==id)ocrProgress.setVisibility(View.GONE);},900);});}
    private long newDocument(){stopReading();camera.stop();cameraMode=false;maskedPreview="";lastQwenText="";lastQwenError="";lastQwenMs=-1;lastQwenPssMb=-1f;compareBitmap=null;ocrChoice.incrementAndGet();progressGeneration.incrementAndGet();if(ocrProgress!=null)ocrProgress.setVisibility(View.GONE);if(compare!=null){setCompareAvailable(false);compare.setVisibility(View.GONE);}if(specialist!=null){setSpecialistAvailable(false);specialist.setVisibility(View.GONE);}return sessions.replaceSource();}
    private void startCamera(){
        newDocument();cameraMode=true;imageArea.setVisibility(View.VISIBLE);preview.setVisibility(View.VISIBLE);frozen.setVisibility(View.GONE);freeze.setVisibility(View.VISIBLE);freeze.setEnabled(true);setText("");status.setText("Vise le texte puis appuie sur Figer l’image.");
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
    }
    private void choosePhoto(){newDocument();Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,PHOTO);}catch(ActivityNotFoundException e){status.setText("Aucun sélecteur de photos disponible.");}}
    private void chooseSpecialistModel(){
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(pick,SPECIALIST_MODEL);}
        catch(ActivityNotFoundException e){status.setText("Aucun gestionnaire de fichiers disponible.");}
    }
    private void importSpecialistModel(Uri uri){
        status.setText("Import local de PaddleOCR-VL-1.6 : vérification SHA-256…");
        modelWorker.execute(()->{
            try{
                specialistOcr.importModel(this,uri,p->ui(()->status.setText("Import du modèle OCR spécialisé : "+p+" %")));
                DebugLog.append(this,"SPECIALIST model installed");
                ui(()->{
                    setSpecialistAvailable(compareBitmap!=null&&!compareBitmap.isRecycled());
                    status.setText("PaddleOCR-VL-1.6 importé et vérifié. Capture une image pour comparer.");
                });
            }catch(Exception|OutOfMemoryError e){
                DebugLog.exception(this,"SPECIALIST import",e);
                ui(()->status.setText("Import OCR spécialisé refusé ou incomplet : fichier, empreinte ou espace de stockage non conforme."));
            }
        });
    }
    private void manualText(){newDocument();imageArea.setVisibility(View.GONE);freeze.setVisibility(View.GONE);status.setText("Colle ou écris ton texte, puis appuie sur Lire.");text.requestFocus();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA_PERMISSION&&cameraMode){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else{freeze.setEnabled(false);status.setText("Caméra refusée. Photo et Texte restent disponibles.");}}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();if(request==PHOTO)processImage(()->ImageLoader.fromUri(this,uri));else if(request==SPECIALIST_MODEL)importSpecialistModel(uri);}}
    interface LoadImage{Bitmap get()throws Exception;}
    private void processImage(LoadImage source){
        long ticket=newDocument(),privacy=sessions.historyTicket(),choice=ocrChoice.get();imageArea.setVisibility(View.VISIBLE);freeze.setVisibility(View.GONE);preview.setVisibility(View.GONE);frozen.setVisibility(View.VISIBLE);frozen.setImageDrawable(null);setText("");status.setText("Préparation de l’image…");
        final long loadProgress=beginOcrProgress(2);
        worker.execute(()->{
            try{
                if(!sessions.documentCurrent(ticket))return;Bitmap image=source.get();if(!sessions.documentCurrent(ticket)){image.recycle();return;}
                DebugLog.append(this,"IMAGE ready "+image.getWidth()+"x"+image.getHeight()+" pssMb="+formatMb(processPssMb()));
                ui(()->{if(sessions.documentCurrent(ticket)){frozen.setImageBitmap(image);compareBitmap=image;setCompareAvailable(true);compare.setVisibility(View.VISIBLE);specialist.setVisibility(View.VISIBLE);setSpecialistAvailable(specialistOcr.isInstalled(this));status.setText("Image prête. Qwen démarre ; les OCR de comparaison sont disponibles.");}});
                finishOcrProgress(loadProgress);
                final long qProgress=beginOcrProgress(5);
                qwenWorker.execute(()->{
                    long q0=SystemClock.elapsedRealtime();
                    try{
                        specialistOcr.unload();
                        String found=qwen.recognize(this,image,
                            message->ui(()->{if(sessions.documentCurrent(ticket)&&progressGeneration.get()==qProgress)status.setText(message);}),
                            value->updateOcrProgress(qProgress,value));
                        long qMs=SystemClock.elapsedRealtime()-q0;
                        if(!sessions.documentCurrent(ticket))return;
                        lastQwenText=found;lastQwenError="";lastQwenMs=qMs;lastQwenPssMb=processPssMb();
                        DebugLog.append(this,"QWEN done ms="+qMs+" chars="+found.length()+" pssMb="+formatMb(lastQwenPssMb));
                        finishOcrProgress(qProgress);
                        ui(()->{
                            if(!sessions.documentCurrent(ticket))return;
                            if(ocrChoice.get()==choice)setText(found);
                            if(progressGeneration.get()==qProgress)status.setText(found.trim().isEmpty()?"Qwen : aucun texte trouvé ("+qMs+" ms). Essaie PP-OCRv6-Small.":"Qwen : texte reconnu en "+qMs+" ms. PP-OCRv6-Small est disponible pour comparer.");
                        });
                        if(!found.trim().isEmpty()&&prefs.getBoolean("history",true)&&ocrChoice.get()==choice)maskAndSave(found,ticket,privacy,true);
                    }catch(Exception|OutOfMemoryError e){
                        long qMs=SystemClock.elapsedRealtime()-q0;
                        lastQwenText="";lastQwenError=e.getClass().getSimpleName()+(e.getMessage()==null?"":": "+e.getMessage());lastQwenMs=qMs;lastQwenPssMb=processPssMb();
                        DebugLog.exception(this,"QWEN after "+qMs+" ms",e);finishOcrProgress(qProgress);
                        ui(()->{if(sessions.documentCurrent(ticket)&&progressGeneration.get()==qProgress)status.setText("Qwen n’a pas terminé correctement. PP-OCRv6-Small reste disponible.");});
                    }
                });
            }catch(Exception|OutOfMemoryError e){
                DebugLog.exception(this,"IMAGE load",e);finishOcrProgress(loadProgress);
                ui(()->{if(sessions.documentCurrent(ticket))status.setText("Impossible de préparer cette image.");});
            }
        });
    }

    /**
     * Serializes LiteRT engines on qwenWorker. Release Qwen's native resources
     * before initializing the larger specialist model to avoid double RAM.
     * PP-OCR stays on its independent worker as an emergency fallback.
     */
    private void compareSpecialist(){
        final Bitmap image=compareBitmap;
        final long ticket=sessions.documentTicket();
        if(image==null||image.isRecycled()||!specialistOcr.isInstalled(this)||!sessions.documentCurrent(ticket))return;
        setSpecialistAvailable(false);
        final long specProgress=beginOcrProgress(-1);
        status.setText("PaddleOCR-VL-1.6 démarre après la tâche Qwen active…");
        qwenWorker.execute(()->{
            final long start=SystemClock.elapsedRealtime();
            String value="",error=null;
            try{
                qwen.unload();
                value=specialistOcr.recognize(this,image,message->ui(()->{
                    if(sessions.documentCurrent(ticket)&&progressGeneration.get()==specProgress)status.setText(message);
                }));
            }catch(Exception|OutOfMemoryError e){
                error=e.getClass().getSimpleName()+(e.getMessage()==null?"":": "+e.getMessage());
                DebugLog.exception(this,"SPECIALIST OCR",e);
            }
            final long elapsed=SystemClock.elapsedRealtime()-start;
            final float pss=processPssMb();
            DebugLog.append(this,"SPECIALIST result ms="+elapsed+" chars="+value.length()+" pssMb="+formatMb(pss)+(error==null?"":" error="+error));
            finishOcrProgress(specProgress);
            final String textResult=value,errorResult=error;
            ui(()->{
                if(!sessions.documentCurrent(ticket)||compareBitmap!=image)return;
                setSpecialistAvailable(true);
                if(errorResult!=null){
                    status.setText("PaddleOCR-VL a échoué. Qwen et PP-OCR restent accessibles.");
                    return;
                }
                status.setText("OCR spécialisé terminé en "+elapsed+" ms ; mémoire processus ≈ "+formatMb(pss)+".");
                showComparisonText("PaddleOCR-VL-1.6",textResult,ticket);
            });
        });
    }

    /** Run the known-good PP-OCRv6-Small on the exact bitmap already sent to Qwen. */
    private void compareOcr() {
        final Bitmap image=compareBitmap;
        final long ticket=sessions.documentTicket();
        if(image==null||image.isRecycled()||!sessions.documentCurrent(ticket))return;
        setCompareAvailable(false);
        final long pProgress=beginOcrProgress(-1);
        status.setText("PP-OCRv6-Small analyse exactement la même image…");
        DebugLog.append(this,"PADDLE start image="+image.getWidth()+"x"+image.getHeight());
        paddleWorker.execute(()->{
            String paddleText="";
            String paddleError=null;
            long t0=SystemClock.elapsedRealtime();
            try { paddleText=new OcrService().recognize(this,image); }
            catch(Exception|OutOfMemoryError e) { paddleError=e.getClass().getSimpleName()+(e.getMessage()==null?"":": "+e.getMessage());DebugLog.exception(this,"PADDLE",e); }
            long paddleMs=SystemClock.elapsedRealtime()-t0;
            float paddlePss=processPssMb();
            DebugLog.append(this,"PADDLE done ms="+paddleMs+" chars="+paddleText.length()+" pssMb="+formatMb(paddlePss)+(paddleError==null?"":" error="+paddleError));
            finishOcrProgress(pProgress);
            if(!sessions.documentCurrent(ticket)||destroyed)return;
            final String p=paddleText,err=paddleError;final float pPss=paddlePss;
            ui(()->{
                if(!sessions.documentCurrent(ticket)||compareBitmap!=image)return;
                setCompareAvailable(true);
                status.setText(err==null?"Comparaison terminée. Choisis le résultat à examiner.":"PP-OCRv6-Small a échoué ; ouvre le journal debug pour le détail.");
                String qState=lastQwenMs<0?"en cours / non terminé":(lastQwenError.isEmpty()?lastQwenMs+" ms, "+lastQwenText.length()+" caractères, PSS processus ≈ "+formatMb(lastQwenPssMb):"échec après "+lastQwenMs+" ms ("+lastQwenError+")");
                String summary="Même image : "+image.getWidth()+" × "+image.getHeight()+" pixels.\n"
                    +"Qwen3.5-0.8B-VL : "+qState+"\n"
                    +"PP-OCRv6-Small : "+paddleMs+" ms, "+p.length()+" caractères, PSS processus après test ≈ "+formatMb(pPss)+(err==null?"":" ("+err+")")+"\n\n"
                    +"Les deux moteurs ont reçu le même bitmap. Le nombre de caractères ne mesure pas à lui seul l’exactitude. "
                    +"Aucun texte de comparaison n’est envoyé sur Internet.";
                new AlertDialog.Builder(this)
                    .setTitle("Qwen ↔ PP-OCRv6-Small")
                    .setMessage(summary)
                    .setPositiveButton("Voir Qwen",(dialog,which)->showComparisonText("Qwen",lastQwenText,ticket))
                    .setNeutralButton("Voir PP-OCR",(dialog,which)->showComparisonText("PP-OCRv6-Small",p,ticket))
                    .setNegativeButton("Fermer",null).show();
            });
        });
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
        panel.addView(button("Importer PaddleOCR-VL-1.6 (fichier .litertlm)",INK,this::chooseSpecialistModel));
        panel.addView(label("Le fichier doit être exactement PaddleOCR-VL-1.6.litertlm (1,39 Go), contrôlé SHA-256. L'application ne télécharge rien.",16));
        panel.addView(label("Le moteur est amorcé silencieusement sur le canal vocal avant la première vraie phrase.",16));
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
    private void help(){new AlertDialog.Builder(this).setTitle("Lire avec VisionMaxRead").setMessage("1. Caméra : vise, puis Figer l’image.\n2. Photo : choisis une image du téléphone.\n3. Qwen reconnaît automatiquement le texte hors ligne.\n4. Le bouton PP-OCRv6-Small permet une seconde lecture de la même image et une comparaison.\n5. Corrige le résultat puis Lire : Jessica parle avec Piper / Sherpa-ONNX.\n\nLa première utilisation de Qwen copie son modèle local d’environ 1,3 Go ; cela peut prendre du temps et nécessite de l’espace libre. La voix est amorcée silencieusement afin de protéger le début de la première phrase.\n\nAucune permission INTERNET n’est utilisée par l’application. Les images et textes originaux ne sont pas enregistrés dans l’historique ; seul le texte masqué peut y être conservé.\n\nLAB 0.4.5 · Qwen + PP-OCR + PaddleOCR-VL optionnel · Siwis/Jessica Piper hors ligne.").setPositiveButton("Fermer",null).show();}
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
    @Override public void onTrimMemory(int level){
        super.onTrimMemory(level);
        if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW&&!destroyed){
            // Do not close a running LiteRT engine on the main thread. Wait
            // until the active recognition and comparison tasks have completed.
            qwenWorker.execute(()->{
                if(destroyed)return;
                qwen.unload();
                specialistOcr.unload();
                DebugLog.append(this,"OCR native engines released on memory pressure level="+level);
            });
        }
    }
    @Override protected void onDestroy(){destroyed=true;DebugLog.append(this,"SESSION destroy");sessions.replaceSource();sessions.clearHistory();speech.next();voice.release();qwen.release();specialistOcr.release();if(camera!=null)camera.destroy();worker.shutdownNow();qwenWorker.shutdownNow();paddleWorker.shutdownNow();modelWorker.shutdownNow();super.onDestroy();}
}
