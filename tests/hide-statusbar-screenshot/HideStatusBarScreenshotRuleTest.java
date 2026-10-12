package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.SurfaceControl;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.util.ArrayList;

/**
 * 截屏隐藏状态栏规则的宿主机回归测试（见 README.md）。
 *
 * <p>规则本体与 stubs 一起编译，不需要设备：stub 负责记录 receiver、复刻真机上
 * `MiuiCollapsedStatusBarFragment#onViewCreated` / `MiuiPhoneStatusBarView#onAttachedToWindow`
 * 被 hook 后回调的时机，测试再手工投递 `miui.intent.TAKE_SCREENSHOT` 广播。</p>
 */
public final class HideStatusBarScreenshotRuleTest {

    private static final Context HOST_CONTEXT = new Context();
    private static final String ACTION_TAKE_SCREENSHOT = "miui.intent.TAKE_SCREENSHOT";
    private static final String EXTRA_IS_FINISHED = "IsFinished";
    private static final String LEGACY_TARGET =
        "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment#onViewCreated";
    private static final String OS4_TARGET =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView#onAttachedToWindow";

    public static void main(String[] args) {
        restoresAfterSingleRegistration();
        duplicateRegistrationOfSameViewKeepsOneReceiver();
        staleReceiverAfterHotReloadDoesNotReHide();
        legacyImplementationWouldStayHidden();
        recreatedStatusBarViewIsRestoredToo();
        repeatedStartBroadcastsKeepOriginalVisibility();
        strayFinishedKeepsForeignHiddenStatusBar();
        alreadyHiddenStatusBarStaysHidden();
        recreatedWindowSurfaceDoesNotStayHidden();
        os4BranchHooksAttachedToWindow();
        System.out.println("Hide status bar on screenshot: single/duplicated registration, hot reload, "
            + "view recreation, repeated start, stray finish and already hidden cases passed");
    }

    /** 只注册一次时：开始隐藏、结束恢复。 */
    private static void restoresAfterSingleRegistration() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        check(BaseHook.hookTargets.size() == 1 && LEGACY_TARGET.equals(BaseHook.hookTargets.get(0)),
            "SDK 35 应 hook 旧状态栏 fragment");
        check(ContextCompat.registered.size() == 1, "应只注册一个 receiver");
        check(alphaOf(statusBar) == 1.0f, "初始窗口 alpha 应为 1");

        broadcast(false);
        check(statusBar.getVisibility() == View.INVISIBLE, "开始截屏应隐藏状态栏");
        check(alphaOf(statusBar) == 0.0f, "隐藏时应把窗口 alpha 置 0");
        check(XposedLog.contains("HOOK_STATE=HIDE"), "应记录 HIDE 日志");

