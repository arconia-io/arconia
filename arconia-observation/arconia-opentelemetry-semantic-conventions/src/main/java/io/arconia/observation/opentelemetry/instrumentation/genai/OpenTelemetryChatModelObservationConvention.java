package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;

public final class OpenTelemetryChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private static final KeyValue REQUEST_MODEL_NONE = KeyValue
            .of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), KeyValue.NONE_VALUE);

    private static final KeyValue RESPONSE_MODEL_NONE = KeyValue
            .of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(), KeyValue.NONE_VALUE);

    private final OpenTelemetryGenAiOptions openTelemetryGenAiOptions;

    public OpenTelemetryChatModelObservationConvention(OpenTelemetryGenAiOptions openTelemetryGenAiOptions) {
        this.openTelemetryGenAiOptions = openTelemetryGenAiOptions;
    }

    @Override
    public String getContextualName(ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        String operationName = OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        if (options != null && StringUtils.hasText(options.getModel())) {
            return "%s %s".formatted(operationName, options.getModel());
        }
        return operationName;
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiOperationType(ChatModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValue requestModel(ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && StringUtils.hasText(options.getModel())) {
            return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(),
                    options.getModel());
        }
        return REQUEST_MODEL_NONE;
    }

    @Override
    protected KeyValue responseModel(ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(),
                    context.getResponse().getMetadata().getModel());
        }
        return RESPONSE_MODEL_NONE;
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = KeyValues.empty();
        // Request
        keyValues = requestFrequencyPenalty(keyValues, context);
        keyValues = requestMaxTokens(keyValues, context);
        keyValues = requestPresencePenalty(keyValues, context);
        keyValues = requestStopSequences(keyValues, context);
        keyValues = requestTemperature(keyValues, context);
        keyValues = requestTools(keyValues, context);
        keyValues = requestTopK(keyValues, context);
        keyValues = requestTopP(keyValues, context);
        // Response
        keyValues = responseFinishReasons(keyValues, context);
        keyValues = responseId(keyValues, context);
        keyValues = usageInputTokens(keyValues, context);
        keyValues = usageOutputTokens(keyValues, context);
        keyValues = usageTotalTokens(keyValues, context);
        // Content
        if (OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_ATTRIBUTES == openTelemetryGenAiOptions.getInference().getCaptureContent()) {
            keyValues = inputMessages(keyValues, context);
            keyValues = outputMessages(keyValues, context);
        }
        return keyValues;
    }

    // Request

    @Override
    protected KeyValues requestFrequencyPenalty(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && options.getFrequencyPenalty() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY.getKey(),
                    String.valueOf(options.getFrequencyPenalty()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestMaxTokens(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && options.getMaxTokens() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS.getKey(),
                    String.valueOf(options.getMaxTokens()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestPresencePenalty(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && options.getPresencePenalty() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY.getKey(),
                    String.valueOf(options.getPresencePenalty()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestStopSequences(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && !CollectionUtils.isEmpty(options.getStopSequences())) {
            StringJoiner stopSequencesJoiner = new StringJoiner(", ", "[", "]");
            options.getStopSequences().forEach(value -> stopSequencesJoiner.add("\"" + value + "\""));
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES.getKey(),
                    stopSequencesJoiner.toString());
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestTemperature(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && options.getTemperature() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE.getKey(),
                    String.valueOf(options.getTemperature()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!openTelemetryGenAiOptions.getInference().isIncludeToolDefinitions()) {
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
            toolDefinition.put("name", toolCallback.getToolDefinition().name());
            toolDefinition.put("description", toolCallback.getToolDefinition().description());
            toolDefinition.put("parameters", JsonParser.fromJson(toolCallback.getToolDefinition().inputSchema(),
                    new TypeReference<Map<String, Object>>() {}));
            toolDefinitions.add(toolDefinition);
        }

        List<String> toolNames = new ArrayList<>(options.getToolNames());
        for (String toolName : toolNames) {
            Map<String, Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("name", toolName);
            toolDefinitions.add(toolDefinition);
        }

        return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_DEFINITIONS.getKey(), JsonParser.toJson(toolDefinitions));
    }

    @Override
    protected KeyValues requestTopK(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && options.getTopK() != null) {
            return keyValues.and(GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_K.getKey(),
                    String.valueOf(options.getTopK().doubleValue()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestTopP(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options != null && options.getTopP() != null) {
            return keyValues.and(GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P.getKey(),
                    String.valueOf(options.getTopP()));
        }
        return keyValues;
    }

    // Response

    @Override
    protected KeyValues responseFinishReasons(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && !CollectionUtils.isEmpty(context.getResponse().getResults())) {
            var finishReasons = context.getResponse().getResults().stream()
                    .filter(generation -> StringUtils.hasText(generation.getMetadata().getFinishReason()))
                    .map(generation -> generation.getMetadata().getFinishReason())
                    .toList();
            if (CollectionUtils.isEmpty(finishReasons)) {
                return keyValues;
            }
            StringJoiner finishReasonsJoiner = new StringJoiner(", ", "[", "]");
            finishReasons.forEach(finishReason -> finishReasonsJoiner.add("\"" + finishReason + "\""));
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS.getKey(),
                    finishReasonsJoiner.toString());
        }
        return keyValues;
    }

    @Override
    protected KeyValues responseId(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getId())) {
            return keyValues.and(GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID.getKey(),
                    context.getResponse().getMetadata().getId());
        }
        return keyValues;
    }

    @Override
    protected KeyValues usageInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS.getKey(),
                    String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues usageOutputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS.getKey(),
                    String.valueOf(context.getResponse().getMetadata().getUsage().getCompletionTokens()));
        }
        return keyValues;
    }

    // Content

    private KeyValues inputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        List<Message> messages = context.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }

        var inputMessages = OpenTelemetryGenAiContent.fromMessages(messages);
        if (!inputMessages.isEmpty()) {
            return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_INPUT_MESSAGES.getKey(), JsonParser.toJson(inputMessages));
        }
        return keyValues;
    }

    private KeyValues outputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        var outputMessages = OpenTelemetryGenAiContent.fromGenerations(context.getResponse().getResults());
        if (!outputMessages.isEmpty()) {
            return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), JsonParser.toJson(outputMessages));
        }
        return keyValues;
    }

}
