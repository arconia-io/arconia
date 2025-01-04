package io.arconia.ai.core.client;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.DefaultChatClient.DefaultCallResponseSpec;
import org.springframework.ai.chat.client.DefaultChatClient.DefaultChatClientRequestSpec;
import org.springframework.ai.chat.client.DefaultChatClient.DefaultStreamResponseSpec;
import org.springframework.ai.chat.client.advisor.DefaultAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackProvider;
import io.arconia.ai.core.tools.method.MethodToolCallbackProvider;

/**
 * Default implementation of {@link ArconiaChatClient} based on {@link DefaultChatClient}.
 */
public class DefaultArconiaChatClient implements ArconiaChatClient {

    private static final ChatClientObservationConvention DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION = new DefaultChatClientObservationConvention();

    private final DefaultArconiaChatClientRequestSpec defaultArconiaChatClientRequest;

    public DefaultArconiaChatClient(DefaultArconiaChatClientRequestSpec defaultArconiaChatClientRequest) {
        this.defaultArconiaChatClientRequest = defaultArconiaChatClientRequest;
    }

    @Override
    public ArconiaChatClientRequestSpec prompt() {
        return new DefaultArconiaChatClientRequestSpec(this.defaultArconiaChatClientRequest);
    }

    @Override
    public ArconiaChatClientRequestSpec prompt(String content) {
        Assert.hasText(content, "content cannot be null or empty");
        return prompt(new Prompt(content));
    }

    @Override
    public ArconiaChatClientRequestSpec prompt(Prompt prompt) {
        Assert.notNull(prompt, "prompt cannot be null");

        DefaultArconiaChatClientRequestSpec spec = new DefaultArconiaChatClientRequestSpec(
                this.defaultArconiaChatClientRequest);

        // Options
        if (prompt.getOptions() != null) {
            spec.options(prompt.getOptions());
        }

        // Messages
        if (prompt.getInstructions() != null) {
            spec.messages(prompt.getInstructions());
        }

        return spec;
    }

    @Override
    public ArconiaBuilder mutate() {
        return this.defaultArconiaChatClientRequest.mutate();
    }

    public static class DefaultArconiaPromptUserSpec implements ArconiaPromptUserSpec {

        private final Map<String, Object> params = new HashMap<>();

        private final List<Media> media = new ArrayList<>();

        @Nullable
        private String text;

        @Override
        public ArconiaPromptUserSpec media(Media... media) {
            Assert.notNull(media, "media cannot be null");
            Assert.noNullElements(media, "media cannot contain null elements");
            this.media.addAll(Arrays.asList(media));
            return this;
        }

        @Override
        public ArconiaPromptUserSpec media(MimeType mimeType, URL url) {
            Assert.notNull(mimeType, "mimeType cannot be null");
            Assert.notNull(url, "url cannot be null");
            this.media.add(Media.builder().mimeType(mimeType).data(url).build());
            return this;
        }

        @Override
        public ArconiaPromptUserSpec media(MimeType mimeType, Resource resource) {
            Assert.notNull(mimeType, "mimeType cannot be null");
            Assert.notNull(resource, "resource cannot be null");
            this.media.add(Media.builder().mimeType(mimeType).data(resource).build());
            return this;
        }

        @Override
        public ArconiaPromptUserSpec text(String text) {
            Assert.hasText(text, "text cannot be null or empty");
            this.text = text;
            return this;
        }

