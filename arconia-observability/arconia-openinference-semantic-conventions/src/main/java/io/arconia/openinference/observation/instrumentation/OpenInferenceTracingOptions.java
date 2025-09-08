package io.arconia.openinference.observation.instrumentation;

/**
 * Options for customizing the OpenInference tracing instrumentation.
 */
public class OpenInferenceTracingOptions {

    public static final String REDACTED_PLACEHOLDER = "__REDACTED__";

    /**
     * Maximum length of a base64-encoded image.
     */
    private long base64ImageMaxLength = 32_000;

    /**
     * Whether to hide all embedding vectors.
     */
    private boolean hideEmbeddingVectors = false;

    /**
     * Whether to hide the LLM invocation parameters.
     */
    private boolean hideLlmInvocationParameters = false;

    /**
     * Whether to hide all inputs.
     */
    private boolean hideInputs = false;

    /**
     * Whether to hide all images from the input messages.
     */
    private boolean hideInputImages = false;

    /**
     * Whether to hide all inputs messages.
     */
    private boolean hideInputMessages = false;

    /**
     * Whether to hide all texts from the input messages.
     */
    private boolean hideInputText = false;

    /**
     * Whether to hide all output messages.
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

    /**
     * Whether to hide all LLM prompts.
     */
    private boolean hidePrompts = false;

    public long getBase64ImageMaxLength() {
        return base64ImageMaxLength;
    }

    public void setBase64ImageMaxLength(long base64ImageMaxLength) {
        this.base64ImageMaxLength = base64ImageMaxLength;
    }

    public boolean isHideEmbeddingVectors() {
        return hideEmbeddingVectors;
    }

    public void setHideEmbeddingVectors(boolean hideEmbeddingVectors) {
        this.hideEmbeddingVectors = hideEmbeddingVectors;
    }

    public boolean isHideLlmInvocationParameters() {
        return hideLlmInvocationParameters;
    }

    public void setHideLlmInvocationParameters(boolean hideLlmInvocationParameters) {
        this.hideLlmInvocationParameters = hideLlmInvocationParameters;
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

    public boolean isHidePrompts() {
        return hidePrompts;
    }

    public void setHidePrompts(boolean hidePrompts) {
        this.hidePrompts = hidePrompts;
    }

}
