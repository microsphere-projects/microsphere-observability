package io.microsphere.observability.logging.spring.boot.autoconfigure;

import io.microsphere.spring.boot.webmvc.autoconfigure.condition.ConditionalOnWebMvcAvailable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import static io.microsphere.observability.logging.util.LoggerUtils.trace;

/**
 * Web MVC Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebMvcAvailable
@AutoConfigureAfter(
        name = {
                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration"        // Spring Boot Web API
        }
)
public class WebMvcLoggingAutoConfiguration implements ApplicationListener<ServletRequestHandledEvent> {

    @Override
    public void onApplicationEvent(ServletRequestHandledEvent event) {
        trace(logger -> logger.trace("{}", event));
    }
}