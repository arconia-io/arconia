package io.arconia.observation.openllmetry.instrumentation;

/**
 * Options for customizing the OpenLLMetry instrumentation.
 */
public class OpenLLMetryOptions {

    public static final String REDACTED_PLACEHOLDER = "__REDACTED__";

    /**
     * Whether to include input and output content in traces.
     */
    private boolean traceContent = true;

    /**
     * Whether to include tool definitions in traces.
     */
    private boolean includeToolDefinitions = true;

    public boolean isTraceContent() {
        return traceContent;
    }

    public void setTraceContent(boolean traceContent) {
        this.traceContent = traceContent;
    }

    public boolean isIncludeToolDefinitions() {
        return includeToolDefinitions;
    }

    public void setIncludeToolDefinitions(boolean includeToolDefinitions) {
        this.includeToolDefinitions = includeToolDefinitions;
    }

}
