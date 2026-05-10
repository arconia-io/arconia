package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;

import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

public class OpenTelemetryChatModelEventObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryChatModelEventObservationHandler(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        if (!OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_EVENTS.equals(
                properties.getCaptureContent())) {
            return;
        }

        List<GenAiContent.InputMessage> inputMessages = new ArrayList<>();
        if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            inputMessages = GenAiContent.fromMessages(context.getRequest().getInstructions());
        }

        List<GenAiContent.OutputMessage> outputMessages = new ArrayList<>();
        if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
            outputMessages = GenAiContent.fromGenerations(context.getResponse().getResults());
        }

        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);

        if (span == null) {
            return;
        }

        span.addEvent(GenAiAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS, Attributes.builder()
                .put(GenAiAttributes.GEN_AI_INPUT_MESSAGES, JsonParser.toJson(inputMessages))
                .put(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES, JsonParser.toJson(outputMessages))
                .build());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

}
