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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.microsphere.alibaba.sentinel.common.SentinelTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrderer.OrderAnnotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.PREFIX;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link SentinelMetricsAutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsAutoConfiguration
 * @since 1.0.0
 */
@TestClassOrder(OrderAnnotation.class)
class SentinelMetricsAutoConfigurationIntegrationTest {

    @Order(1)
    @Nested
    @DisplayName("Default Test")
    @SpringBootTest(
            classes = {
                    SentinelMetricsAutoConfigurationIntegrationTest.class
            },
            webEnvironment = NONE
    )
    @DirtiesContext
    @EnableAutoConfiguration
    class DefaultTest {

        @BeforeEach
        void setUp() throws Throwable {
            SentinelTemplate sentinelTemplate = new SentinelTemplate();
            for (int i = 0; i < 100; i++) {
                sentinelTemplate.call("test-resource-" + (i + 1), () -> {
                    sleep(10L);
                });
            }
        }

        @Autowired
        private MeterRegistry registry;

        @Test
        void test() throws Throwable {

            List<Meter> meters = this.registry.getMeters();

            long count = meters.stream()
                    .map(Meter::getId)
                    .map(Meter.Id::getName)
                    .filter(name -> name.startsWith(PREFIX))
                    .count();

            assertTrue(count > 0);
        }
    }

    @Order(2)
    @Nested
    @DisplayName("Prometheus Test")
    @SpringBootTest(
            classes = {
                    SentinelMetricsAutoConfigurationIntegrationTest.class
            },
            webEnvironment = NONE,
            properties = {
                    "management.prometheus.metrics.export.enabled=true"
            }
    )
    @DirtiesContext
    @EnableAutoConfiguration
    class PrometheusTest {

        @Autowired
        private PrometheusMeterRegistry registry;

        @Test
        void test() throws Throwable {
            String content = registry.scrape();
            assertTrue(content.contains(PREFIX));
        }
    }
}
