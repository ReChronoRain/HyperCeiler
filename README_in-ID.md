<div align="center">

<img src="\imgs\icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperCeiler

### Make HyperOS Great Again!

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;[繁體中文](/README_zh-HK.md)&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en-US.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Українська](/README_uk_UA.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Português (Brasil)](/README_pt-BR.md)&nbsp;&nbsp;|&nbsp;&nbsp;Bahasa Indonesia

</div>

## Versi yang Didukung Saat Ini

HyperOS untuk Android 15–16

## Petunjuk Sebelum Menggunakan

Silakan aktifkan HyperCeiler di [LSPosed](https://github.com/LSPosed/LSPosed/releases), kemudian aktifkan fitur yang diinginkan di dalam aplikasi HyperCeiler dan mulai ulang cakupan terkait (memerlukan akses Root).

Modul ini <b>tidak mendukung</b> `ROM kustom Xiaomi HyperOS pihak ketiga dengan banyak modifikasi`, `aplikasi sistem dengan modifikasi berlebihan`, serta beberapa `ROM Xiaomi HyperOS versi global/internasional tertentu`.

Saat ini HyperCeiler diadaptasi untuk perangkat ponsel pintar berbasis Xiaomi HyperOS 3.0 (Android 16). Cakupannya belum mencakup seluruh aspek secara sempurna sehingga memerlukan pengujian dan perbaikan secara berkelanjutan.

Sebelum mengirimkan umpan balik atau laporan kendala, harap periksa terlebih dahulu apakah laporan serupa sudah pernah diajukan guna menghindari duplikasi bagi para pengembang. Memeriksa laporan yang sama berulang kali sangat memakan waktu.

HyperCeiler telah menghentikan dukungan untuk ROM MIUI pada Android 11–13 serta ROM HyperOS 1.0/2.0 pada Android 13/14.

> Untuk ROM MIUI pada Android 11–12, silakan gunakan [versi ini](https://github.com/ReChronoRain/Cemiuiler/releases/tag/1.3.130).
>
> Untuk ROM MIUI pada Android 13 dan ROM HyperOS 1.0, silakan gunakan [versi ini](https://github.com/Xposed-Modules-Repo/com.sevtinge.hyperceiler/releases/download/3866-2.5.156_20250118/HyperCeiler_2.5.156_20250118_3866_release_miui.apk).
>
> HyperOS 1.0/2.0 pada Android 14 telah diarsipkan, dan dukungan untuk Android 14 akan dihapus mulai dari versi 2.6.162.

## Rencana Proyek

Xiaomi HyperOS 3 atau 4 akan menjadi titik akhir dari proyek ini. Saat ini, Xiaomi secara bertahap telah mengintegrasikan beberapa fitur Xposed ke dalam perangkat mereka sendiri, dan fungsionalitas modul ini perlahan berkurang seiring pembaruan sistem.

Kami menyaksikan bagaimana Xiaomi HyperOS berkembang selangkah demi selangkah, dari sistem yang awalnya hampir tidak dapat digunakan tanpa root hingga menjadi sistem yang cukup baik (kecuali pada lini Xiaomi Pad).

Ke depannya, modul ini akan diadaptasi secara luas untuk Xiaomi HyperOS 3.0 dan versi setelahnya, meskipun tidak ada jaminan bahwa semua fitur akan dapat berfungsi secara optimal. Pemeliharaan untuk Xiaomi HyperOS 2 akan dihentikan pada pembaruan mendatang karena banyaknya perbedaan mendasar dibandingkan versi yang lebih baru.

## Aplikasi yang Didukung

<details>
    <summary>Klik untuk melihat daftar aplikasi yang didukung</summary>

| Nama Aplikasi                     | Nama Paket                         |
|:----------------------------------|:-----------------------------------|
| Kerangka Sistem                   | system                             |
| Antarmuka Sistem                  | com.android.systemui               |
| Peluncur Sistem                   | com.miui.home                      |
| Pembaru Sistem                    | com.android.updater                |
| Joyose                            | com.xiaomi.joyose                  |
| Setelan Mi                        | com.xiaomi.misettings              |
| Keamanan                          | com.miui.securitycenter            |
| Wallpaper                         | com.miui.miwallpaper               |
| Taplus                            | com.miui.contentextension          |
| Notifikasi Barrage                | com.xiaomi.barrage                 |
| Telepon                           | com.android.incallui               |
| Layanan Telepon                   | com.android.phone                  |
| Daya & Performa                   | com.miui.powerkeeper               |
| Pesan                             | com.android.mms                    |
| Tangkapan Layar                   | com.miui.screenshot                |
| Kalender                          | com.android.calendar               |
| Catatan                           | com.miui.notes                     |
| Mi Browser                        | com.android.browser                |
| Alat Uji Modem MTB                | com.xiaomi.mtb                     |
| Perekam Layar                     | com.miui.screenrecorder            |
| Layanan Izin Keamanan             | com.lbe.security.miui              |
| Setelan                           | com.android.settings               |
| Keyboard Sogou untuk MIUI         | com.sohu.inputmethod.sogou.xiaomi  |
| Cuaca                             | com.miui.weather2                  |
| Interkoneksi Xiaomi MiLink        | com.milink.service                 |
| Penyimpanan Eksternal             | com.android.externalstorage        |
| Tampilan Selalu Aktif             | com.miui.aod                       |
| Kelola File                       | com.android.fileexplorer           |
| Plugin Layanan Sistem             | com.miui.securityadd               |
| Unduhan                           | com.android.providers.downloads.ui |
| Pengelola Unduhan                 | com.android.providers.downloads    |
| Galeri                            | com.miui.gallery                   |
| Mi Canvas                         | com.miui.creation                  |
| Xiaomi Share                      | com.miui.mishare.connectivity      |
| Editor Galeri                     | com.miui.mediaeditor               |
| Xiaomi Cloud                      | com.miui.cloudservice              |
| Kartu Pintar TSM                  | com.miui.tsmclient                 |
| Mi Wallet                         | com.mipay.wallet                   |
| Keyboard iFlytek untuk MIUI       | com.iflytek.inputmethod.miui       |
| Pemasang Paket                    | com.miui.packageinstaller          |
| GetApps                           | com.xiaomi.market                  |
| App Vault                         | com.miui.personalassistant         |
| Tema                              | com.android.thememanager           |
| Komponen Keamanan Sistem          | com.miui.guardprovider             |
| Kamera                            | com.android.camera                 |
| Terjemahan AI Xiaomi              | com.xiaomi.aiasst.vision           |
| Mesin AI Xiaomi                   | com.xiaomi.aicr                    |
| Pemindai                          | com.xiaomi.scanner                 |
| Layanan NFC                       | com.android.nfc                    |
| Efek Suara                        | com.miui.misound                   |
| Cadangan & Pulihkan               | com.miui.backup                    |
| Mi Mover                          | com.miui.huanji                    |
| MiTrustService                    | com.xiaomi.trustservice            |
| Penampil HTML                     | com.android.htmlviewer             |
| Kelola Panggilan                  | com.android.server.telecom         |
| Mi Remote                         | com.duokan.phone.remotecontroller  |
| Layanan Analitik                  | com.miui.analytics                 |
| Komunitas Xiaomi                  | com.xiaomi.vipaccount              |
| Pemicu Suara                      | com.miui.voicetrigger              |
| Perekam Suara                     | com.android.soundrecorder          |
| Layanan LPA eSIM                  | com.miui.euicc                     |
| Layanan Aktivasi SIM Xiaomi       | com.xiaomi.simactivate.service     |
| Layanan Interkoneksi Perangkat    | com.xiaomi.mirror                  |

</details>

> Semua cakupan di atas aktif melalui LSPosed

## Grup Obrolan & Umpan Balik

Bergabunglah dengan grup kami untuk melaporkan masalah atau mendapatkan pembaruan terkini.

[![badge_trguild]][trguild_url]
[![badge_tcguild]][tcguild_url]
[![badge_telegram]][telegram_url]

## Kontribusi Penerjemahan

Kami menggunakan Hosted Weblate untuk menyinkronkan terjemahan proyek. Anda dapat berkontribusi menerjemahkan proyek HyperCeiler [di sini](https://hosted.weblate.org/engage/hyperceiler/).

<a href="https://hosted.weblate.org/engage/hyperceiler/">
<img src="https://hosted.weblate.org/widget/hyperceiler/multi-auto.svg" alt="Status terjemahan" />
</a>

> Catatan: Ketika progres terjemahan bahasa baru mencapai 85% atau lebih, bahasa tersebut akan dimasukkan ke dalam proses penggabungan (merge). Jika progres terjemahan dari bahasa yang ditambahkan kurang dari atau sama dengan 30% dari teks sumber, terjemahan tersebut akan dihapus sementara hingga progresnya mencapai minimal 85%.

## Ucapan Terima Kasih

> <b>HyperCeiler</b> menggunakan sebagian atau seluruh komponen dari proyek-proyek berikut. Terima kasih kepada para pengembang atas dedikasi dan dukungannya (urutan tidak berdasarkan prioritas tertentu):

- [「Accompanist」 oleh Android Open Source Project, Google Inc.](https://google.github.io/accompanist)
- [「Android」 oleh Android Open Source Project, Google Inc.](https://source.android.google.cn/license)
- [「AndroidHiddenApiBypass」 oleh LSPosed](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [「AndroidX」 oleh Android Open Source Project, Google Inc.](https://github.com/androidx/androidx)
- [「AutoSEffSwitch」 oleh HChenX](https://github.com/HChenX/AutoSEffSwitch)
- [「AntiAntiDefraud」 oleh MinaMichita](https://github.com/MinaMichita/AntiAntiDefraud)
- [「AutoNFC」 oleh GSWXXN](https://github.com/GSWXXN/AutoNFC)
- [「BypassSignCheck」 oleh Weverses](https://github.com/Weverses/BypassSignCheck)
- [「CorePatch」 oleh LSPosed](https://github.com/LSPosed/CorePatch)
- [「CustoMIUIzer」 oleh MonwF](https://github.com/MonwF/customiuizer)
- [「CustoMIUIzerMod」 oleh liyafe1997](https://github.com/liyafe1997/CustoMIUIzerMod)
- [「ClipboardList」 oleh HChenX](https://github.com/HChenX/ClipboardList)
- [「DexKit」 oleh LuckyPray](https://github.com/LuckyPray/DexKit)
- [「Disable app link verify」 oleh tehcneko](https://github.com/Xposed-Modules-Repo/io.github.tehcneko.applinkverify)
- [「DisableFlagSecure」 oleh LSPosed](https://github.com/LSPosed/DisableFlagSecure)
- [「DisableLogRequest」 oleh QueallyTech](https://github.com/QueallyTech/DisableLogRequest)
- [「EzXHelper」 oleh KyuubiRan](https://github.com/KyuubiRan/EzXHelper)
- [「EzHookTool」 oleh lingqiqi5211](https://github.com/lingqiqi5211/EzHookTool)
- [「FixMiuiMediaControlPanel」 oleh qqlittleice](https://github.com/qqlittleice/FixMiuiMediaControlPanel)
- [「FocusNotifLyric」 oleh wuyou-123](https://github.com/wuyou-123/FocusNotifLyric)
- [「ForegroundPin」 oleh HChenX](https://github.com/HChenX/ForegroundPin)
- [「FuckNFC」 oleh xiaowine](https://github.com/xiaowine/FuckNFC)
- [「Gson」 oleh Android Open Source Project, Google Inc.](https://github.com/google/gson)
- [「XiaomiHelper」 oleh HowieHChen](https://github.com/HowieHChen/XiaomiHelper)
- [「HideMiuiClipboardDialog」 oleh zerorooot](https://github.com/zerorooot/HideMiuiClipboardDialog)
- [「HyperFocusApi」 oleh ghhccghk](https://github.com/ghhccghk/HyperFocusApi)
- [「HyperSmartCharge」 oleh buffcow](https://github.com/buffcow/HyperSmartCharge)
- [「HyperStar」 oleh YunZiA](https://github.com/YunZiA/HyperStar)
- [「Kotlin」 oleh JetBrains](https://github.com/JetBrains/kotlin)
- [「MaxFreeForm」 oleh YifePlayte](https://github.com/YifePlayte/MaxFreeForm)
- [「MediaControlOpt」 oleh YuKongA](https://github.com/YuKongA/MediaControlOpt)
- [「MiuiFeature」 oleh MoralNorm](https://github.com/moralnorm/miui_feature)
- [「MiuiHomeR」 oleh qqlittleice](https://github.com/qqlittleice/MiuiHome_R)
- [「MIUI IME Unlock」 oleh RC1844](https://github.com/RC1844/MIUI_IME_Unlock)
- [「MIUIQOL」 oleh chsbuffer](https://github.com/chsbuffer/MIUIQOL)
- [「MiuiXXL」 oleh Wine-Network](https://github.com/Wine-Network/Miui_XXL)
- [「HyperOSXXL」 oleh YuKongA](https://github.com/YuKongA/HyperOS_XXL)
- [「MIUI 通知修复」 oleh tehcneko](https://github.com/Xposed-Modules-Repo/io.github.tehcneko.miuinotificationfix)
- [「ModemPro」 oleh Weverse](https://github.com/Weverses/ModemPro)
- [「NoStorageRestrict」 oleh DanGLES3](https://github.com/Xposed-Modules-Repo/com.github.dan.nostoragerestrict)
- [「PortalHook」 oleh Haocen2004](https://github.com/Haocen2004/PortalHook)
- [「PinningApp」 oleh HChenX](https://github.com/HChenX/PinningApp)
- [「RemoveMiuiSystemSelfProtection」 oleh gfbjngjibn](https://github.com/gfbjngjibn/RemoveMiuiSystemSelfProtection)
- [「SettingsDontThroughTheList」 oleh weixiansen574](https://github.com/weixiansen574/settingsdontthroughthelist)
- [「StarVoyager」 oleh hosizoraru](https://github.com/hosizoraru/StarVoyager)
- [「SuperLyric」 oleh HChenX](https://github.com/HChenX/SuperLyric)
- [「WINI」 oleh ouhoukyo](https://github.com/ouhoukyo/WINI)
- [「WOMMO」 oleh YifePlayte](https://github.com/YifePlayte/WOMMO)
- [「Woobox For MIUI」 oleh hosizoraru](https://github.com/hosizoraru/WooBoxForMIUI)
- [「Woobox For MIUI」 oleh Simplicity-Team](https://github.com/Simplicity-Team/WooBoxForMIUI)
- [「Xposed」 oleh rovo89, Tungstwenty](https://github.com/rovo89/XposedBridge)
- [「XposedBridge」 oleh rovo89](https://github.com/rovo89/XposedBridge)
- [「.xlDownload」 oleh Kr328](https://github.com/Kr328/.xlDownload)

[trguild_url]: https://t.me/cemiuiler_release

[badge_trguild]: https://img.shields.io/badge/TG-Channel-4991D3?style=for-the-badge&logo=telegram

[tcguild_url]: https://t.me/cemiuiler_canary

[badge_tcguild]: https://img.shields.io/badge/TGCI-Channel-4991D3?style=for-the-badge&logo=telegram

[telegram_url]: https://t.me/cemiuiler

[badge_telegram]: https://img.shields.io/badge/dynamic/json?style=for-the-badge&color=2CA5E0&label=Telegram&logo=telegram&query=%24.data.totalSubs&url=https%3A%2F%2Fapi.spencerwoo.com%2Fsubstats%2F%3Fsource%3Dtelegram%26queryKey%3Dcemiuiler
