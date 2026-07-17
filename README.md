# Microsphere Observability

> Microsphere Projects for Observability

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/microsphere-projects/microsphere-observability)
[![Maven Build](https://github.com/microsphere-projects/microsphere-observability/actions/workflows/maven-build.yml/badge.svg)](https://github.com/microsphere-projects/microsphere-observability/actions/workflows/maven-build.yml)
[![Codecov](https://codecov.io/gh/microsphere-projects/microsphere-observability/branch/main/graph/badge.svg)](https://app.codecov.io/gh/microsphere-projects/microsphere-observability)
![Maven](https://img.shields.io/maven-central/v/io.github.microsphere-projects/microsphere-observability.svg)
![License](https://img.shields.io/github/license/microsphere-projects/microsphere-observability.svg)

Microsphere Observability

## Modules

TODO

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

| **Branches** | **Purpose**                                      | **Latest Version** |
|--------------|--------------------------------------------------|--------------------|
| **main**     | Compatible with Spring Cloud 2022.0.x - 2025.0.x | `0.2.0`            |
| **1.x**      | Compatible with Spring Cloud Hoxton - 2021.0.x   | `0.1.0`            |

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

TODO 

## License

The Microsphere Spring is released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).