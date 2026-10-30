package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Helpers to identify the conversation a chat client call belongs to.
 */
public final class GenAiConversations {

    private GenAiConversations() {}

    /**
     * Returns the conversation id of the chat memory the chat client call belongs to,
     * or null when the call isn't part of a conversation.
     */
    public static @Nullable String conversationId(ChatClientObservationContext context) {
        if (CollectionUtils.isEmpty(context.getRequest().context())) {
            return null;
        }
        var conversationIdValue = context.getRequest().context().get(ChatMemory.CONVERSATION_ID);
        return conversationIdValue instanceof String conversationId && StringUtils.hasText(conversationId)
                ? conversationId : null;
    }

}
