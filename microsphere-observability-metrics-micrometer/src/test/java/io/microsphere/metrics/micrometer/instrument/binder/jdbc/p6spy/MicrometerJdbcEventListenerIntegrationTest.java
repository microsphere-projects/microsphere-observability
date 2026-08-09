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

package io.microsphere.metrics.micrometer.instrument.binder.jdbc.p6spy;

import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.DefaultJdbcEventListenerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.microsphere.alibaba.druid.test.AbstractAlibabaDruidTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static io.microsphere.metrics.micrometer.instrument.binder.jdbc.p6spy.MicrometerJdbcEventListener.DEFAULT_SLOW_SQL_TIME_THRESHOLD;
import static io.microsphere.reflect.FieldUtils.getStaticFieldValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link MicrometerJdbcEventListener} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see MicrometerJdbcEventListener
 * @since 1.0.0
 */
class MicrometerJdbcEventListenerIntegrationTest extends AbstractAlibabaDruidTest {

    private MicrometerJdbcEventListener micrometerJdbcEventListener;

    @BeforeEach
    public void init() throws Throwable {
        super.init();
        micrometerJdbcEventListener = new MicrometerJdbcEventListener();
        CompoundJdbcEventListener compoundJdbcEventListener = getStaticFieldValue(true, DefaultJdbcEventListenerFactory.class, "jdbcEventListener");
        List<JdbcEventListener> eventListeners = compoundJdbcEventListener.getEventListeners();
        this.micrometerJdbcEventListener = (MicrometerJdbcEventListener) eventListeners.get(eventListeners.size() - 1);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        this.micrometerJdbcEventListener.setRegistry(meterRegistry);
    }

    @Test
    @Override
    public void test() throws Throwable {
        this.micrometerJdbcEventListener.setSlowSQLThresholdNanos(1);
        long slowSQLThresholdNanos = this.micrometerJdbcEventListener.getSlowSQLThresholdNanos();
        assertEquals(1L, slowSQLThresholdNanos);

        super.test();

        // recover to default
        this.micrometerJdbcEventListener.setSlowSQLThresholdNanos(DEFAULT_SLOW_SQL_TIME_THRESHOLD);

        // Error Case
        assertThrows(SQLException.class, () -> executeStatement(statement -> {
            statement.executeUpdate("INSERT INTO users");
        }));

        // Special Case
        assertDoesNotThrow(() -> executeStatement(statement -> {
            statement.execute("HELP");
        }));
    }
}