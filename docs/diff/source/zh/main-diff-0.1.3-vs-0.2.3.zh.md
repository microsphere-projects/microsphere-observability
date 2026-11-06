# 源码差异：0.1.3 vs 0.2.3（`src/main`）

> 范围：仅 `src/main`（Java 源码 + `META-INF` 资源）。`src/test` 见 [test-diff-0.1.3-vs-0.2.3.zh.md](test-diff-0.1.3-vs-0.2.3.zh.md)。
> 比较时排除：Java 8 与 Java 17 的语法及 API 差异、Java EE 与 Jakarta EE API 差异。本次两个 tag 的对比中，这两类差异均未出现。

## 1. 版本背景

| | tag `0.1.3` | tag `0.2.3` |
|---|---|---|
| Maven `revision` | `0.1.3-SNAPSHOT` | `0.2.3-SNAPSHOT` |
| 根 pom `java.version` | 未声明（继承 `microsphere-spring-cloud-parent` **0.1.26**） | `17` |
| Microsphere Spring Cloud 父工程 | `0.1.26` | `0.2.26` |
| microsphere-alibaba-sentinel | `0.1.5` | `0.2.5` |
| microsphere-i18n | `0.1.21` | `0.2.21` |

两个 tag 分别是同一仓库两条并行维护线的最新发布版本（`0.1.x` = Java 8 / Spring Boot 2 世代，`0.2.x` = Java 17 / Spring Boot 3–4 世代）。两条线的 `<modules>` 列表完全一致，因此所有 Maven 模块都能按名称一一匹配。

## 2. 模块对照表

| Maven 模块 | 0.1.3 `src/main` 文件数 | 0.2.3 `src/main` 文件数 | 差异文件数 | 结论 |
|---|---|---|---|---|
| microsphere-observability-parent | 无（仅 pom） | 无（仅 pom） | 0 | 一致 |
| microsphere-observability-dependencies | 无（仅 pom） | 无（仅 pom） | 0 | 一致 |
| microsphere-observability-logging | 6 | 6 | 0 | 一致 |
| microsphere-observability-metrics-commons | 2 | 2 | 0 | 一致 |
| microsphere-observability-metrics-alibaba-sentinel | 4 | 4 | 0 | 一致 |
| microsphere-observability-metrics-micrometer | 32 | 32 | 0 | 一致 |
| microsphere-observability-metrics-prometheus | 5 | 5 | 0 | 一致 |
| microsphere-observability-logging-spring-boot | 20 | 20 | 3 | **有差异** |
| microsphere-observability-metrics-spring-boot | 29 | 28 | 9 | **有差异** |
| microsphere-observability-spring-boot | 0 | 0 | 0 | 一致（聚合模块，无源码） |
| microsphere-observability-metrics-spring-cloud | 5 | 4 | 3 | **有差异** |
| microsphere-observability-spring-cloud | 0 | 0 | 0 | 一致（聚合模块，无源码） |

差异只集中在三个 Spring Boot / Spring Cloud 自动装配模块。这三个模块在 0.2.3 中遵循同一策略：**保留 Spring Boot 2 时代的旧坐标，并列新增 Spring Boot 4.0 迁移后的坐标**，使同一份产物同时服务 `[2.0, 4.0)` 与 `[4.0, ∞)`。

## 3. microsphere-observability-logging-spring-boot

| 文件 | 0.2.3 的变化 | 差异原因 |
|---|---|---|
| `logging/log4j2/spring/boot/autoconfigure/Log4j2AutoConfiguration.java` | Log4j2 Kafka Appender 构建链中 `.setSendEventTimestamp(properties.isSendEventTimestamp())` 由注释状态恢复为生效 | 功能需要 —— 0.2.x 线锁定的 log4j-ext 版本提供了 `setSendEventTimestamp`，0.1.x 线因该构建方法不存在而保持关闭 |
| `logging/spring/boot/autoconfigure/WebMvcLoggingAutoConfiguration.java` | `@AutoConfigureAfter(name=…)` 新增 `org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration`（并补充对应 `@see`） | API 兼容 —— Spring Boot 4.0 将 `WebMvcAutoConfiguration` 从 `spring-boot-autoconfigure` 迁移到新的 `spring-boot-webmvc` 模块；同时保留 2.x/3.x 全限定名以维持向后解析 |
| `resources/META-INF/spring.factories` | 删除 `EnableAutoConfiguration=…` 段（4 个自动装配类），保留 `ApplicationListener=…` 段 | API 兼容 —— `META-INF/spring/…AutoConfiguration.imports` 在两个 tag 中都已存在且内容完全一致，0.2.3 因此移除 Boot 2 遗留注册路径；监听器注册没有对应的 `imports` 文件，故必须保留 |

## 4. microsphere-observability-metrics-spring-boot

