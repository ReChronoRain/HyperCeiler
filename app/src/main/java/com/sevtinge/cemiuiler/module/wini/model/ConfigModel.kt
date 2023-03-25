package com.sevtinge.cemiuiler.module.wini.model

// 总配置
data class ConfigModel(
    var versionCode: Int = 0,
    var BlurWhenShowShortcutMenu: MiuiHomeModel = MiuiHomeModel(),
    var BlurSystemUI: SystemUIModel = SystemUIModel(),
    var BlurPersonalAssistant: PersonalAssistantModel = PersonalAssistantModel(),
    var BlurSecurity: SecurityCenterModel = SecurityCenterModel(),
)