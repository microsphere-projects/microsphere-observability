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

package io.microsphere.metrics.sentinel.util;


import com.alibaba.csp.sentinel.node.metric.MetricNode;
import io.microsphere.alibaba.sentinel.common.SentinelTemplate;
import io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION;
import static com.alibaba.csp.sentinel.ResourceTypeConstants.COMMON_WEB;
import static io.microsphere.alibaba.sentinel.common.util.ProcessorSlotCallbackUtils.addEntryCallback;
import static io.microsphere.alibaba.sentinel.common.util.ProcessorSlotCallbackUtils.removeEntryCallback;
import static io.microsphere.collection.MapUtils.of;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.BLOCK_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.CONCURRENCY_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.CONTEXT_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.EXCEPTION_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.OCCUPIED_PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RESOURCE_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RESOURCE_TYPE_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RT_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.SUCCESS_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.TIMESTAMP_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.VERSION_LABEL_NAME;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.METRIC_FAMILIES;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.METRIC_FAMILIES_SIZE;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.METRIC_NODE_TO_VALUE_FUNCTIONS;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.REQUIRED_LABEL_NAMES;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.buildMetricName;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.combineLabels;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getContextMetricNodesMap;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getMetricFamily;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getMetricValue;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getRequiredLabelValue;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getRequiredLabels;
import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SentinelMetricUtitls} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricUtitls
 * @since 1.0.0
 */
class SentinelMetricUtitlsTest {

    String context = "test-context";

    String resource = "test-resource";

    long timestamp = System.currentTimeMillis();

    MetricNode metricNode;

    @BeforeEach
    void setUp() {
        metricNode = new MetricNode();
        metricNode.setResource(resource);
        metricNode.setClassification(COMMON_WEB);
        metricNode.setTimestamp(timestamp);
    }

    @Test
    void testConstants() {
        assertEquals(5, REQUIRED_LABEL_NAMES.size());
        assertTrue(REQUIRED_LABEL_NAMES.contains(RESOURCE_LABEL_NAME));
        assertTrue(REQUIRED_LABEL_NAMES.contains(CONTEXT_LABEL_NAME));
        assertTrue(REQUIRED_LABEL_NAMES.contains(RESOURCE_TYPE_LABEL_NAME));
        assertTrue(REQUIRED_LABEL_NAMES.contains(VERSION_LABEL_NAME));
        assertTrue(REQUIRED_LABEL_NAMES.contains(TIMESTAMP_LABEL_NAME));

        assertEquals(7, METRIC_FAMILIES_SIZE);
        assertEquals(RT_METRIC_NAME, METRIC_FAMILIES.get(0).getName());
        assertEquals(CONCURRENCY_METRIC_NAME, METRIC_FAMILIES.get(1).getName());
        assertEquals(SUCCESS_QPS_METRIC_NAME, METRIC_FAMILIES.get(2).getName());
        assertEquals(PASS_QPS_METRIC_NAME, METRIC_FAMILIES.get(3).getName());
        assertEquals(OCCUPIED_PASS_QPS_METRIC_NAME, METRIC_FAMILIES.get(4).getName());
        assertEquals(BLOCK_QPS_METRIC_NAME, METRIC_FAMILIES.get(5).getName());
        assertEquals(EXCEPTION_QPS_METRIC_NAME, METRIC_FAMILIES.get(6).getName());

        assertEquals(7, METRIC_NODE_TO_VALUE_FUNCTIONS.size());
    }

    @Test
    void testGetMetricFamily() {
        assertEquals(RT_METRIC_NAME, getMetricFamily(0).getName());
        assertEquals(CONCURRENCY_METRIC_NAME, getMetricFamily(1).getName());
        assertEquals(SUCCESS_QPS_METRIC_NAME, getMetricFamily(2).getName());
        assertEquals(PASS_QPS_METRIC_NAME, getMetricFamily(3).getName());
        assertEquals(OCCUPIED_PASS_QPS_METRIC_NAME, getMetricFamily(4).getName());
        assertEquals(BLOCK_QPS_METRIC_NAME, getMetricFamily(5).getName());
        assertEquals(EXCEPTION_QPS_METRIC_NAME, getMetricFamily(6).getName());
    }

