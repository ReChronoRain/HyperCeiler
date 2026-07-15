# HyperCeiler 代码代理说明

## 适用范围

- 本文件适用于仓库根目录及全部子目录；更深层的 `AGENTS.md` 可补充或覆盖对应目录规则。
- 仓库可能有多人同时修改。开始和结束任务都检查工作区，只改任务要求的文件，不还原、覆盖或整理他人的未提交改动。
- 只做满足需求所需的最小改动；不得顺带重构、升级依赖、批量改格式或扩大兼容范围。

## 沟通与执行

- 回报使用与用户相同的语言，专有名词保留英文；表达直接、清楚，不使用工程汇报腔。
- 最终回报只写结论、实际改动、原因和验证结果，不描述推进过程，不使用“先……再……”式叙述。
- 有多个方案时列出各自优缺点，明确推荐项及原因。
- 开始任务前定义完成标准；交付前按标准检查，发现问题应修好再交付。
- 可拆成互不依赖任务时可并行分工；每个执行者必须有清楚且不重叠的职责和文件范围。
- 除非用户明确要求，不运行编译、测试、安装、联网检索或耗时的全仓扫描；审查任务只在指定范围做必要的静态检查。

## 项目与构建基线

HyperCeiler 是面向 Xiaomi HyperOS 的 Android 设置应用与 LSPosed/libxposed 模块。设置应用负责功能入口、偏好、引导和日志；Hook 代码运行在系统或目标应用进程中。

- Gradle Kotlin DSL、Version Catalog、类型安全项目访问器。
- Android 模块使用 `compileSdk 37`；应用为 `minSdk 35`、`targetSdk 37`，只打包 `arm64-v8a`。
- Java/Kotlin 工具链为 21。
- 当前 Xposed 入口使用 libxposed API 102；元数据声明 `minApiVersion=101`、`targetApiVersion=102`。
- README 的主线适配范围为 Android 15–16、Xiaomi HyperOS 3.0。`compileSdk 37` 不代表 Android 17 已完整支持。
- 项目依赖 Root、LSPosed 作用域和目标进程重启；APK 能生成不等于 Hook 已验证。
- `docs/` 当前为空，主要说明来自 `README`、构建文件和源码。

## 模块职责

| 路径 | 职责 |
| --- | --- |
| `app/` | 最终应用入口与 APK 打包；首页、关于、搜索、日志、设置入口、Room 日志数据和 Manifest。 |
| `library/common/` | 多模块共用基础类、日志、工具、控件、MIUIX 与共享偏好能力；不得放具体目标包功能。 |
| `library/core/` | 设置界面主体；Dashboard、目标应用设置页、偏好 XML、功能提示、模型和界面组件。 |
| `library/provision/` | 首次启动、条款、权限、基础设置、完成页、状态流程、服务与 AIDL。 |
| `library/libhook/` | 当前 Hook 运行层；Xposed 入口、包分发、规则、DexKit、资源注入、远程偏好、崩溃保护和 Xposed 元数据。 |
| `library/processor/` | 定义 `@HookBase` 并生成 Hook 索引 `DataBase`。生成结果不得手工修改。 |
| `library/hidden-api/` | Android/MIUI 隐藏接口的编译期桩；只提供声明，不放真实实现。 |
| `library/hook/` | 已被 `settings.gradle.kts` 排除的旧 Hook 模块；除非任务明确要求，不修改、不依赖、不重新启用。 |

主要源码位置：

- 应用外壳：`app/src/main/java/com/sevtinge/hyperceiler/`
- 设置逻辑与页面：`library/core/src/main/java/com/sevtinge/hyperceiler/`
- `library/core/.../hooker/` 虽名为 `hooker`，实际仍是设置页面；目标进程中的 Hook 实现位于 `library/libhook/`。
- 功能偏好页：`library/core/src/main/res/xml/`
- 目标包分发：`library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/`
- Hook 基类：`library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/`
- Hook 规则：`library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/`
- Xposed 元数据：`library/libhook/src/main/resources/META-INF/xposed/`

## 依赖方向

```text
app
├─ core
│  ├─ common
│  └─ provision
│     ├─ common
│     └─ libhook
│        ├─ common
│        ├─ processor
│        └─ hidden-api（仅编译期）
└─ common
```

- 上层可调用下层；`common`、`libhook`、`provision` 不得反向依赖 `app` 或 `core`。
- `core` 负责“显示和配置什么”，`libhook` 负责“目标进程中如何生效”。同一功能常需两侧共用偏好键，但 Hook 代码不得放进 `core`。
- `provision` 和 `core` 使用 `api` 暴露部分下层模块。新增代码仍应依职责声明直接依赖，不利用传递依赖掩盖分层问题。
- `hidden-api` 保持 `compileOnlyApi`；Xposed 和系统运行时提供的 API 保持 `compileOnly` 语义。
- 新增跨模块工具前先检查现有 `common`、`core`、`libhook` 是否已有可复用实现。

