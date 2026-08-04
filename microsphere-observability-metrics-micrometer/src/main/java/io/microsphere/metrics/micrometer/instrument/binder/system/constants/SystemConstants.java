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

package io.microsphere.metrics.micrometer.instrument.binder.system.constants;

import io.microsphere.annotation.ConfigurationProperty;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;

/**
 * The Constants of System
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface SystemConstants {

    /**
     * The Prefix of System : "system."
     */
    String PREFIX = "system.";

    /**
     * The Prefix of CGroup : "system.cgroup."
     */
    String CGROUP_PREFIX = PREFIX + "cgroup.";

    /**
     * The Default Value of CGroup Directory Path: "/sys/fs/cgroup/"
     */
    String DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE = "/sys/fs/cgroup/";

    /**
     * The Property Name of CGroup Directory Path: "system.cgroup.dir"
     * Please do not change the default value of this property, unless it's for testing purposes.
     *
     * @see <a href="https://www.kernel.org/doc/Documentation/cgroup-v1/">https://www.kernel.org/doc/Documentation/cgroup-v1/</a>
     */
    @ConfigurationProperty(
            defaultValue = DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE,
            source = APPLICATION_SOURCE
    )
    String CGROUP_DIRECTORY_PATH_PROPERTY_NAME = CGROUP_PREFIX + "dir";

    /**
     * The Default Network Stats File Path: "/proc/net/dev"
     */
    String DEFAULT_NETWORK_STATS_FILE_PATH = "/proc/net/dev";

    /**
     * The Prefix of Network : "system.network."
     */
    String NETWORK_PREFIX = PREFIX + "network.";

    /**
     * The Property Name of Network Stats File Path: "system.network.stats.file"
     * Please do not change the default value of this property, unless it's for testing purposes.
     */
    @ConfigurationProperty(
            defaultValue = DEFAULT_NETWORK_STATS_FILE_PATH,
            source = APPLICATION_SOURCE
    )
    String NETWORK_STATS_FILE_PATH_PROPERTY_NAME = NETWORK_PREFIX + "stats.file";

    /**
     * The Default Property Value of metrics collection interval(unit : millisecond) : "60000"
     */
    String DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE = "60000";

    /**
     * The Property Name of metrics collection interval(unit : millisecond) : "system.metrics.micrometer.collection.interval"
     */
    @ConfigurationProperty(
            type = long.class,
            defaultValue = DEFAULT_METRICS_COLLECTION_INTERVAL_PROPERTY_VALUE,
            source = APPLICATION_SOURCE
    )
    String METRICS_COLLECTION_INTERVAL_PROPERTY_NAME = PREFIX + "metrics.micrometer.collection.interval";

}