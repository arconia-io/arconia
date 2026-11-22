package io.arconia.dev.ui.server.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

import io.arconia.core.annotations.Incubating;
import io.arconia.dev.ui.server.error.DevUiErrorViewResolver;
import io.arconia.dev.ui.server.web.DevUiHosts;

/**
 * Auto-configuration for the welcome page and the error pages of the Dev UI, in applications
 * that use the error handling of Spring MVC.
 */
@AutoConfiguration(after = { DevUiAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@ConditionalOnClass({ DispatcherServlet.class, ErrorViewResolver.class })
@ConditionalOnBean(DevUiHosts.class)
@ConditionalOnBooleanProperty(prefix = DevUiProperties.CONFIG_PREFIX, name = "error-pages.enabled", matchIfMissing = true)
@Incubating
public final class DevUiErrorPagesAutoConfiguration {

    @Bean
    DevUiErrorViewResolver devUiErrorViewResolver(ApplicationContext applicationContext, ObjectProvider<ErrorViewResolver> errorViewResolvers,
            ObjectProvider<ErrorAttributes> errorAttributes, DevUiHosts devUiHosts) {
        return new DevUiErrorViewResolver(applicationContext, errorViewResolvers, errorAttributes, devUiHosts);
    }

}