## 写法约定

### 通用与 Java/Kotlin

- 遵循 `.editorconfig`：UTF-8、LF、4 空格、删除行尾空格；修改时保持相邻代码的命名、导入和排版，不做无关全文件格式化。
- 新建生产源码或 XML 时参考同目录文件保留现有 AGPL 版权头。
- Java 使用 Java 21；类名 PascalCase，方法和变量 camelCase，常量 `UPPER_SNAKE_CASE`。
- Kotlin 使用惯用空安全和表达式写法，避免不必要的 `!!`、分号和 Java 式样板；`MultiDollarInterpolation` 只可假定在已配置的 `libhook` 中可用。
- 不因小改动进行 Java/Kotlin 互相改写。
- 优先复用 `BaseHook`、`BaseLoad`、`PrefsBridge`、`ResourcesTool`、现有日志和版本工具，不另建平行框架。
- 不用空的广泛异常捕获掩盖兼容失败；可降级失败应记录足以定位目标包、版本和规则的信息。
- 保留现有 `SystemUI`、`SystemFramework`、`Home` 等目录大小写，不做仅大小写变化的重命名。

### XML 与资源

- 资源名使用小写 snake_case；用户可见文字必须放入 `strings.xml`，不得硬编码在布局或源码中。
- 默认 `values/` 为基础英文，`values-zh-rCN/` 为简体中文；不要批量覆盖其他翻译。语言目录变化时同步核对 `app/src/main/res/xml/locales_config.xml`。
- 偏好键使用 `prefs_key_` 前缀和 snake_case。XML 通常写完整键；Hook 侧可通过 `PrefsBridge` 使用去掉前缀的名称。两侧必须映射到同一键。
- 已发布偏好键不得随意重命名；确需修改时同步处理界面、Hook 读取、默认值、观察者和旧数据迁移。
- 设置页的 `app:myLocation`、`app:quick_restart`、`app:hot_reload_preferred` 会被 Dashboard 使用，复制或移动页面时必须核对。
- 应用固定资源包 ID 为 `0x36`。除非任务专门处理资源 ID，不得修改相关 `aapt` 参数或打包规则。
- 注入目标应用的资源放在 `library/libhook/`，并使用现有 `ResourcesTool` 与资源 Hook 生命周期。

### Gradle Kotlin DSL

- 插件和依赖版本放在 `gradle/libs.versions.toml`，模块使用 `libs.*`；项目模块使用 `projects.library.*`。
- 仓库启用 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，不得在模块中新增 `repositories`。
- 按公开类型是否需要传递选择 `api` 或 `implementation`；不得为快速消除错误一律改成 `api`。
- 保持 Java/Kotlin 21、SDK、ABI、AIDL、BuildConfig 及 `release`/`beta`/`canary` 变体匹配。
- GitHub Packages 凭据来自 `GIT_ACTOR`、`GIT_TOKEN` 或本地且被忽略的 `signing.properties`；不得提交账号、令牌、签名文件或本机路径。
- 不顺带修改版本号、APK 名称、签名、混淆、SDK、固定资源包 ID或依赖版本。

## Android、Xposed、资源与兼容性约束

- 当前入口为 `com.sevtinge.hyperceiler.libhook.base.XposedInitEntry`，由 `META-INF/xposed/java_init.list` 声明；不要改回旧版 `xposed_init`。
- `libhook/app/` 的分发类使用 `@HookBase` 声明 `targetPackage`、SDK、HyperOS 和设备条件。注解处理器生成 `DataBase`，不要编辑 `build/generated/`。
- `deviceType` 约定：`0` 通用、`1` 平板、`2` 手机。版本与设备判断优先使用现有 `DeviceHelper.System` 和目标应用版本工具。
- 实际规则继承 `BaseHook`，包级装配继承 `BaseLoad`；不得绕过统一加载、日志、偏好判断和失败隔离流程。
- 目标类必须使用目标进程 ClassLoader。不得假定应用、`system_server`、SystemUI 和普通目标进程共享 Context、资源或类加载器。
- 使用 DexKit 的规则在 `initDexKit()` 完成成员解析，在 `init()` 安装 Hook；不得在高频回调中重复扫描 Dex。
- 必需成员使用现有 `requiredMember`/`requiredMemberList`；允许缺失的成员应安全降级，不能拖垮同包其他规则。
- Hook 侧通过 `PrefsBridge` 读取远程偏好，视为只读；写入从应用进程使用现有 `putByApp`、`removeByApp`、`clearAllByApp` 或数据存储入口完成。
- `module.prop`、`java_init.list`、`scope.list` 是打包必需文件，不得被排除、改名或移动。
- `scope.list` 只加入实际需要注入的包。新增目标包时同步核对分发类、设置入口、README 作用域说明和重启目标。
- Hook 运行在系统或其他应用进程中：避免阻塞、高频 I/O、重复反射、无限递归、长期持有 Activity/Context 和无条件高频日志。
- 功能关闭时应保持宿主原行为；兼容失败应只禁用当前规则，不导致宿主启动失败或系统进程循环重启。
- 不绕过安全模式、模块完整性检查、Safe Crash、作用域检查或现有失败隔离。
- 不扩大 Android 权限、导出组件、Provider 访问范围或 LSPosed 作用域，除非需求明确要求并完成安全检查。
- README 已说明不保证重度修改 ROM、被改版系统应用和部分国际版 ROM。不得把单一设备结果宣称为全机型支持。
- Android 11–14 及旧 MIUI/HyperOS 分支不是当前主线重点；未明确要求时不新增旧系统兼容分支。

