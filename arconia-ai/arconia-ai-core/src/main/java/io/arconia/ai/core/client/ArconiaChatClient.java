package io.arconia.ai.core.client;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackProvider;

/**
 * A {@link ChatClient} enhanced for more advanced features.
 */
public interface ArconiaChatClient extends ChatClient {

    static ArconiaBuilder builder(ChatModel chatModel) {
        return builder(chatModel, ObservationRegistry.NOOP, null);
    }

    static ArconiaBuilder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
            @Nullable ChatClientObservationConvention customObservationConvention) {
        Assert.notNull(chatModel, "chatModel cannot be null");
        Assert.notNull(observationRegistry, "observationRegistry cannot be null");
        return new DefaultArconiaChatClientBuilder(chatModel, observationRegistry, customObservationConvention);
    }

    // @formatter:off

    ArconiaChatClientRequestSpec prompt();

    ArconiaChatClientRequestSpec prompt(String content);

    ArconiaChatClientRequestSpec prompt(Prompt prompt);

    ArconiaBuilder mutate();

    // PROMPT
    interface ArconiaPromptUserSpec extends PromptUserSpec {}

    interface ArconiaPromptSystemSpec extends PromptSystemSpec {}

    // ADVISOR
    interface ArconiaAdvisorSpec extends AdvisorSpec {}

    // RESPONSE
    interface ArconiaCallResponseSpec extends CallResponseSpec {}

    interface ArconiaStreamResponseSpec extends StreamResponseSpec {}

    // REQUEST
    interface ArconiaChatClientRequestSpec extends ChatClientRequestSpec {

        // BUILD
        ArconiaBuilder mutate();

        // ADVISORS
        ArconiaChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer);

        ArconiaChatClientRequestSpec advisors(Advisor... advisors);

        ArconiaChatClientRequestSpec advisors(List<Advisor> advisors);

        // MESSAGES
        ArconiaChatClientRequestSpec messages(Message... messages);

        ArconiaChatClientRequestSpec messages(List<Message> messages);

        // OPTIONS
        <T extends ChatOptions> ArconiaChatClientRequestSpec options(T options);

        // TOOLS
        ArconiaChatClientRequestSpec tools(String... toolNames);

        ArconiaChatClientRequestSpec tools(Class<?>... toolBoxes);

        ArconiaChatClientRequestSpec tools(Object... toolBoxes);

        ArconiaChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks);

        ArconiaChatClientRequestSpec toolCallbackProviders(ToolCallbackProvider... toolCallbackProviders);

        ArconiaChatClientRequestSpec functions(FunctionCallback... functionCallbacks);

        ArconiaChatClientRequestSpec functions(String... functionBeanNames);

        ArconiaChatClientRequestSpec toolContext(Map<String, Object> toolContext);

        // SYSTEM
        ArconiaChatClientRequestSpec system(String text);

        ArconiaChatClientRequestSpec system(Resource textResource, Charset charset);

        ArconiaChatClientRequestSpec system(Resource text);

        ArconiaChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer);

        // USER
        ArconiaChatClientRequestSpec user(String text);

        ArconiaChatClientRequestSpec user(Resource text, Charset charset);

        ArconiaChatClientRequestSpec user(Resource text);

        ArconiaChatClientRequestSpec user(Consumer<PromptUserSpec> consumer);

        // CALL
        ArconiaCallResponseSpec call();

        ArconiaStreamResponseSpec stream();
    }

    // BUILDER
    interface ArconiaBuilder extends Builder {

        // ADVISORS
        ArconiaBuilder defaultAdvisors(Advisor... advisors);

        ArconiaBuilder defaultAdvisors(List<Advisor> advisors);

        ArconiaBuilder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer);

        // OPTIONS
        ArconiaBuilder defaultOptions(ChatOptions chatOptions);

        // USER
        ArconiaBuilder defaultUser(String text);

        ArconiaBuilder defaultUser(Resource text, Charset charset);

        ArconiaBuilder defaultUser(Resource text);

        ArconiaBuilder defaultUser(Consumer<PromptUserSpec> userSpecConsumer);

        // SYSTEM
        ArconiaBuilder defaultSystem(String text);

        ArconiaBuilder defaultSystem(Resource text, Charset charset);

        ArconiaBuilder defaultSystem(Resource text);

        ArconiaBuilder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer);

        // TOOLS
        ArconiaBuilder defaultTools(String... toolNames);

        ArconiaBuilder defaultTools(Class<?>... toolBoxes);

        ArconiaBuilder defaultTools(Object... toolBoxes);

        ArconiaBuilder defaultToolCallbacks(ToolCallback... toolCallbacks);

        ArconiaBuilder defaultToolCallbackProviders(ToolCallbackProvider... toolCallbackProviders);

        ArconiaBuilder defaultFunctions(String... functionNames);

        ArconiaBuilder defaultFunctions(FunctionCallback... functionCallbacks);

        ArconiaBuilder defaultToolContext(Map<String, Object> toolContext);

        default <I, O> Builder defaultFunction(String name, String description, Function<I, O> function) {
            throw new UnsupportedOperationException();
        }

        default <I, O> Builder defaultFunction(String name, String description,
                BiFunction<I, ToolContext, O> function) {
            throw new UnsupportedOperationException();
        }

        // BUILD
        ArconiaBuilder clone();

        ArconiaChatClient build();
    }

    // @formatter:on

}
