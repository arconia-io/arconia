package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;

import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.CollectionUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

public final class OpenTelemetryChatModelEventObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private final JsonHelper jsonHelper = new JsonHelper();

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryChatModelEventObservationHandler(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        if (properties.getInferenceContentDestination() != OpenTelemetryAiConventionsProperties.InferenceContentDestination.SPAN_EVENTS
                || !(properties.isCaptureMessageContent() || properties.isCaptureToolDefinitions())) {
            return;
        }

        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);

        if (span == null || !span.isRecording()) {
            // Events added to a span that isn't recording, such as an unsampled one, are dropped:
            // serialising the messages for it would be wasted work.
            return;
        }

        AttributesBuilder attributes = Attributes.builder();

        if (properties.isCaptureMessageContent()) {
            List<GenAiContent.InputMessage> inputMessages = new ArrayList<>();
            if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
                inputMessages = GenAiContent.fromMessages(context.getRequest().getInstructions());
            }

            List<GenAiContent.OutputMessage> outputMessages = new ArrayList<>();
            if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
                outputMessages = GenAiContent.fromGenerations(context.getResponse().getResults());
            }

            attributes.put(GenAiAttributes.GEN_AI_INPUT_MESSAGES, jsonHelper.toJson(inputMessages));
            attributes.put(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES, jsonHelper.toJson(outputMessages));
        }

        if (properties.isCaptureToolDefinitions()) {
            var toolDefinitions = GenAiContent.toolDefinitions(context.getRequest().getOptions());
            if (!toolDefinitions.isEmpty()) {
                attributes.put(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS, jsonHelper.toJson(toolDefinitions));
            }
        }

        span.addEvent(GenAiAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS, attributes.build());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

}
