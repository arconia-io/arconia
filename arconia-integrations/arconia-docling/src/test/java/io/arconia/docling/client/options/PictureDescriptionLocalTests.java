package io.arconia.docling.client.options;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.docling.client.convert.request.options.PictureDescriptionLocal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PictureDescriptionLocal}.
 */
class PictureDescriptionLocalTests {

    @Test
    void createLocalWithAllFields() {
        String repoId = "microsoft/Florence-2-large";
        String prompt = "Describe this image in detail";
        Map<String, Object> generationConfig = Map.of("max_length", 100, "temperature", 0.7);

        PictureDescriptionLocal local = new PictureDescriptionLocal(
                repoId,
                prompt,
                generationConfig
        );

        assertThat(local.repoId()).isEqualTo(repoId);
        assertThat(local.prompt()).isEqualTo(prompt);
        assertThat(local.generationConfig()).isEqualTo(generationConfig);
    }

    @Test
    void createLocalWithOnlyRequiredFields() {
        String repoId = "microsoft/Florence-2-large";

        PictureDescriptionLocal local = new PictureDescriptionLocal(
                repoId,
                null,
                null
        );

        assertThat(local.repoId()).isEqualTo(repoId);
        assertThat(local.prompt()).isNull();
        assertThat(local.generationConfig()).isNull();
    }

    @Test
    void createLocalWithNullRepoIdThrowsException() {
        assertThatThrownBy(() -> new PictureDescriptionLocal(
                null,
                null,
                null
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("repoId cannot be null or empty");
    }

    @Test
    void createLocalWithEmptyRepoIdThrowsException() {
        assertThatThrownBy(() -> new PictureDescriptionLocal(
                "",
                null,
                null
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("repoId cannot be null or empty");
    }

    @Test
    void createLocalWithBlankRepoIdThrowsException() {
        assertThatThrownBy(() -> new PictureDescriptionLocal(
                "   ",
                null,
                null
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("repoId cannot be null or empty");
    }

    @Test
    void pictureDescriptionLocalIsImmutable() {
        String repoId = "microsoft/Florence-2-large";
        Map<String, Object> generationConfig = new HashMap<>(Map.of("max_length", 100));

        PictureDescriptionLocal local = new PictureDescriptionLocal(
                repoId,
                "Original prompt",
                generationConfig
        );

        assertThat(local.generationConfig()).isEqualTo(generationConfig);

        generationConfig.put("temperature", 0.8);

        assertThat(local.generationConfig()).hasSize(1);
        assertThat(local.generationConfig().get("max_length")).isEqualTo(100);
        assertThat(local.generationConfig()).doesNotContainKey("temperature");
    }

}
