package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.incubating.GenAiIncubatingEvents;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties.InferenceContentDestination;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

/**
 * Records the captured content of model calls in a {@code gen_ai.client.inference.operation.details} event,
 * when the content destination is an event: attached to the span of the observation for {@code span-events},
 * or as an OpenTelemetry log record correlated with that span for {@code log-events}. The log record also carries
 * the attributes of the span, and the content in structured form.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/general/events/">OpenTelemetry Semantic Conventions for Events</a>
 */
public final class OpenTelemetryChatModelEventObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    static final String INSTRUMENTATION_SCOPE_NAME = "io.arconia.observation.opentelemetry.ai";

    private final JsonMapper jsonMapper = JacksonUtils.getDefaultJsonMapper();

    private final OpenTelemetryAiConventionsProperties properties;

    private final Logger logger;

    public OpenTelemetryChatModelEventObservationHandler(OpenTelemetryAiConventionsProperties properties, OpenTelemetry openTelemetry) {
        this(properties, openTelemetry, OpenTelemetryChatModelEventObservationHandler.class.getPackage().getImplementationVersion());
    }

    /**
     * Creates the handler with the version of the instrumentation scope of the log records, which is the Arconia
     * version when known.
     */
    OpenTelemetryChatModelEventObservationHandler(OpenTelemetryAiConventionsProperties properties, OpenTelemetry openTelemetry,
            @Nullable String instrumentationVersion) {
        Assert.notNull(properties, "properties cannot be null");
        Assert.notNull(openTelemetry, "openTelemetry cannot be null");
        this.properties = properties;
        LoggerBuilder loggerBuilder = openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE_NAME);
        if (StringUtils.hasText(instrumentationVersion)) {
            loggerBuilder.setInstrumentationVersion(instrumentationVersion);
        }
        this.logger = loggerBuilder.build();
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        InferenceContentDestination destination = properties.contentDestination();
        if (destination == InferenceContentDestination.SPAN_ATTRIBUTES
                || !(properties.capturesMessageContent() || properties.capturesToolDefinitions())) {
            return;
        }
        Span span = MicrometerBridge.extractOtelSpan(context.get(TracingObservationHandler.TracingContext.class));
        if (destination == InferenceContentDestination.SPAN_EVENTS) {
            // Events added to a span that isn't recording, such as an unsampled one, are dropped:
            // serialising the messages for it would be wasted work.
            if (span != null && span.isRecording()) {
                addSpanEvent(span, context);
            }
            return;
        }
        emitLogRecord(span, context);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

    private void addSpanEvent(Span span, ChatModelObservationContext context) {
        Content content = content(context);
        AttributesBuilder attributes = Attributes.builder();
        if (!content.inputMessages().isEmpty()) {
            attributes.put(GenAiAttributes.GEN_AI_INPUT_MESSAGES, jsonMapper.writeValueAsString(content.inputMessages()));
        }
        if (!content.outputMessages().isEmpty()) {
            attributes.put(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES, jsonMapper.writeValueAsString(content.outputMessages()));
        }
        if (!content.toolDefinitions().isEmpty()) {
            attributes.put(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS, jsonMapper.writeValueAsString(content.toolDefinitions()));
        }
        span.addEvent(GenAiIncubatingEvents.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS, attributes.build());
    }

    private void emitLogRecord(@Nullable Span span, ChatModelObservationContext context) {
        LogRecordBuilder logRecord = logger.logRecordBuilder()
                .setEventName(GenAiIncubatingEvents.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS)
                .setSeverity(Severity.DEBUG)
                .setTimestamp(Instant.now())
                .setContext(span != null ? Context.root().with(span) : Context.current());
        for (KeyValue keyValue : context.getAllKeyValues()) {
            if (isSemanticConventionKey(keyValue.getKey())) {
                logRecord.setAttribute(keyValue.getKey(), keyValue.getValue());
            }
        }
        Content content = content(context);
        if (!content.inputMessages().isEmpty()) {
            logRecord.setAttribute(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey(), toValue(jsonMapper.valueToTree(content.inputMessages())));
        }
        if (!content.outputMessages().isEmpty()) {
            logRecord.setAttribute(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), toValue(jsonMapper.valueToTree(content.outputMessages())));
        }
        if (!content.toolDefinitions().isEmpty()) {
            // The tool definitions embed the input schemas as raw JSON, so they are serialised and read back
            // into the tree the structured attribute is built from: this is the one place that needs the tree.
            JsonNode tree = jsonMapper.readTree(jsonMapper.writeValueAsString(content.toolDefinitions()));
            logRecord.setAttribute(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey(), toValue(tree));
        }
        logRecord.emit();
    }

    /**
     * Collects the content captured for the model call: the input and output messages, and the tool definitions.
     */
    private Content content(ChatModelObservationContext context) {
        List<GenAiContent.InputMessage> inputMessages = List.of();
        List<GenAiContent.OutputMessage> outputMessages = List.of();
        if (properties.capturesMessageContent()) {
            if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
                inputMessages = GenAiContent.fromMessages(context.getRequest().getInstructions());
            }
            if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
                outputMessages = GenAiContent.fromGenerations(context.getResponse().getResults());
            }
        }
        List<Map<String, Object>> toolDefinitions = properties.capturesToolDefinitions()
                ? GenAiContent.toolDefinitions(context.getRequest().getOptions()) : List.of();
        return new Content(inputMessages, outputMessages, toolDefinitions);
    }

    private record Content(List<GenAiContent.InputMessage> inputMessages, List<GenAiContent.OutputMessage> outputMessages,
            List<Map<String, Object>> toolDefinitions) {
    }

    /**
     * Whether the key belongs to the attributes the semantic conventions define for the event,
     * leaving out the Spring AI specific ones.
     */
    private static boolean isSemanticConventionKey(String key) {
        return key.startsWith("gen_ai.") || key.startsWith("server.") || key.equals("error.type");
    }

    /**
     * Converts a JSON tree into an OpenTelemetry {@link Value}, so that the content is recorded in structured form.
     */
    private static Value<?> toValue(JsonNode node) {
        if (node.isObject()) {
            List<io.opentelemetry.api.common.KeyValue> keyValues = new ArrayList<>();
            for (Map.Entry<String, JsonNode> property : node.properties()) {
                keyValues.add(io.opentelemetry.api.common.KeyValue.of(property.getKey(), toValue(property.getValue())));
            }
            return Value.of(keyValues.toArray(io.opentelemetry.api.common.KeyValue[]::new));
        }
        if (node.isArray()) {
            List<Value<?>> values = new ArrayList<>();
            for (JsonNode element : node.values()) {
                values.add(toValue(element));
            }
            return Value.of(values);
        }
        if (node.isBoolean()) {
            return Value.of(node.booleanValue());
        }
        if (node.isIntegralNumber()) {
            return Value.of(node.longValue());
        }
        if (node.isNumber()) {
            return Value.of(node.doubleValue());
        }
        if (node.isNull() || node.isMissingNode()) {
            return Value.empty();
        }
        return Value.of(node.asString());
    }

}
