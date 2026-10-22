# metrics-prometheus 用户文档

[English version](../metrics-prometheus.md) · [用户文档索引](../../README.md)

## 概述

`microsphere-observability-metrics-prometheus` 把 `metrics-alibaba-sentinel` 拥有的 Alibaba Sentinel 指标目录渲染为仍在
使用的两套 Prometheus 客户端 API。它只导出两个公开类型，每个客户端 API 各一个：

- `SentinelMultiCollector` —— 实现 `io.prometheus.metrics.model.registry.MultiCollector`（client_java 1.x，构件
  `io.prometheus:prometheus-metrics-core`），返回 `MetricSnapshots`。
- `SentinelCollector` —— 继承旧版 `io.prometheus.client.Collector`（构件 `io.prometheus:simpleclient`），返回
  `List<Collector.MetricFamilySamples>`。

二者读取同一组 7 个 `GAUGE` 指标族（`sentinel_rt`、`sentinel_concurrency`、`sentinel_success_qps`、
`sentinel_pass_qps`、`sentinel_occupied_pass_qps`、`sentinel_block_qps`、`sentinel_exception_qps`）与同一组 4 个必需
标签（`sentinel_resource`、`sentinel_context`、`sentinel_resource_type`、`sentinel_version`）；都以毫秒为单位接收采集
窗口；都提供链式的 `commonLabel(name, value)`；并且在 Sentinel 指标仓库未安装时都退化为“什么都不导出”（返回空结果并
记录警告日志），因此抓取永远不会失败。每个 JVM 只按你实际部署的 Prometheus 客户端选择其中之一。

本模块不含注册表、不含 exposition server、不含 push gateway，也没有 Spring 代码：你把实例交给 `PrometheusRegistry`
（新客户端）或 `CollectorRegistry`（旧客户端），或者交给 [metrics-spring-boot](./metrics-spring-boot.md) 来做。

## 版本与兼容性

| 属性 | 值 |
|:---|:---|
| 分析分支 | `release` |
| Maven 坐标 | `io.github.microsphere-projects:microsphere-observability-metrics-prometheus` |
| 版本线 | `0.2.x`（reactor `${revision}` = `0.2.4-SNAPSHOT`，最新已发布标签 `0.2.3`） |
| Java 版本 | `17` |
| 两个采集器的 `@since` | `1.0.0` |
| Prometheus 客户端 BOM | `io.prometheus:prometheus-metrics-bom` `1.9.0`（`microsphere-observability-parent` 中的 `${prometheus.version}`） |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-prometheus](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-prometheus) |

| 依赖 | 作用域 | 运行时用途 |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | 与后端无关的 `MetricFamily` 模型 |
| `microsphere-observability-metrics-alibaba-sentinel` | compile | `SentinelMetricUtitls`——目录、标签、取值方法 |
| `io.prometheus:prometheus-metrics-core` | optional | 仅 `SentinelMultiCollector` 需要 |
| `io.prometheus:simpleclient` | optional | 仅 `SentinelCollector` 需要 |
| `com.alibaba.csp:sentinel-core` | optional | `MetricNode`——所有数值与标签的来源 |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `getContextMetricNodesMap(interval)` 背后的 `SentinelMetricsRepository` |
| `microsphere-java-core`、`microsphere-annotation-processor` | optional | 日志与集合工具、构建期注解处理 |
| `junit-jupiter`、`prometheus-metrics-instrumentation-jvm`、`prometheus-metrics-exporter-httpserver`、`microsphere-java-test`、`logback-classic` | test | 模块测试 |

两个 Prometheus 构件都是 **optional**，因此在缺少对应客户端 API 的 classpath 上实例化采集器会以
`NoClassDefFoundError` 收场。`prometheus-metrics-core` 由导入的 BOM 解析为 `1.9.0`；`simpleclient` 在模块 pom 中没有写
版本号，其版本来自 `microsphere-observability-parent` 继承的 `dependencyManagement`——执行
`./mvnw -pl microsphere-observability-metrics-prometheus dependency:tree` 可看到实际生效值。仓库 README 声明
`main` / `1.x` 版本线分别面向 Spring Cloud `2022.0.x – 2025.0.x` 与 `Hoxton – 2021.0.x`；Spring 世代由 reactor 父工程
`microsphere-spring-cloud-parent:0.2.26` 通过 profile 选择（默认项加粗），只有当你从 Spring Boot 应用消费本模块时才有
意义：

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-2022` | 3.0.13 | 2022.0.5 | 2022.0.0.2 |
| `spring-cloud-2023` | 3.3.13 | 2023.0.6 | 2023.0.3.4 |
| `spring-cloud-2024` | 3.4.13 | 2024.0.3 | 2023.0.3.4 |
| **`spring-cloud-2025`（默认）** | **3.5.15** | **2025.0.3** | **2025.0.0.0** |
| `spring-cloud-2025.1` | 4.0.8 | 2025.1.3 | 2025.1.0.0 |

## 安装

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-observability-metrics-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>prometheus-metrics-core</artifactId>
</dependency>
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient</artifactId>
</dependency>
```