        @Override
        public ArconiaPromptUserSpec text(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");
            try {
                this.text(text.getContentAsString(charset));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public ArconiaPromptUserSpec text(Resource text) {
            Assert.notNull(text, "text cannot be null");
            this.text(text, Charset.defaultCharset());
            return this;
        }

        @Override
        public ArconiaPromptUserSpec param(String key, Object value) {
            Assert.hasText(key, "key cannot be null or empty");
            Assert.notNull(value, "value cannot be null");
            this.params.put(key, value);
            return this;
        }

        @Override
        public ArconiaPromptUserSpec params(Map<String, Object> params) {
            Assert.notNull(params, "params cannot be null");
            Assert.noNullElements(params.keySet(), "param keys cannot contain null elements");
            Assert.noNullElements(params.values(), "param values cannot contain null elements");
            this.params.putAll(params);
            return this;
        }

        @Nullable
        protected String text() {
            return this.text;
        }

        protected Map<String, Object> params() {
            return this.params;
        }

        protected List<Media> media() {
            return this.media;
        }

    }

    public static class DefaultArconiaPromptSystemSpec implements ArconiaPromptSystemSpec {

        private final Map<String, Object> params = new HashMap<>();

        @Nullable
        private String text;

        @Override
        public ArconiaPromptSystemSpec text(String text) {
            Assert.hasText(text, "text cannot be null or empty");
            this.text = text;
            return this;
        }

        @Override
        public ArconiaPromptSystemSpec text(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");
            try {
                this.text(text.getContentAsString(charset));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public ArconiaPromptSystemSpec text(Resource text) {
            Assert.notNull(text, "text cannot be null");
            this.text(text, Charset.defaultCharset());
            return this;
        }

        @Override
        public ArconiaPromptSystemSpec param(String key, Object value) {
            Assert.hasText(key, "key cannot be null or empty");
            Assert.notNull(value, "value cannot be null");
            this.params.put(key, value);
            return this;
        }

        @Override
        public ArconiaPromptSystemSpec params(Map<String, Object> params) {
            Assert.notNull(params, "params cannot be null");
            Assert.noNullElements(params.keySet(), "param keys cannot contain null elements");
            Assert.noNullElements(params.values(), "param values cannot contain null elements");
            this.params.putAll(params);
            return this;
        }

        @Nullable
        protected String text() {
            return this.text;
        }

        protected Map<String, Object> params() {
            return this.params;
        }

    }

    public static class DefaultArconiaAdvisorSpec implements ArconiaAdvisorSpec {

        private final List<Advisor> advisors = new ArrayList<>();

        private final Map<String, Object> params = new HashMap<>();

        @Override
        public ArconiaAdvisorSpec param(String key, Object value) {
            Assert.hasText(key, "key cannot be null or empty");
            Assert.notNull(value, "value cannot be null");
            this.params.put(key, value);
            return this;
        }

        @Override
        public ArconiaAdvisorSpec params(Map<String, Object> params) {
            Assert.notNull(params, "params cannot be null");
            Assert.noNullElements(params.keySet(), "param keys cannot contain null elements");
            Assert.noNullElements(params.values(), "param values cannot contain null elements");
            this.params.putAll(params);
            return this;
        }

        @Override
        public ArconiaAdvisorSpec advisors(Advisor... advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            Assert.noNullElements(advisors, "advisors cannot contain null elements");
            this.advisors.addAll(List.of(advisors));
            return this;
        }

        @Override
        public ArconiaAdvisorSpec advisors(List<Advisor> advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            Assert.noNullElements(advisors, "advisors cannot contain null elements");
            this.advisors.addAll(advisors);
            return this;
        }

        public List<Advisor> getAdvisors() {
            return this.advisors;
        }

        public Map<String, Object> getParams() {
            return this.params;
        }

    }

    public static class DefaultArconiaCallResponseSpec extends DefaultCallResponseSpec
            implements ArconiaCallResponseSpec {

        public DefaultArconiaCallResponseSpec(DefaultChatClientRequestSpec request) {
            super(request);
        }

    }

    public static class DefaultArconiaStreamResponseSpec extends DefaultStreamResponseSpec
            implements ArconiaStreamResponseSpec {

        public DefaultArconiaStreamResponseSpec(DefaultChatClientRequestSpec request) {
            super(request);
        }

    }

    public static class DefaultArconiaChatClientRequestSpec extends DefaultChatClientRequestSpec
            implements ArconiaChatClientRequestSpec {

        private final ObservationRegistry observationRegistry;

        private final ChatClientObservationConvention customObservationConvention;

        private final ChatModel chatModel;

        private final List<Media> media = new ArrayList<>();

        private final List<String> toolNames = new ArrayList<>();

        private final List<FunctionCallback> toolCallbacks = new ArrayList<>();

        private final List<Message> messages = new ArrayList<>();

        private final Map<String, Object> userParams = new HashMap<>();

        private final Map<String, Object> systemParams = new HashMap<>();

        private final List<Advisor> advisors = new ArrayList<>();

        private final Map<String, Object> advisorParams = new HashMap<>();

        private final DefaultAroundAdvisorChain.Builder aroundAdvisorChainBuilder;

        private final Map<String, Object> toolContext = new HashMap<>();

        @Nullable
        private String userText;

        @Nullable
        private String systemText;

        @Nullable
        private ChatOptions chatOptions;

        DefaultArconiaChatClientRequestSpec(DefaultArconiaChatClientRequestSpec ccr) {
            this(ccr.chatModel, ccr.userText, ccr.userParams, ccr.systemText, ccr.systemParams, ccr.toolCallbacks,
                    ccr.messages, ccr.toolNames, ccr.media, ccr.chatOptions, ccr.advisors, ccr.advisorParams,
                    ccr.observationRegistry, ccr.customObservationConvention, ccr.toolContext);
        }

        public DefaultArconiaChatClientRequestSpec(ChatModel chatModel, @Nullable String userText,
                Map<String, Object> userParams, @Nullable String systemText, Map<String, Object> systemParams,
                List<FunctionCallback> toolCallbacks, List<Message> messages, List<String> toolNames, List<Media> media,
                @Nullable ChatOptions chatOptions, List<Advisor> advisors, Map<String, Object> advisorParams,
                ObservationRegistry observationRegistry,
                @Nullable ChatClientObservationConvention customObservationConvention,
                Map<String, Object> toolContext) {

            super(chatModel, userText, userParams, systemText, systemParams, toolCallbacks, messages, toolNames, media,
                    chatOptions, advisors, advisorParams, observationRegistry, customObservationConvention,
                    toolContext);

            Assert.notNull(chatModel, "chatModel cannot be null");
            Assert.notNull(userParams, "userParams cannot be null");
            Assert.notNull(systemParams, "systemParams cannot be null");
            Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
            Assert.notNull(messages, "messages cannot be null");
            Assert.notNull(toolNames, "toolNames cannot be null");
            Assert.notNull(media, "media cannot be null");
            Assert.notNull(advisors, "advisors cannot be null");
            Assert.notNull(advisorParams, "advisorParams cannot be null");
            Assert.notNull(observationRegistry, "observationRegistry cannot be null");
            Assert.notNull(toolContext, "toolContext cannot be null");

            this.chatModel = chatModel;
            this.chatOptions = chatOptions != null ? chatOptions.copy()
                    : (chatModel.getDefaultOptions() != null) ? chatModel.getDefaultOptions().copy() : null;

            this.userText = userText;
            this.userParams.putAll(userParams);
            this.systemText = systemText;
            this.systemParams.putAll(systemParams);

            this.toolNames.addAll(toolNames);
            this.toolCallbacks.addAll(toolCallbacks);
            this.messages.addAll(messages);
            this.media.addAll(media);
            this.advisors.addAll(advisors);
            this.advisorParams.putAll(advisorParams);
            this.observationRegistry = observationRegistry;
            this.customObservationConvention = customObservationConvention != null ? customObservationConvention
                    : DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION;
            this.toolContext.putAll(toolContext);

            this.advisors.add(new CallAroundAdvisor() {
                @Override
                public String getName() {
                    return CallAroundAdvisor.class.getSimpleName();
                }

                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }

                @Override
                public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
                    return new AdvisedResponse(chatModel.call(advisedRequest.toPrompt()),
                            Collections.unmodifiableMap(advisedRequest.adviseContext()));
                }
            });

            this.advisors.add(new StreamAroundAdvisor() {
                @Override
                public String getName() {
                    return StreamAroundAdvisor.class.getSimpleName();
                }

                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }

                @Override
                public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest,
                        StreamAroundAdvisorChain chain) {
                    return chatModel.stream(advisedRequest.toPrompt())
                        .map(chatResponse -> new AdvisedResponse(chatResponse,
                                Collections.unmodifiableMap(advisedRequest.adviseContext())))
                        .publishOn(Schedulers.boundedElastic());
                }
            });

            this.aroundAdvisorChainBuilder = DefaultAroundAdvisorChain.builder(observationRegistry)
                .pushAll(this.advisors);
        }

