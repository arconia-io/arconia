package io.arconia.ai.core.tools;

import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.core.tools.metadata.ToolMetadata;

/**
 * Specialization of {@link FunctionCallback} to identify tools in Spring AI.
 */
public interface ToolCallback extends FunctionCallback {

    /**
     * Metadata for the tool.
     */
    ToolMetadata getToolMetadata();

    @Override
    default String getName() {
        return getToolMetadata().name();
    }

    @Override
    default String getDescription() {
        return getToolMetadata().description();
    }

    @Override
    default String getInputTypeSchema() {
        return getToolMetadata().inputTypeSchema();
    }

}
