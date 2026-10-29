# 2026-10-27 采集覆盖缺失误报

## 设备证据

用户在 MIUIX 智能诊断详情看到诊断数据采集部分异常。只读复制 Room 主库与稳定 WAL 后，最新报告的唯一异常来自 LogSync.HomeCoverage，原始记录为 NO_HOME_RECORDS，规则被映射为 LOGS_INCOMPLETE。Root 和通用及专用 Logcat 采集的最新状态均为成功，没有采集失败记录。通用采集只查看最近 12000 条日志，未捕获桌面原生记录不等于采集命令失败，也不能判定功能失败。

## 修改

LegacyDiagnosticAdapter 把 NO_HOME_RECORDS 单独映射为 WAITING 和 HOME_RECORDS_MISSING，成功的覆盖与导入记录可恢复对应旧失败状态。真正的导入失败或超限仍为 DEGRADED 和 LOGS_INCOMPLETE，Root 明确被拒绝仍保留 ROOT_UNAVAILABLE。

DiagnosticRules 为缺少桌面证据，采集失败，Root 不可用分别给出建议，只有 Root 不可用提示授权。缺少桌面记录保持待检查，不计入异常或成功。DiagnosticsRepository 仍将缺少覆盖视为数据不完整。

## 验证

- 新增分类回归在修改前失败，repro.log 位于 /tmp/hc-diagnostics-collection-20261027
- 55 项规则检查，实际导出，Java 与原生观察器，采样，Room SQL 和 MIUIX 展示回归全部通过
- Release 完整构建通过，1 分 1 秒，签名验证通过，git diff --check 通过
- APK SHA256 d7aec80e537826101f1ff675e5336156f35a3f5147dfe54e5b77c6feefcd391a
- 非增量安装成功，手机 base.apk 哈希一致
- 打开诊断页，保留同一条原始数据库记录，概览显示异常 0，待检查 1，详情为 HOME_RECORDS_MISSING，并显示本次采集范围内未发现桌面原生日志
- 新报告仍标记采集不完整，未将缺少证据计为成功
- 未重启桌面或系统，桌面 PID 保持 31172，没有清空日志，未修改功能设置

设备证据位于 /tmp/hc-diagnostics-collection-20261027，包含原报告，修正报告及 UI 层级。设备仅验证已有记录的重分析与显示，未人工制造 Root 被拒绝或采集命令失败，相关失败保留行为由 JVM 回归验证。没有提交或推送远端。

## 弹窗标点安装复核

后续用户允许日志弹窗使用其他标点，详情及关联证据列表移除了逗号格式化，诊断页面说明继续使用原规则。Release 35 秒构建成功，签名验证和非增量安装成功，手机 base.apk SHA256 与产物一致，为 5266b71a036ca9f317acb42f9f5636ef8d809b213409c036108c505dbfc81974。

ADB 恢复后完成三层实测，详情中的中文冒号，错误码下划线，目标斜杠及代际连字符均保留，关联证据列表保留时间冒号与错误码下划线，原始日志弹窗保留 tag 点号与原始文本标点。该次记录已变为 COLLECTION_READY，故最初限定 HOME_RECORDS_MISSING 的验收断言不再适用，后续按实际当前分类与标点验证通过。没有为验收修改任何功能开关或重启进程。UI 证据位于 /tmp/hc-diagnostics-collection-20261027 的 dialog-verified.xml，evidence-list-verified.xml 和 raw-evidence-verified.xml。
