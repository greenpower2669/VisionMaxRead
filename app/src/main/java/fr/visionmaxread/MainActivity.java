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
import fr.visionmaxread.core.SessionGate;
import fr.visionmaxread.core.ReaderSessions;

public final class MainActivity extends Activity {
    private static final int CAMERA_PERMISSION=10,PHOTO=20;
    private static final int INK=Color.rgb(16,44,60),PAPER=Color.rgb(255,249,238),TEAL=Color.rgb(0,105,92),RED=Color.rgb(179,38,32);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ReaderSessions sessions=new ReaderSessions();
    private final SessionGate speech=new SessionGate();
    private final SupertonicEngine voice=new SupertonicEngine();
    private final OfflineAndroidTts offlineVoice=new OfflineAndroidTts();
    private TextureView preview;private ImageView frozen;private FrameLayout imageArea;
    // Only an in-memory reference to the displayed photo, never copied to persistent storage.
    private Bitmap compareBitmap;
    private EditText text;private TextView status;private Button freeze,play,compare;private CameraController camera;
    private HistoryStore history;private ModelFiles models;private SharedPreferences prefs;
    private boolean changingText,destroyed,reading,cameraMode;private String maskedPreview="";private int textSp=24;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);prefs=getSharedPreferences("settings",MODE_PRIVATE);history=new HistoryStore(this);textSp=prefs.getInt("textSize",24);
        buildUi();camera=new CameraController(this,preview,new CameraController.Listener(){
            public void captured(byte[] jpeg,long ticket){ui(()->{if(cameraMode&&sessions.sourceCurrent(ticket)){cameraMode=false;processImage(()->ImageLoader.fromJpeg(jpeg));}});}
            public void error(String message){ui(()->{if(cameraMode){status.setText(message);freeze.setEnabled(true);}});}
        });
        incoming(getIntent());
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
        compare=button("Comparer les OCR (test)",TEAL,this::compareOcr);compare.setVisibility(View.GONE);compare.setEnabled(false);content.addView(compare);
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
    private long newDocument(){stopReading();camera.stop();cameraMode=false;maskedPreview="";compareBitmap=null;if(compare!=null){compare.setEnabled(false);compare.setVisibility(View.GONE);}return sessions.replaceSource();}
    private void startCamera(){
        newDocument();cameraMode=true;imageArea.setVisibility(View.VISIBLE);preview.setVisibility(View.VISIBLE);frozen.setVisibility(View.GONE);freeze.setVisibility(View.VISIBLE);freeze.setEnabled(true);setText("");status.setText("Vise le texte puis appuie sur Figer l’image.");
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
    }
    private void choosePhoto(){newDocument();Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,PHOTO);}catch(ActivityNotFoundException e){status.setText("Aucun sélecteur de photos disponible.");}}
    private void manualText(){newDocument();imageArea.setVisibility(View.GONE);freeze.setVisibility(View.GONE);status.setText("Colle ou écris ton texte, puis appuie sur Lire.");text.requestFocus();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA_PERMISSION&&cameraMode){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)camera.start(sessions.sourceTicket());else{freeze.setEnabled(false);status.setText("Caméra refusée. Photo et Texte restent disponibles.");}}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PHOTO&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();processImage(()->ImageLoader.fromUri(this,uri));}}
    interface LoadImage{Bitmap get()throws Exception;}
    private void processImage(LoadImage source){
        long ticket=newDocument(),privacy=sessions.historyTicket();imageArea.setVisibility(View.VISIBLE);freeze.setVisibility(View.GONE);preview.setVisibility(View.GONE);frozen.setVisibility(View.VISIBLE);frozen.setImageDrawable(null);setText("");status.setText("Préparation de l’image…");
        worker.execute(()->{
            try{
                if(!sessions.documentCurrent(ticket))return;Bitmap image=source.get();if(!sessions.documentCurrent(ticket)){image.recycle();return;}
                ui(()->{if(sessions.documentCurrent(ticket)){frozen.setImageBitmap(image);compareBitmap=image;compare.setVisibility(View.VISIBLE);compare.setEnabled(true);status.setText("Reconnaissance du texte sur ce téléphone…");}});
                String found=new OcrService().recognize(this,image);if(!sessions.documentCurrent(ticket))return;
                ui(()->{if(sessions.documentCurrent(ticket)){setText(found);status.setText(found.trim().isEmpty()?"Aucun texte trouvé. Essaie une image plus nette.":"Texte reconnu. Corrige-le si nécessaire, puis appuie sur Lire.");}});
                if(!found.trim().isEmpty()&&prefs.getBoolean("history",true))maskAndSave(found,ticket,privacy,true);
            }catch(Exception|OutOfMemoryError e){ui(()->{if(sessions.documentCurrent(ticket))status.setText("Impossible de reconnaître cette image. Essaie une photo plus petite ou plus nette.");});}
        });
    }

    /** Compare both real models on exactly the same bitmap and report inference time. */
    private void compareOcr() {
        final Bitmap image=compareBitmap;
        final long ticket=sessions.documentTicket();
        if(image==null||image.isRecycled()||!sessions.documentCurrent(ticket))return;
        compare.setEnabled(false);
        status.setText("Comparaison locale des deux moteurs…");
        worker.execute(()->{
            String paddleText="",mlText="";
            String paddleError=null,mlError=null;
            long t0=SystemClock.elapsedRealtime();
            try { paddleText=new OcrService().recognize(this,image); }
            catch(Exception|OutOfMemoryError e) { paddleError="Échec du moteur PaddleOCR"; }
            long paddleMs=SystemClock.elapsedRealtime()-t0;
            if(!sessions.documentCurrent(ticket)||destroyed)return;
            t0=SystemClock.elapsedRealtime();
            try (MlKitOcrService mlKit=new MlKitOcrService()) {
                mlText=mlKit.recognize(image);
            }catch(Exception|OutOfMemoryError e) { mlError="Échec du moteur ML Kit"; }
            long mlMs=SystemClock.elapsedRealtime()-t0;
            if(!sessions.documentCurrent(ticket)||destroyed)return;
            final String a=paddleText,b=mlText,errA=paddleError,errB=mlError;
            final long msA=paddleMs,msB=mlMs;
            ui(()->{
                if(!sessions.documentCurrent(ticket)||compareBitmap!=image)return;
                compare.setEnabled(true);
                status.setText("Comparaison terminée. Choisis le texte à examiner.");
                String summary="Même image : "+image.getWidth()+" × "+image.getHeight()+" pixels.\n"
                    +"PaddleOCR : "+msA+" ms, "+a.length()+" caractères"+(errA==null?"":" ("+errA+")")+"\n"
                    +"ML Kit : "+msB+" ms, "+b.length()+" caractères"+(errB==null?"":" ("+errB+")")+"\n\n"
                    +"Durées = initialisation + inférence, hors attente dans la file. "
                    +"Le nombre de caractères ne mesure pas l’exactitude. "
                    +"Aucun texte de comparaison n’est enregistré ni envoyé.";
                new AlertDialog.Builder(this)
                    .setTitle("Test OCR hors ligne")
                    .setMessage(summary)
                    .setPositiveButton("Voir Paddle", (dialog,which)->showComparisonText("PaddleOCR",a,ticket))
                    .setNeutralButton("Voir ML Kit", (dialog,which)->showComparisonText("ML Kit",b,ticket))
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
                sessions.editText();maskedPreview="";stopReading();setText(value);
                status.setText("Texte "+name+" affiché, non ajouté à l’historique par ce test.");
            })
            .setNegativeButton("Fermer",null).show();
    }

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
        long ticket=speech.next();reading=true;play.setText("■ Arrêter la lecture");
        boolean androidTts="android-offline".equals(prefs.getString("ttsEngine","supertonic"));
        status.setText(androidTts?"Préparation de la voix Android hors ligne…":"Préparation de Supertonic 3 sur le téléphone…");
        String selected=prefs.getString("voice","F1");float speed=prefs.getFloat("speed",1.05f);
        worker.execute(()->{
            try{
                java.util.function.BooleanSupplier cancelled=()->!speech.current(ticket)||destroyed;
                java.util.function.Consumer<String> report=message->ui(()->{if(speech.current(ticket))status.setText(message);});
                if(androidTts)offlineVoice.speak(this,value,speed,cancelled,report);
                else voice.speak(this,value,selected,speed,models(),cancelled,report);
                ui(()->{if(speech.current(ticket)){reading=false;play.setText("▶ Lire le texte");status.setText("Lecture terminée.");}});
            }catch(CancellationException ignored){ui(()->{if(speech.current(ticket)){reading=false;play.setText("▶ Lire le texte");status.setText("Lecture interrompue.");}});}
            catch(Exception|OutOfMemoryError e){ui(()->{if(speech.current(ticket)){reading=false;play.setText("▶ Lire le texte");status.setText(androidTts?"Voix Android hors ligne indisponible. Installe une voix française hors ligne ou sélectionne Supertonic.":"Supertonic indisponible. Vérifie l’espace libre et réessaie.");}});}
        });
    }
    private void stopReading(){speech.next();voice.stop();offlineVoice.stop();if(reading){reading=false;play.setText("▶ Lire le texte");status.setText("Lecture arrêtée.");}}
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
        panel.addView(label("Voix Supertonic (si sélectionnée)",18));RadioGroup voices=new RadioGroup(this);RadioButton female=new RadioButton(this);female.setId(View.generateViewId());female.setText("F1 — voix féminine");RadioButton male=new RadioButton(this);male.setId(View.generateViewId());male.setText("M1 — voix masculine");voices.addView(female);voices.addView(male);voices.check(prefs.getString("voice","F1").equals("M1")?male.getId():female.getId());panel.addView(voices);
        panel.addView(label("Moteur de lecture (expérimental)",18));
        RadioGroup engines=new RadioGroup(this);
        RadioButton supertonic=new RadioButton(this);supertonic.setId(View.generateViewId());supertonic.setText("Supertonic 3 — voix actuelle");
        RadioButton android=new RadioButton(this);android.setId(View.generateViewId());android.setText("Android — voix française installée hors ligne (test)");
        engines.addView(supertonic);engines.addView(android);
        engines.check("android-offline".equals(prefs.getString("ttsEngine","supertonic"))?android.getId():supertonic.getId());panel.addView(engines);
        panel.addView(label("Android nécessite une voix française hors ligne déjà installée. Aucun remplacement automatique par une voix réseau.",16));
        TextView speedLabel=label("Vitesse de lecture",18);panel.addView(speedLabel);SeekBar speed=new SeekBar(this);speed.setMax(100);speed.setProgress(Math.round((prefs.getFloat("speed",1.05f)-0.75f)*100));panel.addView(speed);speed.setContentDescription("Vitesse de lecture de 0,75 à 1,75");
        panel.addView(button("Effacer l’historique",RED,this::confirmClear));
        ScrollView settingsScroll=new ScrollView(this);settingsScroll.addView(panel);
        new AlertDialog.Builder(this).setTitle("Réglages").setView(settingsScroll).setPositiveButton("Enregistrer",(d,w)->{stopReading();prefs.edit().putBoolean("history",keep.isChecked()).putString("voice",voices.getCheckedRadioButtonId()==male.getId()?"M1":"F1").putString("ttsEngine",engines.getCheckedRadioButtonId()==android.getId()?"android-offline":"supertonic").putFloat("speed",0.75f+speed.getProgress()/100f).apply();if(!keep.isChecked())sessions.clearHistory();}).setNegativeButton("Annuler",null).show();
    }
    private void help(){new AlertDialog.Builder(this).setTitle("Lire avec VisionMaxRead").setMessage("1. Caméra : vise, puis Figer l’image.\n2. Photo : choisis une image du téléphone.\n3. Corrige le texte affiché, puis Lire.\n\nL’aperçu léger sert uniquement à viser. La photo est prise à meilleure résolution. Le manuscrit, les colonnes et le texte peu net peuvent être mal reconnus.\n\nTout fonctionne hors ligne. La première lecture Supertonic peut demander du temps pour charger les modèles. Dans Réglages, une voix Android française hors ligne peut être sélectionnée pour comparer la réactivité. Prévois environ 2 Go libres pour l’installation et les modèles locaux.\n\nLes images et textes originaux ne sont pas enregistrés par l’application. Seul l’historique masqué est conservé ; le masquage automatique peut être incomplet.\n\nVersion 0.1.0 · PaddleOCR PP-OCRv6 Small · Supertonic 3 · NER multilingual BERT. Licences disponibles dans le dépôt et incluses dans l’APK.").setPositiveButton("Fermer",null).show();}
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
    @Override protected void onDestroy(){destroyed=true;sessions.replaceSource();sessions.clearHistory();speech.next();voice.stop();offlineVoice.stop();if(camera!=null)camera.destroy();worker.shutdownNow();super.onDestroy();}
}
