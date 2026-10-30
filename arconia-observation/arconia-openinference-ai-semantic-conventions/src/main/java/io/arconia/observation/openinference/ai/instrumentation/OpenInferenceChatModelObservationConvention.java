package io.arconia.observation.openinference.ai.instrumentation;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.arize.semconv.trace.SemanticConventions;
import tools.jackson.databind.util.RawValue;

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
        List<KeyValue> keyValues = new ArrayList<>();

        // Request
        inputValue(keyValues, context);
        llmInputMessages(keyValues, context);
        llmInvocationParameters(keyValues, context);
        llmRequestModelName(keyValues, context);
        llmTools(keyValues, context);

        // Response
        outputValue(keyValues, context);
        llmOutputMessages(keyValues, context);
        llmResponseModelName(keyValues, context);
        KeyValues responseKeyValues = responseFinishReasons(KeyValues.empty(), context);
        responseKeyValues = responseId(responseKeyValues, context);
        responseKeyValues = usageInputTokens(responseKeyValues, context);
        responseKeyValues = usageOutputTokens(responseKeyValues, context);
        responseKeyValues = usageTotalTokens(responseKeyValues, context);
        responseKeyValues.forEach(keyValues::add);

        return KeyValues.of(keyValues);
    }

    // Request

    private void inputValue(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideInputMessages()
                || openInferenceOptions.isHideInputText()) {
            keyValues.add(KeyValue.of(SemanticConventions.INPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER));
            keyValues.add(KeyValue.of(SemanticConventions.INPUT_MIME_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE));
            return;
        }
        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Message message : context.getRequest().getInstructions()) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                toolResponseMessage.getResponses().forEach(response -> messages.add(toolResponseValue(response)));
            } else {
                messages.add(messageValue(message));
            }
        }
        keyValues.add(KeyValue.of(SemanticConventions.INPUT_VALUE, jsonHelper.toJson(Map.of("messages", messages))));
        keyValues.add(KeyValue.of(SemanticConventions.INPUT_MIME_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE));
    }

    private void llmRequestModelName(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            keyValues.add(KeyValue.of(SemanticConventions.LLM_REQUEST_MODEL_NAME, context.getRequest().getOptions().getModel()));
        }
    }

    private void llmInputMessages(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideInputMessages()) {
            keyValues.add(KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES, OpenInferenceOptions.REDACTED_PLACEHOLDER));
            return;
        }

        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return;
        }

        int i = 0;
        for (Message message : context.getRequest().getInstructions()) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                    String messagePrefix = SemanticConventions.LLM_INPUT_MESSAGES + "." + i;
                    keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_ROLE,
                            message.getMessageType().getValue()));
                    keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_TOOL_CALL_ID,
                            toolResponse.id()));
                    keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_NAME,
                            toolResponse.name()));
                    keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_CONTENT,
                            openInferenceOptions.isHideInputText() ? OpenInferenceOptions.REDACTED_PLACEHOLDER
                                    : toolResponse.responseData()));
                    i++;
                }
                continue;
            }

            String messagePrefix = SemanticConventions.LLM_INPUT_MESSAGES + "." + i;
            keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_ROLE,
                    message.getMessageType().getValue()));
            addMessageContent(keyValues, messagePrefix, message,
                    openInferenceOptions.isHideInputText(), openInferenceOptions.isHideInputImages());

            if (message instanceof AssistantMessage assistantMessage) {
                String toolCallObservationPrefix = messagePrefix + "." + SemanticConventions.MESSAGE_TOOL_CALLS;
                addToolCalls(keyValues, assistantMessage.getToolCalls(), toolCallObservationPrefix, openInferenceOptions.isHideInputText());
            }
            i++;
        }
    }

    private void llmInvocationParameters(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideLlmInvocationParameters()) {
            keyValues.add(KeyValue.of(SemanticConventions.LLM_INVOCATION_PARAMETERS, OpenInferenceOptions.REDACTED_PLACEHOLDER));
            return;
        }

        ChatOptions options = context.getRequest().getOptions();
        if (options == null) {
            return;
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

        keyValues.add(KeyValue.of(SemanticConventions.LLM_INVOCATION_PARAMETERS,  jsonHelper.toJson(invocationParameters)));
    }

    private void llmTools(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideLlmTools()) {
            keyValues.add(KeyValue.of(SemanticConventions.LLM_TOOLS, OpenInferenceOptions.REDACTED_PLACEHOLDER));
            return;
        }

        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return;
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
            keyValues.add(KeyValue.of(toolPrefix + SemanticConventions.TOOL_NAME,
                    toolCallbacks.get(i).getToolDefinition().name()));
            if (StringUtils.hasText(toolCallbacks.get(i).getToolDefinition().description())) {
                keyValues.add(KeyValue.of(toolPrefix + SemanticConventions.TOOL_DESCRIPTION,
                        toolCallbacks.get(i).getToolDefinition().description()));
            }
            keyValues.add(KeyValue.of(toolPrefix + SemanticConventions.TOOL_JSON_SCHEMA, jsonHelper.toJson(toolDefinition)));
        }
    }

    /**
     * Embeds the input schema of a tool in the tool definition as it is, since it's already JSON.
     * A schema that isn't valid JSON is recorded as a string rather than failing the observation.
     */
    private static Object toolParameters(String inputSchema) {
        return OpenInferenceJson.isValid(inputSchema) ? new RawValue(inputSchema) : inputSchema;
    }

    // Response

    private void outputValue(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideOutputs() || openInferenceOptions.isHideOutputMessages()
                || openInferenceOptions.isHideOutputText()) {
            keyValues.add(KeyValue.of(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER));
            keyValues.add(KeyValue.of(SemanticConventions.OUTPUT_MIME_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE));
            return;
        }
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Generation generation : context.getResponse().getResults()) {
            messages.add(messageValue(generation.getOutput()));
        }
        keyValues.add(KeyValue.of(SemanticConventions.OUTPUT_VALUE, jsonHelper.toJson(Map.of("messages", messages))));
        keyValues.add(KeyValue.of(SemanticConventions.OUTPUT_MIME_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE));
    }

    private void llmResponseModelName(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            keyValues.add(KeyValue.of(SemanticConventions.LLM_RESPONSE_MODEL_NAME, context.getResponse().getMetadata().getModel()));
        }
    }

    private void llmOutputMessages(List<KeyValue> keyValues, ChatModelObservationContext context) {
        if (openInferenceOptions.isHideOutputs() || openInferenceOptions.isHideOutputMessages()) {
            keyValues.add(KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES, OpenInferenceOptions.REDACTED_PLACEHOLDER));
            return;
        }

        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return;
        }

        List<Generation> generations = context.getResponse().getResults();
        for (int i = 0; i < generations.size(); i++) {
            AssistantMessage message = generations.get(i).getOutput();
            keyValues.add(KeyValue.of(
                    SemanticConventions.LLM_OUTPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_ROLE,
                    message.getMessageType().getValue()
            ));
            addMessageContent(keyValues,
                    SemanticConventions.LLM_OUTPUT_MESSAGES + "." + i,
                    message,
                    openInferenceOptions.isHideOutputText(),
                    false);

            String toolCallObservationPrefix = SemanticConventions.LLM_OUTPUT_MESSAGES + "." + i + "." + SemanticConventions.MESSAGE_TOOL_CALLS;
            addToolCalls(keyValues, message.getToolCalls(), toolCallObservationPrefix, openInferenceOptions.isHideOutputText());
        }
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

    private void addMessageContent(List<KeyValue> keyValues, String messagePrefix, Message message,
                                   boolean hideText, boolean hideImages) {
        boolean hasMedia = message instanceof MediaContent mediaContent
                && !CollectionUtils.isEmpty(mediaContent.getMedia());

        if (!hasMedia) {
            if (hideText) {
                keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_CONTENT,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER));
                return;
            }
            keyValues.add(KeyValue.of(messagePrefix + "." + SemanticConventions.MESSAGE_CONTENT,
                    message.getText() != null ? message.getText() : ""));
            return;
        }

        String contentsPrefix = messagePrefix + "." + SemanticConventions.MESSAGE_CONTENTS;
        int contentIndex = 0;

        if (StringUtils.hasText(message.getText())) {
            String entryPrefix = contentsPrefix + "." + contentIndex;
            keyValues.add(KeyValue.of(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TYPE, "text"));
            if (hideText) {
                keyValues.add(KeyValue.of(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TEXT,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER));
            } else {
                keyValues.add(KeyValue.of(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TEXT,
                        message.getText()));
            }
            contentIndex++;
        }

        for (Media media : ((MediaContent) message).getMedia()) {
            String entryPrefix = contentsPrefix + "." + contentIndex;
            String mediaType = media.getMimeType().getType();

            if ("image".equals(mediaType)) {
                keyValues.add(KeyValue.of(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TYPE, "image"));
                if (hideImages) {
                    keyValues.add(KeyValue.of(
                            entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL,
                            OpenInferenceOptions.REDACTED_PLACEHOLDER));
                } else {
                    keyValues.add(KeyValue.of(
                            entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL,
                            resolveMediaUrl(media)));
                }
                contentIndex++;
            } else if ("audio".equals(mediaType)) {
                keyValues.add(KeyValue.of(entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_TYPE, "audio"));
                keyValues.add(KeyValue.of(
                        entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_AUDIO + "." + SemanticConventions.AUDIO_URL,
                        resolveMediaUrl(media)));
                keyValues.add(KeyValue.of(
                        entryPrefix + "." + SemanticConventions.MESSAGE_CONTENT_AUDIO + "." + SemanticConventions.AUDIO_MIME_TYPE,
                        media.getMimeType().toString()));
                contentIndex++;
            }
        }
    }

    private String resolveMediaUrl(Media media) {
        Object data = media.getData();
        if (data instanceof String url) {
            if (url.startsWith("data:") && url.contains(";base64,")) {
                return limitBase64DataUri(url);
            }
            return url;
        } else if (data instanceof byte[] bytes) {
            String prefix = "data:" + media.getMimeType() + ";base64,";
            long maxLength = openInferenceOptions.getBase64ImageMaxLength();
            int encodedLength = 4 * ((bytes.length + 2) / 3);
            if (maxLength > 0 && prefix.length() + encodedLength > maxLength) {
                return OpenInferenceOptions.REDACTED_PLACEHOLDER;
            }
            return prefix + Base64.getEncoder().encodeToString(bytes);
        }
        return "";
    }

    /**
     * Redacts a base64 data URI longer than the configured maximum length, as the OpenInference
     * specification requires. A truncated data URI would be invalid. Binary media is checked before
     * being encoded, see {@link #resolveMediaUrl(Media)}.
     */
    private String limitBase64DataUri(String dataUri) {
        long maxLength = openInferenceOptions.getBase64ImageMaxLength();
        if (maxLength > 0 && dataUri.length() > maxLength) {
            return OpenInferenceOptions.REDACTED_PLACEHOLDER;
        }
        return dataUri;
    }

    private void addToolCalls(List<KeyValue> keyValues, List<AssistantMessage.ToolCall> toolCalls, String observationPrefix, boolean hideText) {
        if (CollectionUtils.isEmpty(toolCalls)) {
            return;
        }
        for (int t = 0; t < toolCalls.size(); t++) {
            AssistantMessage.ToolCall toolCall = toolCalls.get(t);
            keyValues.add(KeyValue.of(
                    observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_ID,
                    toolCall.id()
            ));
            keyValues.add(KeyValue.of(
                    observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_FUNCTION_NAME,
                    toolCall.name()
            ));
            if (hideText) {
                keyValues.add(KeyValue.of(
                        observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER
                ));
            } else {
                keyValues.add(KeyValue.of(
                        observationPrefix + "." + t + "." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON,
                        toolCall.arguments()
                ));
            }
        }
    }

}
