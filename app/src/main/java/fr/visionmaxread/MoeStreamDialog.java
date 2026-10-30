package fr.visionmaxread;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;

/**
 * Displays genuine Hobby-rs token deltas. There is no separate reasoning
 * channel: only the model's visible emitted continuation is shown here.
 * No text is persisted to system logs or the clipboard automatically.
 */
final class MoeStreamDialog {
    private final Activity activity;
    private final long started=SystemClock.elapsedRealtime();
    private AlertDialog dialog;
    private TextView tokensLabel,streamText;
    private ScrollView scroller;
    private boolean open,finished;
    private Runnable compareAction;
    MoeStreamDialog(Activity activity){this.activity=activity;}

    void show() {
        LinearLayout panel=new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int margin=(int)(12*activity.getResources().getDisplayMetrics().density);
        panel.setPadding(margin,margin,margin,margin);
        tokensLabel=new TextView(activity);
        tokensLabel.setTextSize(18);
        tokensLabel.setTextColor(Color.rgb(0,82,77));
        tokensLabel.setText("Chargement du modèle… génération pas encore démarrée.");
        panel.addView(tokensLabel);
        scroller=new ScrollView(activity);
        streamText=new TextView(activity);
        streamText.setTextSize(22);
        streamText.setTextColor(Color.rgb(15,40,56));
        streamText.setTextIsSelectable(true);
        streamText.setText("En attente du premier jeton…");
        scroller.addView(streamText);
        panel.addView(scroller,new LinearLayout.LayoutParams(-1,
            (int)(320*activity.getResources().getDisplayMetrics().density)));
        dialog=new AlertDialog.Builder(activity)
            .setTitle("HobbyLM · génération en direct")
            .setView(panel)
            .setPositiveButton("Copier flux",null)
            .setNeutralButton("Arrêter",null)
            .setNegativeButton("Fermer",null)
            .create();
        dialog.setOnDismissListener(d->{
            open=false;
            if(!finished&&HobbyMoeNative.available())HobbyMoeNative.stop();
        });
        dialog.show();open=true;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->confirmCopy());
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
            if(!finished){
                tokensLabel.setText("Arrêt demandé…");
                if(HobbyMoeNative.available())HobbyMoeNative.stop();
            }else if(compareAction!=null){
                compareAction.run();
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->dialog.dismiss());
    }
    /** Called on the inference worker; marshals incremental updates to the UI. */
    void token(String delta,int count) {
        activity.runOnUiThread(()->{
            if(!open||activity.isFinishing())return;
            if(count==1)streamText.setText("");
            if(!delta.isEmpty())streamText.append(delta);
            long seconds=(SystemClock.elapsedRealtime()-started)/1000;
            tokensLabel.setText("Jetons émis : "+count+" / 110 · "+seconds+" s. "+
                "Flux réel, pas une chaîne de raisonnement cachée.");
            scroller.post(()->{if(open)scroller.fullScroll(View.FOCUS_DOWN);});
        });
    }
    void finish(String message,Runnable validComparison) {
        activity.runOnUiThread(()->{
            finished=true;
            if(!open||activity.isFinishing())return;
            tokensLabel.setText(message);
            compareAction=validComparison;
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setText(validComparison==null?"Terminé":"Comparer");
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setEnabled(validComparison!=null);
        });
    }
    private void confirmCopy(){
        if(!open)return;
        String text=streamText.getText().toString();
        if(text.isBlank()||text.startsWith("En attente"))return;
        new AlertDialog.Builder(activity)
            .setTitle("Copier le flux MoE ?")
            .setMessage("Le flux peut contenir des informations privées. "+
                "Le presse-papiers peut être lu par d'autres applications ; anonymise avant partage.")
            .setPositiveButton("Copier",(d,w)->{
                ((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("VisionMaxRead MoE flux sensible",text));
                Toast.makeText(activity,"Flux MoE copié",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Annuler",null).show();
    }
}
