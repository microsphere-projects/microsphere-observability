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
import io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository;
import io.microsphere.logging.Logger;
import io.microsphere.metrics.commons.MetricFamily;
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
import static io.microsphere.collection.MapUtils.of;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.commons.MetricFamily.builder;
import static io.microsphere.metrics.commons.MetricType.GAUGE;
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
import static io.microsphere.metrics.sentinel.constants.SentinelMetricsConstants.VERSION_LABEL_NAME;
import static io.microsphere.util.StringUtils.isBlank;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * The utilities class of Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsRepository
 * @since 1.0.0
 */
public abstract class SentinelMetricUtitls implements Utils {

    private static final Logger logger = getLogger(SentinelMetricUtitls.class);

    private static final Map<String, BiFunction<String, MetricNode, String>> REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP = of(
            RESOURCE_LABEL_NAME, (context, metricNode) -> metricNode.getResource(),
            CONTEXT_LABEL_NAME, (context, metricNode) -> context,
            RESOURCE_TYPE_LABEL_NAME, (type, metricNode) -> getResourceTypeAsString(metricNode.getClassification()),
            VERSION_LABEL_NAME, (version, metricNode) -> SENTINEL_VERSION
    );

    /**
     * The required labels of the Metrics Family
     */
    public static final Set<String> REQUIRED_LABEL_NAMES = REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP.keySet();

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
     * The size of {@link #REQUIRED_LABEL_NAMES}
     */
    public static final int REQUIRED_LABEL_NAMES_SIZE = REQUIRED_LABEL_NAMES.size();

    /**
     * The size of {@link #METRIC_FAMILIES}
     */
    public static final int METRIC_FAMILIES_SIZE = METRIC_FAMILIES.size();

    /**
     * Get the {@link MetricFamily} by index
     *
     * @param index the index of {@link MetricFamily}
     * @return non-null {@link MetricFamily}
     */
    public static MetricFamily getMetricFamily(int index) {
        return METRIC_FAMILIES.get(index);
    }

    /**
     * Build the metric name of the given {@link MetricFamily}
     *
     * @param metricFamily the {@link MetricFamily} instance
     * @return the metric name
     */
    public static String buildMetricName(MetricFamily metricFamily) {
        return buildMetricName(metricFamily.getName(), metricFamily.getUnit());
    }

    /**
     * Build the metric name of the given name and unit
     *
     * @param name the name of metric
     * @param unit the unit of metric
     * @return the metric name
     */
    public static String buildMetricName(String name, String unit) {
        name = isBlank(unit) ? name : name + "_" + unit;
        return name;
    }

    /**
     * Get the {@link Map} of {@link MetricNode} by time interval
     *
     * @param interval the time interval
     * @return non-null read-only {@link Map}
     */
    public static Map<String, List<MetricNode>> getContextMetricNodesMap(long interval) {
        SentinelMetricsRepository sentinelMetricsRepository = getSentinelMetricsRepository();
        if (sentinelMetricsRepository == null) {
            logger.warn("SentinelMetricsRepository is not ready, please check whether it is configured");
            return emptyMap();
        }

        long endTimeMs = currentTimeMillis();
        long beginTimeMs = endTimeMs - interval;

        return sentinelMetricsRepository.findContextMetricNodesMap(beginTimeMs, endTimeMs);
    }

    /**
     * Combine the {@link Map} of Alibaba Sentinel labels from {@link MetricNode}
     *
     * @param context    the context
     * @param metricNode the {@link MetricNode} instance
     * @return non-null read-only {@link Map}
     */
    public static Map<String, String> combineLabels(String context, MetricNode metricNode, Map<String, String> commonLabels) {
        Map<String, String> labels = newFixedLinkedHashMap(REQUIRED_LABEL_NAMES_SIZE + commonLabels.size());
        labels.putAll(commonLabels);
        labels.putAll(getRequiredLabels(context, metricNode));
        return labels;
    }

    /**
     * Get the required labels of the given {@link MetricNode}
     *
     * @param context    the context
     * @param metricNode the {@link MetricNode} instance
     * @return non-null read-only {@link Map}
     */
    public static Map<String, String> getRequiredLabels(String context, MetricNode metricNode) {
        Map<String, String> labels = newFixedLinkedHashMap(REQUIRED_LABEL_NAMES_SIZE);
        REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP.forEach((labelName, function) -> {
            String value = function.apply(context, metricNode);
            labels.put(labelName, value);
        });
        return unmodifiableMap(labels);
    }

    /**
     * Get the label value of the given {@link MetricNode} by label name
     *
     * @param labelName  the name of label
     * @param context    the context
     * @param metricNode the {@link MetricNode} instance
     * @return the label value
     * @throws NullPointerException if the labelName can not be found in {@link #REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP}
     */
    public static String getRequiredLabelValue(String labelName, String context, MetricNode metricNode) {
        return REQUIRED_LABEL_NAME_TO_VALUE_FUNCTION_MAP.get(labelName).apply(context, metricNode);
    }

    /**
     * Get the metric value of the given {@link MetricNode} by index
     *
     * @param metricNode the {@link MetricNode} instance
     * @param index      the index of {@link #METRIC_NODE_TO_VALUE_FUNCTIONS}
     * @return the metric value
     * @throws IndexOutOfBoundsException if the index is out of range of {@link #METRIC_NODE_TO_VALUE_FUNCTIONS}
     * @throws NullPointerException      if the metricNode is null
     */
    public static double getMetricValue(MetricNode metricNode, int index) {
        Function<MetricNode, Number> function = METRIC_NODE_TO_VALUE_FUNCTIONS.get(index);
        return function.apply(metricNode).doubleValue();
    }

    private SentinelMetricUtitls() {
    }
}