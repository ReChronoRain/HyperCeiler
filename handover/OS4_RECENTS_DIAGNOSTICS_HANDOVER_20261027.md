# OS4 最近任务与智能诊断交接

日期，2026 年 10 月 27 日

最后补充，2026 年 10 月 28 日，最近任务隐藏状态栏问题正在真机验收

## 交接状态

用户要求将当前代码提交并推送到 `os4-personal` 和 `os4-branch`，然后编写此文档。
代码已先推送至 `origin`，本交接文档随后单独提交并同步到两分支。

| 内容 | 提交 |
| --- | --- |
| OS4 隐藏最近任务卡片与原始参数转发修复 | `00eeaf646` |
| 按功能汇总诊断结果与用户操作提示 | `c5a95b0a3` |
| os4-branch 合并上述代码 | `dc1531794` |
| 首版交接文档 | `5637989ce` |
| 最近任务状态栏 WMS 参数转发修复 | `1cd325590` |
| os4-branch 合并首版文档与状态栏修复 | `e74d2fa01` |

上述是代码提交，不包含本文后续的文档提交。合并后两分支代码树一致。
仅操作 `origin`，没有推送 `upstream` 或 `hypermax`，没有操作远端 PR。

主工作区为 `/Users/shrimp/Repo/HyperCeiler`，保留在 `os4-personal`。
另一个现有工作区 `/Users/shrimp/.codex/worktrees/pr-1731-build-fix/HyperCeiler` 对应 `os4-branch`，直接复用，没有创建新工作树。

## 用户约定

- 遵守根目录 `AGENTS.md` 的 Dart 堆对齐、带标签指针及字节偏移约定，本次没有修改 Dart 分配或 native 寻址代码。
- 诊断目前只针对系统桌面，不能把 SystemUI 或 system_server 的普通日志包装成桌面功能成功证据。
- 用户要求已执行计为成功，不再展示安装、执行、最终效果三个层级。当前汇总仍保留明确异常优先的规则。
- 页面描述只使用逗号，查看日志的弹窗允许原始标点。使用 MIUIX 页面和对话框，概览保留在页面最顶部。
- 按具体功能命名，不能统一显示桌面布局这类模糊名称。用途无法确定时保留未知，不猜测功能。
- 10 月 27 日提交推送阶段没有进一步操作手机，10 月 28 日用户又明确允许安装测试及一轮重启，该授权已使用。后续不得为了验收擅自再次重启系统或 zygote。

## 已完成的代码

### OS4 隐藏后台卡片

入口沿用系统桌面，最近任务，隐藏后台卡片，偏好键为 `prefs_key_home_recent_hide_card`。

- `HomeHideRecentCardsOS4.java` 在 SystemUI 中观察标准 Shell AIDL 的实际 Stub 构造，获得运行时具体实现。
- 从真实 Stub 方法确认三个 int 参数与分组任务数组返回，兼容厂商将接口方法优化到抽象 Stub、接口本身只保留标记的情况。
- `HomeRecentCardsFilter.java` 校验当前任务组 getter 和 `TaskInfo` 公开字段，只过滤发给系统桌面 UID 的查询回复副本。
- 任一所选应用属于分屏或自由窗口组合时隐藏整张组合卡片，避免返回缺少成员的组合。
- 不调用删除系统任务、清理后台或结束应用进程的接口。保留其他调用方、原权限检查、原异常和未删除项的顺序及对象。
- 不使用固定 RVA、其他模块的 Hook 地址或硬编码厂商私有实现类名。动态识别不等于已覆盖所有系统版本。
- 配置来自 Provider 同次物理快照的可选 cursor extras，`hc_recent_cards_version` 为 1，选择集合复用现有长度编码。
- 原固定 schema、原有列、native combined Binder 字段顺序和长度均未改变，不需要为本次功能更新 system_server 协议。
- 不可变选择集合与修订号避免并发查询混用设置，监听器通知桌面刷新。共享后台线程池，无新常驻轮询线程。
- 日志标签为 `HyperCeiler.RecentsCards`，每次选择变化后的首次实际筛选报告一次，错误去重。

关键文件位于 `library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/recent/`，详细说明见 `tests/home-hide-recent-cards-os4/README.md`。

### 解锁时系统界面崩溃修复

