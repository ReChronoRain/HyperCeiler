# dock 解锁飞入跟随动画 — 方向反转的真实定界与两次误判

时间线：2026-09-30。当前状态：**代码在 HEAD（`95eaba580`），未改动；根因已定界，修法待实测确认。**

本文件记录这个问题上的**两次误判**与最终的正确定界。留着是因为两次栽的坑完全不同：
第一次是「把符号当证据」+「把量级差当公式错」；第二次是**被一条模糊反馈带偏，把正确的方向回退了**。

---

## 1 现象（用户逐轮澄清后的最终版本）

| 轮次 | 用户原话 | 我的解读 | 是否正确 |
|---|---|---|---|
| 1 | 「跟随动画是反着向下的，正常应该向上」 | 解锁飞入时 dock 背景与图标方向相反 | ✅ 准确 |
| 2 | 「只有重力势能（`AUTO_AIM`）这个动画跟随不会反，用其他全是反的」 | style 定界 | ✅ **准确，就是根因** |
| 3 | 「dock 跟随动画不会跟随图标变小了」 | 我的改动让背景不再随图标缩 | ⚠️ 只描述了我的改动后果，**不代表原代码正常** |
| 4 | 「重力势能动画下跟随动画是完全正常的，但是在其他解锁动画下是反向的」 | **确认：AUTO_AIM 正常，其余全部反向** | ✅ 决定性 |
| 5 | 「只有重力势能下是正常的，其他的解锁动画下是反向」 | 同上，重复确认 | ✅ 决定性 |

**教训**：第 3 条反馈我当成了「原代码是对的」，于是回退。实际它只说明**我的那次改动量级失控**，
与「原代码在其他风格下是否反向」无关。**用户的负面反馈只否证我的改动，不证成原代码。**

---

## 2 真实根因（量化）

`HomeDockWindow.revealShiftY`（`HomeDockWindow.kt:1544-1550`）在两个分支间切换：

```kotlin
private fun revealShiftY(layer: Layer, scaleY: Float, restY: Float): Float {
    if (layer.reveal.getStyle() == DockUnlockReveal.Style.AUTO_AIM && layer.parentHeight > 0) {
        val pivot = layer.parentHeight * DockUnlockReveal.REVEAL_PIVOT_Y_FRACTION   // 0.32572
        return (pivot - restY) * (1f - scaleY)                                      // ← 负、大
    }
    return DockNativeMotion.centerShift(layer.height.toFloat(), scaleY)             // ← 正、小
}
```

本机代入（launcher frame `1200×2670`，`pivot_y ≈ 869.7`，dock `restY ≈ 2350`，`h ≈ 450px`）：

| 风格 | 公式 | `scaleY = 0.82`（飞入中） | 方向 |
|---|---|---|---|
| **AUTO_AIM** | `(869.7 − 2350) · (1 − 0.82)` | **≈ −266 px** | **上推** ✅ 用户说正常 |
| **其余全部** | `450 · (1 − 0.82) · 0.5` | **≈ +40 px** | **下推** ❌ 用户说反向 |

⇒ **同一 `scaleY`，AUTO_AIM 给 −266 px（上），其余给 +40 px（下）。符号相反、量级差 ~6×。**
这与「只有重力势能正常」**完全自洽**：
- AUTO_AIM 的负位移 = 绕 launcher pivot 的残差补偿 → 与 launcher 自己的图标飞入**同向** ✅
- 其余风格的正位移 = 绕自身中心的补偿 → 与「图标根本没动」的实际情况**反向** ❌

### 2.1 为什么其余风格不该用 `centerShift`

上一轮（§3）我以为是「两个分支服务不同父变换，不能互换」。**那个结论有一半是对的，但漏了前提**：

- `AUTO_AIM`：解锁飞入**由 launcher 驱动**，是绕 `pivotPoint` 的均匀缩放；我们的层**继承了它**
  ⇒ 需要绕**同一 pivot** 做残差补偿 ⇒ `(pivot − restY)·(1−s)` 正确。
