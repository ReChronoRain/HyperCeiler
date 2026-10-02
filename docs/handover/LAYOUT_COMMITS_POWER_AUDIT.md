# 布局适配相关 commit 功耗静态审计

审计方式：**纯静态只读**（读 diff + 源码 + 常量表），未连接设备、未运行构建、未改动任何代码。
审计时间：2026-09-30。被审范围：下表中 6 个「布局适配」commit 及其引入/修改的运行时路径。

## 1 被审 commit 清单

| commit | 主题 | 触碰的运行时路径 |
|---|---|---|
| `3d3e0e766` | OS4 workspace margins + desktop search positioning | `home_layout_hooks.cpp` worker、新增 `hc_layout_workspace_entry` / `hc_layout_capsule_entry` / `hc_layout_container_probe_entry` 三个 trampoline |
| `95eaba580` | Dock backdrop 跟随图标 recents 运动 | `HomeDockWindow.kt` 帧循环 `motionScale`/`revealShift*`、`DockNativeMotion.scale()` |
| `5c7676e96` | OS4 灰置图标尺寸入口 | 仅设置页 `HomeTitleSettings.java`，**无运行时路径** ⇒ 无功耗面 |
| `70f5751b8` | geometry 读取失败全部留痕 | `DockGlassClient.kt` 900 ms 节流读取的日志门 |
| `7026fb395` | geometry 改走 provider | `HomeLayoutNativeEndpointOS4` refresher + `DockGlassClient.refreshGeometry()` |
| `0f912b997`* | dock sweep 不再整夜 tick | `HomeDockWindow.scheduleNativeBindSweep()`（已含 doze 门控） |

\* 为对照组：该 commit 是「已修好的功耗基线」，本审计以它为标尺衡量新 commit 有没有破坏它。

## 2 结论摘要

| # | 发现 | 严重度 | 是否新增于本轮 commit |
|---|---|---|---|
| P1 | `hc_layout_workspace_*` trampoline 每次 `performLayout` 压入 **704 B 栈帧 + 33 对寄存器存取**，即使 delta 全为 0 | 中 | ✅ 新增（`3d3e0e766`） |
| P2 | `hc_layout_container_probe_entry` 是**校准残留死代码**；出厂不可达（slot 2 symbol 已换），仅 debug 通道可点亮，点亮后会污染 slot 2 命中计数 | **低**（二轮下调） | ✅ 新增（`3d3e0e766`） |
| P3 | `performLayout` 钩子命中计数 `hits` **无条件自增**（每个 cell、每帧） | 低 | ⚠️ 继承既有模式 |
| P4 | `motionScale()` 让**拖动段**也走矩阵缩放，每帧多一次 `setMatrix` → SurfaceFlinger 重采样 | 中 | ✅ 新增（`95eaba580`） |
| P5 | `relativeOffsetY` 由固定 20dp 改为 `8dp × progress`，上限提高 ⇒ 每帧 `setPosition` 位移量增大 | 低 | ✅ 新增（`95eaba580`） |
| P6 | `refreshFromProvider()` 在 app 被冻结时**每次失败只试第 1 个 key** ⇒ 已修，但 1.5 s 周期仍有 1 次跨进程 IPC | 低（已缓解） | 部分（`7026fb395` 引入、已含退避） |
| P7 | `hc_layout_apply_now()` 在**每次 config 捕获调用**时执行 `apply_knob_fields()`，遍历 8 knob | 低 | ⚠️ 继承 |

**未发现**：新增 wakelock、新增 alarm、新增无条件高频 Binder、新增帧循环泄漏。

**二轮更正**：初版把 P2 判为「中」并称「无条件绑到通用 `Container.build`」——**该说法有误**，
详见 §4「修正后的结论」。因此**唯一确定值得动手的只剩 P1**（外加 P4 的补文档）。

---

## 3 P1 — workspace trampoline 的 704 B 帧搬运（`3d3e0e766`）

### 现象

`home_layout_dart_arm64.S` 新增的 `workspace_layout_splice` 宏：

```asm
ldrb w10, [x9]        // enabled 门控
cbz  w10, 1f          // ✓ 关闭时直接跳过
sub  sp, sp, #704     // ← 打开后
stp  x0..x30          // 15 对通用寄存器
stp  q0..q31          // 32 对 SIMD 寄存器（512 B）
bl   hc_layout_workspace_layout
... 逐条恢复
```

设计上 **exactly 一次 704 B 栈帧 + 63 组 stp/ldp**，无论 delta 是否为 0。门控只有 `enabled`，
没有「delta 全 0 也照样压栈」的短路。