初次手机验收时，指纹解锁触发 SystemUI 的 `miui-touch-fod` 线程崩溃，框架报错为 `dispatchInputEvent` 的 int 参数收到了 `java.lang.Integer`。
实际链路中 EzHookTool 1.3.0 对未修改的实例方法参数也调用 `proceedWith(receiver, args)`，运行中的框架拒绝了该替换调用。
这是系统界面崩溃后被重新拉起，并非用户未解锁，zygote 与 system_server 没有重启。

- 新增 `base/OriginalArgumentsHook.java`，继续使用实际依赖库的 before 与 after 链。
- receiver 和每个参数对象身份均未改变时调用原 `Chain.proceed()`。
- 显式替换参数或 receiver 时仍走框架替换接口，保留框架校验，不吞掉异常，不忽略修改。
- 仅接入 OS4 快速切换观察回调及本次最近任务查询，没有全局替换 Hook 框架或关闭功能。
- 回归使用实际 EzHookTool 与 libxposed API 类库，检查原调用一次、after、原异常身份、提前返回和显式参数修改。

### 智能诊断信息优化

这些改动已经编译及回归通过，**已随 10 月 28 日状态栏修复版安装，诊断页面验收尚未完成**。

- 新增纯 Java `DiagnosticSummary`，明确命名的相同功能只展示一行和计数一次，未知用途记录保留独立身份。
- 当前功能任一路径出现明确异常或降级时优先提示异常，无异常且有执行记录时计为正常，其他等待记录不再重复计数。
- 模块加载与日志采集放在诊断准备中，不计为桌面功能成功或失败。
- 已确认过期的记录放入上次运行记录入口，不影响当前数量，信息不足仍保持独立状态。
- 概览给出当前情况与下一步，说明只检查本次有运行记录的功能，不能把零异常解释成全部功能正常。
- 四项指标为正常、需处理、待检查、信息不足，分别定位到自己的分组，修复第四项也跳待检查的问题。
- 列表和详情使用当前情况与接下来怎么做，提供文件夹、最近任务、小组件、抽屉等具体操作提示。
- 技术字段和原始记录保留在诊断日志入口，技术弹窗有显示长度限制，完整脱敏报告仍保留全部相关证据及原始规则结果。
- 导出先给按功能汇总的结果与操作建议，再给原始技术信息，统计与页面共用一份汇总逻辑。
- 来源检查、进程身份、代际隔离、原始事件格式及 native 采样逻辑没有改动，没有新增 Hook 或后台采样线程。

关键文件为 `DiagnosticSummary.java`、`DiagnosticPresentation.java`、`DiagnosticRules.java`、`HookDiagnosticsFragment.java`、诊断 XML 资源及 `docs/hook-diagnostics.md`。

## 验证结果

### 10 月 27 日版本的 Host 与构建

| 项目 | 结果 |
| --- | --- |
| `python3 tests/home-hide-recent-cards-os4/run.py` | 10046 项通过，含 10000 次热查询与实际 Hook 库转发 |
| `python3 tests/gestures-os4/verify_systemui.py` | 63 项状态检查通过 |
| `python3 tests/hook-diagnostics/run.py` | 全部通过，含 55 项原始规则、31 项新汇总检查、观察器、导出、native、采样、迁移和展示契约 |
| 最新完整 Release APK | `BUILD SUCCESSFUL in 48s` |
| 最新 APK 签名检查 | 通过 |
| `git diff --check` | 通过 |

没有修改 CI、签名或仓库认证配置。构建复用既有本地受控脚本，不在文档或日志中输出凭据。
新汇总检查覆盖功能去重、当前异常优先、历史隔离、采集问题与功能状态分离、未知身份不伪称成功、条件不满足不伪称用户关闭，以及页面与导出统计一致。

### 手机上已经验证的版本

已安装并验收的是隐藏后台卡片及参数转发修复版，SHA256 为

`34656320a601bd81657172852f7204144a4bdebd0e2c4e7920d908035d77d535`

- 在原入口选择 HyperCeiler 本身，最近任务卡片消失，取消选择后卡片恢复。
- 隐藏期间系统任务与应用进程仍存在。
- 补充三轮隐藏与恢复，另做重复进入最近任务和两次重新打开被隐藏应用，总计 12 次最近任务进出。
- 被隐藏应用仍可正常打开，再进入最近任务仍保持隐藏，取消选择正常恢复。
- 原参数转发修复后指纹解锁成功，之后测试期间主 SystemUI、桌面、zygote、system_server 进程均稳定，SystemUI crash buffer 无新增记录。
- 使用 UI 取消测试选择，全部偏好与本轮测试前逐项一致，已返回桌面。
- 真实分屏组合、工作资料、多用户卡片、冷启动与设备重启场景尚未额外验证。设备现有 XSpace 不等于工作资料。

