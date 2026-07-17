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

import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.constants.PropertyConstants;
import io.microsphere.metrics.micrometer.instrument.binder.system.NetworkStatisticsMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.SystemMemoryMetrics;
import io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnMicrometerEnabled.PREFIX;
import static io.microsphere.metrics.micrometer.util.MicrometerUtils.getScheduledExecutor;

/**
 * The Auto-Configuration class for System Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration
 * @see org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = {
        "org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration",            // Spring Boot API [2.0, 4.0)
        "org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration"   // Spring Boot API [4.0,)
})
@ConditionalOnProperty(name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnMicrometerEnabled
public class SystemMetricsAutoConfiguration {

    /**
     * The Property Name of enabling System metrics : "microsphere.metrics.micrometer.system.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true",
            source = APPLICATION_SOURCE
    )
    public static final String ENABLED_PROPERTY_NAME = PREFIX + "system" + DOT + PropertyConstants.ENABLED_PROPERTY_NAME;

    /**
     * The Default Property Value of metrics collection interval : "60000"
     */
    public static final String DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE = "60000";

    /**
     * The Property Name of metrics collection interval : "microsphere.metrics.collection.interval"
     */
    @ConfigurationProperty(
            type = long.class,
            defaultValue = DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE,
            source = APPLICATION_SOURCE
    )
    public static final String METRICS_COLLECTION_INTERVAL_PROPERTY_NAME = "microsphere.metrics.collection.interval";

    /**
     * The Property Placeholder of metrics collection interval : "${microsphere.metrics.collection.interval:60000}"
     */
    public static final String METRICS_COLLECTION_INTERVAL_PLACEHOLDER = "${" + METRICS_COLLECTION_INTERVAL_PROPERTY_NAME + ":" + DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE + "}";

    @Bean
    @ConditionalOnMissingBean
    public NetworkStatisticsMetrics networkStatisticsMetrics(@Value(METRICS_COLLECTION_INTERVAL_PLACEHOLDER) Duration interval) {
        return new NetworkStatisticsMetrics(getScheduledExecutor(), interval.toMillis());
    }

    @Bean
    @ConditionalOnMissingBean
    public SystemMemoryMetrics systemMemoryMetrics() {
        return new SystemMemoryMetrics();
    }
}
