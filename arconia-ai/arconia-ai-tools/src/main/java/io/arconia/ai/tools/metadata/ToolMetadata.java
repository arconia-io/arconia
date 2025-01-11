package io.arconia.ai.tools.metadata;

import java.lang.reflect.Method;

import io.arconia.ai.tools.execution.ToolExecutionMode;
import io.arconia.ai.tools.util.ToolUtils;

/**
 * Metadata about a tool specification and execution.
 */
public interface ToolMetadata {

    /**
     * How the tool should be executed.
     */
    default ToolExecutionMode executionMode() {
        return ToolExecutionMode.BLOCKING;
    }

    /**
     * Whether the tool result should be returned directly or passed back to the model.
     */
    default boolean returnDirect() {
        return false;
    }

    /**
     * Create a default {@link ToolMetadata} builder.
     */
    static DefaultToolMetadata.Builder builder() {
        return DefaultToolMetadata.builder();
    }

    /**
     * Create a default {@link ToolMetadata} instance from a {@link Method}.
     */
    static ToolMetadata from(Method method) {
        return DefaultToolMetadata.builder()
                .executionMode(ToolUtils.getToolExecutionMode(method))
                .returnDirect(ToolUtils.getToolReturnDirect(method))
                .build();
    }

}
