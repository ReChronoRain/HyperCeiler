# dock 跟手动画在「重力势能」(AUTO_AIM) 下反向 — 静态排查与取证计划

> ## ⚠️ 本文档的前提是错的，已被推翻（保留仅作负面记录）
>
> 用户第 4/5 轮澄清：**重力势能（AUTO_AIM）是正常的，其余解锁动画才反向**。
> 本文档把 AUTO_AIM 当成异常方向来排查，**从标题到结论都搞反了**。
> 正确根因与量化见 **`DOCK_FOLLOW_DIRECTION_BUG.md`**。
> 下面 P1 / P2 两个候选**均不成立**（P1 的 `relativeOffsetY` 恒非负，给不出「反向」）。

时间：2026-09-30。状态：**未定位，需要真机日志。本轮只做静态排查 + 制定取证方案。**

用户原话（关键）：

> 「是在重力势能的解锁动画下，dock 跟手动画是反向的，但是这两者其实并没什么关系，
> 但是不知道为什么会有这个问题」

拆解：
- **触发条件**：解锁动画 style = 重力势能（`Style.AUTO_AIM`，偏好字符串 `elastic_burst`）。
- **现象**：dock 的**跟手**动画（上滑/概览时随手指移动）**方向反了**。
- **用户的困惑点（也是本问题的核心）**：解锁飞入 与 上滑跟手 是**两条互不相干**的路径，
  为什么选 AUTO_AIM 会让**跟手**出问题？—— 这正是要回答的。

---

## 1 静态结论：AUTO_AIM **不应该**影响跟手（所以更可疑）

### 1.1 解锁飞入 与 跟手 是两条独立路径

| 路径 | 驱动 | 代码入口 | 归属函数 |
|---|---|---|---|
| 上滑**跟手** | launcher 的 recents 手势 + native Hotseat scale 采样 | `motionOffset()` | `relativeOffsetY()` |
| 解锁**飞入** | keyguard going-away → `DockUnlockReveal` | `applyRevealPose()` / `updateMotionFrame()` | `revealShiftX/Y()` + `revealContainerPose()` |

`DockUnlockReveal` 的生命周期被 `TOTAL_MS` 与 `isRunning()`（= `armed || running`）
严格限死在一次解锁内，`needsFrame()` 到点即自行 `running = false`
⇒ 飞入状态**不可能**残留到一次普通上滑手势里。

### 1.2 全部 style 相关的代码点（穷举 `getStyle()`）

```
HomeDockWindow.kt:1537  revealShiftX()            ← 仅 reveal.isRunning() 期间生效
HomeDockWindow.kt:1545  revealShiftY()            ← 仅 reveal.isRunning() 期间生效
HomeDockWindow.kt:1607  revealContainerPose()     ← 仅 reveal.isRunning() 期间生效
HomeDockWindow.kt:2107  hasPendingMotionFrame()   ← 只影响「要不要排帧」，不改位姿数学
```

前三个都带 `reveal.isRunning()` 前置条件；第四个只决定是否 `scheduleAnimationFrame()`。
**按静态阅读，没有任何一条能把 AUTO_AIM 的选择传导到跟手的位姿上。**

⇒ 这就是用户说的「这两者其实并没什么关系」——**代码结构上确实没关系**。
所以真凶只可能藏在**共享状态**里，而不是共享代码路径。下面两条是仅有的候选。

---

## 2 候选机制（**均未证实**，按可疑度排序）

### P1（最可疑）`motionOffset()` 的 scene-3 分支劫持了跟手

`HomeDockWindow.kt:1999`：

```kotlin
private fun motionOffset(layer: Layer, now: Long): Float {
    val sample = nativeMotionEndpoint.latest(layer.nativeUid, layer.nativePid)   // ← 主 lane
    if (sample?.scene() == DockNativeMotion.SCENE_AUTO_AIM) {                    // ← scene == 3
        if (layer.nativeApplied) {
            layer.nativeMotion.reset()      // 关掉 native 跟随
            layer.nativeApplied = false
            ...
            layer.motion.finish()           // 关掉本地弹簧
        }
        return DockNativeMotion.relativeOffsetY(layer.density, layer.motion.progress(now))
    }
    ...
}
```

**问题**：这条分支在**跟手**路径里也会跑。一旦它命中，native 跟随被 `reset()`、
本地弹簧被 `finish()`，返回值不再是手指采样的位移 ⇒ 跟手**失去跟随** ⇒ 观感就是「反向/乱掉」。
而且它是**按 scene 值**命中的，与用户选的 style 无关 —— 但如果 **AUTO_AIM style 会让 launcher
额外发出 scene-3 包**（例如解锁期间 native 投影 setter 被调用），就构成了
「选 AUTO_AIM ⇒ 跟手异常」的因果链。

**已核实的部分**（静态）：module 自己的 native 写侧是干净的 ——
`dock_native_motion_arm64.S` 里 scene 写路径只产生 `0/1/2`（`mov x10,#1` / `#2`），
AUTO_AIM 写侧（`.Lunlock_publish_`）只写 `dock_auto_aim_value`，走独立的
`kAutoAimTransaction`。**所以 scene 3 不会由 module 自己的 hook 写入主 lane。**

