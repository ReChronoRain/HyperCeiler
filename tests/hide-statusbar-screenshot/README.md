# 截屏隐藏状态栏收尾恢复回归

Run `python tests/hide-statusbar-screenshot/run.py [source-root]` with JDK 25. Set
`JAVA_HOME` to its installation directory, or use the configured build cache as
described in [PORTABILITY.md](../PORTABILITY.md).

测试把规则的真实源码和最小 android / androidx / libhook stub 一起编译后在宿主机执行，
不需要设备、也不需要完整 Gradle 依赖。

Coverage:

- 只注册一次时开始隐藏、结束恢复（`view` 可见性 + 窗口 alpha 0 → 1）；
- 上游重复 `initHook` 时同一个状态栏 view 只注册一个 receiver；
- 热重载后旧 receiver 与新 receiver 同时盯着同一个 view 时，收尾只恢复一次
  （旧实现在这里会把状态栏重新隐藏）；
- 反向对照：把旧实现放进同一场景，状态栏确实留在隐藏态，用来证明上面的断言不是空转；
- 状态栏 view 被重建后新旧 view 都会被恢复；
- 重复开始广播不会把 INVISIBLE 记成恢复值；
- 没有开始广播的结束广播不会强行显示状态栏；
- 隐藏前就已不可见时恢复成 INVISIBLE（保留上游“记住隐藏前可见性”的语义）；
- 截屏期间窗口 Surface 被重建时，收尾不会留下 0 alpha；
- SDK ≥ 37 走 `MiuiPhoneStatusBarView#onAttachedToWindow` 分支。

Source guards 另外检查 `SystemUIV`/`SystemUIB` 各只注册一次该规则、`BaseLoad`
仍会拦下重复注册、规则状态仍是进程内共享而不是实例字段。

Host tests 覆盖规则逻辑与去重路径；真机验收已通过：截屏结束后状态栏立即恢复，
`finished=true` 的 `CALLBACK_HIT` 为 `visibility=0`。
