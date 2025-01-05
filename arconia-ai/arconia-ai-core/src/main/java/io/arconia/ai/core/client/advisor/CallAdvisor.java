package io.arconia.ai.core.client.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

/**
 * An advisor that calls a {@link ChatModel}.
 */
public class CallAdvisor implements CallAroundAdvisor {

    private final ChatModel chatModel;

    public CallAdvisor(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        ChatResponse chatResponse = chatModel.call(advisedRequest.toPrompt());
        return new AdvisedResponse(chatResponse, Map.copyOf(advisedRequest.adviseContext()));
    }

    @Override
    public String getName() {
        return CallAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
