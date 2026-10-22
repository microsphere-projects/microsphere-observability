# metrics-prometheus — User Guide

[中文版](./zh/metrics-prometheus.md) · [User Guide index](./README.md)

## Overview

`microsphere-observability-metrics-prometheus` renders the Alibaba Sentinel metric catalogue owned by
`metrics-alibaba-sentinel` into the two Prometheus client APIs still in circulation. It ships exactly two public types,
one per client API:

- `SentinelMultiCollector` — implements `io.prometheus.metrics.model.registry.MultiCollector` (client_java 1.x,
  artifact `io.prometheus:prometheus-metrics-core`), returns `MetricSnapshots`.
- `SentinelCollector` — extends the legacy `io.prometheus.client.Collector` (artifact `io.prometheus:simpleclient`),
  returns `List<Collector.MetricFamilySamples>`.

Both read the same 7 `GAUGE` families (`sentinel_rt`, `sentinel_concurrency`, `sentinel_success_qps`,
`sentinel_pass_qps`, `sentinel_occupied_pass_qps`, `sentinel_block_qps`, `sentinel_exception_qps`) and the same 4
required labels (`sentinel_resource`, `sentinel_context`, `sentinel_resource_type`, `sentinel_version`); both take a
collection window in milliseconds, both offer a fluent `commonLabel(name, value)`, and both degrade to "nothing
exported" (empty result plus a warning log) when the Sentinel metrics repository is not installed, so a scrape never
fails. Pick one per JVM, matching the Prometheus client you deploy. The module itself has no registry, no exposition
server, no push gateway and no Spring code: you hand an instance to `PrometheusRegistry` (new client) or
`CollectorRegistry` (legacy client), or let [metrics-spring-boot](./metrics-spring-boot.md) do it.

## Version and compatibility

| Attribute | Value |
|:---|:---|
| Branch analysed | `release` |
| Maven coordinates | `io.github.microsphere-projects:microsphere-observability-metrics-prometheus` |
| Version line | `0.2.x` (reactor `${revision}` = `0.2.4-SNAPSHOT`, latest released tag `0.2.3`) |
| Java version | `17` |
| `@since` of both collectors | `1.0.0` |
| Prometheus client BOM | `io.prometheus:prometheus-metrics-bom` `1.9.0` (`${prometheus.version}` in `microsphere-observability-parent`) |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-prometheus](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-prometheus) |

| Dependency | Scope | Needed at runtime for |
|:---|:---|:---|
| `microsphere-observability-metrics-commons` | compile | the backend-neutral `MetricFamily` model |
| `microsphere-observability-metrics-alibaba-sentinel` | compile | `SentinelMetricUtitls` — catalogue, labels, value readers |
| `io.prometheus:prometheus-metrics-core` | optional | `SentinelMultiCollector` only |
| `io.prometheus:simpleclient` | optional | `SentinelCollector` only |
| `com.alibaba.csp:sentinel-core` | optional | `MetricNode` — the source of every value and label |
| `io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` | optional | `SentinelMetricsRepository` behind `getContextMetricNodesMap(interval)` |
| `microsphere-java-core`, `microsphere-annotation-processor` | optional | logging/collection helpers, build-time processing |
| `junit-jupiter`, `prometheus-metrics-instrumentation-jvm`, `prometheus-metrics-exporter-httpserver`, `microsphere-java-test`, `logback-classic` | test | module tests |

Both Prometheus artifacts are **optional**, so instantiating a collector whose client API is missing ends in
`NoClassDefFoundError`. `prometheus-metrics-core` resolves to `1.9.0` from the imported BOM; `simpleclient` carries no
version in the module pom and comes from the `dependencyManagement` inherited by `microsphere-observability-parent` —
`./mvnw -pl microsphere-observability-metrics-prometheus dependency:tree` prints the effective number. The repository
README states that the `main` / `1.x` lines target Spring Cloud `2022.0.x – 2025.0.x` and `Hoxton – 2021.0.x`; Spring
generations are chosen by the reactor parent `microsphere-spring-cloud-parent:0.2.26` through profiles (default in
bold) and only matter once you consume this module from a Spring Boot application:

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

