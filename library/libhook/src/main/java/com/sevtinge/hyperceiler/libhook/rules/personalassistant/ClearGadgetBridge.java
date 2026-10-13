/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.personalassistant;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.base.BaseData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** Transport only. PA's original MAML parser, renderer, DAO and drag ACK own the item.
 * The resource is copied into PA's private files, so killing HyperCeiler changes nothing. */
public final class ClearGadgetBridge extends BaseHook {
    public static final int WIRE_TYPE = 0x18430c01;
    private static final String PRODUCT = "hyperceiler.home.clear12.v1";
    private static final String ICON = "flutter_assets/assets/images/gadget/gadget_clear_button.webp";
    private Method parser;
    private int diagnostics;

    @Override protected boolean useDexKit() { return true; }

    @Override protected boolean initDexKit() {
        parser = requiredMember("ClearGadgetParserV1", new IDexKit() {
            @Override public BaseData dexkit(DexKitBridge bridge) {
                return bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings("widget_type", "widget_origin_id", "maml_product_id")
                    .paramTypes(Bundle.class)
                    .returnType("com.miui.personalassistant.widget.entity.ItemInfo")))
                    .singleOrNull();
            }
        });
        return true;
    }

    // Gadget has no Android appWidgetId/widget_id; the guarded private wire marker identifies it.
    public static boolean matches(int type, int x, int y) {
        return type == WIRE_TYPE && x == 1 && y == 1;
    }

    @Override public void init() {
        hookMethod(parser, new IMethodHook() {
            @Override public void before(HookParam param) {
                Bundle source = (Bundle) param.getArgs()[0];
                if (source != null && diagnostics++ < 3)
                    Log.i("HyperCeiler.GadgetBridge", "parser input type=" + source.getInt("widget_type")
                        + " span=" + source.getInt("widget_span_x") + "x" + source.getInt("widget_span_y")
                        + " id=" + source.getInt("widget_id", -1));
                if (source == null || !matches(source.getInt("widget_type"),
                    source.getInt("widget_span_x"), source.getInt("widget_span_y"))) return;
                try {
                    Context context = EzXposed.getAppContext();
                    File resource = ensureResource(context);
                    // Clone: retain session/source/preview/origin. Never mutate a shared Bundle.
                    Bundle converted = new Bundle(source);
                    converted.putInt("widget_type", 2);
                    converted.putString("maml_product_id", PRODUCT);
                    converted.putInt("maml_version", 1);
                    converted.putString("maml_type", "1x1");
                    converted.putString("maml_title", "一键清理");
                    converted.putParcelable("maml_uri", Uri.fromFile(resource));
                    converted.putBoolean("maml_clip_corner", false);
                    converted.putString("picker_id", PRODUCT);
                    param.getArgs()[0] = converted;
                    Log.i("HyperCeiler.GadgetBridge", "Gadget12 -> original PA MAML parser, 1x1");
                } catch (Exception failure) {
                    // Leave the unknown wire type intact: original failure keeps the Home item.
                    Log.e("HyperCeiler.GadgetBridge", "resource preparation failed; no success ACK", failure);
                }
            }
        });
        Log.i("HyperCeiler.GadgetBridge", "parser bridge installed " + parser);
    }

    private static synchronized File ensureResource(Context context) throws Exception {
        File directory = new File(context.getFilesDir(), "hyperceiler-gadget-bridge");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException("mkdir");
        File target = new File(directory, "clear12-v1.zip");
        if (target.isFile() && target.length() > 0) return target;
        File staging = new File(directory, "clear12-v1.tmp");
        Context home = context.createPackageContext("com.miui.home", Context.CONTEXT_IGNORE_SECURITY);
        try (InputStream icon = home.getAssets().open(ICON);
             FileOutputStream file = new FileOutputStream(staging);
             ZipOutputStream zip = new ZipOutputStream(file)) {
            zip.putNextEntry(new ZipEntry("description.xml"));
            zip.write(description().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            // PA's real importer requires an outer theme and a nested widget_AxB ZIP.
            // A loose manifest is rejected and must never be acknowledged as a saved widget.
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            try (ZipOutputStream widget = new ZipOutputStream(payload)) {
                widget.putNextEntry(new ZipEntry("manifest.xml"));
                widget.write(manifest().getBytes(StandardCharsets.UTF_8));
                widget.closeEntry();
                widget.putNextEntry(new ZipEntry("images/icon.webp"));
                byte[] buffer = new byte[8192];
                int count;
                while ((count = icon.read(buffer)) != -1) widget.write(buffer, 0, count);
                widget.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("widget_1x1"));
            zip.write(payload.toByteArray());
            zip.closeEntry();
            zip.finish();
            file.getFD().sync();
        } catch (Exception failure) {
            staging.delete();
            throw failure;
        }
        if (!staging.renameTo(target)) {
            staging.delete();
            throw new java.io.IOException("resource rename");
        }
        return target;
    }

    public static String manifest() {
        // Original Home NormalClearButton broadcasts this exact action. No app service/timer.
        return """
            <Widget frameRate="0" version="1" screenWidth="1080" resDensity="480" scaleByDensity="false">
              <Image x="#view_width*0.16" y="#view_height*0.08" w="#view_width*0.68" h="#view_height*0.68" src="icon.webp"/>
              <Text x="#view_width/2" y="#view_height*0.82" align="center" size="26" color="#FFFFFFFF" text="一键清理"/>
              <Button x="0" y="0" w="#view_width" h="#view_height">
                <Triggers><Trigger action="up">
                  <IntentCommand broadcast="true" action="com.android.systemui.taskmanager.Clear" package="com.miui.home"/>
                </Trigger></Triggers>
              </Button>
            </Widget>
            """;
    }

    public static String description() {
        return """
            <theme><title>一键清理</title><author>HyperCeiler</author><designer>HyperCeiler</designer>
              <version>1</version><osVersion>2</osVersion><uiVersion>12</uiVersion><frame>0</frame>
              <description>桌面一键清理 1x1 负一屏桥接</description>
              <typeTag>theme_bind:com.miui.home</typeTag><editable>false</editable>
            </theme>
            """;
    }
}
