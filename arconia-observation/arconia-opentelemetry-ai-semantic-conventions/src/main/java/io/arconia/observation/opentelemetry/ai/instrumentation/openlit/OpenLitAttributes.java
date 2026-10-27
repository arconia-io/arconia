package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

/**
 * Semantic convention attribute keys for OpenLIT that differ from the OTel GenAI spec.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitAttributes {

    /**
     * Whether the request is a streaming request.
     * <p>
     * Note: the OpenLIT UI reads {@code gen_ai.request.is_stream}, although the OpenLIT
     * Python SDK has moved to {@code gen_ai.request.stream}.
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

    private OpenLitAttributes() {
    }

}