`prometheus-metrics-core` 是 `SentinelMultiCollector` 所需的客户端，`simpleclient` 是 `SentinelCollector` 所需的客户端，
只需声明你实际实例化的那一个。此外还要补上 `com.alibaba.csp:sentinel-core` 与
`io.github.microsphere-projects:microsphere-alibaba-sentinel-commons`：它们在本模块中是 optional，但缺少它们时永远不会
产生任何数据点。observability 构件的版本由消费者 BOM
[microsphere-observability-dependencies](./dependencies.md) 管理（已发布构建为 `0.2.3`）；该 BOM 只列出本项目的模块，
因而在 Spring Boot 父工程之外使用时请自行导入 `io.prometheus:prometheus-metrics-bom:1.9.0` 以获得客户端版本。

## 快速开始

新客户端（`MultiCollector`）：

```java
import io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

SentinelMultiCollector collector = new SentinelMultiCollector(60_000L).commonLabel("application", "demo");
new PrometheusRegistry().register(collector);   // MultiCollector 由注册表负责注册
```

旧客户端（`simpleclient`）：

```java
import io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector;
import io.prometheus.client.CollectorRegistry;

SentinelCollector collector = new SentinelCollector(60_000L).commonLabel("application", "demo");
collector.register(new CollectorRegistry());    // 采集器自行注册
```

配置到此为止：每次抓取都会调用 `SentinelMetricUtitls.getContextMetricNodesMap(interval)`，渲染该时间窗内 Sentinel
记录到的内容。

## API 参考

### `io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector`

`public class SentinelMultiCollector implements io.prometheus.metrics.model.registry.MultiCollector`

| 签名 | 说明 |
|:---|:---|
| `SentinelMultiCollector(long interval)` | `interval` 是以**毫秒**为单位的采集窗口；7 个 `MetricFamilyDescriptor` 在该构造器中被提前构建 |
| `SentinelMultiCollector commonLabel(String name, String value)` | 为每个数据点追加一个固定标签并返回 `this`；重复的 `name` 会替换其取值 |
| `MetricSnapshots collect()` | `MultiCollector` 契约。遍历 `[now - interval, now]` 上的 `Map<context, List<MetricNode>>`，每个指标族返回一个 `GaugeSnapshot`；没有数据时返回空的 `MetricSnapshots` 并记录警告日志 |
| `List<MetricFamilyDescriptor> getMetricFamilyDescriptors()` | `MultiCollector` 契约。构造期生成的描述符列表，用于对外声明 `# HELP` / `# TYPE` |
| `Labels toLabels(String context, MetricNode metricNode)` | 公开辅助方法：4 个必需标签 + 已配置的公共标签 + `sentinel_timestamp`，渲染为 `io.prometheus.metrics.model.snapshots.Labels` |

所有方法都不声明 `throws`；唯一的失败来源是 `SentinelMetricUtitls` 的 `NullPointerException` /
`IndexOutOfBoundsException` 契约，而本类固定使用的 `0..6` 索引范围不会触发它们。实例未做同步——请在注册之前完成所有
`commonLabel(...)` 调用。

### `io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector`

`public class SentinelCollector extends io.prometheus.client.Collector`

| 签名 | 说明 |
|:---|:---|
| `SentinelCollector(long interval)` | 以**毫秒**为单位的采集窗口；不做任何预先构建——指标族元数据在每次抓取时从目录读取 |
| `SentinelCollector commonLabel(String name, String value)` | 为每个样本追加一个固定标签，返回 `this` |
| `List<MetricFamilySamples> collect()` | `Collector` 契约。返回 7 个 `MetricFamilySamples`，其名称来自 `SentinelMetricUtitls.buildMetricName(name, unit)`（因此 `sentinel_rt` 导出为 `sentinel_rt_millseconds`）；仓库无数据时返回 `Collections.emptyList()` |
| `register()` / `register(CollectorRegistry)` | 继承自 `io.prometheus.client.Collector`；无参形式会注册到 `CollectorRegistry.defaultRegistry` |

