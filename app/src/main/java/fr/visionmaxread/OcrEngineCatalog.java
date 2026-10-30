package fr.visionmaxread;
/** Future on-device OCR options. Never advertise an engine without real image inference. */
final class OcrEngineCatalog {
    static final class Entry {
        final String id,name,source,notes;
        final int parametersMillions;
        final double benchmark;
        Entry(String id,String name,int count,double score,String src,String details){
            this.id=id;this.name=name;this.parametersMillions=count;
            this.benchmark=score;this.source=src;this.notes=details;
        }
    }
    static final Entry[] PLANNED={
        new Entry("SURYA2","Surya OCR 2",670,83.1,"https://github.com/datalab-to/surya",
            "Port multimodal et licence des poids OpenRAIL-M modifiée à valider."),
        new Entry("LIGHTON3","LightOnOCR-3 0.8B",850,81.6,"https://github.com/lightonai/LightOnOCR",
            "Le projecteur vision est nécessaire ; le GGUF textuel seul ne suffit pas."),
        new Entry("NEMOTRON35","Nemotron 3.5 Parse",631,80.0,"https://huggingface.co/nvidia/Nemotron-3.5-Parse",
            "Prétraitement image C-RADIO / NaViT et décodage mBART à porter.")
    };
    static String[] choices(){
        String[] out=new String[PLANNED.length+1];
        out[0]="Indra OCR actif · PP-OCRv6-Small en secours";
        for(int i=0;i<PLANNED.length;i++)out[i+1]=PLANNED[i].name+" · indisponible (portage Android)";
        return out;
    }
    static String details(Entry e){
        return "Score OmniParseBench : "+e.benchmark+" / 100. Score serveur, pas un test Android."+
            "\nParamètres : ~"+e.parametersMillions+" millions."+
            "\nNon installé, non exécutable dans cette version."+
            "\n\n"+e.notes+"\nSource : "+e.source+
            "\n\nUn portage réel de l'inférence image hors ligne et des essais Samsung restent nécessaires.";
    }
    private OcrEngineCatalog(){}
}
