# metrics-alibaba-sentinel — User Guide (1.x line)

[中文版](./zh/metrics-alibaba-sentinel.md) · [User Guide index](./README.md)

## Overview

`microsphere-observability-metrics-alibaba-sentinel` maps Alibaba Sentinel `MetricNode` data onto the
backend-neutral vocabulary of `metrics-commons`: `SentinelMetricsConstants` names 7 metrics and their labels, and
`SentinelMetricUtitls` holds the metric-family catalogue plus the readers used by `metrics-micrometer` and
`metrics-prometheus`.

On this branch both the module pom and the Java sources are **byte-identical to `release`** (`git diff` reports no
changed file under `microsphere-observability-metrics-alibaba-sentinel/`). The differences a 1.x user meets are in the
versions resolved around it and in how the Sentinel metrics get auto-configured downstream.

## Version and compatibility

| Attribute | Value |
|:---|:---|
| Branch analysed | `release-1.x` |
| Maven coordinates | `io.github.microsphere-projects:microsphere-observability-metrics-alibaba-sentinel` |
| Version line | `0.1.x` (reactor `${revision}` = `0.1.4-SNAPSHOT`, latest released tag `0.1.3`) |
| Java version | not declared by the 1.x reactor pom (inherited from `microsphere-spring-cloud-parent:0.1.26`) |
| `@since` of both types | `1.0.0` |
| API delta vs `current` | none |

| Dependency | Scope | Version on this line |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | `${revision}` = `0.1.x` |
| `com.alibaba.csp:sentinel-core` | optional | managed by `microsphere-alibaba-sentinel-dependencies:0.1.5` |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `0.1.5` line |
| `microsphere-java-core`, `microsphere-annotation-processor` | optional | upstream BOMs |

Spring compatibility is inherited from the parent profiles (default in bold); it reaches this module only through the
Sentinel BOM, since the module itself has no Spring dependency.

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-hoxton` | 2.3.12.RELEASE | Hoxton.SR12 | 2.2.10 |
| `spring-cloud-2020` | 2.4.13 | 2020.0.5 | 2021.1 |
| **`spring-cloud-2021` (default)** | **2.6.15** | **2021.0.9** | **2021.0.6.1** |

## Installation

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

Declare it only when you build the export path yourself. On this line a Spring Boot application normally obtains the
module through [metrics-spring-boot](./metrics-spring-boot.md), whose `SentinelMetricsAutoConfiguration` is registered
both by `META-INF/spring/…AutoConfiguration.imports` and by the legacy `META-INF/spring.factories`.

## Quick start

```java
import io.microsphere.metrics.commons.MetricFamily;
import io.microsphere.metrics.sentinel.util.SentinelMetricUtitls;

import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.*;

