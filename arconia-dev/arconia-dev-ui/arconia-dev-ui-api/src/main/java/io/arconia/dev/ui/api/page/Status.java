package io.arconia.dev.ui.api.page;

import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * The status of what a {@link Card} is about, as a word or two and what they mean, such as
 * "Running" for a container that works as expected.
 *
 * @param text the status in a word or two (e.g. {@code Running})
 * @param level what the status means, which decides how it is shown
 */
@Incubating
public record Status(String text, StatusLevel level) {

    public Status {
        Assert.hasText(text, "text cannot be null or empty");
        Assert.notNull(level, "level cannot be null");
    }

}
