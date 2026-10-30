package io.arconia.observation.openinference.ai.instrumentation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceJson}.
 */
class OpenInferenceJsonTests {

    @Test
    void isValidShouldAcceptWellFormedJson() {
        assertThat(OpenInferenceJson.isValid("""
                {"type": "object", "properties": {"location": {"type": "string"}}}""")).isTrue();
        assertThat(OpenInferenceJson.isValid("[1, 2, 3]")).isTrue();
        assertThat(OpenInferenceJson.isValid("null")).isTrue();
        assertThat(OpenInferenceJson.isValid("\"text\"")).isTrue();
    }

    @Test
    void isValidShouldRejectMalformedOrBlankText() {
        assertThat(OpenInferenceJson.isValid("{\"location\":\"Par")).isFalse();
        assertThat(OpenInferenceJson.isValid("location: string")).isFalse();
        assertThat(OpenInferenceJson.isValid("{} trailing")).isFalse();
        assertThat(OpenInferenceJson.isValid(" ")).isFalse();
    }

    @Test
    void isObjectOrArrayShouldJudgeByTheOuterCharacters() {
        assertThat(OpenInferenceJson.isObjectOrArray(" {\"a\": 1} ")).isTrue();
        assertThat(OpenInferenceJson.isObjectOrArray("[1, 2]")).isTrue();
        assertThat(OpenInferenceJson.isObjectOrArray("rainy, 57°F")).isFalse();
        assertThat(OpenInferenceJson.isObjectOrArray("\"text\"")).isFalse();
        assertThat(OpenInferenceJson.isObjectOrArray("{")).isFalse();
    }

    @Test
    void isObjectShouldAcceptObjectsOnly() {
        assertThat(OpenInferenceJson.isObject("{\"a\": 1}")).isTrue();
        assertThat(OpenInferenceJson.isObject("[1, 2]")).isFalse();
    }

}
