package io.arconia.docling.client.convert.request.options;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PictureDescriptionLocal(

        @JsonProperty("repo_id")
        String repoId,

        @JsonProperty("prompt")
        @Nullable
        String prompt,

        @JsonProperty("generation_config")
        @Nullable
        Map<String,Object> generationConfig

) {

    public PictureDescriptionLocal {
        Assert.hasText(repoId, "repoId cannot be null or empty");

        if (generationConfig != null) {
            generationConfig = new HashMap<>(generationConfig);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @Nullable private String repoId;
        @Nullable private String prompt;
        @Nullable private Map<String,Object> generationConfig;

        private Builder() {}

        /**
         * Repository id from the Hugging Face Hub.
         */
        public Builder repoId(String repoId) {
            this.repoId = repoId;
            return this;
        }

        /**
         * Prompt used when calling the vision-language model.
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * Config from <a href="https://huggingface.co/docs/transformers/en/main_classes/text_generation#transformers.GenerationConfig">Hugging Face</a>
         */
        public Builder generationConfig(Map<String,Object> generationConfig) {
            this.generationConfig = generationConfig;
            return this;
        }

        public PictureDescriptionLocal build() {
            return new PictureDescriptionLocal(repoId, prompt, generationConfig);
        }

    }

}
