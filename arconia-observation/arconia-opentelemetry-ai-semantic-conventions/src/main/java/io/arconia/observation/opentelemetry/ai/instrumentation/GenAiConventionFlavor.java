package io.arconia.observation.opentelemetry.ai.instrumentation;

/**
 * The convention flavor to apply for GenAI observations.
 * <p>
 * Each flavor uses the OpenTelemetry GenAI semantic conventions as a base,
 * overriding only the attributes that differ from the OTel spec.
 */
public enum GenAiConventionFlavor {

    /**
     * OpenTelemetry GenAI semantic conventions.
     *
     * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/">OTel GenAI Semantic Conventions</a>
     */
    OPENTELEMETRY,

    /**
     * Traceloop OpenLLMetry semantic conventions.
     *
     * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
     */
    OPENLLMETRY,

    /**
     * LangSmith semantic conventions.
     *
     * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
     */
    LANGSMITH,

    /**
     * OpenLIT semantic conventions.
     *
     * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
     */
    OPENLIT,

    /**
     * Langfuse semantic conventions, on top of the OpenTelemetry GenAI semantic conventions.
     *
     * @see <a href="https://langfuse.com/integrations/native/opentelemetry">Langfuse OpenTelemetry</a>
     */
    LANGFUSE;

    /**
     * Whether the flavor captures the content of model calls and tool executions unless configured otherwise.
     * The OpenTelemetry flavor doesn't, as the conventions require, while the other flavors follow the
     * instrumentations of their platform, which capture it.
     */
    public boolean capturesContentByDefault() {
        return this != OPENTELEMETRY;
    }

}
