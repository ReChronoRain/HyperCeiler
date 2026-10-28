# OS4 最近任务隐藏状态栏

实现从当前 Rust 镜像的三个场景日志引用定位函数，用程序头与 EH 索引限定代码范围，验证 W0 布尔参数到原生全局状态的写入关系及控制流。地址和全局字节偏移来自当前指令，不使用符号表、压缩调试数据、固定 RVA 或 Dock 缩放比例。

Rust 三个 Hook 保留原函数调用，另从当前 Dart 镜像的 overview getter，below state 谓词，窗口初始化和状态提交函数之间的指令关系识别主最近任务场景。在原始状态写入与写屏障之后观察，使用当前 Dart 帧根和已解析的字节偏移读取压缩状态引用，只保留原生布尔值，不分配或调用 Dart。观察点的四条原始非分支指令通过尾跳继续执行，原始返回地址及 Dart GC 栈图位置保留。四个 Hook 唤醒已有 Dock 传输线程。功能关闭时不扫描 ELF，不创建新工作线程。已有健康线程完成安装与维护，配置变化通过条件变量唤醒。

独立 HCT 事务 `0x00484354` 请求体为五个 int64，依次为递增序列、CLOCK_MONOTONIC 时间戳、四位状态、修订号和相同的确认修订号。状态低三位保留原协议，主最近任务在已观察到 Dart 状态后以第一位表达，涵盖竖屏与横屏，第二位只作为初始 Rust 兼容来源，第三位为独立最近任务窗口，第四位为四个 Hook 均已验证。Dart 状态已知时不再混入旧 Rust 主场景位，读取失败发布不可用，避免退出后误隐藏。只有全零探测允许修订号为零。回复为三个 int32，ACK `0x48435231`、配置可用、功能开启。旧端点不支持该事务时，仅关闭本功能，不重连或关闭原 Dock 通道。

系统端只接受与主桌面 WindowState 的 Session UID/PID 相同的发送者，拒绝重放、回退修订、同修订不同状态、过期、未来时间及错误长度。源状态六秒过期，偏好沿用现有五秒发布租约。偏好放在 schema 4 Cursor 的可选 extras 中，仍来自同一次 getAll，旧列数、combined 188 字节协议和 231 项布局 bank 均不改变。

WMS Getter 策略只移除前台主桌面窗口请求的 statusBars 类型，不写回客户端字段。WM Handler 在全局锁内通知当前 InsetsPolicy，退出最近任务、关闭功能、切换窗口、锁屏、租约过期及清理时回归原请求。方法通过平台类型契约校验，不使用字段地址。当前真机 Android 17 的通知接口为 `onRequestedVisibleTypesChanged(InsetsTarget,int,ImeTracker.Token)`，保留旧平台单参数 `onInsetsModified` 的唯一匹配。

运行

```sh
python3 tests/home-recents-statusbar-os4/run.py [可选的解出的 libapp_launcher.so ...]
python3 tests/home-recents-statusbar-os4/run_policy.py
python3 tests/home-recents-statusbar-os4/run_dart.py --adb [可选的解出的 libapp.so ...]
JAVA_HOME=/path/to/jdk python3 tests/home-startup-snapshot/run_java.py --output /tmp/hcs-tests
```

`run.py` 覆盖原生重定位和拒绝场景、身份及租约、生产偏好发布。`run_policy.py` 编译真实 Kotlin 策略，模拟 WMS 类型、Handler 和 Hook 分发，覆盖进入退出、横屏与独立场景位、其他应用、锁屏、窗口不可见、开关、过期、进程切换和热重载清理。

2026 年 10 月 28 日补充参数转发回归，WMS 与安装边界仍为夹具，但回调执行使用实际 EzHookTool 1.3.0 和 libxposed API 102 类库。旧 DSL 的 InvocationContext 会把未修改的 int 参数也通过 proceedWith 重新提交，在模拟框架拒绝装箱替换的边界上复现 IllegalArgumentException。生产窗口 getter 现改用 OriginalArgumentsHook，未改参数走原始 proceed，原始异常保持不变，只有成功返回后才筛选状态栏可见性结果。