- 其余风格：**launcher 不做 unlock fly-in 缩放**（那是 AUTO_AIM 独占的联动）；
  缩放只由**模块自己**在 `applyRevealContainer` 用 `setMatrix` 施加，而 `setMatrix` 绕**图层左上角**。
  ⇒ 要让面板视觉中心不动，补偿**数学上**是 `centerShift`。

**但「视觉中心不动」不是目标。目标是「与图标一致」。** 其余风格下图标**没在动**，
面板却在做「中心补偿 + 缩放」，两者必然产生相对位移（约 +40 px 向下）⇒ 用户看到的「反向」。

⇒ 真正的判据不是「补偿公式哪个对」，而是 **「这一帧图标在哪」**。其余风格下图标不动
⇒ 面板的正确行为应是**不缩放、不补偿**（或缩放但不做位置补偿）。

---

## 3 第一次误判与回退（已发生，保留）

### 改动
把**非 AUTO_AIM 兜底分支**从 `centerShift` 改成与 AUTO_AIM 相同的 pivot 形式。

### 错在哪（**部分纠正**）
1. **把符号当证据**：观察到两分支符号相反，就认定兜底分支符号错了。
   ⇒ 事后看，**方向判断是对的**。
2. **把量级失控当成公式错**：pivot 形式在这次改动里最大到 −562 px，用户看到「不跟随变小」。
   ⇒ 真实原因是**补偿基准取错**：当时 `restY` 传的是**当前的 `y`**（已含 `motionOffset` 的 lift），
   于是 lift 被折进自己的补偿里，叠加成 −562 px。**公式本身没错，基准错了。**

### 回退（已执行）
```
git checkout -- library/libhook/src/main/java/.../HomeDockWindow.kt
git checkout -- library/libhook/src/main/java/.../DockNativeMotion.java
git checkout -- tests/home-dock-window/DockNativeMotionTest.java
git checkout -- tests/home-dock-window/README.md
```

---

## 3.5 第二次尝试（**本轮，2026-09-30**）—— 统一 pivot + 修基准

用户在第 5 轮把现象精确到「**上滑到多任务**，非 AUTO_AIM 下面板**向下**」。
这直接落到 `updateMotionFrame` 的合成式：

```
y = baseY + motionOffset + risePx(=0)          ← 上滑时 reveal 未运行 ⇒ risePx = 0
pose = containerPose(now)                       ← !running ⇒ elapsedFraction=1 ⇒ identity(scale=1)
scaleY = pose.scaleY(1) * motionScale           ← motionScale = 1 − 0.05·progress ∈ [0.95, 1]
shiftY = revealShiftY(scaleY, rest)
```

`motionScale` 是**跟手缩小**项，它让 `scaleY < 1`，于是补偿被激活：

| 分支 | 公式 | progress=1 时 | 方向 |
|---|---|---|---|
| 旧·非 AUTO_AIM | `h·(1−s)·0.5` = `450·0.05·0.5` | **+11.25 px** | **向下** ❌ |
| 旧·AUTO_AIM | `(pivot−rest)·(1−s)` = `(869.7−2350)·0.05` | **−74 px** | **向上** ✅ |
| 新（统一） | 同上 pivot 形式，**全部 style** | **−74 px** | **向上** ✅ |

**修改内容**（`HomeDockWindow.kt`）：
1. `revealShiftX/Y` 去掉 style 判断，抽出 `launcherPivotX/Y(layer)`（`parentWidth/Height > 0` 才返回 pivot），
   两支统一走 pivot 形式；父层未知时仍回退 `centerShift`。
2. **三处调用点的 `restY` 从「当前 `y`」改为「静止基准 `layer.baseY` / `x`」** —— 这是上次失败的真因。
   - `HomeDockWindow.kt:964`（traversal 静态路径）
   - `:1685`（`applyRevealPose`）
   - `:1842`（`updateMotionFrame` 帧循环）
3. `restoreRestingTransform`（`:1617`）补上同样的补偿 —— 原先它直接写 `SET_POSITION(layer.x, restY)`，
   **不带补偿却带着 `motionScale` 的 matrix**，释放时会掉一下。

