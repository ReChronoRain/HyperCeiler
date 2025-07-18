<div align="center">

<img src="\imgs\icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperCeiler

### Зробімо HyperOS знову чудовим!

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;[繁體中文](/README_zh-HK.md)&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en-US.md)&nbsp;&nbsp;|&nbsp;&nbsp;Українська&nbsp;&nbsp;|&nbsp;&nbsp;[Português (Brasil)](/README_pt-BR.md)

</div>

## Наразі підтримувані версії

HyperOS для Android 14-15

## Інструкції перед використанням

Будь ласка, увімкніть HyperCeiler у [LSPosed](https://github.com/LSPosed/LSPosed/releases), потім активуйте відповідну функцію в додатку HyperCeiler та перезапустіть область дії (потрібен root-доступ).

Цей модуль <b>не підтримує</b> `сторонні прошивки Xiaomi HyperOS зі значними модифікаціями`, `системне програмне забезпечення зі значними модифікаціями` та деякі `міжнародні прошивки Xiaomi HyperOS`.

Наразі HyperCeiler адаптовано для мобільних пристроїв з Xiaomi HyperOS 2.0.200 на базі Android 15. Покриття не є повним і вимагає постійного тестування та вдосконалення.

Перед тим, як надсилати відгук, будь ласка, перевірте, чи не існує вже такого ж відгуку, щоб не створювати зайвих клопотів розробникам. Витрачати однакову енергію на читання однакових відгуків — це марна трата часу.

Підтримка HyperCeiler припинена для прошивок MIUI на Android 11-13 та прошивки HyperOS 1.0 на Android 13.

> Для прошивок MIUI на Android 11-12, будь ласка, використовуйте [цю версію](https://github.com/ReChronoRain/Cemiuiler/releases/tag/1.3.130).
>
> Для прошивок MIUI на Android 13 та HyperOS 1.0, будь ласка, використовуйте [цю версію](https://github.com/Xposed-Modules-Repo/com.sevtinge.hyperceiler/releases/download/3866-2.5.156_20250118/HyperCeiler_2.5.156_20250118_3866_release_miui.apk).

## Підтримувані додатки

<details>
    <summary>Натисніть, щоб побачити підтримувані додатки</summary>

| Назва додатку                  | Назва пакета                       |
|:-------------------------------|:-----------------------------------|
| Системний фреймворк            | system                             |
| Інтерфейс системи              | com.android.systemui               |
| Системний лаунчер              | com.miui.home                      |
| Оновлення                      | com.android.updater                |
| Joyose                         | com.xiaomi.joyose                  |
| Mi Налаштування                | com.xiaomi.misettings              |
| Безпека                        | com.miui.securitycenter            |
| Шпалери                        | com.miui.miwallpaper               |
| Taplus                         | com.miui.contentextension          |
| Сповіщення на екрані           | com.xiaomi.barrage                 |
| Телефон                        | com.android.incallui               |
| Телефонні служби               | com.android.phone                  |
| Батарея та продуктивність      | com.miui.powerkeeper               |
| Повідомлення                   | com.android.mms                    |
| Знімок екрана                  | com.miui.screenshot                |
| Календар                       | com.android.calendar               |
| Браузер                        | com.android.browser                |
| Rueban (MTB)                   | com.xiaomi.mtb                     |
| Запис екрана                   | com.miui.screenrecorder            |
| Дозволи                        | com.lbe.security.miui              |
| Налаштування                   | com.android.settings               |
| Клавіатура Sogou для MIUI      | com.sohu.inputmethod.sogou.xiaomi  |
| Погода                         | com.miui.weather2                  |
| Трансляція                     | com.milink.service                 |
| Зовнішнє сховище               | com.android.externalstorage        |
| Always-on display              | com.miui.aod                       |
| Файловий менеджер              | com.android.fileexplorer           |
| Плагін системних сервісів      | com.miui.securityadd               |
| Завантаження                   | com.android.providers.downloads.ui |
| Завантаження                   | com.android.providers.downloads    |
| Галерея                        | com.miui.gallery                   |
| Mi Canvas                      | com.miui.creation                  |
| Xiaomi Share                   | com.miui.mishare.connectivity      |
| Редактор галереї               | com.miui.mediaeditor               |
| Xiaomi Cloud                   | com.miui.cloudservice              |
| Смарт-картки                   | com.miui.tsmclient                 |
| iFlytek IME для MIUI           | com.iflytek.inputmethod.miui       |
| Інсталятор пакетів             | com.miui.packageinstaller          |
| GetApps                        | com.xiaomi.market                  |
| Віджет-стрічка                 | com.miui.personalassistant         |
| Теми                           | com.android.thememanager           |
| Компоненти системної безпеки   | com.miui.guardprovider             |
| Камера                         | com.android.camera                 |
| Mi AI Translate                | com.xiaomi.aiasst.vision           |
| Сканер                         | com.xiaomi.scanner                 |
| Служба NFC                     | com.android.nfc                    |
| Навушники                      | com.miui.misound                   |
| Резервне копіювання            | com.miui.backup                    |
| Mi Mover                       | com.miui.huanji                    |
| MiTrustService                 | com.xiaomi.trustservice            |
| Переглядач HTML                | com.android.htmlviewer             |
| Керування викликами            | com.android.server.telecom         |
| Mi Remote                      | com.duokan.phone.remotecontroller  |
| Аналітика                      | com.miui.analytics                 |
| Спільнота Xiaomi               | com.xiaomi.vipaccount              |
| Голосовий тригер               | com.miui.voicetrigger              |
| Диктофон                       | com.android.soundrecorder          |
| LPA                            | com.miui.euicc                     |
| Служба активації Xiaomi SIM    | com.xiaomi.simactivate.service     |

</details>

> Усі перераховані вище області дії активні в LSPosed

## Група для спілкування та відгуків

Приєднуйтесь до нашої групи, щоб повідомляти про проблеми або отримувати останні оновлення.

[![badge_trguild]][trguild_url]
[![badge_tcguild]][tcguild_url]
[![badge_telegram]][telegram_url]

## Внесок у переклад

[![Crowdin](https://badges.crowdin.net/cemiuiler/localized.svg)](https://crowdin.com/project/cemiuiler)

Ви можете зробити свій внесок у переклад проєкту HyperCeiler [тут](https://crwd.in/cemiuiler).

> Примітка: Коли прогрес перекладу новою мовою досягає 90% або більше, він буде доданий до проєкту. Якщо прогрес перекладу доданої мови становить 30% або менше від вихідного тексту, її буде тимчасово видалено, доки прогрес не досягне 90% або більше.

## Подяки!

> <b>HyperCeiler</b> використовує деякі або всі з наступних проєктів. Дякуємо розробникам цих проєктів за їхню підтримку (у довільному порядку).

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
- [「HookTool」 by 焕晨HChen](https://github.com/HChenX/HookTool)
- [「NoStorageRestrict」 by DanGLES3](https://github.com/Xposed-Modules-Repo/com.github.dan.nostoragerestrict)
- [「PortalHook」 by Haocen2004](https://github.com/Haocen2004/PortalHook)
- [「PinningApp」 by 焕晨HChen](https://github.com/HChenX/PinningApp)
- [「RemoveMiuiSystemSelfProtection」 by gfbjngjibn](https://github.com/gfbjngjibn/RemoveMiuiSystemSelfProtection)
- [「SettingsDontThroughTheList」 by weixiansen574](https://github.com/weixiansen574/settingsdontthroughthelist)
- [「StarVoyager」 by hosizoraru](https://github.com/hosizoraru/StarVoyager)
- [「SuperLyric」 by 焕晨HChen](https://github.com/HChenX/SuperLyric)
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
