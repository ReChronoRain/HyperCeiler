# 桌面智能诊断（第一版）

## 范围与证据语义

当前只收集、分析系统桌面 `com.miui.home`（Java 同包子进程亦允许）的功能。
模型可扩展，但其他应用的 Java 观察器保持原回调对象，不增加回调包装或输出；
页面不会展示 SystemUI、system_server 或第三方应用的功能诊断。
已有通用日志查看与导出继续保留。

智能诊断以当前进程的执行记录判定成功，`EXECUTED`、`APPLIED` 和历史
`EFFECT_VERIFIED` 事件均归入成功状态，不再区分展示安装、执行与最终效果。
仅安装但没有执行记录的功能显示待执行，初始化及等待依赖仍计入待检查。
明确失败或降级优先于执行记录，旧进程、旧模块和无法核实归属的记录不计入成功。
关闭或条件不满足单独展示，重复事件不会重复增加同一功能的成功数量。
概览和导出报告共用同一统计函数，报告保留原始事件及关联日志以便排查。
这是诊断结果的统计口径，不会改写生产者事件或生成新的 `EFFECT_VERIFIED`。

## 面向用户的功能结果

`DiagnosticSummary` 在原始规则输出之后按明确的具体功能名汇总，页面和导出共用这份结果。
同一功能只展示一次，当前任一路径出现明确异常时优先提示异常，没有异常且存在执行记录时计为正常。
用途未知的记录保留各自身份，不能把不同的未知路径混成一个已知功能。

模块入口和日志采集归入诊断准备，不计入桌面功能数量。
已确认过期的记录放在上次运行记录入口，不影响当前统计。
无法核实当前身份的记录仍显示信息不足，不能借旧异常判断当前功能失败。
概览明确只检查本次有记录的功能，列表没有出现的功能尚未检查，零异常不代表所有功能正常。

列表和功能详情先说明当前情况与接下来怎么做，按文件夹、小组件、最近任务和抽屉等功能给出操作提示。
PID、规则编号、进程代际等保留在诊断技术日志中，完整原始证据仍随脱敏报告导出。
没有区分关闭和不适用的记录明确说明原因未确定，不猜测用户开关状态。
页面描述继续只用逗号，日志弹窗保留原始标点格式。

## 复用的架构

- `XposedInitEntry`：Java 模块入口、目标就绪、配置失败、热重载失败。
- `BaseLoad`：统一规则条件、DexKit 初始化、规则初始化及失败收集。
- `BaseHook` / `HookBridge`：既有 Java 安装 API 上加观察器，继续由 EzHookTool/libxposed 安装；
  不引入第二套安装、卸载、异常处理或热重载框架。
- NativeHookRuntime：原有 `InlineHookHost` 增加可选的只读事件出口；不改变唯一候选、原指令、
  continuation、第三方所有权或代际检查。其他未接入的 host 默认无输出。
- 原有桌面/Dock 维护循环：采样已有计数和 GOT 健康状态，不增加轮询线程或重新解析镜像。
  执行计数统一由现有 MonitorLoop 至多每 5 秒只读采样一次，位于两个运行锁之外；
  不依赖可选布局 worker 是否启用，中性配置也能记录真实执行。
- 原有 ModuleLog / Logcat / LSPosed 导入和 Room：低频结构化事件进入同一日志库。
- MIUIX：日志页菜单进入“桌面智能诊断”，使用项目 `BasePreferenceFragment`
  （`fan.preference.PreferenceFragment`）、分组偏好卡片、MIUIX 对话框和既有 Activity 外观。
  系统桌面主页也有“智能诊断”入口，顶部第一张独立大卡片展示四组数量，
  正常、需处理、待检查、信息不足分别统计，四个数字跳到对应分组，历史记录单独展示，未采集时不伪显示为零。

`common/diagnostics` 的规则与 wire 编码是纯 Java，不依赖 Android UI、网络或 AI。
`diagnostics/DiagnosticsRepository` 在既有日志 IO executor 上执行分析。
界面刷新仅收集和分析，不修改配置、不重启目标、不发起 Hook 重装。

