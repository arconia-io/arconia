package io.arconia.observation.openllmetry.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;

/**
 * {@link ChatModelObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private static final KeyValue MODEL_NONE = KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, KeyValue.NONE_VALUE);

    private final OpenLLMetryOptions openLLMetryOptions;

    public OpenLLMetryChatModelObservationConvention(OpenLLMetryOptions openLLMetryOptions) {
        this.openLLMetryOptions = openLLMetryOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
        return KeyValues.of(traceloopSpanKind(), genAiSystem(context), genAiOperationName(),
                genAiRequestModel(context));
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.SPAN_KIND_TASK);
    }

    private KeyValue genAiSystem(ChatModelObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                OpenLLMetryConventionsConverters.toSystemName(context.getOperationMetadata().provider()));
    }

    private KeyValue genAiOperationName() {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_OPERATION_NAME, OpenLLMetryAttributes.OPERATION_CHAT);
    }

    private KeyValue genAiRequestModel(ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, context.getResponse().getMetadata().getModel());
        } else if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, context.getRequest().getOptions().getModel());
        }
        return MODEL_NONE;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = KeyValues.empty();

        // Request
        keyValues = llmRequestType(keyValues);
        keyValues = requestParameters(keyValues, context);
        keyValues = requestTools(keyValues, context);
        keyValues = entityInput(keyValues, context);

        // Response
        keyValues = entityOutput(keyValues, context);
        keyValues = responseFinishReasons(keyValues, context);
        keyValues = responseId(keyValues, context);
        keyValues = usageInputTokens(keyValues, context);
        keyValues = usageOutputTokens(keyValues, context);
        keyValues = usageTotalTokens(keyValues, context);

        return keyValues;
    }

    // Request

    private KeyValues llmRequestType(KeyValues keyValues) {
        return keyValues.and(OpenLLMetryAttributes.LLM_REQUEST_TYPE, OpenLLMetryAttributes.LLM_REQUEST_TYPE_CHAT);
    }

    private KeyValues requestParameters(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options == null) {
            return keyValues;
        }

        if (options.getTemperature() != null) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_TEMPERATURE,
                    String.valueOf(options.getTemperature()));
        }
        if (options.getMaxTokens() != null) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_MAX_TOKENS,
                    String.valueOf(options.getMaxTokens()));
        }
        if (options.getTopP() != null) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_TOP_P,
                    String.valueOf(options.getTopP()));
        }
        if (options.getTopK() != null) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_TOP_K,
                    String.valueOf(options.getTopK()));
        }
        if (options.getFrequencyPenalty() != null) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY,
                    String.valueOf(options.getFrequencyPenalty()));
        }
        if (options.getPresencePenalty() != null) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY,
                    String.valueOf(options.getPresencePenalty()));
        }
        if (!CollectionUtils.isEmpty(options.getStopSequences())) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_REQUEST_STOP_SEQUENCES,
                    JsonParser.toJson(options.getStopSequences()));
        }

        return keyValues;
    }

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!openLLMetryOptions.isIncludeToolDefinitions()) {
            return keyValues;
        }

        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return keyValues;
        }

        List<Map<String, Object>> toolDefinitions = new ArrayList<>();

        List<ToolCallback> toolCallbacks = new ArrayList<>(options.getToolCallbacks());
        for (ToolCallback toolCallback : toolCallbacks) {
            Map<String, Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("function", Map.of(
                    "name", toolCallback.getToolDefinition().name(),
                    "description", toolCallback.getToolDefinition().description(),
                    "parameters", JsonParser.fromJson(toolCallback.getToolDefinition().inputSchema(),
                            new TypeReference<Map<String, Object>>() {})
            ));
            toolDefinitions.add(toolDefinition);
        }

        List<String> toolNames = new ArrayList<>(options.getToolNames());
        for (String toolName : toolNames) {
            Map<String, Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("function", Map.of("name", toolName));
            toolDefinitions.add(toolDefinition);
        }

        if (!toolDefinitions.isEmpty()) {
            keyValues = keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME + ".tools",
                    JsonParser.toJson(toolDefinitions));
        }

        return keyValues;
    }

    private KeyValues entityInput(KeyValues keyValues, ChatModelObservationContext context) {
        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return keyValues;
        }

        List<Message> messages = new ArrayList<>(context.getRequest().getInstructions());
        List<Map<String, Object>> inputMessages = new ArrayList<>();

        for (Message message : messages) {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", message.getMessageType().getValue());

            if (message.getText() != null) {
                messageMap.put("content", message.getText());
            }

            if (message instanceof AssistantMessage assistantMessage) {
                List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
                if (!CollectionUtils.isEmpty(toolCalls)) {
                    List<Map<String, Object>> toolCallMaps = new ArrayList<>();
                    for (AssistantMessage.ToolCall toolCall : toolCalls) {
                        toolCallMaps.add(Map.of(
                                "id", toolCall.id(),
                                "function", Map.of(
                                        "name", toolCall.name(),
                                        "arguments", toolCall.arguments()
                                )
                        ));
                    }
                    messageMap.put("tool_calls", toolCallMaps);
                }
            }

            if (message instanceof ToolResponseMessage toolResponseMessage) {
                List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();
                if (!CollectionUtils.isEmpty(toolResponses)) {
                    messageMap.put("tool_call_id", toolResponses.getFirst().id());
                    messageMap.put("name", toolResponses.getFirst().name());
                    messageMap.put("content", toolResponses.getFirst().responseData());
                }
            }

            inputMessages.add(messageMap);
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, JsonParser.toJson(inputMessages));
    }

    // Response

    private KeyValues entityOutput(KeyValues keyValues, ChatModelObservationContext context) {
        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        List<Generation> generations = new ArrayList<>(context.getResponse().getResults());
        List<Map<String, Object>> outputMessages = new ArrayList<>();

        for (Generation generation : generations) {
            AssistantMessage message = generation.getOutput();
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", message.getMessageType().getValue());

            if (message.getText() != null) {
                messageMap.put("content", message.getText());
            }

            List<AssistantMessage.ToolCall> toolCalls = message.getToolCalls();
            if (!CollectionUtils.isEmpty(toolCalls)) {
                List<Map<String, Object>> toolCallMaps = new ArrayList<>();
                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    toolCallMaps.add(Map.of(
                            "id", toolCall.id(),
                            "function", Map.of(
                                    "name", toolCall.name(),
                                    "arguments", toolCall.arguments()
                            )
                    ));
                }
                messageMap.put("tool_calls", toolCallMaps);
            }

            outputMessages.add(messageMap);
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, JsonParser.toJson(outputMessages));
    }

    protected KeyValues responseFinishReasons(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && context.getResponse().getResult() != null) {
            var finishReason = context.getResponse().getResult().getMetadata().getFinishReason();
            if (StringUtils.hasText(finishReason)) {
                return keyValues.and(OpenLLMetryAttributes.GEN_AI_RESPONSE_FINISH_REASONS, finishReason);
            }
        }
        return keyValues;
    }

    protected KeyValues responseId(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getId())) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_RESPONSE_ID,
                    context.getResponse().getMetadata().getId());
        }
        return keyValues;
    }

    protected KeyValues usageInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_USAGE_INPUT_TOKENS,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
        }
        return keyValues;
    }

    protected KeyValues usageOutputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_USAGE_OUTPUT_TOKENS,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getCompletionTokens()));
        }
        return keyValues;
    }

    protected KeyValues usageTotalTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_USAGE_TOTAL_TOKENS,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getTotalTokens()));
        }
        return keyValues;
    }

}
