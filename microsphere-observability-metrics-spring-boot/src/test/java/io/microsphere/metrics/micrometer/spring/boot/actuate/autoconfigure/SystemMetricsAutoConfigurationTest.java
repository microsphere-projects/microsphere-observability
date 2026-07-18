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


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.microsphere.metrics.micrometer.instrument.binder.system.NetworkStatisticsMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.SystemMemoryMetrics;
import io.microsphere.spring.boot.test.AutoConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;

import java.util.Set;

import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.METRICS_COLLECTION_INTERVAL_PLACEHOLDER;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.METRICS_COLLECTION_INTERVAL_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SystemMetricsAutoConfiguration.SYSTEM_METRICS_ENABLED_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.convert.ApplicationConversionService.getSharedInstance;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link SystemMetricsAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SystemMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                SimpleMeterRegistry.class,
                SystemMetricsAutoConfigurationTest.class
        },
        webEnvironment = NONE
)
class SystemMetricsAutoConfigurationTest extends AutoConfigurationTest<SystemMetricsAutoConfiguration> {

    @Bean
    static ConversionService conversionService() {
        return getSharedInstance();
    }

    @Test
    void testConstants() {
        assertEquals("microsphere.metrics.micrometer.system.enabled", SYSTEM_METRICS_ENABLED_PROPERTY_NAME);
        assertEquals("60000", DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE);
        assertEquals("microsphere.metrics.micrometer.system.collection.interval", METRICS_COLLECTION_INTERVAL_PROPERTY_NAME);
        assertEquals("${microsphere.metrics.micrometer.system.collection.interval:60000}", METRICS_COLLECTION_INTERVAL_PLACEHOLDER);
    }

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
        autoConfiguredClasses.add(NetworkStatisticsMetrics.class);
        autoConfiguredClasses.add(SystemMemoryMetrics.class);
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.enabled=false");
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.system.enabled=false");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(MeterRegistry.class);
    }
}