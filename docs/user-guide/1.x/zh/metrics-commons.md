# metrics-commons 用户文档（1.x 版本线）

[English version](../metrics-commons.md) · [用户文档索引](./README.md)

## 概述

`microsphere-observability-metrics-commons` 是 `release-1.x` 分支上 Microsphere Observability 指标族的“词汇表”模块：
两个与后端无关的类型 —— `MetricFamily`（名称、单位、类型、help 文本、标签名）和 `MetricType`（Prometheus 指标类型）
—— 供 `metrics-alibaba-sentinel`、`metrics-micrometer`、`metrics-prometheus` 共用。

本模块的 Java 源码与 `release`（0.2.x）分支**逐字节一致**，两分支之间的 `git diff` 可以证实
（`microsphere-observability-metrics-commons/` 下无任何变更文件）。因此下文所有差异都属于版本装配，而非 API。

## 版本与兼容性

| 属性 | 值 |
|:---|:---|
| 分析分支 | `release-1.x` |
| Maven 坐标 | `io.github.microsphere-projects:microsphere-observability-metrics-commons` |
| 版本线 | `0.1.x`（reactor `${revision}` = `0.1.4-SNAPSHOT`，最新已发布标签 `0.1.3`） |
| Java 版本 | 1.x reactor pom 未声明，继承自 `microsphere-spring-cloud-parent:0.1.26` |
| Javadoc | [io.github.microsphere-projects/microsphere-observability-metrics-commons](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-commons) |
| 两个类的 `@since` | `1.0.0` |
| 与 `current` 文档的 API 差异 | 无 |

Spring 兼容性来自父工程的构建 profile（默认项加粗）。1.x 版本线止步于 Spring Boot 2.x：

| Profile | Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|:---|:---|:---|:---|
| `spring-cloud-hoxton` | 2.3.12.RELEASE | Hoxton.SR12 | 2.2.10 |
| `spring-cloud-2020` | 2.4.13 | 2020.0.5 | 2021.1 |
| **`spring-cloud-2021`（默认）** | **2.6.15** | **2021.0.9** | **2021.0.6.1** |

在本分支上，由这些描述符渲染出的 Prometheus 客户端是 `io.prometheus:prometheus-metrics-bom` 的 **`1.8.0`** 版本
（`release` 分支为 `1.9.0`）；1.x 版本线上更值得注意的是 Prometheus 客户端的新旧并存问题，见
[metrics-prometheus](./metrics-prometheus.md)。

## 安装

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-observability-dependencies</artifactId>
            <version>${microsphere-observability.version}</version> <!-- 例如 0.1.3 -->
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

不要把 `0.1.x` 的 BOM 与解析自 `0.2.x` 的模块混用：描述符本身兼容，但渲染模块面向不同的 Prometheus 客户端与
Spring Boot 世代。

## 快速上手

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

## API 说明

与 `current` 版本线完全一致；这里完整重述，以便 1.x 文档可以独立阅读。

### `io.microsphere.metrics.commons.MetricType`

`public enum MetricType`，包含 `COUNTER`、`GAUGE`、`HISTOGRAM`、`SUMMARY`、`INFO`、`STATESET`、`UNKNOWN`。

| 枚举常量 | 含义 |
|:---|:---|
| `COUNTER` | 单调递增的值 |
| `GAUGE` | 可增可减的值 |
| `HISTOGRAM` | 分桶的观测值 |
| `SUMMARY` | 观测值的分位数摘要 |
| `INFO` | 常量型信息指标 |
| `STATESET` | 一组互斥状态 |
| `UNKNOWN` | 类型未知 |

`metrics-prometheus` 按名称桥接（`io.prometheus.metrics.model.registry.MetricType.valueOf(type.name())`），因此在客户端
枚举中没有同名常量的类型无法被渲染。1.x 版本线上还有一部分 Prometheus 逻辑仍运行在旧版 `io.prometheus:simpleclient`
API 之上（见 [metrics-prometheus](./metrics-prometheus.md) 与
[metrics-spring-cloud](./metrics-spring-cloud.md)）。

### `io.microsphere.metrics.commons.MetricFamily`

| 成员 | 签名 | 说明 |
|:---|:---|:---|
| `builder()` | `public static Builder builder()` | 唯一的公开入口 |
| `MetricFamily(...)` | 包级私有 `MetricFamily(String, String, MetricType, String, Set<String>)` | 包外不可调用 |
| `getName()` | `public String getName()` | 原始名称，未规范化 |
| `getUnit()` | `public String getUnit()` | 基本单位，可能为 `null` |
| `getType()` | `public MetricType getType()` | 指标类型 |
| `getHelp()` | `public String getHelp()` | HELP 文本 |
| `getLabelNames()` | `public Set<String> getLabelNames()` | 构造时提供的标签名 |

`MetricFamily.Builder`：`name(String)`、`unit(String)`、`type(MetricType)`、`help(String)`、
`labelNames(Set<String>)`、`build()`。所有设置方法均可链式调用且不做校验；`build()` 永不返回 `null`，未设置的字段保持
`null`；`labelNames` 按引用保存。

## 代码示例

参考用法来自 `metrics-alibaba-sentinel` 中的 `SentinelMetricUtitls`（该模块源码在本分支与 `release` 一致）：

```java
public static final List<MetricFamily> METRIC_FAMILIES = ofList(
        builder().name(RT_METRIC_NAME).unit("millseconds").type(GAUGE)
                .help("The Response Time (RT) of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build(),
        builder().name(BLOCK_QPS_METRIC_NAME).type(GAUGE)
                .help("The Block QPS of the Sentinel Resource")
                .labelNames(REQUIRED_LABEL_NAMES).build());
```

其测试锁定了指标族目录与“单位后缀”命名规则：

```java
assertEquals(7, METRIC_FAMILIES_SIZE);
assertEquals(RT_METRIC_NAME + "_millseconds", buildMetricName(getMetricFamily(0)));
assertEquals(CONCURRENCY_METRIC_NAME, buildMetricName(getMetricFamily(1)));
```

## 注意事项与限制

- 未实现 `equals`/`hashCode`/`toString`：只有标识语义。
- `labelNames(Set)` 不做防御式拷贝，请传入不可变 Set。
- 本层不校验名称——Prometheus 规范化发生在渲染时。
- 可选依赖（`microsphere-java-core`、`microsphere-annotation-processor`）不会给运行时 classpath 增加内容。

## 与 `current` 版本线的差异

| 项目 | `0.2.x`（`release`） | `0.1.x`（`release-1.x`） |
|:---|:---|:---|
| 本模块 API | — | 完全一致 |
| reactor `${revision}` | `0.2.4-SNAPSHOT` | `0.1.4-SNAPSHOT` |
| 最新已发布版本 | `0.2.3` | `0.1.3` |
| reactor pom 中的 `java.version` | `17` | 未声明 |
| Prometheus metrics BOM | `1.9.0` | `1.8.0` |
| 默认 Spring Boot / Spring Cloud | 3.5.15 / 2025.0.3 | 2.6.15 / 2021.0.9 |

## 相关文档

- [English User Guide](../metrics-commons.md)
- [开发者文档 —— metrics-commons（1.x）](../../developer-guide/1.x/zh/metrics-commons.md)
- [用户文档 —— current / release 分支](../current/zh/metrics-commons.md)
- [本分支用户文档索引](./README.md)
