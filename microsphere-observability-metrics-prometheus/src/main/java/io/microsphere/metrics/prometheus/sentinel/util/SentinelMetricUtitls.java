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

package io.microsphere.metrics.prometheus.sentinel.util;

import com.alibaba.csp.sentinel.node.metric.MetricNode;
import io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository;
import io.microsphere.logging.Logger;
import io.microsphere.metrics.prometheus.sentinel.MetricFamily;
import io.microsphere.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION;
import static io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository.getSentinelMetricsRepository;
import static io.microsphere.alibaba.sentinel.common.util.SentinelUtils.getResourceTypeAsString;
import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.collection.MapUtils.newFixedLinkedHashMap;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.prometheus.sentinel.MetricFamily.builder;
import static io.microsphere.metrics.prometheus.sentinel.MetricType.GAUGE;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.BLOCK_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.CONCURRENCY_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.CONTEXT_LABEL_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.EXCEPTION_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.OCCUPIED_PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.RESOURCE_LABEL_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.RT_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.SUCCESS_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.TIMESTAMP_LABEL_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.TYPE_LABEL_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.VERSION_LABEL_NAME;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;

/**
 * The utilities class of Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsRepository
 * @since 1.0.0
 */
public abstract class SentinelMetricUtitls implements Utils {

    private static final Logger logger = getLogger(SentinelMetricUtitls.class);

    private static final Map<String, BiFunction<String, MetricNode, String>> LABEL_NAME_TO_VALUE_FUNCTION_MAP = Map.of(
            RESOURCE_LABEL_NAME, (context, metricNode) -> metricNode.getResource(),
            CONTEXT_LABEL_NAME, (context, metricNode) -> context,
            TYPE_LABEL_NAME, (type, metricNode) -> getResourceTypeAsString(metricNode.getClassification()),
            TIMESTAMP_LABEL_NAME, (timestamp, metricNode) -> valueOf(metricNode.getTimestamp()),
            VERSION_LABEL_NAME, (version, metricNode) -> SENTINEL_VERSION
    );

    /**
     * The required labels of the Metrics Family
     */
    public static final Set<String> REQUIRED_LABEL_NAMES = LABEL_NAME_TO_VALUE_FUNCTION_MAP.keySet();

    /**
     * The {@link MetricFamily} list of Alibaba Sentinel Metrics
     */
    public static final List<MetricFamily> METRIC_FAMILIES = ofList(
            builder().name(RT_METRIC_NAME).unit("millseconds").type(GAUGE).help("The Response Time (RT) of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build(),
            builder().name(CONCURRENCY_METRIC_NAME).type(GAUGE).help("The Concurrency of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build(),
            builder().name(SUCCESS_QPS_METRIC_NAME).type(GAUGE).help("The Success QPS of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build(),
            builder().name(PASS_QPS_METRIC_NAME).type(GAUGE).help("The Pass QPS of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build(),
            builder().name(OCCUPIED_PASS_QPS_METRIC_NAME).type(GAUGE).help("The Occupied Pass QPS of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build(),
            builder().name(BLOCK_QPS_METRIC_NAME).type(GAUGE).help("The Block QPS of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build(),
            builder().name(EXCEPTION_QPS_METRIC_NAME).type(GAUGE).help("The Block QPS of the Sentinel Resource").labelNames(REQUIRED_LABEL_NAMES).build()
    );

    /**
     * The {@link Function} list of {@link MetricNode} to {@link Number}
     */
    public static final List<Function<MetricNode, Number>> METRIC_NODE_TO_VALUE_FUNCTIONS = ofList(
            MetricNode::getRt,
            MetricNode::getConcurrency,
            MetricNode::getSuccessQps,
            MetricNode::getPassQps,
            MetricNode::getOccupiedPassQps,
            MetricNode::getBlockQps,
            MetricNode::getExceptionQps
    );

    /**
     * The size of {@link #METRIC_FAMILIES}
     */
    public static final int METRIC_FAMILY_SIZE = METRIC_FAMILIES.size();

    /**
     * Get the {@link Map} of {@link MetricNode} by time interval
     *
     * @param interval the time interval
     * @return non-null read-only {@link Map}
     */
    public static Map<String, List<MetricNode>> getContextMetricNodesMap(long interval) {
        SentinelMetricsRepository sentinelMetricsRepository = getSentinelMetricsRepository();
        if (sentinelMetricsRepository == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("SentinelMetricsRepository is not ready, please check whether it is configured");
            }
            return emptyMap();
        }

        long endTimeMs = currentTimeMillis();
        long beginTimeMs = endTimeMs - interval;

        return sentinelMetricsRepository.findContextMetricNodesMap(beginTimeMs, endTimeMs);
    }

    /**
     * Get the {@link Map} of Alibaba Sentinel labels from {@link MetricNode}
     *
     * @param context    the context
     * @param metricNode the {@link MetricNode} instance
     * @return non-null read-only {@link Map}
     */
    public static Map<String, String> getLabels(String context, MetricNode metricNode, Map<String, String> commonLabels) {
        Map<String, String> labels = newFixedLinkedHashMap(LABEL_NAME_TO_VALUE_FUNCTION_MAP.size() + commonLabels.size());
        labels.putAll(commonLabels);
        LABEL_NAME_TO_VALUE_FUNCTION_MAP.forEach((labelName, function) -> {
            String value = function.apply(context, metricNode);
            labels.put(labelName, value);
        });
        return labels;
    }

    private SentinelMetricUtitls() {
    }
}