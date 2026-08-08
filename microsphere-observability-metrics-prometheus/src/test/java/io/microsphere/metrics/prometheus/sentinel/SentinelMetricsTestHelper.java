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
import io.microsphere.alibaba.sentinel.common.SentinelTemplate;
import io.microsphere.alibaba.sentinel.common.reposistory.SentinelMetricsRepository;
import io.microsphere.lang.function.ThrowableAction;

import java.util.List;

import static io.microsphere.alibaba.sentinel.common.util.ProcessorSlotCallbackUtils.addEntryCallback;
import static io.microsphere.alibaba.sentinel.common.util.ProcessorSlotCallbackUtils.removeEntryCallback;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

/**
 * The Test Helper of Alibaba Sentinel Metrics
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SentinelMetricsRepository
 * @since 1.0.0
 */
public class SentinelMetricsTestHelper {

    private final int times;

    private final long waitTimeMillis;

    public SentinelMetricsTestHelper() {
        this(100, 10);
    }

    public SentinelMetricsTestHelper(int times, long waitTimeMillis) {
        this.times = times;
        this.waitTimeMillis = waitTimeMillis;
    }

    public void doInSentinelMetrics(ThrowableAction action) throws Throwable {
        SentinelMetricsRepository sentinelMetricsRepository = new SentinelMetricsRepository();
        try {
            addEntryCallback(sentinelMetricsRepository);

            long beginTimeMs = currentTimeMillis();

            String resourceName = "test-resource";
            SentinelTemplate sentinelTemplate = new SentinelTemplate();
            for (int i = 0; i < this.times; i++) {
                sentinelTemplate.call(resourceName, () -> sleep(this.waitTimeMillis));
            }

            do {
                long endTimeMs = currentTimeMillis();
                List<MetricNode> metricNodes = sentinelMetricsRepository.findMetricNodes(beginTimeMs, endTimeMs);
                if (metricNodes.isEmpty()) {
                    sleep(100);
                    continue;
                }
                break;
            } while (true);

            action.execute();
        } finally {
            removeEntryCallback(sentinelMetricsRepository.getClass());
        }
    }
}
