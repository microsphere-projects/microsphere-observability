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
package io.microsphere.metrics.micrometer.prometheus.client.sentinel;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.EntranceNode;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.node.metric.MetricSearcher;
import com.alibaba.csp.sentinel.node.metric.MetricTimerListener;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotEntryCallback;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusNamingConvention;
import io.microsphere.metrics.micrometer.instrument.binder.sentinel.SentinelMetrics;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.alibaba.csp.sentinel.Constants.CONTEXT_DEFAULT_NAME;
import static com.alibaba.csp.sentinel.Constants.ROOT;
import static com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION;
import static com.alibaba.csp.sentinel.config.SentinelConfig.getAppName;
import static com.alibaba.csp.sentinel.node.metric.MetricWriter.METRIC_BASE_DIR;
import static com.alibaba.csp.sentinel.node.metric.MetricWriter.formMetricFileName;
import static com.alibaba.csp.sentinel.slots.statistic.StatisticSlotCallbackRegistry.addEntryCallback;
import static com.alibaba.csp.sentinel.util.PidUtil.getPid;
import static io.microsphere.collection.CollectionUtils.isEmpty;
import static io.microsphere.sentinel.util.SentinelUtils.getResourceTypeAsString;
import static io.microsphere.sentinel.util.SentinelUtils.getSentinelMetricsTaskExecutor;
import static io.microsphere.util.ClassUtils.getSimpleName;
import static io.prometheus.client.Collector.Type.GAUGE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Prometheus {@link Collector} based on Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Collector
 * @see ClusterBuilderSlot
 * @see ClusterNode
 * @see MetricTimerListener
 * @see SentinelMetrics
 * @since 1.0.0
 */
public class SentinelCollector extends Collector implements ProcessorSlotEntryCallback<DefaultNode> {

    private static final Logger logger = LoggerFactory.getLogger(SentinelCollector.class);


    /**
     * The Metric prefix : "sentinel_"
     */
    public static final String METRIC_PREFIX = "sentinel_";

    /**
     * The Label name of the metrics origin : "origin"
     */
    public static final String ORIGIN_LABEL_NAME = "origin";

    /**
     * The Label name for Sentinel Resource
     */
    public static final String RESOURCE_LABEL_NAME = METRIC_PREFIX + "resource";

    /**
     * The Label name for Sentinel Context
     */
    public static final String CONTEXT_LABEL_NAME = METRIC_PREFIX + "context";

    /**
     * The Label name for Sentinel Resource Type
     */
    public static final String TYPE_LABEL_NAME = METRIC_PREFIX + "resource_type";

    /**
     * The Label name for Sentinel Version
     */
    public static final String VERSION_LABEL_NAME = METRIC_PREFIX + "version";

    /**
     * The interval time of metrics collection in milliseconds.
     */
    private final long interval;

    /**
     * The max records of metrics collection
     */
    private final int maxRecords;

    private final List<String> commonLabelNames;

    private final List<String> commonLabelValues;

    private final NamingConvention namingConvention = new PrometheusNamingConvention();

    private final ConcurrentMap<String, String> resourceToContextMapping = new ConcurrentHashMap<>();

    private volatile MetricSearcher metricSearcher;

    private ScheduledExecutorService scheduler;

    public SentinelCollector(long interval, int maxRecords) {
        this(interval, maxRecords, emptyMap());
    }

    public SentinelCollector(long interval, int maxRecords, Map<String, String> commonLabels) {
        this.interval = interval;
        this.maxRecords = maxRecords;
        this.commonLabelNames = initCommonLabelNames(commonLabels);
        this.commonLabelValues = initCommonLabelValues(commonLabels);
    }

    @Nonnull
    private MetricSearcher getMetricSearcher() {
        MetricSearcher metricSearcher = this.metricSearcher;
        if (metricSearcher == null) {
            metricSearcher = newMetricSearcher();
            this.metricSearcher = metricSearcher;
        }
        return metricSearcher;
    }

