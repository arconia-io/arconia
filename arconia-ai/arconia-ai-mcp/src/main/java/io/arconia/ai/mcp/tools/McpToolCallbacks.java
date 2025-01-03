package io.arconia.ai.mcp.tools;

import org.springframework.ai.mcp.client.McpAsyncClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.model.function.FunctionCallback;

import io.arconia.ai.core.tools.ToolCallback;

/**
 * Provides {@link ToolCallback} instances for tools defined in different sources.
 */
public final class McpToolCallbacks {

    static FunctionCallback[] from(McpSyncClient source) {
        return McpToolCallbackProvider.builder().mcpClients(source).build().getToolCallbacks();
    }

    static FunctionCallback[] from(McpAsyncClient source) {
        return McpToolCallbackProvider.builder().mcpClients(source).build().getToolCallbacks();
    }

}
