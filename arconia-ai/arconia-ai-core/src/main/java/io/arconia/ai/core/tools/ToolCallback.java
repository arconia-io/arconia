package io.arconia.ai.core.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.core.tools.metadata.ToolMetadata;

/**
 * Specialization of {@link FunctionCallback} to identify tools in Spring AI.
 */
public interface ToolCallback extends FunctionCallback {

    /**
     * Get the metadata for the tool.
     */
    ToolMetadata getToolMetadata();

    /**
     * Call the tool with the given input.
     */
    String call(String toolInput);

    /**
     * Call the tool with the given input and context.
     */
    String call(String toolInput, ToolContext tooContext);

}
