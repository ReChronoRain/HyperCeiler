/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.safemode.CrashActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationUtils {

    private static final String CHANNEL_ID_APP_CRASH = "App_Crash";
    private static final String GROUP_KEY_WORK_EMAIL = "App_Crash";


    public static void showAppCrashNotification(Context context, String packageName, Intent intent) {
        notifyNotification(context, 1, getAppCrashNotification(context, packageName, intent));
    }

    public static Notification getAppCrashNotification(Context context, String packageName, Intent intent) {
        createAppCrashChannel(context);
        String title = context.getResources().getString(com.sevtinge.hyperceiler.ui.R.string.notification_title_message_emergency_crash);
        String content = context.getResources().getString(com.sevtinge.hyperceiler.ui.R.string.notification_content_message);

        intent.putExtra("notification_click", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Notification notification = buildNotification(context, CHANNEL_ID_APP_CRASH, String.format(title, packageName), content, getActivity(context, CHANNEL_ID_APP_CRASH.hashCode(), intent), false);
        setEnableFloat(notification, true);
        return notification;
    }

    private static PendingIntent getActivity(Context context, int requestCode, Intent intent) {
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static void notifyNotification(Context context, int id, Notification notification) {
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        manager.notify(id, notification);
    }

    public static void notificationActions(Context context, String title, String content) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, CrashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_APP_CRASH);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(context, com.sevtinge.hyperceiler.ui.R.color.black));
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Much longer text that cannot fit one line..."));

        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);

        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.addAction(R.mipmap.ic_launcher, context.getString(com.sevtinge.hyperceiler.ui.R.string.notification_view), pendingIntent);

        builder.setGroup(GROUP_KEY_WORK_EMAIL);

        createMiuiFoccusAction(context, builder);

        notificationManager.notify(CHANNEL_ID_APP_CRASH.hashCode(), builder.build());
    }


    public static Notification buildNotification(@NonNull Context context,
                                                 @NonNull String channelId, String title,
                                                 String content, PendingIntent intent, boolean ongoing) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setContentIntent(intent);
        // 设置为常驻通知
        builder.setOngoing(ongoing);
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setWhen(System.currentTimeMillis());

        builder.setShowWhen(true);

        return builder.build();
    }

    private static void createMiuiFoccusAction(Context context, NotificationCompat.Builder builder) {
        Bundle extras = new Bundle();
        extras.putBoolean("miui.showAction", true);
        extras.putBoolean("miui.expandableOnKeyguard", true);

        JSONObject jSONObject = new JSONObject();
        JSONObject jSONObject2 = new JSONObject();
        JSONObject jSONObject3 = new JSONObject();
        JSONObject jSONObject4 = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        try {
            jSONObject.put("param_v2", jSONObject2);

            jSONObject2.put("protocol", 1)
                    .put("updatable", true)
                    .put("enableFloat", true)
                    .put("baseInfo", jSONObject3)
                    .put("highlightInfo", jSONObject3)
                    .put("actions", jSONArray);

            jSONObject3.put("type", 1)
                    .put("title", "00:00")
                    .put("subContent", "subContent")
                    .put("colorSubContent", "#3482FF")
                    .put("colorSubContentDark", "#277AF7")
                    .put("picFunction", "miui.focus.pic_timer");

            jSONObject4.put("action", "miui.focus.action_1");

            jSONArray.put(jSONObject4);

            extras.putString("miui.focus.param", jSONObject.toString());

            Notification.Action build = new Notification.Action.Builder(Icon.createWithResource(context, R.mipmap.ic_launcher), "", null).build();
            build.getExtras().putString("icon_name", "action_close");

            Bundle bundle2 = new Bundle();
            bundle2.putParcelable("miui.focus.action_1", build);
            extras.putBundle("miui.focus.actions", bundle2);

            Bundle bundle3 = new Bundle();
            bundle3.putParcelable("miui.focus.pic_ticker", Icon.createWithResource(context, R.mipmap.ic_launcher));
            bundle3.putParcelable("miui.focus.pic_ticker_dark", Icon.createWithResource(context, R.mipmap.ic_launcher));

            extras.putBundle("miui.focus.pics", bundle3);

            builder.addExtras(extras);
            /*return builder.build();*/
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setEnableFloat(Notification notification, boolean enable) {
        try {
            Object obj = notification.getClass().getDeclaredField("extraNotification").get(notification);
            obj.getClass().getDeclaredMethod("setEnableFloat", Boolean.TYPE).invoke(obj, Boolean.valueOf(enable));
        } catch (Exception unused) {}
    }

    private static void createAppCrashChannel(Context context) {
        String name = context.getResources().getString(com.sevtinge.hyperceiler.ui.R.string.notification_channel_app_crash_name);
        createNotificationChannel(context, CHANNEL_ID_APP_CRASH, name, "", NotificationManager.IMPORTANCE_HIGH);
    }

    // 创建渠道并设置重要性
    public static void createNotificationChannel(Context context, String id, CharSequence name, String description, int importance) {
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not available in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            if (!TextUtils.isEmpty(description)) channel.setDescription(description);
            manager.createNotificationChannel(channel);
        }
    }
}
