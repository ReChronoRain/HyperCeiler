package android.text;
import java.util.*;
/** Explicit host span model, not Android framework execution. */
public class SpannableStringBuilder implements CharSequence {
 public record Span(Object object,int start,int end,int flags){}public final List<Span>spans=new ArrayList<>();private final StringBuilder b;
 public SpannableStringBuilder(CharSequence s){b=new StringBuilder(s);if(s instanceof SpannableStringBuilder x)spans.addAll(x.spans);}
 public int length(){return b.length();}public char charAt(int i){return b.charAt(i);}public CharSequence subSequence(int s,int e){return b.substring(s,e);}public String toString(){return b.toString();}
 public SpannableStringBuilder append(CharSequence s){b.append(s);return this;}
 public void setSpan(Object o,int s,int e,int f){spans.add(new Span(o,s,e,f));}
 public SpannableStringBuilder replace(int start,int end,CharSequence s){int delta=s.length()-(end-start);b.replace(start,end,s.toString());for(int i=0;i<spans.size();i++){Span x=spans.get(i);if(x.start>=end)spans.set(i,new Span(x.object,x.start+delta,x.end+delta,x.flags));else if(x.end>start)spans.set(i,new Span(x.object,x.start,Math.max(start,Math.min(start+s.length(),x.end+delta)),x.flags));}return this;}
}
