package io.arconia.observation.opentelemetry.ai.autoconfigure;

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.observation.conventions.AiObservationConventionsProvider;
import io.arconia.observation.opentelemetry.ai.instrumentation.GenAiConventionFlavor;

/**
 * Auto-configuration for OpenTelemetry AI Semantic Conventions.
 * Supports multiple flavors, each one a {@link GenAiConventionFlavor}: {@code opentelemetry} (default),
 * {@code openllmetry}, {@code langsmith}, {@code openlit}, and {@code langfuse}. The flavor configurations
 * define the conventions specific to each flavor, {@link FlavorDefaultsConfiguration} the ones shared by all.
 *
 * @see OpenTelemetryAiConventionsProperties
 */
@AutoConfiguration(afterName = {
        "io.arconia.opentelemetry.autoconfigure.OpenTelemetryAutoConfiguration",
        "org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration"
}, beforeName = {
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
})
@ConditionalOnClass({ ChatModelObservationConvention.class, EmbeddingModelObservationConvention.class, ToolCallingObservationConvention.class })
@ConditionalOnBooleanProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryAiConventionsProperties.class)
@Import({ OpenTelemetryFlavorConfiguration.class, OpenLLMetryFlavorConfiguration.class, LangSmithFlavorConfiguration.class,
        OpenLitFlavorConfiguration.class, LangfuseFlavorConfiguration.class, FlavorDefaultsConfiguration.class })
public final class OpenTelemetryAiConventionsAutoConfiguration {

    @Bean
    AiObservationConventionsProvider openTelemetryAiConventionsProvider() {
        return AiObservationConventionsProvider.of("opentelemetry");
    }

}
