package com.sevtinge.hyperceiler.ui.preference;

import static com.sevtinge.hyperceiler.utils.ShellUtils.checkRootPermission;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import com.sevtinge.hyperceiler.R;

import java.io.DataOutputStream;
import java.io.IOException;

import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceViewHolder;

public class StartActivityWithRootPreference extends Preference {

    private String targetActivityClass;

    public StartActivityWithRootPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        targetActivityClass = attrs.getAttributeValue("http://schemas.android.com/apk/res-custom", "targetActivityClass");
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
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
