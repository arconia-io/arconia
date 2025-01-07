package io.arconia.ai.tools;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public interface ToolCallbackProvider {

    ToolCallback[] getToolCallbacks();

}
