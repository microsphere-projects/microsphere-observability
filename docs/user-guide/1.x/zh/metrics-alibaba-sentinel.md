# metrics-alibaba-sentinel 用户文档（1.x 版本线）

[English version](../metrics-alibaba-sentinel.md) · [用户文档索引](./README.md)

## 概述

`microsphere-observability-metrics-alibaba-sentinel` 把 Alibaba Sentinel 的 `MetricNode` 数据映射到 `metrics-commons`
的中立词汇上：`SentinelMetricsConstants` 定义了 7 个指标及其标签名，`SentinelMetricUtitls` 持有指标族目录以及供
`metrics-micrometer` 和 `metrics-prometheus` 使用的读取方法。

在本分支上，模块 pom 与 Java 源码与 `release` **逐字节一致**（`git diff` 在
`microsphere-observability-metrics-alibaba-sentinel/` 下无变更文件）。1.x 使用者真正会遇到的是周边解析到的版本，以及
下游 Sentinel 指标自动装配方式的差异。

## 版本与兼容性

| 属性 | 值 |
|:---|:---|
| 分析分支 | `release-1.x` |
| Maven 坐标 | `io.github.microsphere-projects:microsphere-observability-metrics-alibaba-sentinel` |
| 版本线 | `0.1.x`（reactor `${revision}` = `0.1.4-SNAPSHOT`，最新已发布标签 `0.1.3`） |
| Java 版本 | 1.x reactor pom 未声明（继承自 `microsphere-spring-cloud-parent:0.1.26`） |
| 两个类型的 `@since` | `1.0.0` |
| 与 `current` 的 API 差异 | 无 |

| 依赖 | 作用域 | 本版本线的版本 |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | `${revision}` = `0.1.x` |
| `com.alibaba.csp:sentinel-core` | optional | 由 `microsphere-alibaba-sentinel-dependencies:0.1.5` 管理 |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `0.1.5` 版本线 |
| `microsphere-java-core`、`microsphere-annotation-processor` | optional | 上游 BOM |

Spring 兼容性继承自父工程 profile（默认项加粗），且只通过 Sentinel BOM 影响到本模块，因为本模块自身不依赖 Spring。

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-hoxton` | 2.3.12.RELEASE | Hoxton.SR12 | 2.2.10 |
| `spring-cloud-2020` | 2.4.13 | 2020.0.5 | 2021.1 |
| **`spring-cloud-2021`（默认）** | **2.6.15** | **2021.0.9** | **2021.0.6.1** |

## 安装

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-observability-metrics-alibaba-sentinel</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
</dependency>
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-alibaba-sentinel-commons</artifactId>
</dependency>
```

只有当你自行搭建导出链路时才需要显式声明。在本版本线上，Spring Boot 应用通常通过
[metrics-spring-boot](./metrics-spring-boot.md) 获得本模块，其中的 `SentinelMetricsAutoConfiguration`
同时由 `META-INF/spring/…AutoConfiguration.imports` 与旧版 `META-INF/spring.factories` 注册。

## 快速上手

```java
import io.microsphere.metrics.commons.MetricFamily;
import io.microsphere.metrics.sentinel.util.SentinelMetricUtitls;

import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.*;

MetricFamily first = getMetricFamily(0);          // sentinel_rt，单位 "millseconds"，GAUGE
double rt = getMetricValue(metricNode, 0);        // MetricNode::getRt
Map<String, String> labels = getRequiredLabels("my-context", metricNode);
Map<String, List<MetricNode>> nodes = getContextMetricNodesMap(60000L);
```

## API 说明

与 `current` 版本线完全一致；此处完整重述以便本文档独立可读。

### `SentinelMetricsConstants`

所有常量都以 `PREFIX = "sentinel_"` 为前缀。

| 常量 | 值 | 作用 |
|:---|:---|:---|
| `ORIGIN_LABEL_NAME` | `sentinel_origin` | 已声明，但不在必需标签集合中 |
| `RESOURCE_LABEL_NAME` | `sentinel_resource` | Sentinel 资源 |
| `CONTEXT_LABEL_NAME` | `sentinel_context` | 入口上下文 |
| `RESOURCE_TYPE_LABEL_NAME` | `sentinel_resource_type` | 资源类型 |
| `VERSION_LABEL_NAME` | `sentinel_version` | Sentinel 版本 |
| `TIMESTAMP_LABEL_NAME` | `sentinel_timestamp` | 由 Prometheus `MultiCollector` 追加 |
| `RT_METRIC_NAME` | `sentinel_rt` | 响应时间 |
| `CONCURRENCY_METRIC_NAME` | `sentinel_concurrency` | 并发数 |
| `SUCCESS_QPS_METRIC_NAME` | `sentinel_success_qps` | 成功 QPS |
| `PASS_QPS_METRIC_NAME` | `sentinel_pass_qps` | 通过 QPS |
| `OCCUPIED_PASS_QPS_METRIC_NAME` | `sentinel_occupied_pass_qps` | 占用通过 QPS |
| `BLOCK_QPS_METRIC_NAME` | `sentinel_block_qps` | 阻断 QPS |
| `EXCEPTION_QPS_METRIC_NAME` | `sentinel_exception_qps` | 异常 QPS |

### `SentinelMetricUtitls`

