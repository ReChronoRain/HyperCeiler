package com.sevtinge.hyperceiler.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.app.GlobalActions;
import com.sevtinge.hyperceiler.utils.ShellUtils;

import java.util.Arrays;
import java.util.List;

import moralnorm.appcompat.app.AlertDialog;

public class RestartAlertDialog extends AlertDialog {

    List<String> mAppNameList;
    List<String> mAppPackageNameList;

    public RestartAlertDialog(Context context) {
        super(context);
        setTitle(R.string.hyperceiler_restart_quick);
        setView(createMultipleChoiceView(context));
    }

    private MultipleChoiceView createMultipleChoiceView(Context context) {
        Resources mRes = context.getResources();
        MultipleChoiceView view = new MultipleChoiceView(context);
        mAppNameList = Arrays.asList(mRes.getStringArray(R.array.restart_apps_name));
        mAppPackageNameList = Arrays.asList(mRes.getStringArray(R.array.restart_apps_packagename));
        view.setData(mAppNameList, null);
        view.deselectAll();
        view.setOnCheckedListener(sparseBooleanArray -> {
            dismiss();
            for (int i = 0; i < sparseBooleanArray.size(); i++) {
                if (sparseBooleanArray.get(i)) {
                    // ShellUtils.execCommand("pkill -l 9 -f " + mAppPackageNameList.get(i), true, false);
                    // String test = "XX";
                    String packageGet = mAppPackageNameList.get(i);
                    ShellUtils.execCommand("{ pid=$(pgrep -f '" + packageGet + "' | grep -v $$);" +
                            " [[ $pid != \"\" ]] && { pkill -l 9 -f \"" + packageGet + "\";" +
                            " { [[ $? != 0 ]] && { killall -s 9 \"" + packageGet + "\" &>/dev/null;};}" +
                            " || { { for i in $pid; do kill -s 9 \"$i\" &>/dev/null;done;};}" +
                            " || { echo \"kill error\";};};}" +
                            " || { echo \"kill error\";}",
                        true, false);
                }
            }
        });
        return view;
    }

    public void restartApp(Context context, String packageName) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartApps");
        intent.putExtra("packageName", packageName);
        context.sendBroadcast(intent);
    }

    public void restartSystemUI(Context context) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI");
        intent.setPackage("com.android.systemui");
        context.sendBroadcast(intent);
    }
}
