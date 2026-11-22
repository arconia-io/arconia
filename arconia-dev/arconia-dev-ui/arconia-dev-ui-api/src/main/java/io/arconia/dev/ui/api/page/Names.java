package io.arconia.dev.ui.api.page;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * The rule for the names used in the addresses of the Dev UI and for the names of icons:
 * lowercase letters, digits, and single hyphens between them.
 */
final class Names {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*+");

    private Names() {}

    /**
     * The identifier of the page the Dev UI opens on, which no other page can have.
     */
    static final String OVERVIEW_ID = "overview";

    /**
     * Checks a name against the rule, and returns it. The message of a failure carries the
     * name, so that it tells which of the pages or cards of an application is at fault.
     */
    static String validate(@Nullable String name, String what) {
        Assert.hasText(name, what + " cannot be null or empty");
        Assert.isTrue(PATTERN.matcher(name).matches(),
                () -> "%s '%s' can only contain lowercase letters, digits, and single hyphens between them".formatted(what, name));
        return name;
    }

    /**
     * Checks the identifier of a page, which follows the rule and is not the one of the
     * Overview, and returns it.
     */
    static String validatePageId(@Nullable String id) {
        String pageId = validate(id, "id");
        Assert.isTrue(!OVERVIEW_ID.equals(pageId), () -> "id '%s' is reserved for the overview of the Dev UI".formatted(OVERVIEW_ID));
        return pageId;
    }

}
