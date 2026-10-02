# NativeHookRuntime

HyperCeiler 的统一 native hook 运行时。它由两部分合流而成：

```
MiuiBackGestureHook                      HyperCeiler（本仓库）
成熟的 ELF / dynsym / PLT-GOT /         成熟的 mapping / snapshot / generation /
ARM64 运行时解析能力                     continuation 校验 / health / re-arm /
（Apache-2.0，见 THIRD_PARTY_NOTICES.md）  Dart AOT hook 生命周期
                    \                    /
                     ↓                  ↓
                  统一 NativeHookRuntime（app/src/main/cpp/nativehook/）
```

## 1. 职责模型（不可混淆）

```
Resolver        = 找到「Hook 谁」，并给出可验证的证据
ABI/Trampoline  = 决定「怎么正确接管」（寄存器/栈/对象布局约定）
NativeHookRuntime = 负责「怎么安全安装、维护、检测失效和恢复」
```

**Resolver 不得安装任何东西**：它不调用 hook 后端、不 mprotect、不写内存。
它返回 `nhk::ResolvedTarget + nhk::ResolverEvidence`，由 runtime/backend 安装。
这条边界是「换一个目标只研究目标函数」的前提。

## 2. 目录与职责

| 文件 | 职责 |
|---|---|
| `nhk_base.h` | 溢出检查、有界范围、LE 读取、页工具 |
| `native_image.h` | `/proc/self/maps` 清点、容器归属（裸文件 / ZIP / 内嵌 ELF）、代际、mapping-state 比对 |
| `elf_image.h` | ELF64 解析与查询：PT_LOAD/PT_DYNAMIC/dynsym/strtab/SYSV+GNU hash/JMPREL/RELA |
| `arm64_decode.h` | 通用 ARM64 解码：ADRP、ADD imm、LDR64、BL/B/B.cond、PLT stub、MOVZ/MOVK、UBFX |
| `resolver.h` | 契约类型与 `evidence_acceptable()` 门 |
| `hook_bank.h` | inline 槽位状态机：安装 / 恢复补丁词 / re-arm / 健康 / 卸载 |
| `inline_hook_backend.h` | LSPosed `NativeAPIEntries`（hookFunc/unhookFunc）边界 |
| `got_hook_backend.h` | caller-specific PLT/GOT hook：唯一性门、RELRO、mprotect 临时加写+恢复、原子替换、全成或全滚回、健康模型 |
| `page_guard.h` | 可选的页生命周期策略：受保护页注册 + MADV_DONTNEED 分段转发 |
| `THIRD_PARTY_NOTICES.md` | Apache-2.0 派生说明 |

## 3. 解析优先级（统一顺序）

```
dynamic/exported symbol（GNU hash → SYSV hash → 线性回退）
  ↓
Rust symbol（mangled 名按字面精确匹配；本项目不做 demangle）
  ↓
caller-specific PLT/GOT relocation（唯一 slot，多候选即失败）
  ↓
target-specific ARM64 semantic resolver（代码形状 + call ordering + import anchor）
  ↓
validated profile/RVA fallback（必须同时验证 image identity、fingerprint、原始指令、当前代际）
  ↓
fail closed
```

**默认不要 inline hook Rust 内部函数**：若真正要改的是某个 Rust caller 对
imported C 函数的调用，用 caller-specific PLT/GOT hook，而不是全局 hook 外部函数，
更不要全局 hook `dlopen`/`android_dlopen_ext`/`dlsym`。

## 4. Fail-closed 规则（逐条对应代码）

