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


import com.alibaba.csp.sentinel.SphU;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.microsphere.alibaba.sentinel.common.SentinelPlugin;
import io.microsphere.metrics.micrometer.instrument.binder.sentinel.SentinelMetrics;
import io.microsphere.spring.boot.test.AutoConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;

import java.util.Set;

import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.SentinelMetricsAutoConfiguration.SENTINEL_METRICS_ENABLED_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.convert.ApplicationConversionService.getSharedInstance;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link SentinelMetricsAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                SimpleMeterRegistry.class,
                SentinelMetricsAutoConfigurationTest.class
        },
        webEnvironment = NONE
)
class SentinelMetricsAutoConfigurationTest extends AutoConfigurationTest<SentinelMetricsAutoConfiguration> {

    @Bean
    static ConversionService conversionService() {
        return getSharedInstance();
    }

    @Test
    void testConstants() {
        assertEquals("microsphere.metrics.micrometer.alibaba-sentinel.enabled", SENTINEL_METRICS_ENABLED_PROPERTY_NAME);
    }

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
        autoConfiguredClasses.add(SentinelMetrics.class);
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("microsphere.sentinel.enabled=false");
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.enabled=false");
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.alibaba-sentinel.enabled=false");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(MeterRegistry.class);
        globalMissingClasses.add(SphU.class);
        globalMissingClasses.add(SentinelPlugin.class);
    }
}