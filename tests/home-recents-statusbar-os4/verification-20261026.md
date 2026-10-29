# 验证记录

分支为 os4-personal，本次未提交、推送或安装手机

通过的验证

- ASan 与 UBSan 原生识别检查 304 项，包括 12 组独立代码、只读数据及可写数据地址重定位，重复或缺失日志特征，错误帧和参数生产者，错误存储及控制流，错误数据段权限与 EH 编码，两份实际桌面 8583 和 8584 Rust 镜像
- 生产 Java 源状态检查 49 项，覆盖精确 UID 与 PID，源租约边界，状态位，确认修订，顺序与重放，进程更换
- 生产 Cursor 与偏好端点检查 107 项，覆盖旧列数，单次物理 getAll 的 extras，旧 Provider 回退，租约到期，开关关闭，全局授权和热重载
- 真实 Kotlin 窗口策略与模拟 WMS 和 Hook 分发检查 88 项，覆盖进入退出最近任务，横屏与独立状态位，其他应用焦点，锁屏，隐藏窗口，错误协议，开关，过期，桌面进程替换和关闭清理
- 现有 combined 188 字节协议回归 57 个测试场景全部通过
- 最终完整 assembleRelease 通过，耗时 1 分 2 秒，现有 recyclerview 依赖的 R8 栈图警告仍存在，没有本次源码编译错误
- APK v3 签名校验通过
- git diff --check 通过

最终 APK

`app/build/outputs/apk/release/HyperCeiler-2.10.166-20261026-release.apk`

SHA256

`f1eb2340d0a0820bbd08fada6fcea0f15391f6163bbffab1d3f385d0a68531a2`

真机只读核查

普通最近任务使用 Launcher 主窗口，Android 17 的 WindowState 请求可见类型、isRequestedVisible 和 InsetsPolicy 通知链已从该机 services.jar 复核。最后读取手机为 showing true 与 Dozing，没有进一步操作。

尚未验证

没有安装本次 APK，没有重启或改变设置，没有验证新 Hook 真机执行次数和实际状态栏画面。需要安装并重启加载 system_server 代码后，再检查开关两态、进入与退出最近任务、竖横屏、连续切换应用、通知栏临时展开、锁屏解锁和重启桌面的恢复。不同桌面镜像若不能唯一证明三条状态写入关系，会保留原状态栏，并记录识别失败原因。
