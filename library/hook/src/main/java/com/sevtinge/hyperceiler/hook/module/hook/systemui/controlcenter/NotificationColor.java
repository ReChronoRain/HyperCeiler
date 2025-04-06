/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class NotificationColor extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_headsUp_color_1", R.color.focus_notification_element_blend_headsUp_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_headsUp_color_2", R.color.focus_notification_element_blend_headsUp_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_headsUp_color_3", R.color.focus_notification_element_blend_headsUp_color_3);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_keyguard_color_1", R.color.focus_notification_element_blend_keyguard_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_keyguard_color_2", R.color.focus_notification_element_blend_keyguard_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_keyguard_color_3", R.color.focus_notification_element_blend_keyguard_color_3);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_shade_color_1", R.color.focus_notification_element_blend_shade_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_shade_color_2", R.color.focus_notification_element_blend_shade_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "focus_notification_element_blend_shade_color_3", R.color.focus_notification_element_blend_shade_color_3);
        mResHook.setResReplacement("com.android.systemui", "color", "media_notification_element_blend_keyguard_color_1", R.color.media_notification_element_blend_keyguard_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "media_notification_element_blend_keyguard_color_2", R.color.media_notification_element_blend_keyguard_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "media_notification_element_blend_keyguard_color_3", R.color.media_notification_element_blend_keyguard_color_3);
        mResHook.setResReplacement("com.android.systemui", "color", "media_notification_element_blend_shade_color_1", R.color.media_notification_element_blend_shade_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "media_notification_element_blend_shade_color_2", R.color.media_notification_element_blend_shade_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "media_notification_element_blend_shade_color_3", R.color.media_notification_element_blend_shade_color_3);
        mResHook.setResReplacement("com.android.systemui", "color", "notification_element_blend_headsUp_color_1", R.color.notification_element_blend_headsUp_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "notification_element_blend_headsUp_color_2", R.color.notification_element_blend_headsUp_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "notification_element_blend_keyguard_color_1", R.color.notification_element_blend_keyguard_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "notification_element_blend_keyguard_color_2", R.color.notification_element_blend_keyguard_color_2);
        mResHook.setResReplacement("com.android.systemui", "color", "notification_element_blend_shade_color_1", R.color.notification_element_blend_shade_color_1);
        mResHook.setResReplacement("com.android.systemui", "color", "notification_element_blend_shade_color_2", R.color.notification_element_blend_shade_color_2);
    }
}
