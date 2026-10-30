package io.arconia.observation.openinference.ai.instrumentation;

/**
 * Options for customizing the OpenInference instrumentation.
 */
public class OpenInferenceOptions {

    public static final String REDACTED_PLACEHOLDER = "__REDACTED__";

    /**
     * Maximum length of a base64-encoded image. Longer images are redacted.
     */
    private long base64ImageMaxLength = 32_000;

    /**
     * Whether to hide all embedding text.
     */
    private boolean hideEmbeddingsText = false;

    /**
     * Whether to hide the dimension summary recorded in place of embedding vectors, which are never recorded.
     */
    private boolean hideEmbeddingsVectors = false;

    /**
     * Whether to hide the LLM invocation parameters.
     */
    private boolean hideLlmInvocationParameters = false;

    /**
     * Whether to hide the definitions of the tools available to the model.
     */
    private boolean hideLlmTools = false;

    /**
     * Whether to hide all inputs.
     */
    private boolean hideInputs = false;

    /**
     * Whether to hide all images from the input messages.
     */
    private boolean hideInputImages = false;

    /**
     * Whether to hide all input messages.
     */
    private boolean hideInputMessages = false;

    /**
     * Whether to hide all texts from the input messages.
     */
    private boolean hideInputText = false;

    /**
     * Whether to hide all outputs.
     */
    private boolean hideOutputs = false;

    /**
     * Whether to hide all texts from the output messages.
     */
    private boolean hideOutputText = false;

    /**
     * Whether to hide all output messages.
     */
    private boolean hideOutputMessages = false;

    public long getBase64ImageMaxLength() {
        return base64ImageMaxLength;
    }

    public void setBase64ImageMaxLength(long base64ImageMaxLength) {
        this.base64ImageMaxLength = base64ImageMaxLength;
    }

    public boolean isHideEmbeddingsText() {
        return hideEmbeddingsText;
    }

    public void setHideEmbeddingsText(boolean hideEmbeddingsText) {
        this.hideEmbeddingsText = hideEmbeddingsText;
    }

    public boolean isHideEmbeddingsVectors() {
        return hideEmbeddingsVectors;
    }

    public void setHideEmbeddingsVectors(boolean hideEmbeddingsVectors) {
        this.hideEmbeddingsVectors = hideEmbeddingsVectors;
    }

    public boolean isHideLlmInvocationParameters() {
        return hideLlmInvocationParameters;
    }

    public void setHideLlmInvocationParameters(boolean hideLlmInvocationParameters) {
        this.hideLlmInvocationParameters = hideLlmInvocationParameters;
    }

    public boolean isHideLlmTools() {
        return hideLlmTools;
    }

    public void setHideLlmTools(boolean hideLlmTools) {
        this.hideLlmTools = hideLlmTools;
    }

    public boolean isHideInputs() {
        return hideInputs;
    }

    public void setHideInputs(boolean hideInputs) {
        this.hideInputs = hideInputs;
    }

    public boolean isHideInputImages() {
        return hideInputImages;
    }

    public void setHideInputImages(boolean hideInputImages) {
        this.hideInputImages = hideInputImages;
    }

    public boolean isHideInputMessages() {
        return hideInputMessages;
    }

    public void setHideInputMessages(boolean hideInputMessages) {
        this.hideInputMessages = hideInputMessages;
    }

    public boolean isHideInputText() {
        return hideInputText;
    }

    public void setHideInputText(boolean hideInputText) {
        this.hideInputText = hideInputText;
    }

    public boolean isHideOutputs() {
        return hideOutputs;
    }

    public void setHideOutputs(boolean hideOutputs) {
        this.hideOutputs = hideOutputs;
    }

    public boolean isHideOutputText() {
        return hideOutputText;
    }

    public void setHideOutputText(boolean hideOutputText) {
        this.hideOutputText = hideOutputText;
    }

    public boolean isHideOutputMessages() {
        return hideOutputMessages;
    }

    public void setHideOutputMessages(boolean hideOutputMessages) {
        this.hideOutputMessages = hideOutputMessages;
    }

}
