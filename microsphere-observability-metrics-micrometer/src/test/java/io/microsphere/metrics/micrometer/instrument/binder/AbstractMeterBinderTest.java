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

package io.microsphere.metrics.micrometer.instrument.binder;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

/**
 * {@link AbstractMeterBinder} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see AbstractMeterBinder
 * @since 1.0.0
 */
class AbstractMeterBinderTest extends AbstractMeterBinder {

    @Override
    protected boolean supports(MeterRegistry registry) {
        return true;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) throws Throwable {
        throw new Throwable("For testing");
    }

    @Test
    void testBindTo() {
        MeterRegistry registry = new SimpleMeterRegistry();
        bindTo(registry);
    }
}