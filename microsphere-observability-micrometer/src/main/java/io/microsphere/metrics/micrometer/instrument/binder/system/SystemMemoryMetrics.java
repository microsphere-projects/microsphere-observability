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

import com.sun.management.OperatingSystemMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.microsphere.metrics.micrometer.instrument.binder.AbstractMeterBinder;

import java.lang.management.PlatformManagedObject;

import static io.micrometer.core.instrument.Gauge.builder;
import static io.microsphere.lang.function.ThrowableSupplier.execute;
import static io.microsphere.management.JmxUtils.getOperatingSystemMXBean;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.PREFIX;
import static io.microsphere.util.ClassLoaderUtils.resolveClass;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.management.ManagementFactory.getPlatformMXBean;
import static java.util.Collections.emptyList;

/**
 * System Memory Metrics based on JMX
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class SystemMemoryMetrics extends AbstractMeterBinder {

    public static final String METRIC_PREFIX = PREFIX;

    public static final String OPERATING_SYSTEM_MXBEAN_CLASS_NAME = "com.sun.management.OperatingSystemMXBean";

    private static final Class<?> OPERATING_SYSTEM_MXBEAN_CLASS = resolveClass(OPERATING_SYSTEM_MXBEAN_CLASS_NAME, getSystemClassLoader());

    public static final boolean SUPPORTED = isSupported();

    public static boolean isSupported() {
        return execute(() -> getPlatformMXBean((Class<PlatformManagedObject>) OPERATING_SYSTEM_MXBEAN_CLASS) != null, e -> false);
    }

    public SystemMemoryMetrics() {
        this(emptyList());
    }

    public SystemMemoryMetrics(Iterable<Tag> tags) {
        super(tags);
    }

    @Override
    protected boolean supports(MeterRegistry registry) {
        return SUPPORTED;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {

        OperatingSystemMXBean operatingSystemBean = (OperatingSystemMXBean) getOperatingSystemMXBean();

        builder(METRIC_PREFIX + "memory.swap.space.total", operatingSystemBean, OperatingSystemMXBean::getTotalSwapSpaceSize)
                .tags(tags)
                .description("Total swap space size")
                .baseUnit(BaseUnits.BYTES)
                .strongReference(true)
                .register(registry);

        builder(METRIC_PREFIX + "memory.swap.space.free", operatingSystemBean, OperatingSystemMXBean::getFreeSwapSpaceSize)
                .tags(tags)
                .description("Free swap space size")
                .baseUnit(BaseUnits.BYTES)
                .strongReference(true)
                .register(registry);

        builder(METRIC_PREFIX + "memory.committed.virtual", operatingSystemBean, OperatingSystemMXBean::getCommittedVirtualMemorySize)
                .tags(tags)
                .description("Committed virtual memory size")
                .baseUnit(BaseUnits.BYTES)
                .strongReference(true)
                .register(registry);

        builder(METRIC_PREFIX + "memory.physical.total", operatingSystemBean, OperatingSystemMXBean::getTotalPhysicalMemorySize)
                .tags(tags)
                .description("Total physical memory size")
                .baseUnit(BaseUnits.BYTES)
                .strongReference(true)
                .register(registry);

        builder(METRIC_PREFIX + "memory.physical.free", operatingSystemBean, OperatingSystemMXBean::getFreePhysicalMemorySize)
                .tags(tags)
                .description("Free physical memory size")
                .baseUnit(BaseUnits.BYTES)
                .strongReference(true)
                .register(registry);
    }
}
