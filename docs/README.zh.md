# Microsphere Observability 文档索引

[English version](./README.md) · 本文件为中文索引

## 目录结构

- 用户文档（`current` 对应最新线，`1.x` 对应旧版线）：
  [metrics-commons](./user-guide/current/zh/metrics-commons.md) ·
  [metrics-micrometer](./user-guide/current/zh/metrics-micrometer.md) ·
  [metrics-prometheus](./user-guide/current/zh/metrics-prometheus.md) ·
  [metrics-alibaba-sentinel](./user-guide/current/zh/metrics-alibaba-sentinel.md) —— 英文位于 `user-guide/*/`
- 开发者文档（`current` / `1.x`）：
  [metrics-commons](./developer-guide/current/zh/metrics-commons.md) ·
  [metrics-alibaba-sentinel](./developer-guide/current/zh/metrics-alibaba-sentinel.md) —— 英文位于 `developer-guide/*/`
- [Tag 差异分析](./diff/source/README.md)

## Tag 差异分析（`diff/source`）

对比方式：逐个 Maven 模块按名称匹配，`src/main` 与 `src/test` 独立比较，并排除 Java 17 与 Java 8 的语法及 API
差异、Java EE 与 Jakarta EE API 差异。

| Tag 对 | 版本线 | main 差异 | test 差异 |
|---|---|---|---|
| `0.1.3` vs `0.2.3` | Java 8 / Spring Boot 2 世代 与 Java 17 / Spring Boot 3–4 世代 | [英文](./diff/source/main-diff-0.1.3-vs-0.2.3.md) / [中文](./diff/source/zh/main-diff-0.1.3-vs-0.2.3.zh.md) | [英文](./diff/source/test-diff-0.1.3-vs-0.2.3.md) / [中文](./diff/source/zh/test-diff-0.1.3-vs-0.2.3.zh.md) |
