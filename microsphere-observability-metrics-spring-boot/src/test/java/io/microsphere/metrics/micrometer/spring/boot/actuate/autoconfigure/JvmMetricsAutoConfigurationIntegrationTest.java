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

package io.microsphere.metrics.micrometer.spring.boot.actuate.autoconfigure;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * {@link JvmMetricsAutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see JvmMetricsAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                JvmMetricsAutoConfigurationIntegrationTest.class,
                JvmMetricsAutoConfigurationIntegrationTest.Config.class
        },
        webEnvironment = NONE,
        properties = {
                "microsphere.metrics.micrometer.jvm.executor-service.prefix=test",
                "microsphere.metrics.micrometer.jvm.executor-service.tags=name-1,value-1,name-2,value-2,name-3,value-3"
        }
)
@EnableAutoConfiguration
class JvmMetricsAutoConfigurationIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void test() {
        assertEquals(20, this.meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .filter(name -> name.startsWith("test"))
                .count());
    }


    static class Config {

        @Bean(destroyMethod = "shutdown")
        public ExecutorService executorService() {
            return newCachedThreadPool();
        }

        @Bean
        public ConcurrentTaskExecutor concurrentTaskExecutor(ExecutorService executorService) {
            return new ConcurrentTaskExecutor(executorService);
        }
    }

}