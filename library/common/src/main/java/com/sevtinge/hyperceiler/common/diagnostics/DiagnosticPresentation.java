/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

/** Comma-only page copy; log dialogs, stored evidence and exported identifiers keep their punctuation. */
public final class DiagnosticPresentation {
    private DiagnosticPresentation() {}
    public static String prose(String text){
        if(text==null)return "";
        StringBuilder out=new StringBuilder();
        for(int at=0;at<text.length();){
            int point=text.codePointAt(at);at+=Character.charCount(point);
            int kind=Character.getType(point);
            if((kind>=Character.DASH_PUNCTUATION&&kind<=Character.OTHER_PUNCTUATION)
                ||kind==Character.INITIAL_QUOTE_PUNCTUATION||kind==Character.FINAL_QUOTE_PUNCTUATION
                ||(kind>=Character.MATH_SYMBOL&&kind<=Character.OTHER_SYMBOL)){
                int last=out.length()-1;while(last>=0&&out.charAt(last)==' ')last--;
                if(last>=0&&out.charAt(last)!='，'&&out.charAt(last)!='\n')out.append('，');
            }else out.appendCodePoint(point);
        }
        return out.toString().replaceAll("，[ \\t]*\\n","\n").replaceAll("[，\\s]+$","");
    }
}
