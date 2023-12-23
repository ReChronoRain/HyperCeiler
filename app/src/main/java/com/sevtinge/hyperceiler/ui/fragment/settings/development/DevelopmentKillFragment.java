package com.sevtinge.hyperceiler.ui.fragment.settings.development;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.AppData;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.ShellUtils;

import java.util.ArrayList;
import java.util.List;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.preference.Preference;

public class DevelopmentKillFragment extends SettingsPreferenceFragment {
    public List<AppData> appData = new ArrayList<>();
    Preference mKillPackage;

    Preference mName;
    Preference mCheck;
    Handler handler;

    public interface EditDialogCallback {
        void onInputReceived(String userInput);
    }

    public interface KillCallback {
        void onKillCallback(String kill, String rest);
    }

    public interface GetCallback {
        void onCallback(Boolean boo, String rest);
    }

    @Override
    public int getContentResId() {
        return R.xml.prefs_development_kill;
    }

    @Override
    public void initPrefs() {
        mKillPackage = findPreference("prefs_key_development_kill_package");
        mName = findPreference("prefs_key_development_kill_name");
        mCheck = findPreference("prefs_key_development_kill_check");
        mKillPackage.setVisible(false);
        mName.setVisible(false);
        mCheck.setVisible(false);
        handler = new Handler();
        initApp();
        mCheck.setOnPreferenceClickListener(
            preference -> {
                showInDialog(
                    userInput -> {
                        String pkg = "";
                        for (AppData appData1 : appData) {
                            if (appData1.label.equals(userInput)) {
                                pkg = appData1.packageName;
                            }
                        }
                        if (!(pkg == null || pkg.equals(""))) {
                            showOutDialog(getPackage(pkg, true, null));
                            return;
                        }
                        showOutDialog(getPackage(userInput, true, null));
                    }
                );
                return true;
            }
        );
        mName.setOnPreferenceClickListener(
            preference -> {
                showInDialog(
                    userInput -> {
                        if (!userInput.equals("")) {
                            String pkg = "";
                            for (AppData appData1 : appData) {
                                if (appData1.label.equals(userInput)) {
                                    pkg = appData1.packageName;
                                }
                            }
                            if (!(pkg == null || pkg.equals(""))) {
                                String finalPkg = pkg;
                                getAndKill(pkg, (boo, rest) -> {
                                    if (boo) {
                                        showOutDialog("kill success: " + rest);
                                    } else {
                                        showOutDialog("kill error: " + userInput + "\npkg: " + finalPkg);
                                    }
                                });
                            } else showOutDialog("kill error maybe not present: " + userInput);
                        }
                    }
                );
                return true;
            }
        );
        mKillPackage.setOnPreferenceClickListener(
            preference -> {
                showInDialog(
                    userInput -> {
                        if (!userInput.equals("")) {
                            getAndKill(userInput, (boo, rest) -> {
                                if (boo) {
                                    showOutDialog("kill success: " + rest);
                                } else showOutDialog("kill error: " + userInput);
                            });
                        }
                    }
                );
                return true;
            }
        );
    }

    private void getAndKill(String pkg, GetCallback getCallback) {
        getPackage(pkg, false, (kill, rest) -> {
                getCallback.onCallback(killPackage(kill), rest);
            }
        );
    }

    private boolean killPackage(String kill) {
        ShellUtils.CommandResult commandResult =
            ShellUtils.execCommand(
                "{ pid=$(pgrep -f '" + kill + "' | grep -v $$);" +
                    " [[ $pid != \"\" ]] && { pkill -l 9 -f \"" + kill + "\";" +
                    " { [[ $? != 0 ]] && { killall -s 9 \"" + kill + "\" &>/dev/null;};}" +
                    " || { { for i in $pid; do kill -s 9 \"$i\" &>/dev/null;done;};}" +
                    " || { echo \"kill error\";};};}" +
                    " || { echo \"kill error\";}",
                true, true);
        if (commandResult.result == 0) {
            return !commandResult.successMsg.equals("kill error");
        } else
            return false;
    }

    private String getPackage(String pkg, boolean ned, KillCallback killCallback) {
        ShellUtils.CommandResult commandResult = ShellUtils.execCommand(
            "pid=$(ps -A -o PID,ARGS=CMD | grep \"" + pkg + "\" | grep -v \"grep\");" +
                " get=\"\"; for i in $pid; do if [[ $(echo $i | grep '[0-9]' 2>/dev/null) == \"\" ]];" +
                " then if [[ $get == \"\" ]]; then get=$i; else get=\"$get฿$i\";" +
                " fi; fi; done; echo $get\n", true, true);
        if (ned) return commandResult.successMsg.replace("฿", "\n");
        if (commandResult.result == 0) {
            killCallback.onKillCallback(pkg, commandResult.successMsg.replace("฿", "\n"));
        }
        return null;
    }

    private void initApp() {
        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getAppSelector(getContext(), appData);
                            mKillPackage.setVisible(true);
                            mName.setVisible(true);
                            mCheck.setVisible(true);
                        }
                    });
                }
            }
        ).start();
    }

    private void getAppSelector(Context context, List<AppData> appInfoList) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);

        AppData appData;
        for (PackageInfo packageInfo : packageInfos) {
            appData = new AppData();
            appData.label = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            appData.packageName = packageInfo.packageName;
            appInfoList.add(appData);
        }
    }

    private void showInDialog(EditDialogCallback callback) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.kill_1)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String userInput = input.getText().toString();
                if (userInput.equals("")) {
                    dialog.dismiss();
                    showInDialog(callback);
                    return;
                }
                callback.onInputReceived(userInput);
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void showOutDialog(String show) {
        new AlertDialog.Builder(getActivity())
            .setCancelable(false)
            .setTitle(R.string.edit_out)
            .setMessage(show)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
}
