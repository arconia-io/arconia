package io.arconia.ai.core.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import io.arconia.ai.core.client.DefaultArconiaChatClient.DefaultArconiaChatClientRequestSpec;
import io.arconia.ai.core.tools.ToolCallbackProvider;
import io.arconia.ai.core.tools.method.MethodToolCallbackProvider;

/**
 * Default implementation of {@link ArconiaChatClient.ArconiaBuilder} based on
 * {@link DefaultChatClientBuilder}.
 */
public class DefaultArconiaChatClientBuilder implements ArconiaChatClient.ArconiaBuilder {

    private final DefaultArconiaChatClientRequestSpec arconiaRequest;

    public DefaultArconiaChatClientBuilder(ChatModel chatModel, ObservationRegistry observationRegistry,
            ChatClientObservationConvention customObservationConvention) {
        Assert.notNull(chatModel, "the " + ChatModel.class.getName() + " must be non-null");
        Assert.notNull(observationRegistry, "the " + ObservationRegistry.class.getName() + " must be non-null");
        this.arconiaRequest = new DefaultArconiaChatClientRequestSpec(chatModel, null, Map.of(), null, Map.of(),
                List.of(), List.of(), List.of(), List.of(), null, List.of(), Map.of(), observationRegistry,
                customObservationConvention, Map.of());
    }

    // ADVISORS

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultAdvisors(Advisor... advisors) {
        this.arconiaRequest.advisors(advisors);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultAdvisors(List<Advisor> advisors) {
        this.arconiaRequest.advisors(advisors);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultAdvisors(Consumer<ChatClient.AdvisorSpec> advisorSpecConsumer) {
        this.arconiaRequest.advisors(advisorSpecConsumer);
        return this;
    }

    // OPTIONS

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultOptions(ChatOptions chatOptions) {
        this.arconiaRequest.options(chatOptions);
        return this;
    }

    // USER

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultUser(String text) {
        this.arconiaRequest.user(text);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultUser(Resource text, Charset charset) {
        Assert.notNull(text, "text cannot be null");
        Assert.notNull(charset, "charset cannot be null");
        try {
            this.arconiaRequest.user(text.getContentAsString(charset));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultUser(Resource text) {
        return this.defaultUser(text, Charset.defaultCharset());
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultUser(Consumer<ChatClient.PromptUserSpec> userSpecConsumer) {
        this.arconiaRequest.user(userSpecConsumer);
        return this;
    }

    // SYSTEM

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultSystem(String text) {
        this.arconiaRequest.system(text);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultSystem(Resource text, Charset charset) {
        Assert.notNull(text, "text cannot be null");
        Assert.notNull(charset, "charset cannot be null");
        try {
            this.arconiaRequest.system(text.getContentAsString(charset));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultSystem(Resource text) {
        return this.defaultSystem(text, Charset.defaultCharset());
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultSystem(Consumer<ChatClient.PromptSystemSpec> systemSpecConsumer) {
        this.arconiaRequest.system(systemSpecConsumer);
        return this;
    }

    // TOOLS

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultTools(String... toolNames) {
        this.arconiaRequest.functions(toolNames);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultTools(Class<?>... toolBoxes) {
        ToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder().sources(toolBoxes).build();
        return defaultToolCallbackProviders(toolCallbackProvider);
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultTools(Object... toolBoxes) {
        ToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder().sources(toolBoxes).build();
        return defaultToolCallbackProviders(toolCallbackProvider);
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultToolCallbacks(FunctionCallback... toolCallbacks) {
        this.arconiaRequest.functions(toolCallbacks);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultToolCallbackProviders(
            ToolCallbackProvider... toolCallbackProviders) {
        for (ToolCallbackProvider toolCallbackProvider : toolCallbackProviders) {
            this.arconiaRequest.functions(toolCallbackProvider.getToolCallbacks());
        }
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultFunctions(String... functionNames) {
        return defaultTools(functionNames);
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultFunctions(FunctionCallback... functionCallbacks) {
        return defaultToolCallbacks(functionCallbacks);
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder defaultToolContext(Map<String, Object> toolContext) {
        this.arconiaRequest.toolContext(toolContext);
        return this;
    }

    @Override
    public ArconiaChatClient.ArconiaBuilder clone() {
        return this.arconiaRequest.mutate();
    }

    @Override
    public ArconiaChatClient build() {
        return new DefaultArconiaChatClient(this.arconiaRequest);
    }

    void addMessages(List<Message> messages) {
        this.arconiaRequest.messages(messages);
    }

    void addToolCallbacks(List<FunctionCallback> toolCallbacks) {
        this.arconiaRequest.tools(toolCallbacks);
    }

    void addToolContext(Map<String, Object> toolContext) {
        this.arconiaRequest.toolContext(toolContext);
    }

}
