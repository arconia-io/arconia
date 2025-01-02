package io.arconia.ai.autoconfigure.core.client;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.autoconfigure.chat.client.ChatClientBuilderConfigurer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import io.arconia.ai.core.client.ArconiaChatClient;

@AutoConfiguration
public class ArconiaChatClientAutoConfiguration {

    @Bean
    @Scope("prototype")
    @Primary
    ChatClient.Builder arconiaChatClientBuilder(ChatClientBuilderConfigurer chatClientBuilderConfigurer, ChatModel chatModel,
                                         ObjectProvider<ObservationRegistry> observationRegistry,
                                         ObjectProvider<ChatClientObservationConvention> observationConvention) {
        ChatClient.Builder builder = ArconiaChatClient.builder(chatModel,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                observationConvention.getIfUnique(() -> null));
        return chatClientBuilderConfigurer.configure(builder);
    }

}
