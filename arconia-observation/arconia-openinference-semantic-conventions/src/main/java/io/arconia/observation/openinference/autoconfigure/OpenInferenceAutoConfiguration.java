package io.arconia.observation.openinference.autoconfigure;

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.observation.autoconfigure.ObservationProperties;
import io.arconia.observation.openinference.instrumentation.OpenInferenceChatModelObservationConvention;
import io.arconia.observation.openinference.instrumentation.OpenInferenceEmbeddingModelObservationConvention;
import io.arconia.observation.openinference.instrumentation.OpenInferenceOnlyObservationPredicate;
import io.arconia.observation.openinference.instrumentation.OpenInferenceToolCallingObservationConvention;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for OpenInference instrumentation in Spring AI.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
})
@ConditionalOnClass(ModelObservationContext.class)
@ConditionalOnOpenTelemetry
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "openinference")
@EnableConfigurationProperties(OpenInferenceProperties.class)
@Import({OpenInferenceChatClientConfiguration.class, OpenInferenceResourceConfiguration.class})
public final class OpenInferenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenInferenceChatModelObservationConvention chatModelObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceChatModelObservationConvention(properties.getOptions());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenInferenceEmbeddingModelObservationConvention embeddingModelObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceEmbeddingModelObservationConvention(properties.getOptions());
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenInferenceToolCallingObservationConvention toolCallingObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceToolCallingObservationConvention(properties.getOptions());
    }

    @Bean
    @ConditionalOnBooleanProperty(prefix = OpenInferenceProperties.CONFIG_PREFIX, value = "exclusive", matchIfMissing = true)
    OpenInferenceOnlyObservationPredicate openInferenceSpringAiOnlyObservationPredicate() {
        return new OpenInferenceOnlyObservationPredicate();
    }

}
