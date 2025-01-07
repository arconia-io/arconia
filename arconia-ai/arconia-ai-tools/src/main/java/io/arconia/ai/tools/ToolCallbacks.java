package io.arconia.ai.tools;

import io.arconia.ai.tools.method.MethodToolCallbackProvider;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public final class ToolCallbacks {

    private ToolCallbacks() {}

    public static ToolCallback[] from(Object... sources) {
        return MethodToolCallbackProvider.builder().toolObjects(sources).build().getToolCallbacks();
    }

}
