# OS4 最近任务隐藏状态栏

实现从当前 Rust 镜像的三个场景日志引用定位函数，用程序头与 EH 索引限定代码范围，验证 W0 布尔参数到原生全局状态的写入关系及控制流。地址和全局字节偏移来自当前指令，不使用符号表、压缩调试数据、固定 RVA 或 Dock 缩放比例。

三个 Hook 保留原函数调用，仅读取当前原生场景状态，唤醒已有 Dock 传输线程。功能关闭时不扫描 ELF，不创建新工作线程。已有健康线程完成安装与维护，配置变化通过条件变量唤醒。

独立 HCT 事务 `0x00484354` 请求体为五个 int64，依次为递增序列、CLOCK_MONOTONIC 时间戳、四位状态、修订号和相同的确认修订号。状态低三位为竖屏最近任务、横屏最近任务、独立最近任务窗口，第四位为三个 Hook 均已验证。只有全零探测允许修订号为零。回复为三个 int32，ACK `0x48435231`、配置可用、功能开启。旧端点不支持该事务时，仅关闭本功能，不重连或关闭原 Dock 通道。

系统端只接受与主桌面 WindowState 的 Session UID/PID 相同的发送者，拒绝重放、回退修订、同修订不同状态、过期、未来时间及错误长度。源状态六秒过期，偏好沿用现有五秒发布租约。偏好放在 schema 4 Cursor 的可选 extras 中，仍来自同一次 getAll，旧列数、combined 188 字节协议和 231 项布局 bank 均不改变。

WMS Getter 策略只移除前台主桌面窗口请求的 statusBars 类型，不写回客户端字段。WM Handler 在全局锁内通知当前 InsetsPolicy，退出最近任务、关闭功能、切换窗口、锁屏、租约过期及清理时回归原请求。方法通过平台类型契约校验，不使用字段地址。当前真机 Android 17 的通知接口为 `onRequestedVisibleTypesChanged(InsetsTarget,int,ImeTracker.Token)`，保留旧平台单参数 `onInsetsModified` 的唯一匹配。

运行

```sh
python3 tests/home-recents-statusbar-os4/run.py [可选的解出的 libapp_launcher.so ...]
python3 tests/home-recents-statusbar-os4/run_policy.py
JAVA_HOME=/path/to/jdk python3 tests/home-startup-snapshot/run_java.py --output /tmp/hcs-tests
```

`run.py` 覆盖原生重定位和拒绝场景、身份及租约、生产偏好发布。`run_policy.py` 编译真实 Kotlin 策略，模拟 WMS 类型、Handler 和 Hook 分发，覆盖进入退出、横屏与独立场景位、其他应用、锁屏、窗口不可见、开关、过期、进程切换和热重载清理。

2026 年 10 月 28 日补充参数转发回归，WMS 与安装边界仍为夹具，但回调执行使用实际 EzHookTool 1.3.0 和 libxposed API 102 类库。旧 DSL 的 InvocationContext 会把未修改的 int 参数也通过 proceedWith 重新提交，在模拟框架拒绝装箱替换的边界上复现 IllegalArgumentException。生产窗口 getter 现改用 OriginalArgumentsHook，未改参数走原始 proceed，原始异常保持不变，只有成功返回后才筛选状态栏可见性结果。

策略回归 2094 项通过，含 2000 次整数参数查询不重新传参、原方法调用次数及旧路径负向对照。源身份和租约 49 项、偏好发布 107 项、合成重定位与单份 8584 Rust 镜像解析共 302 项通过。本次未修改原生 resolver 或 HCT 协议。既有 runner 排除重复 ModuleLog 夹具，继续编译生产 ModuleLog。

这些测试不能替代手机上的注入、状态栏像素和手势验收。2026 年 10 月 26 日读取了桌面 8584 真机窗口与 services.jar，只读检查确认正常最近任务与桌面共用 Launcher 主窗口，没有安装、修改设置或重启。需要安装本版本并重启以加载 system_server 策略，再验证开关两态、竖横屏、返回应用、通知栏临时展开、锁屏解锁及桌面重启。安装成功、原生执行、Insets 消费和最终画面需分别确认。
