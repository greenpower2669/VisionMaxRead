package fr.visionmaxread;

/** VMR-017: conservative initial output allowance; model-tokenizer calibration remains pending. */
final class AdaptiveTokenBudget {
    static final int DEFAULT_SAFETY_CAP=512;
    private AdaptiveTokenBudget(){}
    static int forOcrCorrection(String source,int safetyCap){
        if(source==null||source.isEmpty())throw new IllegalArgumentException("Empty OCR phrase");
        if(safetyCap<128||safetyCap>512)throw new IllegalArgumentException("Safety cap 128..512");
        // This is a character-based *estimate*, not a tokenizer measurement.
        int estimatedTokens=(source.codePointCount(0,source.length())+2)/3;
        int desired=Math.max(96,(int)Math.ceil(estimatedTokens*1.9)+48);
        int rounded=((desired+15)/16)*16;
        return Math.min(safetyCap,Math.min(512,rounded));
    }
    static boolean outputMayBeCapped(int emitted,int budget){return emitted>=budget;}
    static String endReason(int emitted,int budget){
        // hobby-rs does not expose an EOS vs length enum: never fabricate EOS.
        return outputMayBeCapped(emitted,budget)
            ?"TOKEN_BUDGET_REACHED_OR_EOS_UNKNOWN"
            :"UPSTREAM_END_REASON_UNKNOWN";
    }
}