| 规则 | 落实处 |
|---|---|
| 0 候选失败，1 候选继续验证，多候选默认失败 | `resolve()` 的 `candidate_count`；`unique_slot()` |
| 多候选必须结构化消歧，诊断文本不算证明 | `evidence_acceptable()`（要求 `candidate_count == 1`，或 ≥2 时 `disambiguators` 非空） |
| 禁止「取第一个像的」 | `GotHook` 安装要求所有 slot 指向同一 original |
| 损坏/截断 ELF 不得越界 | `ElfImage` 全程 `in_image`/`contains` 校验；**校验失败的表在解析期即被禁用**（清指针），查询期再验一次 |
| hash 链不得成为无限循环 | GNU 链按表容量设预算，SYSV 链按 `nchain` 设预算 |
| 地址域必须一致 | ELF 层一律使用**虚拟地址域**（p_vaddr / st_value / r_offset）；文件偏移仅在 `file_offset_to_vaddr()` 内出现 |
| live 与 snapshot 不一致不安装 | 宿主（feature）的 `read_slot` 必须做双清点校验 |
| 补丁范围必须先受页保护 | `InlineHookHost::protect_range`（安装前调用，失败即拒绝） |
| continuation 为 null 不启用 | `install_slot()` 写回原 prologue 并拒绝 |
| 第三方未知修改不覆盖 | `ensure_slots_live()` 遇 foreign words 直接返回失败；`uninstall_slot()` 拒绝非本方的槽位 |
| GOT 部分写失败必须回滚，且回滚结果可见 | `write_all_or_rollback()`（含失败槽位本身；`rollback_clean` 上报） |
| 页表满必须报告 | `add_protected_page()` 返回 bool；调用方拒绝该目标 |
| replacement 可达前其转发目标必须已发布 | `install_madvise_guard(..., publish_original)` 先发布再写 GOT |
| 权限必须恢复 | `write_pointer()` 用原保护位 mprotect 回去 |
| 代际变化即失效 | `mapping_state()` + 宿主 `stable_read` 双重校验 |

## 4.1 地址域与快照（重要）

`ElfImage::make(base, span)` 的语义是：**`span[0]` 就是镜像虚拟地址 `base` 处的字节**，
镜像内部一律以虚拟地址（RVA）表达。

- **文件视图**：把文件内容读成 span。仅当各 PT_LOAD 的 `p_vaddr == p_offset`（正常链接的
  Android `.so`）时两者等价；否则解析会因 `file_offset_to_vaddr()` 落空而**拒绝**，不会误读。
- **内存视图**：`image_view_from_segments()`（`file_image_view` 的正确版本）按程序头把每条
  运行时映射放回虚拟地址域，并给出 `needed_vaddr`（快照需要覆盖到多高）。调用方按地址把这些
  范围读进一个 buffer（空洞留 0），再用 `ElfImage::make_with_segments(base, buffer, segments)`
  解析——段表由调用方提供，避免两处解码漂移。

**为什么必须用程序头**：真实的 `libhyper_os_flutter.so` 四个 PT_LOAD 的
`p_vaddr - p_offset` 分别是 `0x0 / 0x10000 / 0x20000 / 0x30000`——不相等也不恒定。
任何「`begin - file_offset` 恒定」的假设都看不到这个镜像。归属判定因此改为：
每条映射在所有段上求候选 load bias，全集合**唯一一致**的 bias 才被接受；
bias 一确定，`vaddr = begin - bias` 即唯一（同一文件页被两个段页对齐覆盖不是歧义）。

**快照大小**：`needed_vaddr` 覆盖到最高段末尾。该库的 `PT_DYNAMIC` 在 vaddr `0x1101638`
（约 17.8 MB），所以任何「前 N MB 前缀」的常量都会漏掉动态表。预算
（`kGuardImageBudgetBytes = 64 MiB`）是**拒绝阈值**，不是截断阈值：超预算明确拒绝并记日志，
绝不假装前缀够用。

## 4.2 AArch64 重定位编号（易错点）

```
R_AARCH64_GLOB_DAT   = 1025 (0x401)
R_AARCH64_JUMP_SLOT  = 1026 (0x402)   ← 不是 0x403
R_AARCH64_RELATIVE   = 1027 (0x403)
```