### 为什么这是功耗问题

`GridCellDelegate.performLayout` / `GridOccupiedCellDelegate.performLayout` 是 **RenderBox 的
per-cell 布局函数**：一次桌面重排会按 cell 数调用（默认 4×6=24，icons 越多越多），
且发生在 Flutter 的 **layout 相位**，通常紧接在 vsync 上。

调用点包括但不限于：滑动换页、图标拖拽、文件夹开合、打开/关闭应用回到桌面、
锁屏解锁、壁纸视差重排。这些都是**用户可见的交互帧**，也就是最不该加 CPU 的帧。

粗估开销：24 cell/帧 ×（704 B store + 704 B load + 一次函数调用与 33 组寄存器搬运）。
在现代 SoC 上单次约 **1–3 µs**，24 cell 约 **25–70 µs/帧**——单看不足以掉帧，
但它是「**连 delta 全为 0 也要付**」的常数税：用户从未打开 workspace 边距滑块时也在付。

### 关键判断

这不是「设计错误」，而是「**缺少一层 delta 短路**」。`publish_hooks()` 里已经有现成的
`workspace_active` 语义（`g_knobs[2..4]` 任一非 0），但那个门控只写 `hook_enabled`，
**没有把 workspace 的 `enabled` 一起带上**。

而 `publish_hooks` 对 index 2/3 的写法是：

```cpp
const bool workspace_active = (index == 2 || index == 3)
    && (g_knobs[2].delta_dp != 0 || g_knobs[3].delta_dp != 0 || g_knobs[4].delta_dp != 0);
if ((!workspace_active && delta == 0) || !knob.hook_armed) { *knob.hook_enabled = 0; continue; }
```

即：**workspace 三键全 0 时，`g_knobs[2].enabled` 会被置 0 == falsy**，`cbz` 就会跳过压栈。
**所以只要 workspace_active 判定为真且在 delta 归零后能重新回收，P1 自动消失。**

问题在于 `workspace_active` 的条件里有 `g_knobs[4]`（WorkspaceSide），而 `HC_LAYOUT_KNOBS` 里
WorkspaceSide 的表项是 **`nullptr` 符号**（上一轮表里已注明 inert）。同时 `readPreferences()` 里
`index == 6 || index == 7` 被显式 `continue`（searchbar 两项），但 **`index == 4`（WorkspaceSide）
没有对应的 skip**——它只有 `if (!readBoolean(key + "_enable", false)) continue;`。
用户一旦开过「工作区水平边距」开关并保存，`g_knobs[4].delta_dp` 可能长期非 0 ⇒
`workspace_active` 恒真 ⇒ **workspace trampoline 永久压栈**，即使用户从未碰过 top/bottom。

**待验证（需设备）**：`g_knobs[2..4]` 在「workspace 三键全关」后是否真的都回到 0。
静态看只有 `sync_requested()` 会写回 0（`enabled ? delta : 0`），而 `enabled` 来自
`readPreferences()` 的 `enabled[index] = 1` 分支——那需要 `_enable` 开关为真。
⇒ 关掉开关即回 0，**这条静态上是自洽的**；P1 的实际暴露面因此收窄为
「用户开了 workspace 边距」这一个场景，属于「用户显式要求该功能」的代价，可接受。

### 建议（不作为本轮改动）

1. 在 `workspace_layout_splice` 宏里**把 `delta` 也纳入门控**：`enabled` 之外再加一个
   `hc_layout_dart_<slot>_delta_nonzero` 字节，全 0 直接 `b 1f`。约 5 条指令换掉 704 B 搬运。
2. 或在 C 侧 `hc_layout_workspace_layout()` 开头加「三 delta 全 0 立即 return」——
   **但这不省栈搬运**（栈已经压了），只能省 C 函数体。**推荐做法 1。**
3. **不要**删掉 `workspace_layout_splice` 本身：它是 `3d3e0e766` 的核心功能，
   去掉等于回退 OS4 workspace 边距。

---

## 4 P2 — `hc_layout_container_probe_entry` 是校准残留（`3d3e0e766`）

> **★ 更正（2026-09-30 二轮）**：本节初版称它是「无条件绑到通用 `Container.build` 上」，
> **这个说法不准确**。事实是：**出厂配置下这段分支根本不可达**，因为 slot 2 的
> `kKnobHookSymbols[2]` 已被 `3d3e0e766` 改成 `"GridCellDelegate.performLayout"`
> （`home_layout_hooks.cpp:308`），不是 `Container.build`。
> 精算过的结论见本节末「修正后的结论」。**优先级从「中」下调为「低」。**

