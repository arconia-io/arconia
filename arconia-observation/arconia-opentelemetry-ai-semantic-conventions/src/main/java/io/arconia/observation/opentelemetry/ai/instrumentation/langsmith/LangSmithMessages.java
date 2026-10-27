package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Builds the inputs and outputs of LangSmith runs from Spring AI messages, as a {@code messages} list
 * in the format LangSmith renders as a conversation.
 *
 * @see <a href="https://docs.langchain.com/langsmith/log-llm-trace">LangSmith: Log LLM calls</a>
 */
final class LangSmithMessages {

    private static final String MESSAGES = "messages";

    private LangSmithMessages() {
    }

    /**
     * Builds the inputs of a run from the messages sent to the model.
     */
    static Map<String, Object> fromMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof AssistantMessage assistantMessage) {
                result.add(assistantMessage(assistantMessage));
            } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                toolResponseMessage.getResponses().forEach(response -> result.add(toolResponse(response)));
            } else {
                result.add(simpleMessage(message));
            }
        }
        return Map.of(MESSAGES, result);
    }

    /**
     * Builds the outputs of a run from the generations returned by the model.
     */
    static Map<String, Object> fromGenerations(List<Generation> generations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Generation generation : generations) {
            result.add(assistantMessage(generation.getOutput()));
        }
        return Map.of(MESSAGES, result);
    }

    private static Map<String, Object> simpleMessage(Message message) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", message.getMessageType().getValue());
        if (StringUtils.hasText(message.getText())) {
            messageMap.put("content", message.getText());
        }
        return messageMap;
    }

    private static Map<String, Object> assistantMessage(AssistantMessage message) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "assistant");
        messageMap.put("content", StringUtils.hasText(message.getText()) ? message.getText() : "");
        if (!CollectionUtils.isEmpty(message.getToolCalls())) {
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                Map<String, Object> toolCallMap = new LinkedHashMap<>();
                toolCallMap.put("id", toolCall.id());
                toolCallMap.put("name", toolCall.name());
                toolCallMap.put("args", toolCall.arguments());
                toolCallMap.put("type", "tool_call");
                toolCalls.add(toolCallMap);
            }
            messageMap.put("tool_calls", toolCalls);
        }
        return messageMap;
    }

    private static Map<String, Object> toolResponse(ToolResponseMessage.ToolResponse response) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "tool");
        messageMap.put("tool_call_id", response.id());
        messageMap.put("name", response.name());
        if (StringUtils.hasText(response.responseData())) {
            messageMap.put("content", response.responseData());
        }
        return messageMap;
    }

}
