package io.arconia.ai.core.tools;

/**
 * Metadata for a tool.
 */
public interface ToolMetadata {

    /**
     * The tool name. Unique within the tool set provided to a model.
     */
    String getName();

    /**
     * The tool description, used by the model to decide if and when to use the tool.
     */
    String getDescription();

    /**
     * The JSON Schema of the parameters used to call the tool.
     */
    String getInputTypeSchema();

}
