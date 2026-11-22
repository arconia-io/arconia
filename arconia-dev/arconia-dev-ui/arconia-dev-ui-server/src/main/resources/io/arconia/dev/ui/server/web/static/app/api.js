import { applicationStartedAt, basePath } from './paths.js';

/**
 * The header of the responses of the API that carries when the application started.
 */
const STARTED_AT_HEADER = 'Arconia-Dev-UI-Started-At';

/**
 * Where the Dev UI notes, across a reload of the page, that it reloaded because the
 * application was restarted.
 */
const RESTARTED_KEY = 'arconia-dev-ui-restarted';

/**
 * How long to wait, in milliseconds, before asking again an application that cannot be
 * reached, as while it is being restarted.
 */
export const RETRY_DELAY = 1500;

/**
 * What the Dev UI says when the application cannot be reached, and while it waits for it.
 */
export const UNREACHABLE = 'The application is not responding. It may be restarting.';
export const WAITING_HEADING = 'Waiting for the application';

/**
 * Reads JSON from the API of the Dev UI. When the request fails, the error carries the message
 * the server reports and the status of the response. When the application cannot be reached at all, as while it is being
 * restarted, the error says so with its "unreachable" property, so that the caller can try
 * again.
 */
export async function getJson(path) {
    let response;
    try {
        response = await fetch(`${basePath}/api/${path}`, { headers: { Accept: 'application/json' } });
    } catch {
        throw unreachable();
    }
    if (response.status === 503) {
        throw unreachable();
    }
    await reloadIfRestarted(response);
    if (!response.ok) {
        throw Object.assign(new Error(await errorMessage(response)), { status: response.status });
    }
    return response.json();
}

/**
 * Asks the Dev UI to do something that changes the application, such as restarting it. The
 * request carries the header by which the server knows that it comes from the Dev UI itself.
 * Errors are reported as for reading.
 */
export async function post(path) {
    let response;
    try {
        response = await fetch(`${basePath}/api/${path}`, { method: 'POST', headers: { Accept: 'application/json', 'Arconia-Dev-UI': '1' } });
    } catch {
        throw unreachable();
    }
    if (!response.ok) {
        throw new Error(await errorMessage(response));
    }
}

/**
 * Whether the page was just reloaded because the application was restarted. The answer is
 * given once.
 */
export function reloadedAfterRestart() {
    try {
        const restarted = sessionStorage.getItem(RESTARTED_KEY) !== null;
        sessionStorage.removeItem(RESTARTED_KEY);
        return restarted;
    } catch {
        return false;
    }
}

/**
 * A restarted application may have other pages, another version, and other content than the
 * one this page was loaded from. The page is loaded again, at the same address, and the
 * request that found out never completes.
 */
function reloadIfRestarted(response) {
    const startedAt = response.headers.get(STARTED_AT_HEADER);
    if (!startedAt || startedAt === applicationStartedAt) {
        return Promise.resolve();
    }
    restarted(startedAt);
    return new Promise(() => {});
}

function restarted(startedAt) {
    try {
        sessionStorage.setItem(RESTARTED_KEY, String(startedAt));
    } catch { /* The page is reloaded without saying why. */ }
    location.reload();
}

function unreachable() {
    const error = new Error(UNREACHABLE);
    error.unreachable = true;
    return error;
}

async function errorMessage(response) {
    try {
        const body = await response.json();
        if (body?.error?.message) {
            return body.error.message;
        }
    } catch {
        // The response is not an error of the Dev UI API.
    }
    return `The request failed with status ${response.status}`;
}