策略回归 2094 项通过，含 2000 次整数参数查询不重新传参、原方法调用次数及旧路径负向对照。源身份和租约 49 项、偏好发布 107 项、合成重定位与单份 8584 Rust 镜像解析共 302 项通过。本次未修改原生 resolver 或 HCT 协议。既有 runner 排除重复 ModuleLog 夹具，继续编译生产 ModuleLog。

这些测试不能替代手机上的注入、状态栏像素和手势验收。2026 年 10 月 26 日读取了桌面 8584 真机窗口与 services.jar，只读检查确认正常最近任务与桌面共用 Launcher 主窗口，没有安装、修改设置或重启。需要安装本版本并重启以加载 system_server 策略，再验证开关两态、竖横屏、返回应用、通知栏临时展开、锁屏解锁及桌面重启。安装成功、原生执行、Insets 消费和最终画面需分别确认。

2026 年 10 月 28 日 Flutter 场景补充，真机 8584 进入 overViewState 时 Rust 主场景仍为 false，已确认旧来源未覆盖实际入口。新 resolver 仅依赖当前 ELF 指令和相互调用关系，固定指令与寄存器模式是已验证的 ARM64 Dart ABI 约束，不表示跨版本保证。合成无符号 ELF，十二组独立重定位，控制流绕过与消费者生产者变异，内存异常和状态组合共 325 项通过，另验证 8583 和 8584 的真实 Dart 镜像。手机上的独立 ARM64 程序使用生产 ASM 入口，覆盖 GP 寄存器，全部 SIMD，NZCV 和两种 Dart 栈对齐，共 1062 项通过。此独立程序不能替代注入后的画面验证。接口回复只在可用或启用状态变化时记录，避免逐次 Binder 日志。

Dart 补丁字读取继续复用 Code 指令清单，不使用要求 PT_GNU_EH_FRAME 的 Rust Image 读取器，避免解析成功而安装阶段拒绝没有异常展开表的 Dart 镜像。回归同时覆盖实际补丁字提取，不只测试地址解析。场景安装阶段只在阶段变化时输出等待镜像，契约拒绝，来源代次变化或已安装的原因。

真机进一步确认，Flutter 场景观察 active 为 true，服务端策略 applied hidden 为 true，InsetsSource visible 为 false，但客户端仍持有状态栏 leash，画面仍有状态栏。补充 InsetsPolicy.areTypesForciblyHidden(int) 的类型契约观察，仅当前绑定显示的 statusBars 查询进入平台原有隐藏控制器。系统通知栏临时显示和强制显示分支仍优先，导航栏和其他显示不受影响，不修改强制类型字段。控制权与表面分离的夹具复现原请求过滤不足，新增恢复，开关关闭，跨显示和混合类型隔离回归后，共 2108 项通过。此策略需安装后重启 system_server 才能完成最终画面验收。

首次加载强制隐藏谓词后，真机仍由桌面持有控制权，需处理 ART 预编译内联。安装阶段仅对 getStatusControlTargetInner，getStatusControlTarget，updateBarControlTarget 三个直接调用者去优化，并记录成功数。策略回归增加内联谓词绕过夹具和去优化范围断言，共 2109 项通过，不去优化整类或全部 WMS。最终效果仍以更新后系统端加载和截图为准。

2026 年 10 月 28 日最终真机验收通过，桌面 8584，手机竖屏，安装 9d19dfb765cc39d938e595170cdc3cf6ad735e98b922ddc79935eef640737115 版并经用户许可重启后，实际截图状态栏消失，statusBars source=false，控制权为 HidingPermanentControlTarget。三组桌面进入退出，应用进入与返回，实际底部上滑手势均通过。关闭开关时画面显示且 source=true，控制权回归客户端，恢复开关后重新隐藏。通知栏展开显示并获得控制权，收起后恢复隐藏，导航栏保持可见。原隐藏开关 true 已恢复，手机返回桌面，未发现测试期间桌面或 system_server 新崩溃。横屏，锁屏切换和其他桌面版本的完整交互未在本轮设备验收，真实镜像解析覆盖 8583 和 8584。证据仅保存在本地 /tmp/hc-recents-statusbar-20261028，不提交个人截图。
