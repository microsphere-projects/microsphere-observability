# metrics-commons — User Guide

[中文版](./zh/metrics-commons.md) · [User Guide index](./README.md)

## Overview

`microsphere-observability-metrics-commons` is the vocabulary module of the Microsphere Observability metrics family. It
carries no collector, no registry and no Spring code — only two types that describe *what a metric is* so that the
downstream modules (`metrics-alibaba-sentinel`, `metrics-micrometer`, `metrics-prometheus`) can share one metric
definition instead of duplicating it per backend.

- `MetricFamily` — an immutable, backend-neutral descriptor of a Prometheus metric family: name, unit, type, help text
  and label names.
- `MetricType` — the enumeration of Prometheus metric types.

Both types are pure data carriers. Anything that converts them into a concrete client model (Prometheus
`MetricFamilyDescriptor`, Micrometer `Meter`) lives in the modules that depend on this one.

## Version and compatibility

| Attribute | Value |
|:---|:---|
| Branch analysed | `release` |
| Maven coordinates | `io.github.microsphere-projects:microsphere-observability-metrics-commons` |
| Version line | `0.2.x` (reactor `${revision}` = `0.2.4-SNAPSHOT`, latest released tag `0.2.3`) |
| Java version | `17` (declared by the reactor root pom) |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-commons](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-commons) |
| `@since` of both types | `1.0.0` |

This module has no dependency on Spring, so it is unaffected by the Spring Boot / Spring Cloud profile matrix. The
versions below are inherited from the reactor parent `microsphere-spring-cloud-parent:0.2.26`, which selects Spring
Boot/Spring Cloud through build profiles (default in bold):

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-2022` | 3.0.13 | 2022.0.5 | 2022.0.0.2 |
| `spring-cloud-2023` | 3.3.13 | 2023.0.6 | 2023.0.3.4 |
| `spring-cloud-2024` | 3.4.13 | 2024.0.3 | 2023.0.3.4 |
| **`spring-cloud-2025` (default)** | **3.5.15** | **2025.0.3** | **2025.0.0.0** |
| `spring-cloud-2025.1` | 4.0.8 | 2025.1.3 | 2025.1.0.0 |

Prometheus client compatibility matters only to the modules that *render* these descriptors: the reactor manages
`io.prometheus:prometheus-metrics-bom` at version `1.9.0` on this branch (see
[metrics-prometheus](./metrics-prometheus.md)).

## Installation

The module is declared `compile` scope by `metrics-alibaba-sentinel`, `metrics-micrometer` and `metrics-prometheus`, so
adding any of those pulls it in transitively. Declare it directly only when you build your own metric catalogue:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-observability-dependencies</artifactId>
            <version>${microsphere-observability.version}</version>
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

`microsphere-observability-dependencies` pins the version, so `<version>` is omitted on the module itself.

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

String name = family.getName();   // "sentinel_rt"
String unit = family.getUnit();   // "millseconds"
MetricType type = family.getType(); // GAUGE
```

## API reference

### `io.microsphere.metrics.commons.MetricType`

