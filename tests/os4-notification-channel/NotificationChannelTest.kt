package fixture

import android.content.Context
import android.provider.Settings
import android.service.notification.Notification
import android.service.notification.StatusBarNotification
import android.widget.ImageView
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.RedirectToNotificationChannelSetting
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.Hooks

object Scenario { var legacy=false; val queue=CommandQueue(); val modal=ModalController() }
class CommandQueue { var collapses=0; fun animateCollapsePanels(flags: Int, force: Boolean) { collapses++ } }
class ModalController { var exits=0; fun animExitModal(delay: Long, animate: Boolean, reason: String, force: Boolean) { exits++ } }
class InfoItem(@JvmField val mIcon: ImageView?)
open class Os4Menu(@JvmField val mContext: Context, @JvmField val mSbn: StatusBarNotification?, @JvmField val mInfoItem: InfoItem?) {
    fun createMenuViews(reset: Boolean) {}
}
class LegacyMenu(context: Context, sbn: StatusBarNotification?, item: InfoItem?) : Os4Menu(context,sbn,item) { fun onClickInfoItem() {} }
object SettingsHelper { fun startAppNotificationSettings(context: Context, pkg: String, channel: String, uid: Int, category: String) {} }

fun main(args: Array<String>) {
    Scenario.legacy=args.single()=="legacy"
    val context=Context(); EzXposed.initAppContext(context)
    val notification=StatusBarNotification("example.source", Notification("source-channel", "conversation-7"), 10234)
    fun menu(sbn: StatusBarNotification? = notification, item: InfoItem? = InfoItem(ImageView())): Os4Menu =
        if (Scenario.legacy) LegacyMenu(context,sbn,item) else Os4Menu(context,sbn,item)
    fun helper(): HookParam {
        val p=HookParam(SettingsHelper,arrayOf(context,notification.packageName,notification.notification.channelId,notification.uid,"category"))
        Hooks.hook("startAppNotificationSettings").beforeCallback!!.invoke(p)
        return p
    }
    fun verifyIntent() {
        val intent=context.started.last()
        check(intent.targetPackage=="com.android.settings")
        check(intent.targetClass=="com.android.settings.SubSettings")
        check(intent.extras[":android:show_fragment"]=="com.android.settings.notification.ChannelNotificationSettings")
        check(intent.extras[Settings.EXTRA_APP_PACKAGE]==notification.packageName)
        check(intent.extras[Settings.EXTRA_CHANNEL_ID]==notification.notification.channelId)
        check(intent.extras[Settings.EXTRA_CONVERSATION_ID]==notification.notification.shortcutId)
        check(intent.extras["app_uid"]==notification.uid)
    }
    RedirectToNotificationChannelSetting.init()
    var passed=0;var failed=0
    fun test(name: String, body: () -> Unit) {
        runCatching(body).onSuccess { passed++; println("PASS $name") }.onFailure { failed++; println("FAIL $name: $it") }
    }
    test("ROM menu method availability registers direct hook and optional legacy hook") {
        check(Hooks.registered.keys.any { it.name=="createMenuViews" && it.parameterTypes.toList()==listOf(Boolean::class.javaPrimitiveType) })
        check(Hooks.registered.keys.any { it.name=="onClickInfoItem" }==Scenario.legacy)
    }
    test("uncaptured helper call preserves original result and launches no activity") {
        val before=context.started.size;val p=helper()
        check(!p.intercepted && p.result=="original-result")
        check(context.started.size==before)
    }
    test("direct menu icon still opens this notification's exact channel and collapses panel") {
        val row=menu();val before=context.started.size;val collapse=Scenario.queue.collapses
        Hooks.hook("createMenuViews").afterCallback!!.invoke(HookParam(row,arrayOf(false)))
        row.mInfoItem!!.mIcon!!.click()
        check(context.started.size==before+1);verifyIntent()
        check(Scenario.queue.collapses==collapse+1)
    }
    test("direct icon click leaves unrelated helper launches untouched") {
        val before=context.started.size;val p=helper()
        check(!p.intercepted && context.started.size==before)
    }
    test("menu without notification installs no redirect listener") {
        val row=menu(sbn=null)
        Hooks.hook("createMenuViews").afterCallback!!.invoke(HookParam(row,arrayOf(false)))
        check(row.mInfoItem!!.mIcon!!.listener==null)
    }
    test("menu without info item preserves normal fallthrough") {
        val row=menu(item=null);val before=context.started.size
        Hooks.hook("createMenuViews").afterCallback!!.invoke(HookParam(row,arrayOf(false)))
        check(context.started.size==before)
    }
    if (Scenario.legacy) {
        val click=Hooks.hook("onClickInfoItem")
        test("legacy captured notification redirects helper and suppresses only that call") {
            val p=HookParam(menu());click.beforeCallback!!.invoke(p)
            try {
                val before=context.started.size;val result=helper()
                check(result.intercepted && result.result==null)
                check(context.started.size==before+1);verifyIntent()
            } finally { click.afterCallback!!.invoke(p) }
        }
        test("legacy after callback clears capture before next unrelated helper") {
            val before=context.started.size;val result=helper()
            check(!result.intercepted && context.started.size==before)
        }
        test("legacy click without notification cannot intercept helper") {
            val p=HookParam(menu(sbn=null));click.beforeCallback!!.invoke(p)
            try { check(!helper().intercepted) } finally { click.afterCallback!!.invoke(p) }
        }
    }
    println("RESULT ${args.single()}: $passed passed, $failed failed")
    check(failed==0) { "$failed callback regression checks failed" }
}
