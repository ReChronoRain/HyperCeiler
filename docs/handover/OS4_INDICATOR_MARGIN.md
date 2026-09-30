# OS4 系统桌面「指示器底部边距」适配交接

更新：2026-09-29（终版）。状态：**已实现并装机验证。**
- **dp/px 单位修正已落地**：删除 `* density`，滑块名值即启动器实际位移（真机复核 +30 → 约 26 px）。
- **slot 5 挂点已定**：`HotseatLayerGetxController._getTotalHotSeatsMarginBottom`，干净 A/B 下
  +40 dp 使「问小爱」胶囊相对 dock 上移约 24 px，dock 不动，工作区网格随动。
  这是目前唯一找到的"胶囊与底栏分离"的杠杆，**不是胶囊独占**。
- 未提交、未合并、未推送；设备已恢复原设置。

## 1. 工作区与当前改动

- 工作区：`C:\Users\XiaoYe\.codex\worktrees\f0f3\HyperCeiler`，分支 `codex/os4-dock-icon-align`，
  基点 HEAD `95eaba5805f4988be6beb79b1b159d23e96e430c`（`os4-personal` 上的 Dock 动画修复）。
- 初次交接时跟踪文件改动 7 个（+85/−71）；本次续做另在 `symtab.cpp` 增加两个只读校准目标：

| 文件 | 内容 |
| --- | --- |
| `home_layout_hooks.cpp` | slot 5 符号 → `_getTotalHotSeatsMarginBottom`；删除上一轮的 Offset trampoline 接线、AOT 尾部签名校验与 pointer/applied/after 诊断 |
| `tweaks/symtab.cpp` | kTargets 增加 `_getTotalHotSeatsMarginBottom` 与 `indicatorOffsetBottomPortrait`；`workspaceIndicatorMarginBottom` 保留为校准探针；删除 `calIndicatorCenterPosition` |
| `home_layout_config.h/.cpp` | `delta_px`→`delta_dp`，注释改为"逻辑像素" |
| `HomeLayoutNativeEndpointOS4.java` | 删除 `* density`（`toPixels`→`clampDelta`），`knobDeltaPx`→`knobDeltaDp`，`MAX_DELTA_PX`→`MAX_DELTA_DP` |
| `HomeDockWindow.kt` | 同步 `knobDeltaDp()` 引用 |
| `HomeLayoutNativeEndpointOS4Test.java` | 期望值改为 30 / -70 / 12（dp 直传） |

- 新增（未跟踪）：`docs/handover/`、`tests/home-layout-native/dart_dump.py`、`tests/*.sh`（7 个真机脚本）、
  `tests/capsule_shift.py`、`app/hc-debug.keystore`（本地构建用）。
- `home_layout_dart_arm64.S` 与 HEAD 一致（上一轮的实验 trampoline 已整段删除）。
- 构建：`./gradlew :app:assembleRelease` → `BUILD SUCCESSFUL`；APK 已 `adb install -r` 装机。

## 2. 最终结论

1. **「指示器底部边距」在 OS4 上不等价于 MIUI 时代的页码点边距。** OS4 桌面默认显示「问小爱」胶囊，
   点状指示器已不可见；`Workspace._createIndicator` 创建的点状指示器只在编辑模式出现。
2. **胶囊与 dock 图标同层**：整层由 `HotseatLayerGetxController.calHotseatCenterPosition` 定位，
   改它则两者同距移动（间距不变）——这就是"底栏边距旋钮会连胶囊一起动"（C8）的结构原因。
3. **层内分离杠杆是 `_getTotalHotSeatsMarginBottom`**：它决定胶囊与图标之间的相对位置，
   是当前唯一能把胶囊与 dock 分开的挂点，代价是工作区网格会跟着动。
4. **胶囊"独占"的偏移尚未找到**。层内布局 `calculatePositionY` 是 void（结果写 `this+0xc6`），
   现有 double trampoline 够不着。
5. **启动器 Dart 几何量全部是逻辑像素（dp）**，`HomeLayoutNativeEndpointOS4` 原来的
   `* density` 使 8 个旋钮全部 3.25 倍过大，已删。

## 3. 关键证据链（离线，可复现）

工具：`tests/home-layout-native/dart_dump.py`（capstone 解析 `libapp.so` + `.gnu_debugdata` mini-ELF，
子命令 `sym / at / near / dump / callers / evidence / fields`）。
镜像：`launcher-7722-libapp.so`（Xiaomi 15，`RELEASE-8.01.02.7722-260904-09221533-R`）。

