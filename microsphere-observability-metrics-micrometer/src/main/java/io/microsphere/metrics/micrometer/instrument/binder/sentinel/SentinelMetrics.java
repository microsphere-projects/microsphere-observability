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

import static com.alibaba.csp.sentinel.Constants.SENTINEL_VERSION;
import static io.micrometer.core.instrument.Tags.concat;
import static io.micrometer.core.instrument.TimeGauge.builder;
import static io.microsphere.alibaba.sentinel.common.util.SentinelUtils.getResourceTypeAsString;
import static io.microsphere.alibaba.sentinel.event.SentinelNodeEventPublisher.getSentinelNodeEventPublisher;
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
    public static final String RESOURCE_TAG_KEY = RESOURCE_LABEL_NAME;

    /**
     * The {@link Tag} key for Sentinel Context
     */
    public static final String CONTEXT_TAG_KEY = CONTEXT_LABEL_NAME;

    /**
     * The {@link Tag} key for Sentinel Resource Type
     */
    public static final String TYPE_TAG_KEY = RESOURCE_TYPE_LABEL_NAME;

    /**
     * The {@link Tag} key for Sentinel Version
     */
    public static final String VERSION_TAG_KEY = VERSION_LABEL_NAME;

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

    private void addMetrics(String contextName, String resourceName, ClusterNode clusterNode, MeterRegistry registry) {

        Iterable<Tag> tags = buildTags(resourceName, contextName, clusterNode);

        builder(RT_METRIC_NAME, clusterNode, MILLISECONDS, ClusterNode::avgRt)
                .tags(tags)
                .register(registry);

        Gauge.builder(CONCURRENCY_METRIC_NAME, clusterNode::totalRequest)
                .tags(tags)
                .register(registry);

        Gauge.builder(SUCCESS_QPS_METRIC_NAME, clusterNode::successQps)
                .tags(tags)
                .register(registry);

        Gauge.builder(PASS_QPS_METRIC_NAME, clusterNode::passQps)
                .tags(tags)
                .register(registry);

        Gauge.builder(OCCUPIED_PASS_QPS_METRIC_NAME, clusterNode::occupiedPassQps)
                .tags(tags)
                .register(registry);

        Gauge.builder(BLOCK_QPS_METRIC_NAME, clusterNode::blockQps)
                .tags(tags)
                .register(registry);

        Gauge.builder(EXCEPTION_QPS_METRIC_NAME, clusterNode::exceptionQps)
                .tags(tags)
                .register(registry);
    }

    private Iterable<Tag> buildTags(String resourceName, String contextName, ClusterNode clusterNode) {
        return combine(
                RESOURCE_TAG_KEY, resourceName,
                CONTEXT_TAG_KEY, contextName,
                TYPE_TAG_KEY, getResourceTypeAsString(clusterNode.getResourceType())
        );
    }
}