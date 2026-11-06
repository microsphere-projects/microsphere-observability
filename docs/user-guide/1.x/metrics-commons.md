# metrics-commons — User Guide (1.x line)

[中文版](./zh/metrics-commons.md) · [User Guide index](./README.md)

## Overview

`microsphere-observability-metrics-commons` is the vocabulary module of the Microsphere Observability metrics family on
the `release-1.x` branch: two backend-neutral types — `MetricFamily` (name, unit, type, help text, label names) and
`MetricType` (Prometheus metric types) — shared by `metrics-alibaba-sentinel`, `metrics-micrometer` and
`metrics-prometheus`.

The Java sources of this module are **byte-identical to the `release` (0.2.x) branch**, which `git diff` between the two
branches confirms (no changed files under `microsphere-observability-metrics-commons/`). Everything below that differs is
version plumbing, not API.

## Version and compatibility

| Attribute | Value |
|:---|:---|
| Branch analysed | `release-1.x` |
| Maven coordinates | `io.github.microsphere-projects:microsphere-observability-metrics-commons` |
| Version line | `0.1.x` (reactor `${revision}` = `0.1.4-SNAPSHOT`, latest released tag `0.1.3`) |
| Java version | not declared by the 1.x reactor pom; inherited from `microsphere-spring-cloud-parent:0.1.26` |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-commons](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-commons) |
| `@since` of both types | `1.0.0` |
| API delta vs the `current` docs | none |

Spring compatibility comes from the parent's build profiles (default in bold). The 1.x line stops at Spring Boot 2.x:

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-hoxton` | 2.3.12.RELEASE | Hoxton.SR12 | 2.2.10 |
| `spring-cloud-2020` | 2.4.13 | 2020.0.5 | 2021.1 |
| **`spring-cloud-2021` (default)** | **2.6.15** | **2021.0.9** | **2021.0.6.1** |

The Prometheus client rendered from these descriptors is `io.prometheus:prometheus-metrics-bom` at version **`1.8.0`** on
this branch (vs `1.9.0` on `release`); see [metrics-prometheus](./metrics-prometheus.md) for the client split that
matters more on the 1.x line.

## Installation

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-observability-dependencies</artifactId>
            <version>${microsphere-observability.version}</version> <!-- e.g. 0.1.3 -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-observability-metrics-commons</artifactId>
    </dependency>
</dependencies>
```

Never mix the `0.1.x` BOM with modules resolved from `0.2.x`: the descriptors themselves are compatible, but the
rendering modules target different Prometheus client and Spring Boot generations.

## Quick start

```java
import io.microsphere.metrics.commons.MetricFamily;
import io.microsphere.metrics.commons.MetricType;

import static java.util.Set.of;

MetricFamily family = MetricFamily.builder()
        .name("sentinel_rt")
        .unit("millseconds")
        .type(MetricType.GAUGE)
        .help("The Response Time (RT) of the Sentinel Resource")
        .labelNames(of("sentinel_resource", "sentinel_context", "sentinel_resource_type", "sentinel_version"))
        .build();
```

## API reference

Identical to the `current` line; restated here so the 1.x document is self-contained.

### `io.microsphere.metrics.commons.MetricType`

`public enum MetricType` with `COUNTER`, `GAUGE`, `HISTOGRAM`, `SUMMARY`, `INFO`, `STATESET`, `UNKNOWN`.

| Constant | Meaning |
|:---|:---|
| `COUNTER` | Monotonically increasing value |
| `GAUGE` | Value that may go up and down |
| `HISTOGRAM` | Bucketed observations |
| `SUMMARY` | Quantile summary of observations |
| `INFO` | Constant informational metric |
| `STATESET` | Mutually exclusive state set |
| `UNKNOWN` | Type not known |

`metrics-prometheus` bridges by name (`io.prometheus.metrics.model.registry.MetricType.valueOf(type.name())`), so names
without a client counterpart cannot be rendered. On the 1.x line, note that part of the Prometheus story still runs on
the legacy `io.prometheus:simpleclient` API (see [metrics-prometheus](./metrics-prometheus.md) and
[metrics-spring-cloud](./metrics-spring-cloud.md)).

### `io.microsphere.metrics.commons.MetricFamily`

| Member | Signature | Description |
|:---|:---|:---|
| `builder()` | `public static Builder builder()` | Only public entry point |
| `MetricFamily(...)` | package-private `MetricFamily(String, String, MetricType, String, Set<String>)` | Not callable outside the package |
| `getName()` | `public String getName()` | Authored name, pre-sanitising |
| `getUnit()` | `public String getUnit()` | Base unit, may be `null` |
| `getType()` | `public MetricType getType()` | Metric type |
| `getHelp()` | `public String getHelp()` | HELP text |
| `getLabelNames()` | `public Set<String> getLabelNames()` | Label names as supplied |

`MetricFamily.Builder`: `name(String)`, `unit(String)`, `type(MetricType)`, `help(String)`,
`labelNames(Set<String>)`, `build()`. All setters are fluent and unchecked; `build()` never returns `null` and leaves
unset fields `null`. `labelNames` is stored by reference.

## Code examples

Reference usage is `SentinelMetricUtitls` in `metrics-alibaba-sentinel` (module sources are identical on this branch):

```java
public static final List<MetricFamily> METRIC_FAMILIES = ofList(
        builder().name(RT_METRIC_NAME).unit("millseconds").type(GAUGE)
                .help("The Response Time (RT) of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build(),
        builder().name(BLOCK_QPS_METRIC_NAME).type(GAUGE)
                .help("The Block QPS of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build());
```

Its test asserts the catalogue and the unit-suffix naming rule:

```java
assertEquals(7, METRIC_FAMILIES_SIZE);
assertEquals(RT_METRIC_NAME + "_millseconds", buildMetricName(getMetricFamily(0)));
assertEquals(CONCURRENCY_METRIC_NAME, buildMetricName(getMetricFamily(1)));
```

## Notes and limitations

- No `equals`/`hashCode`/`toString`: identity semantics only.
- `labelNames(Set)` is not defensively copied; pass an immutable set.
- No name validation at this layer — Prometheus sanitising happens at render time.
- Optional dependencies (`microsphere-java-core`, `microsphere-annotation-processor`) add nothing to the runtime
  classpath.

## Version differences vs the `current` line

| Item | `0.2.x` (`release`) | `0.1.x` (`release-1.x`) |
|:---|:---|:---|
| API of this module | — | identical |
| Reactor `${revision}` | `0.2.4-SNAPSHOT` | `0.1.4-SNAPSHOT` |
| Latest released version | `0.2.3` | `0.1.3` |
| `java.version` in reactor pom | `17` | not declared |
| Prometheus metrics BOM | `1.9.0` | `1.8.0` |
| Default Spring Boot / Spring Cloud | 3.5.15 / 2025.0.3 | 2.6.15 / 2021.0.9 |

## Related documents

- [中文用户文档](./zh/metrics-commons.md)
- [Developer Guide — metrics-commons (1.x)](../../developer-guide/1.x/metrics-commons.md)
- [User Guide — current / release branch](../current/metrics-commons.md)
- [User Guide index for this branch](./README.md)
