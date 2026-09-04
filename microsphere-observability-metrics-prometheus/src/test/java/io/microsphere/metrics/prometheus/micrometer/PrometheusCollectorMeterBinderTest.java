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

package io.microsphere.metrics.prometheus.micrometer;


import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.microsphere.metrics.prometheus.sentinel.SentinelMetricsTestHelper;
import io.microsphere.metrics.prometheus.sentinel.client.SentinelCollector;
import io.microsphere.metrics.prometheus.sentinel.client.SentinelCollectorTest;
import io.prometheus.client.Collector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.micrometer.core.instrument.Meter.Type.COUNTER;
import static io.micrometer.core.instrument.Meter.Type.DISTRIBUTION_SUMMARY;
import static io.micrometer.core.instrument.Meter.Type.GAUGE;
import static io.micrometer.core.instrument.Meter.Type.OTHER;
import static io.micrometer.core.instrument.Meter.Type.TIMER;
import static io.micrometer.core.instrument.Statistic.VALUE;
import static io.micrometer.core.instrument.Statistic.values;
import static io.microsphere.metrics.prometheus.micrometer.PrometheusCollectorMeterBinder.toStatistic;
import static io.microsphere.metrics.prometheus.micrometer.PrometheusCollectorMeterBinder.toType;
import static io.prometheus.client.Collector.Type.HISTOGRAM;
import static io.prometheus.client.Collector.Type.SUMMARY;
import static io.prometheus.client.Collector.Type.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PrometheusCollectorMeterBinder} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see PrometheusCollectorMeterBinder
 * @since 1.0.0
 */
class PrometheusCollectorMeterBinderTest {

    @Test
    void testBindTo() throws Throwable {
        SentinelCollectorTest sentinelCollectorTest = new SentinelCollectorTest();
        sentinelCollectorTest.setUp();

        SentinelCollector sentinelCollector = sentinelCollectorTest.sentinelCollector;
        SentinelMetricsTestHelper testHelper = sentinelCollectorTest.testHelper;

        testHelper.doInSentinelMetrics(() -> {
            MeterRegistry registry = new SimpleMeterRegistry();
            PrometheusCollectorMeterBinder binder = new PrometheusCollectorMeterBinder(sentinelCollector);
            binder.bindTo(registry);

            List<Meter> meters = registry.getMeters();
            assertTrue(meters.size() > 7);
        });
    }

    @Test
    void testToType() {
        assertSame(COUNTER, toType(Collector.Type.COUNTER));
        assertSame(GAUGE, toType(Collector.Type.GAUGE));
        assertSame(DISTRIBUTION_SUMMARY, toType(SUMMARY));
        assertSame(TIMER, toType(HISTOGRAM));
        assertSame(OTHER, toType(UNKNOWN));
    }

    @Test
    void testToStatistic() {
        for (Statistic statistic : values()) {
            assertNotNull(toStatistic("_" + statistic.getTagValueRepresentation()));
        }
        assertSame(VALUE, toStatistic("unknown-statistic"));
    }
}