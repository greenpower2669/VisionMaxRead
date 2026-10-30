package fr.visionmaxread;

/** Real HobbyLM input; previous OCR is tentative and precedes the current phrase. */
final class MoePromptBuilder {
    private MoePromptBuilder(){}
    static String build(String line,String language,String previous){
        return build(line,language,previous,"");
    }
    static String build(String line,String language,String previous,String opening){
        if(line==null||line.isBlank())throw new IllegalArgumentException("Empty OCR phrase");
        if(!"fr".equals(language)&&!"en".equals(language))
            throw new IllegalArgumentException("Language");
        boolean en="en".equals(language);
        StringBuilder prompt=new StringBuilder("<|user|>\n");
        if(en)prompt.append("Fix obvious OCR spelling errors in this English line. ")
            .append("Keep numbers, names and dates unchanged. Output ONLY the corrected line.\n");
        else prompt.append("Corrige les fautes OCR évidentes dans cette seule ligne française. ")
            .append("Conserve exactement les nombres, noms propres et dates. ")
            .append("Réponds uniquement par la ligne.\n");
        String preceding=clean(previous,150),anchor=clean(opening,120);
        if(!anchor.isEmpty()&&!anchor.equals(preceding))
            prompt.append(en?"Opening OCR excerpt (UNVERIFIED, context only): ":
                "Début du document (OCR INCERTAIN, contexte seulement) : ")
                .append(anchor).append('\n');
        if(!preceding.isEmpty())
            prompt.append(en?"Previous OCR phrase (UNVERIFIED, context only): ":
                "Phrase OCR précédente (INCERTAINE, contexte seulement) : ")
                .append(preceding).append('\n');
        prompt.append(en?"CURRENT OCR TO CORRECT:\n":"PHRASE OCR ACTUELLE À CORRIGER :\n");
        prompt.append(clean(line,3500)).append("\n<|assistant|>\n");
        return prompt.toString();
    }
    static String clean(String source,int max){
        if(source==null||source.isBlank())return "";
        String value=source.replaceAll("[\\r\\n\\t]+"," ").trim();
        if(value.length()>max){
            int end=max;
            if(Character.isHighSurrogate(value.charAt(end-1)))end--;
            value=value.substring(0,end);
        }
        // Avoid mistaking OCR-printed role markers for actual chat boundaries.
        return value.replace("<|","< |").replace("|>","| >");
    }
}
