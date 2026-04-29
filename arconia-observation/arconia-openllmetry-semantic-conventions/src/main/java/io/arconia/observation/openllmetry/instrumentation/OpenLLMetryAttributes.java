package io.arconia.observation.openllmetry.instrumentation;

/**
 * Semantic convention attribute keys and values for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 * @see <a href="https://github.com/traceloop/openllmetry-js/tree/main/packages/ai-semantic-conventions">AI Semantic Conventions</a>
 */
public final class OpenLLMetryAttributes {

    // Traceloop-specific attributes

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

    // GenAI attributes (standard OTel GenAI semantic conventions)

    /**
     * The GenAI system / provider name.
     */
    public static final String GEN_AI_SYSTEM = "gen_ai.system";

    /**
     * The name of the GenAI operation.
     */
    public static final String GEN_AI_OPERATION_NAME = "gen_ai.operation.name";

    /**
     * The model requested by the client.
     */
    public static final String GEN_AI_REQUEST_MODEL = "gen_ai.request.model";

    /**
     * The model used in the response.
     */
    public static final String GEN_AI_RESPONSE_MODEL = "gen_ai.response.model";

    /**
     * The response identifier.
     */
    public static final String GEN_AI_RESPONSE_ID = "gen_ai.response.id";

    /**
     * The maximum number of tokens requested.
     */
    public static final String GEN_AI_REQUEST_MAX_TOKENS = "gen_ai.request.max_tokens";

    /**
     * The temperature setting for the request.
     */
    public static final String GEN_AI_REQUEST_TEMPERATURE = "gen_ai.request.temperature";

    /**
     * The top-p (nucleus sampling) setting.
     */
    public static final String GEN_AI_REQUEST_TOP_P = "gen_ai.request.top_p";

    /**
     * The top-k sampling setting.
     */
    public static final String GEN_AI_REQUEST_TOP_K = "gen_ai.request.top_k";

    /**
     * The frequency penalty setting.
     */
    public static final String GEN_AI_REQUEST_FREQUENCY_PENALTY = "gen_ai.request.frequency_penalty";

    /**
     * The presence penalty setting.
     */
    public static final String GEN_AI_REQUEST_PRESENCE_PENALTY = "gen_ai.request.presence_penalty";

    /**
     * The stop sequences for the request.
     */
    public static final String GEN_AI_REQUEST_STOP_SEQUENCES = "gen_ai.request.stop_sequences";

    /**
     * The number of input tokens used.
     */
    public static final String GEN_AI_USAGE_INPUT_TOKENS = "gen_ai.usage.input_tokens";

    /**
     * The number of output tokens used.
     */
    public static final String GEN_AI_USAGE_OUTPUT_TOKENS = "gen_ai.usage.output_tokens";

    /**
     * The total number of tokens used (input + output).
     */
    public static final String GEN_AI_USAGE_TOTAL_TOKENS = "gen_ai.usage.total_tokens";

    /**
     * The finish reasons for the response.
     */
    public static final String GEN_AI_RESPONSE_FINISH_REASONS = "gen_ai.response.finish_reasons";

    // LLM request type (deprecated, for backwards compatibility)

    /**
     * The type of the LLM request. Deprecated in favor of {@link #GEN_AI_OPERATION_NAME}.
     */
    public static final String LLM_REQUEST_TYPE = "llm.request.type";

    // Traceloop span kind values

    public static final String SPAN_KIND_WORKFLOW = "workflow";

    public static final String SPAN_KIND_TASK = "task";

    public static final String SPAN_KIND_AGENT = "agent";

    public static final String SPAN_KIND_TOOL = "tool";

    public static final String SPAN_KIND_SESSION = "session";

    // LLM request type values

    public static final String LLM_REQUEST_TYPE_CHAT = "chat";

    public static final String LLM_REQUEST_TYPE_COMPLETION = "completion";

    // GenAI operation name values

    public static final String OPERATION_CHAT = "chat";

    public static final String OPERATION_TEXT_COMPLETION = "text_completion";

    public static final String OPERATION_EMBEDDINGS = "embeddings";

    // GenAI provider name values

    public static final String PROVIDER_OPENAI = "openai";

    public static final String PROVIDER_ANTHROPIC = "anthropic";

    public static final String PROVIDER_AWS_BEDROCK = "aws.bedrock";

    public static final String PROVIDER_GCP_VERTEX_AI = "gcp.vertex_ai";

    public static final String PROVIDER_GCP_GEN_AI = "gcp.gen_ai";

    public static final String PROVIDER_AZURE_AI_OPENAI = "azure.ai.openai";

    public static final String PROVIDER_DEEPSEEK = "deepseek";

    public static final String PROVIDER_MISTRAL_AI = "mistral_ai";

    private OpenLLMetryAttributes() {
    }

}