    @Test
    void testBuildMetricName() {
        assertEquals(RT_METRIC_NAME + "_millseconds", buildMetricName(getMetricFamily(0)));
        assertEquals(CONCURRENCY_METRIC_NAME, buildMetricName(getMetricFamily(1)));
        assertEquals(SUCCESS_QPS_METRIC_NAME, buildMetricName(getMetricFamily(2)));
        assertEquals(PASS_QPS_METRIC_NAME, buildMetricName(getMetricFamily(3)));
        assertEquals(OCCUPIED_PASS_QPS_METRIC_NAME, buildMetricName(getMetricFamily(4)));
        assertEquals(BLOCK_QPS_METRIC_NAME, buildMetricName(getMetricFamily(5)));
        assertEquals(EXCEPTION_QPS_METRIC_NAME, buildMetricName(getMetricFamily(6)));
    }

    @Test
    void testGetContextMetricNodesMap() throws Throwable {
        SentinelTemplate sentinelTemplate = new SentinelTemplate();
        for (int i = 0; i < 100; i++) {
            sentinelTemplate.call(resource, () -> sleep(10));
        }

        SentinelMetricsRepository sentinelMetricsRepository = new SentinelMetricsRepository();
        try {
            addEntryCallback(sentinelMetricsRepository);
            Map<String, List<MetricNode>> contextMetricNodesMap = getContextMetricNodesMap(60000);
            do {
                sleep(100);
                contextMetricNodesMap = getContextMetricNodesMap(60000);
            } while (contextMetricNodesMap.isEmpty());

            assertFalse(contextMetricNodesMap.isEmpty());

        } finally {
            removeEntryCallback(sentinelMetricsRepository.getClass());
        }
    }

    @Test
    void testGetContextMetricNodesMapOnSentinelMetricsRepositoryNotReady() {
        assertSame(emptyMap(), getContextMetricNodesMap(1000));
    }

    @Test
    void testCombineLabels() {
        Map<String, String> commonLabels = of("key1", "value1");
        Map<String, String> labels = combineLabels(context, metricNode, commonLabels);
        assertEquals(6, labels.size());
        assertEquals("value1", labels.get("key1"));
        assertRequiredLabels(labels);
    }

    @Test
    void testGetRequiredLabels() {
        Map<String, String> requiredLabels = getRequiredLabels(context, metricNode);
        assertEquals(5, requiredLabels.size());
        assertRequiredLabels(requiredLabels);
    }

    @Test
    void testGetRequiredLabelValue() {
        assertEquals(resource, getRequiredLabelValue(RESOURCE_LABEL_NAME, context, metricNode));
        assertEquals(context, getRequiredLabelValue(CONTEXT_LABEL_NAME, context, metricNode));
        assertEquals("COMMON_WEB", getRequiredLabelValue(RESOURCE_TYPE_LABEL_NAME, context, metricNode));
        assertEquals(valueOf(timestamp), getRequiredLabelValue(TIMESTAMP_LABEL_NAME, context, metricNode));
        assertEquals(SENTINEL_VERSION, getRequiredLabelValue(VERSION_LABEL_NAME, context, metricNode));

        assertThrows(NullPointerException.class, () -> getRequiredLabelValue("not-found", context, metricNode));
    }

    @Test
    void testGetMetricValue() {
        assertEquals(0.0, getMetricValue(metricNode, 0));
        assertEquals(0.0, getMetricValue(metricNode, 1));
        assertEquals(0.0, getMetricValue(metricNode, 2));
        assertEquals(0.0, getMetricValue(metricNode, 3));
        assertEquals(0.0, getMetricValue(metricNode, 4));
        assertEquals(0.0, getMetricValue(metricNode, 5));
        assertEquals(0.0, getMetricValue(metricNode, 6));

        assertThrows(IndexOutOfBoundsException.class, () -> getMetricValue(metricNode, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> getMetricValue(metricNode, 7));
        assertThrows(NullPointerException.class, () -> getMetricValue(null, 0));
    }

    void assertRequiredLabels(Map<String, String> requiredLabels) {
        assertRequiredLabel(requiredLabels, RESOURCE_LABEL_NAME, resource);
        assertRequiredLabel(requiredLabels, CONTEXT_LABEL_NAME, context);
        assertRequiredLabel(requiredLabels, RESOURCE_TYPE_LABEL_NAME, "COMMON_WEB");
        assertRequiredLabel(requiredLabels, TIMESTAMP_LABEL_NAME, valueOf(timestamp));
        assertRequiredLabel(requiredLabels, VERSION_LABEL_NAME, SENTINEL_VERSION);
    }

    void assertRequiredLabel(Map<String, String> requiredLabels, String name, String value) {
        assertEquals(value, requiredLabels.get(name));
    }
}