**未核实的部分**（必须在真机看）：
- `DockNativeMotionEndpoint.receive()` 只拦「AUTO_AIM 通道却带非 scene-3」，
  **不拦「主通道却带 scene-3」**（过滤在 `:180`，方向是单侧的）。
  ⇒ 若 launcher 或旧版 native 往主通道发 scene-3，它会进 `retainPending` → `state.sample()`
  → `latest()` 返回它 → P1 命中。
- 真机上 scene-3 包到底走哪条通道、主 lane 有没有出现过 scene 3。

### P2（次可疑）`autoAimLive` / `autoAimDeadlineNs` 的跨 style 残留

`revealContainerPose()`（`:1607`）只在 `getStyle() != AUTO_AIM` 时调 `resetAutoAim()`：

```kotlin
if (layer.reveal.getStyle() != DockUnlockReveal.Style.AUTO_AIM || !layer.reveal.isRunning()) {
    if (layer.autoAimLive || layer.autoAimDeadlineNs != 0L) layer.resetAutoAim()
```

而 `hasPendingMotionFrame()`（`:2107`）在 style == AUTO_AIM 时会把
`latestAutoAim(...)` 也算进「需要排帧」。若 style 中途切换，理论上有残留窗口。
`resetAutoAim()` 本身是完整的（清 sequence / deadline / live / samples），所以这条比 P1 弱。

---

## 3 真机取证方案（下次设备在线时按序执行）

> 前置：`MEMORY.md §0` 铁律 —— **先取证，再动代码**。本轮**不要**先改任何东西。

### 3.1 判定「跟手 offset 的符号是否真的翻了」

现成日志行（已被 `noisyPrefixes` 抑制，需临时放开或直接看原始 logcat）：

```
motion position offsetY=<dy> running=<bool> visible=<bool> source=<native-layout|...>
```

它由 `recordMotion()`（`:1743`）在 `motionEndPending` 期间采样 ≤6 次。
这是**唯一能直接读出跟手位移符号**的字段。

**A/B**：
1. style = **日升**(DAYBREAK) → 上滑到概览 → 记 `offsetY` 序列。
2. style = **重力势能**(AUTO_AIM) → 同样上滑 → 记 `offsetY` 序列。
3. 两者**同号** ⇒ 跟手数学没变，用户看到的是**别的**东西（回到 §3.3）。
   两者**反号** ⇒ 复现了，进入 §3.2。

### 3.2 判定 P1：主 lane 有没有出现 scene 3

```
native motion scene=<n> scale=<s>          ← 每次 scene 变化一行
```

在 AUTO_AIM 下做一次**上滑跟手**，看是否出现 `scene=3`。
- 出现 `scene=3` ⇒ **P1 成立**，修法是把 `motionOffset()` 的 AUTO_AIM 分支
  **加 `!layer.overview` 守卫**（跟手期间绝不让 scene-3 分支生效），
  或让 endpoint 拒绝主通道的 scene-3（`:180` 的过滤补成双向）。
- 不出现 ⇒ P1 排除，转 P2 / §3.3。

也可看 native 侧计数（`HyperCeiler.DockNative` tag 的周期行）：
`dock_auto_aim_*` 一组计数器能说明 AUTO_AIM 包的收发时机。

### 3.3 若 offsetY 同号：现象可能不在跟手路径

用户说的「跟手动画反向」也可能指 **解锁飞入时视觉上随手指的错觉**，
或 `risePx`（+96dp→0，起始隐藏位姿）在 AUTO_AIM 下的表现。
此时要录屏逐帧量 **背景 vs 图标** 的相对 dy，而不是只读 offsetY。

---

## 4 本轮不做的事

- ❌ 不改 `motionOffset()`（P1 未证实，改了就是又一次「猜函数→改→看效果」）
- ❌ 不改 `revealShiftX/Y`（上一轮就是在这里栽的，见 `DOCK_FOLLOW_DIRECTION_BUG.md`）
- ❌ 不因「符号相反」下任何结论

---

## 5 关联文件

| 文件 | 关系 |
|---|---|
| `HomeDockWindow.kt:1997-2046` | `motionOffset()`：P1 现场 |
| `HomeDockWindow.kt:1604-1636` | `revealContainerPose()`：P2 现场 |
| `HomeDockWindow.kt:1743-1753` | `recordMotion()`：唯一能读跟手 offsetY 的地方 |
| `DockNativeMotionEndpoint.java:179-183` | 单侧 scene-3 过滤（P1 的漏洞点） |
| `dock_native_motion_arm64.S:175-197, 252-269` | 两条 lane 的写侧（已核实干净） |
| `DOCK_FOLLOW_DIRECTION_BUG.md` | 上一轮在此处的错误修复与回退 |

---

*本轮为纯静态排查，结论是「代码结构上两者无关，因此真凶在共享状态」，并给出可执行的取证方案。
未做任何代码改动。*
