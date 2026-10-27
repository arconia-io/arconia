package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.CollectionUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * LangSmith flavor of {@link OpenTelemetryChatModelObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithChatModelObservationConvention extends OpenTelemetryChatModelObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

    public LangSmithChatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                GenAiConventionsConverter.toLangSmithProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(ChatModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!getProperties().isCaptureToolDefinitions()) {
            return keyValues;
        }
        var toolDefinitions = GenAiContent.toolDefinitions(context.getRequest().getOptions());
        if (toolDefinitions.isEmpty()) {
            return keyValues;
        }
        return keyValues.and(LangSmithAttributes.TOOLS.getKey(), jsonHelper.toJson(toolDefinitions));
    }

    @Override
    protected KeyValues usageInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith requires integer input tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

    @Override
    protected KeyValues usageOutputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith requires integer output tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

    @Override
    protected KeyValues usageTotalTokens(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith requires integer total tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

    @Override
    protected KeyValues usageCacheReadInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith requires integer cache read tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

    @Override
    protected KeyValues usageCacheWriteInputTokens(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith requires integer cache creation tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        // LangSmith records content on span attributes only.
        return true;
    }

    @Override
    protected KeyValues inputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith reads the inputs of the run from gen_ai.prompt, as the LangSmith SDKs record them.
        // Message events would place the assistant and tool messages of the conversation among the outputs.
        List<Message> messages = context.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }
        return keyValues.and(LangSmithAttributes.GEN_AI_PROMPT.getKey(),
                jsonHelper.toJson(LangSmithMessages.fromMessages(messages)));
    }

    @Override
    protected KeyValues outputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }
        return keyValues.and(LangSmithAttributes.GEN_AI_COMPLETION.getKey(),
                jsonHelper.toJson(LangSmithMessages.fromGenerations(context.getResponse().getResults())));
    }

}
