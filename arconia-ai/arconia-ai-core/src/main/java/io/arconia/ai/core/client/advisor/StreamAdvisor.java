package io.arconia.ai.core.client.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.Ordered;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * An advisor that calls a {@link ChatModel} in stream mode.
 */
public class StreamAdvisor implements StreamAroundAdvisor {

    private final ChatModel chatModel;

    public StreamAdvisor(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chatModel.stream(advisedRequest.toPrompt())
                .map(chatResponse -> new AdvisedResponse(chatResponse, Map.copyOf(advisedRequest.adviseContext())))
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public String getName() {
        return StreamAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
