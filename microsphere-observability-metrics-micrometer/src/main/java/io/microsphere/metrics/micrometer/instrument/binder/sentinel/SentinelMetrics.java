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

import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.metric.MetricTimerListener;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotEntryCallback;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.microsphere.alibaba.sentinel.event.ClusterNodeAddedEvent;
import io.microsphere.alibaba.sentinel.event.ClusterNodeAddedEventListener;
import io.microsphere.alibaba.sentinel.event.SentinelNodeEventPublisher;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;

import java.util.Collection;

import static com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION;
import static com.alibaba.csp.sentinel.slots.statistic.StatisticSlotCallbackRegistry.getEntryCallbacks;
import static io.micrometer.core.instrument.Tags.concat;
import static io.micrometer.core.instrument.TimeGauge.builder;
import static io.microsphere.alibaba.sentinel.common.util.SentinelUtils.getResourceTypeAsString;
import static io.microsphere.constants.SymbolConstants.DOT;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see MeterBinder
 * @see ProcessorSlotEntryCallback
 * @see MetricTimerListener
 * @since 1.0.0
 */
public class SentinelMetrics extends AbstractMeterBinder implements ClusterNodeAddedEventListener {

    /**
     * The Metric prefix : "sentinel."
     */
    public static final String METRIC_PREFIX = "sentinel.";

    /**
     * The {@link Tag} key for Sentinel Resource
     */
    public static final String RESOURCE_TAG_KEY = METRIC_PREFIX + "resource";

    /**
     * The {@link Tag} key for Sentinel Context
     */
    public static final String CONTEXT_TAG_KEY = METRIC_PREFIX + "context";

    /**
     * The {@link Tag} key for Sentinel Resource Type
     */
    public static final String TYPE_TAG_KEY = METRIC_PREFIX + "resource-type";

    /**
     * The {@link Tag} key for Sentinel Version
     */
    public static final String VERSION_TAG_KEY = METRIC_PREFIX + "version";

    MeterRegistry registry;

    public SentinelMetrics() {
        this(emptyList());
    }

    public SentinelMetrics(Iterable<Tag> tags) {
        super(concat(tags, VERSION_TAG_KEY, SENTINEL_VERSION));
    }

    @Override
    protected boolean supports(MeterRegistry registry) {
        return getSentinelNodeEventPublisher() != null;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        this.registry = registry;
        SentinelNodeEventPublisher sentinelNodeEventPublisher = getSentinelNodeEventPublisher();
        sentinelNodeEventPublisher.addEventListener(this);
    }

    @Override
    public void onEvent(ClusterNodeAddedEvent event) {
        String contextName = event.getContextName();
        String resourceName = event.getResourceName();
        ClusterNode clusterNode = event.getClusterNode();
        addMetrics(contextName, resourceName, clusterNode, this.registry);
    }

    private SentinelNodeEventPublisher getSentinelNodeEventPublisher() {
        Collection<ProcessorSlotEntryCallback<DefaultNode>> entryCallbacks = getEntryCallbacks();
        for (ProcessorSlotEntryCallback<DefaultNode> callback : entryCallbacks) {
            if (callback instanceof SentinelNodeEventPublisher) {
                return (SentinelNodeEventPublisher) callback;
            }
        }
        return null;
    }

    private void addMetrics(String contextName, String resourceName, ClusterNode clusterNode, MeterRegistry registry) {
        String metricNamePrefix = METRIC_PREFIX + resourceName + DOT;

        Iterable<Tag> tags = buildTags(resourceName, contextName, clusterNode);

        builder(metricNamePrefix + "rt", clusterNode, MILLISECONDS, ClusterNode::avgRt)
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

    private Iterable<Tag> buildTags(String resourceName, String contextName, ClusterNode clusterNode) {
        return combine(RESOURCE_TAG_KEY, resourceName, CONTEXT_TAG_KEY, contextName, TYPE_TAG_KEY,
                getResourceTypeAsString(clusterNode.getResourceType()));
    }
}