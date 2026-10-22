package android.content.res;
public class Resources {
 public static final Resources SYSTEM=new Resources();
 public static boolean noSlot;public static String slot="data_saver";
 public boolean noDrawable,noDescription;public int lookups;
 public static Resources getSystem(){return SYSTEM;}
 public int getIdentifier(String name,String kind,String pkg){lookups++;if(pkg.equals("android")&&kind.equals("string")&&name.equals("status_bar_data_saver"))return noSlot?0:0x1040b6d;if(pkg.equals("com.android.systemui")&&kind.equals("drawable")&&name.equals("stat_sys_data_saver"))return noDrawable?0:0x7f081a1e;if(pkg.equals("com.android.systemui")&&kind.equals("string")&&name.equals("data_saver"))return noDescription?0:0x7f140491;throw new AssertionError("unexpected resource lookup "+pkg+":"+kind+"/"+name);}
 public String getString(int id){if(id==0x1040b6d)return slot;if(id==0x7f140491)return "流量节省模式";throw new IllegalArgumentException("unknown id");}
}