- `GridController.workspaceIndicatorMarginBottom`（0x95eecc）只有一个调用者
  `workspaceEditIndicatorMarginBottom`，再往上是 `WorkspaceGetxController.indicatorOffsetBottomPortrait`，
  其调用者是 `startIndictorAnimation`（0x962390）与 `onOrientationChanged`（0x95e798）。
  ——**旧实验 hits=0 是时序问题，不是死挂点**。
- `WidgetLocationCalculator.calIndicatorCenterPosition`（0x1371618）返回屏幕坐标系 Offset
  （`y = P + statusBar + navBar − V1 − V2/2`），只有 `Workspace._createIndicator`（创建时一次）与
  `_UnlockWidgetState._resolveCellLocationInfo` 两个调用点；孪生 Laptop 版近乎死代码。
  ——**上一轮把它当杠杆是错的，已撤销**。
- `evidence` 子命令把以上数据流做成断言，6 项全过；换 launcher 版本时数据流变形会直接报错。

## 4. 真机验证数据（Xiaomi 15，density 3.25，逻辑 369×821.5）

日志 tag `HyperCeiler.HomeLayout`，`layout knob ...` / `layout hook hits ...`。

| 试验 | 读数 | 结论 |
| --- | --- | --- |
| `searchBarWidthPx` | `last=337.1446` | 369 dp 屏宽的 91% → **几何量是 dp**（证据 1） |
| `calIndicatorCenterPosition` | `last=710.3415` | 821.5 dp 屏高的 86%（dock 上方）→ **是 dp**（证据 2，与上独立） |
| 底栏 +30（修正后） | 实测位移约 26 px | 修正前同一名值移动 97.5 → **3.25 倍已消除** |
| `indicatorOffsetBottomPortrait`（编辑模式） | `hits=1 last=-7.8773 caller=0x962690 d=98` | caller 与离线预测 `startIndictorAnimation+0x300` 一致；但视觉无变化 |
| `calHotseatCenterPosition` +80 | `hits=8 last=861.5992`，胶囊与图标同距移动 | 整层中心 |
| `HotSeatsConstants2.hotSeatsMarginBottom` | `hits=0` | 本机不走 dock 独立窗口（平板/折叠路径） |
| `stableIndicatorHeight` / `calculateDockOffsetY` / `dockIconPaddingTop` | `hits=0` | 非本机活动路径 |
| **`_getTotalHotSeatsMarginBottom`** | `hits=1`，干净条件下自身值 `+22.5031`（onInit 读取） | **slot 5 挂点**；+40 dp → 胶囊相对 dock 上移约 24 px，dock 不动 |

### 必须记录的一次自我纠错

中间几轮"胶囊大幅移动、图标反向移动"的结论是**错的**：当时 `capture_differential.sh` 无条件把底栏
旋钮也打开（值 0 → −70 dp），两组变量混叠，`_getTotal...` 的 `last` 因此被污染成 −47.4969。
脚本已修（`HS_VALUE=off` 表示不启用底栏旋钮），上表结论全部来自修正后的干净复测；
干净条件下该函数自身值是 `+22.5031`。

## 5. 复现方法

```bash
# 离线证据链（不依赖设备）
python tests/home-layout-native/dart_dump.py <libapp.so> <mini.elf> evidence
python tests/home-layout-native/dart_dump.py <libapp.so> <mini.elf> callers <符号|0x地址>

# 真机校准通道：免重编译换 slot 5 挂点（符号必须在 symtab.cpp 的 kTargets 白名单内）
adb shell setprop debug.hyperceiler.layout.override 1
adb shell setprop debug.hyperceiler.layout.hook5 <Dart符号名>
# 之后 force-stop com.miui.home 重启桌面；关闭用 hook5 off + override 0

# 真机 A/B 截图
tests/capture_baseline.sh  <输出.png>                    # 全部旋钮关
tests/capture_differential.sh <slot5符号|off> <滑块值> <底栏值|off> <输出.png>
tests/capture_knob.sh <prefs键名> <值> <输出.png>         # 单旋钮
tests/restore_device.sh                                   # 恢复设备

# 对比分析（本地 python，需清空 PYTHONPATH）
#   band.py  : 两帧差异带（哪个区域动了）
#   match2.py: 模板匹配定位胶囊的精确 y（胶囊被图标遮挡时会失锁，需配合裁剪目检）
```

注意：`run.sh` / `run_java.sh` 的 `mktemp -d -t <name>` 在本机缺 `XXXXXX` 会失败，
需包一层 `mktemp(){ command mktemp -d -t "${3}-XXXXXX"; }`；
C++ host 测试仍跑不了（本机无主机端 clang++，NDK 工具链缺 Windows sysroot）。

## 6. 未决与下一步