    @Override
    public <T extends Collector> T register(CollectorRegistry registry) {
        this.scheduler = initScheduler();
        addEntryCallback(getClass().getName(), this);
        return super.register(registry);
    }

    @Override
    public void onPass(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, Object... args) throws Exception {
        async(() -> updateResourceToContextMapping(context, node));
    }

    @Override
    public void onBlocked(BlockException ex, Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, Object... args) {
        async(() -> updateResourceToContextMapping(context, node));
    }

    private void async(Runnable runnable) {
        if (scheduler != null) {
            scheduler.execute(runnable);
        }
    }

    private ScheduledExecutorService initScheduler() {
        ScheduledExecutorService scheduledExecutorService = getSentinelMetricsTaskExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this::update, 0, interval, TimeUnit.MILLISECONDS);
        return scheduledExecutorService;
    }

    private void update() {
        updateResourceToContextMapping();
    }

    private void updateResourceToContextMapping() {
        updateResourceToContextMapping(ROOT);
    }

    private void updateResourceToContextMapping(DefaultNode currentNode) {
        if (currentNode instanceof EntranceNode) {
            String context = getResource(currentNode);
            for (Node node : currentNode.getChildList()) {
                if (node instanceof DefaultNode) {
                    DefaultNode childNode = (DefaultNode) node;
                    updateResourceToContextMapping(context, childNode);
                    if (node instanceof EntranceNode) {
                        updateResourceToContextMapping(childNode);
                    }
                }
            }
        }
    }

    private void updateResourceToContextMapping(Context context, DefaultNode node) {
        String resource = getResource(node);
        resourceToContextMapping.put(resource, context.getName());
    }

    private void updateResourceToContextMapping(String context, DefaultNode node) {
        String resource = getResource(node);
        resourceToContextMapping.put(resource, context);
    }

    private String getResource(DefaultNode node) {
        return node.getId().getName();
    }

    private String getContext(String resource) {
        return resourceToContextMapping.getOrDefault(resource, CONTEXT_DEFAULT_NAME);
    }

    private List<String> initCommonLabelNames(Map<String, String> commonLabels) {
        List<String> labelNames = new ArrayList<>(commonLabels.size() + 2);
        labelNames.add(ORIGIN_LABEL_NAME);
        labelNames.addAll(commonLabels.keySet());
        return labelNames;
    }

    private List<String> initCommonLabelValues(Map<String, String> commonLabels) {
        List<String> labelValues = new ArrayList<>(commonLabels.size() + 2);
        labelValues.add(getSimpleName(this.getClass()));
        labelValues.addAll(commonLabels.values());
        return labelValues;
    }


    private MetricSearcher newMetricSearcher() {
        String appName = getAppName();
        int pid = getPid();
        return new MetricSearcher(METRIC_BASE_DIR, formMetricFileName(appName, pid));
    }

    /**
     * Configure the common label
     *
     * @param labelName  the label name
     * @param labelValue the label value
     * @return {@link SentinelCollector}
     */
    public SentinelCollector commonLabel(String labelName, String labelValue) {
        this.commonLabelNames.add(labelName);
        this.commonLabelValues.add(labelValue);
        return this;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        Map<String, List<MetricNode>> resourceMetricsNodesMap = getResourceMetricNodeMaps();
        if (resourceMetricsNodesMap.isEmpty()) {
            return emptyList();
        }
        List<String> labelNames = buildLabelNames();
        List<MetricFamilySamples> metricFamilySamplesList = new ArrayList<>(resourceMetricsNodesMap.size());
        for (Map.Entry<String, List<MetricNode>> entry : resourceMetricsNodesMap.entrySet()) {
            String resource = entry.getKey();
            List<MetricNode> metricNodes = entry.getValue();
            int size = metricNodes.size();
            if (size > 0) {
                String metric = buildMetric(resource);
                List<String> labelValues = buildLabelValues(resource, metricNodes.get(0));

                List<MetricFamilySamples.Sample> samples = new ArrayList<>(size * 7);
                for (int i = 0; i < size; i++) {
                    MetricNode metricNode = metricNodes.get(i);
                    samples.add(createSample(metric, "rt", labelNames, labelValues, metricNode, MetricNode::getRt));
                    samples.add(createSample(metric, "concurrency", labelNames, labelValues, metricNode, MetricNode::getConcurrency));
                    samples.add(createSample(metric, "success_qps", labelNames, labelValues, metricNode, MetricNode::getSuccessQps));
                    samples.add(createSample(metric, "pass_qps", labelNames, labelValues, metricNode, MetricNode::getPassQps));
                    samples.add(createSample(metric, "occupied_pass_qps", labelNames, labelValues, metricNode, MetricNode::getOccupiedPassQps));
                    samples.add(createSample(metric, "block_qps", labelNames, labelValues, metricNode, MetricNode::getBlockQps));
                    samples.add(createSample(metric, "exception_qps", labelNames, labelValues, metricNode, MetricNode::getExceptionQps));
                }

                metricFamilySamplesList.add(new MetricFamilySamples(metric, GAUGE, "Sentinel Resource : " + resource, samples));
            }
        }
        return metricFamilySamplesList;
    }

    private List<String> buildLabelNames() {
        List<String> commonLabelNames = this.commonLabelNames;
        List<String> labelNames = new ArrayList<>(commonLabelNames.size() + 4);
        labelNames.addAll(commonLabelNames);
        labelNames.add(RESOURCE_LABEL_NAME);
        labelNames.add(CONTEXT_LABEL_NAME);
        labelNames.add(TYPE_LABEL_NAME);
        labelNames.add(VERSION_LABEL_NAME);
        return labelNames;
    }

    private List<String> buildLabelValues(String resource, MetricNode metricNode) {
        List<String> labelValues = new ArrayList<>(commonLabelValues.size() + 2);
        List<String> commonLabelValues = this.commonLabelValues;
        String context = getContext(resource);
        String resourceType = getResourceTypeAsString(metricNode.getClassification());
        labelValues.addAll(commonLabelValues);
        labelValues.add(resource);
        labelValues.add(context);
        labelValues.add(resourceType);
        labelValues.add(SENTINEL_VERSION);
        return labelValues;
    }

    private String buildMetric(String resource) {
        String name = METRIC_PREFIX + resource;
        return namingConvention.name(name, Meter.Type.GAUGE);
    }

    private Map<String, List<MetricNode>> getResourceMetricNodeMaps() {

        MetricSearcher metricSearcher = this.getMetricSearcher();

        long beginTimeMs = System.currentTimeMillis() - interval;
        List<MetricNode> metricNodes = null;
        try {
            metricNodes = metricSearcher.find(beginTimeMs, maxRecords);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("MetricSearcher can't find any MetricNode at {}", beginTimeMs, e);
            }
        }

        if (isEmpty(metricNodes)) {
            return emptyMap();
        }

        Map<String, List<MetricNode>> resourceMetricsNodesMap = new TreeMap<>();

        for (MetricNode metricNode : metricNodes) {
            String resource = metricNode.getResource();
            List<MetricNode> resourceMetricNodes = resourceMetricsNodesMap.computeIfAbsent(resource, r -> new LinkedList<>());
            resourceMetricNodes.add(metricNode);
        }

        return resourceMetricsNodesMap;
    }

    private MetricFamilySamples.Sample createSample(String metric,
                                                    String childName,
                                                    List<String> labelNames, List<String> labelValues,
                                                    MetricNode metricNode,
                                                    Function<MetricNode, Number> metricValueFunction) {
        String name = metric + "_" + childName;
        Number value = metricValueFunction.apply(metricNode);
        Long timestampMs = metricNode.getTimestamp();
        return new MetricFamilySamples.Sample(name, labelNames, labelValues, value.doubleValue(), timestampMs);
    }
}