`prometheus-metrics-core` is the client `SentinelMultiCollector` needs, `simpleclient` the one `SentinelCollector`
needs; declare only the API you instantiate. Add `com.alibaba.csp:sentinel-core` and
`io.github.microsphere-projects:microsphere-alibaba-sentinel-commons` too: optional in this module, but without them no
data point is ever produced. Observability artifact versions are managed by the consumer BOM
[microsphere-observability-dependencies](./dependencies.md) (`0.2.3` for released builds); that BOM lists this project's
modules only, so outside a Spring Boot parent import `io.prometheus:prometheus-metrics-bom:1.9.0` for client versions.

## Quick start

New client (`MultiCollector`):

```java
import io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

SentinelMultiCollector collector = new SentinelMultiCollector(60_000L).commonLabel("application", "demo");
new PrometheusRegistry().register(collector);   // a MultiCollector is registered by the registry
```

Legacy client (`simpleclient`):

```java
import io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector;
import io.prometheus.client.CollectorRegistry;

SentinelCollector collector = new SentinelCollector(60_000L).commonLabel("application", "demo");
collector.register(new CollectorRegistry());    // the collector registers itself
```

That is the whole setup: each scrape calls `SentinelMetricUtitls.getContextMetricNodesMap(interval)` and renders what
Sentinel recorded in that window.

## API reference

### `io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector`

`public class SentinelMultiCollector implements io.prometheus.metrics.model.registry.MultiCollector`

| Signature | Description |
|:---|:---|
| `SentinelMultiCollector(long interval)` | `interval` is the collection window in **milliseconds**; the 7 `MetricFamilyDescriptor`s are built eagerly in this constructor |
| `SentinelMultiCollector commonLabel(String name, String value)` | Adds one constant label to every data point, returns `this`; a repeated `name` replaces its value |
| `MetricSnapshots collect()` | The `MultiCollector` contract. Walks `Map<context, List<MetricNode>>` over `[now - interval, now]`, returns one `GaugeSnapshot` per family, or an empty `MetricSnapshots` plus a warning log when nothing was recorded |
| `List<MetricFamilyDescriptor> getMetricFamilyDescriptors()` | The `MultiCollector` contract. The descriptors built at construction time, used to advertise `# HELP` / `# TYPE` |
| `Labels toLabels(String context, MetricNode metricNode)` | Public helper: the 4 required labels + configured common labels + `sentinel_timestamp`, rendered as `io.prometheus.metrics.model.snapshots.Labels` |

No method declares `throws`; the only failure modes are the `NullPointerException` / `IndexOutOfBoundsException`
contracts of `SentinelMetricUtitls`, unreachable for the fixed index range `0..6` used here. Not synchronised — finish
all `commonLabel(...)` calls before registering.

### `io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector`

`public class SentinelCollector extends io.prometheus.client.Collector`

| Signature | Description |
|:---|:---|
| `SentinelCollector(long interval)` | Collection window in **milliseconds**; nothing is built up front — family metadata is read from the catalogue on every scrape |
| `SentinelCollector commonLabel(String name, String value)` | Adds one constant label to every sample, returns `this` |
| `List<MetricFamilySamples> collect()` | The `Collector` contract. Returns 7 `MetricFamilySamples` whose names come from `SentinelMetricUtitls.buildMetricName(name, unit)` (so `sentinel_rt` is exported as `sentinel_rt_millseconds`), or `Collections.emptyList()` when the repository yields nothing |
| `register()` / `register(CollectorRegistry)` | Inherited from `io.prometheus.client.Collector`; the no-argument form installs into `CollectorRegistry.defaultRegistry` |

Label lists come from `combineLabels(context, metricNode, commonLabels)` per sample, and the node timestamp travels as
the `Sample` timestamp (`new Sample(metricName, labelNames, labelValues, value, timestampMs)`) **rather than** a
`sentinel_timestamp` label — the visible output difference from the new-client collector. Not synchronised either.

