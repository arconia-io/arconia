package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.opentelemetry.api.common.AttributeKey;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * Provides OpenTelemetry Semantic Conventions additional
 * attributes, event names, and metric names that are currently
 * part of the specification, but not included in the Java SDK.
 */
public final class GenAiMoreIncubatingAttributes {

    // Event names

    /**
     * Describes the details of a GenAI inference operation, including input and output messages.
     */
    public static final String GEN_AI_CLIENT_INFERENCE_OPERATION_DETAILS =
            "gen_ai.client.inference.operation.details";

    // Metric names

    /**
     * Number of input and output tokens used.
     */
    public static final String GEN_AI_CLIENT_TOKEN_USAGE =
            "gen_ai.client.token.usage";

    // Attribute keys

    /**
     * The chat history provided to the model as an input.
     */
    public static final AttributeKey<String> GEN_AI_INPUT_MESSAGES =
            stringKey("gen_ai.input.messages");

    /**
     * Messages returned by the model where each message represents a specific model response (choice, candidate).
     */
    public static final AttributeKey<String> GEN_AI_OUTPUT_MESSAGES =
            stringKey("gen_ai.output.messages");

    /**
     * The list of source system tool definitions available to the GenAI agent or model.
     */
    public static final AttributeKey<String> GEN_AI_TOOL_DEFINITIONS =
            stringKey("gen_ai.tool.definitions");

    /**
     * Parameters passed to the tool call.
     */
    public static final AttributeKey<String> GEN_AI_TOOL_CALL_ARGUMENTS =
            stringKey("gen_ai.tool.call.arguments");

    /**
     * The result returned by the tool call (if any and if execution was successful).
     */
    public static final AttributeKey<String> GEN_AI_TOOL_CALL_RESULT =
            stringKey("gen_ai.tool.call.result");

}
