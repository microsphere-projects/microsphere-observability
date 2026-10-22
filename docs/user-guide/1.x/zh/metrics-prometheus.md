# metrics-prometheus 用户文档（1.x 版本线）

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
记录警告日志），因此抓取永远不会失败。每个 JVM 只选择一个。本模块自身不含注册表、不含 exposition server、不含 push
gateway，也没有 Spring 代码：你把实例交给 `PrometheusRegistry`（新客户端）或 `CollectorRegistry`（旧客户端），或者交给
[metrics-spring-boot](./metrics-spring-boot.md) 来做。

在本分支上，该模块的 Java 源码与测试与 `release` **逐字节相同**：
`git diff origin/release origin/release-1.x -- microsphere-observability-metrics-prometheus` 只列出 `pom.xml`（新增 7 行，
即一个 optional 的 `micrometer-core`）。不同的是它所处的环境——Prometheus 客户端 BOM 为 `1.8.0`，并且本线的
`metrics-spring-boot` 自动配置的是**旧版** `SentinelCollector`。本文档可独立阅读，跨版本线的对照见最后一节。

## 版本与兼容性

| 属性 | 值 |
|:---|:---|
| 分析分支 | `release-1.x` |
| Maven 坐标 | `io.github.microsphere-projects:microsphere-observability-metrics-prometheus` |
| 版本线 | `0.1.x`（reactor `${revision}` = `0.1.4-SNAPSHOT`，最新已发布标签 `0.1.3`） |
| Java 版本 | 1.x reactor pom 未声明（由 `microsphere-spring-cloud-parent:0.1.26` 继承） |
| 两个采集器的 `@since` | `1.0.0` |
| Prometheus 客户端 BOM | `io.prometheus:prometheus-metrics-bom` `1.8.0`（`microsphere-observability-parent` 中的 `${prometheus.version}`） |
| 与 `current` 的 API 差异 | 无——只有 pom 与下游装配方式不同 |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-prometheus](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-prometheus) |

| 依赖 | 作用域 | 运行时用途 |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | 与后端无关的 `MetricFamily` 模型 |
| `microsphere-observability-metrics-alibaba-sentinel` | compile | `SentinelMetricUtitls`——目录、标签、取值方法 |
| `io.prometheus:prometheus-metrics-core` | optional | 仅 `SentinelMultiCollector` 需要 |
| `io.prometheus:simpleclient` | optional | 仅 `SentinelCollector` 需要 |
| `io.micrometer:micrometer-core` | optional | 本模块无人使用——模块内没有任何源文件 import `io.micrometer`；仅在此版本线声明 |
| `com.alibaba.csp:sentinel-core` | optional | `MetricNode`——所有数值与标签的来源 |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `getContextMetricNodesMap(interval)` 背后的 `SentinelMetricsRepository` |
| `microsphere-java-core`、`microsphere-annotation-processor` | optional | 日志与集合工具、构建期注解处理 |
| `junit-jupiter`、`prometheus-metrics-instrumentation-jvm`、`prometheus-metrics-exporter-httpserver`、`microsphere-java-test`、`logback-classic` | test | 模块测试 |

两个 Prometheus 构件都是 **optional**，因此在缺少对应客户端 API 的 classpath 上实例化采集器会以
`NoClassDefFoundError` 收场。`prometheus-metrics-core` 由导入的 BOM 解析为 `1.8.0`；`simpleclient` 在模块 pom 中没有写
版本号，其版本来自 `microsphere-observability-parent` 继承的 `dependencyManagement`——执行
`./mvnw -pl microsphere-observability-metrics-prometheus dependency:tree` 可看到实际生效值。仓库 README 把 `1.x` 线定位
在 Spring Cloud `Hoxton – 2021.0.x`；Spring 世代由 reactor 父工程 `microsphere-spring-cloud-parent:0.1.26` 通过 profile
选择（默认项加粗）：

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-hoxton` | 2.3.12.RELEASE | Hoxton.SR12 | 2.2.10 |
| `spring-cloud-2020` | 2.4.13 | 2020.0.5 | 2021.1 |
| **`spring-cloud-2021`（默认）** | **2.6.15** | **2021.0.9** | **2021.0.6.1** |

## 安装

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-observability-metrics-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient</artifactId>
</dependency>
```

在 Boot 2.6 + `micrometer-registry-prometheus` 下本来就带有 `simpleclient`，而本线的自动配置正是用它与
`SentinelCollector` 配合。此外还要补上 `com.alibaba.csp:sentinel-core` 与
`io.github.microsphere-projects:microsphere-alibaba-sentinel-commons`：它们在本模块中是 optional，但缺少它们时永远不会
产生任何数据点。若想改用新客户端，请额外声明 `io.prometheus:prometheus-metrics-core`（此处为 `1.8.0`，由
`io.prometheus:prometheus-metrics-bom` 管理）。observability 构件的版本由消费者 BOM
[microsphere-observability-dependencies](./dependencies.md) 管理（已发布构建为 `0.1.3`）。

