package io.microsphere.observability.logging.spring.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import static io.microsphere.observability.logging.util.LoggerUtils.trace;

/**
 * {@link WebServer} Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
public class WebServerLoggingAutoConfiguration implements ApplicationListener<WebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        trace(logger -> {
            WebServer webServer = event.getWebServer();
            WebServerApplicationContext context = event.getApplicationContext();
            logger.trace("WebServer['{}' , context : '{}'] port : {}", webServer, context.getId(), webServer.getPort());
        });
    }
}