        broadcast(true);
        check(statusBar.getVisibility() == View.VISIBLE, "结束截屏应恢复状态栏");
        check(alphaOf(statusBar) == 1.0f, "恢复时应把窗口 alpha 置回 1");
        check(XposedLog.lastCallback(true).contains("visibility=0"),
            "结束回调日志里的可见性应为 0");
    }

    /** 上游重复 initHook 时，同一个 view 只应注册一个 receiver。 */
    private static void duplicateRegistrationOfSameViewKeepsOneReceiver() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        check(BaseHook.installedHooks.size() == 2, "两次注册都会各自挂上 hook");
        check(ContextCompat.registered.size() == 1, "同一个 view 只注册一个 receiver");
        check(XposedLog.contains("HOOK_STATE=SKIP_DUPLICATE_VIEW"), "重复注册应被记录");

        broadcast(false);
        check(statusBar.getVisibility() == View.INVISIBLE, "重复注册后仍能隐藏");
        broadcast(true);
        check(statusBar.getVisibility() == View.VISIBLE, "重复注册后仍能恢复");
    }

    /**
     * 旧 receiver 仍活着时又有新的 receiver 盯上同一个 view（热重载最贴近真机的场景）：
     * 两个 receiver 都会处理同一个广播，收尾只能有一个“恢复值”生效。旧实现里后注册的一方
     * 会把 INVISIBLE 当成恢复值，把状态栏重新盖上；现在必须只恢复一次。
     */
    private static void staleReceiverAfterHotReloadDoesNotReHide() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        BaseHook.runHotReloadCleanups();
        install(new HideStatusBarBeforeScreenshot(), statusBar);
        check(ContextCompat.registered.size() == 2, "宿主侧旧 receiver 未注销，新实例会再注册一个");

        broadcast(false);
        check(statusBar.getVisibility() == View.INVISIBLE, "两个 receiver 都收到开始广播");
        broadcast(true);
        check(statusBar.getVisibility() == View.VISIBLE,
            "收尾后状态栏必须可见（旧实现在这里会留在 INVISIBLE）");
        check(alphaOf(statusBar) == 1.0f, "窗口 alpha 也要恢复");
    }

    /**
     * 反向对照：把旧实现（隐藏前的可见性放在实例字段里）放进同一个场景，状态栏必然留在隐藏态。
     *
     * <p>这条用例同时说明前面的断言不是空转：广播投递、hook 回调、可见性写入这条链路只要有一环
     * 失效，这里也会失败。</p>
     */
    private static void legacyImplementationWouldStayHidden() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new LegacyHideStatusBar(), statusBar);
        install(new LegacyHideStatusBar(), statusBar);

        broadcast(false);
        broadcast(true);
        check(statusBar.getVisibility() == View.INVISIBLE,
            "旧实现在收尾后仍留在 INVISIBLE，本修复的目标正是这一条");
    }

    /** 状态栏 view 被重建后，新旧 view 都应在收尾时恢复。 */
    private static void recreatedStatusBarViewIsRestoredToo() {
        reset();
        View oldStatusBar = new View(HOST_CONTEXT);
        View newStatusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), oldStatusBar);
        install(new HideStatusBarBeforeScreenshot(), newStatusBar);
        check(ContextCompat.registered.size() == 2, "两个 view 各注册一个 receiver");

        broadcast(false);
        check(oldStatusBar.getVisibility() == View.INVISIBLE
                && newStatusBar.getVisibility() == View.INVISIBLE,
            "两个 view 都应被隐藏");

        broadcast(true);
        check(oldStatusBar.getVisibility() == View.VISIBLE
                && newStatusBar.getVisibility() == View.VISIBLE,
            "两个 view 都应被恢复");
    }

    /** 同一次截屏里重复收到开始广播，不会把 INVISIBLE 记成恢复值。 */
    private static void repeatedStartBroadcastsKeepOriginalVisibility() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        broadcast(false);
        broadcast(false);
        broadcast(false);
        check(statusBar.getVisibility() == View.INVISIBLE, "重复开始广播应保持隐藏");

        broadcast(true);
        check(statusBar.getVisibility() == View.VISIBLE, "收尾后应恢复为可见");
    }

    /** 没有开始广播的结束广播不应把状态栏强行显示出来。 */
    private static void strayFinishedKeepsForeignHiddenStatusBar() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        statusBar.setVisibility(View.INVISIBLE);
        broadcast(true);
        check(statusBar.getVisibility() == View.INVISIBLE, "只应恢复本规则隐藏过的 view");
    }

    /** 隐藏前就已不可见时，恢复值应是当时的状态而不是 VISIBLE。 */
    private static void alreadyHiddenStatusBarStaysHidden() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        statusBar.setVisibility(View.INVISIBLE);
        broadcast(false);
        broadcast(true);
        check(statusBar.getVisibility() == View.INVISIBLE, "应恢复成隐藏前的 INVISIBLE");
    }

    /** SDK >= 37 走 onAttachedToWindow 分支。 */
    private static void os4BranchHooksAttachedToWindow() {
        reset();
        Build.VERSION.SDK_INT = 37;
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        check(BaseHook.hookTargets.size() == 1 && OS4_TARGET.equals(BaseHook.hookTargets.get(0)),
            "SDK 37 应 hook MiuiPhoneStatusBarView#onAttachedToWindow");

        broadcast(false);
        check(statusBar.getVisibility() == View.INVISIBLE, "OS4 分支也要隐藏");
        broadcast(true);
        check(statusBar.getVisibility() == View.VISIBLE, "OS4 分支也要恢复");
    }

    /** 截屏期间窗口 Surface 被重建时，收尾不能留下 0 alpha。 */
    private static void recreatedWindowSurfaceDoesNotStayHidden() {
        reset();
        View statusBar = new View(HOST_CONTEXT);
        install(new HideStatusBarBeforeScreenshot(), statusBar);

        broadcast(false);
        SurfaceControl hiddenSurface = statusBar.getViewRootImpl().getSurfaceControl();
        check(alphaOf(statusBar) == 0.0f, "隐藏时应把窗口 alpha 置 0");

        hiddenSurface.invalidateForTest();
        broadcast(true);
        check(statusBar.getVisibility() == View.VISIBLE, "窗口 Surface 重建后仍要恢复可见");
        check(alphaOf(statusBar) == 1.0f, "重建后的窗口 alpha 也要为 1");
    }

    private static void install(BaseHook rule, View statusBar) {
        rule.init();
        BaseHook.driveHooks(statusBar);
    }

    private static void broadcast(boolean finished) {
        Intent intent = new Intent(ACTION_TAKE_SCREENSHOT);
        intent.putExtra(EXTRA_IS_FINISHED, finished);
        for (BroadcastReceiver receiver : new ArrayList<>(ContextCompat.registered)) {
            receiver.onReceive(HOST_CONTEXT, intent);
        }
    }

    private static float alphaOf(View statusBar) {
        SurfaceControl surface = statusBar.getViewRootImpl().getSurfaceControl();
        return surface.getAlpha();
    }

    private static void reset() {
        // 借用一次 init() 注册的热重载清理，确保上一用例遗留的隐藏状态与 view 注册都被清空。
        new HideStatusBarBeforeScreenshot().init();
        BaseHook.runHotReloadCleanups();
        BaseHook.hookTargets.clear();
        BaseHook.installedHooks.clear();
        ContextCompat.registered.clear();
        XposedLog.messages.clear();
        Build.VERSION.SDK_INT = 35;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** 上游 4e9dc2789 的写法：隐藏前的可见性存在实例字段里，供反向对照。 */
    private static final class LegacyHideStatusBar extends BaseHook {
        private int visibilityBefore = View.VISIBLE;
        private boolean hidden;

        @Override
        public void init() {
            hookAllMethods("legacy", "onViewCreated", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    View view = (View) param.getArgs()[0];
                    Context context = view.getContext();
                    ContextCompat.registerReceiver(context, new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context ctx, Intent intent) {
                            if (!ACTION_TAKE_SCREENSHOT.equals(intent.getAction())) return;
                            if (intent.getBooleanExtra(EXTRA_IS_FINISHED, true)) {
                                if (hidden) {
                                    view.setVisibility(visibilityBefore);
                                    hidden = false;
                                }
                            } else {
                                if (!hidden) {
                                    visibilityBefore = view.getVisibility();
                                    hidden = true;
                                }
                                view.setVisibility(View.INVISIBLE);
                            }
                        }
                    }, new IntentFilter(ACTION_TAKE_SCREENSHOT), ContextCompat.RECEIVER_EXPORTED);
                }
            });
        }
    }
}
