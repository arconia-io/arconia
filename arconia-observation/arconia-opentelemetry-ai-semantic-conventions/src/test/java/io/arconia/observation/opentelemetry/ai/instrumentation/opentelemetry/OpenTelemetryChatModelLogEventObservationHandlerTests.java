package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

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
 * Unit tests for {@link OpenTelemetryChatModelLogEventObservationHandler}.
 */
class OpenTelemetryChatModelLogEventObservationHandlerTests {

    private final InMemoryLogRecordExporter logRecordExporter = InMemoryLogRecordExporter.create();

    private final OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setLoggerProvider(SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(logRecordExporter))
                    .build())
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build();

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();

    private final OpenTelemetryChatModelObservationConvention convention = new OpenTelemetryChatModelObservationConvention(properties);

    private final OpenTelemetryChatModelLogEventObservationHandler handler =
            new OpenTelemetryChatModelLogEventObservationHandler(properties, openTelemetry);

    @AfterEach
    void tearDown() {
        openTelemetry.close();
    }

    @Test
    void shouldNotEmitEventWhenDestinationIsNotLogEvents() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

        observe(createContext());

        assertThat(logRecordExporter.getFinishedLogRecordItems()).isEmpty();
    }

    @Test
    void shouldNotEmitEventWhenNoContentIsCaptured() {
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);

        observe(createContext());

        assertThat(logRecordExporter.getFinishedLogRecordItems()).isEmpty();
    }

    @Test
    void shouldEmitEventWithSpanAttributes() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);

        observe(createContext());

        LogRecordData logRecord = singleLogRecord();
        assertThat(logRecord.getEventName()).isEqualTo(GenAiAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS);
        assertThat(logRecord.getInstrumentationScopeInfo().getName())
                .isEqualTo(OpenTelemetryChatModelLogEventObservationHandler.INSTRUMENTATION_SCOPE_NAME);
        assertThat(logRecord.getAttributes().get(AttributeKey.stringKey(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey())))
                .isEqualTo("chat");
        assertThat(logRecord.getAttributes().get(AttributeKey.stringKey(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey())))
                .isEqualTo("spring_ai");
        assertThat(logRecord.getAttributes().get(AttributeKey.stringKey(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey())))
                .isEqualTo("gpt-5");
        assertThat(logRecord.getAttributes().get(AttributeKey.stringKey(GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID.getKey())))
                .isEqualTo("resp-1");
        assertThat(logRecord.getAttributes().get(AttributeKey.stringKey("spring.ai.kind"))).isNull();
    }

    @Test
    void shouldUseInstrumentationScopeVersionWhenKnown() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);
        var versionedHandler = new OpenTelemetryChatModelLogEventObservationHandler(properties, openTelemetry, "1.2.3");
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(versionedHandler);

        Observation.createNotStarted(convention, this::createContext, registry).start().stop();

        assertThat(singleLogRecord().getInstrumentationScopeInfo().getVersion()).isEqualTo("1.2.3");
    }

    @Test
    void shouldOmitInstrumentationScopeVersionWhenUnknown() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);

        observe(createContext());

        assertThat(singleLogRecord().getInstrumentationScopeInfo().getVersion()).isNull();
    }

    @Test
    void shouldEmitEventWithStructuredMessages() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);

        observe(createContext());

        LogRecordData logRecord = singleLogRecord();
        assertThat(logRecord.getAttributes().get(AttributeKey.valueKey(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey())))
                .isEqualTo(Value.of(List.of(Value.of(
                        KeyValue.of("role", Value.of("user")),
                        KeyValue.of("parts", Value.of(Value.of(
                                KeyValue.of("type", Value.of("text")),
                                KeyValue.of("content", Value.of("What's the weather in Paris?")))))))));
        assertThat(logRecord.getAttributes().get(AttributeKey.valueKey(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey())))
                .isEqualTo(Value.of(List.of(Value.of(
                        KeyValue.of("role", Value.of("assistant")),
                        KeyValue.of("parts", Value.of(Value.of(
                                KeyValue.of("type", Value.of("text")),
                                KeyValue.of("content", Value.of("Rainy, 14°C."))))),
                        KeyValue.of("finish_reason", Value.of("stop"))))));
    }

    @Test
    void shouldEmitEventWithStructuredToolDefinitionsOnly() {
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);
        properties.setCaptureToolDefinitions(true);
        ChatModelObservationContext context = createContext(ToolCallingChatOptions.builder()
                .model("gpt-5")
                .toolCallbacks(FunctionToolCallback.builder("get_weather", (String location) -> "Rainy")
                        .description("Get the weather")
                        .inputType(String.class)
                        .build())
                .build());

        observe(context);

        Value<?> toolDefinitions = singleLogRecord().getAttributes()
                .get(AttributeKey.valueKey(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey()));
        assertThat(toolDefinitions).isNotNull();
        assertThat(toolDefinitions.asString()).contains("get_weather").contains("Get the weather");
        assertThat(singleLogRecord().getAttributes().get(AttributeKey.stringKey(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey())))
                .isNull();
        assertThat(singleLogRecord().getAttributes().get(AttributeKey.valueKey(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey())))
                .isNull();
    }

    @Test
    void shouldCorrelateEventWithObservationSpan() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);
        ChatModelObservationContext context = createContext();
        Span span = openTelemetry.getTracer("test").spanBuilder("chat gpt-5").startSpan();
        TracingObservationHandler.TracingContext tracingContext = new TracingObservationHandler.TracingContext();
        tracingContext.setSpan(OtelSpan.fromOtel(span));
        context.put(TracingObservationHandler.TracingContext.class, tracingContext);

        observe(context);
        span.end();

        assertThat(singleLogRecord().getSpanContext()).isEqualTo(span.getSpanContext());
    }

    // Helpers

    private void observe(ChatModelObservationContext context) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        Observation.createNotStarted(convention, () -> context, registry).start().stop();
    }

    private LogRecordData singleLogRecord() {
        assertThat(logRecordExporter.getFinishedLogRecordItems()).hasSize(1);
        return logRecordExporter.getFinishedLogRecordItems().getFirst();
    }

    private ChatModelObservationContext createContext() {
        return createContext(ToolCallingChatOptions.builder().model("gpt-5").build());
    }

    private ChatModelObservationContext createContext(ToolCallingChatOptions options) {
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
