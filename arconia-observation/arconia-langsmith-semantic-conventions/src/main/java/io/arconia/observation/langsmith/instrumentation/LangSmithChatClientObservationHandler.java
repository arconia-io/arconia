package io.arconia.observation.langsmith.instrumentation;

import java.util.LinkedHashMap;
import java.util.Map;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.StringUtils;

/**
 * Observation handler that emits input/output span events for chat client observations.
 */
public final class LangSmithChatClientObservationHandler implements ObservationHandler<ChatClientObservationContext> {

    private final LangSmithOptions langSmithOptions;

    public LangSmithChatClientObservationHandler(LangSmithOptions langSmithOptions) {
        this.langSmithOptions = langSmithOptions;
    }

    @Override
    public void onStop(ChatClientObservationContext context) {
        if (!langSmithOptions.getInference().isIncludeContent()) {
            return;
        }

        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);

        if (span == null) {
            return;
        }

        emitInputEvent(span, context);
        emitOutputEvent(span, context);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatClientObservationContext;
    }

    private void emitInputEvent(Span span, ChatClientObservationContext context) {
        String userInput = context.getRequest().prompt().getUserMessage().getText();

        if (!StringUtils.hasText(userInput)) {
            return;
        }

        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "user");
        messageMap.put("content", userInput);

        span.addEvent(LangSmithAttributes.GEN_AI_USER_MESSAGE,
                Attributes.of(LangSmithAttributes.GEN_AI_EVENT_CONTENT, JsonParser.toJson(messageMap)));
    }

    private void emitOutputEvent(Span span, ChatClientObservationContext context) {
        if (context.getResponse() == null || context.getResponse().chatResponse() == null) {
            return;
        }

        ChatResponse chatResponse = context.getResponse().chatResponse();
        Generation result = chatResponse.getResult();
        if (result == null) {
            return;
        }

        String outputText = result.getOutput().getText();
        if (!StringUtils.hasText(outputText)) {
            return;
        }

        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "assistant");
        messageMap.put("content", outputText);

        span.addEvent(LangSmithAttributes.GEN_AI_ASSISTANT_MESSAGE,
                Attributes.of(LangSmithAttributes.GEN_AI_EVENT_CONTENT, JsonParser.toJson(messageMap)));
    }

}