### 现象

`hits` 门槛之外的执行路径：

```asm
hc_layout_container_probe_entry:
    dart_save
    ldr/str   hits++            // 无条件
    ... 保存 x1/x2/x30
    mov x0, x1 ; bl hc_layout_probe_container   // ← 无条件调用
```

C 侧 `hc_layout_probe_container()`：

```cpp
const uint32_t ordinal = seen.fetch_add(1, ...);         // 无条件原子自增
if ((widget & 1u) == 0) { if (ordinal < 12) log; return; }
memcpy header; 提取 widget_cid;
if (ordinal < 12) log;
if (widget_cid != 0x2017) return;                        // 大部分情况在此返回
... 读 margin、再读两次 header、读 top/bottom
```

### 为什么是残留

1. **注释自述**：「`Read-only slot-2 calibration`」「`was the capsule's new Container.margin
   consumed by Flutter?`」——这是一个**探针**，不是功能。
2. **没有 delta 门控**：与 `hc_layout_capsule_entry` 不同（后者有 `enabled` 检查），
   这个 entry **完全没有 enabled 判断**。
3. **绑定分支存在**：`bind_knobs():1201` 与 `arm_hooks():1341` 都有一份
   `if (symbol == "Container.build") hook_entry = hc_layout_container_probe_entry;`。

### ★ 修正后的结论（二轮核算）

这两处绑定都是 **`symbol == "Container.build"` 的守卫**，而 slot 2 的出厂 symbol 是
`GridCellDelegate.performLayout` ⇒ **出厂配置下两支都不命中，探针不会被装载**。

它还能被点亮的**唯一路径**是 debug 校准通道（`debug.hyperceiler.layout.override=1` 且
`hook2=Container.build`，或 `symbols` 列表第 3 项写成 `Container.build`）：

```cpp
// apply_debug_hook_overrides(): g_knobs[index].symbol = name;   ← 只有这里能换成 Container.build
```

**也就是说：用户即使装了这个模块、开了 workspace 边距，也永远不会走到这个探针。**
我在初版报告里把它写成「无条件绑到通用函数上」，是因为我只读了 `bind_knobs` 的赋值语句
而**没有回头核对 slot 2 的出厂 symbol 是什么**——这是本报告唯一一处结论性错误，已更正。

### 精算过的残留成本（即使被点亮）

假如真的通过 debug 通道点亮了，成本也远低于初版估计：

1. **入口自增的是 `WorkspaceTop_hits`**（`home_layout_dart_arm64.S:298-299`），
   即**复用 slot 2 的计数器**，不是独立计数器。所以一旦点亮，`layout hook hits`
   那行日志里 `GridCellDelegate.performLayout=...` 的命中数会被这个探针**污染**——
   这是**诊断失真**问题，不是功耗问题。
2. **入口不压 704 B 帧**（那是 `workspace_layout_splice` 宏才有的），只有
   `dart_save`（6 条）+ hits 自增（3 条）+ `stp x1/x2` + `stp x30`。
3. **C 侧在 `widget_cid != 0x2017` 时提前返回**，`ordinal < 12` 之外不产生日志。
4. **结论**：单次约数十 ns；唯一确定的副作用是**污染 slot 2 的命中计数**，
   会让人误判 `GridCellDelegate.performLayout` 的真实调用情况。

### 建议（下调优先级）

- **不是「必须删」**。出厂不可达 ⇒ 无生产功耗影响，可以先留着。
- 但它是**死代码 + 校准残留**，且**一旦有人走 debug 通道复现胶囊调试就会污染 slot 2 计数**。
  若胶囊定位已收尾 ⇒ 顺手删掉 3 处（两处绑定分支 + C 函数 + asm entry）最干净。
- **不建议**把它加进 §7 的「测试残留暂不清」豁免名单：那两条（`startDiagnostics()`、
  `probeLauncherNativeEntry()`）都是有明确诊断价值且在用的；这条是**未使用**的死分支。

---

## 5 P3 — `hits` 计数器无条件自增（继承既有模式）

三个新 trampoline（`workspace_layout_splice`、`hc_layout_capsule_entry`、
`hc_layout_container_probe_entry`）以及既有的 `geometry_trampoline`，都在**入口最先**做：

```asm
adrp x9, \hits ; ldr x10,[x9] ; add x10,x10,#1 ; str x10,[x9]
```