标签列表在每个样本上由 `combineLabels(context, metricNode, commonLabels)` 生成，而节点时间戳是作为 `Sample` 的时间戳
传递的（`new Sample(metricName, labelNames, labelValues, value, timestampMs)`），**而不是** `sentinel_timestamp` 标签
——这就是它与新客户端采集器在输出上的可见差异。同样未做同步。

## 配置

本模块不读取任何属性文件，也不声明任何配置键；调用方能调整的只有一个构造器参数和若干标签键值对。

| 可调项 | 位置 | 仓库自带测试中的取值 | 作用 |
|:---|:---|:---|:---|
| `interval` | 构造器参数，毫秒 | `60000` | 传给 `SentinelMetricUtitls.getContextMetricNodesMap(long)` 的 `[now - interval, now]` 窗口 |
| 公共标签 | `commonLabel(String, String)` | `application=test`、`test-label=test-value` | 追加在 4 个必需标签之后作用于每个数据点；同名时**必需标签覆盖**公共标签，因为 `combineLabels` 先插入公共标签 |
| 注册表 | 你的 `PrometheusRegistry` 或 `CollectorRegistry` | 每个测试新建实例 | 决定导出端点；采集器本身与注册表无关 |

### 由 `metrics-spring-boot` 装配

`SentinelMetricsAutoConfiguration`（包 `io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure`）注册的是
**新客户端**采集器：其内部类 `PrometheusConfiguration` 由 `@ConditionalOnEnabledPrometheusMetricsExport` 把关，该注解
本身是 `@ConditionalOnEnabledMetricsExport("prometheus")` 加上
`@ConditionalOnBean(type = "io.micrometer.prometheusmetrics.PrometheusMeterRegistry")`。bean 方法
`sentinelMultiCollector(PrometheusMeterRegistry registry, @Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration
interval, ConfigurableEnvironment environment)` 的实质就是五条语句：

```java
SentinelMultiCollector sentinelMultiCollector = new SentinelMultiCollector(interval.toMillis());
Map<String, String> commonTags = (Map) getSubProperties(environment, "management.metrics.tags");
commonTags.forEach(sentinelMultiCollector::commonLabel);
PrometheusRegistry prometheusRegistry = registry.getPrometheusRegistry();
prometheusRegistry.register(sentinelMultiCollector);
```

| 配置属性 | 默认值 | 作用 |
|:---|:---|:---|
| `microsphere.metrics.micrometer.alibaba-sentinel.enabled` | `true`（`matchIfMissing = true`） | 开关整条 Sentinel 指标链路，包含本采集器 |
| `management.metrics.tags.*` | 随带的 `META-INF/config/default/metrics.yaml` 中提供 `application`、`profiles` | 每个 tag 都会被应用为一个 `commonLabel` |
| `system.metrics.collection.interval` | `60000`；`metrics.yaml` 将其映射为 `${management.metrics.export.prometheus.pushgateway.push-rate:60000}` | 传给 `interval.toMillis()` 的 `Duration` |

Micrometer binder bean `SentinelMetrics` 仅在
`@ConditionalOnMissingBean(type = "io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector")` 时创建，因此你
自己声明采集器 bean 也会一并关掉 Micrometer 路径；本版本线从不会自动配置 `SentinelCollector`。

## 代码示例

`SentinelMultiCollectorTest#setUp` 使用
`new SentinelMultiCollector(60000).commonLabel("application", "test")` 构造采集器，并把它注册到新建的
`PrometheusRegistry` 上。取自
`src/test/java/io/microsphere/metrics/prometheus/sentinel/SentinelMultiCollectorTest.java` 的抓取用例：

```java
@Test
void testCollect() throws Throwable {
    this.testHelper.doInSentinelMetrics(() -> {
        MetricSnapshots metricSnapshots = this.sentinelMultiCollector.collect();
        assertEquals(7, metricSnapshots.size());
    });
}
```

配套的 `testCollectOnSentinelMetricsRepositoryNotReady` 在没有安装仓库的情况下抓取同一个采集器，并断言
`assertEquals(0, …collect().size())`。旧客户端在 `…/sentinel/client/SentinelCollectorTest.java` 中通过注册表被断言：

```java
this.registry = new CollectorRegistry();
this.sentinelCollector = new SentinelCollector(60000).commonLabel("test-label", "test-value");
this.sentinelCollector.register(registry);

List<MetricFamilySamples> metricFamilySamplesList = newArrayList(this.registry.metricFamilySamples());
assertEquals(7, metricFamilySamplesList.size());
// 逐个索引断言：name == buildMetricName(metricFamily)，type.name() 与 help 等于目录中的取值
```

