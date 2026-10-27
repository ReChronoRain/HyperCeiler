# OS4 性能及安装后运行时重启审计 2026-10-27

当前分支 os4-personal，保留工作区原有修改，未提交或推送。此次不操作手机，不安装或重启。

## 结论与证据边界

确认了会增加低版本或不支持镜像处理成本的代码路径，并已修复。没有拿到此次新反馈的版本和对应崩溃现场，不能把这些缺陷认定为全部掉帧的唯一原因。

### 加载回调等待后台解码

HomeLayoutEngineFindSymbol 原来以 attempted 作为允许进入条件，随后等待 g_workMutex。attempted 并不表示解析完成，后台可能仍持锁读取并解压。SymbolSpan 和 TargetImage 原来没有 loader 消费边界，也会发现镜像或等待互斥锁。实际生产函数的忙锁夹具在修复前失败。

现在 loader 回调不发现镜像，不等待锁，不调用 EnsureLoaded 或触发缓存落盘，只读取已就绪结果。后台原有解析能力保留。首次显式缓存 priming 的既有预算解析不是本次消费者守卫测试的对象，不能宣称所有冷启动路径零 I/O。

### 不支持目标重复绑定

生产启动片段在缺失目标时，knob 绑定 201 次，动画绑定 200 次，人工等待累计 10000 ms。改为各一次，恢复交给现有 worker。

生产 auxiliary 绑定前缀在 1200 次 500 ms 维护调用中，原来调用绑定函数 21600 次，修复后 414 次。这是探测调用数，不能等同于重复安装了 21600 个 Hook。已确认完整可执行镜像的特征图结果本来已有缓存，本次修复针对尚未绑定的生产者消费者校验及其周边文件和符号查询。

新增 BindingRetry 仅用于 discovery，依次退避 500 ms，2 秒，10 秒，30 秒。设置请求变化，完整镜像识别完成或进程镜像状态重置时立即清除退避。保持有效 Hook 的半秒健康检查和必要恢复，不延缓写入回滚责任，不恢复固定地址，不放宽代码校验。

没有镜像或配置时，旧 monitor 会长期维持 50 ms 节奏，现改为已有的 1500 ms 节奏，目标库及配置通知仍可立即唤醒。没有打开实验性 field-write 通道时，不再在维护循环里尝试该通道的 capture。

### 安装与热替换

全仓检索没有发现安装广播触发 zygote 重启，唯一 setprop ctl.restart zygote 位于 DialogHelper.showRestartZygoteDialog 的用户确认回调。module.prop 为 autoHotReload=true，因此更新会申请热重载，但不能把申请或返回 false 等同于模块执行了重启。

libxposed API 102 明确规定返回 false 取消热重载，返回 true 前应停止模块常驻 native 线程及撤销 native Hook。当前桌面 native runtime 没有完整退出接口，SDK 37 以外原来仍可能允许这类代际退役。已增加 native load claim 检查，保留当前代际并拒绝危险热替换，日志明确 automatic_restart=false。SDK 37 和 OS4 的原有资源保护保持，其他未加载桌面 native runtime 的平台和进程保持原事务。此修复不声称能够解决所有 system_server 资源崩溃。

官方接口 https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.html

重新读取先前提供的 LSPosed_20261026_142550.zip，tombstones/tombstone_22 记录 2026-10-26 14:04:14 system_server SIGABRT，ByteBucketArray.editItemAt(index=18446744073709551615) 检查失败，堆栈位于 AssetManager 资源配置及窗口收尾。AOSP 在 system_server 退出时会结束 zygote，让 init 拉起新运行时，因此系统资源崩溃能够产生类似反馈。该日志没有证明本次新反馈的安装时间，不能确认此崩溃由 HyperCeiler 引起，也不能把桌面探测重试直接当作 zygote 重启原因。

系统机制 https://android.googlesource.com/platform/frameworks/base/+/master/core/jni/com_android_internal_os_Zygote.cpp

## 修改文件

- app/src/main/cpp/targets/home/tweaks/entry.cpp
- app/src/main/cpp/targets/home/home_layout_hooks.cpp
- app/src/main/cpp/targets/home/home_binding_retry.h
- library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/XposedInitEntry.java
- library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/other/NativeHomeHooksOS4.kt
- tests/home-render-performance 下新增 loader_lookup.py，binding_retry.py，hot_reload.py，并接入 run.py

## 验证

修改前复现日志在 /tmp/hc-performance-audit-20261027 的 loader-repro.log，binding-repro.log 和 reload-repro.log。

- 新增生产函数回归全部通过，覆盖忙锁，未就绪索引，worker 保留解析，缺失依赖慢节奏，启动失败一次尝试，辅助探测退避及显式重置，时钟零值及溢出，原生代际保留和其他进程原事务
- home-render-performance/run.py 全部通过，含冷暖 property 各 10000 次不触发 raster 初始化，以及现有 bootstrap，原生 primer 和 drawer 回归
- hook-diagnostics/run.py 全部通过，含 55 项规则，实际观察器，导出，原生 health，监控采样及展示
- 完整 Release 构建通过，1 分 21 秒，签名验证通过，git diff --check 通过
- APK SHA256 d45edf23ed9c1a72f3d037936c86462bd1f21043b73d8005c9aecca6f77ca62c

本轮没有真机实测，未声称已改善某个百分比帧率或根治所有卡顿。必要设备验收为低版本桌面冷启动及特征失败后的空闲 CPU，长时间后台和恢复，缩放及文件夹与最近任务连续中断，安装更新前后 system_server 与 zygote 的 PID 和崩溃时间线。测试应保留匹配运行 Build ID 的二进制，配合 FrameTimeline 或 Perfetto，而非仅凭 Hook 安装日志认定流畅。
