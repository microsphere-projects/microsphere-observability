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
 * The Constants of CGroup
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see <a href="https://www.kernel.org/doc/Documentation/cgroup-v1/">https://www.kernel.org/doc/Documentation/cgroup-v1/</a>
 * @since 1.0.0
 */
public interface CGroupConstants {

    /**
     * The Prefix of CGroup : "cgroup."
     */
    String PREFIX = "cgroup.";

    /**
     * The Default Value of CGroup Directory Path: "/sys/fs/cgroup/"
     */
    String DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE = "/sys/fs/cgroup/";

    /**
     * The Property Name of CGroup Directory Path: "cgroup.dir"
     * Please do not change the default value of this property, unless it's for testing purposes.
     */
    @ConfigurationProperty(
            defaultValue = DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE,
            source = APPLICATION_SOURCE
    )
    String CGROUP_DIRECTORY_PATH_PROPERTY_NAME = PREFIX + "dir";

}