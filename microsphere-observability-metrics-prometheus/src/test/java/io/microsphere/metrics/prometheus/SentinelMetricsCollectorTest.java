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

package io.microsphere.metrics.prometheus;


import com.alibaba.csp.sentinel.node.metric.MetricNode;
import io.microsphere.alibaba.sentinel.common.SentinelTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link SentinelMetricsCollector} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsCollector
 * @since 1.0.0
 */
class SentinelMetricsCollectorTest {

    private SentinelMetricsCollector sentinelMetricsCollector;

    @BeforeEach
    void setUp() {
        this.sentinelMetricsCollector = new SentinelMetricsCollector(60000L);
    }

    @Test
    void testGetContextMetricNodesMap() throws Throwable {
        String resourceName = "test-resource";
        SentinelTemplate sentinelTemplate = new SentinelTemplate();
        for (int i = 0; i < 100; i++) {
            sentinelTemplate.call(resourceName, () -> {
                sleep(10);
            });
        }

        sleep(500);

        Map<String, List<MetricNode>> contextMetricNodesMap = this.sentinelMetricsCollector.getContextMetricNodesMap();
        assertFalse(contextMetricNodesMap.isEmpty());
    }
}