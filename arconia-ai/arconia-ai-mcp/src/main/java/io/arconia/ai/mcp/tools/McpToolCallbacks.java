package io.arconia.ai.mcp.tools;

import org.springframework.ai.mcp.client.McpAsyncClient;
import org.springframework.ai.mcp.client.McpSyncClient;

import io.arconia.ai.tools.ToolCallback;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public final class McpToolCallbacks {

    public static ToolCallback[] from(McpSyncClient... sources) {
        return McpToolCallbackProvider.builder().mcpClients(sources).build().getToolCallbacks();
    }

    public static ToolCallback[] from(McpAsyncClient... sources) {
        return McpToolCallbackProvider.builder().mcpClients(sources).build().getToolCallbacks();
    }

}
