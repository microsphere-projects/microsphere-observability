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


import org.junit.jupiter.api.Test;

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
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getContextMetricNodesMap;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getMetricFamily;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SentinelMetricUtitls} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricUtitls
 * @since 1.0.0
 */
class SentinelMetricUtitlsTest {

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
    void testGetContextMetricNodesMap() {

    }

    @Test
    void testGetContextMetricNodesMapOnSentinelMetricsRepositoryNotReady() {
        assertSame(emptyMap(), getContextMetricNodesMap(1000));
    }
}