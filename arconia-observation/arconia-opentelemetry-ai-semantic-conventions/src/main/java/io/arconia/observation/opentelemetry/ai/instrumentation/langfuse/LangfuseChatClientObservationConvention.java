package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;

/**
 * Langfuse flavor of {@link OpenTelemetryChatClientObservationConvention}.
 *
 * @see <a href="https://langfuse.com/integrations/native/opentelemetry">Langfuse OpenTelemetry</a>
 */
public final class LangfuseChatClientObservationConvention extends OpenTelemetryChatClientObservationConvention {

    public LangfuseChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        return true;
    }

}
