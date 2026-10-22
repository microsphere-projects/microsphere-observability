# metrics-alibaba-sentinel — User Guide

[中文版](./zh/metrics-alibaba-sentinel.md) · [User Guide index](./README.md)

## Overview

`microsphere-observability-metrics-alibaba-sentinel` turns Alibaba Sentinel's internal `MetricNode` data into the
backend-neutral metric vocabulary of `metrics-commons`. It ships no registry, no collector and no Spring code; it is the
single source of truth for **what Sentinel metrics exist, what they are called, which labels they carry and how a
`MetricNode` yields each value**.

- `SentinelMetricsConstants` — the metric-name and label-name constants.
- `SentinelMetricUtitls` — the catalogue of 7 metric families, the 4 required labels, and the readers that pull values
  and labels out of a Sentinel `MetricNode`.

Two consumers use it: `metrics-micrometer` (binds the values as Micrometer meters) and `metrics-prometheus` (renders
them as a Prometheus `MultiCollector` / legacy `Collector`).

## Version and compatibility

| Attribute | Value |
|:---|:---|
| Branch analysed | `release` |
| Maven coordinates | `io.github.microsphere-projects:microsphere-observability-metrics-alibaba-sentinel` |
| Version line | `0.2.x` (reactor `${revision}` = `0.2.4-SNAPSHOT`, latest released tag `0.2.3`) |
| Java version | `17` |
| `@since` of both types | `1.0.0` |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-alibaba-sentinel](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-alibaba-sentinel) |

| Dependency | Scope | Version source |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | `${revision}` — same reactor |
| `com.alibaba.csp:sentinel-core` | optional | managed by `microsphere-alibaba-sentinel-dependencies:0.2.5` |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `0.2.5` line |
| `microsphere-java-core`, `microsphere-annotation-processor` | optional | upstream BOMs |

`sentinel-core` and `microsphere-alibaba-sentinel-commons` are **optional**: the module compiles without them on the
classpath, but `getContextMetricNodesMap(long)` then has no repository to read from and returns an empty map.

Spring compatibility is inherited, not used directly (the module has no Spring dependency). The reactor parent
`microsphere-spring-cloud-parent:0.2.26` selects Spring generations by profile, default in bold:

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-2022` | 3.0.13 | 2022.0.5 | 2022.0.0.2 |
| `spring-cloud-2023` | 3.3.13 | 2023.0.6 | 2023.0.3.4 |
| `spring-cloud-2024` | 3.4.13 | 2024.0.3 | 2023.0.3.4 |
| **`spring-cloud-2025` (default)** | **3.5.15** | **2025.0.3** | **2025.0.0.0** |
| `spring-cloud-2025.1` | 4.0.8 | 2025.1.3 | 2025.1.0.0 |

## Installation

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-observability-metrics-alibaba-sentinel</artifactId>
</dependency>

<!-- the two optional dependencies have to be added explicitly -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
</dependency>
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-alibaba-sentinel-commons</artifactId>
</dependency>
```

Versions come from the `microsphere-observability-dependencies` BOM; see
[dependencies](./dependencies.md). In a Spring Boot application you normally get this module transitively through
[metrics-spring-boot](./metrics-spring-boot.md) instead of declaring it.

## Quick start

```java
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import io.microsphere.metrics.commons.MetricFamily;
import io.microsphere.metrics.sentinel.util.SentinelMetricUtitls;

import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RT_METRIC_NAME;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.*;

// 1. the fixed catalogue
MetricFamily first = getMetricFamily(0);            // sentinel_rt, unit "millseconds", GAUGE
int familyCount = METRIC_FAMILIES_SIZE;             // 7
int labelCount = REQUIRED_LABEL_NAMES.size();       // 4

// 2. read a value out of a Sentinel MetricNode by catalogue index
double rt = getMetricValue(metricNode, 0);          // MetricNode::getRt

// 3. build the label map for one data point
Map<String, String> labels = getRequiredLabels("my-context", metricNode);
```

## API reference

### `io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants`

An `interface` holding `String` constants, all prefixed with `PREFIX = "sentinel_"`.

