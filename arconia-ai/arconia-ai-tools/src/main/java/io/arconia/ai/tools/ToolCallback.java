package io.arconia.ai.tools;

import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.metadata.ToolMetadata;

/**
 * Represents a tool whose execution can be triggered by an AI model.
 */
public interface ToolCallback extends FunctionCallback {

    /**
     * Definition used by the AI model to determine when and how to call the tool.
     */
    ToolDefinition getToolDefinition();

    /**
     * Metadata providing additional information on how to handle the tool.
     */
    default ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    default String getName() {
        return getToolDefinition().name();
    }

    @Override
    default String getDescription() {
        return getToolDefinition().description();
    }

    @Override
    default String getInputTypeSchema() {
        return getToolDefinition().inputTypeSchema();
    }

}
