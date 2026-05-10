package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.util.json.JsonParser;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * LangSmith flavor of {@link OpenTelemetryChatModelObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithChatModelObservationConvention extends OpenTelemetryChatModelObservationConvention {

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
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(ChatModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!getProperties().isIncludeToolDefinitions()) {
            return keyValues;
        }
        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return keyValues;
        }
        return keyValues.and(LangSmithAttributes.TOOLS.getKey(), JsonParser.toJson(buildToolDefinitions(options)));
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
    protected KeyValues inputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith uses span events instead of span attributes for input messages.
        return keyValues;
    }

    @Override
    protected KeyValues outputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        // LangSmith uses span events instead of span attributes for output messages.
        return keyValues;
    }

}
