package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;

/**
 * Langfuse flavor of {@link OpenTelemetryChatModelObservationConvention}.
 *
 * @see <a href="https://langfuse.com/integrations/native/opentelemetry">Langfuse OpenTelemetry</a>
 */
public final class LangfuseChatModelObservationConvention extends OpenTelemetryChatModelObservationConvention {

    public LangfuseChatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        return true;
    }

}
