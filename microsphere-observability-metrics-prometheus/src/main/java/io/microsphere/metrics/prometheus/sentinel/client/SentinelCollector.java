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
import io.microsphere.metrics.commons.MetricFamily;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.ListUtils.newLinkedList;
import static io.microsphere.collection.MapUtils.newLinkedHashMap;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.METRIC_FAMILIES;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.METRIC_FAMILIES_SIZE;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.METRIC_NODE_TO_VALUE_FUNCTIONS;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.buildMetricName;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getContextMetricNodesMap;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getLabels;
import static io.microsphere.metrics.sentinel.util.SentinelMetricUtitls.getMetricFamily;
import static io.microsphere.util.StringUtils.EMPTY_STRING;
import static io.prometheus.client.Collector.Type.valueOf;
import static java.util.Collections.emptyList;

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
        this.interval = interval;
        this.commonLabels = newLinkedHashMap();
    }

    public SentinelCollector commonLabel(String name, String value) {
        this.commonLabels.put(name, value);
        return this;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, List<MetricNode>> resourceMetricsNodesMap = getContextMetricNodesMap(this.interval);
        if (resourceMetricsNodesMap.isEmpty()) {
            logger.warn("No metrics collected");
            return emptyList();
        }

        int size = METRIC_FAMILIES_SIZE;

        List<MetricFamilySamples> metricFamilySamplesList = newArrayList(size);
        Map<Integer, List<Sample>> samplesMap = newLinkedHashMap(size);

        for (Entry<String, List<MetricNode>> entry : resourceMetricsNodesMap.entrySet()) {
            String context = entry.getKey();
            List<MetricNode> metricNodes = entry.getValue();
            for (MetricNode metricNode : metricNodes) {
                addSamples(context, metricNode, samplesMap, size);
            }
        }

        for (int i = 0; i < size; i++) {
            List<Sample> samples = samplesMap.get(i);
            MetricFamily metricFamily = METRIC_FAMILIES.get(i);
            String name = metricFamily.getName();
            String unit = metricFamily.getUnit() == null ? EMPTY_STRING : metricFamily.getUnit();
            Type type = valueOf(metricFamily.getType().name());
            String help = metricFamily.getHelp();
            name = buildMetricName(name, unit);
            MetricFamilySamples metricFamilySamples = new MetricFamilySamples(name, unit, type, help, samples);
            metricFamilySamplesList.add(metricFamilySamples);
        }

        return metricFamilySamplesList;
    }

    private void addSamples(String context, MetricNode metricNode, Map<Integer, List<Sample>> samplesMap, int size) {
        for (int i = 0; i < size; i++) {
            addSample(context, metricNode, samplesMap, i);
        }
    }

    private void addSample(String context, MetricNode metricNode, Map<Integer, List<Sample>> samplesMap, int index) {
        MetricFamily metricFamily = getMetricFamily(index);
        Function<MetricNode, Number> metricNodeNumberFunction = METRIC_NODE_TO_VALUE_FUNCTIONS.get(index);
        List<Sample> samples = samplesMap.computeIfAbsent(index, k -> newLinkedList());
        Sample sample = createSample(metricFamily.getName(), context, metricNode, metricNodeNumberFunction);
        samples.add(sample);
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