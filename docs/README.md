# Microsphere Observability Documentation Index

[中文版本](./README.zh.md) · This file is the default (English) index

## Layout

- User Guide (`current` for the latest line, `1.x` for the legacy line):
  [metrics-commons](./user-guide/current/metrics-commons.md) ·
  [metrics-micrometer](./user-guide/current/metrics-micrometer.md) ·
  [metrics-prometheus](./user-guide/current/metrics-prometheus.md) ·
  [metrics-alibaba-sentinel](./user-guide/current/metrics-alibaba-sentinel.md) — Chinese under `user-guide/*/zh/`
- Developer Guide (`current` / `1.x`):
  [metrics-commons](./developer-guide/current/metrics-commons.md) ·
  [metrics-alibaba-sentinel](./developer-guide/current/metrics-alibaba-sentinel.md) — Chinese under `developer-guide/*/zh/`
- [Tag diff analysis](./diff/source/README.md)

## Tag diff analysis (`diff/source`)

Method: Maven modules are matched one-by-one by name, `src/main` and `src/test` are compared independently, and
Java 17 vs Java 8 syntax/API differences plus Java EE vs Jakarta EE API differences are excluded.

| Tag pair | Version lines | main diff | test diff |
|---|---|---|---|
| `0.1.3` vs `0.2.3` | Java 8 / Spring Boot 2 generation vs Java 17 / Spring Boot 3–4 generation | [EN](./diff/source/main-diff-0.1.3-vs-0.2.3.md) / [中文](./diff/source/zh/main-diff-0.1.3-vs-0.2.3.zh.md) | [EN](./diff/source/test-diff-0.1.3-vs-0.2.3.md) / [中文](./diff/source/zh/test-diff-0.1.3-vs-0.2.3.zh.md) |
