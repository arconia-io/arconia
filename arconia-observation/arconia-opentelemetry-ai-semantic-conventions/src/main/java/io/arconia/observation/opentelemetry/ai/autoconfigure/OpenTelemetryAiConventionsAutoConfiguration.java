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

/**
 * Auto-configuration for OpenTelemetry AI Semantic Conventions.
 * Supports multiple flavors: {@code opentelemetry} (default), {@code openllmetry}, and {@code langsmith}.
 *
 * @see OpenTelemetryAiConventionsProperties
 */
@AutoConfiguration(beforeName = {
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
})
@ConditionalOnClass({ ChatModelObservationConvention.class, EmbeddingModelObservationConvention.class, ToolCallingObservationConvention.class })
@ConditionalOnBooleanProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryAiConventionsProperties.class)
@Import({ OpenTelemetryFlavorConfiguration.class, OpenLLMetryFlavorConfiguration.class, LangSmithFlavorConfiguration.class })
public final class OpenTelemetryAiConventionsAutoConfiguration {

    @Bean
    AiObservationConventionsProvider openTelemetryAiConventionsProvider() {
        return AiObservationConventionsProvider.of("opentelemetry");
    }

}
