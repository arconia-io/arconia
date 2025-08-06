package io.arconia.docling.client.convert.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZipTarget}.
 */
class ZipTargetTests {

    @Test
    void whenValidParametersThenCreateZipTarget() {
        ZipTarget zipTarget = new ZipTarget(Target.Kind.ZIP);

        assertThat(zipTarget.kind()).isEqualTo(Target.Kind.ZIP);
    }

    @Test
    void kindIsAlwaysSetToZip() {
        ZipTarget zipTarget = new ZipTarget(Target.Kind.PUT);

        assertThat(zipTarget.kind()).isEqualTo(Target.Kind.ZIP);
    }

    @Test
    void createStaticMethodCreatesZipTarget() {
        ZipTarget zipTarget = ZipTarget.create();

        assertThat(zipTarget.kind()).isEqualTo(Target.Kind.ZIP);
    }

}
