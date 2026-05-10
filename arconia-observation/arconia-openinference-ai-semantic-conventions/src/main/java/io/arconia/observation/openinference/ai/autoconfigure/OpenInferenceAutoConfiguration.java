package io.arconia.observation.openinference.ai.autoconfigure;

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

import io.arconia.observation.conventions.AiObservationConventionsProvider;
import io.arconia.observation.openinference.ai.instrumentation.OpenInferenceChatModelObservationConvention;
import io.arconia.observation.openinference.ai.instrumentation.OpenInferenceEmbeddingModelObservationConvention;
import io.arconia.observation.openinference.ai.instrumentation.OpenInferenceToolCallingObservationConvention;

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
@ConditionalOnBooleanProperty(prefix = OpenInferenceProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenInferenceProperties.class)
@Import({OpenInferenceChatClientConfiguration.class, OpenInferenceResourceConfiguration.class})
public final class OpenInferenceAutoConfiguration {

    @Bean
    AiObservationConventionsProvider openInferenceConventionsProvider() {
        return AiObservationConventionsProvider.of("openinference");
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenInferenceChatModelObservationConvention chatModelObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenInferenceEmbeddingModelObservationConvention embeddingModelObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceEmbeddingModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenInferenceToolCallingObservationConvention toolCallingObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceToolCallingObservationConvention(properties);
    }

}
