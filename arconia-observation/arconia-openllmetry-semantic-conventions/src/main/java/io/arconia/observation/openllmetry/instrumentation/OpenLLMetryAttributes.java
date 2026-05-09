package io.arconia.observation.openllmetry.instrumentation;

/**
 * Semantic convention attribute keys and values for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 * @see <a href="https://github.com/traceloop/openllmetry-js/tree/main/packages/ai-semantic-conventions">AI Semantic Conventions</a>
 */
public final class OpenLLMetryAttributes {

    // Traceloop

    /**
     * The span kind within the Traceloop workflow hierarchy.
     */
    public static final String TRACELOOP_SPAN_KIND = "traceloop.span.kind";

    /**
     * The name of the workflow.
     */
    public static final String TRACELOOP_WORKFLOW_NAME = "traceloop.workflow.name";

    /**
     * The name of the entity (e.g. a function or a module).
     */
    public static final String TRACELOOP_ENTITY_NAME = "traceloop.entity.name";

    /**
     * The path of the entity in the workflow hierarchy.
     */
    public static final String TRACELOOP_ENTITY_PATH = "traceloop.entity.path";

    /**
     * The version of the entity.
     */
    public static final String TRACELOOP_ENTITY_VERSION = "traceloop.entity.version";

    /**
     * The input to the entity.
     */
    public static final String TRACELOOP_ENTITY_INPUT = "traceloop.entity.input";

    /**
     * The output of the entity.
     */
    public static final String TRACELOOP_ENTITY_OUTPUT = "traceloop.entity.output";

    /**
     * Association properties for correlating traces (e.g. user ID, session ID).
     */
    public static final String TRACELOOP_ASSOCIATION_PROPERTIES = "traceloop.association.properties";

    // LLM (not in upstream OTel spec)

    /**
     * The GenAI system / provider name.
     */
    public static final String GEN_AI_SYSTEM = "gen_ai.system";

    public static final String GEN_AI_TOOL_DEFINITIONS = "gen_ai.tool.definitions";

    public static final String GEN_AI_IS_STREAMING = "gen_ai.is_streaming";

    public static final String GEN_AI_INPUT_MESSAGES = "gen_ai.input.messages";

    public static final String GEN_AI_OUTPUT_MESSAGES = "gen_ai.output.messages";

    public enum TraceloopSpanKind {
        WORKFLOW("workflow"),
        TASK("task"),
        AGENT("agent"),
        TOOL("tool"),
        SESSION("session");

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
