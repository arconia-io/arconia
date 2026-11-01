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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithTokenUsageObservationHandler}.
 */
class LangSmithTokenUsageObservationHandlerTests {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    private final LangSmithTokenUsageObservationHandler handler = new LangSmithTokenUsageObservationHandler();

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Nested
    class ChatModel {

        @Test
        void shouldSetTokenUsageAsNumbers() {
            SpanData span = observe(chatContext(new DefaultUsage(100, 20, 120, null, 80L, 10L)));

            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isEqualTo(100L);
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS)).isEqualTo(20L);
            assertThat(span.getAttributes().get(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS)).isEqualTo(120L);
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS)).isEqualTo(80L);
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS)).isEqualTo(10L);
        }

        @Test
        void shouldNotSetCacheTokensWhenNotReported() {
            SpanData span = observe(chatContext(new DefaultUsage(100, 20, 120)));

            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isEqualTo(100L);
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS)).isNull();
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS)).isNull();
        }

        @Test
        void shouldNotSetTokenUsageWhenNotReported() {
            SpanData span = observe(chatContext(new EmptyUsage()));

            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isNull();
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS)).isNull();
            assertThat(span.getAttributes().get(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS)).isNull();
        }

        @Test
        void shouldNotSetTokenUsageWithoutResponse() {
            ChatModelObservationContext context = ChatModelObservationContext.builder()
                    .prompt(new Prompt(List.of(new UserMessage("Hi"))))
                    .provider(AiProvider.SPRING_AI.value())
                    .build();

            assertThat(observe(context).getAttributes().isEmpty()).isTrue();
        }

        private ChatModelObservationContext chatContext(Usage usage) {
            ChatModelObservationContext context = ChatModelObservationContext.builder()
                    .prompt(new Prompt(List.of(new UserMessage("Hi"))))
                    .provider(AiProvider.SPRING_AI.value())
                    .build();
            context.setResponse(ChatResponse.builder()
                    .metadata(ChatResponseMetadata.builder().id("resp-1").model("gpt-5").usage(usage).build())
                    .generations(List.of(new Generation(AssistantMessage.builder().content("Hello").build())))
                    .build());
            return context;
        }

    }

    @Nested
    class EmbeddingModel {

        @Test
        void shouldSetTokenUsageAsNumbers() {
            SpanData span = observe(embeddingContext(new DefaultUsage(100, 0, 100)));

            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isEqualTo(100L);
            assertThat(span.getAttributes().get(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS)).isEqualTo(100L);
            assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS)).isNull();
        }

        @Test
        void shouldNotSetTokenUsageWhenNotReported() {
            SpanData span = observe(embeddingContext(new EmptyUsage()));

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

        private EmbeddingModelObservationContext embeddingContext(Usage usage) {
            EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                    .embeddingRequest(new EmbeddingRequest(List.of("Hi"), EmbeddingOptions.builder().build()))
                    .provider(AiProvider.SPRING_AI.value())
                    .build();
            context.setResponse(new EmbeddingResponse(List.of(new Embedding(new float[] {0.1f, 0.2f}, 0)),
                    new EmbeddingResponseMetadata("text-embedding-3", usage)));
            return context;
        }

    }

    // Helpers

    private SpanData observe(Observation.Context context) {
        Span span = tracerProvider.get("test").spanBuilder("model call").startSpan();
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

}
