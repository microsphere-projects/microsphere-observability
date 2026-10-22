# Source Diff: 0.1.3 vs 0.2.3 (`src/main`)

> Scope: `src/main` only (Java sources + `META-INF` resources). `src/test` is covered in [test-diff-0.1.3-vs-0.2.3.md](test-diff-0.1.3-vs-0.2.3.md).
> Excluded from the comparison: Java 8 vs Java 17 syntax/API differences, and Java EE vs Jakarta EE API differences. Neither category produced any change in this pair of tags.

## 1. Version context

| | tag `0.1.3` | tag `0.2.3` |
|---|---|---|
| Maven `revision` | `0.1.3-SNAPSHOT` | `0.2.3-SNAPSHOT` |
| `java.version` (root pom) | not declared (inherited from `microsphere-spring-cloud-parent` **0.1.26**) | `17` |
| Microsphere Spring Cloud parent | `0.1.26` | `0.2.26` |
| microsphere-alibaba-sentinel | `0.1.5` | `0.2.5` |
| microsphere-i18n | `0.1.21` | `0.2.21` |

The two tags are the latest release of two parallel maintenance lines of the same repository (`0.1.x` = Java 8 / Spring Boot 2 generation, `0.2.x` = Java 17 / Spring Boot 3–4 generation). Both module lists are identical, so every Maven module matches by name.

## 2. Module matrix

| Maven module | `src/main` files 0.1.3 | `src/main` files 0.2.3 | Changed files | Verdict |
|---|---|---|---|---|
| microsphere-observability-parent | n/a (pom only) | n/a | 0 | identical |
| microsphere-observability-dependencies | n/a (pom only) | n/a | 0 | identical |
| microsphere-observability-logging | 6 | 6 | 0 | identical |
| microsphere-observability-metrics-commons | 2 | 2 | 0 | identical |
| microsphere-observability-metrics-alibaba-sentinel | 4 | 4 | 0 | identical |
| microsphere-observability-metrics-micrometer | 32 | 32 | 0 | identical |
| microsphere-observability-metrics-prometheus | 5 | 5 | 0 | identical |
| microsphere-observability-logging-spring-boot | 20 | 20 | 3 | **differs** |
| microsphere-observability-metrics-spring-boot | 29 | 28 | 9 | **differs** |
| microsphere-observability-spring-boot | 0 | 0 | 0 | identical (aggregator, no sources) |
| microsphere-observability-metrics-spring-cloud | 5 | 4 | 3 | **differs** |
| microsphere-observability-spring-cloud | 0 | 0 | 0 | identical (aggregator, no sources) |

Only the three Spring Boot / Spring Cloud auto-configuration modules differ. All three follow the same strategy in `0.2.3`: **keep the legacy Spring Boot 2 coordinates and add the relocated Spring Boot 4.0 coordinates side by side**, so a single artifact serves `[2.0, 4.0)` and `[4.0, ∞)`.

## 3. microsphere-observability-logging-spring-boot

| File | Change in 0.2.3 | Reason category |
|---|---|---|
| `logging/log4j2/spring/boot/autoconfigure/Log4j2AutoConfiguration.java` | `.setSendEventTimestamp(properties.isSendEventTimestamp())` un-commented out of the Log4j2 Kafka Appender builder chain | Feature need — the upstream log4j-ext version pinned by the 0.2.x line exposes `setSendEventTimestamp`; the 0.1.x line keeps it disabled because that builder method is absent there |
| `logging/spring/boot/autoconfigure/WebMvcLoggingAutoConfiguration.java` | `@AutoConfigureAfter(name=…)` gains `org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration` (+ matching `@see`) | API compatibility — Spring Boot 4.0 relocated `WebMvcAutoConfiguration` out of `spring-boot-autoconfigure` into the new `spring-boot-webmvc` module; the 2.x/3.x FQN is kept for backward resolution |
| `resources/META-INF/spring.factories` | The `EnableAutoConfiguration=…` block (4 auto-configurations) removed; the `ApplicationListener=…` block retained | API compatibility — `META-INF/spring/…AutoConfiguration.imports` already existed and is byte-identical in both tags, so 0.2.3 drops the legacy Boot 2 registration path; listener registration has no `imports` equivalent and therefore stays |

## 4. microsphere-observability-metrics-spring-boot