        private ObservationRegistry getObservationRegistry() {
            return this.observationRegistry;
        }

        private ChatClientObservationConvention getCustomObservationConvention() {
            return this.customObservationConvention;
        }

        @Nullable
        public String getUserText() {
            return this.userText;
        }

        public Map<String, Object> getUserParams() {
            return this.userParams;
        }

        @Nullable
        public String getSystemText() {
            return this.systemText;
        }

        public Map<String, Object> getSystemParams() {
            return this.systemParams;
        }

        @Nullable
        public ChatOptions getChatOptions() {
            return this.chatOptions;
        }

        public List<Advisor> getAdvisors() {
            return this.advisors;
        }

        public Map<String, Object> getAdvisorParams() {
            return this.advisorParams;
        }

        public List<Message> getMessages() {
            return this.messages;
        }

        public List<Media> getMedia() {
            return this.media;
        }

        public List<String> getFunctionNames() {
            return this.toolNames;
        }

        public List<FunctionCallback> getFunctionCallbacks() {
            return this.toolCallbacks;
        }

        public Map<String, Object> getToolContext() {
            return this.toolContext;
        }

        // BUILD

        @Override
        public ArconiaBuilder mutate() {
            DefaultArconiaChatClientBuilder builder = (DefaultArconiaChatClientBuilder) ArconiaChatClient
                .builder(this.chatModel, this.observationRegistry, this.customObservationConvention)
                .defaultTools(StringUtils.toStringArray(this.toolNames));

            if (StringUtils.hasText(this.userText)) {
                builder.defaultUser(
                        u -> u.text(this.userText).params(this.userParams).media(this.media.toArray(new Media[0])));
            }

            if (StringUtils.hasText(this.systemText)) {
                builder.defaultSystem(s -> s.text(this.systemText).params(this.systemParams));
            }

            if (this.chatOptions != null) {
                builder.defaultOptions(this.chatOptions);
            }

            builder.addMessages(this.messages);
            builder.addToolCallbacks(this.toolCallbacks);
            builder.addToolContext(this.toolContext);

            return builder;
        }

