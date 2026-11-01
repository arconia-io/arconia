package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;
import io.opentelemetry.semconv.incubating.GenAiIncubatingEvents;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
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
import io.arconia.observation.opentelemetry.ai.instrumentation.GenAiConventionFlavor;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryChatModelEventObservationHandler}.
 */
class OpenTelemetryChatModelEventObservationHandlerTests {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    private final InMemoryLogRecordExporter logRecordExporter = InMemoryLogRecordExporter.create();

    private final OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build())
            .setLoggerProvider(SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(logRecordExporter))
                    .build())
            .build();

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();

    private final OpenTelemetryChatModelObservationConvention convention = new OpenTelemetryChatModelObservationConvention(properties);

    private final OpenTelemetryChatModelEventObservationHandler handler =
            new OpenTelemetryChatModelEventObservationHandler(properties, openTelemetry);

    @AfterEach
    void tearDown() {
        openTelemetry.close();
    }

    @Test
    void shouldNotRecordEventWhenDestinationIsSpanAttributes() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.SPAN_ATTRIBUTES);

        assertThat(observeWithSpan(createContext()).getEvents()).isEmpty();
        assertThat(logRecordExporter.getFinishedLogRecordItems()).isEmpty();
    }

    @Test
    void shouldNotRecordEventForOtherFlavors() {
        properties.setFlavor(GenAiConventionFlavor.LANGFUSE);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);

        observe(createContext());

        assertThat(logRecordExporter.getFinishedLogRecordItems()).isEmpty();
    }

    @Nested
    class SpanEvents {

        @Test
        void shouldNotAddEventWhenNoContentIsCaptured() {
            properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

            assertThat(observeWithSpan(createContext()).getEvents()).isEmpty();
        }

        @Test
        void shouldNotAddEventWhenSpanIsNotRecording() {
            properties.setCaptureMessageContent(true);
            properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);
            ChatModelObservationContext context = createContext();
            TracingObservationHandler.TracingContext tracingContext = new TracingObservationHandler.TracingContext();
            tracingContext.setSpan(OtelSpan.fromOtel(Span.getInvalid()));
            context.put(TracingObservationHandler.TracingContext.class, tracingContext);

            observe(context);

            assertThat(spanExporter.getFinishedSpanItems()).isEmpty();
        }

        @Test
        void shouldAddEventWithMessages() {
            properties.setCaptureMessageContent(true);
            properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

            EventData event = singleEvent(observeWithSpan(createContext()));
            assertThat(event.getName()).isEqualTo(GenAiIncubatingEvents.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS);
            assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_INPUT_MESSAGES)).contains("What's the weather in Paris?");
            assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES)).contains("Rainy, 14°C.");
            assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS)).isNull();
        }

        @Test
        void shouldAddEventWithToolDefinitionsOnly() {
            properties.setCaptureToolDefinitions(true);
            properties.setInferenceContentDestination(InferenceContentDestination.SPAN_EVENTS);

            EventData event = singleEvent(observeWithSpan(createContext(optionsWithTool())));
            assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS))
                    .contains("get_weather").contains("Get the weather");
            assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_INPUT_MESSAGES)).isNull();
            assertThat(event.getAttributes().get(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES)).isNull();
        }

        private EventData singleEvent(SpanData span) {
            assertThat(span.getEvents()).hasSize(1);
            return span.getEvents().getFirst();
        }

    }

    @Nested
    class LogEvents {

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
            assertThat(logRecord.getEventName()).isEqualTo(GenAiIncubatingEvents.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS);
            assertThat(logRecord.getSeverity()).isEqualTo(Severity.DEBUG);
            assertThat(logRecord.getInstrumentationScopeInfo().getName())
                    .isEqualTo(OpenTelemetryChatModelEventObservationHandler.INSTRUMENTATION_SCOPE_NAME);
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
            var versionedHandler = new OpenTelemetryChatModelEventObservationHandler(properties, openTelemetry, "1.2.3");
            ObservationRegistry registry = ObservationRegistry.create();
            registry.observationConfig().observationHandler(versionedHandler);

            Observation.createNotStarted(convention, () -> createContext(), registry).start().stop();

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

            observe(createContext(optionsWithTool()));

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

            SpanData span = observeWithSpan(createContext());

            assertThat(singleLogRecord().getSpanContext()).isEqualTo(span.getSpanContext());
        }

        private LogRecordData singleLogRecord() {
            assertThat(logRecordExporter.getFinishedLogRecordItems()).hasSize(1);
            return logRecordExporter.getFinishedLogRecordItems().getFirst();
        }

    }

    // Helpers

    private void observe(ChatModelObservationContext context) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        Observation.createNotStarted(convention, () -> context, registry).start().stop();
    }

    private SpanData observeWithSpan(ChatModelObservationContext context) {
        Span span = openTelemetry.getTracer("test").spanBuilder("chat gpt-5").startSpan();
        TracingObservationHandler.TracingContext tracingContext = new TracingObservationHandler.TracingContext();
        tracingContext.setSpan(OtelSpan.fromOtel(span));
        context.put(TracingObservationHandler.TracingContext.class, tracingContext);

        observe(context);
        span.end();

        assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
        return spanExporter.getFinishedSpanItems().getFirst();
    }

    private static ToolCallingChatOptions optionsWithTool() {
        return ToolCallingChatOptions.builder()
                .model("gpt-5")
                .toolCallbacks(FunctionToolCallback.builder("get_weather", (String location) -> "Rainy")
                        .description("Get the weather")
                        .inputType(String.class)
                        .build())
                .build();
    }

    private static ChatModelObservationContext createContext() {
        return createContext(ToolCallingChatOptions.builder().model("gpt-5").build());
    }

    private static ChatModelObservationContext createContext(ToolCallingChatOptions options) {
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
