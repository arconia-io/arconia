package io.arconia.openinference.observation.autoconfigure.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

/**
 * Auto-configuration for OpenInference Resource instrumentation.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({OpenTelemetryResourceAutoConfiguration.class, ResourceContributor.class})
class OpenInferenceResourceConfiguration {

    @Bean
    @Order(OpenTelemetryResourceAutoConfiguration.DEFAULT_ORDER)
    OpenInferenceResourceContributor openInferenceResourceContributor() {
        return new OpenInferenceResourceContributor();
    }

}
