# metrics-alibaba-sentinel 用户文档

[English version](../metrics-alibaba-sentinel.md) · [用户文档索引](./README.md)

## 概述

`microsphere-observability-metrics-alibaba-sentinel` 把 Alibaba Sentinel 内部的 `MetricNode` 数据翻译成
`metrics-commons` 中与后端无关的指标词汇。它不含注册表、采集器和 Spring 代码，而是**Sentinel 指标有哪些、叫什么、
携带哪些标签、如何从 `MetricNode` 取值**的唯一事实来源。

- `SentinelMetricsConstants` —— 指标名与标签名常量。
- `SentinelMetricUtitls` —— 7 个指标族的目录、4 个必需标签，以及从 `MetricNode` 读取值与标签的方法。

两个使用方：`metrics-micrometer`（把这些值绑定为 Micrometer meter）与 `metrics-prometheus`（渲染为 Prometheus
`MultiCollector` 或旧版 `Collector`）。

## 版本与兼容性

| 属性 | 值 |
|:---|:---|
| 分析分支 | `release` |
| Maven 坐标 | `io.github.microsphere-projects:microsphere-observability-metrics-alibaba-sentinel` |
| 版本线 | `0.2.x`（reactor `${revision}` = `0.2.4-SNAPSHOT`，最新已发布标签 `0.2.3`） |
| Java 版本 | `17` |
| 两个类型的 `@since` | `1.0.0` |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-alibaba-sentinel](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-alibaba-sentinel) |

| 依赖 | 作用域 | 版本来源 |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | `${revision}`——同一 reactor |
| `com.alibaba.csp:sentinel-core` | optional | 由 `microsphere-alibaba-sentinel-dependencies:0.2.5` 管理 |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `0.2.5` 版本线 |
| `microsphere-java-core`、`microsphere-annotation-processor` | optional | 上游 BOM |

`sentinel-core` 与 `microsphere-alibaba-sentinel-commons` 是 **optional**：没有它们本模块依然可以编译，但
`getContextMetricNodesMap(long)` 将无仓库可读并返回空 Map。

本模块不依赖 Spring，因此 Spring 兼容性是继承而来而非直接使用。reactor 父工程
`microsphere-spring-cloud-parent:0.2.26` 通过 profile 选择 Spring 世代（默认项加粗）：

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
    <artifactId>microsphere-observability-metrics-alibaba-sentinel</artifactId>
</dependency>

<!-- 两个 optional 依赖需要显式补充 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
</dependency>
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-alibaba-sentinel-commons</artifactId>
</dependency>
```

版本由 `microsphere-observability-dependencies` BOM 锁定，见 [dependencies](./dependencies.md)。在 Spring Boot
应用中，你通常通过 [metrics-spring-boot](./metrics-spring-boot.md) 传递获得本模块，而不必显式声明。

## 快速上手

```java
import io.microsphere.metrics.commons.MetricFamily;
import io.microsphere.metrics.sentinel.util.SentinelMetricUtitls;

import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.*;

// 1. 固定目录
MetricFamily first = getMetricFamily(0);      // sentinel_rt，单位 "millseconds"，GAUGE
int familyCount = METRIC_FAMILIES_SIZE;       // 7
int labelCount = REQUIRED_LABEL_NAMES.size(); // 4

// 2. 按目录索引从 MetricNode 读取数值
double rt = getMetricValue(metricNode, 0);    // MetricNode::getRt

