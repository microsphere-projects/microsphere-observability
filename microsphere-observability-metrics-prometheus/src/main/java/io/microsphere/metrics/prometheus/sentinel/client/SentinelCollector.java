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

import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.node.metric.MetricTimerListener;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import io.microsphere.logging.Logger;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.BLOCK_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.CONCURRENCY_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.EXCEPTION_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.OCCUPIED_PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.PASS_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.RT_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.constants.MetricsConstants.SUCCESS_QPS_METRIC_NAME;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.getContextMetricNodesMap;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.getLabels;
import static io.prometheus.client.Collector.Type.GAUGE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Prometheus {@link Collector} based on ALibaba Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Collector
 * @see ClusterBuilderSlot
 * @see ClusterNode
 * @see MetricTimerListener
 * @see io.microsphere.metrics.micrometer.instrument.binder.sentinel.SentinelMetrics
 * @since 1.0.0
 */
public class SentinelCollector extends Collector {

    private static final Logger logger = getLogger(SentinelCollector.class);

    /**
     * The interval time of metrics collection in milliseconds.
     */
    private final long interval;

    private final Map<String, String> commonLabels;

    public SentinelCollector(long interval) {
        this(interval, emptyMap());
    }

    public SentinelCollector(long interval, Map<String, String> commonLabels) {
        this.interval = interval;
        this.commonLabels = commonLabels;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, List<MetricNode>> resourceMetricsNodesMap = getContextMetricNodesMap(this.interval);
        if (resourceMetricsNodesMap.isEmpty()) {
            return emptyList();
        }
        List<MetricFamilySamples> metricFamilySamplesList = newArrayList(resourceMetricsNodesMap.size());
        for (Map.Entry<String, List<MetricNode>> entry : resourceMetricsNodesMap.entrySet()) {
            List<MetricNode> metricNodes = entry.getValue();
            int size = metricNodes.size();
            if (size > 0) {
                String context = entry.getKey();
                String metric = context;

                List<Sample> samples = newArrayList(size * 7);
                for (int i = 0; i < size; i++) {
                    MetricNode metricNode = metricNodes.get(i);
                    samples.add(createSample(RT_METRIC_NAME, context, metricNode, MetricNode::getRt));
                    samples.add(createSample(CONCURRENCY_METRIC_NAME, context, metricNode, MetricNode::getConcurrency));
                    samples.add(createSample(SUCCESS_QPS_METRIC_NAME, context, metricNode, MetricNode::getSuccessQps));
                    samples.add(createSample(PASS_QPS_METRIC_NAME, context, metricNode, MetricNode::getPassQps));
                    samples.add(createSample(OCCUPIED_PASS_QPS_METRIC_NAME, context, metricNode, MetricNode::getOccupiedPassQps));
                    samples.add(createSample(BLOCK_QPS_METRIC_NAME, context, metricNode, MetricNode::getBlockQps));
                    samples.add(createSample(EXCEPTION_QPS_METRIC_NAME, context, metricNode, MetricNode::getExceptionQps));
                }

                metricFamilySamplesList.add(new MetricFamilySamples(metric, GAUGE, "Sentinel Context : " + context, samples));
            }
        }
        return metricFamilySamplesList;
    }

    private Sample createSample(String metricName, String context, MetricNode metricNode,
                                Function<MetricNode, Number> metricValueFunction) {
        Map<String, String> labels = getLabels(context, metricNode, this.commonLabels);
        List<String> labelNames = newArrayList(labels.keySet());
        List<String> labelValues = newArrayList(labels.values());
        Number value = metricValueFunction.apply(metricNode);
        Long timestampMs = metricNode.getTimestamp();
        return new Sample(metricName, labelNames, labelValues, value.doubleValue(), timestampMs);
    }
}