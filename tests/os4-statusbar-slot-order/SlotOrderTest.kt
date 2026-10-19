package fixture
import java.util.Collections
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.SwapWiFiAndMobileNetwork
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.Hooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.Param

class Slot(@JvmField val mName: String?, @JvmField val holder: Any = Any())
class IconList(names: List<String?>) {
    @JvmField val mSlots = names.map { Slot(it) }.toMutableList()
    @JvmField val mViewOnlySlots: List<Slot> = Collections.unmodifiableList(mSlots)
}
object Factory { @JvmStatic fun provideStatusBarIconList(context: Any): Any = context }

// config_statusBarIcons in supplied SystemUI APK SHA256 7da817a4...e7662a.
val otaNames = "handle network_speed mute micphone headset mikey privacy_mode nfc gps missed_call managed_profile second_space ime cast location stealth tty alarm_clock vpn ethernet handle_battery bluetooth bluetooth_handsfree_battery hotspot sound_box_group stereo sound_box_screen sound_box wireless_headset zen volume dist_compute camera glasses car tv pc pad phone hd airplane mobile demo_mobile no_sim wifi demo_wifi".split(' ')
val ordered = listOf("wifi", "demo_wifi", "mobile", "demo_mobile", "no_sim")
fun apply(list: IconList) { Hooks.factory!!(Param(list, Factory, arrayOf(list))) }
fun names(list: IconList) = list.mSlots.map { it.mName }

fun main() {
    DeviceHelper.System.version = 4f
    SwapWiFiAndMobileNetwork.init()
    checkNotNull(Hooks.factory) { "OS4 factory was not hooked; String[] constructor is absent from target DEX" }
    check(Hooks.legacy == null)
    var count = 0
    fun test(name: String, body: () -> Unit) { body(); count++; println("PASS $name") }
    test("actual OTA slots put Wi-Fi before mobile") {
        val list = IconList(otaNames); apply(list)
        check(names(list) == otaNames.take(41) + ordered)
    }
    test("all 46 original slot objects and holders survive") {
        val list = IconList(otaNames); val original = list.mSlots.toList(); apply(list)
        check(list.mSlots.size == original.size)
        check(original.all { old -> list.mSlots.count { it === old } == 1 })
    }
    test("read-only list view stays bound to original mutable list") {
        val list = IconList(otaNames); val mutable = list.mSlots; val view = list.mViewOnlySlots
        apply(list)
        check(list.mSlots === mutable && list.mViewOnlySlots === view)
        check(view.takeLast(5).map { it.mName } == ordered)
    }
    test("interleaved unknown slots retain positions and identity") {
        val list = IconList(listOf("mobile", "vendor", "demo_mobile", "no_sim", "wifi", "demo_wifi"))
        val unknown = list.mSlots[1]; apply(list)
        check(list.mSlots[1] === unknown)
        check(names(list).filter { it != "vendor" } == ordered)
    }
    test("incomplete signal groups are unchanged") {
        val list = IconList(otaNames.filter { it != "demo_wifi" }); val original = list.mSlots.toList()
        apply(list); check(list.mSlots == original)
    }
    test("duplicate signal names are unchanged") {
        val list = IconList(otaNames + "wifi"); val original = list.mSlots.toList()
        apply(list); check(list.mSlots == original)
    }
    test("nullable unrelated slot is preserved") {
        val list = IconList(listOf(null) + otaNames); val original = list.mSlots[0]
        apply(list); check(list.mSlots[0] === original)
    }
    test("repeated factory callback is idempotent") {
        val list = IconList(otaNames); apply(list); val once = list.mSlots.toList()
        apply(list); check(list.mSlots == once)
    }
    test("OS3 retains constructor interception") {
        Hooks.factory = null; DeviceHelper.System.version = 3f
        SwapWiFiAndMobileNetwork.init()
        check(Hooks.legacy != null && Hooks.factory == null)
    }
    println("OS4 slot order: $count passed")
}