1. **胶囊独占偏移**：`HotseatLayerGetxController.calHotseatOffsetY`（0x121d4b8，void，
   结果写 `this+0xc7`）不是 `calculatePositionY`；后者实际位于 0xb77364。
   不应再按旧笔记里的函数名/地址配对写入。两条待核实的路：
   给模块加"写缓存字段"类钩子，或找到读取 `this+0xc6` 的 double getter 再挂。
2. **网格随动**：`_getTotalHotSeatsMarginBottom` 会带动工作区网格；要"只动胶囊"需配对反向补偿
   （例如与 `GridSizeCalRules.stableWorkspaceCellPaddingTop` 组合，未验证）。
3. **滑块方向与量程**：现增益 `kKnobDeltaGain[5] = 1.0`（滑块越大胶囊越低、越贴近 dock）；
   改 `-1.0` 即反向，一行。极值会把 dock 图标挤出屏幕，量程是否收窄由产品定。
4. **符号尺寸口径**：`SymbolIndex` 对同名符号取最大 size——真机上
   `GridController.hotSeatsMarginBottom` 解析出 sym_size=108，mini-ELF 里是 60。
   离线与在线的尺寸规则不一致，任何写死尺寸的断言都要小心。
5. **验证遗留**：C++ host 测试未跑（无主机 clang++）；提交/合并到 `os4-personal` 待确认。

## 7. 附件