## 修改边界与禁止事项

- 只修改任务直接需要的文件；发现相邻问题可以说明，不得顺带修复。
- 不使用 `git reset --hard`、`git checkout -- .`、`git clean` 或等价命令清理共享工作区。
- 不修改或提交 `build/`、`.gradle/`、`.idea/`、`local.properties`、签名文件、生成索引和其他本机或生成内容。
- 不批量移动包、格式化全仓、重命名资源或偏好键，也不把功能修改混入无关重构。
- 不直接修改或重新启用 `library/hook/`。
- 不提交密钥、令牌、账号、隐私日志、反编译目标文件或受限制的二进制内容。
- 不硬编码仅在单一 ROM 成立的类名、资源数值 ID 或版本结论；没有稳定匹配条件时必须限制适用范围并安全降级。
- 不把编译成功写成真机功能验证通过，不报告未实际执行的检查。
- 用户明确禁止编译、安装或联网时，必须遵守，不得以验证为由执行。

## 验证要求

仓库当前没有 `src/test` 或 `src/androidTest` 测试集，持续集成主要执行 APK 组装。验证必须与改动风险匹配：

| 改动类型 | 最低检查 |
| --- | --- |
| 所有改动 | `git status --short`；检查目标文件差异；`git diff --check`；确认无他人改动被覆盖。 |
| Java/Kotlin | 核对导入、可空性、调用方、模块依赖和兼容分支；只有用户明确要求编译时，才运行覆盖改动的最小 Gradle Wrapper 任务。 |
| 跨模块或最终集成 | 用户明确要求编译时以 `:app:assembleDebug` 为基础；变体专属改动再检查对应 Beta/Canary/Release。 |
| XML/资源 | 核对资源引用、偏好键、默认值、页面入口、深浅色、英文/简体中文及涉及的手机/平板布局。 |
| Hook | 核对分发类、规则、作用域、版本/设备条件、必需与可选成员、关闭状态和失败降级。 |
| 真机 Hook | LSPosed 激活与作用域正确；开关开/关符合预期；按要求热重载或重启；宿主无崩溃、循环重启和持续异常日志。 |
| 仅文档 | 不运行 Gradle；检查内容与仓库结构一致、UTF-8/LF、链接或路径有效、无尾随空格。 |

- 未获用户明确要求时，不运行任何 Gradle 编译、测试、打包或安装命令；明确禁止编译的任务不得以验证为由绕过。
- Gradle 解析 GitHub Packages 可能需要 `GIT_ACTOR`、`GIT_TOKEN` 或本地凭据。缺少条件时准确说明未执行或失败原因，不得伪造结果。
- 无法覆盖的 Android、HyperOS、目标应用版本或设备类型必须列为未验证，不得扩大支持声明。

## 提交前检查

- [ ] `git status --short` 只包含任务文件和已识别的他人改动。
- [ ] 模块职责与依赖方向正确，未使用旧 `library/hook`。
- [ ] Java/Kotlin/XML/Gradle 写法与相邻代码一致，无无关格式变化。
- [ ] 偏好键、默认值、界面显示、Hook 读取和重启/热重载要求一致。
- [ ] Hook 的目标包、SDK、HyperOS、手机/平板条件及失败降级明确。
- [ ] 新目标包仅在必要时加入 `scope.list`，设置入口和说明同步核对。
- [ ] 用户可见文字使用资源，基础语言完整，未覆盖无关翻译。
- [ ] 未意外修改资源包 ID、应用 ID、Provider authority、权限、签名、版本或依赖。
- [ ] 无生成文件、构建产物、本机配置、凭据、签名材料或隐私日志。
- [ ] `git diff --check` 无当前任务引入的问题。
- [ ] 只报告实际执行的验证；未执行的编译或真机检查已说明原因。
