/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.mediaeditor;

import android.content.ContentProvider;
import android.os.Bundle;
import android.util.Log;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** Gallery OS4 queries these creation capabilities in the ART MediaEditor process. */
public class GalleryCreationAvailabilityOS4 extends BaseHook {
    private static final String PROVIDER =
        "com.miui.mediaeditor.provider.MediaEditorProviderForGallery";
    private static final String AVAILABLE = "key_common_is_available";
    private boolean warned;

    @Override
    public void init() {
        findAndHookMethod(PROVIDER, "call", String.class, String.class, Bundle.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                // Run the real provider first, including its caller allowlist and
                // capability/module checks. Never turn an error/null into success.
                if (param.getHasThrowable() || param.isSkipped()) return;
                try {
                    if (!(param.getThisObject() instanceof ContentProvider provider)
                        || !"com.miui.gallery".equals(provider.getCallingPackage())
                        || !(param.getArgs()[0] instanceof String method)
                        || !(param.getResult() instanceof Bundle original)) return;
                    String preference = preferenceFor(method);
                    if (preference == null || !PrefsBridge.getBoolean(preference)) return;
                    // Do not invent a missing or differently typed provider field,
                    // and do not mutate a Bundle potentially retained by its owner.
                    if (!Boolean.FALSE.equals(original.get(AVAILABLE))) return;
                    Bundle result = new Bundle(original);
                    result.putBoolean(AVAILABLE, true);
                    param.setResult(result);
                } catch (Throwable t) {
                    warnOnce(t);
                }
            }
        });
    }

    private static String preferenceFor(String method) {
        return switch (method) {
            case "method_is_id_photo_available" -> "gallery_enable_id_photo";
            case "method_is_magic_matting_available" -> "gallery_enable_magic_matting";
            case "method_is_video_post_available" -> "gallery_enable_video_post";
            case "method_is_vlog_available" -> "gallery_enable_video_editor";
            default -> null;
        };
    }

    private synchronized void warnOnce(Throwable failure) {
        if (warned) return;
        warned = true;
        Log.w("HyperCeiler", "GalleryCreationAvailabilityOS4: retaining original provider result", failure);
    }
}
