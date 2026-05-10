package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.opentelemetry.api.common.AttributeKey;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * LangSmith-specific attribute keys and event names for OpenTelemetry-based tracing.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith OpenTelemetry Tracing</a>
 */
public final class LangSmithAttributes {

    // LangSmith-specific attribute keys

    /** The run type for the span. Values: llm, chain, tool, retriever, embedding, prompt, parser. */
    public static final AttributeKey<String> LANGSMITH_SPAN_KIND = stringKey("langsmith.span.kind");

    /** Session identifier for grouping related traces. */
    public static final AttributeKey<String> LANGSMITH_TRACE_SESSION_ID = stringKey("langsmith.trace.session_id");

    // GenAI attribute keys (used by LangSmith, differ from OTel naming)

    /**
     * The GenAI system / provider name.
     * <p>
     * Note: LangSmith uses {@code gen_ai.system} (older naming), not {@code gen_ai.provider.name}.
     */
    public static final AttributeKey<String> GEN_AI_SYSTEM = stringKey("gen_ai.system");

    /** Total number of tokens used (input + output). */
    public static final AttributeKey<String> GEN_AI_USAGE_TOTAL_TOKENS = stringKey("gen_ai.usage.total_tokens");

    /** The input prompt or arguments sent to the model or tool. */
    public static final AttributeKey<String> GEN_AI_PROMPT = stringKey("gen_ai.prompt");

    /** The output completion or result from the model or tool. */
    public static final AttributeKey<String> GEN_AI_COMPLETION = stringKey("gen_ai.completion");

    // Tool attribute keys

    /** The list of tool definitions available to the GenAI model. */
    public static final AttributeKey<String> TOOLS = stringKey("tools");

    // Event names

    public static final String GEN_AI_SYSTEM_MESSAGE = "gen_ai.system.message";
    public static final String GEN_AI_USER_MESSAGE = "gen_ai.user.message";
    public static final String GEN_AI_ASSISTANT_MESSAGE = "gen_ai.assistant.message";
    public static final String GEN_AI_TOOL_MESSAGE = "gen_ai.tool.message";

    // Event attributes

    public static final AttributeKey<String> GEN_AI_EVENT_CONTENT = stringKey("gen_ai.event.content");

    private LangSmithAttributes() {
    }

}
