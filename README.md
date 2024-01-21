<div align="center">

<img width="" src="/imgs/icon.png" width=160 height=160 align="center">

# HyperCeiler

### Make HyperOS/MIUI Great Again!

简体中文&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en-US.md) |&nbsp;&nbsp;[Português (Brasil)](/README_pt-BR.md)

</div>

## 支持的版本

Android 11-14 的 MIUI 和 HyperOS

注：系统框架、系统界面作用域不支持 Android 11-12 的 MIUI

## 使用前说明

请在 [LSPosed](https://github.com/LSPosed/LSPosed/releases) 中启用 HyperCeiler， 然后在 HyperCeiler 应用内启用对应的功能，重启作用域 (需要 Root 权限)；

本模块<b>不支持</b> `修改较多的第三方 MIUI/Xiaomi HyperOS ROM`、`修改较多的系统软件`，以及`部分国际 MIUI/Xiaomi HyperOS ROM`；

目前 HyperCeiler 是基于 Android 14 的 Xiaomi HyperOS1.0 的手机端设备进行适配，覆盖不是很完整，需要不断测试和改进

HyperCeiler 已停止维护 Android 11-12 的 MIUI ROM，除系统框架、系统界面等核心作用域，原则上其他作用域可正常使用，核心作用域如需使用请停留[此版本](https://github.com/ReChronoRain/Cemiuiler/releases/tag/1.3.130)

## 作用域包含的应用

<details>
    <summary>点击展开折叠的内容</summary>

| 应用名                         | 包名                                |
|:------------------------------|:-----------------------------------|
| 系统框架                       | system                              |
| 系统界面                       | com.android.systemui                |
| 系统桌面                       | com.miui.home                       |
| 系统更新                       | com.android.updater                 |
| Joyose                        | com.xiaomi.joyose                   |
| 小米设置                       | com.xiaomi.misettings               |
| 安全服务 (手机管家、平板管家)    | com.miui.securitycenter              |
| 笔记                           | com.miui.notes                     |
| 壁纸                           | com.miui.miwallpaper               |
| 传送门                         | com.miui.contentextension          |
| 弹幕通知                       | com.xiaomi.barrage                  |
| 百度输入法小米版                | com.baidu.input_mi                  |
| 电话                           | com.android.incallui               |
| 电话服务                       | com.android.phone                   |
| 电量与性能                     | com.miui.powerkeeper                |
| 短信                           | com.android.mms                    |
| 截屏                           | com.miui.screenshot                |
| 垃圾清理                       | com.miui.cleanmaster                |
| 浏览器                         | com.android.browser                 |
| 鲁班（MTB）                    | com.xiaomi.mtb                      |
| 屏幕录制                       | com.miui.screenrecorder             |
| 权限管理服务                    | com.lbe.security.miui               |
| 设置                           | com.android.settings                |
| 搜狗输入法小米版                | com.sohu.inputmethod.sogou.xiaomi    |
| 天气                           | com.miui.weather2                   |
| 互联互通服务 (投屏)             | com.milink.service                   |
| 外部存储设备                    | com.android.externalstorage         |
| 息屏与锁屏编辑 (万象息屏)        | com.miui.aod                         |
| 文件管理                        | com.android.fileexplorer            |
| 系统服务组件                    | com.miui.securityadd                 |
| 下载管理                        | com.android.providers.downloads.ui  |
| 下载管理程序                    | com.android.providers.downloads      |
| 相册                           | com.miui.gallery                    |
| 小米创作                        | com.miui.creation                   |
| 小米互传                        | com.miui.mishare.connectivity       |
| 小米相册 - 编辑                  | com.miui.mediaeditor                |
| 小米云服务                       | com.miui.cloudservice               |
| 小米智能卡                       | com.miui.tsmclient                  |
| 讯飞输入法小米版                 | com.iflytek.inputmethod.miui         |
| 应用包管理组件                   | com.miui.packageinstaller            |
| 应用商店                        | com.xiaomi.market                   |
| 智能助理                        | com.miui.personalassistant          |
| 主题商店 (主题壁纸、壁纸与个性化)   | com.android.thememanager            |
| com.miui.rom                   | com.miui.rom                        |
| 系统安全组件                     | com.miui.guardprovider               |
| 时钟                            | com.android.deskclock                |
| 相机                            | com.android.camera                   |
| 小爱翻译                         | com.xiaomi.aiasst.vision            |
| 小爱建议                         | com.xiaomi.aireco                   |
| 小爱视觉                         | com.xiaomi.scanner                  |
| 小爱同学                         | com.miui.voiceassist                |
| 音乐                             | com.miui.player                    |
| 跨屏协同服务 (MIUI+ Beta 版)      | com.xiaomi.mirror                   |
| NetworkBoost                    | com.xiaomi.NetworkBoost            |
| NFC 服务                         | com.android.nfc                     |
| 音质音效                          | com.miui.misound                   |
| 备份                             | com.miui.backup                     |
| 小米换机                          | com.miui.huanji                     |
| MiTrustService                   | com.xiaomi.trustservice            |

</details>

> 与 LSPosed 中推荐的作用域相同

## 交流 & 反馈群组

加入我们所创建的群组以反馈问题或是了解最新情况。

[![badge_qgroup]][qgroup_url]
[![badge_qguild]][qguild_url]
[![badge_telegram]][telegram_url]

## 为 HyperCeiler 贡献翻译

[![Crowdin](https://badges.crowdin.net/cemiuiler/localized.svg)](https://crowdin.com/project/cemiuiler)

您可以在[这里](https://crwd.in/cemiuiler)为 HyperCeiler 项目贡献翻译。

## 感谢

> HyperCeiler 使用了以下开源项目的部分或全部内容，感谢这些项目的开发者提供的大力支持（排名顺序不分先后）。

- [「Accompanist」 by Android Open Source Project, Google Inc.](https://google.github.io/accompanist)
- [「Android」 by Android Open Source Project, Google Inc.](https://source.android.google.cn/license)
- [「AndroidHiddenApiBypass」 by LSPosed](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [「AndroidX」 by Android Open Source Project, Google Inc.](https://github.com/androidx/androidx)
- [「AntiAntiDefraud」 by MinaMichita](https://github.com/MinaMichita/AntiAntiDefraud)
- [「Auto NFC」 by GSWXXN](https://github.com/GSWXXN/AutoNFC)
- [「BypassSignCheck」 by Weverses](https://github.com/Weverses/BypassSignCheck)
- [「CorePatch」 by LSPosed](https://github.com/LSPosed/CorePatch)
- [「CustoMIUIzer」 by MonwF](https://github.com/MonwF/customiuizer)
- [「CustoMIUIzerMod」 by liyafe1997](https://github.com/liyafe1997/CustoMIUIzerMod)
- [「ClipboardList」 by 焕晨HChen](https://github.com/HChenX/ClipboardList)
- [「DexKit」 by LuckyPray](https://github.com/LuckyPray/DexKit)
- [「Disable app link verify」 by tehcneko](https://github.com/Xposed-Modules-Repo/io.github.tehcneko.applinkverify)
- [「DisableFlagSecure」 by LSPosed](https://github.com/LSPosed/DisableFlagSecure)
- [「DisableLogRequest」 by QueallyTech](https://github.com/QueallyTech/DisableLogRequest)
- [「EzXHelper」 by KyuubiRan](https://github.com/KyuubiRan/EzXHelper)
- [「FixMiuiMediaControlPanel」 by qqlittleice](https://github.com/qqlittleice/FixMiuiMediaControlPanel)
- [「FuckNFC」 by xiaowine](https://github.com/xiaowine/FuckNFC)
- [「ForegroundPin」 by 焕晨HChen](https://github.com/HChenX/ForegroundPin)
- [「Gson」 by Android Open Source Project, Google Inc.](https://github.com/google/gson)
- [「HideMiuiClipboardDialog」 by zerorooot](https://github.com/zerorooot/HideMiuiClipboardDialog)
- [「HyperSmartCharge」 by buffcow](https://github.com/buffcow/HyperSmartCharge)
- [「Kotlin」 by JetBrains](https://github.com/JetBrains/kotlin)
- [「MaxFreeForm」 by YifePlayte](https://github.com/YifePlayte/MaxFreeForm)
- [「Miui Feature」 by MoralNorm](https://github.com/moralnorm/miui_feature)
- [「MiuiHomeR」 by qqlittleice](https://github.com/qqlittleice/MiuiHome_R)
- [「MIUI IME Unlock」 by RC1844](https://github.com/RC1844/MIUI_IME_Unlock)
- [「MIUI QOL」 by chsbuffer](https://github.com/chsbuffer/MIUIQOL)
- [「Miui XXL」 by Wine-Network](https://github.com/Wine-Network/Miui_XXL)
- [「Miui XXL」 by YuKongA](https://github.com/YuKongA/Miui_XXL)
- [「MIUI 通知修复」 by tehcneko](https://github.com/Xposed-Modules-Repo/io.github.tehcneko.miuinotificationfix)
- [「ModemPro」 by Weverse](https://github.com/Weverses/ModemPro)
- [「NoStorageRestrict」 by DanGLES3](https://github.com/Xposed-Modules-Repo/com.github.dan.nostoragerestrict)
- [「Portal Hook」 by Haocen2004](https://github.com/Haocen2004/PortalHook)
- [「RemoveMiuiSystemSelfProtection」 by gfbjngjibn](https://github.com/gfbjngjibn/RemoveMiuiSystemSelfProtection)
- [「SettingsDontThroughTheList」 by weixiansen574](https://github.com/weixiansen574/settingsdontthroughthelist)
- [「StarVoyager」 by hosizoraru](https://github.com/hosizoraru/StarVoyager)
- [「WINI」 by ouhoukyo](https://github.com/ouhoukyo/WINI)
- [「WOMMO」 by YifePlayte](https://github.com/YifePlayte/WOMMO)
- [「Woobox For MIUI」 by hosizoraru](https://github.com/hosizoraru/WooBoxForMIUI)
- [「Woobox For MIUI」 by Simplicity-Team](https://github.com/Simplicity-Team/WooBoxForMIUI)
- [「Xposed」 by rovo89, Tungstwenty](https://github.com/rovo89/XposedBridge)
- [「XposedBridge」 by rovo89](https://github.com/rovo89/XposedBridge)
- [「.xlDownload」 by Kr328](https://github.com/Kr328/.xlDownload)

[qgroup_url]: https://jq.qq.com/?_wv=1027&k=TedCJq8V

[badge_qgroup]: https://img.shields.io/badge/QQ-群组-4DB8FF?style=for-the-badge&logo=tencentqq

[qguild_url]: https://pd.qq.com/s/35ooe0ssj

[badge_qguild]: https://img.shields.io/badge/QQ-频道-4991D3?style=for-the-badge&logo=tencentqq

[telegram_url]: https://t.me/cemiuiler

[badge_telegram]: https://img.shields.io/badge/dynamic/json?style=for-the-badge&color=2CA5E0&label=Telegram&logo=telegram&query=%24.data.totalSubs&url=https%3A%2F%2Fapi.spencerwoo.com%2Fsubstats%2F%3Fsource%3Dtelegram%26queryKey%3Dcemiuiler