## 快速开始

旧客户端（`simpleclient`）——本线自动配置使用的采集器：

```java
import io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector;
import io.prometheus.client.CollectorRegistry;

SentinelCollector collector = new SentinelCollector(60_000L).commonLabel("application", "demo");
collector.register(new CollectorRegistry());    // 采集器自行注册
```

新客户端（`MultiCollector`）——本线同样可用，但 `prometheus-metrics-core` 需要你自己添加：

```java
import io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

SentinelMultiCollector collector = new SentinelMultiCollector(60_000L).commonLabel("application", "demo");
new PrometheusRegistry().register(collector);   // MultiCollector 由注册表负责注册
```

每次抓取都会调用 `SentinelMetricUtitls.getContextMetricNodesMap(interval)`，并渲染该窗口内 Sentinel 记录到的内容。

## API 参考

两条版本线完全一致；为了让本文档可以独立阅读，此处完整重述。

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
传递的（`new Sample(metricName, labelNames, labelValues, value, timestampMs)`），**而不是** `sentinel_timestamp` 标签。
由于本线的自动配置使用旧采集器，`/actuator/prometheus` 上的序列不携带 `sentinel_timestamp` 标签。

## 配置

本模块不读取任何属性文件，也不声明任何配置键；调用方能调整的只有一个构造器参数和若干标签键值对。

| 可调项 | 位置 | 仓库自带测试中的取值 | 作用 |
|:---|:---|:---|:---|
| `interval` | 构造器参数，毫秒 | `60000` | 传给 `SentinelMetricUtitls.getContextMetricNodesMap(long)` 的 `[now - interval, now]` 窗口 |
| 公共标签 | `commonLabel(String, String)` | `test-label=test-value`、`application=test` | 追加在 4 个必需标签之后作用于每个数据点；同名时**必需标签覆盖**公共标签，因为 `combineLabels` 先插入公共标签 |
| 注册表 | 你的 `CollectorRegistry` 或 `PrometheusRegistry` | 每个测试新建实例 | 决定导出端点；采集器本身与注册表无关 |

### 由 `metrics-spring-boot` 装配

`SentinelMetricsAutoConfiguration`（包 `io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure`）在本线注册
的是**旧版**采集器：其内部类 `PrometheusConfiguration` 由 `@ConditionalOnEnabledPrometheusMetricsExport` 把关，该注解本身
是 `@ConditionalOnEnabledMetricsExport("prometheus")` 加上
`@ConditionalOnBean(type = "io.micrometer.prometheus.PrometheusMeterRegistry")`——即 Boot 2 时代的 registry 类型。bean
方法 `sentinelCollector(PrometheusMeterRegistry registry, MetricsProperties properties, @Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration interval)`
额外带一个条件，实质就是下面几句：

```java
@ConditionalOnClass(name = {"io.prometheus.client.Collector"})
SentinelCollector sentinelCollector = new SentinelCollector(interval.toMillis());
Map<String, String> commonTags = properties.getTags();      // Common tags covert to Prometheus labels
commonTags.forEach(sentinelCollector::commonLabel);
CollectorRegistry prometheusRegistry = registry.getPrometheusRegistry();
sentinelCollector.register(prometheusRegistry);
```

| 配置属性 | 默认值 | 作用 |
|:---|:---|:---|
| `microsphere.metrics.micrometer.alibaba-sentinel.enabled` | `true`（`matchIfMissing = true`） | 开关整条 Sentinel 指标链路，包含本采集器 |
| `management.metrics.tags.*` | 随带的 `META-INF/config/default/metrics.yaml` 中提供 `application`、`profiles` | 通过 `MetricsProperties.getTags()` 读取，随后被应用为 `commonLabel` |
| `system.metrics.collection.interval` | `60000`；`metrics.yaml` 将其映射为 `${management.metrics.export.prometheus.pushgateway.push-rate:60000}` | 传给 `interval.toMillis()` 的 `Duration` |

Micrometer binder bean `SentinelMetrics` 仅在
`@ConditionalOnMissingBean(type = "io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector")` 时创建，因此你
自己声明 `SentinelCollector` bean 也会一并关掉 Micrometer 路径——但声明 `SentinelMultiCollector` bean 不会，这与
`current` 线不同。本线自动配置同时通过 `META-INF/spring/…AutoConfiguration.imports` 与旧式
`META-INF/spring.factories` 注册。

## 代码示例

本模块的测试与 `release` 逐字节相同。取自
`src/test/java/io/microsphere/metrics/prometheus/sentinel/client/SentinelCollectorTest.java`，其 `setUp` 使用
`new SentinelCollector(60000).commonLabel("test-label", "test-value")` 并在新建的 `CollectorRegistry` 上调用
`sentinelCollector.register(registry)`：

