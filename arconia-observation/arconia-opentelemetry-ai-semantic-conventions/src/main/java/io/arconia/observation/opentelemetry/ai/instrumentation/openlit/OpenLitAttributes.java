package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

/**
 * Semantic convention attribute keys for OpenLIT that differ from the OTel GenAI spec.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitAttributes {

    /**
     * The name of the AI provider.
     * <p>
     * Note: OpenLIT UI reads {@code gen_ai.system} for provider identification (e.g. cost
     * widget lookups), while the OTel spec uses {@code gen_ai.provider.name}.
     */
    public static final String GEN_AI_SYSTEM = "gen_ai.system";

    /**
     * Whether the request is a streaming request.
     * <p>
     * Note: OpenLIT TypeScript SDK uses {@code gen_ai.request.is_stream} (legacy key);
     * the Python SDK has already aligned to {@code gen_ai.request.stream}.
     */
    public static final String GEN_AI_REQUEST_IS_STREAM = "gen_ai.request.is_stream";

    /**
     * The number of dimensions requested for the embedding.
     * <p>
     * Note: The OpenLIT UI reads {@code gen_ai.request.embedding_dimension}
     * (underscore-separated), while the OTel base uses {@code gen_ai.request.embedding.dimensions}
     * (dot-separated).
     */
    public static final String GEN_AI_REQUEST_EMBEDDING_DIMENSION = "gen_ai.request.embedding_dimension";

    /**
     * The size of the generated image (e.g. {@code 1024x1024}).
     * <p>
     * Note: The OpenLIT UI reads {@code gen_ai.request.image_size} (underscore-separated),
     * while the OTel base uses {@code gen_ai.request.image.size} (dot-separated).
     */
    public static final String GEN_AI_REQUEST_IMAGE_SIZE = "gen_ai.request.image_size";

    /**
     * The style of the generated image.
     * <p>
     * Note: The OpenLIT UI reads {@code gen_ai.request.image_style} (underscore-separated),
     * while the OTel base uses {@code gen_ai.request.image.style} (dot-separated).
     */
    public static final String GEN_AI_REQUEST_IMAGE_STYLE = "gen_ai.request.image_style";

    /**
     * The tool call arguments.
     * <p>
     * Note: the OpenLIT UI TraceMapping uses {@code gen_ai.tool.args} (legacy alias);
     * the OTel spec uses {@code gen_ai.tool.call.arguments}.
     */
    public static final String GEN_AI_TOOL_ARGS = "gen_ai.tool.args";

    private OpenLitAttributes() {
    }

}
