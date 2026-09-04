# Release Notes

## v0.1.0

_Release notes generation failed. Raw commits since the beginning:_

```
96b0330 Integrate Docker Compose setup in Maven publish workflow
1991cf9 Merge pull request #6 from mercyblitz/dev-1.x
7cf726e Fix CGroup memory test path setup
3fb54c1 Improve CGroupMemoryMetrics test coverage
2775dd1 Add MBeanMetrics binder test
0e90961 Refactor system metrics file parsing
5d80dfd Simplify Kafka producer extraction logic
d48ca23 Add DynamicLayout test across appenders
1e86f4c Add failure-path test for logging auto-config
83ee929 Mark logging auto-config classes as @Configuration
12bb238 Clean up metrics auto-config docs
cce5e27 Remove Boot 4 WebMvc auto-config reference
9096000 Add logger utilities for observability
0537e8b Merge branch 'release-1.x' into dev-1.x
240711b Fix AbstractMeterBinder bindTo test path
fd46054 Stabilize Sentinel metrics test
7aab32e Refactor Sentinel metric label/value utilities
5e8d817 Remove unused ClassOrderer import
1ff758f Add test for AbstractMeterBinder bindTo path
5d3c361 Bump Sentinel dependency to 0.1.4
c1245b3 Remove wiki publishing and branch merge workflows
d29c7f8 Adjust JVM metrics integration test count
9bb83dd Wire Sentinel metric callbacks on startup
882a903 Update Codecov badge to dev-1.x branch
dc0141c Require KafkaAppender bean for Kafka metrics
563349b Scope Kafka appender bean condition to metrics bean
d20acd8 Gate Kafka metrics auto-config on appender bean
e93d2e7 Fix PrometheusMeterRegistry test import
5cbdfc5 Fix metrics collection config path
5c2b8ab Align metrics auto-config with legacy APIs
1bfaa0e Align 1.x branch CI and dependency versions
61d33f3 chore: merge main into dev [skip ci]
170b6f7 Remove log4j2 test dependency
d0c5693 Add bindTo test for AbstractMeterBinder
e8336ce chore: merge main into dev [skip ci]
8302f20 Register more executor JVM metrics
5b5d8ae Cover bean-based Log4j2 filter and layout lookup
9b52ccb Broaden test Log4j logger package
cc66a42 chore: merge main into dev [skip ci]
2f57362 Fix Kafka appender layout fallback
7e3d94b Add common label in Sentinel collector test
5548974 Annotate Micrometer executor size property
f483f0b chore: merge main into dev [skip ci]
2b34658 Harden MicrometerUtils and add tests
00a1911 chore: merge main into dev [skip ci]
6ffd1ca Add Prometheus case to Sentinel metrics test
6199a27 chore: merge main into dev [skip ci]
78528a8 Use fluent common labels for Sentinel collectors
d6d1963 chore: merge main into dev [skip ci]
0e08211 Use shared Sentinel metric prefix
```

**Full Changelog**: https://github.com/microsphere-projects/microsphere-observability/compare/...0.1.0## v0.1.1

_Release notes generation failed. Raw commits since 0.1.0:_

```
4b0e6e8 Update version numbers in README.md
ed9da46 Merge pull request #11 from mercyblitz/dev-1.x
e22cc36 Fix Sentinel metrics bean backoff condition
e8c2366 Update README with module and BOM details
7fca780 chore: merge release-1.x into dev-1.x [skip ci]
45bc726 chore: bump version to next patch after publishing 0.1.0
```

**Full Changelog**: https://github.com/microsphere-projects/microsphere-observability/compare/0.1.0...0.1.1## v0.1.2

_Release notes generation failed. Raw commits since 0.1.1:_

```
5ca304d Update README.md
84655c8 Merge pull request #14 from mercyblitz/dev-1.x
1b8a3a5 Use server.port for instance metrics tag
4b53f75 Use application as default metrics tag
cc3908f Remove app label from Sentinel collector
7e57b99 Remove unused Sentinel metric imports
837217f Add spring-cloud module and refresh wrapper
719aabe Add Spring Cloud metrics auto-config
e7392d4 Fix LoggerUtils import in Kafka metrics test
5cb5612 Guard Sentinel metrics auto-config by classpath
22e1baa Fix imports in observability auto-config tests
7fac4df Make logging starter optional
adfe94c Remove LoggerUtils unit test
2832346 Use shared LoggerUtils, drop local duplicate
e0d1f5c Restructure metrics config for Boot compatibility
aa6a716 Merge branch 'dev-1.x' of https://github.com/mercyblitz/microsphere-observability into dev-1.x
3cadba3 Bump microsphere-i18n to 0.1.21
ea48386 Merge pull request #13 from mercyblitz/dev-1.x
eaf1dab Relax Kafka metrics classpath condition
9b9ef88 Bump microsphere-spring-cloud to 0.1.26
fa2b965 chore: merge release-1.x into dev-1.x [skip ci]
cb56703 chore: bump version to next patch after publishing 0.1.1
```

**Full Changelog**: https://github.com/microsphere-projects/microsphere-observability/compare/0.1.1...0.1.2## v0.1.3

_Release notes generation failed. Raw commits since 0.1.2:_

```
fc8f703 Update version numbers in README.md
bf64565 Merge pull request #17 from mercyblitz/dev-1.x
20086f4 Avoid duplicate SentinelMetrics beans
e7c75be Fix Sentinel Prometheus metrics conditions
1e6f47c Merge pull request #15 from mercyblitz/dev-1.x
6216101 Bump microsphere-alibaba-sentinel to 0.1.5
3aca962 Add metrics-commons to metrics modules
d291d46 Gate service metrics on Prometheus export
ebabcc9 Use Prometheus registry bean condition
e3acc82 Add Micrometer binder for Prometheus collector
9a645c9 Make Sentinel metrics dependency optional
7c66a60 Align Sentinel bean post-processor type
133f02e Fix Prometheus service registration metrics
71e2d42 Update Docker Compose action in workflows
242de1d Add instance label support for Prometheus
46f30e0 Apply common metric tags to Sentinel collector
483736b chore: merge release-1.x into dev-1.x [skip ci]
850fb20 chore: bump version to next patch after publishing 0.1.2
```

**Full Changelog**: https://github.com/microsphere-projects/microsphere-observability/compare/0.1.2...0.1.3