```java
@Test
void testCollect() throws Throwable {
    this.testHelper.doInSentinelMetrics(() -> {
        Enumeration<MetricFamilySamples> metricFamilySamples = this.registry.metricFamilySamples();
        List<MetricFamilySamples> metricFamilySamplesList = newArrayList(metricFamilySamples);
        assertMetricFamilySamplesList(metricFamilySamplesList);   // 断言 size == 7，随后逐索引断言 name/type/help
    });
}

@Test
void testCollectOnSentinelMetricsRepositoryNotReady() {
    List<MetricFamilySamples> metricFamilySamplesList = newArrayList(this.registry.metricFamilySamples());
    assertTrue(metricFamilySamplesList.isEmpty());
}
```

逐索引断言的内容是 `samples.name == buildMetricName(metricFamily)`、
`samples.type.name() == metricFamily.getType().name()` 以及 `samples.help == metricFamily.getHelp()`。
`…/sentinel/SentinelMultiCollectorTest.java` 中的新客户端测试以
`new SentinelMultiCollector(60000).commonLabel("application", "test")` 构造并注册到 `PrometheusRegistry`，抓取后断言
`assertEquals(7, metricSnapshots.size())`，在没有仓库时断言 `assertEquals(0, …size())`。

两者都借助同包下的 `SentinelMetricsTestHelper`：它创建 `SentinelMetricsRepository`，用 `addEntryCallback(...)` 安装，
随后以 `new SentinelTemplate().call("test-resource", () -> sleep(10))` 驱动 `100` 次调用，再用 `sleep(100)` 轮询直到
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

- 本线的 `metrics-spring-boot` 与 `metrics-spring-cloud` 工作在 `io.prometheus:simpleclient` 之上，因此自动配置出来的
  序列形如上表的旧客户端列；新客户端只有在你自行添加构件并注册采集器时才生效。
- 每个 JVM 只注册**一个**采集器：两者读取同一个仓库，只是以不同模型输出同样的 7 个指标族。
- `SentinelMultiCollector` 在构造器中就冻结了描述符列表，因此之后追加的公共标签会出现在样本上，却不会出现在描述符
  声明的 `labelNames` 中——请先配置标签，再注册。
- 空抓取只记录日志而不抛异常：两个采集器都会输出 `No metrics collected`，目录工具会输出
  `SentinelMetricsRepository is not ready, please check whether it is configured`。
- 目录中的两处瑕疵会直接进入导出结果：`sentinel_exception_qps` 的 help 文本是
  `"The Block QPS of the Sentinel Resource"`（上游复制错误），`sentinel_rt` 的单位拼作 `millseconds`，而旧采集器还会把
  该单位上报两次（指标名 + `MetricFamilySamples` 的 unit 参数）。
- 数值是每个 `MetricNode` 的瞬时值（Sentinel 每秒每资源写入一个节点），并非 `interval` 上的聚合，因此跨越数秒的时间窗
  会为同一序列产生多个数据点。
- 本模块自身不含 `AutoConfiguration.imports`、`spring.factories` 或服务发现文件。

## 与 `current` 版本线的差异

| 项目 | `0.1.x`（`release-1.x`） | `0.2.x`（`release`） |
|:---|:---|:---|
| 模块 Java 源码与测试 | — | 完全一致 |
| 模块 pom | 额外声明 optional 的 `io.micrometer:micrometer-core` | 无 `micrometer-core` |
| Prometheus 客户端 BOM | `1.8.0` | `1.9.0` |
| `metrics-spring-boot` 注册的采集器 | `SentinelCollector`（旧版 `simpleclient`），标签取自 `MetricsProperties.getTags()` | `SentinelMultiCollector`（新客户端），标签取自 `getSubProperties(environment, "management.metrics.tags")` |
| 该处使用的 registry 对象 | `registry.getPrometheusRegistry()` → `io.prometheus.client.CollectorRegistry` | `registry.getPrometheusRegistry()` → `io.prometheus.metrics.model.registry.PrometheusRegistry` |
| 条件所要求的 meter registry 类型 | `io.micrometer.prometheus.PrometheusMeterRegistry` | `io.micrometer.prometheusmetrics.PrometheusMeterRegistry` |
| 自动配置输出中的 `sentinel_timestamp` 标签 | 不存在（改为样本时间戳） | 存在 |
| Micrometer binder 的抑制键 | `…sentinel.client.SentinelCollector` | `…sentinel.SentinelMultiCollector` |
| 下游自动配置注册方式 | `.imports` + `spring.factories` | 仅 `.imports` |
| 默认 Spring Boot / Spring Cloud | 2.6.15 / 2021.0.9 | 3.5.15 / 2025.0.3 |

## 相关文档

- [English User Guide](../metrics-prometheus.md)
- [开发者文档 —— metrics-prometheus（1.x）](../../developer-guide/1.x/zh/metrics-prometheus.md)
- [用户文档 —— current / release 分支](../current/zh/metrics-prometheus.md)
- [本分支用户文档索引](./README.md)
