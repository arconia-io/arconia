package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.observation.conventions.AiProvider;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryChatModelMeterObservationHandler}.
 */
class OpenTelemetryChatModelMeterObservationHandlerTests {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final OpenTelemetryChatModelMeterObservationHandler handler = new OpenTelemetryChatModelMeterObservationHandler(meterRegistry);

    @Test
    void shouldSupportChatModelContextsOnly() {
        assertThat(handler.supportsContext(createContext(new DefaultUsage(100, 20, 120)))).isTrue();
        assertThat(handler.supportsContext(new Observation.Context())).isFalse();
    }

    @Test
    void shouldRecordTokenUsageAsDistributionSummaries() {
        handler.onStop(createContext(new DefaultUsage(100, 20, 120)));

        assertThat(meterRegistry.find(GenAiAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE.getKey(), GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.INPUT)
                .summary()).isNotNull().satisfies(summary -> assertThat(summary.totalAmount()).isEqualTo(100));
        assertThat(meterRegistry.find(GenAiAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE.getKey(), GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.OUTPUT)
                .summary()).isNotNull().satisfies(summary -> assertThat(summary.totalAmount()).isEqualTo(20));
    }

    @Test
    void shouldNotRecordTokenUsageWhenNotReported() {
        handler.onStop(createContext(new EmptyUsage()));

        assertThat(meterRegistry.find(GenAiAttributes.GEN_AI_CLIENT_TOKEN_USAGE).summaries()).isEmpty();
    }

    @Test
    void shouldNotRecordTokenUsageWithoutResponse() {
        handler.onStop(createContextWithoutResponse());

        assertThat(meterRegistry.find(GenAiAttributes.GEN_AI_CLIENT_TOKEN_USAGE).summaries()).isEmpty();
    }

    // Helpers

    private static ChatModelObservationContext createContext(Usage usage) {
        ChatModelObservationContext context = createContextWithoutResponse();
        context.setResponse(ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("gpt-5").usage(usage).build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hello").build())))
                .build());
        return context;
    }

    private static ChatModelObservationContext createContextWithoutResponse() {
        return ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi"))))
                .provider(AiProvider.SPRING_AI.value())
                .build();
    }
}
