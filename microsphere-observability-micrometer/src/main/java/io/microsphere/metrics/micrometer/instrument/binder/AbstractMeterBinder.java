package io.microsphere.metrics.micrometer.instrument.binder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.micrometer.core.instrument.Tags.concat;
import static io.microsphere.util.ClassUtils.getSimpleName;
import static io.microsphere.util.StringUtils.substringBefore;
import static java.util.Collections.emptyList;

/**
 * Abstract {@link MeterBinder}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 */
public abstract class AbstractMeterBinder implements MeterBinder {

    /**
     * The {@link Tag} key of the metrics origin : "origin"
     */
    public static final String ORIGIN_TAG_KEY = "origin";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Iterable<Tag> tags;

    public AbstractMeterBinder() {
        this(emptyList());
    }

    public AbstractMeterBinder(Iterable<Tag> tags) {
        this.tags = concat(tags, ORIGIN_TAG_KEY, getOriginTagValue());
    }

    /**
     * The Metric tag value of "origin"
     *
     * @return non-null
     */
    protected String getOriginTagValue() {
        return getSimpleName(this.getClass());
    }

    @Override
    public final void bindTo(MeterRegistry registry) {
        if (!supports(registry)) {
            logger.info("Current Metrics is not Supported");
            return;
        }
        try {
            doBindTo(registry);
        } catch (Throwable e) {
            logger.error("Bind To MeterRegistry[{}] failed", registry, e);
        }
    }

    protected Iterable<Tag> combine(String... tags) {
        return concat(this.tags, tags);
    }

    protected abstract boolean supports(MeterRegistry registry);

    protected abstract void doBindTo(MeterRegistry registry) throws Throwable;

}