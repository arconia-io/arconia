package io.arconia.opentelemetry.autoconfigure.resource;

import java.util.Map;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.SchemaUrls;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.BuildResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.DistroResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.EnvironmentResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.GitResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.HostResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.JavaResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.OsResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.ProcessResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

/**
 * Auto-configuration for OpenTelemetry {@link Resource}.
 */
@AutoConfiguration(after = org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(OpenTelemetryResourceProperties.class)
public final class OpenTelemetryResourceAutoConfiguration {

    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    @ConditionalOnMissingBean
    Resource resource(ObjectProvider<ResourceContributor> resourceContributors,
                      ObjectProvider<OpenTelemetryResourceBuilderCustomizer> customizers
    ) {
        ResourceBuilder builder = Resource.getDefault().toBuilder().setSchemaUrl(SchemaUrls.V1_44_0);
        resourceContributors.orderedStream().forEach(contributor -> contributor.contribute(builder));
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "build", matchIfMissing = true)
    @ConditionalOnBean(BuildProperties.class)
    @Order(DEFAULT_ORDER)
    BuildResourceContributor buildResourceContributor(BuildProperties properties) {
        return new BuildResourceContributor(properties);
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "distro", matchIfMissing = true)
    @Order(DEFAULT_ORDER)
    DistroResourceContributor distroResourceContributor() {
        return new DistroResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "environment", matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    EnvironmentResourceContributor environmentResourceContributor(Environment environment, OpenTelemetryResourceProperties properties) {
        return new EnvironmentResourceContributor(environment, properties);
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "git")
    @ConditionalOnBean(GitProperties.class)
    @Order(DEFAULT_ORDER)
    GitResourceContributor gitResourceContributor(GitProperties properties) {
        return new GitResourceContributor(properties);
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "host")
    @Order(DEFAULT_ORDER)
    HostResourceContributor hostResourceContributor() {
        return new HostResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "java")
    @Order(DEFAULT_ORDER)
    JavaResourceContributor javaRuntimeResourceContributor() {
        return new JavaResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "os")
    @Order(DEFAULT_ORDER)
    OsResourceContributor osResourceContributor() {
        return new OsResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "process")
    @Order(DEFAULT_ORDER)
    ProcessResourceContributor processRuntimeResourceContributor() {
        return new ProcessResourceContributor();
    }

    @Bean
    OpenTelemetryResourceBuilderCustomizer filterAttributes(OpenTelemetryResourceProperties properties) {
        return builder -> {
            Map<String, Boolean> enable = properties.getEnable();
            if (!enable.isEmpty()) {
                builder.removeIf(attributeKey -> !isEnabled(enable, attributeKey.getKey()));
            }
        };
    }

    /**
     * Whether the attribute with the given key is enabled, using the longest matching name, compared
     * by dot-separated segments, and falling back to the {@code all} key, like the Spring Boot
     * {@code management.metrics.enable} properties.
     */
    static boolean isEnabled(Map<String, Boolean> enable, String attributeKey) {
        String name = attributeKey;
        while (StringUtils.hasLength(name)) {
            Boolean enabled = enable.get(name);
            if (enabled != null) {
                return enabled;
            }
            int lastDot = name.lastIndexOf('.');
            name = (lastDot != -1) ? name.substring(0, lastDot) : "";
        }
        return enable.getOrDefault("all", true);
    }

}
