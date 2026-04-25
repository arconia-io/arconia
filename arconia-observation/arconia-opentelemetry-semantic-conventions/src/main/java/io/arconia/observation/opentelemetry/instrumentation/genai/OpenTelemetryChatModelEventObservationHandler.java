package io.arconia.observation.opentelemetry.instrumentation.genai;

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

import io.arconia.observation.opentelemetry.instrumentation.util.MicrometerBridge;

public class OpenTelemetryChatModelEventObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private final OpenTelemetryGenAiOptions openTelemetryGenAiOptions;

    public OpenTelemetryChatModelEventObservationHandler(OpenTelemetryGenAiOptions openTelemetryGenAiOptions) {
        this.openTelemetryGenAiOptions = openTelemetryGenAiOptions;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        var captureContent = openTelemetryGenAiOptions.getInference().getCaptureContent();
        if (!OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_EVENTS.equals(captureContent)) {
            return;
        }

        List<OpenTelemetryGenAiContent.InputMessage> inputMessages = new ArrayList<>();
        if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            inputMessages = OpenTelemetryGenAiContent.fromMessages(context.getRequest().getInstructions());
        }

        List<OpenTelemetryGenAiContent.OutputMessage> outputMessages = new ArrayList<>();
        if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
            outputMessages = OpenTelemetryGenAiContent.fromGenerations(context.getResponse().getResults());
        }


        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);

        if (span == null) {
            return;
        }

        span.addEvent(GenAiMoreIncubatingAttributes.GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS, Attributes.builder()
                .put(GenAiMoreIncubatingAttributes.GEN_AI_INPUT_MESSAGES, JsonParser.toJson(inputMessages))
                .put(GenAiMoreIncubatingAttributes.GEN_AI_OUTPUT_MESSAGES, JsonParser.toJson(outputMessages))
                .build());

    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

}
