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

package io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure;

import io.microsphere.alibaba.sentinel.spring.boot.condition.ConditionalOnSentinelAvailable;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.metrics.micrometer.instrument.binder.sentinel.SentinelMetrics;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerAvailable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SentinelMetricsAutoConfiguration.SENTINEL_METRICS_ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.METRICS_COLLECTION_INTERVAL_PLACEHOLDER;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;

/**
 * The Auto-Configuration class for Alibaba Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnSentinelAvailable
@ConditionalOnMicrometerAvailable
@ConditionalOnProperty(name = SENTINEL_METRICS_ENABLED_PROPERTY_NAME, matchIfMissing = true)
@AutoConfigureAfter(name = {
        // Spring Boot Actuator API [2.0, 4.0)
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        // Spring Boot Actuator API [4.0, )
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration"
})
public class SentinelMetricsAutoConfiguration {

    /**
     * The Property Name of enabling Alibaba Sentinel metrics : "microsphere.metrics.micrometer.alibaba-sentinel.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String SENTINEL_METRICS_ENABLED_PROPERTY_NAME = PREFIX + "alibaba-sentinel" + DOT + ENABLED_PROPERTY_NAME;

//        @Bean
//        @ConditionalOnBean(type = "io.micrometer.prometheus.PrometheusMeterRegistry")
//        public SentinelCollector sentinelCollector(PrometheusMeterRegistry registry,
//                                                   @Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration interval,
//                                                   @Value("${spring.application.name:default}") String applicationName) {
//            SentinelCollector sentinelCollector = new SentinelCollector(interval.toMillis());
//            sentinelCollector.commonLabel("application", applicationName);
//            PrometheusRegistry prometheusRegistry = registry.getPrometheusRegistry();
//            sentinelCollector.register(collectorRegistry);
//            return sentinelCollector;
//        }

    @Bean
    @ConditionalOnMissingBean
    public SentinelMetrics sentinelMetrics(@Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration interval) {
        return new SentinelMetrics(interval.toMillis());
    }
}
