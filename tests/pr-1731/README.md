# PR #1731 本地编译修复与验证

起点：PR 最新 head `f521887ed2ffac4bbfeb5983175443c890ff85af`，远端分支
`LDS-XiaoYe:os4-branch`。本地修复在 `codex/pr-1731-build-fix` 隔离工作区，
未混入 `os4-personal` 的未提交改动，未推送、发布评论或修改远端 PR。

## 根因与修改范围

对比合并提交 `b88b8bf5d` 两个 parent：第一侧已有文件夹语义校验和扩展布局快照；
第二侧带来 combined 协议。合并结果保留了旧版文件夹调用表，却没有其类型和原始数组，
同时把 combined 的读取调用与缺失的状态、查询／发布实现拼在一起。
`0d4fc4ba3` 引入完整 combined 实现，`ea5db0e81` 将查询、epoch 和布局解码拆为辅助函数；
本次按这些配套实现恢复，叠加当前偏好通知保护及标题／缩放扩展，而非整文件回退。

生产修改只有：

- `app/src/main/cpp/targets/home/home_folder_geometry.h`：清理未被生产调用的旧版本调用表和
  `folder_original_body_matches`。现有 `bind_folder_geometry` 始终调用
  `folder_geometry_contract`；完整语义 profile、命名 callee、原始 helper、字段、分支目的地
  和补丁窗口校验保留，没有恢复旧函数地址或降低校验。
- `library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java`：
  恢复 PublishedSnapshot、CombinedSnapshot、预取、失效发布、epoch／revision／租约、
  typed provider 查询、统一 decodeLayout 和 readBit。查询只返回完整 ProviderSnapshot 或 null；
  decoder 返回完整扩展 Snapshot，tweak decoder 只返回 int[]。授权与布局从同一次 getAll
  发布；拒绝旧查询、查询期间的偏好变化、热重载退休和超过原始五秒租约的响应。
  数组及自定义标题记录做防御性复制。恢复 1.5 秒续期，避免原合并结果的“无限等待通知”
  与五秒授权租约冲突。通知同时使旧 combined 授权失效，legacy 布局仍保留最后完整缓存。
- `library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java`：
  补回 stringset switch 分支缺失的闭合括号，保留 text／long 元数据及标题字符串集协议。

`HomeDockWindow.kt` 无需删除或替换调用：补回真实 Java API 后已通过实际 Kotlin/Android 编译。
其余改动均是上述功能的回归测试、缺失的 host Android API fixture 和测试依赖声明。

## HCS1 协议核对

交易码 `0x00484353`，ACK `0x48435331`，长度固定 188 字节／47 个 int32 单元。
生产 native decoder 是 `targets/hyos/hyos_snapshot.h`。

| 字节位置 | 内容 |
| --- | --- |
| 0 / 4 / 8 | ACK / available / flags，各 int32 |
| 12 / 20 / 28 | epoch / revision / BOOTTIME deadline，各 int64 |
| 36 / 40 / 44 | gridEnabled / cellX / cellY，各 int32 |
| 48 | knob count = 8 |
| 52–115 | 8 组 enabled、delta，各两个 int32 |
| 116 | tweak count = 17 |
| 120–187 | 17 个 tweak int32 |

有效授权要求正 epoch／revision，以及距离当前 BOOTTIME 不超过五秒的原始 deadline。
查询耗时会消耗租约，读取／接收不会续期。明确关闭为 available=1、flags=0；
不可用为 available=0、flags=0。两者 deadline 及布局 payload 均归零，只有固定 count 保留；
不可用可携带最后一次发布身份，冷启动身份为零。错 token、非 Home caller、oneway 或多余请求数据拒绝。
HCL4 扩展布局与 HCY1 兼容接口的交易码、顺序和确认值未变；HCS1 不增加可选尾部。
自定义标题、大小文件夹／组件缩放、文件夹选项、返回手势及 Dock 标题仍由完整布局快照保留。

## 复现与验证结果