| Constant | Value | Role |
|:---|:---|:---|
| `PREFIX` | `sentinel_` | Shared prefix |
| `ORIGIN_LABEL_NAME` | `sentinel_origin` | Label for the caller origin — declared but not part of the required label set |
| `RESOURCE_LABEL_NAME` | `sentinel_resource` | Label for the Sentinel resource |
| `CONTEXT_LABEL_NAME` | `sentinel_context` | Label for the entry context |
| `RESOURCE_TYPE_LABEL_NAME` | `sentinel_resource_type` | Label for the resource type |
| `VERSION_LABEL_NAME` | `sentinel_version` | Label carrying the Sentinel version |
| `TIMESTAMP_LABEL_NAME` | `sentinel_timestamp` | Label carrying the sample timestamp — added by the Prometheus `MultiCollector`, not by `getRequiredLabels` |
| `RT_METRIC_NAME` | `sentinel_rt` | Response time |
| `CONCURRENCY_METRIC_NAME` | `sentinel_concurrency` | Current concurrency |
| `SUCCESS_QPS_METRIC_NAME` | `sentinel_success_qps` | Success QPS |
| `PASS_QPS_METRIC_NAME` | `sentinel_pass_qps` | Pass QPS |
| `OCCUPIED_PASS_QPS_METRIC_NAME` | `sentinel_occupied_pass_qps` | Occupied pass QPS |
| `BLOCK_QPS_METRIC_NAME` | `sentinel_block_qps` | Block QPS |
| `EXCEPTION_QPS_METRIC_NAME` | `sentinel_exception_qps` | Exception QPS |

### `io.microsphere.metrics.sentinel.util.SentinelMetricUtitls`

`public abstract class SentinelMetricUtitls implements Utils` — a utility class with a private constructor; the class name
keeps the historical spelling `Utitls`.

Static state:

| Member | Type | Content |
|:---|:---|:---|
| `REQUIRED_LABEL_NAMES` | `Set<String>` | `sentinel_resource`, `sentinel_context`, `sentinel_resource_type`, `sentinel_version` — the key set of the internal label-to-extractor map, so iteration order is that declaration order |
| `METRIC_FAMILIES` | `List<MetricFamily>` | 7 families, all `MetricType.GAUGE`, in the order rt, concurrency, success_qps, pass_qps, occupied_pass_qps, block_qps, exception_qps |
| `METRIC_NODE_TO_VALUE_FUNCTIONS` | `List<Function<MetricNode, Number>>` | `getRt`, `getConcurrency`, `getSuccessQps`, `getPassQps`, `getOccupiedPassQps`, `getBlockQps`, `getExceptionQps` — index-aligned with `METRIC_FAMILIES` |
| `REQUIRED_LABEL_NAMES_SIZE` | `int` | 4 |
| `METRIC_FAMILIES_SIZE` | `int` | 7 |

The catalogue, as declared:

| Index | Metric name | Unit | Type | Extracted from |
|:---|:---|:---|:---|:---|
| 0 | `sentinel_rt` | `millseconds` | `GAUGE` | `MetricNode::getRt` |
| 1 | `sentinel_concurrency` | — | `GAUGE` | `MetricNode::getConcurrency` |
| 2 | `sentinel_success_qps` | — | `GAUGE` | `MetricNode::getSuccessQps` |
| 3 | `sentinel_pass_qps` | — | `GAUGE` | `MetricNode::getPassQps` |
| 4 | `sentinel_occupied_pass_qps` | — | `GAUGE` | `MetricNode::getOccupiedPassQps` |
| 5 | `sentinel_block_qps` | — | `GAUGE` | `MetricNode::getBlockQps` |
| 6 | `sentinel_exception_qps` | — | `GAUGE` | `MetricNode::getExceptionQps` |

Index alignment between `METRIC_FAMILIES` and `METRIC_NODE_TO_VALUE_FUNCTIONS` is the contract everything downstream
relies on; the tests assert both lists element by element.

Methods:

| Method | Contract |
|:---|:---|
| `getMetricFamily(int index)` | Returns the family at `index`; `IndexOutOfBoundsException` when out of range |
| `buildMetricName(MetricFamily)` | Convenience overload; delegates to `buildMetricName(name, unit)` |
| `buildMetricName(String name, String unit)` | If `unit` is blank or `null`, returns `name`; otherwise `name + "_" + unit`. This is why `sentinel_rt` is exported as `sentinel_rt_millseconds` |
| `getContextMetricNodesMap(long interval)` | Returns a non-null, read-only `Map<String, List<MetricNode>>` keyed by context, covering `[now - interval, now]`. Reads the `SentinelMetricsRepository` singleton; when it is not ready it logs `SentinelMetricsRepository is not ready, please check whether it is configured` and returns `Collections.emptyMap()` |
| `combineLabels(String context, MetricNode, Map<String, String> commonLabels)` | Non-null read-only map = `commonLabels` first, then the 4 required labels (linked hash map sized `REQUIRED_LABEL_NAMES_SIZE + commonLabels.size()`) |
| `getRequiredLabels(String context, MetricNode)` | Non-null read-only map of the 4 required labels only |
| `getRequiredLabelValue(String labelName, String context, MetricNode)` | Applies the extractor for `labelName`; **`NullPointerException`** if the name is not one of the 4 required labels (documented in the javadoc, asserted by the test) |
| `getMetricValue(MetricNode, int index)` | `IndexOutOfBoundsException` if `index` is outside `0..6`; `NullPointerException` if `metricNode` is `null`; returns `doubleValue()` of the extracted number |

