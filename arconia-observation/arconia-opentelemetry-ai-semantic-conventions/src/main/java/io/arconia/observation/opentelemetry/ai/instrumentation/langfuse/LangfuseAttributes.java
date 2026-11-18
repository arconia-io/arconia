package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import io.arconia.core.annotations.Incubating;

/**
 * Langfuse-specific attribute keys for OpenTelemetry-based tracing.
 *
 * @see <a href="https://langfuse.com/integrations/native/opentelemetry">Langfuse OpenTelemetry</a>
 */
@Incubating
public final class LangfuseAttributes {

    /**
     * The type of the Langfuse observation, for the observations whose type can't be derived
     * from the OpenTelemetry Semantic Conventions for Generative AI, such as {@code chain}.
     */
    public static final String LANGFUSE_OBSERVATION_TYPE = "langfuse.observation.type";

    private LangfuseAttributes() {
    }

}
