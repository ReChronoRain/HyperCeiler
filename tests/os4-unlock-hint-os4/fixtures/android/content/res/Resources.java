package android.content.res;
public class Resources {
 public String text="Swipe up to continue";public boolean missingBottom,missingText,foreignBottom,foreignText,fail;public Runnable duringRead;
 public int getIdentifier(String n,String t,String p){if(fail)throw new IllegalStateException("resources");if(!p.equals("com.android.systemui"))throw new AssertionError();if(n.equals("keyguard_indication_text_bottom")&&t.equals("id"))return missingBottom?0:0x7f0b05f7;if(n.equals("dismissible_keyguard_swipe")&&t.equals("string"))return missingText?0:0x7f1404dd;throw new AssertionError(n);}
 public String getResourceName(int id){return id==0x7f0b05f7?(foreignBottom?"other:id/keyguard_indication_text_bottom":"com.android.systemui:id/keyguard_indication_text_bottom"):(foreignText?"other:string/dismissible_keyguard_swipe":"com.android.systemui:string/dismissible_keyguard_swipe");}
 public String getString(int id){if(id!=0x7f1404dd)throw new AssertionError();if(duringRead!=null)duringRead.run();return text;}
}