**验证**：
- `:library:libhook:compileDebugKotlin` ✅
- 13 个 host 测试全过（含 `DockNativeMotion` / `DockUnlockReveal`）✅
- `assembleRelease` 见构建记录
- **真机 A/B 待做**：非 AUTO_AIM 上滑到多任务，确认面板由「向下」变「向上且贴合图标」。

**残留风险**：`motionScale` 只缩 5% ⇒ 补偿 −74 px；若图标实际上收只有 ~11 px，
会**冲过头**（方向对但幅度大）。真机若见「冲过头」，把 pivot 项按 `motionScale` 的占比缩放
（`(pivot−rest)·(motionScale−1)` 而不是 `(1−totalScale)`）即可 —— 但**必须先有实测**。

---

## 4 下一步（**必须先实测，禁止直接改**）

### 4.1 待测量的唯一判据
**非 AUTO_AIM 风格解锁飞入时，dock 面板 vs Hotseat 图标的相对 dy 随时间的曲线。**
- 若曲线显示面板相对图标**先下后上/整体向下** ⇒ 证实 `centerShift` 是反向源。
- 若曲线**≤1 px** ⇒ 现象不在 `revealShift`，转查 `applyRevealContainer` 的 `setMatrix`
  （是否该在非 AUTO_AIM 下**完全跳过缩放**，因为图标不动）。

### 4.2 最省的取证路径
1. 用现有 `reveal dbg frame yOff=` / `reveal dbg pose` / `reveal dbg layout` 日志
   （`revealDebugActive()` 打开时每 40 ms 一行）确认面板 y 的时间曲线。
2. 与**同一次解锁里图标的实际 y**（录屏逐帧量）对比。
3. 需要时再加**临时**一条 `reveal dbg shiftY=`（含 `revealShiftY` 返回值）——**属改代码，需先确认**。

### 4.3 候选修法（**未验证，勿直接套用**）
| 方案 | 内容 | 风险 |
|---|---|---|
| A | 非 AUTO_AIM：`revealShiftY/X` 返回 **0**（不补偿），保留 `setMatrix` 缩放 | 面板会绕左上角缩放，视觉中心下移 |
| B | 非 AUTO_AIM：`applyRevealContainer` **跳过 `setMatrix`**（scaleX/Y 恒 1），补偿自然为 0 | 失去该风格的容器缩放效果 |
| C | 非 AUTO_AIM：改为 `-centerShift`（反向补偿） | 纯猜，无依据 |
| D | 统一用 pivot 形式，但 `restY` 改用**静止基准 `baseY`** 并 clamp | 上一轮失败版本的修正版，需实测 |

**必须先有 4.1 的曲线才能选。** §0 铁律：不许「猜公式 → 改 → 看效果」。

---

## 5 受影响文件

| 文件 | 状态 |
|---|---|
| `HomeDockWindow.kt:1536-1550` | 两个 `revealShift` 分支 = 根因现场，**未改** |
| `HomeDockWindow.kt:1440-1505` | `applyRevealContainer`：`setMatrix` 与 `motionScale` 合成处 |
| `HomeDockWindow.kt:1646-1664` | `applyRevealPose`：调用 `revealShiftY` 的路径之一 |
| `HomeDockWindow.kt:1755-1845` | `updateMotionFrame`：另一条调用路径 |
| `DockUnlockReveal.java:112-113` | `REVEAL_PIVOT_X/Y_FRACTION = 0.5 / 0.32572`（来源已核：launcher `pivotPoint`） |
| `DockUnlockReveal.java:386-389` | `isShapeReveal` 含 AUTO_AIM ⇒ `risePx = 0`（已核） |
| `DOCK_AUTO_AIM_FOLLOW_REVERSED.md` | 上一轮的静态排查，**方向搞反了**（把 AUTO_AIM 当异常） |
| `DockNativeMotion.java:120-133` | `centerShift` / `relativeOffsetY` |

---

*结论：根因定界为「非 AUTO_AIM 风格的 `revealShift` 用 `centerShift`，与图标静止的实际不符」，
量化为 +40 px vs −266 px 的符号/量级差。修法必须由真机实测曲线决定，本轮未改代码。*