`public enum MetricType` — Prometheus metric types, documented against
[prometheus.io/docs/concepts/metric_types](https://prometheus.io/docs/concepts/metric_types/).

| Constant | Meaning |
|:---|:---|
| `COUNTER` | Monotonically increasing value |
| `GAUGE` | Value that may go up and down |
| `HISTOGRAM` | Bucketed observations |
| `SUMMARY` | Quantile summary of observations |
| `INFO` | Constant informational metric |
| `STATESET` | Set of possible states, only one active |
| `UNKNOWN` | Type not known — a fallback for sources that expose no type |

The enumeration is deliberately name-compatible with the Prometheus client model: `metrics-prometheus` bridges it with
`io.prometheus.metrics.model.registry.MetricType.valueOf(metricType.name())`, so a constant whose name has no
counterpart in that client enum cannot be rendered by the Prometheus modules. Every family shipped by this repository
uses `GAUGE`.

### `io.microsphere.metrics.commons.MetricFamily`

`public class MetricFamily` — the model of a metric family; the class-level javadoc positions it against
`io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor` and the legacy
`io.prometheus.client.Collector.MetricFamilySamples`.

| Member | Signature | Description |
|:---|:---|:---|
| `builder()` | `public static Builder builder()` | Creates an empty builder — the only public way to obtain an instance |
| `MetricFamily(...)` | `MetricFamily(String name, String unit, MetricType type, String help, Set<String> labelNames)` | Package-private constructor; not callable outside `io.microsphere.metrics.commons` |
| `getName()` | `public String getName()` | Metric name as authored, before any Prometheus sanitising or unit suffix is applied |
| `getUnit()` | `public String getUnit()` | Base unit, may be `null`; rendering modules append it to the name |
| `getType()` | `public MetricType getType()` | The metric type |
| `getHelp()` | `public String getHelp()` | Human-readable HELP line |
| `getLabelNames()` | `public Set<String> labelNames` → returns the set handed to the builder | Label names (not values) declared by the family |

`MetricFamily.Builder` (nested, public):

| Method | Returns | Notes |
|:---|:---|:---|
| `name(String name)` | `Builder` | Fluent |
| `unit(String unit)` | `Builder` | Optional; `null` or blank means "no unit" |
| `type(MetricType type)` | `Builder` | Optional in the type system — nothing validates it |
| `help(String help)` | `Builder` | Optional |
| `labelNames(Set<String> labelNames)` | `Builder` | The set is stored by reference, not copied |
| `build()` | `MetricFamily` | Never returns `null`; an unset field stays `null` |

Nullability and validation: the builder performs no checks and the constructor assigns fields directly. A family may
therefore be built with a `null` name or `null` type; validity is enforced where the descriptor is consumed, not here.
This is the contract the shipped code relies on — `SentinelMetricUtitls` supplies every field explicitly.

## Code examples

The module ships no unit tests of its own; the reference usage below comes from `metrics-alibaba-sentinel` and its test
`SentinelMetricUtitlsTest` (see [metrics-alibaba-sentinel](./metrics-alibaba-sentinel.md)).

Defining a catalogue of families (production code):

```java
public static final Set<String> REQUIRED_LABEL_NAMES = Set.of(
        RESOURCE_LABEL_NAME, CONTEXT_LABEL_NAME, RESOURCE_TYPE_LABEL_NAME, VERSION_LABEL_NAME);

public static final List<MetricFamily> METRIC_FAMILIES = ofList(
        builder().name(RT_METRIC_NAME).unit("millseconds").type(GAUGE)
                .help("The Response Time (RT) of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build(),
        builder().name(CONCURRENCY_METRIC_NAME).type(GAUGE)
                .help("The Concurrency of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build()
        // …5 more families
);
```

Asserting the catalogue (test code, from `SentinelMetricUtitlsTest#testConstants`):

```java
assertEquals(4, REQUIRED_LABEL_NAMES.size());
assertEquals(7, METRIC_FAMILIES_SIZE);
assertEquals(RT_METRIC_NAME, METRIC_FAMILIES.get(0).getName());
assertEquals(EXCEPTION_QPS_METRIC_NAME, METRIC_FAMILIES.get(6).getName());
```

The unit influences the exported metric name, which the same test pins down:

```java
assertEquals(RT_METRIC_NAME + "_millseconds", buildMetricName(getMetricFamily(0)));
assertEquals(CONCURRENCY_METRIC_NAME, buildMetricName(getMetricFamily(1))); // no unit -> name unchanged
```

## Notes and limitations

- Fields are read-only but the class defines no `equals`, `hashCode` or `toString`; identity, not value, semantics
  apply. Keep a single instance per metric (as `METRIC_FAMILIES` does) if you compare or cache by instance.
- `labelNames(Set)` keeps the reference it is given. `SentinelMetricUtitls` hands it an immutable `keySet()` view,
  which is the pattern to follow; a mutable set shared elsewhere can change a "frozen" descriptor.
- There is no name validation: Prometheus sanitising (`sanitizeMetricName`) happens in `metrics-prometheus`. A name
  such as `sentinel.rt` therefore survives this layer and is rewritten only at render time.
- Dependencies of this module are `microsphere-java-core` and `microsphere-annotation-processor`, both `optional`, so
  importing it adds nothing to a runtime classpath.

## Related documents

- [中文用户文档](./zh/metrics-commons.md)
- [Developer Guide — metrics-commons](../../developer-guide/current/metrics-commons.md)
- [User Guide — 1.x / release-1.x branch](../1.x/metrics-commons.md)
- [User Guide index for this branch](./README.md)