Label extraction rules (from the internal `REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP`):

| Label | Value source |
|:---|:---|
| `sentinel_resource` | `metricNode.getResource()` |
| `sentinel_context` | the `context` argument |
| `sentinel_resource_type` | `SentinelUtils.getResourceTypeAsString(metricNode.getClassification())` — e.g. classification `COMMON_WEB` renders as `"COMMON_WEB"` |
| `sentinel_version` | `com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION` |

## Configuration

This module reads no configuration of its own. Two external inputs decide what it returns:

| Input | How it is provided | Effect |
|:---|:---|:---|
| `SentinelMetricsRepository` | `io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository`, obtained via `getSentinelMetricsRepository()`; populated by a Sentinel `NodeEntryCallback` registered through `ProcessorSlotCallbackUtils.addEntryCallback(...)` | Without it, `getContextMetricNodesMap` yields an empty map and no metric is exported |
| collection interval | the `long interval` argument, in milliseconds | Selects the `[now - interval, now]` window on the repository |

In Spring Boot applications both are wired for you by
[`SentinelMetricsAutoConfiguration`](./metrics-spring-boot.md), whose interval default lives in
`META-INF/config/default/metrics.yaml`.

## Code examples

All of the following are taken from
`src/test/java/io/microsphere/metrics/sentinel/util/SentinelMetricUtitlsTest.java`.

Building a `MetricNode` the way Sentinel's metric timer would (test fixture):

```java
metricNode = new MetricNode();
metricNode.setResource("test-resource");
metricNode.setClassification(COMMON_WEB);
metricNode.setTimestamp(timestamp);
```

Reading labels:

```java
Map<String, String> requiredLabels = getRequiredLabels(context, metricNode);
assertEquals(4, requiredLabels.size());
assertEquals("COMMON_WEB", requiredLabels.get(RESOURCE_TYPE_LABEL_NAME));
assertEquals(SENTINEL_VERSION, requiredLabels.get(VERSION_LABEL_NAME));
```

Combining common labels with the required ones:

```java
Map<String, String> labels = combineLabels(context, metricNode, of("key1", "value1"));
assertEquals(5, labels.size());               // 1 common + 4 required
assertEquals("value1", labels.get("key1"));
```

Driving real traffic so that the repository has data, then querying the window (integration-style test):

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

The graceful-degradation case, asserted directly:

```java
assertSame(emptyMap(), getContextMetricNodesMap(1000)); // repository not ready
```

## Metrics reference

Exported metric families (all `GAUGE`, all labelled with the 4 required labels):

| Metric (as authored) | Metric (as exported, after `buildMetricName`) | Meaning |
|:---|:---|:---|
| `sentinel_rt` | `sentinel_rt_millseconds` | Average response time in the sampled window |
| `sentinel_concurrency` | `sentinel_concurrency` | In-flight concurrency |
| `sentinel_success_qps` | `sentinel_success_qps` | Successful QPS |
| `sentinel_pass_qps` | `sentinel_pass_qps` | Passed QPS |
| `sentinel_occupied_pass_qps` | `sentinel_occupied_pass_qps` | Occupied-pass QPS |
| `sentinel_block_qps` | `sentinel_block_qps` | Blocked QPS |
| `sentinel_exception_qps` | `sentinel_exception_qps` | Exception QPS |

## Notes and limitations

- Sentinel's metric nodes are produced per second by Sentinel's own metric timer; `getContextMetricNodesMap` therefore
  returns nothing until the callback is installed and traffic has flowed. The shipped test loops until data appears
  rather than asserting an immediate result.
- The help text of `sentinel_exception_qps` is `"The Block QPS of the Sentinel Resource"` — a copy artefact in the
  catalogue; it is what Prometheus HELP lines show on this branch.
- The unit of `sentinel_rt` is spelled `millseconds` in the source, and that spelling is baked into the exported metric
  name.
- `getRequiredLabelValue` fails with `NullPointerException` (not a checked or descriptive error) for an unknown label
  name; `ORIGIN_LABEL_NAME` and `TIMESTAMP_LABEL_NAME` are such names — they are handled by the rendering modules, not by
  this utility.
- The catalogue is mutable in principle (`List` obtained from `ofList`), but is treated as read-only everywhere; nothing
  in the module re-registers or edits it.

## Related documents

- [中文用户文档](./zh/metrics-alibaba-sentinel.md)
- [Developer Guide — metrics-alibaba-sentinel](../../developer-guide/current/metrics-alibaba-sentinel.md)
- [User Guide — 1.x / release-1.x branch](../1.x/metrics-alibaba-sentinel.md)
- [User Guide index for this branch](./README.md)