| File | Change in 0.2.3 | Reason category |
|---|---|---|
| `…/autoconfigure/CGGroupMetricsAutoConfiguration.java` | `@AutoConfigureAfter` + `@see` add `org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration` and `…CompositeMeterRegistryAutoConfiguration`, with `[2.0, 4.0)` / `[4.0, )` comments | API compatibility — Boot 4.0 per-starter auto-configuration packages |
| `…/autoconfigure/JvmMetricsAutoConfiguration.java` | Same pattern, plus `…micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration` | API compatibility |
| `…/autoconfigure/KafkaMetricsAutoConfiguration.java` | Same pattern, plus `org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration` | API compatibility |
| `…/autoconfigure/SystemMetricsAutoConfiguration.java` | Same pattern, plus `…micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration` | API compatibility |
| `…/autoconfigure/SentinelMetricsAutoConfiguration.java` | Micrometer Prometheus registry package `io.micrometer.prometheus` → `io.micrometer.prometheusmetrics`; Prometheus client `io.prometheus.client.CollectorRegistry` → `io.prometheus.metrics.model.registry.PrometheusRegistry`; bean `SentinelCollector` → `SentinelMultiCollector`; `@ConditionalOnClass("io.prometheus.client.Collector")` removed; common tags now read via `getSubProperties(environment, "management.metrics.tags")` instead of injected `MetricsProperties`; `@ConditionalOnMissingBean(type=…)` updated | API compatibility (micrometer-registry-prometheus 1.13+ package move and prometheus-metrics 1.x client) + implementation need: `MultiCollector` is the client-1.x replacement for the deprecated `Collector` SPI, and `MetricsProperties` injection was replaced by direct environment reads because the property binding moved in the newer Boot generation |
| `…/condition/ConditionalOnEnabledPrometheusMetricsExport.java` | `@ConditionalOnBean(type=…)` now points at `io.micrometer.prometheusmetrics.PrometheusMeterRegistry`; unused `ConditionalOnClass` import added to the header | API compatibility — the conditional must match the registry class name actually exported by the new stack, otherwise the Sentinel Prometheus beans never activate |
| `resources/META-INF/spring.factories` | File deleted (5 auto-configurations) | API compatibility — identical content already registered in `AutoConfiguration.imports` |

## 5. microsphere-observability-metrics-spring-cloud

| File | Change in 0.2.3 | Reason category |
|---|---|---|
| `…/actuate/autoconfigure/ServiceRegistrationMetricsAutoConfiguration.java` | Import `SentinelCollector` → `SentinelMultiCollector`; `@AutoConfigureAfter`/`@see` gain the three `org.springframework.boot.micrometer.metrics.autoconfigure.*` coordinates; `@ConditionalOnClass` switches from `io.prometheus.client.Collector` to `io.prometheus.metrics.model.registry.MultiCollector`; `@ConditionalOnBean(type="…SentinelCollector")` becomes `@ConditionalOnBean(SentinelMultiCollector.class)`; bean/method renamed to `sentinelMultiCollectorBeanPostProcessor` | API compatibility — the post-processor must attach the `instance` label to the collector type actually published by the 0.2.x metrics-spring-boot module; type-safe `@ConditionalOnBean` replaces the string form once the class is on the compile classpath |
| `resources/META-INF/spring.factories` | File deleted (1 auto-configuration) | API compatibility — already present in `AutoConfiguration.imports` |

## 6. Cross-cutting conclusions

1. **Auto-configuration registration**: 0.1.3 ships both `spring.factories` and `AutoConfiguration.imports`; 0.2.3 ships `AutoConfiguration.imports` only. The list of registered classes is unchanged in every module — this is purely a Spring Boot 2 legacy-path removal, no functional difference.
2. **Spring Boot 4.0 package relocation is handled additively**, not by migration: old and new FQNs coexist inside `@AutoConfigureAfter(name=…)` string lists, which is the only mechanism that survives a missing class silently.
3. **Prometheus client migration is the one genuine behavioral change**: `SentinelCollector` (client `0.x` `Collector` SPI, still present and identical in both tags) is no longer wired in 0.2.3; `SentinelMultiCollector` (client 1.x `MultiCollector`) is used instead. `microsphere-observability-metrics-prometheus` itself is untouched, because it already carried both collectors in both tags.
4. **Metric tag plumbing** moved from `MetricsProperties.getTags()` to `ConfigurableEnvironment` sub-property lookup, decoupling the code from the Boot property class that relocated across generations.
5. No Java 8/17 syntax-only or Java EE/Jakarta EE-only diffs exist in `src/main` for this tag pair.
