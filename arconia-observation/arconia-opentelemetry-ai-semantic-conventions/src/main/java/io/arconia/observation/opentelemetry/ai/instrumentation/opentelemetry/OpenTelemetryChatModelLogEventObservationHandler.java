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
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

/**
 * Records the {@code gen_ai.client.inference.operation.details} event as an OpenTelemetry log record,
 * carrying the attributes of the inference span and the message content in structured form.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/general/events/">OpenTelemetry Semantic Conventions for Events</a>
 */
public final class OpenTelemetryChatModelLogEventObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    static final String INSTRUMENTATION_SCOPE_NAME = "io.arconia.observation.opentelemetry.ai";

    private final JsonMapper jsonMapper = JacksonUtils.getDefaultJsonMapper();

    private final OpenTelemetryAiConventionsProperties properties;

    private final Logger logger;

    public OpenTelemetryChatModelLogEventObservationHandler(OpenTelemetryAiConventionsProperties properties, OpenTelemetry openTelemetry) {
        this(properties, openTelemetry, OpenTelemetryChatModelLogEventObservationHandler.class.getPackage().getImplementationVersion());
    }

    /**
     * Creates the handler with the version of the instrumentation scope, which is the Arconia version when known.
     */
    OpenTelemetryChatModelLogEventObservationHandler(OpenTelemetryAiConventionsProperties properties, OpenTelemetry openTelemetry,
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
        if (properties.getInferenceContentDestination() != OpenTelemetryAiConventionsProperties.InferenceContentDestination.LOG_EVENTS
                || !(properties.isCaptureMessageContent() || properties.isCaptureToolDefinitions())) {
            return;
        }

        LogRecordBuilder logRecord = logger.logRecordBuilder()
                .setEventName(GenAiAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS)
                .setTimestamp(Instant.now())
                .setContext(spanContext(context));

        for (KeyValue keyValue : context.getAllKeyValues()) {
            if (isSemanticConventionKey(keyValue.getKey())) {
                logRecord.setAttribute(keyValue.getKey(), keyValue.getValue());
            }
        }

        if (properties.isCaptureMessageContent()) {
            if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
                var inputMessages = GenAiContent.fromMessages(context.getRequest().getInstructions());
                if (!inputMessages.isEmpty()) {
                    logRecord.setAttribute(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey(), toValue(jsonMapper.valueToTree(inputMessages)));
                }
            }

            if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
                var outputMessages = GenAiContent.fromGenerations(context.getResponse().getResults());
                if (!outputMessages.isEmpty()) {
                    logRecord.setAttribute(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), toValue(jsonMapper.valueToTree(outputMessages)));
                }
            }
        }

        if (properties.isCaptureToolDefinitions()) {
            var toolDefinitions = GenAiContent.toolDefinitions(context.getRequest().getOptions());
            if (!toolDefinitions.isEmpty()) {
                logRecord.setAttribute(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey(), toValue(jsonMapper.valueToTree(toolDefinitions)));
            }
        }

        logRecord.emit();
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

    /**
     * Returns the context of the span of the observation, so that the event is correlated with it.
     */
    private static Context spanContext(ChatModelObservationContext context) {
        Span span = MicrometerBridge.extractOtelSpan(context.get(TracingObservationHandler.TracingContext.class));
        return span != null ? Context.root().with(span) : Context.current();
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