页面描述通过 `DiagnosticPresentation` 仅使用逗号，规则证据、状态与原始日志不被重写。
原始日志对话框及导出文件保留排查必需的标识格式。
列表与导出的功能名由生产者语义标签或 `DiagnosticFeatureNames` 生成，
Native 槽位不再统一标为“桌面布局”，最近任务按当前运行时 Plan 的具体 setting/action 命名。
原 HCD1 槽位编号是诊断元数据标识，不是 Hook 地址，历史 v1 名称表通过逐槽回归核对；
未来重排槽位必须升级生产者 ID 版本，不得重新解释旧证据。
旧日志未记录最近任务具体参数时保留“项目未知”，不根据地址或异常关键词猜测功能。

## 覆盖范围

### Java

覆盖通过 BaseLoad 注册的规则初始化与 DexKit 阶段，以及 BaseHook 的常用 method / constructor /
all-method 安装帮助方法、Java chain 帮助方法和 HookBridge 优先级/异常策略入口。
首次回调仅记录一次，后续走原回调；保留参数对象、结果、null、extras、异常实例和既有异常模式。
chain 中传播的异常可能来自 `proceed()`，只标“异常来源未定”，不直接归因到 Hook。
热重载批次返回的待提交 handle 只记等待，不伪称物理安装完成。

**未覆盖**：直接使用 EzHookTool Kotlin DSL、直接调用 Hooks 或独立封装而绕过这些入口的
安装/回调，不会自动获得执行证据；只保留能够观察到的初始化事实。
DexKit 未给出明确数量时，不能区分无结果和歧义。ClassCastException 只说明类型/加载器可能异常，
不证明 ClassLoader 一定错误。

### Native

桌面布局与 Dock motion 的 inline bank 接入：已验证安装、后端拒绝、原指令变化、
空 continuation、页保护拒绝、补丁丢失、安全重装、未知/第三方修改。
Dock madvise 的现有 GOT 安装和维护链路接入安装、补丁丢失、第三方接管、代际过期。
普通图标、小组件、文件夹预览归位、抽屉搜索隐藏、禁止预加载使用既有计数采样执行/内部应用证据。
部分既有 runtime-contract / feature 拒绝记录通过严格格式适配关联。

**未覆盖**：所有 Native 消费方并非自动接入；未暴露原因的 resolver 不能自动细分为
ELF 损坏、符号缺失、ABI 错误或多个候选。generic resolver 的安全判定完全保留。
一次健康/安装记录不是持续完整性证明；没有当前健康证据时不能证明画面正常。
现有标量计数表示当前模块进程生命周期内曾执行，未提供目标 SO 独立重载代际；
不能据此证明目标 SO 重载后仍执行。bank 的镜像代际证据仍单独关联。

已有崩溃/安全模式机制继续保留；本版尚未把所有崩溃与安全模式状态转换为具有
当前进程身份的结构化证据，因此不能自动把进程退出归因到某个 Hook。

## 规则与关联

- Class/Method/Field 未找到、类型/加载器可能不匹配、DexKit、依赖、配置、安装、回调、热重载失败。
- Native `RESOLVE_FAILED`、`VALIDATION_FAILED`、`INSTALL_FAILED`、`HOOK_LOST`、
  `GENERATION_CHANGED`、`FOREIGN_MODIFICATION` 和未知分类；无明确细分证据时不猜测。
- LSPosed 对本模块 APK 的明确 open/parse 失败；无法归属桌面当前进程时显示未知及历史证据，
  不把其他进程的加载失败移植给桌面。
- Native 入口有证据但 Java 入口无证据：等待/未知，**不判断 Java 注入失败**。
- 当前框架接口的桌面作用域、框架运行记录与 installed APK 版本分别展示。
  APK 路径摘要和 native GNU Build ID 与运行事件分别核对，安装版本不代替运行版本。
- boot ID、PID、`/proc/PID/stat` 启动字段、模块/镜像代际隔离旧记录；权限不足不能确认进程时为 UNKNOWN。
- 同进程、相同明确时间窗口的上游失败优先于后续缺证据；不推断其他 Hook 也失败。
- 重复证据聚合；实际失败不被无关的后续成功回调掩盖；格式未知/截断不强行分类。

