package io.arconia.ai.mcp.tools;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.model.ModelOptionsUtils;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.json.JsonParser;
import io.arconia.ai.core.tools.metadata.DefaultToolMetadata;
import io.arconia.ai.core.tools.metadata.ToolMetadata;

/**
 * A {@link ToolCallback} for handling calls to MCP tools.
 */
public class McpToolCallback implements ToolCallback {

    private final ToolMetadata toolMetadata;
    private final McpSyncClient mcpClient;

    public McpToolCallback(McpSchema.Tool tool, McpSyncClient mcpClient) {
        this.toolMetadata = DefaultToolMetadata.builder()
            .name(tool.name())
            .description(tool.description())
            .inputTypeSchema(JsonParser.toJson(tool.inputSchema()))
            .build();
        this.mcpClient = mcpClient;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return this.toolMetadata;
    }

    @Override
    public String call(String toolInput) {
        Map<String, Object> arguments = JsonParser.fromJson(toolInput, new TypeReference<>() {});
        McpSchema.CallToolResult response = this.mcpClient.callTool(new McpSchema.CallToolRequest(this.getName(), arguments));
        return ModelOptionsUtils.toJsonString(response.content());
    }

}
