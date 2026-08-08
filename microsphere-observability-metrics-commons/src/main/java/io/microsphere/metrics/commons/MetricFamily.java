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

package io.microsphere.metrics.commons;

import java.util.Set;

/**
 * The model of Metric Family
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor
 * @see io.prometheus.client.Collector.MetricFamilySamples
 * @since 1.0.0
 */
public class MetricFamily {

    private final String name;

    private final String unit;

    private final MetricType type;

    private final String help;

    private final Set<String> labelNames;

    MetricFamily(String name, String unit, MetricType type, String help, Set<String> labelNames) {
        this.name = name;
        this.unit = unit;
        this.type = type;
        this.help = help;
        this.labelNames = labelNames;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public MetricType getType() {
        return type;
    }

    public String getHelp() {
        return help;
    }

    public Set<String> getLabelNames() {
        return labelNames;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;
        private String unit;
        private MetricType type;
        private String help;
        private Set<String> labelNames;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder type(MetricType type) {
            this.type = type;
            return this;
        }

        public Builder help(String help) {
            this.help = help;
            return this;
        }

        public Builder labelNames(Set<String> labelNames) {
            this.labelNames = labelNames;
            return this;
        }

        public MetricFamily build() {
            return new MetricFamily(name, unit, type, help, labelNames);
        }
    }
}