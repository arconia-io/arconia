package io.arconia.observation.opentelemetry.ai.autoconfigure;

import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelEventObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelLogEventObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversationIdObservationHandler;

/**
 * Conventions of the {@code opentelemetry} flavor: the OpenTelemetry GenAI semantic conventions, with the
 * content of inference observations recorded where {@code inference-content-destination} says.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, name = "flavor", havingValue = "opentelemetry", matchIfMissing = true)
class OpenTelemetryFlavorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenTelemetryChatModelObservationConvention chatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new OpenTelemetryChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(OpenTelemetryChatModelEventObservationHandler.class)
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    OpenTelemetryChatModelEventObservationHandler chatModelEventObservationHandler(OpenTelemetryAiConventionsProperties properties) {
        return new OpenTelemetryChatModelEventObservationHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean(OpenTelemetryChatModelLogEventObservationHandler.class)
    @ConditionalOnBean(OpenTelemetry.class)
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    OpenTelemetryChatModelLogEventObservationHandler chatModelLogEventObservationHandler(OpenTelemetryAiConventionsProperties properties, OpenTelemetry openTelemetry) {
        return new OpenTelemetryChatModelLogEventObservationHandler(properties, openTelemetry);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        OpenTelemetryChatClientObservationConvention chatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
            return new OpenTelemetryChatClientObservationConvention(properties);
        }

        @Bean
        @ConditionalOnMissingBean(GenAiConversationIdObservationHandler.class)
        GenAiConversationIdObservationHandler conversationIdObservationHandler() {
            return new GenAiConversationIdObservationHandler(List.of(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey()));
        }

    }

}
