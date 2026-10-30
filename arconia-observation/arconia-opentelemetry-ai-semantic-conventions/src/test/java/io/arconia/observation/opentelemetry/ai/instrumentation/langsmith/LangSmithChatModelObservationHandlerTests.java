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
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithChatModelObservationHandler}.
 */
class LangSmithChatModelObservationHandlerTests {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    private final LangSmithChatModelObservationHandler handler = new LangSmithChatModelObservationHandler();

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void shouldSetTokenUsageAsNumbers() {
        SpanData span = observe(createContext(new DefaultUsage(100, 20, 120, null, 80L, 10L)));

        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isEqualTo(100L);
        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS)).isEqualTo(20L);
        assertThat(span.getAttributes().get(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS)).isEqualTo(120L);
        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS)).isEqualTo(80L);
        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS)).isEqualTo(10L);
    }

    @Test
    void shouldNotSetCacheTokensWhenNotReported() {
        SpanData span = observe(createContext(new DefaultUsage(100, 20, 120)));

        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS)).isEqualTo(100L);
        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS)).isNull();
        assertThat(span.getAttributes().get(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS)).isNull();
    }

    @Test
    void shouldNotSetTokenUsageWhenNotReported() {
        SpanData span = observe(createContext(new EmptyUsage()));

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

    // Helpers

    private SpanData observe(ChatModelObservationContext context) {
        Span span = tracerProvider.get("test").spanBuilder("chat gpt-5").startSpan();
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

    private ChatModelObservationContext createContext(Usage usage) {
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
