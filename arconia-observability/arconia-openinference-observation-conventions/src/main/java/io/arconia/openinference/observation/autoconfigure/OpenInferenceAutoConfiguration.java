package io.arconia.openinference.observation.autoconfigure;

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.openinference.observation.instrumentation.OpenInferenceChatModelObservationConvention;
import io.arconia.openinference.observation.instrumentation.OpenInferenceEmbeddingModelObservationConvention;
import io.arconia.openinference.observation.instrumentation.OpenInferenceSpringAiOnlyObservationPredicate;
import io.arconia.openinference.observation.instrumentation.OpenInferenceToolCallingObservationConvention;
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
@ConditionalOnBooleanProperty(prefix = OpenInferenceProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenInferenceProperties.class)
@Import({OpenInferenceChatClientConfiguration.class, OpenInferenceResourceConfiguration.class})
public class OpenInferenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenInferenceChatModelObservationConvention chatModelObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceChatModelObservationConvention(properties.getTraces());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenInferenceEmbeddingModelObservationConvention embeddingModelObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceEmbeddingModelObservationConvention(properties.getTraces());
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenInferenceToolCallingObservationConvention toolCallingObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceToolCallingObservationConvention(properties.getTraces());
    }

    @Bean
    @ConditionalOnBooleanProperty(prefix = OpenInferenceProperties.CONFIG_PREFIX, value = "include-only-ai-observations", matchIfMissing = true)
    OpenInferenceSpringAiOnlyObservationPredicate openInferenceSpringAiOnlyObservationPredicate() {
        return new OpenInferenceSpringAiOnlyObservationPredicate();
    }

}
