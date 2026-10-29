package io.arconia.observation.openinference.ai.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.observation.ChatModelObservationContext;

/**
 * Observation handler that sets typed span attributes for chat model observations.
 * <p>
 * It records the cached input tokens as numbers directly on the OpenTelemetry span, since
 * Micrometer key values are always strings and Phoenix ignores the token count details
 * that aren't numbers when computing costs.
 * <p>
 * See <a href="https://github.com/micrometer-metrics/micrometer/issues/7433">micrometer#7433</a>.
 */
public final class OpenInferenceChatModelObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private static final AttributeKey<Long> LLM_TOKEN_COUNT_PROMPT_DETAILS_CACHE_READ =
            AttributeKey.longKey(SemanticConventions.LLM_TOKEN_COUNT_PROMPT_DETAILS_CACHE_READ);

    private static final AttributeKey<Long> LLM_TOKEN_COUNT_PROMPT_DETAILS_CACHE_WRITE =
            AttributeKey.longKey(SemanticConventions.LLM_TOKEN_COUNT_PROMPT_DETAILS_CACHE_WRITE);

    @Override
    public void onStop(ChatModelObservationContext context) {
        TracingObservationHandler.TracingContext tracingContext = context
                .get(TracingObservationHandler.TracingContext.class);
        if (tracingContext == null || tracingContext.getSpan() == null || context.getResponse() == null) {
            return;
        }
        Span span = OtelSpan.toOtel(tracingContext.getSpan());
        Usage usage = context.getResponse().getMetadata().getUsage();
        if (usage.getCacheReadInputTokens() != null) {
            span.setAttribute(LLM_TOKEN_COUNT_PROMPT_DETAILS_CACHE_READ, usage.getCacheReadInputTokens());
        }
        if (usage.getCacheWriteInputTokens() != null) {
            span.setAttribute(LLM_TOKEN_COUNT_PROMPT_DETAILS_CACHE_WRITE, usage.getCacheWriteInputTokens());
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

}
