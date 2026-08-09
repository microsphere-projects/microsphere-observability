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
package io.microsphere.metrics.micrometer.util;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.util.BaseUtils;

import java.util.concurrent.ScheduledExecutorService;

import static io.microsphere.annotation.ConfigurationProperty.SYSTEM_PROPERTIES_SOURCE;
import static io.microsphere.concurrent.ExecutorUtils.shutdownOnExit;
import static java.lang.Integer.getInteger;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * The utilities class for Micrometer
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public abstract class MicrometerUtils extends BaseUtils {

    /**
     * The property name of scheduled executor core size: "microsphere.metrics.micrometer.scheduled-executor.core-size"
     */
    public static final String MICROETER_SCHEDULED_EXECUTOR_SIZE_PROPERTY_NAME = "microsphere.metrics.micrometer.scheduled-executor.core-size";

    /**
     * The default property value of scheduled executor core size: "1"
     */
    public static final String DEFAULT_SCHEDULED_EXECUTOR_SIZE_PROPERTY_VALUE = "1";

    /**
     * The default scheduled executor core size of Micrometer : 1
     */
    public static final int DEFAULT_SCHEDULED_EXECUTOR_SIZE = parseInt(DEFAULT_SCHEDULED_EXECUTOR_SIZE_PROPERTY_VALUE);

    /**
     * The scheduled executor core size of Micrometer
     */
    @ConfigurationProperty(
            name = MICROETER_SCHEDULED_EXECUTOR_SIZE_PROPERTY_NAME,
            defaultValue = DEFAULT_SCHEDULED_EXECUTOR_SIZE_PROPERTY_VALUE,
            source = SYSTEM_PROPERTIES_SOURCE
    )
    public static final int SCHEDULED_EXECUTOR_SIZE = getInteger(MICROETER_SCHEDULED_EXECUTOR_SIZE_PROPERTY_NAME, DEFAULT_SCHEDULED_EXECUTOR_SIZE);

    private static final ScheduledExecutorService scheduledExecutor = newScheduledThreadPool(SCHEDULED_EXECUTOR_SIZE, new NamedThreadFactory("Micrometer-Async-"));

    static {
        shutdownOnExit(scheduledExecutor);
    }

    /**
     * Execute the task asynchronously
     *
     * @param task {@link Runnable} task
     */
    public static void async(Runnable task) {
        if (task != null) {
            scheduledExecutor.execute(task);
        }
    }

    /**
     * Get the instance of {@link ScheduledExecutorService}
     *
     * @return non-null
     */
    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    private MicrometerUtils() {
    }
}
