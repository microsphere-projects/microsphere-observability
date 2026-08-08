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


/**
 * The constants for Alibaba Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see com.alibaba.csp.sentinel.node.DefaultNode
 * @since 1.0.0
 */
public interface SentinelMetricsConstants {

    /**
     * The prefix : "sentinel_"
     */
    String PREFIX = "sentinel_";

    /**
     * The Label name of the metrics origin : "sentinel_origin"
     */
    String ORIGIN_LABEL_NAME = PREFIX + "origin";

    /**
     * The Label name for Sentinel Resource : "sentinel_resource"
     */
    String RESOURCE_LABEL_NAME = PREFIX + "resource";

    /**
     * The Label name for Sentinel Context : "sentinel_context"
     */
    String CONTEXT_LABEL_NAME = PREFIX + "context";

    /**
     * The Label name for Sentinel Resource Type : "sentinel_resource_type"
     */
    String RESOURCE_TYPE_LABEL_NAME = PREFIX + "resource_type";

    /**
     * The Label name for Sentinel Version : "sentinel_version"
     */
    String VERSION_LABEL_NAME = PREFIX + "version";

    /**
     * The Label name of the metrics Timestamp : "sentinel_timestamp"
     */
    String TIMESTAMP_LABEL_NAME = PREFIX + "timestamp";

    /**
     * The Metric name for Response Time (RT): "sentinel_rt"
     */
    String RT_METRIC_NAME = PREFIX + "rt";

    /**
     * The Metric name for Concurrency : "sentinel_concurrency"
     */
    String CONCURRENCY_METRIC_NAME = PREFIX + "concurrency";

    /**
     * The Metric name for Success QPS : "sentinel_success_qps"
     */
    String SUCCESS_QPS_METRIC_NAME = PREFIX + "success_qps";

    /**
     * The Metric name for Pass QPS : "sentinel_pass_qps"
     */
    String PASS_QPS_METRIC_NAME = PREFIX + "pass_qps";

    /**
     * The Metric name for Occupied Pass QPS : "sentinel_occupied_pass_qps"
     */
    String OCCUPIED_PASS_QPS_METRIC_NAME = PREFIX + "occupied_pass_qps";

    /**
     * The Metric name for Block QPS : "sentinel_block_qps"
     */
    String BLOCK_QPS_METRIC_NAME = PREFIX + "block_qps";

    /**
     * The Metric name for Exception QPS : "sentinel_exception_qps"
     */
    String EXCEPTION_QPS_METRIC_NAME = PREFIX + "exception_qps";
}