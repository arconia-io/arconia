package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryEmbeddingMeterObservationHandler}.
 */
class OpenTelemetryEmbeddingMeterObservationHandlerTests {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final OpenTelemetryEmbeddingMeterObservationHandler handler = new OpenTelemetryEmbeddingMeterObservationHandler(meterRegistry);

    @Test
    void shouldSupportEmbeddingModelContextsOnly() {
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
                .summary()).isNull();
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

    private static EmbeddingModelObservationContext createContext(Usage usage) {
        EmbeddingModelObservationContext context = createContextWithoutResponse();
        context.setResponse(new EmbeddingResponse(List.of(new Embedding(new float[] {0.1f, 0.2f}, 0)),
                new EmbeddingResponseMetadata("text-embedding-3", usage)));
        return context;
    }

    private static EmbeddingModelObservationContext createContextWithoutResponse() {
        return EmbeddingModelObservationContext.builder()
                .embeddingRequest(new EmbeddingRequest(List.of("Hi"), EmbeddingOptions.builder().build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
    }
}
