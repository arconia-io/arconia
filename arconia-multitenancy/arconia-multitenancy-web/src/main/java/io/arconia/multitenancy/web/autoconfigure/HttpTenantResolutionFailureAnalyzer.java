package io.arconia.multitenancy.web.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

/**
 * A {@link FailureAnalyzer} that provides actionable feedback when no
 * {@link HttpRequestTenantResolver} is available for the configured resolution mode.
 */
public class HttpTenantResolutionFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
        implements Ordered {

    private static final String RESOLUTION_MODE_PROPERTY = HttpTenantResolutionProperties.CONFIG_PREFIX
            + ".resolution-mode";

    private final Environment environment;

    public HttpTenantResolutionFailureAnalyzer(Environment environment) {
        this.environment = environment;
    }

    @Override
    @Nullable
    protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
        if (!HttpRequestTenantResolver.class.equals(cause.getBeanType())) {
            return null;
        }
        String resolutionMode = environment.getProperty(RESOLUTION_MODE_PROPERTY);
        if (!"oauth2".equalsIgnoreCase(resolutionMode)) {
            return null;
        }
        return new FailureAnalysis(
                "The '%s' property is set to 'oauth2', but Spring Security OAuth2 is not on the classpath, so no %s bean could be auto-configured."
                    .formatted(RESOLUTION_MODE_PROPERTY, HttpRequestTenantResolver.class.getSimpleName()),
                """
                Add Spring Security OAuth2 to your project, for example with the \
                'spring-boot-starter-security-oauth2-resource-server' or 'spring-boot-starter-security-oauth2-client' dependency.
                Alternatively, set %s to 'header' or 'cookie', or register your own %s bean."""
                    .formatted(RESOLUTION_MODE_PROPERTY, HttpRequestTenantResolver.class.getSimpleName()),
                cause);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
