package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.CollectionUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

public class OpenTelemetryChatClientEventObservationHandler implements ObservationHandler<ChatClientObservationContext> {

    private final JsonHelper jsonHelper = new JsonHelper();

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryChatClientEventObservationHandler(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onStop(ChatClientObservationContext context) {
        if (!OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_EVENTS.equals(
                properties.getCaptureContent())) {
            return;
        }

        List<GenAiContent.InputMessage> inputMessages = new ArrayList<>();
        if (!CollectionUtils.isEmpty(context.getRequest().prompt().getInstructions())) {
            inputMessages = GenAiContent.fromMessages(context.getRequest().prompt().getInstructions());
        }

        List<GenAiContent.OutputMessage> outputMessages = new ArrayList<>();
        if (context.getResponse() != null && context.getResponse().chatResponse() != null
                && !CollectionUtils.isEmpty(context.getResponse().chatResponse().getResults())) {
            outputMessages = GenAiContent.fromGenerations(context.getResponse().chatResponse().getResults());
        }

        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);

        if (span == null) {
            return;
        }

        span.addEvent(GenAiAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS, Attributes.builder()
                .put(GenAiAttributes.GEN_AI_INPUT_MESSAGES, jsonHelper.toJson(inputMessages))
                .put(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES, jsonHelper.toJson(outputMessages))
                .build());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatClientObservationContext;
    }

}
