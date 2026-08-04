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

package io.microsphere.metrics.alibaba.sentinel;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.EntranceNode;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotEntryCallback;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.microsphere.annotation.Nullable;
import io.microsphere.event.EventDispatcher;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import static com.alibaba.csp.sentinel.Constants.ROOT;
import static com.alibaba.csp.sentinel.slots.statistic.StatisticSlotCallbackRegistry.addEntryCallback;
import static io.microsphere.alibaba.sentinel.common.util.SentinelUtils.getSentinelMetricsTaskExecutor;
import static io.microsphere.collection.MapUtils.newConcurrentHashMap;
import static io.microsphere.event.EventDispatcher.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * The Event Publisher of Alibaba Sentinel's {@link Node}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ProcessorSlotEntryCallback
 * @see DefaultNode
 * @since 1.0.0
 */
public class SentinelNodeEventPublisher implements Runnable, ProcessorSlotEntryCallback<DefaultNode> {

    /**
     * The interval time of metrics collection in milliseconds.
     */
    private final long interval;

    private final ScheduledExecutorService scheduler;

    private final EventDispatcher eventDispatcher;

    /**
     * Processed the mapping between Sentinel resource name and {@link ClusterNode}
     */
    private final ConcurrentMap<String, ClusterNode> processedResourceClusterNodes = newConcurrentHashMap(256);

    public SentinelNodeEventPublisher(long interval) {
        this(interval, null);
    }

    public SentinelNodeEventPublisher(long interval, @Nullable Executor eventDispatcherExecutor) {
        this.interval = interval;
        this.scheduler = initScheduler();
        this.eventDispatcher = of(eventDispatcherExecutor);
        addEntryCallback(getClass().getName(), this);
    }

    @Override
    public void onPass(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, Object... args) {
        addNodeAsync(context, resourceWrapper, node);
    }

    @Override
    public void onBlocked(BlockException ex, Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count, Object... args) {
        addNodeAsync(context, resourceWrapper, node);
    }

    @Override
    public void run() {
        addNode(ROOT);
    }

    private ScheduledExecutorService initScheduler() {
        ScheduledExecutorService scheduledExecutorService = getSentinelMetricsTaskExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this, 0, this.interval, MILLISECONDS);
        return scheduledExecutorService;
    }

    private void addNodeAsync(Context context, ResourceWrapper resourceWrapper, DefaultNode node) {
        this.scheduler.execute(() -> {
            String contextName = context.getName();
            String resourceName = resourceWrapper.getName();
            addNode(contextName, resourceName, node);
        });
    }

    private void addNode(DefaultNode currentNode) {
        for (Node node : currentNode.getChildList()) {
            if (node instanceof DefaultNode childNode) {
                String resourceName = getResourceName(currentNode);
                String childResourceName = getResourceName(childNode);
                if (node instanceof EntranceNode) {
                    addNode(childNode);
                }
                String contextName = resourceName;
                addNode(contextName, childResourceName, childNode);
            }
        }
    }

    private String getResourceName(DefaultNode node) {
        return node.getId().getName();
    }

    private void addNode(String contextName, String resourceName, DefaultNode node) {
        if (contextName == null || resourceName == null) {
            return;
        }
        ClusterNode clusterNode = node.getClusterNode();
        ClusterNode processedClusterNode = processedResourceClusterNodes.get(resourceName);
        if (!Objects.equals(processedClusterNode, clusterNode)) {
            onClusterNodeAdded(contextName, resourceName, clusterNode);
            processedResourceClusterNodes.put(resourceName, clusterNode);
        }
    }

    protected void onClusterNodeAdded(String contextName, String resourceName, ClusterNode clusterNode) {
    }
}