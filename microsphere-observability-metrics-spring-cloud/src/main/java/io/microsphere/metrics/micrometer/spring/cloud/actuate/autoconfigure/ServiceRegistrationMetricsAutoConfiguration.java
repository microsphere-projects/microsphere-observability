/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.metrics.micrometer.spring.cloud.actuate.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnEnabledPrometheusMetricsExport;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import io.microsphere.metrics.prometheus.sentinel.SentinelMultiCollector;
import io.microsphere.spring.beans.factory.config.GenericBeanPostProcessorAdapter;
import io.microsphere.spring.cloud.client.service.registry.condition.ConditionalOnAutoServiceRegistrationAvailable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

;

/**
 * Micrometer Auto-Configuration for Spring Cloud Service Registration metrics.
 * <p>
 * This configuration enables metrics collection for service registration and discovery,
 * including integration with Alibaba Sentinel metrics. It automatically registers instance
 * information as common tags for all collected metrics.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 *   <li>Automatically registers instance hostname:port as common tag "instance"</li>
 *   <li>Integrates SentinelMultiCollector with instance labels when using Prometheus</li>
 *   <li>Conditional on Spring Cloud Discovery and Service Registration being enabled</li>
 * </ul>
 * <h3>Example Configuration</h3>
 * <pre>{@code
 * // Enable/disable metrics export
 * microsphere.metrics.micrometer.prometheus.export.enabled=true
 *
 * // The instance tag will be automatically set to host:port
 * }</pre>
 * <h3>Example Metrics with Instance Tag</h3>
 * <pre>{@code
 * sentinel_rt{service="user-service",instance="192.168.1.100:8080",resource="login"} 45.2
 * http_requests_total{instance="192.168.1.100:8080",method="GET",path="/api/users"} 1250
 * }</pre>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterRegistry
 * @see org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
 * @see org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration
 * @see io.microsphere.spring.cloud.client.service.registry.autoconfigure.SimpleAutoServiceRegistrationAutoConfiguration
 * @see io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SentinelMetricsAutoConfiguration
 * @since 1.0.0
 */
@ConditionalOnDiscoveryEnabled
@ConditionalOnMicrometerAvailable
@ConditionalOnAutoServiceRegistrationAvailable
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration",

        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration",

        "io.microsphere.spring.cloud.client.service.registry.autoconfigure.SimpleAutoServiceRegistrationAutoConfiguration",
        "io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SentinelMetricsAutoConfiguration"
})
@Import(ServiceRegistrationMetricsAutoConfiguration.PrometheusConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class ServiceRegistrationMetricsAutoConfiguration {

    /**
     * The constant key for instance tag used in metric common tags.
     * <p>
     * This tag value is the host:port combination of the service instance.
     * All metrics will include this tag for proper identification in distributed systems.
     * </p>
     * <h3>Example Usage</h3>
     * <pre>{@code
     * String tagKey = ServiceRegistrationMetricsAutoConfiguration.INSTANCE_TAG_KEY;
     * // Result: "instance"
     *
     * // Metrics will include:
     * // http_requests_total{instance="localhost:8080", ...}
     * }</pre>
     */
    public static final String INSTANCE_TAG_KEY = "instance";

    private static String instance;

    /**
     * Constructs a new {@code ServiceRegistrationMetricsAutoConfiguration} instance.
     * <p>
     * This constructor retrieves the service registration information from Spring Cloud
     * and extracts the host:port combination to use as a common tag for all metrics.
     * </p>
     * <h3>Example Behavior</h3>
     * <pre>{@code
     * // When Spring Cloud service registration is available:
     * // Registration.getHost() -> "192.168.1.100"
     * // server.port -> 8080
     * // instance = "192.168.1.100:8080"
     *
     * // When not available:
     * // instance remains null
     * }</pre>
     *
     * @param registrationProvider provider for retrieving service registration info
     * @param port                 the server port (from ${server.port:-1})
     */
    public ServiceRegistrationMetricsAutoConfiguration(ObjectProvider<Registration> registrationProvider, @Value("${server.port:-1}") int port) {
        registrationProvider.ifAvailable(registration -> {
            String host = registration.getHost();
            instance = host + ":" + port;
        });
    }

    /**
     * Creates a customizer that adds instance information as a common tag to all meters.
     * <p>
     * This customizer ensures every metric registered in the meter registry includes the
     * instance identifier (host:port) for easy filtering and identification in monitoring dashboards.
     * </p>
     * <h3>Example Customization</h3>
     * <pre>{@code
     * MeterRegistryCustomizer customizer = commonMeterRegistryCustomizer();
     * customizer.customize(registry);
     *
     * // Result: All meters will have tag: instance=localhost:8080
     * Gauge gauge = Gauge.builder("memory.used")
     *     .register(registry);
     * // Effectively registered as: memory_used{instance="localhost:8080"}
     * }</pre>
     *
     * @return MeterRegistryCustomizer that adds instance tag to all metrics
     */
    @Bean
    public MeterRegistryCustomizer commonMeterRegistryCustomizer() {
        return registry -> registry.config().commonTags(INSTANCE_TAG_KEY, instance);
    }

    /**
     * Internal configuration class for Prometheus-specific setup.
     * <p>
     * Activated only when Prometheus metrics export is enabled. Adds Sentinel collector
     * bean post-processing to inject instance labels.
     * </p>
     */
    @ConditionalOnEnabledPrometheusMetricsExport
    static class PrometheusConfiguration {

        /**
         * Creates a BeanPostProcessor for SentinelMultiCollector instances.
         * <p>
         * This post-processor intercepts SentinelMultiCollector bean initialization
         * and injects the instance label before the bean is fully initialized.
         * </p>
         * <h3>Example Post-Processing</h3>
         * <pre>{@code
         * SentinelMultiCollector collector = new SentinelMultiCollector(60000);
         * // Before post-processing:
         * // - No instance label
         *
         * // After post-processing via processBeforeInitialization():
         * // - Collector has commonLabel("instance", "192.168.1.100:8080")
         *
         * // Collected metrics will include:
         * // sentinel_rt{instance="192.168.1.100:8080", resource="login"} 45.2
         * }</pre>
         *
         * @return BeanPostProcessor that modifies SentinelMultiCollector beans
         * @see BeanPostProcessor#postProcessBeforeInitialization(Object, String)
         * @see GenericBeanPostProcessorAdapter
         */
        @ConditionalOnClass(name = {
                "io.prometheus.metrics.model.registry.MultiCollector"
        })
        @ConditionalOnBean(SentinelMultiCollector.class)
        @Bean
        public BeanPostProcessor sentinelMultiCollectorBeanPostProcessor() {
            return new GenericBeanPostProcessorAdapter<SentinelMultiCollector>() {
                @Override
                protected void processBeforeInitialization(SentinelMultiCollector bean, String beanName) {
                    bean.commonLabel(INSTANCE_TAG_KEY, instance);
                }
            };
        }
    }
}
