package io.arconia.ai.mcp.tools;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.ai.mcp.client.McpAsyncClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.util.Assert;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackResolver;

/**
 * A {@link ToolCallbackResolver} that resolves {@link ToolCallback} instances from MCP
 * tools.
 */
public class McpToolCallbackResolver implements ToolCallbackResolver {

    private final List<McpSyncClient> mcpClients;

    public McpToolCallbackResolver(List<McpSyncClient> mcpClients) {
        Assert.notNull(mcpClients, "mcpClients cannot be null");
        Assert.noNullElements(mcpClients, "mcpClients cannot contain null elements");
        this.mcpClients = mcpClients;
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
        return mcpClients.stream()
            .flatMap(mcpClient -> mcpClient.listTools()
                .tools()
                .stream()
                .map(tool -> (ToolCallback) new McpToolCallback(mcpClient, tool)))
            .toArray(ToolCallback[]::new);
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

        public McpToolCallbackResolver build() {
            return new McpToolCallbackResolver(mcpClients);
        }

    }

}