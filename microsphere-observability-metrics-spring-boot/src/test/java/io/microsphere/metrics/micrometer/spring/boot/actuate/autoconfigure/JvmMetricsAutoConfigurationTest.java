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
import io.microsphere.spring.boot.test.AutoConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.JVM_METRICS_ENABLED_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.JvmMetricsAutoConfiguration.JVM_METRICS_PROPERTY_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link JvmMetricsAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see JvmMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                JvmMetricsAutoConfigurationTest.class
        },
        webEnvironment = NONE
)
class JvmMetricsAutoConfigurationTest extends AutoConfigurationTest<JvmMetricsAutoConfiguration> {

    @Test
    void testConstants() {
        assertEquals("microsphere.metrics.micrometer.jvm.", JVM_METRICS_PROPERTY_NAME_PREFIX);
        assertEquals("microsphere.metrics.micrometer.jvm.enabled", JVM_METRICS_ENABLED_PROPERTY_NAME);
        assertEquals("microsphere.metrics.micrometer.jvm.executor-service.", EXECUTOR_SERVICE_METRICS_PROPERTY_NAME_PREFIX);
        assertEquals("microsphere.metrics.micrometer.jvm.executor-service.prefix", EXECUTOR_SERVICE_METRICS_PREFIX_PROPERTY_NAME);
        assertEquals("microsphere.metrics.micrometer.jvm.executor-service.tags", EXECUTOR_SERVICE_METRICS_TAGS_PROPERTY_NAME);
    }

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.enabled=false");
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.jvm.enabled=false");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(MeterRegistry.class);
    }
}