// 3. 构造一个数据点的标签映射
Map<String, String> labels = getRequiredLabels("my-context", metricNode);
```

## API 说明

### `io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants`

一个持有 `String` 常量的 `interface`，所有常量都以 `PREFIX = "sentinel_"` 为前缀。

| 常量 | 值 | 作用 |
|:---|:---|:---|
| `PREFIX` | `sentinel_` | 公共前缀 |
| `ORIGIN_LABEL_NAME` | `sentinel_origin` | 调用来源标签——已声明但不在必需标签集合中 |
| `RESOURCE_LABEL_NAME` | `sentinel_resource` | Sentinel 资源标签 |
| `CONTEXT_LABEL_NAME` | `sentinel_context` | 入口上下文标签 |
| `RESOURCE_TYPE_LABEL_NAME` | `sentinel_resource_type` | 资源类型标签 |
| `VERSION_LABEL_NAME` | `sentinel_version` | 携带 Sentinel 版本的标签 |
| `TIMESTAMP_LABEL_NAME` | `sentinel_timestamp` | 携带采样时间戳的标签——由 Prometheus `MultiCollector` 追加，而非 `getRequiredLabels` |
| `RT_METRIC_NAME` | `sentinel_rt` | 响应时间 |
| `CONCURRENCY_METRIC_NAME` | `sentinel_concurrency` | 并发数 |
| `SUCCESS_QPS_METRIC_NAME` | `sentinel_success_qps` | 成功 QPS |
| `PASS_QPS_METRIC_NAME` | `sentinel_pass_qps` | 通过 QPS |
| `OCCUPIED_PASS_QPS_METRIC_NAME` | `sentinel_occupied_pass_qps` | 占用通过 QPS |
| `BLOCK_QPS_METRIC_NAME` | `sentinel_block_qps` | 阻断 QPS |
| `EXCEPTION_QPS_METRIC_NAME` | `sentinel_exception_qps` | 异常 QPS |

### `io.microsphere.metrics.sentinel.util.SentinelMetricUtitls`

`public abstract class SentinelMetricUtitls implements Utils` —— 私有构造器的工具类；类名保留了历史拼写 `Utitls`。

静态状态：

| 成员 | 类型 | 内容 |
|:---|:---|:---|
| `REQUIRED_LABEL_NAMES` | `Set<String>` | `sentinel_resource`、`sentinel_context`、`sentinel_resource_type`、`sentinel_version`——即内部“标签名→取值函数”映射的 keySet，因此遍历顺序即声明顺序 |
| `METRIC_FAMILIES` | `List<MetricFamily>` | 7 个指标族，全部为 `MetricType.GAUGE`，顺序为 rt、concurrency、success_qps、pass_qps、occupied_pass_qps、block_qps、exception_qps |
| `METRIC_NODE_TO_VALUE_FUNCTIONS` | `List<Function<MetricNode, Number>>` | `getRt`、`getConcurrency`、`getSuccessQps`、`getPassQps`、`getOccupiedPassQps`、`getBlockQps`、`getExceptionQps`——与 `METRIC_FAMILIES` 索引一一对应 |
| `REQUIRED_LABEL_NAMES_SIZE` | `int` | 4 |
| `METRIC_FAMILIES_SIZE` | `int` | 7 |

指标族目录：

| 索引 | 指标名 | 单位 | 类型 | 取值来源 |
|:---|:---|:---|:---|:---|
| 0 | `sentinel_rt` | `millseconds` | `GAUGE` | `MetricNode::getRt` |
| 1 | `sentinel_concurrency` | — | `GAUGE` | `MetricNode::getConcurrency` |
| 2 | `sentinel_success_qps` | — | `GAUGE` | `MetricNode::getSuccessQps` |
| 3 | `sentinel_pass_qps` | — | `GAUGE` | `MetricNode::getPassQps` |
| 4 | `sentinel_occupied_pass_qps` | — | `GAUGE` | `MetricNode::getOccupiedPassQps` |
| 5 | `sentinel_block_qps` | — | `GAUGE` | `MetricNode::getBlockQps` |
| 6 | `sentinel_exception_qps` | — | `GAUGE` | `MetricNode::getExceptionQps` |

`METRIC_FAMILIES` 与 `METRIC_NODE_TO_VALUE_FUNCTIONS` 之间的索引对齐是所有下游依赖的契约，测试逐项断言了两个列表。

方法：

| 方法 | 契约 |
|:---|:---|
| `getMetricFamily(int index)` | 返回索引处的指标族；越界抛 `IndexOutOfBoundsException` |
| `buildMetricName(MetricFamily)` | 便捷重载，转调 `buildMetricName(name, unit)` |
| `buildMetricName(String name, String unit)` | `unit` 为空白或 `null` 时返回 `name`；否则返回 `name + "_" + unit`。这就是 `sentinel_rt` 被导出为 `sentinel_rt_millseconds` 的原因 |
| `getContextMetricNodesMap(long interval)` | 返回以 context 为 key 的非空只读 `Map<String, List<MetricNode>>`，时间窗为 `[now - interval, now]`。数据来自 `SentinelMetricsRepository` 单例；仓库未就绪时记录日志 `SentinelMetricsRepository is not ready, please check whether it is configured` 并返回 `Collections.emptyMap()` |
| `combineLabels(String context, MetricNode, Map<String, String> commonLabels)` | 非空只读映射 = 先放 `commonLabels`，再放 4 个必需标签（`LinkedHashMap`，容量为 `REQUIRED_LABEL_NAMES_SIZE + commonLabels.size()`） |
| `getRequiredLabels(String context, MetricNode)` | 仅含 4 个必需标签的非空只读映射 |
| `getRequiredLabelValue(String labelName, String context, MetricNode)` | 应用该标签的取值函数；若名称不属于 4 个必需标签则抛 **`NullPointerException`**（javadoc 已声明，测试已断言） |
| `getMetricValue(MetricNode, int index)` | 索引超出 `0..6` 抛 `IndexOutOfBoundsException`；`metricNode` 为 `null` 抛 `NullPointerException`；返回抽取数值的 `doubleValue()` |

标签取值规则（来自内部 `REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP`）：

| 标签 | 取值来源 |
|:---|:---|
| `sentinel_resource` | `metricNode.getResource()` |
| `sentinel_context` | 入参 `context` |
| `sentinel_resource_type` | `SentinelUtils.getResourceTypeAsString(metricNode.getClassification())`，例如 classification 为 `COMMON_WEB` 时得到 `"COMMON_WEB"` |
| `sentinel_version` | `com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION` |

## 配置

本模块不读取任何自身配置。决定其返回内容的是两个外部输入：

| 输入 | 提供方式 | 影响 |
|:---|:---|:---|
| `SentinelMetricsRepository` | `io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository`，通过 `getSentinelMetricsRepository()` 获取；由 Sentinel `NodeEntryCallback` 填充（`ProcessorSlotCallbackUtils.addEntryCallback(...)`） | 缺失时 `getContextMetricNodesMap` 返回空 Map，不导出任何指标 |
| 采集间隔 | `long interval` 参数，单位毫秒 | 决定仓库上 `[now - interval, now]` 的查询窗口 |

在 Spring Boot 应用中这两者由 [`SentinelMetricsAutoConfiguration`](./metrics-spring-boot.md) 自动装配，其间隔默认值位于
`META-INF/config/default/metrics.yaml`。

## 代码示例

以下内容全部取自 `src/test/java/io/microsphere/metrics/sentinel/util/SentinelMetricUtitlsTest.java`。

按 Sentinel 指标定时器的方式构造 `MetricNode`（测试夹具）：

```java
metricNode = new MetricNode();
metricNode.setResource("test-resource");
metricNode.setClassification(COMMON_WEB);
metricNode.setTimestamp(timestamp);
```

读取标签：

```java
Map<String, String> requiredLabels = getRequiredLabels(context, metricNode);
assertEquals(4, requiredLabels.size());
assertEquals("COMMON_WEB", requiredLabels.get(RESOURCE_TYPE_LABEL_NAME));
assertEquals(SENTINEL_VERSION, requiredLabels.get(VERSION_LABEL_NAME));
```

公共标签与必需标签合并：

```java
Map<String, String> labels = combineLabels(context, metricNode, of("key1", "value1"));
assertEquals(5, labels.size());               // 1 个公共 + 4 个必需
assertEquals("value1", labels.get("key1"));
```

制造真实流量使仓库有数据，再查询时间窗（集成式测试）：

```java
SentinelTemplate sentinelTemplate = new SentinelTemplate();
for (int i = 0; i < 100; i++) {
    sentinelTemplate.call(resource, () -> sleep(10));
}

