package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationView;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.util.Assert;

/**
 * Observation handler that propagates the conversation id of a chat client call to the observations nested in it.
 * <p>
 * The OpenTelemetry GenAI semantic conventions require the conversation id on every span where it's available,
 * and the backends of the other flavors group a conversation by reading it from every span. Spring AI only
 * carries it in the chat client request, so the chat client convention records it on its own span and this
 * handler copies it to the model, tool, and advisor spans below, under the attribute keys of the flavor.
 */
public final class GenAiConversationIdObservationHandler implements ObservationHandler<Observation.Context> {

    private final List<String> attributeKeys;

    public GenAiConversationIdObservationHandler(List<String> attributeKeys) {
        Assert.notEmpty(attributeKeys, "attributeKeys cannot be empty");
        this.attributeKeys = List.copyOf(attributeKeys);
    }

    @Override
    public void onStart(Observation.Context context) {
        String conversationId = conversationId(context.getParentObservation());
        if (conversationId != null) {
            for (String attributeKey : attributeKeys) {
                context.addHighCardinalityKeyValue(KeyValue.of(attributeKey, conversationId));
            }
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext
                || context instanceof EmbeddingModelObservationContext
                || context instanceof ToolCallingObservationContext
                || context instanceof AdvisorObservationContext;
    }

    /**
     * The attribute keys the conversation id is recorded under.
     */
    public List<String> getAttributeKeys() {
        return attributeKeys;
    }

    /**
     * Walks up the observations enclosing the current one until it finds the chat client call,
     * returning its conversation id or null when there is none.
     */
    private static @Nullable String conversationId(@Nullable ObservationView parent) {
        while (parent != null) {
            if (parent.getContextView() instanceof ChatClientObservationContext chatClientContext) {
                return GenAiConversations.conversationId(chatClientContext);
            }
            parent = parent.getContextView().getParentObservation();
        }
        return null;
    }

}
