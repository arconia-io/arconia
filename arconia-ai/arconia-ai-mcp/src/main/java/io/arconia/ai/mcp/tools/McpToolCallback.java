package io.arconia.ai.mcp.tools;

import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.Nullable;

import io.arconia.ai.tools.ToolCallback;
import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.json.JsonParser;

/**
 * A {@link ToolCallback} for handling calls to MCP tools.
 */
public class McpToolCallback implements ToolCallback {

    private static final Function<String,Map<String,Object>> DEFAULT_TOOL_INPUT_PARSER =
            toolInput -> JsonParser.fromJson(toolInput, new TypeReference<>() {});

    private final ToolDefinition toolDefinition;
    private final McpSyncClient mcpClient;
    private final Function<String,Map<String,Object>> toolInputParser;

    public McpToolCallback(McpSchema.Tool tool, McpSyncClient mcpClient, @Nullable Function<String,Map<String,Object>> toolInputParser) {
        this.toolDefinition = ToolDefinition.builder()
            .name(tool.name())
            .description(tool.description())
            .inputTypeSchema(JsonParser.toJson(tool.inputSchema()))
            .build();
        this.mcpClient = mcpClient;
        this.toolInputParser = toolInputParser != null ? toolInputParser : DEFAULT_TOOL_INPUT_PARSER;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        Map<String, Object> arguments = toolInputParser.apply(toolInput);
        McpSchema.CallToolResult response = this.mcpClient.callTool(new McpSchema.CallToolRequest(this.getName(), arguments));
        return ModelOptionsUtils.toJsonString(response.content());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private McpSchema.Tool tool;
        private McpSyncClient mcpClient;
        private Function<String,Map<String,Object>> toolInputParser;

        private Builder() {}

        public Builder tool(McpSchema.Tool tool) {
            this.tool = tool;
            return this;
        }

        public Builder mcpClient(McpSyncClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        public Builder toolInputParser(Function<String,Map<String,Object>> toolInputParser) {
            this.toolInputParser = toolInputParser;
            return this;
        }

        public McpToolCallback build() {
            return new McpToolCallback(tool, mcpClient, toolInputParser);
        }

    }

}
