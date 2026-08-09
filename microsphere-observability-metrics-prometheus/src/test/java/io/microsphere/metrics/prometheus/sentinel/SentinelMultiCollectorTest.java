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

package io.microsphere.metrics.prometheus.sentinel;


import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SentinelMultiCollector} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMultiCollector
 * @since 1.0.0
 */
class SentinelMultiCollectorTest {

    private SentinelMultiCollector sentinelMultiCollector;

    private PrometheusRegistry registry;

    private SentinelMetricsTestHelper testHelper;

    @BeforeEach
    void setUp() {
        this.sentinelMultiCollector = new SentinelMultiCollector(60000).commonLabel("application", "test");
        this.registry = new PrometheusRegistry();
        this.registry.register(this.sentinelMultiCollector);
        this.testHelper = new SentinelMetricsTestHelper();
    }

    @Test
    void testCollect() throws Throwable {
        this.testHelper.doInSentinelMetrics(() -> {
            MetricSnapshots metricSnapshots = this.sentinelMultiCollector.collect();
            assertMetricSnapshots(metricSnapshots);
        });
    }

    @Test
    void testCollectOnSentinelMetricsRepositoryNotReady() {
        MetricSnapshots metricSnapshots = this.sentinelMultiCollector.collect();
        assertEquals(0, metricSnapshots.size());
    }

    void assertMetricSnapshots(MetricSnapshots metricSnapshots) {
        assertEquals(7, metricSnapshots.size());
    }
}