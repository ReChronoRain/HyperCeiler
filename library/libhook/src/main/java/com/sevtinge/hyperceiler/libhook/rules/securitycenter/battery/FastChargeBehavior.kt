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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.BatteryManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.ToastHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findFieldValueAs
import io.github.lingqiqi5211.ezhooktool.core.invokeAs
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class FastChargeBehavior : BaseHook() {
    private var showEnterNotificationMethods = emptyList<Method>()
    private var showExitNotificationMethods = emptyList<Method>()
    private var cancelNotificationMethods = emptyList<Method>()
    private var handlePowerConnectedMethods = emptyList<Method>()
    private var fastChargeNotificationClickMethod: Method? = null
    private var updateFastChargeTipMethod: Method? = null
    private var fastChargeTipClickMethod: Method? = null
    private var setupFastChargeTipMethod: Method? = null
    private var isFastChargeEnabledMethod: Method? = null
    private var isPowerConnectedMethod: Method? = null
    private var isDefaultFastChargeEnabledMethod: Method? = null
    private var fastChargePowerMaxMethod: Method? = null
    private var setWirelessFastChargeMethod: Method? = null
    private var fastChargeNotificationMode = 0
    private var replacingNotification = false
    private var enterNotificationHandled = false
    private val messageViewClass by lazy { findClassIfExists(MESSAGE_VIEW_CLASS_NAME) }

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        fastChargeNotificationMode =
            PrefsBridge.getStringAsInt("security_center_fast_charge_notification_mode", 0)

        showEnterNotificationMethods = findFastChargeNotificationMethods(
            key = "ShowEnterFastChargeNotificationV4",
            enter = true
        )
        showExitNotificationMethods = findFastChargeNotificationMethods(
            key = "ShowExitFastChargeNotificationV4",
            enter = false
        )
        cancelNotificationMethods = optionalMemberList("CancelFastChargeNotification") {
            it.findMethod {
                matcher {
                    usingNumbers(NOTIFICATION_ID_FAST_CHARGE, 5)
                    addInvoke("Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;")
                    addInvoke("Landroid/app/NotificationManager;->cancel(I)V")
                    paramCount = 1
                }
            }
        }
        handlePowerConnectedMethods = optionalMemberList("HandleFastChargePowerConnected") {
            it.findMethod {
                matcher {
                    usingEqStrings(
                        EXTRA_QUICK_CHARGE_TYPE,
                        EXTRA_POWER_MAX,
                        "handlePowerConnected: BATTERY_PLUGGED_WIRELESS=4"
                    )
                    addInvoke("Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I")
                    paramCount = 2
                }
            }
        }
        fastChargeNotificationClickMethod = optionalMember("FastChargeNotificationClick") {
            it.findMethod {
                matcher {
                    usingEqStrings(
                        "FastChargeController",
                        "onFastChargeNotificationClicked: action=",
                        ACTION_TURN_ON_FAST_CHARGE,
                        ACTION_TURN_OFF_FAST_CHARGE,
                        EXTRA_PLUG_TYPE,
                        FAST_CHARGE_ENABLED_KEY
                    )
                    addInvoke("Landroid/content/Intent;->getAction()Ljava/lang/String;")
                    addInvoke("Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I")
                    addInvoke($$"Landroid/provider/Settings$Secure;->putInt(Landroid/content/ContentResolver;Ljava/lang/String;I)Z")
                    paramCount = 1
                }
            }.singleOrNull()
        } as? Method
        updateFastChargeTipMethod = optionalMember("UpdateFastChargeTip") {
            it.findMethod {
                matcher {
                    usingEqStrings(FAST_CHARGE_ENABLED_KEY)
                    usingNumbers(1)
                    addInvoke($$"Landroid/provider/Settings$Secure;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I")
                    addInvoke("Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")
                }
            }.singleOrNull()
        } as? Method
        fastChargeTipClickMethod = optionalMember("FastChargeTipClickV2") {
            it.findMethod {
                matcher {
                    usingEqStrings("fast_charge_activity_enterway", "security_center")
                    addInvoke("Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;")
                    addInvoke("Landroid/content/Context;->startActivity(Landroid/content/Intent;)V")
                }
            }.singleOrNull()
        } as? Method
        setupFastChargeTipMethod = optionalMember("SetupFastChargeTipClick") {
            it.findMethod {
                matcher {
                    addInvoke("Lmiuix/miuixbasewidget/widget/MessageView;->setClosable(Z)V")
                    addInvoke($$"Lmiuix/miuixbasewidget/widget/MessageView;->setOnMessageViewEndIconClickListener(Lmiuix/miuixbasewidget/widget/MessageView$c;)V")
                    paramCount = 1
                }
            }.singleOrNull()
        } as? Method
        isFastChargeEnabledMethod = optionalMember("IsFastChargeEnabled") {
            it.findMethod {
                matcher {
                    modifiers = Modifier.STATIC
                    usingEqStrings(FAST_CHARGE_ENABLED_KEY)
                    addInvoke("Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;")
                    addInvoke($$"Landroid/provider/Settings$Secure;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I")
                }
            }.singleOrNull()
        } as? Method
        isPowerConnectedMethod = findFastChargePreferenceMethod(
            cacheKey = "IsFastChargePowerConnected",
            preferenceKey = "key_fast_charge_power_connected",
            returnTypeName = "boolean"
        )
        isDefaultFastChargeEnabledMethod = findFastChargePreferenceMethod(
            cacheKey = "IsDefaultFastChargeEnabled",
            preferenceKey = "key_fast_charge_enabled_default",
            returnTypeName = "boolean"
        )
        fastChargePowerMaxMethod = findFastChargePreferenceMethod(
            cacheKey = "FastChargePowerMax",
            preferenceKey = "key_fast_charge_power_max",
            returnTypeName = "int"
        )
        setWirelessFastChargeMethod = optionalMember("SetWirelessFastCharge") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingEqStrings(
                            "persist.vendor.reverse.quickcharge",
                            "persist.vendor.accelerate.charge"
                        )
                    }
                    modifiers = Modifier.STATIC
                    usingNumbers(PLUG_TYPE_WIRELESS, 7)
                    paramCount = 1
                }
            }.singleOrNull()
        } as? Method

        return true
    }

    override fun init() {
        updateFastChargeTipMethod?.createAfterHook { param ->
            updateFastChargeTip(param.thisObject)
        }

        fastChargeTipClickMethod?.createBeforeHook { param ->
            if (handleFastChargeTipClick(param.thisObject)) {
                param.result = null
            }
        }

        setupFastChargeTipMethod?.createAfterHook { param ->
            installFastChargeTipClickListener(param.thisObject)
        }

        showEnterNotificationMethods.forEach { method ->
            method.createBeforeHook { param ->
                handleEnterFastChargeNotification(param)
            }
        }

        handlePowerConnectedMethods.forEach { method ->
            method.createBeforeHook {
                enterNotificationHandled = false
            }
            method.createAfterHook { param ->
                handleFastChargePowerConnected(param)
            }
        }

        if (fastChargeNotificationMode == MODE_AUTO_CONFIRM_REMOVE) {
            showExitNotificationMethods.forEach { method ->
                method.createBeforeHook { param ->
                    handleExitFastChargeNotification(param)
                }
            }
        }

        fastChargeNotificationClickMethod?.createBeforeHook { param ->
            if (handleFastChargeNotificationClick(param)) {
                param.result = null
            }
        }

        getHotReloadRuntimeState(HOT_RELOAD_TIP_HOST_KEY, Any::class.java)?.let {
            installFastChargeTipClickListener(it)
        }
    }

    private fun findFastChargeNotificationMethods(key: String, enter: Boolean): List<Method> {
        return optionalMemberList(key) {
            it.findMethod {
                matcher {
                    usingEqStrings(
                        if (enter) "fast_charge_enter_notification" else "fast_charge_exit_notification",
                        if (enter) ACTION_TURN_ON_FAST_CHARGE else ACTION_TURN_OFF_FAST_CHARGE,
                        "miui.showAction",
                        "miui.appIcon"
                    )
                    usingNumbers(NOTIFICATION_ID_FAST_CHARGE, 201326592)
                    addInvoke("Landroid/app/NotificationManager;->notify(ILandroid/app/Notification;)V")
                }
            }
        }
    }

    private fun findFastChargePreferenceMethod(
        cacheKey: String,
        preferenceKey: String,
        returnTypeName: String
    ): Method? {
        return optionalMember(cacheKey) {
            it.findMethod {
                matcher {
                    modifiers = Modifier.STATIC
                    usingEqStrings(preferenceKey)
                    returnType = returnTypeName
                    paramCount = 0
                }
            }.singleOrNull()
        } as? Method
    }

    private fun handleEnterFastChargeNotification(param: HookParam) {
        if (replacingNotification) return

        enterNotificationHandled = true
        val context = param.findContext() ?: return
        val plugType = (param.args.lastOrNull { it is Number } as? Number)?.toInt() ?: 0
        if (!confirmFastCharge(context, plugType)) return

        param.result = null
    }

    private fun handleFastChargePowerConnected(param: HookParam) {
        val context = param.findContext() ?: return
        if (enterNotificationHandled || !shouldAutoConfirmFastCharge(context)) return

        confirmFastCharge(context, getCurrentPlugType(context))
    }

    private fun handleExitFastChargeNotification(param: HookParam) {
        if (replacingNotification) return

        val context = param.findContext() ?: return
        cancelFastChargeNotification(context)
        param.result = null
    }

    private fun handleFastChargeNotificationClick(param: HookParam): Boolean {
        val intent = param.args.firstNotNullOfOrNull { it as? Intent } ?: return false
        val enable = when (intent.action) {
            ACTION_TURN_ON_FAST_CHARGE -> true
            ACTION_TURN_OFF_FAST_CHARGE -> false
            else -> return false
        }
        val context = param.findContext() ?: return false
        val plugType = intent.getIntExtra(EXTRA_PLUG_TYPE, 1)

        return applyFastChargeState(context, plugType, enable, refreshNotification = true)
    }

    private fun confirmFastCharge(context: Context, plugType: Int): Boolean {
        return applyFastChargeState(context, plugType, enable = true, refreshNotification = true)
    }

    private fun applyFastChargeState(
        context: Context,
        plugType: Int,
        enable: Boolean,
        refreshNotification: Boolean
    ): Boolean {
        if (!setFastChargeEnabled(context, plugType, enable)) return false

        if (refreshNotification) {
            updateFastChargeNotification(context, plugType, enable)
        }
        return true
    }

    private fun updateFastChargeNotification(
        context: Context,
        plugType: Int,
        fastChargeEnabled: Boolean
    ) {
        cancelFastChargeNotification(context)
        if (fastChargeNotificationMode == MODE_AUTO_CONFIRM_REMOVE) {
            return
        }

        showReplacementNotification(context, plugType, fastChargeEnabled)
    }

    private fun handleFastChargeTipClick(hostObject: Any?): Boolean {
        val context = hostObject.findContext() ?: return false
        if (!shouldHandleFastChargeTipClick(hostObject, context)) return false

        val enable = !isFastChargeEnabled(context)
        if (!applyFastChargeState(context, plugType = 0, enable = enable, refreshNotification = false)) {
            return false
        }

        updateFastChargeTip(hostObject)
        showFastChargeToast(context, enable)
        return true
    }

    private fun shouldHandleFastChargeTipClick(hostObject: Any?, context: Context): Boolean {
        val tipView = getFastChargeTipView(hostObject) ?: return false
        if (tipView.visibility != View.VISIBLE) return false

        return isModuleFastChargeTipText(context, getMessageViewText(tipView))
    }

    private fun installFastChargeTipClickListener(hostObject: Any?) {
        val tipView = getFastChargeTipView(hostObject) ?: return

        tipView.setOnClickListener {
            if (!handleFastChargeTipClick(hostObject)) {
                invokeOriginalFastChargeTipClick(hostObject)
            }
        }
        if (hostObject != null) {
            putHotReloadRuntimeState(HOT_RELOAD_TIP_HOST_KEY, hostObject)
        }
        registerHotReloadCleanup {
            tipView.setOnClickListener(null)
        }
    }

    private fun invokeOriginalFastChargeTipClick(hostObject: Any?) {
        val method = fastChargeTipClickMethod ?: return
        if (hostObject == null) return

        runCatching {
            method.invokeAs<Any?>(hostObject)
        }.onFailure {
            XposedLog.w(TAG, packageName, "Failed to invoke original fast charge tip click", it)
        }
    }

    private fun setFastChargeEnabled(context: Context, plugType: Int, enable: Boolean): Boolean {
        return runCatching {
            if (plugType != PLUG_TYPE_WIRELESS || !setWirelessFastCharge(enable)) {
                Settings.Secure.putInt(
                    context.contentResolver,
                    FAST_CHARGE_ENABLED_KEY,
                    if (enable) 1 else 0
                )
            }
            true
        }.getOrElse {
            XposedLog.e(TAG, packageName, "Failed to update fast charge state", it)
            false
        }
    }

    private fun updateFastChargeTip(hostObject: Any?) {
        val context = hostObject.findContext() ?: return
        if (!isFastChargeAvailable()) return

        val tipView = getFastChargeTipView(hostObject) ?: return
        val enabled = isFastChargeEnabled(context)

        tipView.visibility = View.VISIBLE
        runCatching {
            tipView.callMethod("setMessage", getFastChargeTipText(context, enabled))
        }.onFailure {
            XposedLog.w(TAG, packageName, "Failed to update fast charge tip", it)
        }
    }

    private fun shouldAutoConfirmFastCharge(context: Context): Boolean {
        return isFastChargeAvailable() && !isFastChargeEnabled(context)
    }

    private fun isFastChargeAvailable(): Boolean {
        val isPowerConnected = runCatching {
            isPowerConnectedMethod?.invokeAs<Boolean>(null)
        }.getOrNull() == true
        val isDefaultFastChargeEnabled = runCatching {
            isDefaultFastChargeEnabledMethod?.invokeAs<Boolean>(null)
        }.getOrNull() == true

        return isPowerConnected && !isDefaultFastChargeEnabled && getFastChargePowerMax() != 0
    }

    private fun isModuleFastChargeTipText(context: Context, text: CharSequence?): Boolean {
        if (text == null) return false

        val current = text.toString()
        return current.contentEquals(getFastChargeTipText(context, enabled = false)) ||
            current.contentEquals(getFastChargeTipText(context, enabled = true))
    }

    private fun getMessageViewText(view: View): CharSequence? {
        return (view.findViewById<View>(android.R.id.text1) as? TextView)?.text
    }

    private fun isFastChargeEnabled(context: Context): Boolean {
        val methodValue = isFastChargeEnabledMethod?.let { method ->
            runCatching { method.invokeAs<Boolean>(null, context) }.getOrNull()
        }
        if (methodValue != null) return methodValue

        return Settings.Secure.getInt(context.contentResolver, FAST_CHARGE_ENABLED_KEY, 0) == 1
    }

    private fun getFastChargeTipView(hostObject: Any?): View? {
        if (hostObject == null) return null
        val viewClass = messageViewClass ?: return null

        return runCatching {
            hostObject.findFieldValueAs<View> {
                type(viewClass)
                notStatic()
            }
        }.getOrNull()
    }

    private fun setWirelessFastCharge(enable: Boolean): Boolean {
        val method = setWirelessFastChargeMethod ?: return false

        return runCatching {
            method.invokeAs<Any?>(null, enable)
            true
        }.getOrElse {
            XposedLog.w(TAG, packageName, "Failed to set wireless fast charge", it)
            false
        }
    }

    private fun showReplacementNotification(
        context: Context,
        plugType: Int,
        fastChargeEnabled: Boolean
    ) {
        runCatching {
            replacingNotification = true
            if (!showFastChargeNotification(context, plugType, fastChargeEnabled)) {
                cancelFastChargeNotification(context)
            }
        }.onFailure {
            XposedLog.w(TAG, packageName, "Failed to update fast charge notification", it)
        }
        replacingNotification = false
    }

    private fun showFastChargeNotification(
        context: Context,
        plugType: Int,
        fastChargeEnabled: Boolean
    ): Boolean {
        if (fastChargeEnabled) {
            return showExitNotificationMethods.invokeAny(context, plugType)
        }

        val powerMax = getFastChargePowerMax()
        return powerMax != 0 && showEnterNotificationMethods.invokeAny(context, powerMax, plugType)
    }

    private fun List<Method>.invokeAny(vararg args: Any?): Boolean {
        return any { method ->
            runCatching {
                method.invokeAs<Any?>(null, *args)
                true
            }.getOrDefault(false)
        }
    }

    private fun HookParam.findContext(): Context? {
        return args.firstNotNullOfOrNull { it as? Context }
            ?: thisObject.findContext()
    }

    private fun Any?.findContext(): Context? {
        return (this as? Context)
            ?: runCatching {
                this?.findFieldValueAs<Context> {
                    typeExtendsFrom(Context::class.java)
                    notStatic()
                }
            }.getOrNull()
    }

    private fun getCurrentPlugType(context: Context): Int {
        return runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        }.getOrDefault(0)
    }

    private fun getFastChargePowerMax(): Int {
        return runCatching {
            fastChargePowerMaxMethod?.invokeAs<Number>(null)?.toInt()
        }.getOrNull() ?: 0
    }

    private fun showFastChargeToast(context: Context, enabled: Boolean) {
        val modRes = AppsTool.getModuleRes(context)
        val featureName = modRes.getString(R.string.security_center_fast_charge_high_power)
        val message = modRes.getString(
            if (enabled) {
                R.string.quick_settings_state_change_message_on_my
            } else {
                R.string.quick_settings_state_change_message_off_my
            },
            featureName
        )
        ToastHelper.makeText(context, message)
    }

    private fun getFastChargeTipText(context: Context, enabled: Boolean): CharSequence {
        val modRes: Resources = AppsTool.getModuleRes(context)
        return modRes.getString(
            if (enabled) {
                R.string.security_center_fast_charge_tip_turn_off
            } else {
                R.string.security_center_fast_charge_tip_turn_on
            }
        )
    }

    private fun cancelFastChargeNotification(context: Context) {
        val cancelled = cancelNotificationMethods.any { method ->
            runCatching {
                method.invokeAs<Any?>(null, context)
                true
            }.getOrDefault(false)
        }
        if (cancelled) return

        runCatching {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_FAST_CHARGE)
        }.onFailure {
            XposedLog.w(TAG, packageName, "Failed to cancel fast charge notification", it)
        }
    }

    companion object {
        private const val HOT_RELOAD_TIP_HOST_KEY = "FastChargeBehavior.tipHost"

        // ── 行为模式 ─────────────────────────────────────────────
        /** 自动确认快充并移除原确认通知。 */
        private const val MODE_AUTO_CONFIRM_REMOVE = 2

        // ── 通知 / 插拔类型 ───────────────────────────────────────
        /** 管家原始快充通知 ID（与 DexKit usingNumbers 配套，不可改）。 */
        private const val NOTIFICATION_ID_FAST_CHARGE = 2021051618
        /** BatteryManager.BATTERY_PLUGGED_WIRELESS 的实际取值。 */
        private const val PLUG_TYPE_WIRELESS = 4

        // ── Settings.Secure / Intent keys ────────────────────────
        private const val FAST_CHARGE_ENABLED_KEY = "key_fast_charge_enabled"
        private const val EXTRA_QUICK_CHARGE_TYPE = "miui.intent.extra.quick_charge_type"
        private const val EXTRA_POWER_MAX = "miui.intent.extra.POWER_MAX"
        private const val EXTRA_PLUG_TYPE = "plugType"

        // ── Intent actions ──────────────────────────────────────
        private const val ACTION_TURN_ON_FAST_CHARGE =
            "com.miui.powercenter.action.TURN_ON_FAST_CHARGE"
        private const val ACTION_TURN_OFF_FAST_CHARGE =
            "com.miui.powercenter.action.TURN_OFF_FAST_CHARGE"

        // ── 视图类 ──────────────────────────────────────────────
        private const val MESSAGE_VIEW_CLASS_NAME =
            "miuix.miuixbasewidget.widget.MessageView"
    }
}
