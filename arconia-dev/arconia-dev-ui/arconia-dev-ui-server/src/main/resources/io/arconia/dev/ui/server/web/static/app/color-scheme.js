/**
 * The color scheme of the Dev UI. It follows the operating system until the developer picks
 * the light or the dark one, which is then remembered in this browser.
 *
 * The script at the top of the HTML page applies the remembered scheme before anything is
 * shown, and must use the same key.
 */
const STORAGE_KEY = 'arconia-dev-ui-color-scheme';

/**
 * The choices, in the order the button in the top bar goes through them.
 */
const CHOICES = ['system', 'light', 'dark'];

/**
 * The current choice: "system" to follow the operating system, "light", or "dark".
 */
export function colorScheme() {
    const scheme = document.documentElement.style.colorScheme;
    return scheme === 'light' || scheme === 'dark' ? scheme : 'system';
}

/**
 * Moves to the next choice, applies it, and remembers it. Returns the new choice.
 */
export function nextColorScheme() {
    const scheme = CHOICES[(CHOICES.indexOf(colorScheme()) + 1) % CHOICES.length];
    // Without a scheme of its own, the page follows the operating system (see app.css).
    document.documentElement.style.colorScheme = scheme === 'system' ? '' : scheme;
    try {
        if (scheme === 'system') {
            localStorage.removeItem(STORAGE_KEY);
        } else {
            localStorage.setItem(STORAGE_KEY, scheme);
        }
    } catch {
        // The choice still applies to this page when the browser storage is not available.
    }
    return scheme;
}
