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

package io.microsphere.metrics.prometheus.micrometer;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Collector.Type;

import java.util.List;

import static io.micrometer.core.instrument.Meter.Type.COUNTER;
import static io.micrometer.core.instrument.Meter.Type.DISTRIBUTION_SUMMARY;
import static io.micrometer.core.instrument.Meter.Type.GAUGE;
import static io.micrometer.core.instrument.Meter.Type.OTHER;
import static io.micrometer.core.instrument.Meter.Type.TIMER;
import static io.micrometer.core.instrument.Meter.builder;
import static io.micrometer.core.instrument.Statistic.VALUE;
import static io.micrometer.core.instrument.Statistic.values;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.Lists.ofList;

/**
 * The {@link MeterBinder} adpater class based on the Prometheus {@link Collector}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see io.prometheus.client.Collector
 * @see MeterBinder
 * @since 1.0.0
 */
public class PrometheusCollectorMeterBinder implements MeterBinder {

    private final Collector collector;

    public PrometheusCollectorMeterBinder(Collector collector) {
        this.collector = collector;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        List<MetricFamilySamples> metricFamilySamples = collector.collect();
        bindTo(metricFamilySamples, registry);
    }

    void bindTo(List<MetricFamilySamples> metricFamilySamples, MeterRegistry registry) {
        for (MetricFamilySamples samples : metricFamilySamples) {
            bindTo(samples, registry);
        }
    }

    void bindTo(MetricFamilySamples samples, MeterRegistry registry) {
        for (Sample sample : samples.samples) {
            bindTo(sample, samples, registry);
        }
    }

    void bindTo(Sample sample, MetricFamilySamples samples, MeterRegistry registry) {
        builder(sample.name, toType(samples.type), toMeasurements(sample, samples))
                .description(samples.help)
                .tags(toTags(sample))
                .baseUnit(samples.unit)
                .register(registry);
    }

    static Meter.Type toType(Type type) {
        switch (type) {
            case COUNTER:
                return COUNTER;
            case GAUGE:
                return GAUGE;
            case SUMMARY:
                return DISTRIBUTION_SUMMARY;
            case HISTOGRAM:
                return TIMER;
            default:
                return OTHER;
        }
    }

    private Iterable<Measurement> toMeasurements(Sample sample, MetricFamilySamples samples) {
        Measurement measurement = new Measurement(() -> sample.value, toStatistic(samples));
        return ofList(measurement);
    }

    private Iterable<Tag> toTags(Sample sample) {
        List<String> labelNames = sample.labelNames;
        int size = labelNames.size();
        List<Tag> tags = newArrayList(labelNames.size());
        for (int i = 0; i < size; i++) {
            String labelName = labelNames.get(i);
            String labelValue = sample.labelValues.get(i);
            Tag tag = Tag.of(labelName, labelValue);
            tags.add(tag);
        }
        return tags;
    }

    private Statistic toStatistic(MetricFamilySamples samples) {
        return toStatistic(samples.name);
    }

    static Statistic toStatistic(String metricName) {
        for (Statistic statistic : values()) {
            if (metricName.endsWith(statistic.getTagValueRepresentation())) {
                return statistic;
            }
        }
        return VALUE;
    }
}