`JUMP_SLOT` 写成 `0x403` 会把所有 PLT 导入识别成 RELATIVE 而**静默丢失**。这个错误在
自洽的合成 fixture 上完全看不出来（fixture 用什么数字，解析器就用什么数字），只有真实镜像
能暴露——所以测试里直接断言这三个常量，并有一例「RELATIVE 不算导入槽位」。

## 5. 为新目标接入（示例：Rust App B）

新增一个 Rust App 只需要四个文件级产物，**不改 Core**：

1. **Module Selector**（feature 层）：决定在哪个进程、哪个 `.so`、哪个代际上工作。
2. **Resolver**：用 `nhk::elf::ElfImage`（符号 / GOT slot）和 `nhk::arm64`（形状）描述
   该目标稳定的语义特征，返回 `ResolvedTarget` + `ResolverEvidence`。
   ```cpp
   // 语义化：唯一调用 importA 再调用 importB、且两者之间无分支的函数
   auto slot_a = image.unique_slot("some_import_a");   // 0 或 ≥2 → 失败
   auto matches = scan_functions(code, [&](const Match &fn) { ... });
   if (matches.size() != 1) return {};                 // fail closed
   return ResolvedTarget{TargetKind::kInline, fn.address, original_words};
   ```
3. **ABI / Trampoline**：明确该目标的调用约定。
   - `extern "C"`：普通 typed C/C++ replacement 即可；
   - Rust internal / Dart AOT：必须由 feature 掌握真实 binary ABI，提供 ARM64 汇编跳板，
     只操作确认过的寄存器、栈槽和对象布局；
   - Unknown：**拒绝** typed replacement。
4. **Replacement**：业务逻辑本身。

然后在 feature 的维护循环里：
```cpp
nhk::InlineHookHost<4> host = make_host();          // 内存访问 + 日志
nhk::LsposedInlineBackend::instance().attach(host);
if (!nhk::ensure_slots_live(bank.slots, host, order)) { /* channel stays down */ }
bool healthy = nhk::slots_healthy(bank.slots, read_all);
```

## 6. 现有消费者

- **桌面 Dart AOT Hook**（`dock_native_*.h/cpp`）：第一个 consumer。
  - mapping/代际/容器/槽位状态机全部走 Core；
  - Dart 专属部分保留在 feature：`is_dart_prologue`、压缩指针约定（x28 高 32 位 + LSL #32）、
    allocation TagAbi、scale/animate/set 语义链；
  - `resolve_hook_targets()` 输出契约结构（`ResolvedTarget×3` + evidence），
    `resolve_generation()` 会校验契约里的 original words 与 live 校验读数一致。
  - 页生命周期：`maybe_install_madvise_guard()` 对 `libhyper_os_flutter.so` 的
    `madvise` import 装 GOT hook（**该决策在 feature，不在 Core**）。

## 7. 并发模型

- 所有 bank/slot 变更在 feature 的 `hook_mutex` 下进行（桌面为 `motion_worker` +
  `health_worker` 两个线程，健康检查 250ms / 不健康 2s）。
- Core 自身只做状态机与数据校验，不加锁；所有共享状态由 feature 持有并加锁。
- GOT 安装/恢复必须在同一条串行路径上完成（桌面挂在同一 health worker 上），
  避免同一 slot 被两个线程同时改写。
- 长时间扫描（ELF 解析、语义匹配）在快照上进行，**不持有 WMS 全局锁**。
- `uninstall_slot()` 幂等：已卸载再调用是成功 no-op，便于清理路径重入。

## 8. 页生命周期策略

`page_guard.h` 是**可选**策略，不是所有 hook 的强制依赖：

- 普通 native 库默认只用 generation/health/re-arm 机制；
- 运行时会对自己代码页发 `MADV_DONTNEED` 的 Runtime（HyperOS Flutter 等）才需要
  `install_madvise_guard()`：它 hook 该库的 `madvise` import，把 discard 请求
  从受保护页两侧分段转发，保护 trampoline 与 GOT slot 不被回收。
