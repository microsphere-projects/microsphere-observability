package io.microsphere.metrics.micrometer.spring.boot.actuate.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.CGROUP_DIRECTORY_PATH_PROPERTY_NAME;
import static io.microsphere.metrics.micrometer.instrument.binder.system.constants.SystemConstants.DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE;
import static io.microsphere.metrics.micrometer.spring.boot.actuate.condition.ConditionalOnCGroupAvailable.CGROUP_DIRECTORY_LOCATION_PLACEHOLDER;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The Spring Boot {@link Conditional} annotation for the "cgroup" Subsystem availability,
 * which is based on the existence of the CGroup Directory System Property :
 * "${cgroup.dir:file:///sys/fs/cgroup/}"
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@ConditionalOnResource(resources = CGROUP_DIRECTORY_LOCATION_PLACEHOLDER)
public @interface ConditionalOnCGroupAvailable {

    /**
     * The Placeholder of CGroup Directory Location : "${cgroup.dir:file:///sys/fs/cgroup/}"
     */
    String CGROUP_DIRECTORY_LOCATION_PLACEHOLDER = "${" + CGROUP_DIRECTORY_PATH_PROPERTY_NAME + ":file://" + DEFAULT_CGROUP_DIRECTORY_PATH_PROPERTY_VALUE + "}";
}