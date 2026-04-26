package io.arconia.observation.langsmith.instrumentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Observation handler that sets typed span attributes
 * and span events for chat model observations.
 * <p>
 * This handler sets token usage attributes with proper numeric types directly
 * on the OTel Span (Micrometer KeyValues are always strings, but LangSmith
 * expects numeric types for usage metrics).
 */
public class LangSmithChatModelObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private final LangSmithOptions langSmithOptions;

    public LangSmithChatModelObservationHandler(LangSmithOptions langSmithOptions) {
        this.langSmithOptions = langSmithOptions;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);

        if (span == null) {
            return;
        }

        setUsageAttributes(span, context);

        if (langSmithOptions.getInference().isIncludeContent()) {
            emitInputMessageEvents(span, context);
            emitOutputMessageEvents(span, context);
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

    private void setUsageAttributes(Span span, ChatModelObservationContext context) {
        if (context.getResponse() == null) {
            return;
        }

        var usage = context.getResponse().getMetadata().getUsage();
        span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS, Long.valueOf(usage.getPromptTokens()));
        span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS, Long.valueOf(usage.getCompletionTokens()));
        span.setAttribute(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS.getKey(), Long.valueOf(usage.getTotalTokens()));
    }

    private void emitInputMessageEvents(Span span, ChatModelObservationContext context) {
        List<Message> messages = context.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        for (Message message : messages) {
            String eventName = toEventName(message.getMessageType());

            if (message instanceof AssistantMessage assistantMessage) {
                span.addEvent(eventName, Attributes.of(
                        LangSmithAttributes.GEN_AI_EVENT_CONTENT,
                        buildAssistantMessageJson(assistantMessage)));
            } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    span.addEvent(eventName, Attributes.of(
                            LangSmithAttributes.GEN_AI_EVENT_CONTENT,
                            buildToolResponseJson(response)));
                }
            } else {
                span.addEvent(eventName, Attributes.of(
                        LangSmithAttributes.GEN_AI_EVENT_CONTENT,
                        buildSimpleMessageJson(message)));
            }
        }
    }

    private void emitOutputMessageEvents(Span span, ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return;
        }

        for (Generation generation : context.getResponse().getResults()) {
            AssistantMessage output = generation.getOutput();
            span.addEvent(LangSmithAttributes.GEN_AI_ASSISTANT_MESSAGE, Attributes.of(
                    LangSmithAttributes.GEN_AI_EVENT_CONTENT,
                    buildAssistantMessageJson(output)));
        }
    }

    private String buildSimpleMessageJson(Message message) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", message.getMessageType().getValue());
        if (StringUtils.hasText(message.getText())) {
            messageMap.put("content", message.getText());
        }
        return JsonParser.toJson(messageMap);
    }

    private String buildAssistantMessageJson(AssistantMessage message) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "assistant");

        if (StringUtils.hasText(message.getText())) {
            messageMap.put("content", message.getText());
        } else {
            messageMap.put("content", "");
        }

        if (!CollectionUtils.isEmpty(message.getToolCalls())) {
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                Map<String, Object> tc = new LinkedHashMap<>();
                tc.put("id", toolCall.id());
                tc.put("name", toolCall.name());
                tc.put("args", toolCall.arguments());
                tc.put("type", "tool_call");
                toolCalls.add(tc);
            }
            messageMap.put("tool_calls", toolCalls);
        }

        return JsonParser.toJson(messageMap);
    }

    private String buildToolResponseJson(ToolResponseMessage.ToolResponse response) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "tool");
        messageMap.put("tool_call_id", response.id());
        messageMap.put("name", response.name());
        if (StringUtils.hasText(response.responseData())) {
            messageMap.put("content", response.responseData());
        }
        return JsonParser.toJson(messageMap);
    }

    private static String toEventName(MessageType messageType) {
        return switch (messageType) {
            case SYSTEM -> LangSmithAttributes.GEN_AI_SYSTEM_MESSAGE;
            case USER -> LangSmithAttributes.GEN_AI_USER_MESSAGE;
            case ASSISTANT -> LangSmithAttributes.GEN_AI_ASSISTANT_MESSAGE;
            case TOOL -> LangSmithAttributes.GEN_AI_TOOL_MESSAGE;
        };
    }

}
