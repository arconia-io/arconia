package io.arconia.observation.openinference.ai.instrumentation;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ChatModelObservationConvention} for OpenInference.
 */
public final class OpenInferenceChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

    private final OpenInferenceOptions openInferenceOptions;

    public OpenInferenceChatModelObservationConvention(OpenInferenceOptions openInferenceOptions) {
        this.openInferenceOptions = openInferenceOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
        KeyValues keyValues = KeyValues.of(aiOperationType(context), aiProvider(context), llmSystem(context));
        String modelName = modelName(context);
        return modelName != null ? keyValues.and(SemanticConventions.LLM_MODEL_NAME, modelName) : keyValues;
    }

    @Override
    protected KeyValue aiOperationType(ChatModelObservationContext context) {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND,
                SemanticConventions.OpenInferenceSpanKind.LLM.getValue());
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_PROVIDER,
                OpenInferenceConventionsConverters.toLlmProvider(context.getOperationMetadata().provider()));
    }

    private KeyValue llmSystem(ChatModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_SYSTEM,
                OpenInferenceConventionsConverters.toLlmSystem(context.getOperationMetadata().provider()));
    }

    private @Nullable String modelName(ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return context.getResponse().getMetadata().getModel();
        } else if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return context.getRequest().getOptions().getModel();
        }
        return null;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = KeyValues.empty();

        // Request
        keyValues = inputValue(keyValues, context);
        keyValues = llmInputMessages(keyValues, context);
        keyValues = llmInvocationParameters(keyValues, context);
        keyValues = llmRequestModelName(keyValues, context);
        keyValues = llmTools(keyValues, context);

        // Response
        keyValues = outputValue(keyValues, context);
        keyValues = llmOutputMessages(keyValues, context);
        keyValues = llmResponseModelName(keyValues, context);
        keyValues = responseFinishReasons(keyValues, context);
        keyValues = responseId(keyValues, context);
        keyValues = usageInputTokens(keyValues, context);
        keyValues = usageOutputTokens(keyValues, context);
        keyValues = usageTotalTokens(keyValues, context);

        return keyValues;
    }

    // Request

    private KeyValues inputValue(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideInputMessages()
                || openInferenceOptions.isHideInputText()) {
            return keyValues.and(SemanticConventions.INPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER)
                    .and(SemanticConventions.INPUT_MIME_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        }
        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return keyValues;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Message message : context.getRequest().getInstructions()) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                toolResponseMessage.getResponses().forEach(response -> messages.add(toolResponseValue(response)));
            } else {
                messages.add(messageValue(message));
            }
        }
        return keyValues.and(SemanticConventions.INPUT_VALUE, jsonHelper.toJson(Map.of("messages", messages)))
                .and(SemanticConventions.INPUT_MIME_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
    }

    private KeyValues llmRequestModelName(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return keyValues.and(SemanticConventions.LLM_REQUEST_MODEL_NAME, context.getRequest().getOptions().getModel());
        }
        return keyValues;
    }

    private KeyValues llmInputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideInputMessages()) {
            return keyValues.and(SemanticConventions.LLM_INPUT_MESSAGES, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return keyValues;
        }

        int i = 0;
        for (Message message : context.getRequest().getInstructions()) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                    String messagePrefix = SemanticConventions.LLM_INPUT_MESSAGES + "." + i;
                    keyValues = keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_ROLE,
                            message.getMessageType().getValue());
                    keyValues = keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_TOOL_CALL_ID,
                            toolResponse.id());
                    keyValues = keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_NAME,
                            toolResponse.name());
                    keyValues = keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_CONTENT,
                            openInferenceOptions.isHideInputText() ? OpenInferenceOptions.REDACTED_PLACEHOLDER
                                    : toolResponse.responseData());
                    i++;
                }
                continue;
            }

            String messagePrefix = SemanticConventions.LLM_INPUT_MESSAGES + "." + i;
            keyValues = keyValues.and(messagePrefix + "." + SemanticConventions.MESSAGE_ROLE,
                    message.getMessageType().getValue());
            keyValues = addMessageContent(keyValues, messagePrefix, message,
                    openInferenceOptions.isHideInputText(), openInferenceOptions.isHideInputImages());

            if (message instanceof AssistantMessage assistantMessage) {
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>(assistantMessage.getToolCalls());
                String toolCallObservationPrefix = messagePrefix + "." + SemanticConventions.MESSAGE_TOOL_CALLS;
                keyValues = addToolCalls(keyValues, toolCalls, toolCallObservationPrefix, openInferenceOptions.isHideInputText());
            }
            i++;
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

        return keyValues.and(SemanticConventions.LLM_INVOCATION_PARAMETERS,  jsonHelper.toJson(invocationParameters));
    }

    private KeyValues llmTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideLlmTools()) {
            return keyValues.and(SemanticConventions.LLM_TOOLS, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return keyValues;
        }

        List<ToolCallback> toolCallbacks = Objects.requireNonNullElse(options.getToolCallbacks(), List.of());
        for (int i = 0; i < toolCallbacks.size(); i++) {
            Map<String,Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("function", Map.of(
                    "name", toolCallbacks.get(i).getToolDefinition().name(),
                    "description", toolCallbacks.get(i).getToolDefinition().description(),
                    "parameters", toolParameters(toolCallbacks.get(i).getToolDefinition().inputSchema())
            ));
            String toolPrefix = SemanticConventions.LLM_TOOLS + "." + i + ".";
            keyValues = keyValues.and(toolPrefix + SemanticConventions.TOOL_NAME,
                    toolCallbacks.get(i).getToolDefinition().name());
            if (StringUtils.hasText(toolCallbacks.get(i).getToolDefinition().description())) {
                keyValues = keyValues.and(toolPrefix + SemanticConventions.TOOL_DESCRIPTION,
                        toolCallbacks.get(i).getToolDefinition().description());
            }
            keyValues = keyValues.and(toolPrefix + SemanticConventions.TOOL_JSON_SCHEMA, jsonHelper.toJson(toolDefinition));
        }

        return keyValues;
    }

    /**
     * Parses the input schema of a tool into the JSON object it represents, recording a schema that isn't
     * valid JSON as the original string rather than failing the observation.
     */
    private Object toolParameters(String inputSchema) {
        try {
            return Objects.requireNonNullElse(jsonHelper.fromJson(inputSchema,
                    new ParameterizedTypeReference<Map<String, Object>>() {}), Map.of());
        } catch (RuntimeException ex) {
            return inputSchema;
        }
    }

    // Response

    private KeyValues outputValue(KeyValues keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideOutputs() || openInferenceOptions.isHideOutputMessages()
                || openInferenceOptions.isHideOutputText()) {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER)
                    .and(SemanticConventions.OUTPUT_MIME_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        }
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Generation generation : context.getResponse().getResults()) {
            messages.add(messageValue(generation.getOutput()));
        }
        return keyValues.and(SemanticConventions.OUTPUT_VALUE, jsonHelper.toJson(Map.of("messages", messages)))
                .and(SemanticConventions.OUTPUT_MIME_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
    }

    private KeyValues llmResponseModelName(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return keyValues.and(SemanticConventions.LLM_RESPONSE_MODEL_NAME, context.getResponse().getMetadata().getModel());
        }
        return keyValues;
    }

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

    @Override
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

    @Override
    protected KeyValues usageInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        Usage usage = reportedUsage(context);
        if (usage != null && usage.getPromptTokens() != null) {
            return keyValues.and(SemanticConventions.LLM_TOKEN_COUNT_PROMPT, String.valueOf(usage.getPromptTokens()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues usageOutputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        Usage usage = reportedUsage(context);
        if (usage != null && usage.getCompletionTokens() != null) {
            return keyValues.and(SemanticConventions.LLM_TOKEN_COUNT_COMPLETION, String.valueOf(usage.getCompletionTokens()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues usageTotalTokens(KeyValues keyValues, ChatModelObservationContext context) {
        Usage usage = reportedUsage(context);
        if (usage != null && usage.getTotalTokens() != null) {
            return keyValues.and(SemanticConventions.LLM_TOKEN_COUNT_TOTAL, String.valueOf(usage.getTotalTokens()));
        }
        return keyValues;
    }

    /**
     * Returns the token usage of the response when the provider reported it. Spring AI falls back to
     * {@link EmptyUsage} when it didn't, such as for streaming responses without usage, and its zeros
     * would show up as free calls in the token counts.
     */
    private static @Nullable Usage reportedUsage(ChatModelObservationContext context) {
        if (context.getResponse() == null) {
            return null;
        }
        Usage usage = context.getResponse().getMetadata().getUsage();
        return usage instanceof EmptyUsage ? null : usage;
    }

    // Utils

    /**
     * Builds the JSON representation of a message for {@code input.value} and {@code output.value}.
     * Media content is left out, since the messages attributes already carry it within the configured limits.
     */
    private Map<String, Object> messageValue(Message message) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", message.getMessageType().getValue());
        messageMap.put("content", Objects.requireNonNullElse(message.getText(), ""));
        if (message instanceof AssistantMessage assistantMessage && !CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                Map<String, Object> toolCallMap = new LinkedHashMap<>();
                toolCallMap.put("id", toolCall.id());
                toolCallMap.put("type", "function");
                toolCallMap.put("function", Map.of("name", toolCall.name(), "arguments", toolCall.arguments()));
                toolCalls.add(toolCallMap);
            }
            messageMap.put("tool_calls", toolCalls);
        }
        return messageMap;
    }

    private Map<String, Object> toolResponseValue(ToolResponseMessage.ToolResponse response) {
        Map<String, Object> messageMap = new LinkedHashMap<>();
        messageMap.put("role", "tool");
        messageMap.put("tool_call_id", response.id());
        messageMap.put("name", response.name());
        messageMap.put("content", response.responseData());
        return messageMap;
    }

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
                        entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_AUDIO + "." + SemanticConventions.AUDIO_URL,
                        resolveMediaUrl(media));
                keyValues = keyValues.and(
                        entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_AUDIO + "." + SemanticConventions.AUDIO_MIME_TYPE,
                        media.getMimeType().toString());
                contentIndex++;
            }
        }

        return keyValues;
    }

    private String resolveMediaUrl(Media media) {
        Object data = media.getData();
        if (data instanceof String url) {
            if (url.startsWith("data:") && url.contains(";base64,")) {
                return limitBase64DataUri(url);
            }
            return url;
        } else if (data instanceof byte[] bytes) {
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUri = "data:" + media.getMimeType() + ";base64," + base64;
            return limitBase64DataUri(dataUri);
        }
        return "";
    }

    /**
     * Redacts a base64 data URI longer than the configured maximum length, as the OpenInference
     * specification requires. A truncated data URI would be invalid.
     */
    private String limitBase64DataUri(String dataUri) {
        long maxLength = openInferenceOptions.getBase64ImageMaxLength();
        if (maxLength > 0 && dataUri.length() > maxLength) {
            return OpenInferenceOptions.REDACTED_PLACEHOLDER;
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