- 镜像与符号表：`C:\Users\XiaoYe\.codex\visualizations\2026\09\29\01a0eb2b-4de7-7442-83d8-db3197b7610f\`
  下 `launcher-7722-libapp.so` / `launcher-7722-mini.elf` / `launcher-7722-symbols.txt`。
- 反汇编产物：同目录 `indicator-analysis\sym_*.txt`。
- 关键截图：同目录 `CAPS_BASE.png`（基线）、`FINAL2_110.png`（+40 dp 效果）、
  `CONTROL_HOTSEAT100.png`（底栏对照，复现 C8）。
- 取证与测试工具：`tests/home-layout-native/dart_dump.py`、`tests/capsule_shift.py`、`tests/*.sh`。

## 8. 续做调查：胶囊独立定位（2026-09-29）

- 离线沿 `LauncherIndicatorState.build` 的未命名闭包找到三次
  `LauncherIndicatorState._wrapWithAnimation` 调用：前两次包装应用列表/点状指示器，第三次
  在 `_buildCapsuleIndicator` 之后，返回地址 VA `0x117499c`。其 AOT 尾部确实向返回对象写入四个
  double 字段（tagged 偏移 0x13/0x1b/0x23/0x2b）。
- 曾在实验副本中只改第三次返回对象：改 0x1b/0x2b 时，+5 与 +40 dp 均记录 `applied=1`，
  但截图测得胶囊中心位移 **0.0 px**。原值分别为 0.000/0.900；这些字段不能当成胶囊屏幕
  top/bottom，所加的实验 trampoline 已撤回。第二组 0x13/0x23 的装机截图也无可见移动，
  但那次模块未产生日志，故**不能**把它视为有效 A/B 结论。
- 更关键的只读探针：把 slot 5 临时指向 `_CapsuleIndicatorState.build`，值 70（delta=0），
  实机日志 `hits=1 caller=0x1b127e0`。这证明当前桌面的胶囊组件确实构建过，排除了
  “第三个组件根本未创建”的猜测；它仍**不证明**其外层动画包装负责定位。
- 当前源码只在 `tweaks/symtab.cpp` 新增 `_CapsuleIndicatorState.build` 和
  `LauncherIndicatorState._wrapWithAnimation` 两个校准白名单符号；原有 slot 5 生产挂点
  `_getTotalHotSeatsMarginBottom` 保持不变。下一轮应从 `_CapsuleIndicatorState.build` 返回的
  widget/RenderObject 父子链追实际绘制变换或独立边距；不要直接把包装对象的四个 double
  当屏幕坐标。
- 实验 APK 两次成功构建、安装；最终已重新安装实验前备份的 APK。设备设置恢复为指示器
  value=121、enable=0，调试属性 `layout.override=0`、`hook5=off`，恢复脚本和安装命令退出码均为 0。
  证据、截图、基线源码副本与构建日志在同日 visualizations 的 `capsule-position` 目录。

## 9. 胶囊整体位移：沿原版构建树注入（2026-09-30）

- 先前三种返回值修改均有真机亮屏 A/B 证伪：①把 `_CapsuleIndicatorState.build`
  的 CID `0x2012` 返回对象误当 `Container`，改 `+0x27` 后胶囊相对 Dock 约 0 px；
  ②实际内层 `Container`（CID `0x2017`）的 `+0x27` margin 确被 `Container.build`
  读到 top=−51/bottom=+51，但胶囊文字消失，背景不动；③在 CID `0x2012`
  的 `+0x2f` 写 margin 虽记录有效，前后桌面截图胶囊像素位置完全一致。
  CID `0x2012` 的构造函数写到 `+0x10b`，它不是 `AnimatedContainer`，先前这一
  类名推断错误。
- 新路径来自原版 `_wrapWithAnimation` 的实际数据流：
  `LauncherIndicatorState.build` 后续匿名闭包在 VA `0x1174998` 第三次调用
  `LauncherIndicatorState._wrapWithAnimation`；此次 `x2` 是 `_buildCapsuleIndicator`
  的结果。`_wrapWithAnimation+0x94` 分配 CID `0x2183` 的包装对象，并把整棵
  胶囊子树写入其 `+0x37`。前两次调用分别包装应用列表与点状指示器，不应改动。
- 当前实现只拦截第三次返回地址（`LauncherIndicatorState.build+0x220`），在
  Dart UI 线程的 bump region 分配 `EdgeInsets` CID `0x15c9` 与 `Padding`
  CID `0x1e25`，用 `Padding.child=原版包装对象`、`padding={left=0,top=Δ,
  right=0,bottom=−Δ}` 替换返回值。这样注入层包住整个胶囊，交由原版 Flutter
  `Padding` 代码布局，而不是再改标签或背景内部字段；Dock/工作区共享几何值不变。
  分配区不足则原样返回。绑定前核验第三调用点、包装对象分配 stub、Padding
  分配 stub、`Padding.createRenderObject` 读取字段的指令；OTA 不匹配则不绑定。
- 真机验证已完成：安装版 APK 与构建产物 SHA256 一致，重启后日志显示第三分支
  `caller=...499c`、`widget_cid=0x2183`、`edge_cid=0x15c9`、
  `padding_cid=0x1e25`、`delta=-51.00`、`valid=1`。相同桌面亮屏截图
  （value=121）开关 A/B 的模板匹配：胶囊整体 `dy=-156 px`
  （correlation=0.8725），Dock 电话图标与工作区图标均 `dy=0 px`
  （correlation=1.0000）。目视确认背景与文字同向移动，关闭开关恢复原位；
  点击区域尚未单独做命中测试。截图与测量脚本位于
  `capsule-position-v2/capsule_wrapper_{on,off}.png` 和 `measure_layout.py`。
- 开关：系统桌面 → 布局 → 指示器 → 底部边距；偏好键
  `home_layout_indicator_margin_bottom_enable`、`home_layout_indicator_margin_bottom`。
  测试后恢复 value=121、enable=0、Dock enable=0，并清除
  `layout.override`、`hook2`、`hook5`、`prefs_write` 调试属性。

## 10. OS4 主屏幕工作区三向边距与搜索框退役（2026-09-30）

- 主屏幕的 `workspace_padding_top/bottom/horizontal` 三个原生槽位原先未连接生产挂点；
  旧的 `GridSizeCalRules.stableWorkspaceCellPaddingTop` 曾触发桌面崩溃，
  `GridController.titleMarginTop` 实际改变图标与标题间距，均不适合冒充工作区边距。
- 第一版把 `Workspace.build`（返回 CID `0x1e11`）整体包进 Flutter `Padding`，
  真机日志证实 `valid=1`，但独立截图 A/B 暴露三向语义不一致：顶部 +20 dp 只把
  工作区图标下移 61 px（胶囊/Dock 0 px）；底部 +20 dp 只把胶囊上移 61 px
  （图标/Dock 0 px）；水平 +15 dp 把左右列都右移 46 px，而非左右对称收窄。
  因此**不能把包整棵 Workspace 当成完成适配**，第一版已从源码撤下。
- 第二版改在 `ExtendedGridView.build` 返回的 CID `0x2007` 网格组件外注入
  `Padding`，其 `EdgeInsets` 来自顶部、底部、水平三个独立槽位；只把工作区
  网格子树交给 Flutter 布局，而不包住胶囊。绑定时核对原版函数尺寸、尾部
  分配 stub、`Padding.createRenderObject` 字段读取，AOT 形状不同则不挂。
  **第二版注入内核已构建/安装；重新启动后的真机 A/B 尚待完成。**
  随后加入的 OS4 搜索框旧偏好屏蔽已在源码与最终 APK 中编译通过，最终 APK
  尚未装机（重启后无线 ADB 未返回）；不要把第二版标为视觉适配完成。
- OS4「搜索框」组在界面置灰，并显示“布局 → 指示器”提示；OS4 配置端还忽略
  已保存的搜索框两项偏好，避免旧值在禁用 UI 后继续生效。非 OS4 设置界面不变。

## 11. 回连后的真机复测与指示器范围（2026-09-30）

- 无线 ADB 回连后，用调试属性启用第二版 `ExtendedGridView.build` 工作区注入；
  日志确认挂点装载，但 `hits=0`。顶部 +20 dp 与基线截图四个区域的相关性均为
  1.0000、位移 0 px。单独启用 `GridController.workspaceCellPaddingSide` 字段写入
  +5 dp，配置捕获 `hits=1` 而 `writes=0/0/0`，左右工作区图标、胶囊和 Dock
  仍各为 0 px。这两条路径**未完成三向边距适配**，不能标为有效功能。
- 指示器胶囊挂点仍命中：`LauncherIndicatorState._wrapWithAnimation` 的第三次
  返回，日志 `widget_cid=0x2183`、`edge_cid=0x15c9`、`padding_cid=0x1e25`、
  `valid=1`。底部边距滑块从 0–150 dp 扩大到 **0–300 dp**，默认 70 dp 不变。
  UI、Java 偏好取值范围、原生槽位范围及胶囊 Padding 的 delta 范围同步扩大；
  新最大值 300 dp 对应注入 delta −230 dp。真机默认值与极值截图匹配显示胶囊
  上移 703 px（相关性 0.8382），左右工作区图标及 Dock 电话图标位移均为 0 px。

## 12. 指示器底部边距扩至 700 dp（2026-09-30）

- 用户要求将「布局 → 指示器 → 底部边距」上限从 300 dp 提至 700 dp，
  默认 70 dp 保持不变。设置页 SeekBar、Java 偏好端点、原生旋钮校验同步更新。
  胶囊注入的合法 delta 区间随之改为 −630..+70 dp；700 dp 对应 −630 dp。
- 本轮构建、安装、zygote 重启及真机结果记录见同日 `indicator-range-700/VERIFICATION.txt`。
  注入日志在极值显示 `delta=-630.00 valid=1`、命中 3 次；700 dp 测试截图中
  胶囊已移出可见区域，调整到较小数值仍可正常显示。

## 13. 指示器向下调节区间（2026-09-30）

- 「布局 → 指示器 → 底部边距」保留 **70 dp = 默认中性位置**（注入 delta=0），
  将下限由 0 dp 扩为 −300 dp，上限仍为 700 dp。Java 偏好端点、原生旋钮
  取值范围与胶囊 Padding 校验同步调整；−300 dp 对应注入 delta=+370 dp。
- 用户原有保存值 70、开关开启。本轮测试使用调试覆盖，不改变这两个持久偏好。
  构建及真机结果见同日 `indicator-negative-range/VERIFICATION.txt`。
- 负端点 −300 dp 装机后重启 zygote；日志显示胶囊第三包装分支 `hits=3`、
  `delta=+370.00 valid=1`。该极值将胶囊移出底部可见区域；70 dp 中性值
  的注入增量为 0。测试后调试覆盖清零，偏好查询仍为 value=70、enable=1。

## 14. 指示器滑块界面修正（2026-09-30）

- 真机旧版数值弹窗实际显示“范围: 0 dp ~ 700 dp, 默认值: 0 dp”；此前 XML
  的负下限和 android:defaultValue=70 未证明控件自身正确。fan.miuix 1.0.13.0
  的 SeekBarPreferenceCompat 构造器将负 minValue 截成 0，并从 app:defaultValue
  读取控件默认值（缺省 0），而 Android 默认属性只影响持久偏好的初始读取。
- 指示器节点补齐 app:defaultValue=70；HomeLayoutSettings 初始化调用
  indicatorMargin.setMinValue(-300)。该 setter 不截负数，并重新计算步进区间；
  控件和数值弹窗共用同一 min/max/default，保存值仍为实际 dp，不做偏移编码。
- 修复 APK 真机界面确认：保存值 70 对应滑块标签“默认”，数值弹窗显示
  “范围: -300 dp ~ 700 dp, 默认值: 70 dp”。UIAutomator 导出验证通过。
  证据位于同日 `indicator-ui-fix/modified-ui.xml`、`modified-dialog.xml/png`。

## 15. 工作区原版坐标逻辑注入（2026-09-30）

- 依据用户要求撤下没有命中的 `ExtendedGridView.build` 包装路径，不再轮换 getter
  候选。离线追踪 `CellLayout.build` → `_buildCellLayoutContent` → 匿名 Obx 闭包 →
  `CellLayout._buildChildren`（7722 VA `0x14c9fbc`，size `0x1bb8`）。原版函数从
  参数 x4 读取网格起点 (`+0x3b` 的 Offset)、格子间距 (`+0x2b/+0x33`) 与
  列/行数 (`+0x1b/+0x23`)，再生成 `origin + cellIndex * stride` 的 Positioned 子树。
- 动态注入改写函数体 **+0x6c**（本版本 VA `0x14ca028`），在原版几何加载后、
  半格尺寸和逐项位置计算前更改四个局部值：`x += side`、`y += top`、
  `cellWidth -= 2*side/columns`、`cellHeight -= (top+bottom)/rows`。因此水平两侧
  对称收窄、底部改变网格可用高度，不把胶囊/Dock 放入共同位移层。
  该路径使用现有 Native Hook Kit 作为机器码跳转/重放载体，不拦截函数入口返回值。
- 核验 37 条原版机器指令（几何加载、被替换指令、最终 x/y 乘加），尺寸/字节不符
  不注入。stub 保留所有 32 个完整 SIMD 寄存器与 Dart 活跃通用寄存器/标志，
  仅改当前栈帧四个 double；不分配 Dart 对象、不写共享 GridConfig、每次原版重建
  重新读基线，避免累加。关闭/中性值直接走原版重放。几何范围/NaN/空网格有校验。
- 构建、安装均成功；原生回归测试覆盖中性、三项独立、组合、极值、退化输入和
  1000 次重建，全部通过。Java 端点回归通过。源码回滚已在独立副本验证。
- 真机第一次重启时模块未启用；用户在 LSPosed 手动重新启用后，内部代码已命中，
  日志显示几何值确实改变，但底部 +20、水平 +15、组合 20/20/15 的截图仍与基线
  完全相同。这证明构建阶段的位置不是最终绘制来源。**本节 +0x6c 实验已撤下**。
  后续原版 `GridCellDelegate` / `GridOccupiedCellDelegate` 的布局循环覆盖了这些位置，
  当前有效实现见第 16 节，不应回到构建阶段包装或继续轮换 getter。
- 本轮证据：同日 visualizations 的 `workspace-code-injection` 目录。

## 16. 三向边距：最终 RenderBox 布局代码注入，真机通过（2026-09-30）

### 原版数据流与当前实现

- 分支仍为 `codex/os4-dock-icon-align`；本轮未合并、未推送，保留其余工作区修改。
- 沿原版 `CustomMultiChildLayout` 追到实际写入 RenderBox 约束和 ParentData.offset
  的两个代理，不修改 getter 返回值，不给整个 Workspace 加 Padding：
  - `GridCellDelegate.performLayout`：7722 VA `0x16f77c8`、size `0x5cc`，注入函数体
    **+0xd8（VA 0x16f78a0）**。原版刚把左起点、垂直校正、格宽、格高存入当前帧，
    注入修改 FP−0x58/−0x40/−0x50/−0x48，再继续原版循环和布局。
  - `GridOccupiedCellDelegate.performLayout`：VA `0x16f702c`、size `0x6ac`，注入函数体
    **+0x234（VA 0x16f7260）**。原版正在准备当前图标的坐标；注入从原始 GridInfo
    及 ItemInfo 的列/行索引重新计算 FP−0x70/−0x68 坐标及 FP−0x60/−0x58 约束。
- 公式仍为 `left += side; top += topDelta; width -= 2*side/columns;
  height -= (topDelta+bottomDelta)/rows`。原版格内居中继续执行，因此边界增量和
  图标中心位移略有差别：这是网格边距变化，不是整组图标等距平移。
- Native Hook Kit 仅承担机器码跳转和原始指令重放。slot 2/3 分别承载两个布局代理，
  **任一 slot 2..4 增量非零都同时启用两个代理**；三个偏好槽位仍各自独立传入 helper。
- 绑定前逐字核验 **84 条机器指令**及函数尺寸，覆盖原始字段加载、栈槽、被替换
  指令及最终约束存储；不匹配不绑定。入口保留 32 个完整 Q 寄存器、所有活跃
  AAPCS 易损 GPR、Dart 栈、LR 和 NZCV，仅修改当前栈帧，不写共享 Dart 堆对象，
  不分配 Flutter 组件。逐图标计算始终重新读取原始 GridInfo，避免格宽/格高累加。
- 一行 hotseat 明确排除（rows < 2）。胶囊和 Dock 的父布局、共享配置均不改。
  几何有限性、范围、行列数、列/行索引和最小格子尺寸均有校验。

### 真机 A/B 与正式偏好链路

测试手机为 4×6 网格、截图 1200×2670；单位为物理截图像素，向右/向下为正。
以下模板匹配相关性最低 0.9916（已排除通知条区域）：

| 原始增量 top/bottom/side | 首行 dy | 末行 dy | 左列 dx | 右列 dx | 胶囊/Dock dx,dy |
|---|---:|---:|---:|---:|---|
| 20/0/0 | +56 | +5 | 0 | 0 | 0,0 |
| 0/20/0 | −5 | −56 | 0 | 0 | 0,0 |
| 0/0/15 | 0 | 0 | +34 | −35 | 0,0 |
| 20/20/15 | +51 | −51 | +34 | −35 | 0,0 |
| −20/−20/−15 | −51 | +51 | −34/−35 | +34 | 0,0 |
| 全部关闭（恢复） | 0 | 0 | 0 | 0 | 0,0 |

- APK 安装成功。此前 system_server 未加载模块导致偏好事务 status=−74、Dock 无背景；
  本轮用户重新启用后安装新的布局代理版，并成功重启 zygote，当前正式偏好读取恢复，
  Dock 背景恢复。不能把此前缺失 Dock 背景归因于工作区坐标注入。
- `layout.override=0` 正式链路测试：保留已保存的顶部 50、底部 140、水平 35，
  同时开启三项，日志 `delta=20/20/15 valid=1`，两个代理都命中；截图结果与上表
  组合值完全一致。全部关闭后恢复原网格。点击位移后的设置图标，前台活动为
  `com.android.settings/.SubSettings`，之后回到桌面。
- 第一轮测试后曾恢复用户原有持久偏好：顶部 value=50 / enable=1；底部 value=140 /
  enable=0；水平 value=35 / enable=0。`layout.override=0`、`knobs_enable=0`、
  `prefs_write=0`、调试旋钮向量全部为 0。因此最终手机顶部仍按用户原设置调整，
  不是强制把三个持久开关全关掉。
- 最终指令核验补齐后再安装并重启 zygote。此时用户主动调整了滑块，实测读取为
  顶部 146、底部 240、水平 100；最终截图/日志记录对应增量 116/120/80，两个
  原版布局代理均 valid=1。极大组合值明显压缩网格，部分标题发生拥挤，不能把
  这组截图按旧 20/20/15 的模板位移表解释。最终第二轮脚本只恢复开关 1/0/0，
  没有覆盖用户的新数值。用户明确反馈“我调过了，现在有用，不用测了”，已停止
  所有后续设备测试，不再写入偏好或重启手机。第一轮正式链路输出保留在
  preferences-test.txt；最终轮保留在 preferences-final-test.txt，后者截图覆盖了
  prefs-*.png，应以文件相应时间及日志增量区分。
- 新原生测试覆盖两个真实栈帧布局、逐图标 1000 次无累加、hotseat 排除，以及
  中性/三项独立/组合/极值/NaN 等；全部 exit=0。84 指令离线核验及逐字变异拒绝
  通过；Java 端点回归 exit=0；最终 release 构建成功（50s）；独立源码副本回滚恢复
  原始六文件 SHA256、删除两个新增源码文件，live 工作区保持新实现。
- 本轮只验证了此 7722 / 4×6 手机桌面、单项/组合/负值及一次图标点击；其他
  桌面版本、横屏/折叠屏、多格小部件和拖拽重排仍需独立回归，不当作已测通过。

### 产物与继续工作入口

- APK：`app/build/outputs/apk/release/HyperCeiler-2.10.166-20260930-release.apk`
  SHA256 `AC93FF09F83579354DF16B7AD0B4B3558FDA8012BAD1A9AE14A67B0D45535B7C`。
- 证据目录：
  `C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/workspace-code-injection`。
  含 MODIFIED_FILE.cpp、DIFF_FILE.diff、VERIFICATION.txt、可执行 ROLLBACK.sh、原始
  六文件副本/哈希、新增 header/test、独立 rollback-tree、全部 A/B PNG/log、
  正式偏好截图/测量及构建日志。回滚脚本已在独立副本执行，不会自动回滚设备 APK。
- 继续工作可复用：
  > 读取本交接第 16 节；保留当前两段最终 RenderBox 布局机器码注入，不重试已证伪
  > 的 getter、ExtendedGridView 或 CellLayout._buildChildren 包装。先记录设备偏好和
  > APK 路径，再对多格小部件、拖拽重排与其他网格规模做实机回归；每次恢复测试值，
  > 用截图和实际命中结果说明结论，保留工作区其他改动。

## 17. 桌面搜索设置文案（2026-09-30）

- 系统桌面 → 布局中原“指示器”分组改为“桌面搜索”，底部边距说明及 OS4 旧搜索栏置灰引导同步更新。
- 14 个已有语言资源共 28 处文本完成更新；资源 ID 和持久偏好 key 保持不变，避免丢失已保存的值。
- 桌面搜索底部边距保持默认 70 dp、范围 -300..700 dp；OS4 顶部、底部、水平边距沿用第 16 节的原始布局逻辑注入。
- 本次分支发布只编译和同步代码，不再次操作手机或修改用户偏好。签名密钥、临时 ADB 探针脚本及本地证据目录不纳入提交。

## 18. 同一滑块驱动胶囊 + 页面圆点（2026-09-30，静态实现，未装机）

### 需求与语义

用户要求「让指示器的位置跟随搜索框」。三轮澄清后锁定：**跟随** = **同一垂直基准**，
对象 = **胶囊** 与 **页面指示圆点**，驱动方式 = **同一个滑块同时驱动两者**。

### 为什么必须挂两个 hook

`LauncherIndicatorState.build` 内 `_getCurrentIndicatorType` **二选一**：
- 正常桌面 → `_buildCapsuleIndicator`（胶囊，第 9/11 节的 `_wrapWithAnimation` 第三次返回）；
- 编辑模式 → `_buildScreenIndicator`（页面圆点）。

两者**互斥且渲染链独立**，只是"桌面此刻显示哪个"的切换，所以一个 hook 够不到另一个。
第 8/9 节已把「胶囊」这条链做通，本次补上「页面圆点」这条链。

### 实现

| 目标 | symbol | 槽位 / 入口 |
|---|---|---|
| 胶囊（已有） | `LauncherIndicatorState._wrapWithAnimation` | knob 5 → slot 9，`hc_layout_capsule_entry`，gain −1.0 |
| 页面圆点（本次） | `GridController.workspaceIndicatorMarginBottom` | **`kIndicatorDotSlot`（新）**，`hc_layout_dart_IndicatorDot_entry` |

- **asm**：`home_layout_dart_arm64.S` 追加一条 `geometry_trampoline hc_layout_dart_IndicatorDot_entry, ...`，
  完全复用现成宏（caller 返回值 + delta）。`workspaceIndicatorMarginBottom` 是"base + 传入 d0"式的
  dp 访问器（7722 VA `0x95eecc`），shape 与其它 geometry trampoline 目标一致。
- **slot**：`kIndicatorDotSlot = kAnimationMagicSlot + 1`，`kSlotCount` 由 13 增到 14。
  companion **刻意不放进 `kKnobHookSlotBase + index` 区间**，因此 `arm_hooks` 的
  index→slot 映射、slot verdict 循环里的回滚扫描、`prime_home_layout_probe` 全部无需改动。
- **绑定**：新增 `bind_indicator_dot_target()`，在 `bind_knobs()` 的 per-knob 循环之后调用。
  幂等（`_address != 0` 直接 return）；不受 `g_field_writes_enabled` 门控（它是 hook 不是 field write）。
- **发布**：`publish_hooks()` 末尾调 `publish_indicator_dot_delta()`，重读 knob 5 的 `delta_dp`，
  门控与 `publish_hooks` 完全一致（address 非 0 && armed && knob.hook_armed && delta != 0），
  否则 `_enabled = 0` —— 否则滑块归零后页面圆点会留着 stale 偏移。
- **诊断**：worker 周期行追加 `layout indicator dot ...` 一行（va / size / addr / armed / enabled /
  delta / hits / caller），否则"hook 没绑上"与"绑上了没命中"在日志上无法区分。

### 未标定值（唯一）

```cpp
constexpr double kIndicatorDotDeltaGain = -1.0;   // home_layout_hooks.cpp
```

**符号有依据**：两个访问器都是"margin 越大越往上"，与胶囊同向。
**幅值 1.0 是假设**：胶囊吃的是 Flutter `EdgeInsets.top`（逻辑像素），页面圆点吃的是
`workspaceIndicatorMarginBottom`（dp）。**无设备可 A/B，需真机标定**。
要调整只动这一个常量，**不要碰已标定的胶囊 gain（`kKnobDeltaGain[5] = -1.0`）**。

### 构建与验证

`./gradlew :app:assembleRelease --offline` → `BUILD SUCCESSFUL in 58s`。
`HyperCeiler-2.10.166-20260930-release.apk` = **8,773,111 B**；
`lib/arm64-v8a/libHyperCeilerNative.so` = **1,382,864 B**（较上次 +504 B = 新增 trampoline + helper，
符合预期；**不是** 20.4 MB 的 strip 静默失效）。

**未装机**（无线调试掉线，且本轮为纯静态改动）。真机待验证：
1. 正常桌面调「桌面搜索 → 底部边距」，胶囊上移；
2. 进编辑模式，页面圆点**同幅**上移；
3. 若两者不同步 → **先量差值**，再按差值调 `kIndicatorDotDeltaGain`。
