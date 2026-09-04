# Microsphere Observability

> Microsphere Projects for Observability

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/microsphere-projects/microsphere-observability)
[![Maven Build](https://github.com/microsphere-projects/microsphere-observability/actions/workflows/maven-build.yml/badge.svg)](https://github.com/microsphere-projects/microsphere-observability/actions/workflows/maven-build.yml)
[![Codecov](https://codecov.io/gh/microsphere-projects/microsphere-observability/branch/dev-1.x/graph/badge.svg)](https://app.codecov.io/gh/microsphere-projects/microsphere-observability)
![Maven](https://img.shields.io/maven-central/v/io.github.microsphere-projects/microsphere-observability.svg)
![License](https://img.shields.io/github/license/microsphere-projects/microsphere-observability.svg)

Microsphere Observability is a comprehensive suite of tools designed to enhance the monitoring, metrics collection, and
logging capabilities of Java applications, with a primary focus on the Spring Boot ecosystem. It provides specialized
integrations for Micrometer, Prometheus, Log4j2, and Alibaba Sentinel, alongside custom metrics for system-level
resources like CGroups and network statistics.

## Modules

### Root and Parent POMs

- Root POM [microsphere-observability](./pom.xml): Acts as the project aggregator.
- Parent POM [microsphere-observability-parent](./microsphere-observability-parent/pom.xml): Manages external dependency
  versions. It imports critical BOMs such as OpenTelemetry Instrumentation

### Bill of Materials (BOM)

- Module: [microsphere-observability-dependencies](./microsphere-observability-dependencies/pom.xml)
- Purpose: Provides a single point of import for consumers. It defines the dependencyManagement section for all internal
  modules (logging, micrometer, prometheus, etc.), ensuring that users do not need to specify versions for individual
  microsphere artifacts

### Functional Sub-Modules

| Module Name                                            | Responsibility                                                                                  |
|:-------------------------------------------------------|:------------------------------------------------------------------------------------------------|
| **microsphere-observability-logging**                  | Core logging abstractions and dynamic layouts. Includes `LoggerUtils` for lambda-based logging. |
| **microsphere-observability-metrics-commons**          | Foundational metrics types and definitions.                                                     |
| **microsphere-observability-metrics-alibaba-sentinel** | Sentinel-specific metrics logic and constants.                                                  |
| **microsphere-observability-metrics-micrometer**       | Custom `MeterBinder` implementations including Sentinel, JDBC (P6Spy), and System metrics.      |
| **microsphere-observability-metrics-prometheus**       | Bridges Sentinel and other metrics to Prometheus format via custom Collectors.                  |

## Getting Started

The easiest way to get started is by adding the Microsphere Observability BOM (Bill of Materials) to your project's
pom.xml:

```xml

<dependencyManagement>
    <dependencies>
        ...
        <!-- Microsphere Observability Dependencies -->
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-observability-dependencies</artifactId>
            <version>${microsphere-observability.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        ...
    </dependencies>
</dependencyManagement>
```

`${microsphere-observability.version}` has two branches:

| Branches | Purpose                                          | Latest Version |
|----------|--------------------------------------------------|----------------|
| main     | Compatible with Spring Cloud 2022.0.x - 2025.0.x | `0.2.3`        |
| 1.x      | Compatible with Spring Cloud Hoxton - 2021.0.x   | `0.1.3`        |

## Building from Source

You don't need to build from source unless you want to try out the latest code or contribute to the project.

To build the project, follow these steps:

1. Clone the repository:

```bash
git clone https://github.com/microsphere-projects/microsphere-observability.git
```

2. Build the source:

- Linux/MacOS:

```bash
./mvnw package
```

- Windows:

```powershell
mvnw.cmd package
```

## Contributing

We welcome your contributions! Please read [Code of Conduct](./CODE_OF_CONDUCT.md) before submitting a pull request.

## Reporting Issues

* Before you log a bug, please search
  the [issues](https://github.com/microsphere-projects/microsphere-observability/issues)
  to see if someone has already reported the problem.
* If the issue doesn't already
  exist, [create a new issue](https://github.com/microsphere-projects/microsphere-observability/issues/new).
* Please provide as much information as possible with the issue report.

## Documentation

### User Guide

[DeepWiki Host](https://deepwiki.com/microsphere-projects/microsphere-observability)

### Wiki

[Github Host](https://github.com/microsphere-projects/microsphere-observability/wiki)

### JavaDoc

- [microsphere-observability-logging](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-logging)
- [microsphere-observability-metrics-commons](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-commons)
- [microsphere-observability-metrics-alibaba-sentinel](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-alibaba-sentinel)
- [microsphere-observability-metrics-micrometer](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-micrometer)
- [microsphere-observability-metrics-prometheus](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-prometheus)
- [microsphere-observability-logging-spring-boot](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-logging-spring-boot)
- [microsphere-observability-metrics-spring-boot](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-metrics-spring-boot)
- [microsphere-observability-spring-boot](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-observability-spring-boot)

## License

The Microsphere Spring is released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
