# OS4 最近任务隐藏后台卡片

沿用系统桌面，最近任务，隐藏后台卡片的应用选择入口与 home_recent_hide_card 键。OS4 的实现位于系统界面作用域，页面说明提示所需作用域。OS2 和 OS3 的 HideRecentCard 保持原实现。

## 实际路径与运行时识别

在当前 launcher 8584 的静态分析副本中，任务提供者调用导入的 IRecentTasks_get_recent_tasks_api36，再接收 SystemUI Shell 返回的分组任务数组，旧 Java 桌面的 needRemoveTask 不参与这条路径。

只读取得手机 PackageManager 所列的 SystemUI 共享库，核对实际 Miui-WindowManager-Shell.jar，SHA256 为 6a03b772d0fbf16b9305f34da4c2006dec8d5cef66c4df99810c2fad0771eeb6。当前厂商优化把 IRecentTasks 的方法移到了抽象 Stub，原接口只保留标记；IRecentTasksListener 的方法也可能只在实际 Stub 或 Proxy 上可见。getRecentTasks 仍是三个 int 参数和 GroupedTaskInfo 数组返回，组提供 getTaskInfoList，成员继承 android.app.TaskInfo。

生产代码从标准 Shell AIDL 的实际 Stub 方法解析参数和数组返回类型，通过 Stub 构造时的真实子类定位具体实现。没有用固定 RVA，私有实现类名，版本分支，其他模块的 Hook 函数或静态符号选择目标。组成员 getter 与 TaskInfo 的公开组件语义在当前类上校验，回调则从实际本地 Stub 或远程 Proxy 解析。这里使用的是 Android Binder 与公开任务对象契约，不涉及 Dart 对象布局或 ABI 分配，不把动态识别当作所有 ROM 自动兼容保证。

只有原权限检查及任务查询成功，且入口调用 UID 属于桌面，才过滤返回的卡片副本。其他调用方，空选择，未知类型，无效数据和原查询异常保留原返回或原异常。没有调用 removeTask，forceStop 或杀进程接口。保留项的顺序，对象和原始数组均不修改。分屏和自由窗口组合是整张卡片，任一成员属于所选应用时隐藏整组，避免生成缺成员的损坏组合卡片。页面说明明确展示分屏整组语义。

## 配置与生命周期

Provider 从现有单次 getAll 快照追加可选 cursor extras，版本 hc_recent_cards_version 为 1，home_recent_hide_card 使用已有长度编码格式。固定 schema 4 和原列，native Binder 回复，combined 长度，缓存格式与 system_server 接口均不变。旧 Provider 或非法可选选择只使本功能不可用，不删除其他配置。

系统界面注册已有设置 Provider 的 ContentObserver，使用项目共享 ThreadPoolManager 后台读取，没有新增常驻线程或定时轮询。读取结果以不可变集合发布，修订号阻止并发变更时旧读覆盖新值，设置变化先使旧选择失效。卡片查询只使用内存快照，在原调用前后确认同一选择。已知有效配置的 10000 次查询不触发额外 Provider 读取，排队任务或重复日志。首次不可用的读取只在后续卡片请求中限频触发后台恢复，不阻塞查询。

选择变化通过桌面实际注册的 IRecentTasksListener 通知其刷新列表。监听器按真实调用 UID 隔离，注销和 ExternalInterfaceBinder.invalidate 时清理，桌面重连不重装同类方法 Hook。热重载清理会撤销设置观察者并停止本代处理，观察者注册和清理并发时也不会留下迟到注册。Hook 的实际撤销仍由项目 BaseHook 的生命周期负责。

日志标签 HyperCeiler.RecentsCards，进入内置日志的既有模块 Logcat 导入路径。成功筛选只报告首次，失败按状态限频，不把这条 SystemUI 日志伪装成桌面 native 采集证据。

## 验证

```sh
python3 tests/home-hide-recent-cards-os4/run.py
python3 tests/home-infinite-scroll-os4/run.py --image /path/to/current/libapp.so
python3 tests/hook-diagnostics/run.py
```

