package io.arconia.ai.mcp.tools;

import org.springframework.ai.mcp.client.McpAsyncClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.core.tools.ToolCallback;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public final class McpToolCallbacks {

    public static FunctionCallback[] from(McpSyncClient... sources) {
        return McpToolCallbackProvider.builder().mcpClients(sources).build().getToolCallbacks();
    }

    public static FunctionCallback[] from(McpAsyncClient... sources) {
        return McpToolCallbackProvider.builder().mcpClients(sources).build().getToolCallbacks();
    }

}
