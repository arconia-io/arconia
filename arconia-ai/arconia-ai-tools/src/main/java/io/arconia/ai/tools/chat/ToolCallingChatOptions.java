package io.arconia.ai.tools.chat;

import io.arconia.ai.tools.ToolCallback;

import io.arconia.ai.tools.metadata.ToolMetadata;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallingOptions;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of options that can be used to configure the interaction with a chat model,
 * including tool calling.
 */
public interface ToolCallingChatOptions extends FunctionCallingOptions {

	/**
	 * ToolCallbacks to be registered with the ChatModel.
	 */
	List<ToolCallback> getToolCallbacks();

	/**
	 * Set the ToolCallbacks to be registered with the ChatModel.
	 */
	void setToolCallbacks(List<ToolCallback> toolCallbacks);

	/**
	 * Set the ToolCallbacks to be registered with the ChatModel.
	 */
	void setToolCallbacks(ToolCallback... toolCallbacks);

	/**
	 * Names of the tools to register with the ChatModel.
	 */
	Set<String> getTools();

	/**
	 * Set the names of the tools to register with the ChatModel.
	 */
	void setTools(Set<String> tools);

	/**
	 * Set the names of the tools to register with the ChatModel.
	 */
	void setTools(String... tools);

	/**
	 * Whether the result of each tool call should be returned directly or passed back to
	 * the model. It can be overridden for each {@link ToolCallback} instance via
	 * {@link ToolMetadata#returnDirect()}.
	 */
	@Nullable
	Boolean getToolCallReturnDirect();

	/**
	 * Set whether the result of each tool call should be returned directly or passed back
	 * to the model. It can be overridden for each {@link ToolCallback} instance via
	 * {@link ToolMetadata#returnDirect()}.
	 */
	void setToolCallReturnDirect(@Nullable Boolean toolCallReturnDirect);

	/**
	 * A builder to create a new {@link ToolCallingChatOptions} instance.
	 */
	static Builder builder() {
		return new DefaultToolCallingChatOptions.Builder();
	}

	/**
	 * A builder to create a {@link ToolCallingChatOptions} instance.
	 */
	interface Builder extends FunctionCallingOptions.Builder {

		/**
		 * ToolCallbacks to be registered with the ChatModel.
		 */
		Builder toolCallbacks(List<ToolCallback> functionCallbacks);

		/**
		 * ToolCallbacks to be registered with the ChatModel.
		 */
		Builder toolCallbacks(ToolCallback... functionCallbacks);

		/**
		 * Names of the tools to register with the ChatModel.
		 */
		Builder tools(Set<String> toolNames);

		/**
		 * Names of the tools to register with the ChatModel.
		 */
		Builder tools(String... toolNames);

		/**
		 * Whether the result of each tool call should be returned directly or passed back
		 * to the model. It can be overridden for each {@link ToolCallback} instance via
		 * {@link ToolMetadata#returnDirect()}.
		 */
		Builder toolCallReturnDirect(@Nullable Boolean toolCallReturnDirect);

		// FunctionCallingOptions.Builder methods

		@Override
		Builder toolContext(Map<String, Object> context);

		@Override
		Builder toolContext(String key, Object value);

		@Override
		@Deprecated // Use toolCallbacks() instead
		Builder functionCallbacks(List<FunctionCallback> functionCallbacks);

		@Override
		@Deprecated // Use toolCallbacks() instead
		Builder functionCallbacks(FunctionCallback... functionCallbacks);

		@Override
		@Deprecated // Use tools() instead
		Builder functions(Set<String> functions);

		@Override
		@Deprecated // Use tools() instead
		Builder function(String function);

		@Override
		@Deprecated // Use toolCallReturnDirect() instead
		Builder proxyToolCalls(@Nullable Boolean proxyToolCalls);

		// ChatOptions.Builder methods

		@Override
		Builder model(@Nullable String model);

		@Override
		Builder frequencyPenalty(@Nullable Double frequencyPenalty);

		@Override
		Builder maxTokens(@Nullable Integer maxTokens);

		@Override
		Builder presencePenalty(@Nullable Double presencePenalty);

		@Override
		Builder stopSequences(@Nullable List<String> stopSequences);

		@Override
		Builder temperature(@Nullable Double temperature);

		@Override
		Builder topK(@Nullable Integer topK);

		@Override
		Builder topP(@Nullable Double topP);

		@Override
		ToolCallingChatOptions build();

	}

}