同包下的 `SentinelMetricsTestHelper` 既是测试夹具，也是采集器所需装配的范例：它创建
`SentinelMetricsRepository`，用 `addEntryCallback(...)` 安装，随后以
`new SentinelTemplate().call("test-resource", () -> sleep(10))` 驱动 `100` 次调用，再用 `sleep(100)` 轮询直到
`findMetricNodes(beginTimeMs, currentTimeMillis())` 非空，然后执行抓取动作，并在 `finally` 中始终调用
`removeEntryCallback(sentinelMetricsRepository.getClass())`。它不启动 HTTP 服务器，也不依赖任何外部服务。

## 指标参考

目录来源：`SentinelMetricUtitls.METRIC_FAMILIES`（`metrics-alibaba-sentinel`）；所有指标族都是 `GAUGE`，都携带 4 个
必需标签外加你的公共标签。

| 指标（目录中声明） | 单位 | 类型 | HELP 文本 | 旧客户端导出名 |
|:---|:---|:---|:---|:---|
| `sentinel_rt` | `millseconds` | `GAUGE` | The Response Time (RT) of the Sentinel Resource | `sentinel_rt_millseconds` |
| `sentinel_concurrency` | — | `GAUGE` | The Concurrency of the Sentinel Resource | `sentinel_concurrency` |
| `sentinel_success_qps` | — | `GAUGE` | The Success QPS of the Sentinel Resource | `sentinel_success_qps` |
| `sentinel_pass_qps` | — | `GAUGE` | The Pass QPS of the Sentinel Resource | `sentinel_pass_qps` |
| `sentinel_occupied_pass_qps` | — | `GAUGE` | The Occupied Pass QPS of the Sentinel Resource | `sentinel_occupied_pass_qps` |
| `sentinel_block_qps` | — | `GAUGE` | The Block QPS of the Sentinel Resource | `sentinel_block_qps` |
| `sentinel_exception_qps` | — | `GAUGE` | The Block QPS of the Sentinel Resource | `sentinel_exception_qps` |

与旧路径不同，`SentinelMultiCollector` 从不自行拼接单位：它通过 `PrometheusNaming.sanitizeMetricName(name)` 命名描述符
（唯一带单位的指标族则使用 `sanitizeMetricName(name, unit)`），并把该 `Unit` 交给描述符构造器。每个数据点上的标签：

| 标签 | 取值 | 由谁输出 |
|:---|:---|:---|
| `sentinel_resource` | `MetricNode.getResource()` | 两者 |
| `sentinel_context` | 仓库映射中按 context 分组的键 | 两者 |
| `sentinel_resource_type` | `SentinelUtils.getResourceTypeAsString(classification)` | 两者 |
| `sentinel_version` | `com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION` | 两者 |
| 你的 `commonLabel(...)` 键值对 | 常量 | 两者 |
| `sentinel_timestamp` | 十进制字符串形式的 `MetricNode.getTimestamp()` | 仅 `SentinelMultiCollector`；旧采集器把同一个值放进 `Sample` 的时间戳 |

## 注意事项与限制

- 每个 JVM 只注册**一个**采集器：两者读取同一个仓库，只是以不同模型输出同样的 7 个指标族。
- `SentinelMultiCollector` 在构造器中就冻结了描述符列表，因此之后追加的公共标签会出现在样本上，却不会出现在描述符
  声明的 `labelNames` 中。请先配置标签，再注册。
- 空抓取只记录日志而不抛异常：两个采集器都会输出 `No metrics collected`，目录工具会输出
  `SentinelMetricsRepository is not ready, please check whether it is configured`。
- 目录中的两处瑕疵会直接进入导出结果：`sentinel_exception_qps` 的 help 文本是
  `"The Block QPS of the Sentinel Resource"`（上游复制错误），`sentinel_rt` 的单位拼作 `millseconds`；旧采集器还会把该
  单位上报两次——既在指标名里，也在 `MetricFamilySamples` 的 unit 参数中。
- 数值是每个 `MetricNode` 的瞬时值（Sentinel 每秒每资源写入一个节点），并非 `interval` 上的聚合，因此跨越数秒的时间窗
  会为同一序列产生多个数据点。
- 本模块不含 `AutoConfiguration.imports`、`spring.factories` 或服务发现文件；装配由你或 `metrics-spring-boot` 完成。

## 相关文档

- [English User Guide](../metrics-prometheus.md)
- [开发者文档 —— metrics-prometheus](../../developer-guide/current/zh/metrics-prometheus.md)
- [用户文档 —— 1.x / release-1.x 分支](../1.x/zh/metrics-prometheus.md)
- [本分支用户文档索引](./README.md)
