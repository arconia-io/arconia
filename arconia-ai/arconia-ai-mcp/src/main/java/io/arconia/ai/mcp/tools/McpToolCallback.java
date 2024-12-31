package io.arconia.ai.mcp.tools;

import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spring.McpFunctionCallback;

import io.arconia.ai.core.tools.ToolCallback;

/**
 * A {@link ToolCallback} for handling calls to MCP tools.
 */
public class McpToolCallback extends McpFunctionCallback implements ToolCallback {

    public McpToolCallback(McpSyncClient clientSession, McpSchema.Tool tool) {
        super(clientSession, tool);
    }

}
