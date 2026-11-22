import { html, nothing } from 'lit';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/select';
import { getJson, RETRY_DELAY, WAITING_HEADING } from './api.js';
import { ArconiaElement } from './arconia-element.js';
import { linked } from './arconia-log-view.js';
import { basePath, iconPath } from './paths.js';

/**
 * The levels of a line, from the least to the most severe. The console can be set to show the
 * lines from a level up.
 */
const LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];

const LEVEL_ITEMS = LEVELS.map((level, index) => ({ label: index ? `${level} and above` : 'All levels', value: level }));

/**
 * Where the choices of the developer are remembered in the browser: whether the console is
 * open, and how tall.
 */
const STORAGE_KEY = 'arconia-dev-ui-console';

const DEFAULT_HEIGHT = 280;

const MIN_HEIGHT = 120;

/**
 * The most of the height of the window the console can take, so that there is always a page
 * above it.
 */
const MAX_HEIGHT = 0.6;

/**
 * The console of the Dev UI, docked at the bottom of every page: the logs of the application,
 * followed live while the console is open. Closed, it is a bar that takes one line.
 * <p>
 * The lines are shown by a log view, which the console adds a choice of level to. The
 * developer can change the height of the console by dragging its top edge.
 */
class ArconiaConsole extends ArconiaElement {

    static properties = {
        opened: { state: true },
        height: { state: true },
        // The least severe level to show.
        level: { state: true },
        status: { state: true },
    };

    constructor() {
        super();
        const choices = readChoices();
        this.opened = choices.opened === true;
        this.height = Number.isFinite(choices.height) ? choices.height : DEFAULT_HEIGHT;
        this.level = 'TRACE';
    }

    /**
     * The logs are followed for as long as the console is open.
     */
    updated(changes) {
        if (changes.has('opened')) {
            if (this.opened) {
                this.follow();
            } else {
                this.unfollow();
            }
        }
    }

    disconnectedCallback() {
        this.unfollow();
        super.disconnectedCallback();
    }

    get view() {
        return this.querySelector('arconia-log-view');
    }

    toggle() {
        this.opened = !this.opened;
        this.remember();
    }

    remember() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify({ opened: this.opened, height: this.height }));
        } catch { /* The choices last until the page is closed. */ }
    }

    /**
     * Follows the logs. Each time it connects, the server sends the lines it has kept first, so
     * the console starts over from them and misses none. When the connection is lost, the
     * console connects again on its own, and once the application is back it asks it
     * something: any answer tells the Dev UI whether the application was restarted.
     */
    follow() {
        this.unfollow();
        this.events = new EventSource(`${basePath}/api/logs`);
        this.events.onopen = () => {
            this.view?.clear();
            if (this.status) {
                this.status = undefined;
                getJson('pages').catch(() => {});
            }
        };
        this.events.onmessage = (event) => this.view?.append(JSON.parse(event.data));
        this.events.onerror = () => {
            this.unfollow();
            this.status = `${WAITING_HEADING}...`;
            this.retry = setTimeout(() => this.follow(), RETRY_DELAY);
        };
    }

    unfollow() {
        clearTimeout(this.retry);
        this.events?.close();
        this.events = undefined;
    }

    /**
     * Whether the level of a line is one to show. A marker always is: it tells where one run
     * of the application ends and the next begins.
     */
    accepts(line) {
        return line.marker || LEVELS.indexOf(line.level) >= LEVELS.indexOf(this.level);
    }

    /**
     * Dragging the top edge of the console changes its height, between a few lines and a good
     * part of the window.
     */
    startResizing(event) {
        event.preventDefault();
        const startY = event.clientY;
        const startHeight = this.height;
        const resize = (move) => {
            this.height = Math.round(Math.min(Math.max(startHeight + startY - move.clientY, MIN_HEIGHT), window.innerHeight * MAX_HEIGHT));
        };
        const stop = () => {
            window.removeEventListener('pointermove', resize);
            window.removeEventListener('pointerup', stop);
            window.removeEventListener('pointercancel', stop);
            this.remember();
        };
        window.addEventListener('pointermove', resize);
        window.addEventListener('pointerup', stop);
        window.addEventListener('pointercancel', stop);
    }

    render() {
        const title = html`
            <vaadin-button class="title" theme="tertiary" aria-expanded=${this.opened} @click=${() => this.toggle()}>
                <vaadin-icon slot="prefix" src=${iconPath('terminal')} aria-hidden="true"></vaadin-icon>
                Logs
                <vaadin-icon slot="suffix" class="chevron" src=${iconPath(this.opened ? 'chevron-down' : 'chevron-up')} aria-hidden="true"></vaadin-icon>
            </vaadin-button>
        `;
        if (!this.opened) {
            return html`<div class="bar">${title}</div>`;
        }
        const leading = html`
            ${title}
            <vaadin-select aria-label="Level" theme="small" .value=${this.level} .items=${LEVEL_ITEMS}
                    @value-changed=${(event) => { this.level = event.detail.value; this.view?.refilter(); }}>
            </vaadin-select>
        `;
        return html`
            <div class="resizer" @pointerdown=${(event) => this.startResizing(event)}></div>
            <arconia-log-view style="height: ${this.height}px" .leading=${leading} .status=${this.status}
                    .lineElement=${lineElement} .lineText=${lineText} .lineCopy=${lineCopy} .accepts=${(line) => this.accepts(line)}>
            </arconia-log-view>
        `;
    }

}