- 顺序：**先装 guard，再登记补丁页、再装 hook**。`InlineHookHost::protect_range` 在
  inline 安装入口调用，页表满即拒绝该槽位；未对齐/溢出的 madvise 请求原样转发给内核，
  `length` 不足一页也按覆盖页处理（Linux 只要求起始地址对齐）。
- 并发与发布：页表用「写锁 + release 发布计数 / 读端 acquire」协议；
  `guarded_madvise` 的转发目标在 GOT 被替换**之前**发布。
- **转发目标永不撤回**：一旦发布就不再清空。即使安装失败且回滚干净，也可能有一个线程
  刚从 GOT 取到 replacement 地址、尚未读取转发目标——"回滚干净"不证明无在途调用。
- **代际身份**：每条安装记录携带 `ImageIdentity{dev, inode, load_base}`。维护时先比对
  身份，才能区分两种情况：
  - 身份仍在、槽位不是我们 → **第三方接管**：记日志（去重）、不再尝试、不覆盖；
  - 身份消失（库被重映射/卸载）→ **代际过期**：绝不写旧地址，保留转发目标，
    清记录并重新发现新代际后重装。
- **残留记录**：回滚不干净时失败请求也会产出一条 `GotHook{partial=true}` 记录
  （含每槽位是否尝试/是否写入、回滚结果、代际身份），而不是丢掉记录——没有记录的残留
  在构造上就不可恢复。残留只有在「所有记录槽位都回到 original」时才可清除。

## 9. 测试

```sh
tests/run_host_native_tests.sh [libapp.so ...]              # 直接跑
NHK_FLUTTER_LIB=/path/libhyper_os_flutter.so tests/run_host_native_tests.sh
./gradlew :app:hostNativeTests                              # 或经 Gradle
./gradlew :app:hostNativeTests -PlibappSo=/path/to/libapp.so
```

覆盖：ELF 解析 / **AArch64 重定位编号断言** / 双 hash 查找 / 坏 hash 表禁用与越界防护 /
循环链终止 / **vaddr 视图（多段不同 gap，真实库布局）** / **真实镜像端到端**
（视图 + 远超 4 MiB 的动态表 + 导入槽位，对照「4 MiB 前缀不可用」）/ 槽位唯一性与歧义 /
损坏拒绝 / GOT 原子替换与回滚（含写入中途失败与权限事务）/ 权限恢复 / 槽位状态机
（null continuation、patch lost、foreign、卸载所有权）/ ARM64 解码与通用语义 resolver
0-1-多候选 / 页保护分段、未对齐透传与页表满 / Dart 契约结构。

`DockNativeArm64Test` 需要 **Linux + aarch64**（eventfd + 真实执行 AArch64 指令）。脚本按
OS 与架构双重判断后**打印 SKIP 原因**；CI 的 x86_64 runner 同样跳过，即该用例当前**没有 CI
覆盖**——这是已知缺口，不以「CI 全绿」代替。

## 10. 已知限制

- `unknown ABI` 目前拒绝 typed replacement；需要时由 feature 提供跳板。
- GOT 后端的设备侧消费方目前只有 madvise 防护（其余是宿主测试）。
- 语义 resolver DSL 尚未引入：先以显式函数实现，等出现第三个目标再抽象。
- **madvise 防护的真机行为未验收**：视图/快照/解析/槽位识别已用真实库离线验证，
  但「设备上确实挡住一次真实 MADV_DONTNEED」需要 arm64 设备观察日志
  `madvise guard installed; N slot(s) protected`。
- 页保护的镜像选择目前只支持**裸文件路径**的运行时库；映射在 APK 容器里的库需要走容器归属，
  尚未接入该调用链（会得到 "not loaded" 并保持 pending）。
- `uninstall_slot` / `restore_got_hooks` 已做所有权校验（非本方不写回），但「卸载时是否仍有
  在途调用」属于生命周期契约，接入清理路径时由 feature 保证。
