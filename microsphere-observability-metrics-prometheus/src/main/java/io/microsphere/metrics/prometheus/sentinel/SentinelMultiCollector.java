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

import com.alibaba.csp.sentinel.node.metric.MetricNode;
import io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository;
import io.microsphere.logging.Logger;
import io.prometheus.metrics.model.registry.MetricType;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor;
import io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor.Builder;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.Unit;

import java.util.List;
import java.util.Map;

import static io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository.getSentinelMetricsRepository;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.ListUtils.newLinkedList;
import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.BLOCK_QPS_METRIC_SUFFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.CONCURRENCY_METRIC_SUFFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.EXCEPTION_QPS_METRIC_SUFFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.OCCUPIED_PASS_QPS_METRIC_SUFFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.ORIGIN_LABEL_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.PASS_QPS_METRIC_SUFFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.PREFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.RT_METRIC_SUFFIX;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.SUCCESS_QPS_METRIC_SUFFIX;
import static io.microsphere.util.ClassUtils.getSimpleName;
import static io.prometheus.metrics.model.registry.MetricType.GAUGE;
import static io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor.of;
import static io.prometheus.metrics.model.snapshots.MetricMetadata.builder;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;

/**
 * Prometheus {@link MultiCollector} based on ALibaba Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see MultiCollector
 * @see io.microsphere.metrics.micrometer.instrument.binder.sentinel.SentinelMetrics
 * @since 1.0.0
 */
public class SentinelMultiCollector implements MultiCollector {

    private static final Logger logger = getLogger(SentinelMultiCollector.class);

    /**
     * The interval time of metrics collection in milliseconds.
     */
    private final long interval;

    private final List<String> commonLabelNames;

    private final List<String> commonLabelValues;

    public SentinelMultiCollector(long interval) {
        this(interval, emptyMap());
    }

    public SentinelMultiCollector(long interval, Map<String, String> commonLabels) {
        this.interval = interval;
        this.commonLabelNames = initCommonLabelNames(commonLabels);
        this.commonLabelValues = initCommonLabelValues(commonLabels);
    }

    @Override
    public MetricSnapshots collect() {
        Map<String, List<MetricNode>> contextMetricsNodesMap = getContextMetricNodesMap();
        if (contextMetricsNodesMap.isEmpty()) {
            return new MetricSnapshots();
        }
        List<MetricSnapshot> metricSnapshots = newLinkedList();
        for (Map.Entry<String, List<MetricNode>> entry : contextMetricsNodesMap.entrySet()) {
            String contextName = entry.getKey();
            List<MetricNode> metricNodes = entry.getValue();
            for (MetricNode metricNode : metricNodes) {
                String resourceName = metricNode.getResource();
            }
        }
        return new MetricSnapshots(metricSnapshots);
    }

    @Override
    public List<MetricFamilyDescriptor> getMetricFamilyDescriptors() {
        return ofList(
                createMetricFamilyDescriptor(RT_METRIC_SUFFIX, GAUGE, new Unit("milliseconds"), "The Response Time (RT) of the Sentinel Resource"),
                createMetricFamilyDescriptor(CONCURRENCY_METRIC_SUFFIX, GAUGE, new Unit("times"), "The Concurrency of the Sentinel Resource"),
                createMetricFamilyDescriptor(SUCCESS_QPS_METRIC_SUFFIX, GAUGE, new Unit("times"), "The Success QPS of the Sentinel Resource"),
                createMetricFamilyDescriptor(PASS_QPS_METRIC_SUFFIX, GAUGE, new Unit("times"), "The Pass QPS of the Sentinel Resource"),
                createMetricFamilyDescriptor(OCCUPIED_PASS_QPS_METRIC_SUFFIX, GAUGE, new Unit("times"), "The Occupied Pass QPS of the Sentinel Resource"),
                createMetricFamilyDescriptor(BLOCK_QPS_METRIC_SUFFIX, GAUGE, new Unit("times"), "The Block QPS of the Sentinel Resource"),
                createMetricFamilyDescriptor(EXCEPTION_QPS_METRIC_SUFFIX, GAUGE, new Unit("times"), "The Block QPS of the Sentinel Resource")
        );
    }

    private Map<String, List<MetricNode>> getContextMetricNodesMap() {
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

    private MetricMetadata createMetricMetadata(String metricSuffix, Unit unit, String help) {
        String metricName = PREFIX + metricSuffix;
        return builder()
                .name(metricName)
                .unit(unit)
                .help(help)
                .build();
    }

    private MetricFamilyDescriptor createMetricFamilyDescriptor(String metricSuffix, MetricType type, Unit unit, String help) {
        String metricName = PREFIX + metricSuffix;

        Builder builder = of(type, metricName)
                .unit(unit)
                .help(help)
                .labelNames(commonLabelNames);

        return builder.build();
    }

    private List<String> initCommonLabelNames(Map<String, String> commonLabels) {
        List<String> labelNames = newArrayList(commonLabels.size() + 2);
        labelNames.add(ORIGIN_LABEL_NAME);
        labelNames.addAll(commonLabels.keySet());
        return labelNames;
    }

    private List<String> initCommonLabelValues(Map<String, String> commonLabels) {
        List<String> labelValues = newArrayList(commonLabels.size() + 2);
        labelValues.add(getSimpleName(this.getClass()));
        labelValues.addAll(commonLabels.values());
        return labelValues;
    }
}