修复前，Clang 头文件检查复现 6 个未定义类型／符号错误；javac 首先复现 provider 的孤立 case
和 try/catch 语法错误。将该括号修复作为隔离前提、仍使用原 endpoint 源码再次编译，
复现 CombinedSnapshot／PublishedSnapshot 缺失、boolean 返回类型不匹配、未定义 epoch／query
以及 Snapshot 构造逻辑放错函数等错误。

2026-10-22 本机结果：

| 检查 | 结果 |
| --- | --- |
| `python3 tests/pr-1731/run_native.py` | 通过：真实 7751 完整函数及 helper，字段／镜像迁移、错误调用／分支／算术／帧／截断拒绝；1536 个文件夹位置、1000 次重建、渲染快照与拖动归位 |
| `tests/home-startup-snapshot/run_java.py` | 57 项通过，包括真实预取线程、无通知续期、通知中途失效、完整标题／独立缩放及防御性复制 |
| `tests/hyos-control/run_java.py` | 65 项通过 |
| `tests/hyos-control/run_legacy_java.py` | endpoint、10 项 idle-cache、10 项 atomic-reload 均通过 |
| `tests/hyos-snapshot/run.py` | native decoder/cache 172 项通过，Java 生产 writer 的六组报文逐字段交叉解码通过 |
| `tests/home-startup-snapshot/run_mutations.py` | 四组破坏租约／退休身份／查询顺序／关闭 payload 的代码变体均被回归拒绝 |
| `python3 tests/dock-title-os4/run.py` | 149 项解析器、26 项缓存协议、18 项 endpoint 检查通过 |
| `:app:assembleDebug` | 完整 APK 构建成功，包含真实 Java/Kotlin 和 ARM64 native 编译 |
| `:app:assembleRelease` | 完整 APK 构建成功，R8 后打包成功；APK 签名验证通过 |
| `git diff --check` | 通过 |

第一次完整构建在 settings.gradle.kts 的 GitHub Packages 凭据检查处停止，这是环境问题。
随后仅为构建子进程复用本机已有下载凭据，构建成功；没有打印凭据，也没有修改认证、CI 或签名配置。
资源格式与 MIUIX D8/R8 stack-map 等现有警告仍有输出，未阻断构建。
隔离工作区未复制发布签名，构建使用仓库原有的默认签名选择逻辑。

Release：`app/build/outputs/apk/release/HyperCeiler-2.10.166-20261022-release.apk`。
SHA-256：`c9129caa5561445e09a7e96d8b5ec13f4471546a8c61fe5ee147700c58015519`。
本机完整日志和机器报告保留在 `/tmp/hc-pr1731/`；测试输出不含构建凭据。

可重跑（JDK 21+；新输出目录）：

```sh
python3 tests/pr-1731/run_native.py
python3 tests/home-startup-snapshot/run_java.py --output /tmp/hcs-java-check
python3 tests/hyos-snapshot/run.py --output /tmp/hcs-native-check --java-corpus /tmp/hcs-java-check/corpus
python3 tests/hyos-control/run_java.py --output /tmp/hcy-java-check
python3 tests/hyos-control/run_legacy_java.py --output /tmp/hcl-java-check
python3 tests/home-startup-snapshot/run_mutations.py --output /tmp/hcs-mutations-check
python3 tests/dock-title-os4/run.py
```

`folder-7751.json` 仅为只读指令样本，来源 APK SHA-256：
`f4fe410efcd72ae30550bf6beac9a991d06d1ce82f0d51b51f644b11658e7cc8`。
fixture 地址不是生产匹配地址；生产校验仍使用当前镜像的符号与指令。

## 尚未验证

本轮没有安装 APK、重启手机、修改用户设置或构建独立 Hyos companion 安装包。
host Parcel／时钟 fixture 和 native decoder 检查不等于 Android Binder 真机通信证明。
设备验收需覆盖：system_server 与 Home 的实际 HCS1 caller／长度及首次预取；连续运行超过五秒、
长时间休眠和唤醒；偏好快速更新及 provider 死亡后的撤销／恢复；拒绝或成功热重载后的新 generation；
Dock 玻璃／标题、布局、大小文件夹与组件缩放的实际画面和交互。
