import android.service.notification.StatusBarNotification;import android.graphics.drawable.Drawable;import android.text.SpannableStringBuilder;import java.util.*;
/** Original-business fixture derived from the saved complete OTA methods; Android/view work modeled. */
public class Targets {
 public static class Item {public CharSequence text;public String barrageMsg;public Drawable barrageDrawable;public Object contentIntent;public int style,user,color,padding,alpha;public boolean live;public long time;}
 public static class Manager {public final List<Item>published=Collections.synchronizedList(new ArrayList<>());public boolean view=true;public int calls,scheduled;public Throwable error;
  public synchronized void addDanmu(Item i)throws Throwable{calls++;if(error!=null)throw error;if(!view)return;i.time=1200L*(++scheduled);published.add(i);}}
 public static class Window {
  public Manager mBarrageShowManager=new Manager();public int currentOrientation=2;public boolean hasAddView=true,view=true,factory=true;public int style;public int calls,cutCalls,spanCalls;public Throwable error;public Runnable nested,beforePublish;public String unexpectedText;public boolean extraSpan;public Object iconSpan=new Object();public SpannableStringBuilder originalSpan;public Item last;public Manager foreignManager;
  public void addBarrageNotification(StatusBarNotification n,boolean live)throws Throwable{
   calls++;if(!hasAddView||!view||!factory)return;if(error!=null)throw error;
   String title=String.valueOf(n.getNotification().extras.getCharSequence("android.title"));String text=String.valueOf(n.getNotification().extras.getCharSequence("android.text"));if(title.isEmpty()||text.isEmpty())return;
   if(n.getPackageName().equals("com.tencent.mm"))text=(String)TestMain.call(this,"cutWeChatMsg",new Class[]{String.class},text);
   if(n.getPackageName().equals("com.whatsapp")&&(n.getTag()==null||n.getTag().isEmpty()))return;
   Item i=new Item();String normalized=(title+": "+text).replaceAll("(\r\n|\r|\n|\n\r)","");String clipped=clip(normalized,currentOrientation==2?31:11);if(unexpectedText!=null)clipped=unexpectedText;
   i.barrageDrawable=new Drawable();i.text=(CharSequence)TestMain.call(this,"createSpannable",new Class[]{Drawable.class,String.class},i.barrageDrawable,clipped);i.barrageMsg=clipped;i.contentIntent=n.getNotification().contentIntent;i.user=n.getUserId();finish(i,live);
  }
  public void sendBarrage(String text,boolean live)throws Throwable{calls++;if(!hasAddView||!view||!factory)return;if(error!=null)throw error;Item i=new Item();i.text=clip(text.replaceAll("(\r\n|\r|\n|\n\r)",""),31);if(unexpectedText!=null)i.text=unexpectedText;finish(i,live);}
  private void finish(Item i,boolean live)throws Throwable {i.style=style;i.color=0xff00+style;i.padding=15+style;i.alpha=200;i.live=live;last=i;if(nested!=null){Runnable r=nested;nested=null;r.run();}if(beforePublish!=null)beforePublish.run();TestMain.call(foreignManager==null?mBarrageShowManager:foreignManager,"addDanmu",new Class[]{Item.class},i);}
  private String cutWeChatMsg(String text){cutCalls++;return text.startsWith("[group]:")?text.substring(8):text;}
  private SpannableStringBuilder createSpannable(Drawable d,String text){spanCalls++;SpannableStringBuilder b=new SpannableStringBuilder("icon");b.setSpan(iconSpan,0,4,17);if(text!=null)b.append(text);if(extraSpan)b.setSpan(TestMain.EXTRA_SPAN,0,2,33);originalSpan=b;return b;}
  public String redraw(Item i){return "icon"+i.barrageMsg;}
 }
 public static String clip(String s,int n){return s.length()>n?s.substring(0,n)+"...":s;}
 public static class WrongWindow{public static void sendBarrage(String x,boolean b){}}
}
