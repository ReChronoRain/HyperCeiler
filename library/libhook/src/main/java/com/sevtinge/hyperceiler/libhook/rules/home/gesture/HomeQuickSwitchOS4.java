/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import java.io.FileInputStream;
import java.util.List;

/** Native gesture actions inside the existing authenticated WMS endpoint. No launcher task-model guesses. */
public final class HomeQuickSwitchOS4 {
    public static final int TRANSACTION = 0x00484351;
    public static final int ACK = 0x48435131;
    private static final Uri PREFS = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs/home_gestures");
    private static long lastSwitch;
    private HomeQuickSwitchOS4() {}

    // Native libbinder adds transport flags (e.g. TF_ACCEPT_FDS). Only ONE_WAY
    // loses the trustworthy synchronous caller/reply contract; mirror the layout endpoint.
    static boolean synchronous(int flags) { return (flags & android.os.IBinder.FLAG_ONEWAY) == 0; }

    private static void diagnostic(String text) {
        if (android.os.SystemProperties.getBoolean("debug.hyperceiler.gesture_probe", false))
            com.sevtinge.hyperceiler.common.log.ModuleLog.i("HyperCeiler.HomeGesture", text);
    }

    public static synchronized void receive(Parcel data, Parcel reply, int flags) {
        boolean result = false;
        int uid = Binder.getCallingUid(), pid = Binder.getCallingPid();
        try {
            data.enforceInterface("android.view.IWindowManager");
            int event = data.readInt(); long time = data.readLong();
            int expected = event == 7 ? data.readInt() : -1;
            Context context = ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID);
            long now = SystemClock.uptimeMillis();
            if (!synchronous(flags) || data.dataAvail() != 0 || event < 1 || event > 12 || time > now
                || now - time > 750 || ((event == 6 || event == 7) && now - lastSwitch < 350)) {
                diagnostic("reject protocol event="+event+" flags="+flags+" remaining="+data.dataAvail()+" age="+(now-time));
                reply.writeInt(ACK); reply.writeInt(0); return;
            }
            if(context==null || (event >= 7 ? (event == 7 && expected < 0) || !isSystemUI(context,uid,pid) : !isLauncher(context,uid,pid))) {
                diagnostic("reject caller uid="+uid+" pid="+pid+" context="+(context!=null));
                reply.writeInt(ACK); reply.writeInt(0); return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                android.app.KeyguardManager keyguard = (android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                diagnostic("dispatch event="+event+" interactive="+power.isInteractive()+" locked="+keyguard.isKeyguardLocked());
                if (power.isInteractive() && !keyguard.isKeyguardLocked()) {
                    if ((event == 6 || event == 7) && enabled(context)) {
                        result = switchPrevious(context, expected);
                        if (result) lastSwitch = now;
                    } else if (event < 6 || event >= 8) result = performGesture(context,event);
                    else diagnostic("reject quick preference");
                }
            } finally { Binder.restoreCallingIdentity(token); }
        } catch (Exception error) {
            com.sevtinge.hyperceiler.common.log.ModuleLog.w("HyperCeiler.HomeGesture", "previous task failed", error);
        }
        reply.writeInt(ACK); reply.writeInt(result ? 1 : 0);
    }
    private static boolean enabled(Context context) {
        try (Cursor c = context.getContentResolver().query(PREFS, null, null, null, null)) {
            if (c == null || !c.moveToFirst() || c.getInt(0) != 1) return false;
            int index = c.getColumnIndex("home_navigation_quick_back");
            return index >= 0 && !c.isNull(index) && c.getInt(index) == 1;
        }
    }
    private static boolean performGesture(Context context,int event) {
        try (Cursor c=context.getContentResolver().query(PREFS,null,null,null,null)) {
            if(c==null||!c.moveToFirst()||c.getColumnCount()!=3+4*com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.ACTIONS.length||c.getInt(0)!=1)return false;
            int master=c.getColumnIndex("home_gesture_enable");
            if(master<0||c.isNull(master)||c.getInt(master)!=1)return false;
            java.util.Map<String,Object> values=new java.util.HashMap<>();
            for(int i=1;i<c.getColumnCount();i++) {
                String key=c.getColumnName(i);
                boolean flag=key.equals("home_gesture_enable")||key.equals("home_navigation_quick_back");
                boolean string=key.endsWith("_app")||key.endsWith("_activity")||key.endsWith("_shortcut_intent");
                Object value=flag?(!c.isNull(i)&&c.getInt(i)==1):string?(c.isNull(i)?"":c.getString(i)):(c.isNull(i)?0:c.getInt(i));
                values.put(key,value);
            }
            int index = actionIndex(event);
            if(index<0||index>=com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.ACTIONS.length)return false;
            if(event==8&&!homeForeground(context))return false;
            String key=com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.ACTIONS[index];
            if(!(values.get(key+"_action") instanceof Integer action)||action<=0||action>100)return false;
            values.forEach(com.sevtinge.hyperceiler.common.utils.PrefsBridge::putHookCache);
            return com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge.handleAction(context,key);
        }
    }
    static int actionIndex(int event) {
        return event>=1&&event<=5?event-1:event>=8&&event<=12?event-3:-1;
    }
    private static boolean homeForeground(Context context) {
        var home=context.getPackageManager().resolveActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_HOME),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        var tasks=context.getSystemService(ActivityManager.class).getRunningTasks(1);
        if(home==null||home.activityInfo==null||tasks.isEmpty())return false;
        var top=tasks.get(0).topActivity;
        return top!=null&&top.getPackageName().equals(home.activityInfo.packageName)
            &&top.getClassName().equals(home.activityInfo.targetActivity!=null?home.activityInfo.targetActivity:home.activityInfo.name);
    }
    public static boolean isLauncher(Context context, int uid, int pid) throws Exception {
        if (uid < 10000 || pid <= 0) return false;
        return packageProcess(context,uid,pid,"com.miui.home");
    }
    private static boolean isSystemUI(Context context,int uid,int pid) throws Exception {
        return uid == 1000 && pid > 0 && packageProcess(context,uid,pid,"com.android.systemui");
    }
    private static boolean packageProcess(Context context,int uid,int pid,String pkg) throws Exception {
        String[] packages = context.getPackageManager().getPackagesForUid(uid);
        if (packages == null || !java.util.Arrays.asList(packages).contains(pkg)) return false;
        byte[] command = new byte[64];
        try (FileInputStream in = new FileInputStream("/proc/" + pid + "/cmdline")) {
            int count = in.read(command); byte[] expected = pkg.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            if (count <= expected.length || command[expected.length] != 0) return false;
            for (int i = 0; i < expected.length; i++) if (command[i] != expected[i]) return false;
            return true;
        }
    }
    /** Input is MRU order from ActivityTaskManager, never a launcher-maintained stale task stack. */
    static int selectPrevious(int current, int user, int[] ids, int[] users, int[] types, boolean[] available) {
        if (ids.length != users.length || ids.length != types.length || ids.length != available.length) return -1;
        for (int i = 0; i < ids.length; i++)
            if (ids[i] >= 0 && ids[i] != current && users[i] == user && types[i] == 1 && available[i]) return ids[i];
        return -1;
    }
    private static boolean switchPrevious(Context context, int expected) throws Exception {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> running = am.getRunningTasks(1);
        if (running.isEmpty()) {com.sevtinge.hyperceiler.common.log.ModuleLog.i("HyperCeiler.HomeGesture", "no current task");return false;}
        ActivityManager.RunningTaskInfo current = running.get(0);
        if (expected >= 0 && current.taskId != expected) return false;
        int user = (Integer) current.getClass().getField("userId").get(current);
        // Framework interface contracts; the implementation and returned task objects are resolved live.
        Class<?> atm = Class.forName("android.app.ActivityTaskManager");
        Object service = atm.getDeclaredMethod("getService").invoke(null);
        Class<?> contract = Class.forName("android.app.IActivityTaskManager");
        java.lang.reflect.Method recent = contract.getMethod("getRecentTasks", int.class, int.class, int.class);
        int flags = ActivityManager.class.getField("RECENT_IGNORE_UNAVAILABLE").getInt(null);
        Object slice = recent.invoke(service, 32, flags, user);
        List<?> tasks = (List<?>) slice.getClass().getMethod("getList").invoke(slice);
        int n = tasks.size(); int[] ids = new int[n], users = new int[n], types = new int[n]; boolean[] available = new boolean[n];
        for (int i = 0; i < n; i++) {
            Object task = tasks.get(i); Class<?> c = task.getClass();
            ids[i] = (Integer) c.getField("taskId").get(task);
            users[i] = (Integer) c.getField("userId").get(task);
            // Unavailable tasks are filtered by framework RECENT_IGNORE_UNAVAILABLE, not a nonexistent TaskInfo field.
            available[i] = true;
            Object config = c.getField("configuration").get(task);
            Object window = config.getClass().getField("windowConfiguration").get(config);
            types[i] = (Integer) window.getClass().getMethod("getActivityType").invoke(window);
        }
        int target = selectPrevious(current.taskId, user, ids, users, types, available);
        if (target < 0) {com.sevtinge.hyperceiler.common.log.ModuleLog.i("HyperCeiler.HomeGesture", "no previous task current="+current.taskId+" recent="+n+" types="+java.util.Arrays.toString(types));return false;}
        Object result = contract.getMethod("startActivityFromRecents", int.class, Bundle.class)
            .invoke(service, target, null);
        boolean started = result instanceof Integer code && code >= 0 && code < 100;
        com.sevtinge.hyperceiler.common.log.ModuleLog.i("HyperCeiler.HomeGesture", "previous task current=" + current.taskId + " target=" + target + " started=" + started);
        return started;
    }
}
