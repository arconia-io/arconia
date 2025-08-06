package io.arconia.docling.client.convert.request;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PutTarget}.
 */
class PutTargetTests {

    @Test
    void whenUriIsNullThenThrow() {
        assertThatThrownBy(() -> new PutTarget(Target.Kind.PUT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URI cannot be null");
    }

    @Test
    void whenValidParametersThenCreatePutTarget() {
        URI uri = URI.create("https://example.com/upload");

        PutTarget putTarget = new PutTarget(Target.Kind.PUT, uri);

        assertThat(putTarget.kind()).isEqualTo(Target.Kind.PUT);
        assertThat(putTarget.uri()).isEqualTo(uri);
    }

    @Test
    void kindIsAlwaysSetToPut() {
        URI uri = URI.create("https://example.com/upload");

        PutTarget putTarget = new PutTarget(Target.Kind.INBODY, uri);

        assertThat(putTarget.kind()).isEqualTo(Target.Kind.PUT);
    }

}
