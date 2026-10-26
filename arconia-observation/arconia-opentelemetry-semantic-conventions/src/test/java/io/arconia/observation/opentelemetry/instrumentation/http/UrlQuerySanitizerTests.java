package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UrlQuerySanitizer}.
 */
class UrlQuerySanitizerTests {

    private final List<String> defaultSensitiveParameters = new OpenTelemetryHttpOptions().getSensitiveQueryParameters();

    @Test
    void redactsDefaultSensitiveParameterValues() {
        assertThat(UrlQuerySanitizer.sanitize(
                "AWSAccessKeyId=a&Signature=b&X-Amz-Signature=c&X-Amz-Credential=d&X-Amz-Security-Token=e&sig=f&X-Goog-Signature=g",
                defaultSensitiveParameters))
                .isEqualTo("AWSAccessKeyId=REDACTED&Signature=REDACTED&X-Amz-Signature=REDACTED&X-Amz-Credential=REDACTED"
                        + "&X-Amz-Security-Token=REDACTED&sig=REDACTED&X-Goog-Signature=REDACTED");
    }

    @Test
    void preservesOtherParametersAndOrder() {
        assertThat(UrlQuerySanitizer.sanitize("q=OpenTelemetry&sig=secret&page=2", defaultSensitiveParameters))
                .isEqualTo("q=OpenTelemetry&sig=REDACTED&page=2");
    }

    @Test
    void matchesKeysCaseSensitively() {
        assertThat(UrlQuerySanitizer.sanitize("SIG=secret&x-amz-signature=secret", defaultSensitiveParameters))
                .isEqualTo("SIG=secret&x-amz-signature=secret");
    }

    @Test
    void keepsParametersWithoutValueAndEmptySegments() {
        assertThat(UrlQuerySanitizer.sanitize("flag&sig&&q=1", defaultSensitiveParameters)).isEqualTo("flag&sig&&q=1");
    }

    @Test
    void keepsEmptyQuery() {
        assertThat(UrlQuerySanitizer.sanitize("", defaultSensitiveParameters)).isEmpty();
    }

    @Test
    void redactsNothingWithEmptyList() {
        assertThat(UrlQuerySanitizer.sanitize("sig=secret", List.of())).isEqualTo("sig=secret");
    }

}