测试期间保存的 PID 仅供证据关联，后续必须重新查询，不能照抄这些 PID 发终止命令。

### 10 月 27 日诊断构建，未单独安装

最新 APK 包含新的诊断信息优化，路径为

`app/build/outputs/apk/release/HyperCeiler-2.10.166-20261027-release.apk`

SHA256 为

`75499415b7a2ca4004e850ba8c01e421ca3af6d464d1127a3a03924e64bd2143`

不能把前一版手机测试结果当作这一版诊断页面已经验收。
APK 在代码提交前完成构建，源文件内容对应本次代码修改，提交后没有重新生成构建中的 Git 元数据。

## 本地证据位置

- `/tmp/hc-hide-recent-cards-20261027/`，接口只读分析、host 结果及此前设备测试。
- 其下 `device/` 包含选择前后偏好、卡片隐藏及恢复截图、系统任务记录、运行日志和 crash buffer。
- `/tmp/hc-diagnostics-clarity-20261027/regression-final.log`，最新诊断回归。
- `/tmp/hc-diagnostics-clarity-20261027/build-retry.log`，最新构建结果。
- `/tmp/hc-feature-addressing/release-build-final.log`，Gradle 完整构建日志。
- `/tmp/hc-diagnostics-clarity-20261027/before.xml` 和 `before.png`，旧诊断页的实际画面，旧页曾把 62 条未知与历史记录合并展示。

临时目录不保证长期保留，设备截图及原始偏好可能包含私人信息，没有提交进仓库。不要把这些文件直接上传到公开仓库。

## 后续工作

1. 验收已随 10 月 28 日修复版安装的诊断页面，验证概览、五个结果分组、历史入口、诊断准备、功能详情、技术日志和 SAF 导出。检查正常、异常、等待、信息不足及只有历史记录的页面，不得仅检查页面能打开。
2. 检查系统字体放大与深色模式，以及概览指标点击目标。普通描述仍只用逗号，日志弹窗保留原始标点。
3. 确认导出与概览按功能计数一致，同一功能多条内部记录不会重复增加数量，原始技术证据没有丢失。
4. 上一轮用户要求进一步细分信息不足的原因，这部分尚未完成。现有 `Report.collectionComplete` 是总标志，已能根据现有代码说明 Root 拒绝、采集失败、没有桌面记录等原因，但框架状态未读取、桌面作用域未开启、进程身份核对未完成等还没有独立的本批次准备事件。
5. 若继续细分准备原因，应在实际读取点生成结构化事实，复用同一批次作用域快照，不通过解析环境文案或异常关键词猜测。此前仅讨论过 `FRAMEWORK_STATE_UNAVAILABLE`、`HOME_SCOPE_DISABLED`、`DESKTOP_PROCESS_UNVERIFIED` 等语义，**这些新事件没有实现**。
6. 只有旧模块记录时，目前概览给出通用的无可确认结果或信息不完整提示，尚未增加专门的只有旧版记录说明。不要仅凭历史记录断言当前桌面仍运行旧版，更不要自动重启桌面作为诊断动作。
7. 有必要时再验证真实分屏整组隐藏、桌面 Binder 重连及其他 Android 版本。本次手机结果来自 OS4、SDK 37，不能宣称所有 OS4 桌面均已通过。

后续继续先检查工作区、分支及手机实际状态，不恢复到旧提交，不覆盖用户的新偏好，不引入固定 Dart 地址或 ABI 猜测来处理兼容问题。

## 10 月 28 日新增问题，最近任务隐藏状态栏

### 已确认的手机事实