SentinelMetricsRepository sentinelMetricsRepository = new SentinelMetricsRepository();
try {
    addEntryCallback(sentinelMetricsRepository);
    Map<String, List<MetricNode>> contextMetricNodesMap = getContextMetricNodesMap(60000);
    do {
        sleep(100);
        contextMetricNodesMap = getContextMetricNodesMap(60000);
    } while (contextMetricNodesMap.isEmpty());
    assertFalse(contextMetricNodesMap.isEmpty());
} finally {
    removeEntryCallback(sentinelMetricsRepository.getClass());
}
```

降级路径被直接断言：

```java
assertSame(emptyMap(), getContextMetricNodesMap(1000)); // 仓库未就绪
```

## 指标参考

导出的指标族（全部为 `GAUGE`，全部携带 4 个必需标签）：

| 书写时的指标名 | 导出名（经 `buildMetricName`） | 含义 |
|:---|:---|:---|
| `sentinel_rt` | `sentinel_rt_millseconds` | 采样窗口内的平均响应时间 |
| `sentinel_concurrency` | `sentinel_concurrency` | 在途并发 |
| `sentinel_success_qps` | `sentinel_success_qps` | 成功 QPS |
| `sentinel_pass_qps` | `sentinel_pass_qps` | 通过 QPS |
| `sentinel_occupied_pass_qps` | `sentinel_occupied_pass_qps` | 占用通过 QPS |
| `sentinel_block_qps` | `sentinel_block_qps` | 阻断 QPS |
| `sentinel_exception_qps` | `sentinel_exception_qps` | 异常 QPS |

## 注意事项与限制

- Sentinel 的指标节点由其自身的指标定时器按秒产出，因此 `getContextMetricNodesMap` 在回调未安装或没有流量时返回空。
  出厂测试采用轮询直到出现数据，而不是断言立即有结果。
- `sentinel_exception_qps` 的 help 文本是 `"The Block QPS of the Sentinel Resource"`——目录中的复制遗留，在本分支上
  Prometheus 的 HELP 行显示的就是它。
- `sentinel_rt` 的单位在源码中拼作 `millseconds`，且该拼写被写进了导出名。
- 对未知标签名，`getRequiredLabelValue` 抛 `NullPointerException`（非受检、也无描述）；`ORIGIN_LABEL_NAME` 与
  `TIMESTAMP_LABEL_NAME` 就属于此类——它们由渲染模块处理，而非本工具类。
- 目录在原理上是可变的（`ofList` 产生的 `List`），但全仓库都按只读对待；模块内没有任何重新注册或修改逻辑。

## 相关文档

- [English User Guide](../metrics-alibaba-sentinel.md)
- [开发者文档 —— metrics-alibaba-sentinel](../../developer-guide/current/zh/metrics-alibaba-sentinel.md)
- [用户文档 —— 1.x / release-1.x 分支](../1.x/zh/metrics-alibaba-sentinel.md)
- [本分支用户文档索引](./README.md)