| 文件 | 0.2.3 的变化 | 差异原因 |
|---|---|---|
| `…/autoconfigure/CGGroupMetricsAutoConfiguration.java` | `@AutoConfigureAfter` 与 `@see` 新增 `org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration`、`…CompositeMeterRegistryAutoConfiguration`，并加上 `[2.0, 4.0)` / `[4.0, )` 注释 | API 兼容 —— Boot 4.0 按 starter 拆分的自动装配包路径 |
| `…/autoconfigure/JvmMetricsAutoConfiguration.java` | 同上模式，另新增 `…micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration` | API 兼容 |
| `…/autoconfigure/KafkaMetricsAutoConfiguration.java` | 同上模式，另新增 `org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration` | API 兼容 |
| `…/autoconfigure/SystemMetricsAutoConfiguration.java` | 同上模式，另新增 `…micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration` | API 兼容 |
| `…/autoconfigure/SentinelMetricsAutoConfiguration.java` | Micrometer Prometheus 包名 `io.micrometer.prometheus` → `io.micrometer.prometheusmetrics`；Prometheus 客户端 `io.prometheus.client.CollectorRegistry` → `io.prometheus.metrics.model.registry.PrometheusRegistry`；Bean 由 `SentinelCollector` 改为 `SentinelMultiCollector`；移除 `@ConditionalOnClass("io.prometheus.client.Collector")`；公共标签改由 `getSubProperties(environment, "management.metrics.tags")` 读取，不再注入 `MetricsProperties`；同步更新 `@ConditionalOnMissingBean(type=…)` | API 兼容（micrometer-registry-prometheus 1.13+ 包迁移与 prometheus-metrics 1.x 客户端）+ 实现需要：`MultiCollector` 是客户端 1.x 对已废弃 `Collector` SPI 的替代；`MetricsProperties` 被替换为直接读取 Environment，因为该属性类在新世代中已迁移位置 |
| `…/condition/ConditionalOnEnabledPrometheusMetricsExport.java` | `@ConditionalOnBean(type=…)` 指向 `io.micrometer.prometheusmetrics.PrometheusMeterRegistry`；头部新增未使用的 `ConditionalOnClass` 导入 | API 兼容 —— 条件必须匹配新栈实际导出的注册表类名，否则 Sentinel 的 Prometheus Bean 永远不生效 |
| `resources/META-INF/spring.factories` | 整个文件删除（5 个自动装配类） | API 兼容 —— 内容已在 `AutoConfiguration.imports` 中等价注册 |

## 5. microsphere-observability-metrics-spring-cloud

| 文件 | 0.2.3 的变化 | 差异原因 |
|---|---|---|
| `…/actuate/autoconfigure/ServiceRegistrationMetricsAutoConfiguration.java` | 导入 `SentinelCollector` → `SentinelMultiCollector`；`@AutoConfigureAfter`/`@see` 新增三条 `org.springframework.boot.micrometer.metrics.autoconfigure.*` 坐标；`@ConditionalOnClass` 由 `io.prometheus.client.Collector` 改为 `io.prometheus.metrics.model.registry.MultiCollector`；`@ConditionalOnBean(type="…SentinelCollector")` 改为 `@ConditionalOnBean(SentinelMultiCollector.class)`；Bean 名与方法名改为 `sentinelMultiCollectorBeanPostProcessor` | API 兼容 —— 该后置处理器需要给 0.2.x metrics-spring-boot 实际导出的采集器类型追加 `instance` 标签；类进入编译期 classpath 后，字符串形式的条件可换成类型安全写法 |
| `resources/META-INF/spring.factories` | 整个文件删除（1 个自动装配类） | API 兼容 —— 已在 `AutoConfiguration.imports` 中注册 |

## 6. 总体结论

1. **自动装配注册方式**：0.1.3 同时提供 `spring.factories` 与 `AutoConfiguration.imports`；0.2.3 只提供后者。各模块注册的类清单完全未变——这是纯粹的 Spring Boot 2 遗留路径清理，不产生功能差异。
2. **Spring Boot 4.0 包迁移采用叠加而非替换**：新旧全限定名并存于 `@AutoConfigureAfter(name=…)` 字符串列表中，这是唯一能在类缺失时静默容错的机制。
3. **Prometheus 客户端迁移是唯一真正的行为变化**：0.2.3 不再装配 `SentinelCollector`（客户端 `0.x` 的 `Collector` SPI，该类在两个 tag 中都仍保留且完全一致），改用客户端 1.x 的 `SentinelMultiCollector`。`microsphere-observability-metrics-prometheus` 模块本身没有任何改动，因为两个 tag 都已同时携带两种采集器。
4. **指标标签装配方式**由 `MetricsProperties.getTags()` 改为基于 `ConfigurableEnvironment` 的子属性读取，使代码不再依赖跨世代位置发生变化的 Boot 属性类。
5. 本 tag 对的 `src/main` 中不存在仅由 Java 8/17 语法或 Java EE/Jakarta EE 造成的差异。