三条内存指令 + 一次跨 cacheline 的读改写，**在 `enabled` 门控之前**，且 `\hits` 是
可写全局 ⇒ 每次调用都产生一次**脏 cacheline**。对于 workspace 那条（per-cell、per-frame），
这会让 704 B 栈搬运之外再叠加一次 cache 抖动。

**这不是本轮引入**：`geometry_trampoline`（既有）就是这么写的，新宏沿用了同一模板。
标为「低」是因为单次成本远小于 P1 的栈搬运，且 `hits` 是校准必需的（区分「从没跑过」
和「跑了但 delta 没用」）。

**若要一起优化**：把 `hits` 自增移到 `enabled` 门控之后。代价是「关闭时不计数」，
但这恰好是校准想要的语义（关闭时你不需要知道它跑了多少次）。

---

## 6 P4/P5 — `95eaba580` 的每帧提交量（Dock 跟随）

### P4：拖动段现在也走矩阵缩放

`95eaba580` 把 `pivotOffset()`（纯 `setPosition` 补偿）换成了 `motionScale()`：

```kotlin
private fun motionScale(layer: Layer): Float = if (layer.nativeApplied)
    layer.nativeMotion.scale() else 1f - .05f * layer.motion.progress(layer.motionTime)
```

并把它乘进 `scaleX/scaleY`：

```kotlin
val scaleX = pose.scaleX * recentsScale * (if (cropApplied) 1f else pose.cropWidth)
```

**关键**：`motionScale()` 在 `nativeApplied` 时直接返回**图标真实 scale**（`DockNativeMotion.scale()`
优先返回 `lastSample.scale()`），这个值在**拖动段**（`layer.overview == false`）也非 1。
⇒ 拖动过程中每一帧都会走 `applyRevealContainer` → `setMatrix`。

`MEMORY.md §4` 已记：**对 dock 层 `setMatrix` 会让 glass/blur 材质重采样**，
`DockUnlockReveal.GROW` 就是因此固定 0 的。

**功耗角度**：`setMatrix` 非 1 ⇒ SurfaceFlinger 每帧对该层做一次**纹理重采样**（blur 层还要
重算模糊采样区）。帧循环是 `FRAME_INTERVAL_MS = 8`（≈120 Hz），`nativeSampleDeadlineNs`
存活期间 `needsFrame` 恒真 ⇒ **整段手势期间 120 Hz 重采样**。

**这是「视觉正确性优先」的有意取舍**（commit 主题就是「align backdrop with icon motion」），
但**材质重采样的功耗代价在 commit message 和 handover 里都没有记一笔**，而它恰恰是本项目
历史上被单独处理过的功耗项。

**建议**：不要求改代码，但应在 `docs/handover/OS4_INDICATOR_MARGIN.md` 或
`DOCK_POWER_PLAN.md` 里**补一条记录**：拖动段引入 matrix 缩放 ⇒ 已知会产生材质重采样，
若后续测到 dock 手势期间 GPU 功耗异常，**第一个怀疑对象就是这里**（以及
`setMatrix` 的幅度是否与图标严格相等）。

### P5：位移上限 20dp → 31.2dp

```java
public static float relativeOffsetY(float density, float progress) {
    return 8f * density * Math.max(0f, Math.min(1.2f, progress));
}
```

测试锁定：`progress=1.0 → 26f`、`progress=1.2 → 31.2f`（density 3.25）。
旧实现 `-Math.min(baseY, LIFT_DP * density * progress)` 的上限是 20 × 3.25 = **65 px**，
但 `progress` 被 clamp 到 1.2 且实际稳态 progress 会回落到 0（§3「lift 是过渡尖峰」）。

⇒ 新实现的**稳态位移略大**（31.2 px vs 旧的「名义 65 px 但实际不达」），
每帧多移动约 1–2 px。`setPosition` 的增量成本与位移量无关（都是 1 次 transaction 写），
**因此 P5 实际上不构成新增功耗**，仅记录在案以免误判。

---

## 7 P6 — endpoint 的 provider 轮询（`7026fb395`，已缓解）

`HomeLayoutNativeEndpointOS4.ensureRefresher()` 是一个 daemon 线程：

```java
period = refreshFromProvider() ? PREFS_REFRESH_MS : Math.min(period * 2, MAX_REFRESH_BACKOFF_MS);
Thread.sleep(period);   // 1500ms → 30000ms
```

**做对的地方**（本轮的加分项）：

1. **失败退避**：1500 ms 起，每次失败 ×2 到 30000 ms 封顶。app 被 MIUI freezer 冻住时，
   从「永久 12 calls/s」降到「1 call / 30 s」。
