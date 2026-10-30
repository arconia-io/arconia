package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

/**
 * Semantic convention attribute keys and values for OpenLLMetry (Traceloop).
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryAttributes {

    // Traceloop

    /** The span kind within the Traceloop workflow hierarchy. */
    public static final String TRACELOOP_SPAN_KIND = "traceloop.span.kind";

    /** The name of the workflow. */
    public static final String TRACELOOP_WORKFLOW_NAME = "traceloop.workflow.name";

    /** The name of the entity (e.g. a function or a module). */
    public static final String TRACELOOP_ENTITY_NAME = "traceloop.entity.name";

    /** The input to the entity. */
    public static final String TRACELOOP_ENTITY_INPUT = "traceloop.entity.input";

    /** The output of the entity. */
    public static final String TRACELOOP_ENTITY_OUTPUT = "traceloop.entity.output";

    /** Association properties for correlating traces (e.g. user ID, session ID). */
    public static final String TRACELOOP_ASSOCIATION_PROPERTIES = "traceloop.association.properties";

    /** The session the trace belongs to, as an association property. */
    public static final String TRACELOOP_ASSOCIATION_PROPERTIES_SESSION_ID = TRACELOOP_ASSOCIATION_PROPERTIES + ".session_id";

    // LLM (not in upstream OTel spec)

    public static final String GEN_AI_IS_STREAMING = "gen_ai.is_streaming";

    public static final String GEN_AI_USAGE_TOTAL_TOKENS = "gen_ai.usage.total_tokens";

    public enum TraceloopSpanKind {
        WORKFLOW("workflow"),
        TASK("task"),
        AGENT("agent"),
        TOOL("tool");

        private final String value;

        TraceloopSpanKind(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private OpenLLMetryAttributes() {
    }

}
