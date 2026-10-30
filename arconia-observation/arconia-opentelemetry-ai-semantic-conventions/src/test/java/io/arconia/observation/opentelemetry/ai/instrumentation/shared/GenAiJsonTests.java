package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GenAiJson}.
 */
class GenAiJsonTests {

    @Test
    void isValidShouldAcceptWellFormedJson() {
        assertThat(GenAiJson.isValid("""
                {"type": "object", "properties": {"location": {"type": "string"}}}""")).isTrue();
        assertThat(GenAiJson.isValid("[1, 2, 3]")).isTrue();
        assertThat(GenAiJson.isValid("null")).isTrue();
        assertThat(GenAiJson.isValid("\"text\"")).isTrue();
    }

    @Test
    void isValidShouldRejectMalformedOrBlankText() {
        assertThat(GenAiJson.isValid("{\"location\":\"Par")).isFalse();
        assertThat(GenAiJson.isValid("location: string")).isFalse();
        assertThat(GenAiJson.isValid("{} trailing")).isFalse();
        assertThat(GenAiJson.isValid(" ")).isFalse();
    }

    @Test
    void isObjectOrArrayShouldJudgeByTheOuterCharacters() {
        assertThat(GenAiJson.isObjectOrArray(" {\"a\": 1} ")).isTrue();
        assertThat(GenAiJson.isObjectOrArray("[1, 2]")).isTrue();
        assertThat(GenAiJson.isObjectOrArray("rainy, 57°F")).isFalse();
        assertThat(GenAiJson.isObjectOrArray("\"text\"")).isFalse();
        assertThat(GenAiJson.isObjectOrArray("{")).isFalse();
    }

    @Test
    void isObjectShouldAcceptObjectsOnly() {
        assertThat(GenAiJson.isObject("{\"a\": 1}")).isTrue();
        assertThat(GenAiJson.isObject("[1, 2]")).isFalse();
    }

}
