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
import io.microsphere.logging.Logger;
import io.prometheus.metrics.model.registry.MetricType;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor;
import io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor.Builder;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.Unit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.ListUtils.newLinkedList;
import static io.microsphere.collection.MapUtils.newFixedLinkedHashMap;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.METRIC_FAMILIES;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.METRIC_FAMILY_SIZE;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.METRIC_NODE_TO_VALUE_FUNCTIONS;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.getContextMetricNodesMap;
import static io.microsphere.metrics.prometheus.sentinel.util.SentinelMetricUtitls.getLabels;
import static io.microsphere.util.StringUtils.isNotBlank;
import static io.prometheus.metrics.model.registry.MetricType.valueOf;
import static io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor.of;
import static io.prometheus.metrics.model.snapshots.PrometheusNaming.sanitizeMetricName;
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

    private final Map<String, String> commonLabels;

    private final List<MetricFamilyDescriptor> metricFamilyDescriptors;

    public SentinelMultiCollector(long interval) {
        this(interval, emptyMap());
    }

    public SentinelMultiCollector(long interval, Map<String, String> commonLabels) {
        this.interval = interval;
        this.commonLabels = commonLabels;
        this.metricFamilyDescriptors = buildMetricFamilyDescriptors();
    }

    @Override
    public MetricSnapshots collect() {
        Map<String, List<MetricNode>> contextMetricsNodesMap = getContextMetricNodesMap(this.interval);
        if (contextMetricsNodesMap.isEmpty()) {
            return new MetricSnapshots();
        }

        int size = metricFamilyDescriptors.size();

        Map<Integer, List<GaugeDataPointSnapshot>> gaugeDataPointSnapshotsMap = newFixedLinkedHashMap(size);

        for (Map.Entry<String, List<MetricNode>> entry : contextMetricsNodesMap.entrySet()) {
            String context = entry.getKey();
            List<MetricNode> metricNodes = entry.getValue();
            for (MetricNode metricNode : metricNodes) {
                addGaugeDataPointSnapshots(context, metricNode, gaugeDataPointSnapshotsMap, size);
            }
        }

        List<MetricSnapshot> metricSnapshots = newArrayList(size);
        for (int i = 0; i < size; i++) {
            MetricSnapshot metricSnapshot = newMetricSnapshot(gaugeDataPointSnapshotsMap, i);
            metricSnapshots.add(metricSnapshot);
        }

        return new MetricSnapshots(metricSnapshots);
    }

    private MetricSnapshot newMetricSnapshot(Map<Integer, List<GaugeDataPointSnapshot>> gaugeDataPointSnapshotsMap, int index) {
        MetricFamilyDescriptor metricFamilyDescriptor = this.metricFamilyDescriptors.get(index);
        MetricMetadata metadata = metricFamilyDescriptor.getMetadata();
        List<GaugeDataPointSnapshot> dataPoints = gaugeDataPointSnapshotsMap.get(index);
        return new GaugeSnapshot(metadata, dataPoints);
    }

    private void addGaugeDataPointSnapshots(String context, MetricNode metricNode,
                                            Map<Integer, List<GaugeDataPointSnapshot>> gaugeDataPointSnapshotsMap, int size) {
        for (int i = 0; i < size; i++) {
            addGaugeDataPointSnapshot(context, metricNode, i, gaugeDataPointSnapshotsMap);
        }
    }

    private void addGaugeDataPointSnapshot(String context, MetricNode metricNode,
                                           int index, Map<Integer, List<GaugeDataPointSnapshot>> gaugeDataPointSnapshotsMap) {
        Function<MetricNode, Number> metricNodeNumberFunction = METRIC_NODE_TO_VALUE_FUNCTIONS.get(index);
        double value = metricNodeNumberFunction.apply(metricNode).doubleValue();
        Labels labels = toLabels(context, metricNode);
        GaugeDataPointSnapshot dataPoint = new GaugeDataPointSnapshot(value, labels, null, metricNode.getTimestamp());
        List<GaugeDataPointSnapshot> dataPoints = gaugeDataPointSnapshotsMap.computeIfAbsent(index, k -> newLinkedList());
        dataPoints.add(dataPoint);
    }

    public Labels toLabels(String context, MetricNode metricNode) {
        Map<String, String> labels = getLabels(context, metricNode, this.commonLabels);
        return Labels.of(newArrayList(labels.keySet()), newArrayList(labels.values()));
    }

    @Override
    public List<MetricFamilyDescriptor> getMetricFamilyDescriptors() {
        return this.metricFamilyDescriptors;
    }

    private List<MetricFamilyDescriptor> buildMetricFamilyDescriptors() {
        List<MetricFamily> metricFamilies = METRIC_FAMILIES;
        int size = METRIC_FAMILY_SIZE;
        List<MetricFamilyDescriptor> metricFamilyDescriptors = newArrayList(size);
        for (int i = 0; i < size; i++) {
            MetricFamily metricFamily = metricFamilies.get(i);
            MetricFamilyDescriptor metricFamilyDescriptor = buildMetricFamilyDescriptor(metricFamily);
            metricFamilyDescriptors.add(metricFamilyDescriptor);
        }
        return metricFamilyDescriptors;
    }

    private MetricFamilyDescriptor buildMetricFamilyDescriptor(MetricFamily metricFamily) {
        Unit unit = getUnit(metricFamily);
        String name = metricFamily.getName();
        String metricName = unit == null ? sanitizeMetricName(name) : sanitizeMetricName(name, unit);
        MetricType type = valueOf(metricFamily.getType().name());
        String help = metricFamily.getHelp();

        Set<String> labelNames1 = metricFamily.getLabelNames();
        Set<String> labelNames2 = this.commonLabels.keySet();
        List<String> labelNames = newArrayList(labelNames1.size() + labelNames2.size());
        labelNames.addAll(labelNames1);
        labelNames.addAll(labelNames2);

        Builder builder = of(type, metricName)
                .help(help)
                .labelNames(labelNames);

        if (unit != null) {
            builder.unit(unit);
        }

        return builder.build();
    }

    private Unit getUnit(MetricFamily metricFamily) {
        String value = metricFamily.getUnit();
        return isNotBlank(value) ? new Unit(value) : null;
    }

}