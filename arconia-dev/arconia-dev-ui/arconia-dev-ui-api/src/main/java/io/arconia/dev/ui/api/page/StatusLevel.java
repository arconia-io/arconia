package io.arconia.dev.ui.api.page;

import io.arconia.core.annotations.Incubating;

/**
 * What a {@link Status} means, which decides how it is shown.
 */
@Incubating
public enum StatusLevel {

    /**
     * Everything is as expected.
     */
    SUCCESS,

    /**
     * Something deserves attention, but nothing is broken.
     */
    WARNING,

    /**
     * Something is broken.
     */
    ERROR,

    /**
     * The status is neither good nor bad, or it is not known.
     */
    NEUTRAL

}
