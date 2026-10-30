package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithEmbeddingModelObservationHandler}.
 */
class LangSmithEmbeddingModelObservationHandlerTests {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    private final LangSmithEmbeddingModelObservationHandler handler = new LangSmithEmbeddingModelObservationHandler();

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void shouldSetTokenUsageAsNumbers() {
        SpanData span = observe(createContext(new DefaultUsage(100, 0, 100)));

        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isEqualTo(100L);
        assertThat(span.getAttributes().get(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS)).isEqualTo(100L);
        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS)).isNull();
    }

    @Test
    void shouldNotSetTokenUsageWhenNotReported() {
        SpanData span = observe(createContext(new EmptyUsage()));

        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isNull();
        assertThat(span.getAttributes().get(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS)).isNull();
    }

    @Test
    void shouldNotSetTokenUsageWithoutResponse() {
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(new EmbeddingRequest(List.of("Hi"), EmbeddingOptions.builder().build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observe(context).getAttributes().isEmpty()).isTrue();
    }

    // Helpers

    private SpanData observe(EmbeddingModelObservationContext context) {
        Span span = tracerProvider.get("test").spanBuilder("embeddings text-embedding-3").startSpan();
        TracingObservationHandler.TracingContext tracingContext = new TracingObservationHandler.TracingContext();
        tracingContext.setSpan(OtelSpan.fromOtel(span));
        context.put(TracingObservationHandler.TracingContext.class, tracingContext);

        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        Observation.createNotStarted("gen_ai.client.operation.duration", () -> context, registry).start().stop();
        span.end();

        assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
        return spanExporter.getFinishedSpanItems().getFirst();
    }

    private EmbeddingModelObservationContext createContext(Usage usage) {
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(new EmbeddingRequest(List.of("Hi"), EmbeddingOptions.builder().build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(new EmbeddingResponse(List.of(new Embedding(new float[] {0.1f, 0.2f}, 0)),
                new EmbeddingResponseMetadata("text-embedding-3", usage)));
        return context;
    }

}
