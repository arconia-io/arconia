package io.arconia.ai.core.tools;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public interface ToolCallbackProvider {

    ToolCallback[] getToolCallbacks();

}
