package io.arconia.dev.ui.server.autoconfigure;

import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;
import io.arconia.core.bootstrap.autoconfigure.ConditionalOnDevMode;
import io.arconia.dev.ui.api.page.DevUiPage;
import io.arconia.dev.ui.api.page.TablePage;
import io.arconia.dev.ui.server.pages.EnvironmentPage;
import io.arconia.dev.ui.server.pages.PropertiesPage;
import io.arconia.dev.ui.server.web.DevUiApplication;
import io.arconia.dev.ui.server.web.DevUiFilter;
import io.arconia.dev.ui.server.web.DevUiHosts;
import io.arconia.dev.ui.server.web.DevUiLogs;
import io.arconia.dev.ui.server.web.DevUiServlet;

/**
 * Auto-configuration for the Dev UI.
 */
@AutoConfiguration
@ConditionalOnDevMode
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBooleanProperty(prefix = DevUiProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(DevUiProperties.class)
@Incubating
public final class DevUiAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DevUiAutoConfiguration.class);

    private static final String URL_MAPPING = DevUiServlet.BASE_PATH + "/*";

    /**
     * The Dev UI is served by its own servlet instead of the Spring MVC infrastructure of the
     * application, so that it is not affected by how the application configures Spring MVC
     * (servlet path, interceptors, message converters, error handling) and adds nothing to it.
     * <p>
     * The servlet is initialized when the application starts. Its requests reach it through
     * the Dev UI filter and not through the web server, which would otherwise initialize it on
     * the first one.
     */
    @Bean
    ServletRegistrationBean<DevUiServlet> devUiServlet(ObjectProvider<DevUiPage> pages, DevUiLogs devUiLogs,
            ApplicationContext applicationContext
    ) {
        ServletRegistrationBean<DevUiServlet> registration = new ServletRegistrationBean<>(
                new DevUiServlet(pages.stream().toList(), devUiLogs, new DevUiApplication(applicationContext)), URL_MAPPING);
        registration.setName("arconiaDevUiServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    /**
     * The Dev UI contributes the pages about any Spring Boot application the same way a
     * library contributes its own.
     */
    @Bean
    TablePage devUiPropertiesPage(ConfigurableEnvironment environment, ApplicationContext applicationContext,
            ObjectProvider<SanitizingFunction> sanitizingFunctions
    ) {
        return PropertiesPage.create(environment, applicationContext.getClassLoader(), sanitizingFunctions.orderedStream().toList());
    }

    @Bean
    TablePage devUiEnvironmentPage(ConfigurableEnvironment environment, ObjectProvider<SanitizingFunction> sanitizingFunctions) {
        return EnvironmentPage.create(environment, sanitizingFunctions.orderedStream().toList());
    }

    @Bean
    DevUiHosts devUiHosts(DevUiProperties properties, Environment environment) {
        boolean remoteAccess = properties.getRemoteAccess().isEnabled();
        boolean nativeForwardedHeaders = usesNativeForwardedHeaders(environment);
        if (nativeForwardedHeaders && !remoteAccess) {
            logger.warn("The web server replaces the address of a request with the one forwarded headers claim "
                    + "(server.forward-headers-strategy=native), so the Dev UI cannot tell which requests come from this machine "
                    + "and rejects the ones addressed to it. Set 'arconia.dev.ui.remote-access.enabled' to 'true' to accept them "
                    + "wherever they come from, or use 'server.forward-headers-strategy=framework'.");
        }
        return new DevUiHosts(properties.getAllowedHosts(), remoteAccess, !nativeForwardedHeaders);
    }

    /**
     * Whether the web server is the one that applies forwarded headers, as Spring Boot
     * decides it: when asked, or by default on a cloud platform.
     */
    private static boolean usesNativeForwardedHeaders(Environment environment) {
        String strategy = environment.getProperty("server.forward-headers-strategy");
        if (strategy != null) {
            return strategy.trim().equalsIgnoreCase("native");
        }
        CloudPlatform platform = CloudPlatform.getActive(environment);
        return platform != null && platform.isUsingForwardHeaders();
    }

    @Bean
    DevUiLogs devUiLogs() {
        return new DevUiLogs();
    }

    /**
     * The filter runs ahead of the other filters of the application, Spring Security included,
     * and hands the requests it accepts over to the servlet, so that none of those filters
     * gets in the way of the Dev UI.
     */
    @Bean
    FilterRegistrationBean<DevUiFilter> devUiFilter(ServletRegistrationBean<DevUiServlet> devUiServlet, DevUiHosts devUiHosts) {
        DevUiServlet servlet = devUiServlet.getServlet();
        Assert.notNull(servlet, "servlet cannot be null");
        FilterRegistrationBean<DevUiFilter> registration = new FilterRegistrationBean<>(new DevUiFilter(servlet, devUiHosts));
        registration.setName("arconiaDevUiFilter");
        // The pattern also matches the path of the Dev UI itself, without a slash at its end.
        registration.addUrlPatterns(URL_MAPPING);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * The requests to the Dev UI are left out of the metrics and traces of the application,
     * where they would be noise: they are not requests to the application, and a log being
     * followed is a request that stays open for minutes.
     */
    @Bean
    @ConditionalOnBooleanProperty(prefix = DevUiProperties.CONFIG_PREFIX, name = "observations.enabled", havingValue = false, matchIfMissing = true)
    ObservationPredicate devUiObservationPredicate() {
        return (name, context) -> !(context instanceof ServerRequestObservationContext serverContext
                && isDevUiRequest(serverContext.getCarrier()));
    }

    private static boolean isDevUiRequest(@Nullable HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(DevUiServlet.BASE_PATH) || path.startsWith(DevUiServlet.BASE_PATH + "/");
    }

}