2. **失败只试第一个 key**（注释明确写了原因）：避免 19 个 key × 每次失败 = 19 次跨进程
   IPC + 19 条 Binder 栈回溯。
3. **成功路径 `values.isEmpty()` 也返回 false** ⇒ 空答复同样触发退避，不会形成忙等。
4. 线程 `setDaemon(true)`，不阻塞进程退出。

**残留成本**：健康状态下 1.5 s 一次、一次最多 19 条 `resolver.query()`（每条一个 ContentProvider
IPC）。这是 **app 进程**（`com.sevtinge.hyperceiler`）的成本，不是 system_server 的。
19 次 ContentProvider 查询 × 0.67 次/秒 ≈ **12.7 次 IPC/秒**——在 app 进程空闲时，
这足以让 app 进程**无法进入深度 idle**（每次 IPC 都会唤醒它的 Binder 线程）。

**判断**：这属于「设置页就绪性」的必要成本，但 `PREFS_REFRESH_MS = 1500` 对「用户改完设置
期望多快生效」而言**过于激进**。`HomeDockWindow` 侧的对应值是 `NATIVE_BIND_SWEEP_MS = 1000`
且**有 idle backoff**；endpoint 这条**没有 idle backoff**（只有失败退避）——
即使一个值都没变，也是恒定 1.5 s 一轮。

**建议**（低优先，不建议本轮改）：成功路径也加 idle backoff——
若 `values` 与上一轮完全相同，则 period ×2 到 30000 ms；任意值变化立刻回 1500 ms。
这样「改了设置 1.5 s 生效」的特性保留，而静置时 IPC 降到 1/30 s。

---

## 8 P7 — `hc_layout_apply_now()` 的 per-capture 调用（继承）

`capture_trampoline` 每次捕获都调：

```asm
bl hc_layout_apply_now     // → hc_layout_record_caller() + apply_knob_fields()
```

`apply_knob_fields()` 遍历 8 个 knob，每个读 `path`（acquire 语义）、`delta_dp`，
然后若干字段读写。**这是既有设计**（`3d3e0e766` 只把 `delta_px` 改名 `delta_dp`）。

`hc_layout_record_caller()` 是 256 槽的 caller 直方图，用于校准；
**它也是无条件执行的**（`if (caller == 0) return;` 之外无门控）。
成本很低（一次 hash + 一次 `++`），但和 P3 一样属于「校准设施在生产路径上」。

⇒ 与 P3 合并看待：**校准插桩应当在功能正式启用后清理或门控**。

---

## 9 未发现问题的部分（明确记录）

| 检查项 | 结论 |
|---|---|
| 新增 wakelock / `PowerManager.WakeLock` | 无 |
| 新增 `AlarmManager` / `JobScheduler` | 无 |
| 新增 `setRepeating` / 无限 postDelayed 无 backoff | 无（`scheduleNativeBindSweep` 有 doze 门 + idle backoff，`0f912b997` 已修） |
| 帧循环泄漏（`scheduledFrameEpoch` 不复位） | 无（`clearScheduledFrame` 与 epoch 校验完整） |
| doze 门控被破坏 | 无（`layout_panel_refresh_and_check()` → `dock_motion_screen_active()` 仍在） |
| worker 在 disabled 时的常驻 | 无（`if (!grid && !any_knob && !top_probe && !any_tweak) return attempt_finished();`） |
| `Container.build` 探针刷 logcat | 无（`ordinal < 12` 门） |
| provider 失败时每次遍历 19 个 key | 无（已改为只试第 1 个） |

## 10 建议的处理顺序

1. **P1（workspace 宏加 delta 门控）** — 5 条指令，换掉每次布局 704 B 搬运。**唯一确定该做的。**
2. **补文档**：把 P4 的「拖动段 matrix 缩放假定会重采样」记进 dock 功耗计划。
3. **P2（删 `Container.build` 死分支）** — 3 处删除，出厂无功耗影响，
   纯属清理 + 防止 debug 通道复现时污染 slot 2 计数。可顺手做，不急。
4. **P6 的 idle backoff** — 需要设备侧验证生效延迟，放到有设备时再做。
5. P3/P7（hits / caller 直方图）— 归入「校准设施清理」一次性处理。

---

*本报告为静态分析产物。所有「量级估算」标注为估算，未经设备实测；
标注「待验证（需设备）」的条目在得到真机数据前不应作为改动依据（项目铁律 §0）。*
