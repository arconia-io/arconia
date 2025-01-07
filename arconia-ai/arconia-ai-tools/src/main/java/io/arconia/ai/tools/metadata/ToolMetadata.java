package io.arconia.ai.tools.metadata;

import java.lang.reflect.Method;

import io.arconia.ai.tools.execution.ToolExecutionMode;

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
     * Create {@link ToolMetadata} from a {@link Method}.
     */
    static ToolMetadata from(Method method) {
        return DefaultToolMetadata.from(method);
    }

}
