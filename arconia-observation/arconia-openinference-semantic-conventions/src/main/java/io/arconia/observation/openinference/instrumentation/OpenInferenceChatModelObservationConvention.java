package io.arconia.observation.openinference.instrumentation;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.semconv.trace.SemanticConventions;

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
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;

/**
 * {@link ChatModelObservationConvention} for OpenInference.
 */
public final class OpenInferenceChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private static final KeyValue MODEL_NONE = KeyValue.of(SemanticConventions.LLM_MODEL_NAME, KeyValue.NONE_VALUE);

    private final OpenInferenceOptions openInferenceOptions;

    public OpenInferenceChatModelObservationConvention(OpenInferenceOptions openInferenceOptions) {
        this.openInferenceOptions = openInferenceOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), llmSystem(context), llmModelName(context));
    }

    protected KeyValue aiOperationType(ChatModelObservationContext context) {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND,
                SemanticConventions.OpenInferenceSpanKind.LLM.getValue());
    }

    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_PROVIDER,
                OpenInferenceConventionsConverters.toLlmProvider(context.getOperationMetadata().provider()));
    }

    private KeyValue llmSystem(ChatModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_SYSTEM,
                OpenInferenceConventionsConverters.toLlmSystem(context.getOperationMetadata().provider()));
    }

    private KeyValue llmModelName(ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return KeyValue.of(SemanticConventions.LLM_MODEL_NAME, context.getResponse().getMetadata().getModel());
        } else if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return KeyValue.of(SemanticConventions.LLM_MODEL_NAME, context.getRequest().getOptions().getModel());
        }
        return MODEL_NONE;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = KeyValues.empty();

        // Request
        keyValues = llmInputMessages(keyValues, context);
        keyValues = llmInvocationParameters(keyValues, context);
        keyValues = llmTools(keyValues, context);

        // Response
        keyValues = llmOutputMessages(keyValues, context);
        keyValues = responseFinishReasons(keyValues, context);
        keyValues = responseId(keyValues, context);
        keyValues = usageInputTokens(keyValues, context);
        keyValues = usageOutputTokens(keyValues, context);
        keyValues = usageTotalTokens(keyValues, context);

        return keyValues;
    }

    // Request

    private KeyValues llmInputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideInputMessages()) {
            return keyValues.and(SemanticConventions.LLM_INPUT_MESSAGES, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return keyValues;
        }

        List<Message> messages = new ArrayList<>(context.getRequest().getInstructions());
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            keyValues = keyValues.and(
                    SemanticConventions.LLM_INPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_ROLE,
                    message.getMessageType().getValue()
            );
            keyValues = addMessageContent(keyValues,
                    SemanticConventions.LLM_INPUT_MESSAGES + "." + i,
                    message,
                    openInferenceOptions.isHideInputText(),
                    openInferenceOptions.isHideInputImages());

            if (message instanceof AssistantMessage assistantMessage) {
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>(assistantMessage.getToolCalls());
                String toolCallObservationPrefix = SemanticConventions.LLM_INPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_TOOL_CALLS;
                keyValues = addToolCalls(keyValues, toolCalls, toolCallObservationPrefix, openInferenceOptions.isHideInputText());
            }

            if (message instanceof ToolResponseMessage toolResponseMessage) {
                List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();
                if (CollectionUtils.isEmpty(toolResponses)) {
                    continue;
                }
                // Even though Spring AI supports parallel tool calling, OpenInference doesn't provide a way to
                // include multiple tool call responses in the same message. So we only include the first response.
                keyValues = keyValues.and(
                        SemanticConventions.LLM_INPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_TOOL_CALL_ID,
                        toolResponses.getFirst().id()
                );
                // Since there is no structured support for tool call responses, we include the first response content
                // as the main message content.
                if (!openInferenceOptions.isHideInputText()) {
                    keyValues = keyValues.and(
                            SemanticConventions.LLM_INPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_CONTENT,
                            toolResponses.getFirst().responseData()
                    );
                }
                keyValues = keyValues.and(
                        SemanticConventions.LLM_INPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_NAME,
                        toolResponses.getFirst().name()
                );
            }
        }

        return keyValues;
    }

    private KeyValues llmInvocationParameters(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideLlmInvocationParameters()) {
            return keyValues.and(SemanticConventions.LLM_INVOCATION_PARAMETERS, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        ChatOptions options = context.getRequest().getOptions();
        if (options == null) {
            return keyValues;
        }

        Map<String, Object> invocationParameters = new HashMap<>();

        if (options.getFrequencyPenalty() != null) {
            invocationParameters.put("frequency_penalty", options.getFrequencyPenalty());
        }
        if (options.getMaxTokens() != null) {
            invocationParameters.put("max_tokens", options.getMaxTokens());
        }
        if (options.getPresencePenalty() != null) {
            invocationParameters.put("presence_penalty", options.getPresencePenalty());
        }
        if (!CollectionUtils.isEmpty(options.getStopSequences())) {
            invocationParameters.put("stop_sequences", options.getStopSequences());
        }
        if (options.getTemperature() != null) {
            invocationParameters.put("temperature", options.getTemperature());
        }
        if (options.getTopK() != null) {
            invocationParameters.put("top_k", options.getTopK());
        }
        if (options.getTopP() != null) {
            invocationParameters.put("top_p", options.getTopP());
        }

        return keyValues.and(SemanticConventions.LLM_INVOCATION_PARAMETERS, JsonParser.toJson(invocationParameters));
    }

    private KeyValues llmTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs()) {
            return keyValues.and(SemanticConventions.LLM_TOOLS, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return keyValues;
        }

        List<ToolCallback> toolCallbacks = new ArrayList<>(options.getToolCallbacks());
        for (int i = 0; i < toolCallbacks.size(); i++) {
            Map<String,Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("function", Map.of(
                    "name", toolCallbacks.get(i).getToolDefinition().name(),
                    "description", toolCallbacks.get(i).getToolDefinition().description(),
                    "parameters", JsonParser.fromJson(toolCallbacks.get(i).getToolDefinition().inputSchema(),
                            new TypeReference<Map<String, Object>>() {})
            ));
            keyValues = keyValues.and(
                    SemanticConventions.LLM_TOOLS + "." + i + "." + SemanticConventions.TOOL_JSON_SCHEMA,
                    JsonParser.toJson(toolDefinition)
            );
        }

        List<String> toolNames = new ArrayList<>(options.getToolNames());
        for (int i = 0; i < toolNames.size(); i ++) {
            int index = i + toolCallbacks.size();
            Map<String,Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("function", Map.of(
                    "name", toolNames.get(i)
            ));
            keyValues = keyValues.and(
                    SemanticConventions.LLM_TOOLS + "." + index + "." + SemanticConventions.TOOL_JSON_SCHEMA,
                    JsonParser.toJson(toolDefinition)
            );
        }

        return keyValues;
    }

    // Response

    private KeyValues llmOutputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideOutputs() || openInferenceOptions.isHideOutputMessages()) {
            return keyValues.and(SemanticConventions.LLM_OUTPUT_MESSAGES, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        List<Generation> generations = new ArrayList<>(context.getResponse().getResults());

        for (int i = 0; i < generations.size(); i++) {
            AssistantMessage message = generations.get(i).getOutput();
            keyValues = keyValues.and(
                    SemanticConventions.LLM_OUTPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_ROLE,
                    message.getMessageType().getValue()
            );
            keyValues = addMessageContent(keyValues,
                    SemanticConventions.LLM_OUTPUT_MESSAGES + "." + i,
                    message,
                    openInferenceOptions.isHideOutputText(),
                    false);

            List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>(message.getToolCalls());
            String toolCallObservationPrefix = SemanticConventions.LLM_OUTPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_TOOL_CALLS;
            keyValues = addToolCalls(keyValues, toolCalls, toolCallObservationPrefix, openInferenceOptions.isHideOutputText());
        }

        return keyValues;
    }

    protected KeyValues responseFinishReasons(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && context.getResponse().getResult() != null) {
            var finishReason = context.getResponse().getResult().getMetadata().getFinishReason();
            if (StringUtils.hasText(finishReason)) {
                return keyValues.and(
                        SemanticConventions.LLM_FINISH_REASON,
                        finishReason);
            }
        }
        return keyValues;
    }

    protected KeyValues usageInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(
                    SemanticConventions.LLM_TOKEN_COUNT_PROMPT,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
        }
        return keyValues;
    }

    protected KeyValues usageOutputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(
                    SemanticConventions.LLM_TOKEN_COUNT_COMPLETION,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getCompletionTokens()));
        }
        return keyValues;
    }

    protected KeyValues usageTotalTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(
                    SemanticConventions.LLM_TOKEN_COUNT_TOTAL,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getTotalTokens()));
        }
        return keyValues;
    }

    // Utils

    private KeyValues addMessageContent(KeyValues keyValues, String messagePrefix, Message message,
                                         boolean hideText, boolean hideImages) {
        boolean hasMedia = message instanceof MediaContent mediaContent
                && !CollectionUtils.isEmpty(mediaContent.getMedia());

        if (!hasMedia) {
            if (hideText) {
                return keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_CONTENT,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER);
            }
            return keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_CONTENT,
                    message.getText() != null ? message.getText() : "");
        }

        String contentsPrefix = messagePrefix + "." + SemanticConventions.MESSAGE_CONTENTS;
        int contentIndex = 0;

        if (StringUtils.hasText(message.getText())) {
            String entryPrefix = contentsPrefix + "." + contentIndex;
            keyValues = keyValues.and(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TYPE, "text");
            if (hideText) {
                keyValues = keyValues.and(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TEXT,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER);
            } else {
                keyValues = keyValues.and(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TEXT,
                        message.getText());
            }
            contentIndex++;
        }

        for (Media media : ((MediaContent) message).getMedia()) {
            String entryPrefix = contentsPrefix + "." + contentIndex;
            String mediaType = media.getMimeType().getType();

            if ("image".equals(mediaType)) {
                keyValues = keyValues.and(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TYPE, "image");
                if (hideImages) {
                    keyValues = keyValues.and(
                            entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL,
                            OpenInferenceOptions.REDACTED_PLACEHOLDER);
                } else {
                    keyValues = keyValues.and(
                            entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL,
                            resolveMediaUrl(media));
                }
                contentIndex++;
            } else if ("audio".equals(mediaType)) {
                keyValues = keyValues.and(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TYPE, "audio");
                keyValues = keyValues.and(
                        entryPrefix + "." + "message_content.audio" + "." + SemanticConventions.AUDIO_URL,
                        resolveMediaUrl(media));
                contentIndex++;
            }
        }

        return keyValues;
    }

    private String resolveMediaUrl(Media media) {
        Object data = media.getData();
        if (data instanceof String url) {
            if (url.startsWith("data:") && url.contains(";base64,")) {
                return truncateBase64DataUri(url);
            }
            return url;
        } else if (data instanceof byte[] bytes) {
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUri = "data:" + media.getMimeType() + ";base64," + base64;
            return truncateBase64DataUri(dataUri);
        }
        return "";
    }

    private String truncateBase64DataUri(String dataUri) {
        long maxLength = openInferenceOptions.getBase64ImageMaxLength();
        if (maxLength > 0 && dataUri.length() > maxLength) {
            return dataUri.substring(0, (int) maxLength);
        }
        return dataUri;
    }

    private KeyValues addToolCalls(KeyValues keyValues, List<AssistantMessage.ToolCall> toolCalls, String observationPrefix, boolean hideText) {
        if (CollectionUtils.isEmpty(toolCalls)) {
            return keyValues;
        }
        for (int t = 0; t < toolCalls.size(); t++) {
            AssistantMessage.ToolCall toolCall = toolCalls.get(t);
            keyValues = keyValues.and(
                    observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_ID,
                    toolCall.id()
            );
            keyValues = keyValues.and(
                    observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_FUNCTION_NAME,
                    toolCall.name()
            );
            if (hideText) {
                keyValues = keyValues.and(
                        observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER
                );
            } else {
                keyValues = keyValues.and(
                        observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON,
                        toolCall.arguments()
                );
            }
        }
        return keyValues;
    }

}
