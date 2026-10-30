package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;

/**
 * Helpers to interpret the token usage reported by a model response.
 */
public final class GenAiUsage {

    private GenAiUsage() {}

    /**
     * Whether the provider reported the token usage of the response. Spring AI falls back to
     * {@link EmptyUsage} when it didn't, such as for streaming responses without usage, and
     * its zeros must not be recorded as if they were actual counts.
     */
    public static boolean isReported(@Nullable Usage usage) {
        return usage != null && !(usage instanceof EmptyUsage);
    }

}
