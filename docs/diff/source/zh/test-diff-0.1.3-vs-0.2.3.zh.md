# 测试差异：0.1.3 vs 0.2.3（`src/test`）

> 范围：仅 `src/test`。`src/main` 见 [main-diff-0.1.3-vs-0.2.3.zh.md](main-diff-0.1.3-vs-0.2.3.zh.md)。
> 比较时排除：Java 8 与 Java 17 的语法及 API 差异、Java EE 与 Jakarta EE API 差异。本次两个 tag 的对比中，这两类差异均未出现。

## 1. 模块对照表

| Maven 模块 | 0.1.3 `src/test` 文件数 | 0.2.3 `src/test` 文件数 | 差异文件数 | 结论 |
|---|---|---|---|---|
| microsphere-observability-logging | 4 | 4 | 0 | 一致 |
| microsphere-observability-metrics-commons | 0 | 0 | 0 | 无测试 |
| microsphere-observability-metrics-alibaba-sentinel | 2 | 2 | 0 | 一致 |
| microsphere-observability-metrics-micrometer | 20 | 20 | 0 | 一致 |
| microsphere-observability-metrics-prometheus | 3 | 3 | 0 | 一致 |
| microsphere-observability-logging-spring-boot | 9 | 9 | 0 | 一致 |
| microsphere-observability-metrics-spring-boot | 13 | 13 | 2 | **有差异** |
| microsphere-observability-metrics-spring-cloud | 2 | 2 | 1（重命名 + 修改） | **有差异** |

两条线的测试代码其余部分完全一致，只有 3 个测试类存在差异，且都是绑定 Spring Boot / Micrometer / Prometheus 坐标的集成测试。

## 2. microsphere-observability-metrics-spring-boot

| 文件 | 0.2.3 的变化 | 差异原因 |
|---|---|---|
| `…/autoconfigure/JvmMetricsAutoConfigurationIntegrationTest.java` | 以 `test` 为前缀的 executor-service binder 指标数量断言由 `18` 提升为 `20` | 功能预期 —— 0.2.x 线使用的较新 Micrometer 为每个缓存线程池多注册了 2 个 `ExecutorServiceMetrics` 指标；断言统计的是 binder 产出数量，并非代码逻辑变化 |
| `…/autoconfigure/SentinelMetricsAutoConfigurationIntegrationTest.java` | 导入 `io.micrometer.prometheus.PrometheusMeterRegistry` → `io.micrometer.prometheusmetrics.PrometheusMeterRegistry`；`@SpringBootTest(properties=…)` 中 `management.metrics.export.prometheus.enabled=true` → `management.prometheus.metrics.export.enabled=true` | API 兼容 —— 与 `src/main` 中 Micrometer Prometheus 包名及 Spring Boot Prometheus 导出属性命名空间的迁移保持一致；新栈会忽略旧属性键，不改则导出无法启用 |

## 3. microsphere-observability-metrics-spring-cloud

| 文件 | 0.2.3 的变化 | 差异原因 |
|---|---|---|
| `ServiceRegistrationMetricsAutoConfigurationIntegrationTest.java` → `ServiceRegistrationMetricsAutoConfigurationIT.java`（相似度 92% 的重命名） | 类名重命名并同步更新 `@SpringBootTest(classes=…)` 自引用；属性中在保留旧键 `management.metrics.export.prometheus.enabled=true` 的同时新增 `management.prometheus.metrics.export.enabled=true` | 命名约定 + API 兼容 —— `IT` 后缀符合 0.2.x 线采用的集成测试命名约定；同时保留新旧两个属性键，使同一测试可在两代 Spring Boot 上通过，与 `src/main` 中"叠加坐标"的策略一致 |

## 4. 观察结论

1. `metrics-spring-cloud` 的测试是唯一新旧 Prometheus 属性键并存的地点，而 `metrics-spring-boot` 的测试直接替换了属性键。这种不对称是有意为之：Spring Cloud 模块的 IT 需要跨两代 Boot 保持可移植，而 Boot 模块的集成测试只在 0.2.x 线上运行。
2. `microsphere-observability-metrics-prometheus` 的测试（`SentinelMultiCollectorTest`、`SentinelCollectorTest`、`SentinelMetricsTestHelper`）在两个 tag 中完全一致，说明 `src/main` 中观察到的采集器切换在两条线上都早有完整测试覆盖。
3. 整体上既没有新增也没有删除测试文件（13/13、2/2 的文件数保持不变），差异仅限于坐标与断言预期的更新。
