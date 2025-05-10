<div align="center">

<img src="\imgs\icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperCeiler

### Make HyperOS Great Again!

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;繁體中文&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en-US.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Português (Brasil)](/README_pt-BR.md)

</div>

## 當前支持的版本

Android 14-15 的 HyperOS

## 使用前說明

請在 [LSPosed](https://github.com/LSPosed/LSPosed/releases) 中啟用 HyperCeiler， 然後在 HyperCeiler 應用內啟用對應的功能，重啟作用域 (需要 Root 權限)；

本模組<b>不支持</b> `修改較多的第三方 Xiaomi HyperOS ROM`、`修改較多的系統軟件`，以及`部分國際 Xiaomi HyperOS ROM`；

目前 HyperCeiler 是基於 Android 15 的 Xiaomi HyperOS 2.0 的手機端設備進行適配，覆蓋範圍不是很完整，需要不斷測試和改進

提交反饋前請注意是否已有相同反饋，避免給開發者造成困擾。花相同精力看相同反饋是一件很浪費時間的事情

HyperCeiler 已停止維護 Android 11-13 的 MIUI ROM 和 Android 13 的 HyperOS 1.0 ROM

> Android 11-12 的 MIUI ROM 請使用 [此版本](https://github.com/ReChronoRain/Cemiuiler/releases/tag/1.3.130)
> 
> Android 13 的 MIUI ROM 和 HyperOS 1.0 ROM 請使用 [此版本](https://github.com/Xposed-Modules-Repo/com.sevtinge.hyperceiler/releases/download/3866-2.5.156_20250118/HyperCeiler_2.5.156_20250118_3866_release_miui.apk)

## 作用域包含的應用

<details>
    <summary>點擊展開折疊的內容</summary>

| 應用名                   | 包名                                 |
|:----------------------|:-----------------------------------|
| 系統框架                  | system                             |
| 系統 UI                  | com.android.systemui               |
| 系統桌面                  | com.miui.home                      |
| 系統更新                  | com.android.updater                |
| Joyose                | com.xiaomi.joyose                  |
| 小米設定                  | com.xiaomi.misettings              |
| 安全服務 (手機管家、平板管家)      | com.miui.securitycenter            |
| 筆記                    | com.miui.notes                     |
| 桌布                    | com.miui.miwallpaper               |
| 傳送門                   | com.miui.contentextension          |
| 彈幕通知                  | com.xiaomi.barrage                 |
| 電話                    | com.android.incallui               |
| 電話服務                  | com.android.phone                  |
| 電量和性能                 | com.miui.powerkeeper               |
| 短信                    | com.android.mms                    |
| 截屏                    | com.miui.screenshot                |
| 日曆                    | com.android.calendar               |
| 瀏覽器                   | com.android.browser                |
| 鲁班（MTB）               | com.xiaomi.mtb                     |
| 螢幕錄製                  | com.miui.screenrecorder            |
| 權限管理服務                | com.lbe.security.miui              |
| 設定                    | com.android.settings               |
| 搜狗輸入法小米版              | com.sohu.inputmethod.sogou.xiaomi  |
| 天氣                    | com.miui.weather2                  |
| 互聯互通服務           | com.milink.service                 |
| 外部儲存空間                | com.android.externalstorage        |
| 隨顥螢幕與鎖定螢幕編輯        | com.miui.aod                       |
| 檔案管理                  | com.android.fileexplorer           |
| 系統服務組件                | com.miui.securityadd               |
| 下載管理                  | com.android.providers.downloads.ui |
| 下載管理員                | com.android.providers.downloads    |
| 相簿                    | com.miui.gallery                   |
| 小米創作                  | com.miui.creation                  |
| 小米互傳                  | com.miui.mishare.connectivity      |
| 小米相簿-編輯             | com.miui.mediaeditor               |
| 小米雲服務                 | com.miui.cloudservice              |
| 小米智慧卡                 | com.miui.tsmclient                 |
| 訊飛輸入法小米版              | com.iflytek.inputmethod.miui       |
| 應用程式檔案管理元件               | com.miui.packageinstaller          |
| 應用商店                  | com.xiaomi.market                  |
| 智慧助理                  | com.miui.personalassistant         |
| 主題商店（個性主題、桌布與個人化）    | com.android.thememanager           |
| 系统安全元件                | com.miui.guardprovider             |
| 相機                    | com.android.camera                 |
| 小愛翻譯                  | com.xiaomi.aiasst.vision           |
| 掃一掃                  | com.xiaomi.scanner                 |
| 小愛同學                  | com.miui.voiceassist               |
| NFC 服務                | com.android.nfc                    |
| 音質音效                  | com.miui.misound                   |
| 備份                    | com.miui.backup                    |
| 小米換機                  | com.miui.huanji                    |
| MiTrustService        | com.xiaomi.trustservice            |
| HTML 檢視器                | com.android.htmlviewer             |
| 通話管理               | com.android.server.telecom         |
| 萬能遙控                  | com.duokan.phone.remotecontroller  |
| Analytics                  | com.miui.analytics                 |
| 小米社區                 | com.xiaomi.vipaccount              |
| 語音喚醒              | com.miui.voicetrigger              |
| 錄音機                      | com.android.soundrecorder          |
| LPA                        | com.miui.euicc                     |
| 小米SIM卡啟動服務             | com.xiaomi.simactivate.service |

</details>

> 與 LSPosed 中推薦的作用域相同

## 交流 & 反饋群組

加入我們所創建的群組以反饋問題或是了解最新情況。

[![badge_trguild]][trguild_url]
[![badge_tcguild]][tcguild_url]
[![badge_telegram]][telegram_url]

## 為 HyperCeiler 貢獻翻譯

[![Crowdin](https://badges.crowdin.net/cemiuiler/localized.svg)](https://crowdin.com/project/cemiuiler)

您可以在[這裡](https://crwd.in/cemiuiler)為 HyperCeiler 項目貢獻翻譯。

> 註：當新語言翻譯進度大於或等於 90% 時，將會進入合併流程，如果已添加的語言，翻譯進度小於或等於原始文本的 30%，將會暫時被移除，直到重新滿足翻譯進度大於或等於 90%

## 感謝

> HyperCeiler 使用了以下開源專案的部分或內容，感謝這些專案的開發者提供的大力支持（排名順序不分先後）。

- [「Accompanist」 by Android Open Source Project, Google Inc.](https://google.github.io/accompanist)
- [「Android」 by Android Open Source Project, Google Inc.](https://source.android.google.cn/license)
- [「AndroidHiddenApiBypass」 by LSPosed](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [「AndroidX」 by Android Open Source Project, Google Inc.](https://github.com/androidx/androidx)
- [「AutoSEffSwitch」 by 焕晨HChen](https://github.com/HChenX/AutoSEffSwitch)
- [「AntiAntiDefraud」 by MinaMichita](https://github.com/MinaMichita/AntiAntiDefraud)
- [「AutoNFC」 by GSWXXN](https://github.com/GSWXXN/AutoNFC)
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
- [「FocusNotifLyric」 by wuyou-123](https://github.com/wuyou-123/FocusNotifLyric)
- [「ForegroundPin」 by 焕晨HChen](https://github.com/HChenX/ForegroundPin)
- [「FuckNFC」 by xiaowine](https://github.com/xiaowine/FuckNFC)
- [「Gson」 by Android Open Source Project, Google Inc.](https://github.com/google/gson)
- [「XiaomiHelper」 by HowieHChen](https://github.com/HowieHChen/XiaomiHelper)
- [「HideMiuiClipboardDialog」 by zerorooot](https://github.com/zerorooot/HideMiuiClipboardDialog)
- [「HyperSmartCharge」 by buffcow](https://github.com/buffcow/HyperSmartCharge)
- [「HyperStar」 by YunZiA](https://github.com/YunZiA/HyperStar)
- [「Kotlin」 by JetBrains](https://github.com/JetBrains/kotlin)
- [「MaxFreeForm」 by YifePlayte](https://github.com/YifePlayte/MaxFreeForm)
- [「MediaControlOpt」 by YuKongA](https://github.com/YuKongA/MediaControlOpt)
- [「MiuiFeature」 by MoralNorm](https://github.com/moralnorm/miui_feature)
- [「MiuiHomeR」 by qqlittleice](https://github.com/qqlittleice/MiuiHome_R)
- [「MIUI IME Unlock」 by RC1844](https://github.com/RC1844/MIUI_IME_Unlock)
- [「MIUIQOL」 by chsbuffer](https://github.com/chsbuffer/MIUIQOL)
- [「MiuiXXL」 by Wine-Network](https://github.com/Wine-Network/Miui_XXL)
- [「HyperOSXXL」 by YuKongA](https://github.com/YuKongA/HyperOS_XXL)
- [「MIUI 通知修复」 by tehcneko](https://github.com/Xposed-Modules-Repo/io.github.tehcneko.miuinotificationfix)
- [「ModemPro」 by Weverse](https://github.com/Weverses/ModemPro)
- [「NoStorageRestrict」 by DanGLES3](https://github.com/Xposed-Modules-Repo/com.github.dan.nostoragerestrict)
- [「PortalHook」 by Haocen2004](https://github.com/Haocen2004/PortalHook)
- [「PinningApp」 by 焕晨HChen](https://github.com/HChenX/PinningApp)
- [「RemoveMiuiSystemSelfProtection」 by gfbjngjibn](https://github.com/gfbjngjibn/RemoveMiuiSystemSelfProtection)
- [「SettingsDontThroughTheList」 by weixiansen574](https://github.com/weixiansen574/settingsdontthroughthelist)
- [「StarVoyager」 by hosizoraru](https://github.com/hosizoraru/StarVoyager)
- [「SuperLyric」 by HChenX](https://github.com/HChenX/SuperLyric)
- [「WINI」 by ouhoukyo](https://github.com/ouhoukyo/WINI)
- [「WOMMO」 by YifePlayte](https://github.com/YifePlayte/WOMMO)
- [「Woobox For MIUI」 by hosizoraru](https://github.com/hosizoraru/WooBoxForMIUI)
- [「Woobox For MIUI」 by Simplicity-Team](https://github.com/Simplicity-Team/WooBoxForMIUI)
- [「Xposed」 by rovo89, Tungstwenty](https://github.com/rovo89/XposedBridge)
- [「XposedBridge」 by rovo89](https://github.com/rovo89/XposedBridge)
- [「.xlDownload」 by Kr328](https://github.com/Kr328/.xlDownload)

[trguild_url]: https://t.me/cemiuiler_release

[badge_trguild]: https://img.shields.io/badge/TG-頻道-4991D3?style=for-the-badge&logo=telegram

[tcguild_url]: https://t.me/cemiuiler_canary

[badge_tcguild]: https://img.shields.io/badge/TGCI-頻道-4991D3?style=for-the-badge&logo=telegram

[telegram_url]: https://t.me/cemiuiler

[badge_telegram]: https://img.shields.io/badge/dynamic/json?style=for-the-badge&color=2CA5E0&label=Telegram&logo=telegram&query=%24.data.totalSubs&url=https%3A%2F%2Fapi.spencerwoo.com%2Fsubstats%2F%3Fsource%3Dtelegram%26queryKey%3Dcemiuiler