        // ADVISORS

        @Override
        public ArconiaChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer) {
            Assert.notNull(consumer, "consumer cannot be null");
            var advisorSpec = new DefaultArconiaAdvisorSpec();
            consumer.accept(advisorSpec);
            this.advisorParams.putAll(advisorSpec.getParams());
            this.advisors.addAll(advisorSpec.getAdvisors());
            this.aroundAdvisorChainBuilder.pushAll(advisorSpec.getAdvisors());
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec advisors(Advisor... advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            Assert.noNullElements(advisors, "advisors cannot contain null elements");
            this.advisors.addAll(Arrays.asList(advisors));
            this.aroundAdvisorChainBuilder.pushAll(Arrays.asList(advisors));
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec advisors(List<Advisor> advisors) {
            Assert.notNull(advisors, "advisors cannot be null");
            Assert.noNullElements(advisors, "advisors cannot contain null elements");
            this.advisors.addAll(advisors);
            this.aroundAdvisorChainBuilder.pushAll(advisors);
            return this;
        }

        // MESSAGES

        @Override
        public ArconiaChatClientRequestSpec messages(Message... messages) {
            Assert.notNull(messages, "messages cannot be null");
            Assert.noNullElements(messages, "messages cannot contain null elements");
            this.messages.addAll(List.of(messages));
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec messages(List<Message> messages) {
            Assert.notNull(messages, "messages cannot be null");
            Assert.noNullElements(messages, "messages cannot contain null elements");
            this.messages.addAll(messages);
            return this;
        }

        // OPTIONS

        @Override
        public <T extends ChatOptions> ArconiaChatClientRequestSpec options(T options) {
            Assert.notNull(options, "options cannot be null");
            this.chatOptions = options;
            return this;
        }

        // TOOLS

        @Override
        public ArconiaChatClientRequestSpec tools(String... toolNames) {
            Assert.notNull(toolNames, "toolNames cannot be null");
            Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
            this.toolNames.addAll(List.of(toolNames));
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec tools(Class<?>... toolBoxes) {
            Assert.notNull(toolBoxes, "toolBoxes cannot be null");
            Assert.noNullElements(toolBoxes, "toolBoxes cannot contain null elements");
            ToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder().sources(toolBoxes).build();
            return toolCallbackProviders(toolCallbackProvider);
        }

        @Override
        public ArconiaChatClientRequestSpec tools(Object... toolBoxes) {
            Assert.notNull(toolBoxes, "toolBoxes cannot be null");
            Assert.noNullElements(toolBoxes, "toolBoxes cannot contain null elements");
            ToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder().sources(toolBoxes).build();
            return toolCallbackProviders(toolCallbackProvider);
        }

        @Override
        public ArconiaChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks) {
            Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
            Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
            this.toolCallbacks.addAll(Arrays.asList(toolCallbacks));
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec toolCallbackProviders(ToolCallbackProvider... toolCallbackProviders) {
            for (ToolCallbackProvider toolCallbackProvider : toolCallbackProviders) {
                this.toolCallbacks.addAll(Arrays.asList(toolCallbackProvider.getToolCallbacks()));
            }
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec functions(String... functionBeanNames) {
            return tools(functionBeanNames);
        }

        @Override
        public ArconiaChatClientRequestSpec functions(FunctionCallback... functionCallbacks) {
            return toolCallbacks(Stream.of(functionCallbacks).map(f -> (ToolCallback) f).toArray(ToolCallback[]::new));
        }

        @Override
        public ArconiaChatClientRequestSpec toolContext(Map<String, Object> toolContext) {
            Assert.notNull(toolContext, "toolContext cannot be null");
            Assert.noNullElements(toolContext.keySet(), "toolContext keys cannot contain null elements");
            Assert.noNullElements(toolContext.values(), "toolContext values cannot contain null elements");
            this.toolContext.putAll(toolContext);
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec system(String text) {
            Assert.hasText(text, "text cannot be null or empty");
            this.systemText = text;
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec system(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");

            try {
                this.systemText = text.getContentAsString(charset);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec system(Resource text) {
            Assert.notNull(text, "text cannot be null");
            return this.system(text, Charset.defaultCharset());
        }

        @Override
        public ArconiaChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer) {
            Assert.notNull(consumer, "consumer cannot be null");

            var systemSpec = new DefaultArconiaPromptSystemSpec();
            consumer.accept(systemSpec);
            this.systemText = StringUtils.hasText(systemSpec.text()) ? systemSpec.text() : this.systemText;
            this.systemParams.putAll(systemSpec.params());

            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec user(String text) {
            Assert.hasText(text, "text cannot be null or empty");
            this.userText = text;
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec user(Resource text, Charset charset) {
            Assert.notNull(text, "text cannot be null");
            Assert.notNull(charset, "charset cannot be null");

            try {
                this.userText = text.getContentAsString(charset);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public ArconiaChatClientRequestSpec user(Resource text) {
            Assert.notNull(text, "text cannot be null");
            return this.user(text, Charset.defaultCharset());
        }

        @Override
        public ArconiaChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
            Assert.notNull(consumer, "consumer cannot be null");

            var us = new DefaultArconiaPromptUserSpec();
            consumer.accept(us);
            this.userText = StringUtils.hasText(us.text()) ? us.text() : this.userText;
            this.userParams.putAll(us.params());
            this.media.addAll(us.media());
            return this;
        }

        @Override
        public ArconiaCallResponseSpec call() {
            return new DefaultArconiaCallResponseSpec(new DefaultChatClientRequestSpec(this.chatModel, this.userText,
                    this.userParams, this.systemText, this.systemParams, this.toolCallbacks, this.messages,
                    this.toolNames, this.media, this.chatOptions, this.advisors, this.advisorParams,
                    this.observationRegistry, this.customObservationConvention, this.toolContext));
        }

        @Override
        public ArconiaStreamResponseSpec stream() {
            return new DefaultArconiaStreamResponseSpec(new DefaultChatClientRequestSpec(this.chatModel, this.userText,
                    this.userParams, this.systemText, this.systemParams, this.toolCallbacks, this.messages,
                    this.toolNames, this.media, this.chatOptions, this.advisors, this.advisorParams,
                    this.observationRegistry, this.customObservationConvention, this.toolContext));
        }

    }

}
