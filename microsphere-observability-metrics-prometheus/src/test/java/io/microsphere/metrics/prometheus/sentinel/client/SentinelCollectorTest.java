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

package io.microsphere.metrics.prometheus.sentinel.client;


import io.microsphere.metrics.prometheus.sentinel.MetricFamily;
import io.microsphere.metrics.prometheus.sentinel.SentinelMetricsTestHelper;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.List;

import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.Maps.ofMap;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.buildMetricName;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.getMetricFamily;
import static io.microsphere.util.ArrayUtils.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SentinelCollector} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelCollector
 * @since 1.0.0
 */
class SentinelCollectorTest {

    private CollectorRegistry registry;

    private SentinelCollector sentinelCollector;

    private SentinelMetricsTestHelper testHelper;

    @BeforeEach
    void setUp() {
        this.registry = new CollectorRegistry();
        this.sentinelCollector = new SentinelCollector(60000, ofMap("test-label", "test-value"));
        this.sentinelCollector.register(registry);
        this.testHelper = new SentinelMetricsTestHelper();
    }

    @Test
    void testCollect() throws Throwable {
        this.testHelper.doInSentinelMetrics(() -> {
            Enumeration<MetricFamilySamples> metricFamilySamples = this.registry.metricFamilySamples();
            List<MetricFamilySamples> metricFamilySamplesList = newArrayList(metricFamilySamples);
            assertMetricFamilySamplesList(metricFamilySamplesList);
        });
    }

    @Test
    void testCollectOnSentinelMetricsRepositoryNotReady() {
        Enumeration<MetricFamilySamples> metricFamilySamples = this.registry.metricFamilySamples();
        List<MetricFamilySamples> metricFamilySamplesList = newArrayList(metricFamilySamples);
        assertTrue(metricFamilySamplesList.isEmpty());
    }

    void assertMetricFamilySamplesList(List<MetricFamilySamples> metricFamilySamplesList) {
        assertEquals(7, metricFamilySamplesList.size());
        for (int i = 0; i < metricFamilySamplesList.size(); i++) {
            MetricFamilySamples samples = metricFamilySamplesList.get(i);
            assertMetricFamilySamples(samples, i);
        }
    }

    void assertMetricFamilySamples(MetricFamilySamples samples, int index) {
        MetricFamily metricFamily = getMetricFamily(index);
        assertTrue(contains(samples.getNames(), buildMetricName(metricFamily)));
        assertEquals(buildMetricName(metricFamily), samples.name);
        assertEquals(metricFamily.getType().name(), samples.type.name());
        assertEquals(metricFamily.getHelp(), samples.help);
    }
}