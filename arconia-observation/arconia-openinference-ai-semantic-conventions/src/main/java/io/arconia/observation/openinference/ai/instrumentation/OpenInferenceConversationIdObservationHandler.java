package io.arconia.observation.openinference.ai.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

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

/**
 * Observation handler that propagates the conversation id of a chat client call to the observations nested in it,
 * as the {@code session.id} attribute.
 * <p>
 * The OpenInference instrumentations record the session on every span of a conversation. Spring AI only
 * carries the conversation id in the chat client request, so the chat client convention records it on its
 * own span and this handler copies it to the model, tool, and advisor spans below.
 */
public final class OpenInferenceConversationIdObservationHandler implements ObservationHandler<Observation.Context> {

    @Override
    public void onStart(Observation.Context context) {
        String conversationId = conversationId(context.getParentObservation());
        if (conversationId != null) {
            context.addHighCardinalityKeyValue(KeyValue.of(SemanticConventions.SESSION_ID, conversationId));
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
     * Walks up the observations enclosing the current one until it finds the chat client call,
     * returning its conversation id or null when there is none.
     */
    private static @Nullable String conversationId(@Nullable ObservationView parent) {
        while (parent != null) {
            if (parent.getContextView() instanceof ChatClientObservationContext chatClientContext) {
                return OpenInferenceChatClientObservationConvention.conversationId(chatClientContext);
            }
            parent = parent.getContextView().getParentObservation();
        }
        return null;
    }

}
