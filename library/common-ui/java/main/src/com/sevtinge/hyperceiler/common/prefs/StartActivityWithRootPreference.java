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
package com.sevtinge.hyperceiler.common.prefs;

import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.checkRootPermission;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.ui.R;

import java.io.DataOutputStream;
import java.io.IOException;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class StartActivityWithRootPreference extends Preference {

    private String targetActivityClass;

    public StartActivityWithRootPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        targetActivityClass = attrs.getAttributeValue("http://schemas.android.com/apk/res-custom", "targetActivityClass");
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setOnClickListener(v -> {
            launchActivityWithRoot();
        });
    }

    private void launchActivityWithRoot() {
        if (checkRootPermission() == 0) {
            try {
                Process suProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                os.writeBytes("am start -n " + targetActivityClass + "\n");
                os.flush();
                os.writeBytes("exit\n");
                os.flush();
                suProcess.waitFor();
            } catch (IOException | InterruptedException e) {
                logE("StartActivityWithRootPreference", "com.sevtinge.hyperceiler", "Failed to start activity \"" + targetActivityClass + "\" with root", e);
            }
        } else {
            Toast.makeText(this.getContext(), R.string.start_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
