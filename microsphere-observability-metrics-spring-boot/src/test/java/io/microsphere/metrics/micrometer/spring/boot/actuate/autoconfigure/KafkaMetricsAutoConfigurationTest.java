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
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.microsphere.observability.logging.log4j2.spring.boot.Log4j2KafkaAppenderProperties;
import io.microsphere.spring.boot.test.AutoConfigurationTest;
import org.apache.kafka.clients.KafkaClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Set;

import static io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure.KafkaMetricsAutoConfiguration.KAFKA_METRICS_ENABLED_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link KafkaMetricsAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see KafkaMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                SimpleMeterRegistry.class,
                KafkaMetricsAutoConfigurationTest.class
        },
        webEnvironment = NONE
)
class KafkaMetricsAutoConfigurationTest extends AutoConfigurationTest<KafkaMetricsAutoConfiguration> {

    @Test
    void testConstants() {
        assertEquals("microsphere.metrics.micrometer.kafka.enabled", KAFKA_METRICS_ENABLED_PROPERTY_NAME);
    }

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.enabled=false");
        globalDisabledPropertyValues.add("microsphere.metrics.micrometer.kafka.enabled=false");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(MeterRegistry.class);
        globalMissingClasses.add(KafkaClientMetrics.class);
        globalMissingClasses.add(KafkaClient.class);
        globalMissingClasses.add(ProducerFactory.class);
        globalMissingClasses.add(Log4j2KafkaAppenderProperties.class);
    }
}