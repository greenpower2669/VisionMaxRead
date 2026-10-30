package fr.visionmaxread;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;

/** Accessible offline text-model token stream; no automatic sharing or saving. */
final class TextModelStreamDialog {
    private final Activity activity;
    private final String engine;
    private AlertDialog dialog;
    private TextView metrics,content;
    private ScrollView scrolling;
    private boolean open,finished;
    private String completed="",header="";
    private int current=-1;
    private long sinceSegment;
    private Runnable compareAction;
    TextModelStreamDialog(Activity activity,String engine){
        this.activity=activity;this.engine=engine;
    }
    void show(){
        LinearLayout pane=new LinearLayout(activity);
        pane.setOrientation(LinearLayout.VERTICAL);
        int pad=Math.round(activity.getResources().getDisplayMetrics().density*12);
        pane.setPadding(pad,pad,pad,pad);
        metrics=new TextView(activity);metrics.setTextSize(18);
        metrics.setTextColor(Color.rgb(0,83,77));
        metrics.setText("Modèle "+engine+" en cours de chargement…");
        pane.addView(metrics);
        scrolling=new ScrollView(activity);
        content=new TextView(activity);content.setTextSize(21);
        content.setTextColor(Color.rgb(16,44,60));content.setTextIsSelectable(true);
        content.setText("En attente du premier jeton…");
        scrolling.addView(content);
        pane.addView(scrolling,new LinearLayout.LayoutParams(-1,
            Math.round(activity.getResources().getDisplayMetrics().density*320)));
        dialog=new AlertDialog.Builder(activity).setTitle(engine+" · flux réel hors ligne")
            .setView(pane).setPositiveButton("Copier flux",null)
            .setNeutralButton("Arrêter",null).setNegativeButton("Fermer",null).create();
        dialog.setOnDismissListener(d->{open=false;if(!finished&&LiquidNative.available())LiquidNative.stop();});
        dialog.show();open=true;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->confirmCopy());
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
            if(!finished){metrics.setText("Arrêt demandé…");LiquidNative.stop();}
            else if(compareAction!=null)compareAction.run();
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->dialog.dismiss());
    }
    void segment(int index,int total){
        activity.runOnUiThread(()->{
            if(!open||activity.isFinishing())return;
            if(current>=0)completed=content.getText().toString()+"\n";
            else completed="";
            current=index;sinceSegment=SystemClock.elapsedRealtime();
            header="〔phrase "+index+"/"+total+"〕\n";
            content.setText(completed+header);
            metrics.setText("Phrase "+index+"/"+total+" · génération des jetons…");
        });
    }
    void token(int index,int total,String cumulative,int count){
        activity.runOnUiThread(()->{
            if(!open||activity.isFinishing()||index!=current)return;
            content.setText(completed+header+cumulative);
            long elapsed=Math.max(1,SystemClock.elapsedRealtime()-sinceSegment);
            metrics.setText(engine+" · phrase "+index+"/"+total+" · jetons "+count+
                " · environ "+Math.round(count*1000f/elapsed)+" jetons/s");
            scrolling.post(()->{if(open)scrolling.fullScroll(View.FOCUS_DOWN);});
        });
    }
    void finish(String text,Runnable compare){
        activity.runOnUiThread(()->{
            finished=true;compareAction=compare;
            if(!open||activity.isFinishing())return;
            metrics.setText(text);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText(compare==null?"Terminé":"Voir candidats");
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(compare!=null);
        });
    }
    void cancelAndClose(){
        if(LiquidNative.available())LiquidNative.stop();
        activity.runOnUiThread(()->{if(dialog!=null&&dialog.isShowing())dialog.dismiss();open=false;});
    }
    private void confirmCopy(){
        if(!open)return;
        String raw=content.getText().toString();
        if(raw.isBlank()||raw.startsWith("En attente"))return;
        new AlertDialog.Builder(activity).setTitle("Copier la sortie sensible ?")
            .setMessage("Le flux contient potentiellement des noms et données privées. "+
                "Le presse-papiers Android peut être lu par d'autres applications. "+
                "Vérifie et anonymise avant de partager.")
            .setPositiveButton("Copier",(d,w)->{
                ((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("VisionMaxRead sortie sensible",raw));
                Toast.makeText(activity,"Flux copié après confirmation",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Annuler",null).show();
    }
}