function readChoices() {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? {};
    } catch {
        return {};
    }
}

/**
 * The element of a line of the logs of the application: its time, its level, its logger,
 * shortened to its last part, and its message, with the stack trace folded below when there
 * is one. A marker is a line of its own kind.
 */
function lineElement(line) {
    const element = document.createElement('div');
    if (line.marker) {
        element.className = 'marker';
        element.textContent = `${time(line.time)}  ${line.message}`;
        return element;
    }
    element.className = `line ${line.level.toLowerCase()}`;
    element.append(
        span('time', time(line.time)),
        span('level', line.level),
        span('logger', shortLogger(line.logger), line.logger),
        linked(span('message', line.message)),
    );
    if (line.stackTrace) {
        const [summary, ...frames] = line.stackTrace.split('\n');
        const details = document.createElement('details');
        details.append(Object.assign(document.createElement('summary'), { textContent: summary }),
            Object.assign(document.createElement('pre'), { textContent: frames.join('\n') }));
        element.append(details);
    }
    return element;
}

/**
 * A logger named after a class is shown as the name of the class, without its package. Any
 * other name is shown whole, such as the one of a container, named after its image.
 */
const CLASS_NAME = /^[\w$]+(\.[\w$]+)*$/;

function shortLogger(logger) {
    return CLASS_NAME.test(logger) ? logger.substring(logger.lastIndexOf('.') + 1) : logger;
}

function lineText(line) {
    return `${line.logger ?? ''} ${line.message} ${line.stackTrace ?? ''}`;
}

/**
 * A line as plain text, the way a log file would have it, with its stack trace in full.
 */
function lineCopy(line) {
    const text = line.marker ? `${time(line.time)} ${line.message}` : `${time(line.time)} ${line.level.padEnd(5)} ${line.logger} : ${line.message}`;
    return line.stackTrace ? `${text}\n${line.stackTrace}` : text;
}

function span(className, text, title) {
    const element = document.createElement('span');
    element.className = className;
    element.textContent = text;
    if (title) {
        element.title = title;
    }
    return element;
}

/**
 * The time of day of a line, to the millisecond, which is what tells the lines of a burst apart.
 */
function time(milliseconds) {
    const date = new Date(milliseconds);
    const pad = (value, length = 2) => String(value).padStart(length, '0');
    return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`;
}

customElements.define('arconia-console', ArconiaConsole);
