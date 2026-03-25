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
package com.sevtinge.hyperceiler.libhook.app.CorePatch;

import android.os.Build;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.AuthCreakPatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.CorePatchHelper;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.DigestCreakPatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.DowngradeCheckPatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.ExactSignCheckPatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.SharedUserPatch;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.VerificationAgentPatch;

import io.github.libxposed.api.XposedModuleInterface;

public class CorePatch {

    private static final String TAG = "CorePatch";
    public void onLoad(XposedModuleInterface.SystemServerStartingParam lpparam) {
        XposedLog.i(TAG, "CorePatchLoad: Current sdk version is " + Build.VERSION.SDK_INT);

        if (!ProjectApi.isRelease()) {
            XposedLog.i(TAG, "system", "downgrade=" + CorePatchHelper.isFeatureEnabled(CorePatchHelper.PREF_DOWNGRADE, true));
            XposedLog.i(TAG, "system", "authcreak=" + CorePatchHelper.isFeatureEnabled(CorePatchHelper.PREF_AUTH_CREAK, true));
            XposedLog.i(TAG, "system", "digestCreak=" + CorePatchHelper.isFeatureEnabled(CorePatchHelper.PREF_DIGEST_CREAK, true));
            XposedLog.i(TAG, "system", "UsePreSig=" + CorePatchHelper.isUsePreSignatureEnabled());
            XposedLog.i(TAG, "system", "exactSignatureCheck=" + CorePatchHelper.isFeatureEnabled(CorePatchHelper.PREF_EXACT_SIGNATURE_CHECK, false));
            XposedLog.i(TAG, "system", "sharedUser=" + CorePatchHelper.isSharedUserEnabled());
            XposedLog.i(TAG, "system", "disableVerificationAgent=" + CorePatchHelper.isFeatureEnabled(CorePatchHelper.PREF_DISABLE_VERIFICATION_AGENT, true));
        }

        new DowngradeCheckPatch().init(lpparam);
        new AuthCreakPatch().init(lpparam);
        new DigestCreakPatch().init(lpparam);
        new ExactSignCheckPatch().init(lpparam);
        new SharedUserPatch().init(lpparam);
        new VerificationAgentPatch().init(lpparam);
    }
}
