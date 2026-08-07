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

package io.microsphere.metrics.prometheus.sentinel.util;

import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.metrics.model.snapshots.MetricFamilyDescriptor;

import java.util.Map;
import java.util.Set;

import static io.microsphere.collection.SetUtils.newLinkedHashSet;


/**
 * The Assembler of the Prometheus Metrics Labels
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see MetricFamilyDescriptor
 * @see Sample
 * @since 1.0.0
 */
public class LabelAssembler {

    private final Set<String> commonLabelNames;

    private final Set<String> commonLabelValues;

    public LabelAssembler(Map<String, String> commonLabels) {
        this.commonLabelNames = initCommonLabelNames(commonLabels);
        this.commonLabelValues = initCommonLabelValues(commonLabels);
    }

    /**
     * Configure the common label
     *
     * @param labelName  the label name
     * @param labelValue the label value
     * @return {@link LabelAssembler}
     */
    public LabelAssembler commonLabel(String labelName, String labelValue) {
        this.commonLabelNames.add(labelName);
        this.commonLabelValues.add(labelValue);
        return this;
    }

    private Set<String> initCommonLabelNames(Map<String, String> commonLabels) {
        return newLinkedHashSet(commonLabels.keySet());
    }

    private Set<String> initCommonLabelValues(Map<String, String> commonLabels) {
        return newLinkedHashSet(commonLabels.values());
    }
}
