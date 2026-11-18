package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.opentelemetry.api.common.AttributeKey;

import io.arconia.core.annotations.Incubating;

import static io.opentelemetry.api.common.AttributeKey.longKey;

/**
 * LangSmith-specific attribute keys and event names for OpenTelemetry-based tracing.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith OpenTelemetry Tracing</a>
 */
@Incubating
public final class LangSmithAttributes {

    // LangSmith-specific attribute keys

    /** The run type for the span. Values: llm, chain, tool, retriever, embedding, prompt, parser. */
    public static final String LANGSMITH_SPAN_KIND = "langsmith.span.kind";

    /**
     * Thread identifier for grouping the traces of a conversation, recorded as the {@code thread_id} metadata.
     * <p>
     * Note: {@code langsmith.trace.session_id} identifies the LangSmith project, not a conversation.
     */
    public static final String LANGSMITH_METADATA_THREAD_ID = "langsmith.metadata.thread_id";

    // GenAI attribute keys (used by LangSmith, differ from OTel naming)

    /** The GenAI system / provider name. */
    public static final String GEN_AI_SYSTEM = "gen_ai.system";

    /** Total number of tokens used (input + output). */
    public static final AttributeKey<Long> GEN_AI_USAGE_TOTAL_TOKENS = longKey("gen_ai.usage.total_tokens");

    /** The inputs of the run as JSON, such as the input messages of a model call or the arguments of a tool. */
    public static final String GEN_AI_PROMPT = "gen_ai.prompt";

    /** The outputs of the run as JSON, such as the output messages of a model call or the result of a tool. */
    public static final String GEN_AI_COMPLETION = "gen_ai.completion";

    // Tool attribute keys

    /** The list of tool definitions available to the GenAI model. */
    public static final String TOOLS = "tools";

    private LangSmithAttributes() {
    }

}