## 存储、性能和权限

- 结构化事件 `HCD1`，字段长度有界；每个进程的 Java 状态去重表上限 512。
  高频回调不写 Room、不解析日志、不读取配置；回调异常与首次执行限次记录。
- Native 观察器只在已有安装/维护转移时输出；健康状态未变不会逐帧输出。
- 日志采集最多 12000 条、8 Mi 字符、20 秒；预算不足提示数据不完整。
  手动刷新另按诊断 tag 有界读取最近 6 小时记录，避免其他应用大量输出挤走桌面启动证据；
  沿用原导入、去重与存储，不增加常驻采集线程。
  header 格式陌生或 tag 含 `]` 时仍隔断记录，防止把其他应用日志拼到模块日志中。
- 每次只读取最近 3000 条、单行至多 8192 字符；解码缓存有界，重复刷新仍重新验证进程身份。
  UID/作用域查询按批次缓存。Root 进程探测最多 128 PID、5 秒，只读临时结果。
- Room 2→3 显式迁移保留 logs，新增 `hook_diagnostics`，只保留 16 个有变化的快照；
  单报告约 1 MiB 上限，有明确截断提示。原始日志轮转后能说明证据不可恢复。
- SAF 导出前在应用私有 cache 暂存不可变的脱敏报告，Fragment 保存令牌；
  选择器期间进程被回收后仍导出当时的快照，不混用新分析结果。
  暂存至多 8 份、每份至多 4 MiB，完成/取消后清理；丢失则提示失败。
- 结构化记录只接受 logd UID/PID 与包 UID、当前作用域匹配的来源；无当前作用域证据时降为未知。
  来源校验、状态判定和完整性提示使用同一批次的作用域快照；服务在中途连接不改变本批次证据。
  不新增可写诊断 Provider 或公共广播。页面不导出，报告仅经用户选择的 SAF 文档输出。
- 这不是针对同 UID 宿主或 Root 的密码学认证；同权限代码本来就能修改 Hook/日志。
  隔离 UID/工作资料的归属不明确时保守拒绝，不宣称验证成功。
- 导出只包含关联的桌面诊断证据，过滤非桌面事件；异常摘要不记录参数值，报告脱敏
  authorization、cookie、token/password/account、URL、账户邮箱、手机号及私有数据路径。
  不上传日志，无网络分析、AI 依赖或自动修复。

## 验证

```sh
python3 tests/hook-diagnostics/run.py
python3 tests/logcat-import/run.py
python3 tests/log-diagnostics/run.py
python3 tests/home-render-performance/run.py
bash tests/run_host_native_tests.sh
```

诊断专用测试包含 48 项规则断言、生产 Java 观察器、生产 Native bank 路径、
生产 MonitorLoop 的采样时限/锁边界，以及迁移/保留 SQL。
导出测试覆盖仅凭保存令牌恢复不可变快照、不同代际隔离、路径越界拒绝、
缓存丢失和字节/文件预算。
展示回归核对全部 236 个历史槽位名称、最近任务运行时参数及共享 Action 名称、
Java 规则名称、未知标识退化、状态与原始证据不变，并检查顶部概览和两个桌面入口。
覆盖安装未执行、执行计为成功、已应用与旧验证事件兼容、成功统计与导出一致、初始化/解析/安装失败、补丁丢失、日志缺失、
Root 不可用、旧进程/PID 复用、重复聚合、冲突、损坏记录、热重载、模块加载失败、上游根因、
脱敏、UID/PID 来源拒绝、Native-only Java 未知、安装版本与运行版本不同。

Native 通用套件在 macOS 上的 `NativeHookBoundedReadsTest` 仍因 Linux-only
`SYS_process_vm_readv` / `SYS_getdents64` 无法编译；这不是本次修改文件的错误。
`DockNativeArm64Test` 仍要求 Linux+aarch64，不把跳过视为通过。
设备验收结果记录在 `tests/hook-diagnostics/device-20261026.md`。
