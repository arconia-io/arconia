package io.arconia.ai.core.tools;

import org.springframework.ai.model.function.FunctionCallback;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public interface ToolCallbackProvider {

    FunctionCallback[] getToolCallbacks();

}
