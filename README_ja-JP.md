<div align="center">

<img src="\imgs\icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperCeiler

### Make HyperOS Great Again!

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;[繁體中文](/README_zh-HK.md)&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en-US.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Українська](/README_uk_UA.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Português (Brasil)](/README_pt-BR.md)&nbsp;&nbsp;|&nbsp;&nbsp;日本語

</div>

## 現在対応しているバージョン

Android 15-16 の HyperOS

## 使用前の説明

[LSPosed](https://github.com/LSPosed/LSPosed/releases) で HyperCeiler を有効にしてから、HyperCeiler のアプリで該当する機能を有効にし、スコープを再起動してください（root 権限が必要です）。

このモジュールは、`大幅に改変されたサードパーティの Xiaomi HyperOS ROM`、`大幅に改変されたシステムソフトウェア`、および一部の `グローバル版の Xiaomi HyperOS ROM` には<b>対応していません</b>。

現在、HyperCeiler は Android 16 をベースにした Xiaomi HyperOS 3.0 のモバイル端末に対応しています。対応範囲はまだ十分とはいえず、継続的なテストと改善が必要です。

フィードバックを送る前に、同じフィードバックがすでにないかを確認し、開発者に手間をかけさせないようにしてください。同じフィードバックを読むために同じ労力を使うのは、時間の無駄です。

HyperCeiler は、Android 11-13 の MIUI ROM と、Android 13/14 の HyperOS 1.0/2.0 の ROM のメンテナンスを終了しました。

> Android 11-12 の MIUI ROM には、[こちらのバージョン](https://github.com/ReChronoRain/Cemiuiler/releases/tag/1.3.130)をお使いください。
>
> Android 13 の MIUI ROM と HyperOS 1.0 の ROM には、[こちらのバージョン](https://github.com/Xposed-Modules-Repo/com.sevtinge.hyperceiler/releases/download/3866-2.5.156_20250118/HyperCeiler_2.5.156_20250118_3866_release_miui.apk)をお使いください。
>
> Android 14 の HyperOS 1.0/2.0 はアーカイブされており、Android 14 への対応はバージョン 2.6.162 から削除されます。

## プロジェクトの計画

Xiaomi HyperOS 3 または 4 が、このプロジェクトの終着点になります。現時点で、Xiaomi は Xposed の機能の一部を選択的に自社の（ユーザーに優しくない）端末に取り込んでおり、システムの更新とともに、モジュールでできることは徐々に減ってきています。

私はまた、Xiaomi HyperOS が、root なしではまったく使い物にならないものから、ほぼ使えるもの（Xiaomi Pad を除く）へと、一歩ずつ進化していく様子を見てきました。

今後、このモジュールは Xiaomi HyperOS 3.0 以降のバージョンに幅広く対応していきますが、すべての機能が使えることは保証できません。Xiaomi HyperOS 2 は、新しいバージョンとの違いが大きいため、今後のメンテナンスの更新で対応を終了します。

## 対応しているアプリ

<details>
    <summary>クリックして対応しているアプリを表示</summary>

| アプリ名                          | パッケージ名                       |
|:----------------------------------|:-----------------------------------|
| システムフレームワーク                  | system                             |
| システム UI                         | com.android.systemui               |
| システムランチャー                   | com.miui.home                      |
| アップデーター                           | com.android.updater                |
| Joyose                            | com.xiaomi.joyose                  |
| Mi 設定                       | com.xiaomi.misettings              |
| セキュリティ                          | com.miui.securitycenter            |
| 壁紙                         | com.miui.miwallpaper               |
| Taplus                            | com.miui.contentextension          |
| 弾幕通知        | com.xiaomi.barrage                 |
| 電話                             | com.android.incallui               |
| 電話サービス                     | com.android.phone                  |
| バッテリーとパフォーマンス           | com.miui.powerkeeper               |
| メッセージ                         | com.android.mms                    |
| スクリーンショット                        | com.miui.screenshot                |
| カレンダー                          | com.android.calendar               |
| メモ                             | com.miui.notes                     |
| ブラウザ                           | com.android.browser                |
| Rueban (MTB)                      | com.xiaomi.mtb                     |
| スクリーンレコーダー                   | com.miui.screenrecorder            |
| 権限                       | com.lbe.security.miui              |
| 設定                          | com.android.settings               |
| Sogou Keyboard for MIUI           | com.sohu.inputmethod.sogou.xiaomi  |
| 天気                           | com.miui.weather2                  |
| キャスト                              | com.milink.service                 |
| 外部ストレージ                  | com.android.externalstorage        |
| 常時表示ディスプレイ                 | com.miui.aod                       |
| ファイルマネージャー                      | com.android.fileexplorer           |
| システムサービスのプラグイン             | com.miui.securityadd               |
| ダウンロード                         | com.android.providers.downloads.ui |
| ダウンロード                         | com.android.providers.downloads    |
| ギャラリー                           | com.miui.gallery                   |
| Mi Canvas                         | com.miui.creation                  |
| Xiaomi Share                      | com.miui.mishare.connectivity      |
| ギャラリーエディター                    | com.miui.mediaeditor               |
| Xiaomi Cloud                      | com.miui.cloudservice              |
| スマートカード                       | com.miui.tsmclient                 |
| Mi Wallet                         | com.mipay.wallet                   |
| iFlytek IME for MIUI              | com.iflytek.inputmethod.miui       |
| パッケージインストーラー                 | com.miui.packageinstaller          |
| GetApps                           | com.xiaomi.market                  |
| App vault                         | com.miui.personalassistant         |
| テーマ                            | com.android.thememanager           |
| システムセキュリティコンポーネント        | com.miui.guardprovider             |
| カメラ                            | com.android.camera                 |
| Mi AI 翻訳                   | com.xiaomi.aiasst.vision           |
| Xiaomi AI エンジン                  | com.xiaomi.aicr                    |
| スキャナー                           | com.xiaomi.scanner                 |
| NFC サービス                       | com.android.nfc                    |
| イヤホン                         | com.miui.misound                   |
| バックアップ                            | com.miui.backup                    |
| Mi Mover                          | com.miui.huanji                    |
| MiTrustService                    | com.xiaomi.trustservice            |
| HTML ビューアー                       | com.android.htmlviewer             |
| 通話の管理                      | com.android.server.telecom         |
| Mi Remote                         | com.duokan.phone.remotecontroller  |
| アナリティクス                         | com.miui.analytics                 |
| Xiaomi Community                  | com.xiaomi.vipaccount              |
| ボイストリガー                     | com.miui.voicetrigger              |
| ボイスレコーダー                    | com.android.soundrecorder          |
| LPA                               | com.miui.euicc                     |
| Xiaomi SIM アクティベーションサービス     | com.xiaomi.simactivate.service     |
| デバイス相互接続サービス | com.xiaomi.mirror                  |

</details>

> 上記のスコープはすべて LSPosed で有効になっています

## チャットとフィードバックのグループ

問題を報告したり、最新の情報を入手したりするには、グループに参加してください。

[![badge_trguild]][trguild_url]
[![badge_tcguild]][tcguild_url]
[![badge_telegram]][telegram_url]

## 翻訳への貢献

プロジェクトの翻訳の同期には Hosted Weblate を使っています。HyperCeiler プロジェクトの翻訳には、[こちら](https://hosted.weblate.org/engage/hyperceiler/)から貢献できます

<a href="https://hosted.weblate.org/engage/hyperceiler/">
<img src="https://hosted.weblate.org/widget/hyperceiler/multi-auto.svg" alt="Translation status" />
</a>

> 注：新しい言語の翻訳の進捗が 85% 以上になると、マージの手続きに入ります。追加された言語の翻訳の進捗が原文の 30% 以下の場合は、翻訳の進捗が 85% 以上になるまで一時的に削除されます。

## 謝辞

> <b>HyperCeiler</b> は、以下のプロジェクトの一部またはすべてを使用しています。これらのプロジェクトの開発者の皆さまのご支援に感謝します（順不同）。

- [「Accompanist」 by Android Open Source Project, Google Inc.](https://google.github.io/accompanist)
- [「Android」 by Android Open Source Project, Google Inc.](https://source.android.google.cn/license)
- [「AndroidHiddenApiBypass」 by LSPosed](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [「AndroidX」 by Android Open Source Project, Google Inc.](https://github.com/androidx/androidx)
- [「AutoSEffSwitch」 by HChenX](https://github.com/HChenX/AutoSEffSwitch)
- [「AntiAntiDefraud」 by MinaMichita](https://github.com/MinaMichita/AntiAntiDefraud)
- [「AutoNFC」 by GSWXXN](https://github.com/GSWXXN/AutoNFC)
- [「BypassSignCheck」 by Weverses](https://github.com/Weverses/BypassSignCheck)
- [「CorePatch」 by LSPosed](https://github.com/LSPosed/CorePatch)
- [「CustoMIUIzer」 by MonwF](https://github.com/MonwF/customiuizer)
- [「CustoMIUIzerMod」 by liyafe1997](https://github.com/liyafe1997/CustoMIUIzerMod)
- [「ClipboardList」 by HChenX](https://github.com/HChenX/ClipboardList)
- [「DexKit」 by LuckyPray](https://github.com/LuckyPray/DexKit)
- [「Disable app link verify」 by tehcneko](https://github.com/Xposed-Modules-Repo/io.github.tehcneko.applinkverify)
- [「DisableFlagSecure」 by LSPosed](https://github.com/LSPosed/DisableFlagSecure)
- [「DisableLogRequest」 by QueallyTech](https://github.com/QueallyTech/DisableLogRequest)
- [「EzXHelper」 by KyuubiRan](https://github.com/KyuubiRan/EzXHelper)
- [「EzHookTool」 by lingqiqi5211](https://github.com/lingqiqi5211/EzHookTool)
- [「FixMiuiMediaControlPanel」 by qqlittleice](https://github.com/qqlittleice/FixMiuiMediaControlPanel)
- [「FocusNotifLyric」 by wuyou-123](https://github.com/wuyou-123/FocusNotifLyric)
- [「ForegroundPin」 by HChenX](https://github.com/HChenX/ForegroundPin)
- [「FuckNFC」 by xiaowine](https://github.com/xiaowine/FuckNFC)
- [「Gson」 by Android Open Source Project, Google Inc.](https://github.com/google/gson)
- [「XiaomiHelper」 by HowieHChen](https://github.com/HowieHChen/XiaomiHelper)
- [「HideMiuiClipboardDialog」 by zerorooot](https://github.com/zerorooot/HideMiuiClipboardDialog)
- [「HyperFocusApi」 by ghhccghk](https://github.com/ghhccghk/HyperFocusApi)
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
- [「PinningApp」 by HChenX](https://github.com/HChenX/PinningApp)
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

[badge_trguild]: https://img.shields.io/badge/TG-Channel-4991D3?style=for-the-badge&logo=telegram

[tcguild_url]: https://t.me/cemiuiler_canary

[badge_tcguild]: https://img.shields.io/badge/TGCI-Channel-4991D3?style=for-the-badge&logo=telegram

[telegram_url]: https://t.me/cemiuiler

[badge_telegram]: https://img.shields.io/badge/dynamic/json?style=for-the-badge&color=2CA5E0&label=Telegram&logo=telegram&query=%24.data.totalSubs&url=https%3A%2F%2Fapi.spencerwoo.com%2Fsubstats%2F%3Fsource%3Dtelegram%26queryKey%3Dcemiuiler
