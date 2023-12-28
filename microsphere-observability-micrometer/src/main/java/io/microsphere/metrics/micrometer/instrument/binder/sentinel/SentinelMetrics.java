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
package io.microsphere.metrics.micrometer.instrument.binder.sentinel;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.EntranceNode;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.node.metric.MetricTimerListener;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotEntryCallback;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.alibaba.csp.sentinel.Constants.ROOT;
import static com.alibaba.csp.sentinel.slots.statistic.StatisticSlotCallbackRegistry.addEntryCallback;
import static io.microsphere.reflect.FieldUtils.getStaticFieldValue;
import static io.microsphere.sentinel.util.SentinelUtils.getResourceTypeAsString;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterBinder
 * @see ProcessorSlotEntryCallback
 * @see MetricTimerListener
 * @since 1.0.0
 */
public class SentinelMetrics implements Runnable, MeterBinder, ProcessorSlotEntryCallback<DefaultNode> {

    private static final Logger logger = LoggerFactory.getLogger(SentinelMetrics.class);

    /**
     * The {@link Tag} key for Sentinel Resource
     */
    public static final String RESOURCE_TAG_KEY = "sentinel-resource";

    /**
     * The {@link Tag} key for Sentinel Context
     */
    public static final String CONTEXT_TAG_KEY = "sentinel-context";

    /**
     * The {@link Tag} key for Sentinel Type
     */
    public static final String TYPE_TAG_KEY = "sentinel-type";

    private MeterRegistry registry;

    private ScheduledExecutorService scheduler;

    /**
     * Processed the mapping between Sentinel resource name and {@link ClusterNode}
     */
    private final ConcurrentMap<String, ClusterNode> processedResourceClusterNodes = new ConcurrentHashMap<>(256);

    private final Map<String, String> contextNamesMap = new ConcurrentHashMap<>();

    @Override
    public void bindTo(MeterRegistry registry) {
        this.registry = registry;
        this.scheduler = initScheduler();
        addEntryCallback(getClass().getName(), this);
    }

    @Override
    public void onPass(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, Object... args) throws Exception {
        addMetricsAsync(context, resourceWrapper, node);
    }

    @Override
    public void onBlocked(BlockException ex, Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, Object... args) {
        addMetricsAsync(context, resourceWrapper, node);
    }

    @Override
    public void run() {
        addMetrics(ROOT);
    }

    private ScheduledExecutorService initScheduler() {
        String fieldName = "SCHEDULER";
        ScheduledExecutorService scheduledExecutorService = null;
        try {
            scheduledExecutorService = getStaticFieldValue(FlowRuleManager.class, "SCHEDULER");
        } catch (Throwable e) {
            logger.warn("The static field[name : '{}'] can't be found in the {}", fieldName, FlowRuleManager.class, e);
        }
        if (scheduledExecutorService == null) {
            scheduledExecutorService = newSingleThreadScheduledExecutor(new NamedThreadFactory("sentinel-metrics-task", true));
        }

        scheduledExecutorService.scheduleAtFixedRate(this, 0, 1, TimeUnit.MINUTES);
        return scheduledExecutorService;
    }

    private void addMetricsAsync(Context context, ResourceWrapper resourceWrapper, DefaultNode node) {
        this.scheduler.execute(() -> {
            String contextName = context.getName();
            String resourceName = resourceWrapper.getName();
            addMetrics(contextName, resourceName, node);
        });
    }

    private void addMetrics(DefaultNode currentNode) {
        for (Node node : currentNode.getChildList()) {
            if (node instanceof DefaultNode) {
                String resourceName = getResourceName(currentNode);
                DefaultNode childNode = (DefaultNode) node;
                String childResourceName = getResourceName(childNode);
                contextNamesMap.put(childResourceName, resourceName);
                if (node instanceof EntranceNode) {
                    addMetrics(childNode);
                } else {
                    String contextName = contextNamesMap.get(childResourceName);
                    addMetrics(contextName, childResourceName, childNode);
                }
            }
        }
    }

    private String getResourceName(DefaultNode node) {
        return node.getId().getName();
    }

    private void addMetrics(String contextName, String resourceName, DefaultNode node) {
        if (contextName == null || resourceName == null) {
            return;
        }
        ClusterNode clusterNode = node.getClusterNode();
        ClusterNode processedClusterNode = processedResourceClusterNodes.get(resourceName);
        if (!Objects.equals(processedClusterNode, clusterNode)) {
            addMetrics(contextName, resourceName, clusterNode, registry);
            processedResourceClusterNodes.put(resourceName, clusterNode);
        }
    }

    private void addMetrics(String contextName, String resourceName, ClusterNode clusterNode, MeterRegistry registry) {
        String metricNamePrefix = resourceName + ".";

        List<Tag> tags = buildTags(resourceName, contextName, clusterNode);

        TimeGauge.builder(metricNamePrefix + "rt", clusterNode, TimeUnit.MILLISECONDS, ClusterNode::avgRt)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "total", clusterNode::totalRequest)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "success", clusterNode::totalSuccess)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "pass", clusterNode::totalPass)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "block", clusterNode::blockRequest)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "exception", clusterNode::totalException)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "total-qps", clusterNode::totalQps)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "success-qps", clusterNode::successQps)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "max-success-qps", clusterNode::maxSuccessQps)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "pass-qps", clusterNode::passQps)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "block-qps", clusterNode::blockQps)
                .strongReference(true)
                .tags(tags)
                .register(registry);

        Gauge.builder(metricNamePrefix + "exception-qps", clusterNode::exceptionQps)
                .strongReference(true)
                .tags(tags)
                .register(registry);
    }

    private List<Tag> buildTags(String resourceName, String contextName, ClusterNode clusterNode) {
        return Arrays.asList(
                Tag.of(RESOURCE_TAG_KEY, resourceName),
                Tag.of(CONTEXT_TAG_KEY, contextName),
                Tag.of(TYPE_TAG_KEY, getResourceTypeAsString(clusterNode.getResourceType()))
        );
    }
}
