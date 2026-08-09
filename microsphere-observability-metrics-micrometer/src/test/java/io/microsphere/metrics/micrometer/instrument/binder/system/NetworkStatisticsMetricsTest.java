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
package io.microsphere.metrics.micrometer.instrument.binder.system;

import io.microsphere.io.StandardFileWatchService;
import io.microsphere.io.event.FileChangedEvent;
import io.microsphere.io.event.FileChangedListener;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMetricsTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static io.microsphere.io.event.FileChangedEvent.Kind.MODIFIED;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.NETWORK_STATS_FILE_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.util.SystemUtils.getNetworkStatsFilePath;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.setProperty;
import static java.nio.file.Paths.get;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link NetworkStatisticsMetrics} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
class NetworkStatisticsMetricsTest extends AbstractMetricsTest<NetworkStatisticsMetrics> {

    @BeforeAll
    static void prepare() throws Throwable {
        ClassLoader classLoader = NetworkStatisticsMetricsTest.class.getClassLoader();
        String testFile = get(classLoader.getResource("test-data/memory/network.stats").toURI()).toAbsolutePath().toString();
        setProperty(NETWORK_STATS_FILE_PATH_PROPERTY_NAME, testFile);
    }

    @Test
    void test() throws Throwable {
        assertFalse(registry.getMeters().isEmpty());
        File statsFile = new File(getNetworkStatsFilePath());

        StandardFileWatchService fileWatchService = new StandardFileWatchService();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        fileWatchService.watch(statsFile, new FileChangedListener() {
            @Override
            public void onFileModified(FileChangedEvent event) {
                assertFalse(registry.getMeters().isEmpty());
                countDownLatch.countDown();
            }
        }, MODIFIED);

        fileWatchService.start();

        statsFile.setLastModified(currentTimeMillis());
        countDownLatch.await();
    }
}