## Configuration

The module reads no properties and declares no configuration key; callers tune one constructor argument and label
pairs.

| Knob | Where | Value used in this repo's own tests | Effect |
|:---|:---|:---|:---|
| `interval` | constructor argument, milliseconds | `60000` | The `[now - interval, now]` window passed to `SentinelMetricUtitls.getContextMetricNodesMap(long)` |
| common labels | `commonLabel(String, String)` | `application=test`, `test-label=test-value` | Appended to the 4 required labels on every data point; required labels **win** on a name collision because `combineLabels` inserts common labels first |
| registry | your `PrometheusRegistry` or `CollectorRegistry` | a fresh instance per test | Decides the exposition endpoint; the collectors are registry-agnostic |

### Wiring through `metrics-spring-boot`

`SentinelMetricsAutoConfiguration` (package `io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure`)
registers the **new-client** collector: its nested `PrometheusConfiguration` is gated by
`@ConditionalOnEnabledPrometheusMetricsExport`, itself `@ConditionalOnEnabledMetricsExport("prometheus")` plus
`@ConditionalOnBean(type = "io.micrometer.prometheusmetrics.PrometheusMeterRegistry")`. The bean method
`sentinelMultiCollector(PrometheusMeterRegistry registry, @Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration
interval, ConfigurableEnvironment environment)` reduces to five statements:

```java
SentinelMultiCollector sentinelMultiCollector = new SentinelMultiCollector(interval.toMillis());
Map<String, String> commonTags = (Map) getSubProperties(environment, "management.metrics.tags");
commonTags.forEach(sentinelMultiCollector::commonLabel);
PrometheusRegistry prometheusRegistry = registry.getPrometheusRegistry();
prometheusRegistry.register(sentinelMultiCollector);
```

| Property | Default | Role |
|:---|:---|:---|
| `microsphere.metrics.micrometer.alibaba-sentinel.enabled` | `true` (`matchIfMissing = true`) | switches the whole Sentinel metrics path, this collector included |
| `management.metrics.tags.*` | `application`, `profiles` in the shipped `META-INF/config/default/metrics.yaml` | each tag is applied as a `commonLabel` |
| `system.metrics.collection.interval` | `60000`; `metrics.yaml` maps it to `${management.metrics.export.prometheus.pushgateway.push-rate:60000}` | the `Duration` handed to `interval.toMillis()` |

The Micrometer binder bean `SentinelMetrics` is created only
`@ConditionalOnMissingBean(type = "io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector")`, so your own
collector bean also suppresses the Micrometer path; `SentinelCollector` is never auto-configured on this line.

## Code examples

`SentinelMultiCollectorTest#setUp` builds the collector with `new SentinelMultiCollector(60000).commonLabel("application", "test")`
and registers it on a fresh `PrometheusRegistry`. The scrape case from
`src/test/java/io/microsphere/metrics/prometheus/sentinel/SentinelMultiCollectorTest.java`:

```java
@Test
void testCollect() throws Throwable {
    this.testHelper.doInSentinelMetrics(() -> {
        MetricSnapshots metricSnapshots = this.sentinelMultiCollector.collect();
        assertEquals(7, metricSnapshots.size());
    });
}
```

Its companion `testCollectOnSentinelMetricsRepositoryNotReady` scrapes the same collector with no repository installed
and asserts `assertEquals(0, …collect().size())`. The legacy client is asserted through its registry in
`…/sentinel/client/SentinelCollectorTest.java`:

```java
this.registry = new CollectorRegistry();
this.sentinelCollector = new SentinelCollector(60000).commonLabel("test-label", "test-value");
this.sentinelCollector.register(registry);

List<MetricFamilySamples> metricFamilySamplesList = newArrayList(this.registry.metricFamilySamples());
assertEquals(7, metricFamilySamplesList.size());
// per index: name == buildMetricName(metricFamily), type.name() and help == the catalogue values
```

