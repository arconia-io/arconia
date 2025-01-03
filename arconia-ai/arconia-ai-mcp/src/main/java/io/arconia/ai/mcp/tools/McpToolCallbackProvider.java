package io.arconia.ai.mcp.tools;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.ai.mcp.client.McpAsyncClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.util.Assert;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackProvider;
import io.arconia.ai.core.tools.ToolUtils;

/**
 * A {@link ToolCallbackProvider} that builds {@link ToolCallback} instances from MCP
 * tools.
 */
public class McpToolCallbackProvider implements ToolCallbackProvider {

    private final List<McpSyncClient> mcpClients;

    private McpToolCallbackProvider(List<McpSyncClient> mcpClients) {
        Assert.notNull(mcpClients, "mcpClients cannot be null");
        Assert.noNullElements(mcpClients, "mcpClients cannot contain null elements");
        this.mcpClients = mcpClients;
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
        var toolCallbacks = mcpClients.stream()
            .flatMap(mcpClient -> mcpClient.listTools()
                .tools()
                .stream()
                .map(tool -> (ToolCallback) new McpToolCallback(mcpClient, tool)))
            .toArray(ToolCallback[]::new);

        if (ToolUtils.hasDuplicateToolNames(toolCallbacks)) {
            throw new IllegalStateException("Multiple tools with the same name found in MCP sources");
        }

        return toolCallbacks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<McpSyncClient> mcpClients;

        public Builder mcpClients(List<McpSyncClient> mcpClients) {
            this.mcpClients = mcpClients;
            return this;
        }

        public Builder mcpClients(McpSyncClient... mcpClients) {
            Assert.notNull(mcpClients, "mcpClients cannot be null");
            this.mcpClients = Arrays.asList(mcpClients);
            return this;
        }

        public Builder mcpClients(McpAsyncClient... mcpClients) {
            this.mcpClients = Stream.of(mcpClients).map(McpSyncClient::new).toList();
            return this;
        }

        public McpToolCallbackProvider build() {
            return new McpToolCallbackProvider(mcpClients);
        }

    }

}
