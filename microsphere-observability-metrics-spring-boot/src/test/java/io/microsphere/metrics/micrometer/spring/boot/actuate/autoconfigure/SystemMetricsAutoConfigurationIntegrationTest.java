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
import io.microsphere.metrics.micrometer.instrument.binder.system.NetworkStatisticsMetrics;
import io.microsphere.metrics.micrometer.instrument.binder.system.SystemMemoryMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link SystemMetricsAutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SystemMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                SystemMetricsAutoConfigurationIntegrationTest.class
        },
        webEnvironment = NONE,
        properties = {
                "system.network.stats.file=file://${user.dir}/../microsphere-observability-micrometer/src/test/resources/test-data/memory/network.stats"
        }
)
@EnableAutoConfiguration
class SystemMetricsAutoConfigurationIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private NetworkStatisticsMetrics networkStatisticsMetrics;

    @Autowired
    private SystemMemoryMetrics systemMemoryMetrics;

    @Test
    void test() {
        assertEquals(56, this.meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .filter(name -> name.startsWith(NetworkStatisticsMetrics.METRIC_PREFIX))
                .count());

        assertEquals(64, this.meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .filter(name -> name.startsWith(SystemMemoryMetrics.METRIC_PREFIX))
                .count());
    }
}