主回归编译实际 Provider，筛选器和 SystemUI Hook，Android 与 Hook 安装边界用可观察夹具替代，回调执行使用实际 EzHookTool 1.3.0 与 libxposed API 102 类库。覆盖厂商标记接口优化，动态改名实现，实际 Proxy 回调形状，普通及分屏组合，旧双 getter，空及全选，未知任务，原始数组身份，其他 UID，其他用户的桌面 UID，权限异常保持，设置更新与原方法并发，旧读取失效，Provider 不可用恢复，监听器注销和重连，数据预算及观察者注册与清理并发。

主回归 10046 检查通过，其中包含 10000 次热查询无额外 I/O 或日志刷屏。关联布局原生 4661 场景和 Provider/JVM 52 检查通过，诊断回归通过。关联布局 runner 修正了重复包含 ModuleLog 类的夹具收集问题，生产日志实现保持不变。

真实手机验证已完成基础隐藏与恢复，使用 HyperCeiler 自身作为所选应用，默认可见，选中后最近任务画面移除该卡片，取消选择后卡片恢复。系统界面日志确认选择计数 1 再到 0 以及实际筛选执行。隐藏期间系统任务 6389 和应用进程 28734 保留，桌面 6278，zygote 1929 与 system_server 3695 均保持。最终系统界面进程 4020 自加载修复后保持稳定，指纹解锁和应用选择，进入最近任务期间无新增 crash buffer 记录。

已通过原入口取消测试选择，恢复为空集合，其他偏好与测试前逐项对比无变化，已返回桌面。实际分屏组合，工作资料和桌面重连仅有 host 回归覆盖，未在本次手机测试中额外创建。

最终修复 Release 完整构建通过，1 分 14 秒，签名校验通过，APK 与设备 base.apk 的 SHA256 均为 34656320a601bd81657172852f7204144a4bdebd0e2c4e7920d908035d77d535。参数转发与隐藏卡片 10046 检查，手势状态 63 检查以及诊断回归通过。测试及设备证据保存在 /tmp/hc-hide-recent-cards-20261027。当前修改留在 os4-personal 工作区，未自动提交或推送。

## 真机发现的参数转发兼容问题

2026-10-27 的初次设备验收中，指纹解锁触发系统界面 miui-touch-fod 线程异常，框架在 InputEventReceiver.dispatchInputEvent 的 proceedWith 路径把未修改的 Integer 参数拒绝为不匹配的 int。系统界面被拉起后重新显示锁屏，zygote 与 system_server 未重启。EzHookTool 的 InvocationContext 对实例方法总是重新提交参数，即使回调未修改参数。

新增 OriginalArgumentsHook，使用同一库的 BeforeChainStage 和 AfterChainStage 保持回调语义，只在 receiver 与参数逐项身份均未变化时调用原 Chain.proceed，显式修改仍走框架的替换参数路径。应用于 OS4 快速切换的观察回调和本次三个整数参数的最近任务查询，未改动全局 Hook 安装或其他模块。实际依赖回归要求未修改参数不进入 proceedWith，覆盖主动替换，提前返回及原异常身份，10000 次真实回调同时复测。

## 补充设备复测

同日继续执行三轮完整应用选择与取消选择，选择及取消后都重新进入最近任务，另追加三次保持隐藏配置时退出桌面再进入最近任务，以及两次把已隐藏应用重新打开到前台再进入最近任务，最后取消选择并检查恢复，总计 12 次最近任务进出。截图复核选中时卡片隐藏，取消后卡片恢复，重新打开已隐藏应用仍保留筛选效果。每轮同时核对物理偏好和界面复选框。

测试结束全部偏好与本轮测试前逐项一致，返回桌面。系统界面主进程 4020，桌面 6278，zygote 1929 与 system_server 3695 均未变化，系统界面 crash buffer 为空。没有为此重启进程或修改系统设置。本轮未进行冷启动，设备重启，真实分屏组合和多用户卡片验证；设备当前只有主用户和 XSpace，没有现成工作资料可用于验收。