MetricFamily first = getMetricFamily(0);          // sentinel_rt, unit "millseconds", GAUGE
double rt = getMetricValue(metricNode, 0);        // MetricNode::getRt
Map<String, String> labels = getRequiredLabels("my-context", metricNode);
Map<String, List<MetricNode>> nodes = getContextMetricNodesMap(60000L);
```

## API reference

Identical to the `current` line; restated so this document stands alone.

### `SentinelMetricsConstants`

All constants are prefixed with `PREFIX = "sentinel_"`.

| Constant | Value | Role |
|:---|:---|:---|
| `ORIGIN_LABEL_NAME` | `sentinel_origin` | Declared, not in the required label set |
| `RESOURCE_LABEL_NAME` | `sentinel_resource` | Sentinel resource |
| `CONTEXT_LABEL_NAME` | `sentinel_context` | Entry context |
| `RESOURCE_TYPE_LABEL_NAME` | `sentinel_resource_type` | Resource type |
| `VERSION_LABEL_NAME` | `sentinel_version` | Sentinel version |
| `TIMESTAMP_LABEL_NAME` | `sentinel_timestamp` | Added by the Prometheus `MultiCollector` |
| `RT_METRIC_NAME` | `sentinel_rt` | Response time |
| `CONCURRENCY_METRIC_NAME` | `sentinel_concurrency` | Concurrency |
| `SUCCESS_QPS_METRIC_NAME` | `sentinel_success_qps` | Success QPS |
| `PASS_QPS_METRIC_NAME` | `sentinel_pass_qps` | Pass QPS |
| `OCCUPIED_PASS_QPS_METRIC_NAME` | `sentinel_occupied_pass_qps` | Occupied pass QPS |
| `BLOCK_QPS_METRIC_NAME` | `sentinel_block_qps` | Block QPS |
| `EXCEPTION_QPS_METRIC_NAME` | `sentinel_exception_qps` | Exception QPS |

### `SentinelMetricUtitls`

| Member / method | Contract |
|:---|:---|
| `REQUIRED_LABEL_NAMES` | `Set<String>` of the 4 required labels, in `resource, context, resource_type, version` order |
| `METRIC_FAMILIES` | `List<MetricFamily>` of 7 `GAUGE` families: rt (unit `millseconds`), concurrency, success_qps, pass_qps, occupied_pass_qps, block_qps, exception_qps |
| `METRIC_NODE_TO_VALUE_FUNCTIONS` | 7 functions, index-aligned with `METRIC_FAMILIES`: `getRt`, `getConcurrency`, `getSuccessQps`, `getPassQps`, `getOccupiedPassQps`, `getBlockQps`, `getExceptionQps` |
| `REQUIRED_LABEL_NAMES_SIZE` / `METRIC_FAMILIES_SIZE` | `4` / `7` |
| `getMetricFamily(int)` | `IndexOutOfBoundsException` out of range |
| `buildMetricName(MetricFamily)` / `buildMetricName(String, String)` | blank unit → name unchanged; otherwise `name + "_" + unit` |
| `getContextMetricNodesMap(long interval)` | non-null read-only map keyed by context over `[now - interval, now]`; `Collections.emptyMap()` plus a warning log when the `SentinelMetricsRepository` is not ready |
| `combineLabels(String, MetricNode, Map)` | common labels first, then the 4 required labels; non-null read-only map |
| `getRequiredLabels(String, MetricNode)` | the 4 required labels only |
| `getRequiredLabelValue(String, String, MetricNode)` | `NullPointerException` for an unknown label name |
| `getMetricValue(MetricNode, int)` | `IndexOutOfBoundsException` outside `0..6`; `NullPointerException` on a `null` node; returns `doubleValue()` |

Label value sources: `metricNode.getResource()`, the `context` argument,
`SentinelUtils.getResourceTypeAsString(metricNode.getClassification())`, and `Constants.SENTINEL_VERSION`.

## Code examples

From `src/test/java/io/microsphere/metrics/sentinel/util/SentinelMetricUtitlsTest.java` (identical on this branch):

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

Driving traffic and polling the repository:

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

## Notes and limitations

- Same catalogue quirks as `release`: `sentinel_exception_qps` reuses the "Block QPS" help text, and the unit is spelled
  `millseconds` in the exported name.
- The module is only as good as the `SentinelMetricsRepository` behind it; on this line that repository comes from
  `microsphere-alibaba-sentinel-commons` `0.1.5`, whose `NodeEntryCallback` wiring differs from the 0.2.x line — check
  the auto-configuration in [metrics-spring-boot (1.x)](./metrics-spring-boot.md) before upgrading across lines.
- Without `sentinel-core` and `microsphere-alibaba-sentinel-commons` on the classpath, every read degrades to an empty
  map rather than an error.

## Version differences vs the `current` line

| Item | `0.2.x` (`release`) | `0.1.x` (`release-1.x`) |
|:---|:---|:---|
| Module sources and pom | — | identical |
| Sentinel / microsphere-alibaba-sentinel BOM | `0.2.5` | `0.1.5` |
| Default Spring Boot / Spring Cloud | 3.5.15 / 2025.0.3 | 2.6.15 / 2021.0.9 |
| Downstream registration | `.imports` only | `.imports` + `spring.factories` |

## Related documents

- [中文用户文档](./zh/metrics-alibaba-sentinel.md)
- [Developer Guide — metrics-alibaba-sentinel (1.x)](../../developer-guide/1.x/metrics-alibaba-sentinel.md)
- [User Guide — current / release branch](../current/metrics-alibaba-sentinel.md)
- [User Guide index for this branch](./README.md)
