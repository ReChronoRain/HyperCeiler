package com.sevtinge.hyperceiler.libhook.rules.packageinstaller;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** Hides the OS3 risk card when the scanner was disabled and has no result. */
public class DisableRiskVerificationUi extends BaseHook {
    @Override
    public void init() {
        Class<?> virusInfo = findClassIfExists(
                "com.miui.packageInstaller.ui.listcomponets.VirusInfoViewObject");

        IMethodHook hide = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Object value = param.getArgs().length == 0 ? null : param.getArgs()[0];
                if (value instanceof RecyclerView.ViewHolder viewHolder) {
                    viewHolder.itemView.setVisibility(View.GONE);
                    // The original binder restores VISIBLE for a null/empty
                    // Virus object and replaces the text with “risk checking”.
                    // Skip it entirely after hiding the row.
                    param.setResult(null);
                }
            }
        };
        // Both names are present in OS3 builds: s() is the adapter entry and
        // E() performs the actual binding. Hook all overloads to avoid a
        // loader-specific nested-holder signature mismatch.
        if (virusInfo != null) {
            hookAllMethods(virusInfo, "s", hide);
            hookAllMethods(virusInfo, "E", hide);
        }

        // u2.r$b is only an abstract callback interface and cannot be hooked.
        // OS3 uses these two concrete button implementations for the risk
        // checking spinner.  Force only the progress request off; leaving the
        // rest of the button implementation intact keeps the install action
        // usable after the scan bypass completes.
        IMethodHook hideProgress = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (param.getArgs().length > 0 && Boolean.TRUE.equals(param.getArgs()[0])) {
                    param.getArgs()[0] = Boolean.FALSE;
                }
            }
        };
        Class<?> scanButton = findClassIfExists("com.miui.packageInstaller.view.ScanActionButton");
        if (scanButton != null) {
            findAndHookMethod(scanButton, "setProgressVisibility", boolean.class, hideProgress);
        }
        Class<?> installerButton = findClassIfExists("com.miui.packageInstaller.view.InstallerActionButton");
        if (installerButton != null) {
            findAndHookMethod(installerButton, "setProgressVisibility", boolean.class, hideProgress);
        }
    }
}
