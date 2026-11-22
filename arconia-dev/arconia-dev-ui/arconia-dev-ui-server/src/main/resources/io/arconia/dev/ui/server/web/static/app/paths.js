/**
 * What the server tells the frontend, as attributes of the root element of the Dev UI.
 */
const root = document.querySelector('arconia-dev-ui');

/**
 * The path the Dev UI is served at, context path of the application included.
 */
export const basePath = root.getAttribute('base-path');

/**
 * The path the icons are served at.
 */
const iconsPath = root.getAttribute('icons-path');

/**
 * The name of the application the Dev UI is part of, and its version when it is known.
 */
export const applicationName = root.getAttribute('application-name');
export const applicationVersion = root.getAttribute('application-version');

/**
 * When the application started, as the server tells it. The value changes when the application
 * is restarted.
 */
export const applicationStartedAt = root.getAttribute('application-started-at');

/**
 * Whether the logs of the application are captured, in which case the Dev UI has a console.
 */
export const applicationLogs = root.getAttribute('application-logs') === 'true';

/**
 * Whether the application can be restarted from the Dev UI.
 */
export const applicationRestart = root.getAttribute('application-restart') === 'true';

/**
 * The path of an icon, given its name in the icon set.
 */
export function iconPath(name) {
    return `${iconsPath}${name}.svg`;
}

/**
 * The path the pages of the Dev UI start with.
 */
const pagesPath = `${basePath}/pages/`;

/**
 * The path of a page of the Dev UI.
 */
export function pagePath(id) {
    return `${pagesPath}${id}`;
}

/**
 * Whether a path is the one the Dev UI opens at.
 */
export function isHomePath(path) {
    return path === basePath || path === `${basePath}/`;
}

/**
 * Whether the server answers a path with the Dev UI: the path it opens at, or the one of a
 * page. It answers any other path as not found.
 */
export function isDevUiPath(path) {
    return isHomePath(path) || path.startsWith(pagesPath);
}

/**
 * The identifier of the page at a path, or undefined when the path is not the one of a page.
 */
export function pageIdOf(path) {
    return path.startsWith(pagesPath) ? path.slice(pagesPath.length).match(/^[a-z0-9-]+$/)?.[0] : undefined;
}

/**
 * What the address of a table page calls the text its rows are filtered by.
 */
export const FILTER_PARAMETER = 'filter';

/**
 * The path of the page of the configuration properties, showing the ones under the given
 * prefix. The dot that ends the prefix keeps out the properties whose name only starts the
 * same way, as "mongodb-atlas" does for "mongodb".
 */
export function propertiesPath(prefix) {
    const filter = prefix.endsWith('.') ? prefix : `${prefix}.`;
    return `${pagePath('properties')}?${FILTER_PARAMETER}=${encodeURIComponent(filter)}`;
}

/**
 * The address of a log in the API of the Dev UI. A log is identified by the identifier of its
 * card and the label of its item.
 */
export function logPath(pageId, cardId, itemLabel) {
    const query = new URLSearchParams({ card: cardId, item: itemLabel });
    return `${basePath}/api/pages/${encodeURIComponent(pageId)}/logs?${query}`;
}