`SentinelMetricsTestHelper` (same package) is the fixture and, outside a test, the worked example of the wiring a
collector needs: it creates a `SentinelMetricsRepository`, installs it with `addEntryCallback(...)`, drives `100` calls
of `new SentinelTemplate().call("test-resource", () -> sleep(10))`, spins with `sleep(100)` until
`findMetricNodes(beginTimeMs, currentTimeMillis())` is non-empty, runs the scrape action, and always calls
`removeEntryCallback(sentinelMetricsRepository.getClass())` in a `finally`. It starts no HTTP server and needs no
external service.

## Metrics reference

Catalogue source: `SentinelMetricUtitls.METRIC_FAMILIES` (`metrics-alibaba-sentinel`); all families are `GAUGE` and
carry the 4 required labels plus your common labels.

| Metric (authored) | Unit | Type | HELP text | Legacy export name |
|:---|:---|:---|:---|:---|
| `sentinel_rt` | `millseconds` | `GAUGE` | The Response Time (RT) of the Sentinel Resource | `sentinel_rt_millseconds` |
| `sentinel_concurrency` | — | `GAUGE` | The Concurrency of the Sentinel Resource | `sentinel_concurrency` |
| `sentinel_success_qps` | — | `GAUGE` | The Success QPS of the Sentinel Resource | `sentinel_success_qps` |
| `sentinel_pass_qps` | — | `GAUGE` | The Pass QPS of the Sentinel Resource | `sentinel_pass_qps` |
| `sentinel_occupied_pass_qps` | — | `GAUGE` | The Occupied Pass QPS of the Sentinel Resource | `sentinel_occupied_pass_qps` |
| `sentinel_block_qps` | — | `GAUGE` | The Block QPS of the Sentinel Resource | `sentinel_block_qps` |
| `sentinel_exception_qps` | — | `GAUGE` | The Block QPS of the Sentinel Resource | `sentinel_exception_qps` |

Unlike the legacy path, `SentinelMultiCollector` never concatenates the unit itself: it names descriptors through
`PrometheusNaming.sanitizeMetricName(name)`, or `sanitizeMetricName(name, unit)` for the single family carrying a unit,
and hands that `Unit` to the descriptor builder. Labels on every data point:

| Label | Value | Emitted by |
|:---|:---|:---|
| `sentinel_resource` | `MetricNode.getResource()` | both |
| `sentinel_context` | the context key of the repository map | both |
| `sentinel_resource_type` | `SentinelUtils.getResourceTypeAsString(classification)` | both |
| `sentinel_version` | `com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION` | both |
| your `commonLabel(...)` pairs | constant | both |
| `sentinel_timestamp` | `MetricNode.getTimestamp()` as a decimal string | `SentinelMultiCollector` only; the legacy collector puts the same value in the `Sample` timestamp |

## Notes and limitations

- Register **one** collector per JVM: both read the same repository and emit the same 7 families in different models.
- `SentinelMultiCollector` freezes its descriptors in the constructor, so common labels added afterwards appear on the
  samples but not in the descriptor's declared `labelNames` — configure labels first, register second.
- An empty scrape is logged, not thrown: `No metrics collected` from either collector, and
  `SentinelMetricsRepository is not ready, please check whether it is configured` from the catalogue utility.
- Two catalogue artefacts reach the exposition: `sentinel_exception_qps` carries the help text
  `"The Block QPS of the Sentinel Resource"` (upstream copy error) and the `sentinel_rt` unit is spelled `millseconds`,
  which the legacy collector also reports twice (name plus `MetricFamilySamples` unit).
- Values are per-`MetricNode` instants (Sentinel writes one node per second per resource), not aggregates over
  `interval`, so a window covering several seconds yields several data points per series.
- The module ships no `AutoConfiguration.imports`, `spring.factories` or service file; wiring is yours or
  `metrics-spring-boot`'s.

## Related documents

- [中文用户文档](./zh/metrics-prometheus.md)
- [Developer Guide — metrics-prometheus](../../developer-guide/current/metrics-prometheus.md)
- [User Guide — 1.x / release-1.x branch](../1.x/metrics-prometheus.md)
- [User Guide index for this branch](./README.md)
