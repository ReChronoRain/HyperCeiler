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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.io.IOException;

/** Plays the bundled original footage once in the module's own surface host. */
public final class XiaoheiziCapsuleView extends ImageView {
    // Source footage: https://www.bilibili.com/video/BV1ot411M7V1/
    public static final int PLAYBACK_DURATION_MS = 3_400;
    private static final String CLIP_ASSET = "xiaoheizi_performance.webp";
    private static final float VERTICAL_PADDING_RATIO = 0.8f;

    private final AnimatedImageDrawable clip;
    private final boolean rage;
    private Runnable onEnd;

    public XiaoheiziCapsuleView(Context context, boolean rage) throws IOException {
        super(context);
        this.rage = rage;
        String asset = rage ? "xiaoheizi_rage.webp" : CLIP_ASSET;
        Drawable media = ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.getAssets(), asset));
        if (!(media instanceof AnimatedImageDrawable animation)) {
            throw new IOException("Xiaoheizi clip is not animated");
        }
        clip = animation;
        clip.setRepeatCount(rage ? AnimatedImageDrawable.REPEAT_INFINITE : 0);
        clip.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override public void onAnimationEnd(Drawable drawable) {
                setAlpha(0f);
                Runnable finished = onEnd;
                onEnd = null;
                if (finished != null) finished.run();
            }
        });
        setScaleType(rage ? ScaleType.FIT_XY : ScaleType.FIT_CENTER);
        setImageDrawable(clip);
        setAlpha(0f);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        setFocusable(false);
        setClickable(false);
    }

    public static int hostHeight(int capsuleHeight) {
        return Math.round(capsuleHeight * (1f + VERTICAL_PADDING_RATIO * 2f));
    }

    public static int capsuleTopInset(int capsuleHeight) {
        return Math.round(capsuleHeight * VERTICAL_PADDING_RATIO);
    }

    public boolean isRage() { return rage; }

    public void play(Runnable end) {
        if (clip.isRunning()) return;
        onEnd = end;
        setAlpha(1f);
        clip.start();
    }

    public void stop() {
        onEnd = null;
        clip.stop();
        setAlpha(0f);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
