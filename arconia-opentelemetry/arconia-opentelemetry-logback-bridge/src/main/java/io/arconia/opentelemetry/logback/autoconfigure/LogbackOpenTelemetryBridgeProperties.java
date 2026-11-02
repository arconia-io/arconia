package io.arconia.opentelemetry.logback.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX)
public class LogbackOpenTelemetryBridgeProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.logs.logback-bridge";

    /**
     * Whether to enable the Logback OpenTelemetry Bridge.
     */
    private boolean enabled = true;

    /**
     * Enable the capture of Logback log event arguments.
     */
    private boolean captureArguments = false;

    /**
     * Enable the capture of the Logback log event message template (if arguments are provided).
     */
    private boolean captureTemplate = false;

    /**
     * Enable the capture of source code attributes. Note that capturing source code attributes at logging sites might add a performance overhead.
     */
    private boolean captureCodeAttributes = false;

    /**
     * Enable the capture of experimental log attributes 'thread.name' and 'thread.id'.
     */
    private boolean captureExperimentalAttributes = false;

    /**
     * Enable the capture of Logback markers as attributes.
     */
    private boolean captureMarkerAttribute = false;

    /**
     * Structured log attributes to capture: SLF4J key-value pairs, Logstash markers, and Logstash structured arguments.
     */
    private final AttributeSelector structuredAttributes = new AttributeSelector();

    /**
     * MDC entries to capture as log attributes.
     */
    private final AttributeSelector mdcAttributes = new AttributeSelector();

    /**
     * Logback logger context properties to capture as log attributes.
     */
    private final AttributeSelector loggerContextAttributes = new AttributeSelector();

    /**
     * Log telemetry is emitted after the initialization of the OpenTelemetry Logback appender with an OpenTelemetry object. This setting allows you to modify the size of the cache used to replay the first logs. 'thread.id' attribute is not captured.
     */
    private int numLogsCapturedBeforeOtelInstall = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCaptureArguments() {
        return captureArguments;
    }

    public void setCaptureArguments(boolean captureArguments) {
        this.captureArguments = captureArguments;
    }

    public boolean isCaptureTemplate() {
        return captureTemplate;
    }

    public void setCaptureTemplate(boolean captureTemplate) {
        this.captureTemplate = captureTemplate;
    }

    public boolean isCaptureCodeAttributes() {
        return captureCodeAttributes;
    }

    public void setCaptureCodeAttributes(boolean captureCodeAttributes) {
        this.captureCodeAttributes = captureCodeAttributes;
    }

    public boolean isCaptureExperimentalAttributes() {
        return captureExperimentalAttributes;
    }

    public void setCaptureExperimentalAttributes(boolean captureExperimentalAttributes) {
        this.captureExperimentalAttributes = captureExperimentalAttributes;
    }

    public boolean isCaptureMarkerAttribute() {
        return captureMarkerAttribute;
    }

    public void setCaptureMarkerAttribute(boolean captureMarkerAttribute) {
        this.captureMarkerAttribute = captureMarkerAttribute;
    }

    public AttributeSelector getStructuredAttributes() {
        return structuredAttributes;
    }

    public AttributeSelector getMdcAttributes() {
        return mdcAttributes;
    }

    public AttributeSelector getLoggerContextAttributes() {
        return loggerContextAttributes;
    }

    public int getNumLogsCapturedBeforeOtelInstall() {
        return numLogsCapturedBeforeOtelInstall;
    }

    public void setNumLogsCapturedBeforeOtelInstall(int numLogsCapturedBeforeOtelInstall) {
        this.numLogsCapturedBeforeOtelInstall = numLogsCapturedBeforeOtelInstall;
    }

    /**
     * Selects the keys to capture as log attributes. Nothing is captured by default.
     */
    public static class AttributeSelector {

        /**
         * Case-sensitive glob patterns for the keys to capture. '*' matches any number of characters and '?' matches one character. Excluded patterns take precedence. Nothing is captured when both included and excluded are empty.
         */
        private List<String> included = new ArrayList<>();

        /**
         * Case-sensitive glob patterns for the keys not to capture. '*' matches any number of characters and '?' matches one character. Excluded patterns take precedence over included patterns. Configuring only excluded patterns captures every key they do not match.
         */
        private List<String> excluded = new ArrayList<>();

        public List<String> getIncluded() {
            return included;
        }

        public void setIncluded(List<String> included) {
            this.included = included;
        }

        public List<String> getExcluded() {
            return excluded;
        }

        public void setExcluded(List<String> excluded) {
            this.excluded = excluded;
        }

    }

}
