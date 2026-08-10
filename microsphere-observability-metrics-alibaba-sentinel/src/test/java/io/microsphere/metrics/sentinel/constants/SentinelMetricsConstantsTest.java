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

package io.microsphere.metrics.sentinel.constants;

import org.junit.jupiter.api.Test;

import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.BLOCK_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.CONCURRENCY_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.CONTEXT_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.EXCEPTION_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.OCCUPIED_PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.ORIGIN_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.PREFIX;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RESOURCE_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RESOURCE_TYPE_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.RT_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.SUCCESS_QPS_METRIC_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.TIMESTAMP_LABEL_NAME;
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.VERSION_LABEL_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SentinelMetricsConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsConstants
 * @since 1.0.0
 */
class SentinelMetricsConstantsTest {

    @Test
    void testConstants() {
        assertEquals("sentinel_", PREFIX);
        assertEquals("sentinel_origin", ORIGIN_LABEL_NAME);
        assertEquals("sentinel_resource", RESOURCE_LABEL_NAME);
        assertEquals("sentinel_context", CONTEXT_LABEL_NAME);
        assertEquals("sentinel_resource_type", RESOURCE_TYPE_LABEL_NAME);
        assertEquals("sentinel_version", VERSION_LABEL_NAME);
        assertEquals("sentinel_timestamp", TIMESTAMP_LABEL_NAME);
        assertEquals("sentinel_rt", RT_METRIC_NAME);
        assertEquals("sentinel_concurrency", CONCURRENCY_METRIC_NAME);
        assertEquals("sentinel_success_qps", SUCCESS_QPS_METRIC_NAME);
        assertEquals("sentinel_pass_qps", PASS_QPS_METRIC_NAME);
        assertEquals("sentinel_occupied_pass_qps", OCCUPIED_PASS_QPS_METRIC_NAME);
        assertEquals("sentinel_block_qps", BLOCK_QPS_METRIC_NAME);
        assertEquals("sentinel_exception_qps", EXCEPTION_QPS_METRIC_NAME);
    }
}