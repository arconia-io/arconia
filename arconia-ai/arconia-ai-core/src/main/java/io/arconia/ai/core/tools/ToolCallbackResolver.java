package io.arconia.ai.core.tools;

import org.springframework.ai.model.function.FunctionCallback;

/**
 * Resolves {@link ToolCallback} instances from different sources.
 */
public interface ToolCallbackResolver {

    FunctionCallback[] getToolCallbacks();

}
