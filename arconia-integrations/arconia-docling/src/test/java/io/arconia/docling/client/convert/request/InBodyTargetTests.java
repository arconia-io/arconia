package io.arconia.docling.client.convert.request;

import org.junit.jupiter.api.Test;

import io.arconia.docling.client.convert.request.target.InBodyTarget;
import io.arconia.docling.client.convert.request.target.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InBodyTarget}.
 */
class InBodyTargetTests {

    @Test
    void whenValidParametersThenCreateInBodyTarget() {
        InBodyTarget inBodyTarget = InBodyTarget.create();

        assertThat(inBodyTarget.kind()).isEqualTo(Target.Kind.INBODY);
    }

    @Test
    void kindIsAlwaysSetToInBody() {
        InBodyTarget inBodyTarget = new InBodyTarget(Target.Kind.PUT);

        assertThat(inBodyTarget.kind()).isEqualTo(Target.Kind.INBODY);
    }

}
