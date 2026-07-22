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

package io.microsphere.metrics.prometheus.client.sentinel;


import io.microsphere.alibaba.sentinel.common.SentinelTemplate;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @BeforeEach
    void setUp() {
        this.registry = new CollectorRegistry();
        this.sentinelCollector = new SentinelCollector(60000);
        this.sentinelCollector.register(registry);
        this.sentinelCollector.commonLabel("test-label", "test-value");
    }

    @Test
    void test() throws Throwable {
        String resourceName = "test-resource";
        SentinelTemplate sentinelTemplate = new SentinelTemplate();
        for (int i = 0; i < 10; i++) {
            sentinelTemplate.call(resourceName, () -> {
                sleep(100);
            });
        }

        sleep(500);

        Enumeration<MetricFamilySamples> metricFamilySamples = this.registry.metricFamilySamples();

        while (metricFamilySamples.hasMoreElements()) {
            MetricFamilySamples samples = metricFamilySamples.nextElement();
            assertFalse(samples.samples.isEmpty());
        }
    }
}