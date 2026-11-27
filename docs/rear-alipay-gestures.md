# 系统功能组件：背屏支付宝手势

入口：**HyperCeiler → 系统功能组件**。参照桌面手势、耳机状态自动切换，将跨作用域功能集中在一个页面，并提示相关作用域。需要在 Xposed 管理器中勾选**系统框架**和**系统功能组件**。

## 使用

两个功能默认关闭，关闭时不安装对应 Hook。首次使用需要手动启用，更改功能开关后重启以应用更改。页面右上角复用 `DashboardFragment` 的 `quick_restart="system"`，通过现有确认对话框重启整个设备。

- **双击电源键**：先将系统动作设为“公交/Mi Pay/门卡”，再启用“同时在背屏展示支付宝码”。启用后出现下拉列表，可选付款码或乘车码；保留主屏卡包，追加背屏支付宝码，需要解锁时使用系统的等待解锁流程。
- **背部轻敲**：启用后，在系统“轻敲两下”“轻敲三下”列表中增加“支付宝付款码(背屏)”和“支付宝乘车码(背屏)”。普通付款码选项保持原行为。
- 两项功能分别提供进入对应系统设置的快捷入口。

关闭背部功能前，先将系统轻敲动作改回普通动作或“无”，再关闭开关并重启。整体停用可在 Xposed 管理器中取消相关作用域或禁用模块。

## 配置与实现

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `securitycore_power_rear_code_enable` | `false` | 双击电源键追加背屏支付宝 |
| `securitycore_power_rear_code_action` | `launch_alipay_payment_code` | 付款码或 `launch_alipay_bus_code` 乘车码 |
| `securitycore_back_tap_rear_alipay_enable` | `false` | 背部轻敲列表与执行逻辑 |

SecurityCore 提供原生设置列表，system_server 执行手势。每个宿主读取同一个功能开关，入口和回调分别检查；不会从旧配置自动启用。

- `SystemFrameworkB` 仅在至少一个功能开启时初始化 `RearAlipayGestures`，关闭时不解析 OEM 类。
- 共享分派 Hook 限定 `ShortCutActionsUtils.triggerFunction(String, String, Bundle, boolean, String)`；四参数委托保留不动。背部轻敲分支单独检查背部开关。
- 电源开关开启才 Hook `PowerKeyRule.triggerDoubleClick`。先执行原始卡包逻辑，在原生手势被接受后调用 `mQuickShowCodeFunction.executeQuickShowCodeFunction`，保留系统锁屏监听、Handler 和待执行请求的生命周期。
- 背部自定义动作分别为 `hyperceiler_launch_alipay_payment_code_rear` 与 `hyperceiler_launch_alipay_bus_code_rear`，执行时转换成对应原生支付宝动作。
- 复制调用方 Bundle 后写入 `show_code_display=1`。这是 OEM 显示选择值，并非 Android 物理 display ID；保留原始 `event_data`、返回值和异常语义。
- 电源请求携带请求用户标识。等待解锁后再次检查用户、开关和所选码类型，取消已过期请求；不重试原生调用。
- `RearAlipayBackTapOption` 只进入 `com.miui.securitycore` 主进程。在原生 `f2.t.e(String[], int)` 返回的列表中，普通付款码之后插入两个背屏选项；复制列表、重复插入幂等，其他手势列表不变。

## 兼容依据

静态分析对象为 pandora 的 `OS4.0.0.32.XBLCNXM`，Android 17 / SDK 37，arm64-v8a，独立背屏设备。其他 ROM 尚未验证；框架 Hook 限制 SDK 37 并检查独立背屏能力，原生列表修改另限制 SecurityCore versionCode `40004433`。

| 输入 | 身份 | SHA-256 |
| --- | --- | --- |
| SecurityCore APK | `com.miui.securitycore`，`4.4.3.3-4-260814 / 40004433`，含 `classes.dex` | `844032C4978A49DA83B37A0DF296D8F2C7D6F0F45B1334051E86FB09B53D5C2E` |
| `/system_ext/framework/miui-services.jar` | `classes.dex`、`classes2.dex` | `8D7890CDA253D1E75DF4F04102A2E07709BF137A07D869A1DBD2B457A4004708` |

JADX 结果已与 smali 交叉核验：`PowerKeyRule$QuickShowCodeFunction.executeQuickShowCodeFunction` 支持付款码、乘车码并处理锁屏；`ShortCutActionsUtils.launchAlipayBusCode(String, int)` 检查 `com.alipay.aptrip.support_display_rear`，通过原生背屏 URI 打开乘车码；付款码使用原生付款码分支。本实现复用原生执行路径，不重新拼接支付宝 Intent。原始 APK/JAR、反编译输出和设备日志不随代码提交。

## 验证

从仓库根目录运行主机检查（需要 JDK 25；脚本也接受 `-JavaHome <JDK>`）：

```powershell
./tests/test-rear-alipay.ps1
./gradlew.bat testDebugUnitTest assembleDebug
./tests/verify-rear-alipay-apk.ps1 -Apk <APK>
```

- 主机脚本已通过 47 项路由/异常检查和 15 项原生列表检查，另验证页面默认值、入口、下拉、重启目标、双语资源、作用域和条件加载。
- 使用上游依赖和 JDK 25 完成 `testDebugUnitTest assembleDebug`。本机使用额外的 Gradle init 脚本隔离旧构建目录；Gradle 单元测试为 `NO-SOURCE`，不计为执行了 JUnit。
- APK 检查覆盖入口、作用域、实现类、独立设置页面以及未打包 Xposed API；签名与编译后 XML 检查通过。
- 用户已确认先前版本的背部轻敲付款码和独立页面 UI 正常。新增乘车码、双击电源键双动作、锁屏/切换用户行为尚未完成真机验证，构建成功不代表这些行为已验证。
