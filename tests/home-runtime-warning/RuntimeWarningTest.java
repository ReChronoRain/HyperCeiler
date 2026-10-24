package com.sevtinge.hyperceiler.hooker;

import com.sevtinge.hyperceiler.prefs.LayoutPreference;
import android.content.pm.PackageManager;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;

public class RuntimeWarningTest {
    static final String API="prefs_key_framework_check_api_version";
    static final String CODE="prefs_key_framework_check_version_code";
    static final String VERSION="prefs_key_framework_check_version";
    static final String WARNING="prefs_key_home_is_rust_no_support";
    static int checks, failures;
    static void check(boolean condition,String name){checks++;if(!condition){failures++;System.out.println("FAIL: "+name);}}
    static HomeFragment create(String version,int api,long code,boolean runtime,boolean legacy){
        PackageManager.runtime=runtime;DeviceHelper.System.version=legacy?2f:4f;
        HomeFragment f=new HomeFragment();f.entries.put("prefs_key_home_unsupported",new LayoutPreference());
        if(!legacy){f.entries.put(WARNING,new LayoutPreference());f.entries.put("prefs_key_home_is_rust",new LayoutPreference());f.entries.put("prefs_key_home_os4_unadapted_tip",new LayoutPreference());}
        if(version!=null)f.prefs.values.put(VERSION,version);
        if(api>=0)f.prefs.values.put(API,api);
        if(code>=0)f.prefs.values.put(CODE,code);
        f.initPrefs();return f;
    }
    static void scenario(String version,int api,long code,boolean runtime,boolean warning,String label){
        try {HomeFragment f=create(version,api,code,runtime,false);check(f.findPreference(WARNING).visible==warning,label);}
        catch(Throwable e){check(false,label+" exception "+e.getClass().getSimpleName());}
    }
    public static void main(String[] args){
        scenario("2.2.0",102,7854,true,false,"actual device 2.2.0/7854/API102 without -it");
        scenario("2.1.1-it",102,7846,true,false,"minimum numeric baseline with -it");
        scenario("2.1.1",102,7846,true,false,"same baseline without -it");
        scenario("2.2.0-IT",102,7854,true,false,"suffix case irrelevant");
        scenario(null,102,7854,true,false,"missing display version irrelevant");
        scenario("renamed",103,8000,true,false,"newer numeric metadata irrespective branding");
        scenario("1.9.2",101,7000,true,true,"known older framework still warns");
        scenario("2.1.1-it",102,7845,true,true,"below existing build baseline");
        scenario("2.2.0",101,7854,true,true,"below existing API baseline");
        scenario(null,-1,-1,true,false,"service pending");
        scenario("Unknown",0,0,true,false,"zero metadata unknown");
        scenario("2.2.0",102,-1,true,false,"partial metadata unknown");
        scenario("2.2.0",-1,7854,true,false,"partial API unknown");
        scenario("1.9.2",101,7000,false,false,"non-runtime launcher");
        try {HomeFragment f=create("1.9.2",101,7000,false,true);f.onResume();f.onPause();check(true,"legacy screen missing runtime preference");}
        catch(Throwable e){check(false,"legacy screen exception");}
        HomeFragment f=create("1.9.2",101,7000,true,false);
        f.onResume();check(f.findPreference(WARNING).visible,"initial old cached metadata");
        f.prefs.put(CODE,7854L);f.prefs.put(API,102);f.prefs.put(VERSION,"2.2.0");
        check(!f.findPreference(WARNING).visible,"asynchronous service refresh clears warning");
        check(f.prefs.listeners.size()==1,"one visible-page listener");
        f.onPause();check(f.prefs.listeners.isEmpty(),"listener released on pause");
        f.prefs.put(API,101);f.onResume();check(f.findPreference(WARNING).visible,"resume rereads current status");f.onPause();
        System.out.println("runtime warning regression: "+(failures==0?"PASS":"FAIL")+" ("+checks+" checks; "+failures+" failures)");
        if(failures!=0)System.exit(1);
    }
}
