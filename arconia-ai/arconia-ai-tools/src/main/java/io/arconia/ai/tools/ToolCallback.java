package io.arconia.ai.tools;

import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.metadata.ToolMetadata;

/**
 * Specialization of {@link FunctionCallback} to identify tools in Spring AI.
 */
public interface ToolCallback extends FunctionCallback {

    /**
     * Definition of the tool.
     */
    ToolDefinition getToolDefinition();

    /**
     * Metadata for the tool.
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