- 用户报告开关开启后仍不生效，已读取实际偏好，`prefs_key_home_recent_hide_status_bar_in_task_view` 与两个全局授权开关均为 true。
- ADB 首次查询为空，用户提示后再次发现并连接，不能继续把问题归因为 ADB 未连接。
- 已通过主屏和最近任务的稳定截图复现顶部状态栏仍显示，最近任务窗口焦点属于主 Launcher 窗口，手机桌面版本仍为 8584。
- `service call window 4735828` 收到 HCT 的 ACK，说明服务端识别这个专用事务，不能据此说 system_server 完全漏加载接口。
- 该探测来自非桌面身份且没有合法状态体，available 与 enabled 均为零是预期拒绝结果，不能拿它证明真实桌面读取不到偏好，更不能伪造桌面身份绕过检查。
- 窗口 dump 中虽有 RemoteInsetsControlTarget，但 `mAllowsSystemBarRemoteInsetsController=false`，状态栏实际控制目标仍是主 Launcher，不能仅凭存在远程对象就归因于远程 Insets 控制。
- 桌面原生镜像在其 APK 内映射，单纯搜索 libapp_launcher 文件名未命中不等于未加载。已有桌面布局与 Dock 原生运行计数。
- 已采到的竖屏与横屏 Rust 场景 setter 日志暂只看到 false，独立窗口 setter 尚无对应日志证据，需要继续核对实际进入时的场景来源，不得据此直接编造运行时布尔值或改用 Dock 缩放比例猜测最近任务状态。

### 已修复的可复现代码缺陷

`HomeRecentsStatusBarOS4.kt` 原来的 `createAfterHook` 仍经 EzHookTool 的 `InvocationContext.proceedWith` 转发未修改的整数参数。
实际 Hook 库配合模拟框架参数校验可复现 `WindowState.isRequestedVisible(int)` 抛出 `IllegalArgumentException`。
此前策略夹具直接运行 after 回调，绕过真实参数转发，没有覆盖这条失败路径。

现在两个窗口可见性查询使用已有 `OriginalArgumentsHook`，未修改参数时走原 `proceed`，原调用异常不被 after 覆盖。
本次没有修改原生寻址、三条场景布尔写入解析、HCT 字段、UID 与 PID 校验、偏好租约或系统任务。
这修复了一个明确的兼容缺陷，**尚不能单凭 host 通过就断言已解释并修好手机上的全部不生效问题**。

### 新验证与安装状态

- 真实 Kotlin 策略与实际 Hook 库回归，2094 项通过，含 2000 次未修改整数参数的查询、原方法调用次数，以及旧转发路径的负向对照。
- 身份与租约 49 项、偏好发布 107 项、合成重定位与单份 8584 Rust 镜像解析合计 302 项通过。
- 既有 runner 因重复包含 ModuleLog 夹具无法编译的问题已修正，仍编译生产 ModuleLog。
- 原受控临时构建脚本已不存在，改用现有配置直接执行 `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew :app:assembleRelease --console=plain`，没有修改签名或认证配置。
- 完整 Release 构建通过，1 分 22 秒，签名校验通过，已使用非增量方式覆盖安装。
- APK 为 `app/build/outputs/apk/release/HyperCeiler-2.10.166-20261028-release.apk`，SHA256 为 `0c0ff741eefe256c074f0daa19018580f5edfea633e40bebd9254a9bf66d7869`。
- 用户明确允许安装后重启继续验证，该轮重启已执行，用户已回复解锁，ADB 已恢复，安装文件哈希与上述 APK 一致。不得重复重启。
- 重启后已读取 boot ID 和新进程，桌面为 6224，system_server 为 3699，zygote64 为 1931。这些只记录本轮证据，后续操作必须重新查询，不能照抄 PID。
- 已解码新启动的 `home.recents.statusbar` 结构化 EXECUTED 计数事件，确认原生观察回调曾执行，不能直接用它证明隐藏画面已经正常。
- 后续检查时手机处于短信会话页，已暂停界面自动操作并发出回到桌面暂停操作的请求，当前尚未完成修复版的隐藏与恢复画面对照。

本轮证据在 `/tmp/hc-recents-statusbar-20261028/`，包括修复前后策略测试、原生回归、构建日志、偏好备份、窗口 dump 与本地截图。私人截图和原始日志仍未提交。

### 接下来优先顺序

1. 手机已解锁并连回，仍应在操作前重新读取 PID 和前台页面，确认仅执行了获准的这一轮重启。等用户回复暂停操作后再切换界面，不要中断短信操作。
2. 已有新进程中的原生执行记录，还需查新窗口策略是否真正消费、HCT 是否收到合法桌面状态、偏好是否可用，并核对 Insets 消费和实际画面。
3. 若仍显示状态栏，继续定位是哪一环缺失，不恢复硬编码地址，不关闭校验，不通过假返回值强行宣称成功。
4. 验证进入隐藏、退出恢复、开关关闭、返回其他应用、锁屏与解锁。每轮对比真实偏好并恢复测试设置。
5. 将最终真机结果补到本文，再同步两分支。智能诊断页面验收和更细的准备原因事件仍是独立未完成项。
