/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.hooker.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.sevtinge.hyperceiler.core.R;

import java.util.Set;

/** Marks features whose default execution uses a HyperCeiler hook in system_server. */
final class HomeOS4FrameworkLabel extends ReplacementSpan {
    private static final Set<String> FRAMEWORK_FEATURES = Set.of(
        "prefs_key_home_layout_xiaoheizi_enable",
        "prefs_key_home_layout_xiaoheizi_rage",
        "prefs_key_home_dock_bg_custom_enable",
        "prefs_key_home_dock_unlock_style",
        "prefs_key_home_dock_add_blur",
        "prefs_key_home_dock_bg_height",
        "prefs_key_home_dock_bg_margin_horizontal",
        "prefs_key_home_dock_bg_margin_bottom",
        "prefs_key_home_dock_bg_radius",
        "prefs_key_home_recent_hide_status_bar_in_task_view",
        "prefs_key_home_recent_card_haptic",
        "prefs_key_home_recent_card_close_haptic",
        "prefs_key_home_recent_card_haptic_type",
        "prefs_key_home_recent_card_haptic_strength",
        "prefs_key_home_recent_card_haptic_custom", "prefs_key_home_recent_card_haptic_effect_id", "home_recent_haptic_effects",
        "prefs_key_home_gesture_enable",
        "prefs_key_home_navigation_quick_back",
        "prefs_key_home_gesture_double_tap",
        "prefs_key_home_gesture_up_swipe",
        "prefs_key_home_gesture_down_swipe",
        "prefs_key_home_gesture_up_swipe2",
        "prefs_key_home_gesture_down_swipe2",
        "prefs_key_home_gesture_shake",
        "prefs_key_home_navigation_assist_left_slide",
        "prefs_key_home_navigation_assist_right_slide",
        "prefs_key_home_gesture_line_long_press",
        "prefs_key_home_gesture_line_double_click"
    );

    private final float density;
    private final float textSize;
    private final int foreground;
    private final int background;
    private final Paint badgePaint = new Paint();

    private HomeOS4FrameworkLabel(Context context, String label) {
        var metrics = context.getResources().getDisplayMetrics();
        density = metrics.density;
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, metrics);
        foreground = context.getColor(R.color.home_os4_framework_label_foreground);
        background = context.getColor(R.color.home_os4_framework_label_background);
        setContentDescription(label);
    }

    static void apply(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup child) {
                apply(child);
                continue;
            }
            String key = preference.getKey();
            CharSequence title = preference.getTitle();
            if (key == null || !FRAMEWORK_FEATURES.contains(key) || title == null) continue;
            if (title instanceof Spanned styled
                && styled.getSpans(0, styled.length(), HomeOS4FrameworkLabel.class).length > 0) continue;

            Context context = preference.getContext();
            String label = context.getString(R.string.home_os4_framework_label);
            var titledLabel = new SpannableStringBuilder(title).append(' ').append(label);
            titledLabel.setSpan(new HomeOS4FrameworkLabel(context, label),
                titledLabel.length() - label.length(), titledLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            preference.setSingleLineTitle(false);
            preference.setTitle(titledLabel);
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt metrics) {
        badgePaint.set(paint);
        badgePaint.setTextSize(textSize);
        return (int) Math.ceil(badgePaint.measureText(text, start, end) + 12 * density);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int baseline, int bottom, @NonNull Paint paint) {
        badgePaint.set(paint);
        badgePaint.setTextSize(textSize);
        float width = badgePaint.measureText(text, start, end) + 12 * density;
        float center = baseline + (paint.ascent() + paint.descent()) / 2;
        float height = Math.min(bottom - top, badgePaint.descent() - badgePaint.ascent() + 6 * density);
        float labelBaseline = center - (badgePaint.ascent() + badgePaint.descent()) / 2;
        badgePaint.setColor(background);
        canvas.drawRoundRect(x, center - height / 2, x + width, center + height / 2,
            4 * density, 4 * density, badgePaint);
        badgePaint.setColor(foreground);
        canvas.drawText(text, start, end, x + 6 * density, labelBaseline, badgePaint);
    }
}