| 成员 / 方法 | 契约 |
|:---|:---|
| `REQUIRED_LABEL_NAMES` | 4 个必需标签的 `Set<String>`，顺序为 `resource, context, resource_type, version` |
| `METRIC_FAMILIES` | 7 个 `GAUGE` 指标族：rt（单位 `millseconds`）、concurrency、success_qps、pass_qps、occupied_pass_qps、block_qps、exception_qps |
| `METRIC_NODE_TO_VALUE_FUNCTIONS` | 7 个函数，与 `METRIC_FAMILIES` 索引对齐：`getRt`、`getConcurrency`、`getSuccessQps`、`getPassQps`、`getOccupiedPassQps`、`getBlockQps`、`getExceptionQps` |
| `REQUIRED_LABEL_NAMES_SIZE` / `METRIC_FAMILIES_SIZE` | `4` / `7` |
| `getMetricFamily(int)` | 越界抛 `IndexOutOfBoundsException` |
| `buildMetricName(MetricFamily)` / `buildMetricName(String, String)` | 单位空白 → 名称不变；否则 `name + "_" + unit` |
| `getContextMetricNodesMap(long interval)` | 以 context 为键、覆盖 `[now - interval, now]` 的非空只读 Map；`SentinelMetricsRepository` 未就绪时返回 `Collections.emptyMap()` 并记录警告日志 |
| `combineLabels(String, MetricNode, Map)` | 先公共标签再 4 个必需标签；非空只读 Map |
| `getRequiredLabels(String, MetricNode)` | 仅 4 个必需标签 |
| `getRequiredLabelValue(String, String, MetricNode)` | 未知标签名抛 `NullPointerException` |
| `getMetricValue(MetricNode, int)` | 索引超出 `0..6` 抛 `IndexOutOfBoundsException`；节点为 `null` 抛 `NullPointerException`；返回 `doubleValue()` |

标签取值来源：`metricNode.getResource()`、入参 `context`、
`SentinelUtils.getResourceTypeAsString(metricNode.getClassification())`、`Constants.SENTINEL_VERSION`。

## 代码示例

出自 `src/test/java/io/microsphere/metrics/sentinel/util/SentinelMetricUtitlsTest.java`（本分支内容相同）：

```java
metricNode = new MetricNode();
metricNode.setResource("test-resource");
metricNode.setClassification(COMMON_WEB);
metricNode.setTimestamp(timestamp);

assertEquals(4, REQUIRED_LABEL_NAMES.size());
assertEquals(7, METRIC_FAMILIES_SIZE);
assertEquals(RT_METRIC_NAME + "_millseconds", buildMetricName(getMetricFamily(0)));
assertEquals("COMMON_WEB", getRequiredLabelValue(RESOURCE_TYPE_LABEL_NAME, context, metricNode));
assertThrows(NullPointerException.class, () -> getRequiredLabelValue("not-found", context, metricNode));
assertThrows(IndexOutOfBoundsException.class, () -> getMetricValue(metricNode, 7));
```

制造流量并轮询仓库：

```java
SentinelTemplate sentinelTemplate = new SentinelTemplate();
for (int i = 0; i < 100; i++) {
    sentinelTemplate.call(resource, () -> sleep(10));
}
SentinelMetricsRepository repository = new SentinelMetricsRepository();
try {
    addEntryCallback(repository);
    Map<String, List<MetricNode>> map = getContextMetricNodesMap(60000);
    do {
        sleep(100);
        map = getContextMetricNodesMap(60000);
    } while (map.isEmpty());
} finally {
    removeEntryCallback(repository.getClass());
}
```

## 注意事项与限制

- 与 `release` 相同的目录瑕疵：`sentinel_exception_qps` 复用了 “Block QPS” 的 help 文本，且导出名中的单位拼作
  `millseconds`。
- 本模块的上限取决于背后的 `SentinelMetricsRepository`；1.x 上它来自 `microsphere-alibaba-sentinel-commons` `0.1.5`，
  其 `NodeEntryCallback` 装配方式与 0.2.x 版本线不同——跨版本线升级前请阅读
  [metrics-spring-boot（1.x）](./metrics-spring-boot.md) 的自动配置说明。
- 若 classpath 上没有 `sentinel-core` 与 `microsphere-alibaba-sentinel-commons`，所有读取都退化为空 Map 而非报错。

## 与 `current` 版本线的差异

| 项目 | `0.2.x`（`release`） | `0.1.x`（`release-1.x`） |
|:---|:---|:---|
| 模块源码与 pom | — | 完全一致 |
| Sentinel / microsphere-alibaba-sentinel BOM | `0.2.5` | `0.1.5` |
| 默认 Spring Boot / Spring Cloud | 3.5.15 / 2025.0.3 | 2.6.15 / 2021.0.9 |
| 下游注册方式 | 仅 `.imports` | `.imports` + `spring.factories` |

## 相关文档

- [English User Guide](../metrics-alibaba-sentinel.md)
- [开发者文档 —— metrics-alibaba-sentinel（1.x）](../../developer-guide/1.x/zh/metrics-alibaba-sentinel.md)
- [用户文档 —— current / release 分支](../current/zh/metrics-alibaba-sentinel.md)
- [本分支用户文档索引](./README.md)
