package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties.InferenceContentDestination;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryChatModelEventObservationHandler}.
 */
class OpenTelemetryChatModelEventObservationHandlerTests {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();

    private final OpenTelemetryChatModelEventObservationHandler handler = new OpenTelemetryChatModelEventObservationHandler(properties);

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void shouldNotAddEventWhenDestinationIsNotSpanEvents() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.SPAN_ATTRIBUTES);

        assertThat(observe(createContext()).getEvents()).isEmpty();
    }

    @Test
    void shouldNotAddEventWhenNoContentIsCaptured() {
        properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

        assertThat(observe(createContext()).getEvents()).isEmpty();
    }

    @Test
    void shouldAddEventWithMessages() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

        EventData event = singleEvent(observe(createContext()));
        assertThat(event.getName()).isEqualTo(GenAiAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS);
        assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_INPUT_MESSAGES)).contains("What's the weather in Paris?");
        assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES)).contains("Rainy, 14°C.");
        assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS)).isNull();
    }

    @Test
    void shouldAddEventWithToolDefinitionsOnly() {
        properties.setCaptureToolDefinitions(true);
        properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

        EventData event = singleEvent(observe(createContext()));
        assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS))
                .contains("get_weather").contains("Get the weather");
        assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_INPUT_MESSAGES)).isNull();
        assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES)).isNull();
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

    private EventData singleEvent(SpanData span) {
        assertThat(span.getEvents()).hasSize(1);
        return span.getEvents().getFirst();
    }

    private ChatModelObservationContext createContext() {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .model("gpt-5")
                .toolCallbacks(FunctionToolCallback.builder("get_weather", (String location) -> "Rainy")
                        .description("Get the weather")
                        .inputType(String.class)
                        .build())
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("What's the weather in Paris?")), options))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().id("resp-1").model("gpt-5").build())
                .generations(List.of(new Generation(
                        AssistantMessage.builder().content("Rainy, 14°C.").build(),
                        ChatGenerationMetadata.builder().finishReason("stop").build())))
                .build());
        return context;
    }

}
