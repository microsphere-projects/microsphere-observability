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

import com.p6spy.engine.common.Loggable;
import com.p6spy.engine.common.P6LogQuery;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.Category;
import com.p6spy.engine.logging.LoggingEventListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.microsphere.annotation.ConfigurationProperty;
import io.microsphere.annotation.Nullable;

import java.sql.SQLException;

import static io.micrometer.core.instrument.Counter.builder;
import static io.micrometer.core.instrument.Gauge.builder;
import static io.microsphere.annotation.ConfigurationProperty.SYSTEM_PROPERTIES_SOURCE;
import static io.microsphere.metrics.micrometer.util.MicrometerUtils.async;
import static java.lang.Character.isWhitespace;
import static java.lang.Long.getLong;
import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Micrometer {@link JdbcEventListener}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class MicrometerJdbcEventListener extends LoggingEventListener {

    private static final String PREFIX = "microsphere.jdbc.";

    public static final String SQL_SUCCESS_COUNT_METRIC_NAME = PREFIX + "sql.success.count";

    public static final String SQL_FAILURE_COUNT_METRIC_NAME = PREFIX + "sql.failure.count";

    public static final String SLOW_SQL_TIME_METRIC_NAME = PREFIX + "slow-sql.time";

    /**
     * The default value of slow SQL time threshold in seconds : 1 seconds
     */
    public static final String DEFAULT_SLOW_SQL_TIME_THRESHOLD_PROPERTY_VALUE = "1";

    /**
     * The property name of slow SQL time threshold in seconds : "microsphere.jdbc.slow-sql.time.threshold",
     * default value : 1 seconds
     */
    @ConfigurationProperty(
            type = long.class,
            defaultValue = DEFAULT_SLOW_SQL_TIME_THRESHOLD_PROPERTY_VALUE,
            source = SYSTEM_PROPERTIES_SOURCE
    )
    public static final String SLOW_SQL_TIME_THRESHOLD_PROPERTY_NAME = PREFIX + "slow-sql.time.threshold";

    /**
     * The default value of slow SQL time threshold in nanos : 1 seconds
     */
    public static final long DEFAULT_SLOW_SQL_TIME_THRESHOLD = SECONDS.toNanos(parseLong(DEFAULT_SLOW_SQL_TIME_THRESHOLD_PROPERTY_VALUE));

    /**
     * {@link MeterRegistry}
     */
    private static MeterRegistry registry;

    public MicrometerJdbcEventListener(MeterRegistry registry) {
        MicrometerJdbcEventListener.registry = registry;
    }

    public MicrometerJdbcEventListener() {
        this(null);
    }

    /**
     * Slow SQL Threshold in nanos, default value : 1 seconds
     */
    private long slowSQLThresholdNanos = getLong(SLOW_SQL_TIME_THRESHOLD_PROPERTY_NAME, DEFAULT_SLOW_SQL_TIME_THRESHOLD);

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        execute(() -> addMetrics(statementInformation, timeElapsedNanos, 0, e));
    }

    @Override
    public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos, int[] updateCounts, SQLException e) {
        execute(() -> addMetrics(statementInformation, timeElapsedNanos, updateCounts.length, e));
    }

    @Override
    protected void logElapsed(Loggable loggable, long timeElapsedNanos, Category category, SQLException e) {
        execute(() -> P6LogQuery.logElapsed(loggable.getConnectionInformation().getConnectionId(), timeElapsedNanos, category, loggable));
    }

    private void execute(Runnable runnable) {
        if (registry == null) {
            return;
        }
        async(runnable);
    }

    /**
     * Add Metrics
     *
     * @param statementInformation SQL Statement
     * @param timeElapsedNanos     the elapsed time of the SQL execution in nanos
     * @param batchCounts          the counts of batch
     * @param e                    the exception of the SQL execution
     */
    private void addMetrics(StatementInformation statementInformation, long timeElapsedNanos, int batchCounts,
                            @Nullable SQLException e) {
        String sql = statementInformation.getSql().trim();
        addMetrics(sql, timeElapsedNanos, batchCounts, e);
    }

    /**
     * Add Metrics
     *
     * @param sql              the executed SQL
     * @param timeElapsedNanos the elapsed time of the SQL execution in nanos
     * @param batchCounts      the counts of batch
     * @param e                the exception of the SQL execution
     */
    void addMetrics(String sql, long timeElapsedNanos, int batchCounts, @Nullable SQLException e) {

        String type = resolveStatementType(sql);

        if (type == null) {
            return;
        }

        String batches = valueOf(batchCounts);

        builder(SQL_SUCCESS_COUNT_METRIC_NAME)
                .tags("type", type)
                .tags("batches", batches)
                .register(registry)
                .increment();

        if (e != null) {
            builder(SQL_FAILURE_COUNT_METRIC_NAME)
                    .tags("type", type)
                    .tags("batches", batches)
                    .register(registry)
                    .increment();
        }

        if (timeElapsedNanos > slowSQLThresholdNanos) {
            builder(SLOW_SQL_TIME_METRIC_NAME, () -> timeElapsedNanos)
                    .tag("sql", sql)
                    .tags("type", type)
                    .tags("batches", batches)
                    .register(registry);
        }
    }

    String resolveStatementType(String sql) {
        int firstWhitespaceIndex = -1;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (isWhitespace(c)) {
                firstWhitespaceIndex = i;
                break;
            }
        }

        if (firstWhitespaceIndex > 0) {
            String type = sql.substring(0, firstWhitespaceIndex);
            return type.toUpperCase(ENGLISH);
        }

        return null;
    }

    /**
     * Set the {@link MeterRegistry}
     *
     * @param registry the {@link MeterRegistry}
     */
    public static void setRegistry(MeterRegistry registry) {
        MicrometerJdbcEventListener.registry = registry;
    }

    public long getSlowSQLThresholdNanos() {
        return slowSQLThresholdNanos;
    }

    public void setSlowSQLThresholdNanos(long slowSQLThresholdNanos) {
        this.slowSQLThresholdNanos = slowSQLThresholdNanos;
    }

}
