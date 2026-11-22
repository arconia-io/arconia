import { html, nothing } from 'lit';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/text-field';
import { ArconiaElement } from './arconia-element.js';
import { UNREACHABLE } from './api.js';
import { iconPath } from './paths.js';

/**
 * The number of lines kept on screen. Older lines are dropped as new ones arrive.
 */
const MAX_LINES = 5000;

/**
 * Shows the lines of a log as they arrive: a toolbar to filter them by the text they contain,
 * to copy the ones shown, to clear the ones received so far and to jump back to the latest, above the lines
 * themselves, in an area that scrolls on its own.
 * <p>
 * Given the address of a log in the API of the Dev UI, the element follows it for as long as
 * it is on the page. Otherwise whoever uses the element adds the lines to it, and can say how
 * a line is drawn, what text it is searched by, and what else decides whether it is shown.
 */
class ArconiaLogView extends ArconiaElement {

    static properties = {
        // The address of a log of plain lines to follow, if any.
        url: { type: String },
        // What there is to say about the log, such as its having ended.
        status: { type: String },
        // What comes first in the toolbar, before the filter.
        leading: { attribute: false },
        // The text the lines must contain to be shown.
        filter: { state: true },
        // Whether the end of the log is in view, where new lines appear.
        atEnd: { state: true },
        // How many lines are kept, and how many of them are shown.
        total: { state: true },
        shown: { state: true },
        // Whether the lines have just been copied, which the button says for a moment.
        copied: { state: true },
    };

    constructor() {
        super();
        this.filter = '';
        this.atEnd = true;
        this.total = 0;
        this.shown = 0;
        // The lines received since the last time the screen was updated. A log can produce
        // many lines at once, so they are added to the screen together, once per frame.
        this.pending = [];
        // How a line is drawn, what text it is searched by and copied as, and what else decides
        // whether it is shown. By default a line is a text.
        this.lineElement = (line) => linked(Object.assign(document.createElement('div'), { className: 'line', textContent: line }));
        this.lineText = (line) => line;
        this.lineCopy = (line) => line;
        this.accepts = () => true;
    }

    updated(changes) {
        if (changes.has('url') && this.url) {
            this.follow();
        }
    }

    disconnectedCallback() {
        this.events?.close();
        cancelAnimationFrame(this.frame);
        this.frame = undefined;
        clearTimeout(this.copiedTimer);
        super.disconnectedCallback();
    }

    follow() {
        this.events?.close();
        this.clear();
        this.status = undefined;
        this.events = new EventSource(this.url);
        this.events.onmessage = (event) => this.append(event.data);
        this.events.addEventListener('end', () => this.stop('The log has ended.'));
        this.events.addEventListener('failure', (event) => this.stop(`The log cannot be followed: ${event.data}`));
        // The browser would connect again on its own and the log would start over.
        this.events.onerror = () => this.stop(UNREACHABLE);
    }

    stop(status) {
        this.events.close();
        this.status = status;
    }

    get output() {
        return this.querySelector('.lines');
    }

    /**
     * Adds a line to the log.
     */
    append(line) {
        this.pending.push(line);
        // A tab in the background is not drawn, and its lines wait. Only the most recent ones
        // would be shown anyway.
        if (this.pending.length > 2 * MAX_LINES) {
            this.pending = this.pending.slice(-MAX_LINES);
        }
        this.frame ??= requestAnimationFrame(() => {
            this.frame = undefined;
            this.show(this.pending);
            this.pending = [];
        });
    }

    /**
     * Shows lines, drops the oldest ones beyond the limit, and keeps the end of the log in
     * view unless the developer scrolled up. Each line is an element that knows its line, and
     * is hidden when the filters don't let it through.
     */
    show(lines) {
        const output = this.output;
        const atEnd = this.isAtEnd();
        output.append(...lines.map((line) => {
            const element = this.lineElement(line);
            element.line = line;
            element.hidden = !this.matches(line);
            return element;
        }));
        while (output.childElementCount > MAX_LINES) {
            output.firstElementChild.remove();
        }
        this.count();
        if (atEnd) {
            this.jumpToEnd();
        }
    }

    matches(line) {
        const filter = this.filter.trim().toLowerCase();
        return this.accepts(line) && (!filter || this.lineText(line).toLowerCase().includes(filter));
    }

    /**
     * Applies the filters again to the lines on screen, after one of them has changed.
     */
    refilter() {
        for (const element of this.output.children) {
            element.hidden = !this.matches(element.line);
        }
        this.count();
        this.jumpToEnd();
    }

    count() {
        this.total = this.output.childElementCount;
        this.shown = this.output.querySelectorAll(':scope > :not([hidden])').length;
    }

    isAtEnd() {
        const output = this.output;
        return output.scrollHeight - output.scrollTop - output.clientHeight < 40;
    }

    jumpToEnd() {
        this.output.scrollTop = this.output.scrollHeight;
        this.atEnd = true;
    }

    /**
     * Copies the lines that are shown, the ones the filters let through, as plain text.
     */
    async copy() {
        const lines = [...this.output.children].filter((element) => !element.hidden).map((element) => this.lineCopy(element.line));
        try {
            await navigator.clipboard.writeText(lines.join('\n'));
            this.copied = true;
            clearTimeout(this.copiedTimer);
            this.copiedTimer = setTimeout(() => { this.copied = false; }, 2000);
        } catch { /* The browser refused, and the button says nothing. */ }
    }

    /**
     * Removes the lines received so far from the screen. The log itself is not affected, and
     * the lines that follow are shown as they arrive.
     */
    clear() {
        this.pending = [];
        this.output?.replaceChildren();
        this.total = 0;
        this.shown = 0;
        this.atEnd = true;
    }

    render() {
        const count = this.shown === this.total ? `${this.total} lines` : `${this.shown} of ${this.total} lines`;
        return html`
            <div class="toolbar">
                ${this.leading ?? nothing}
                <vaadin-text-field theme="small" placeholder="Filter" accessible-name="Filter the lines of the log" clear-button-visible
                        .value=${this.filter} @value-changed=${(event) => { this.filter = event.detail.value; this.refilter(); }}>
                    <vaadin-icon slot="prefix" src=${iconPath('search')}></vaadin-icon>
                </vaadin-text-field>
                <span class="count">${count}${this.status ? '. ' : nothing}<span role="status">${this.status ?? nothing}</span></span>
                ${this.atEnd ? nothing : html`
                    <vaadin-button theme="tertiary small" @click=${() => this.jumpToEnd()}>
                        <vaadin-icon slot="prefix" src=${iconPath('arrow-down-to-line')}></vaadin-icon>
                        Jump to latest
                    </vaadin-button>
                `}
                ${navigator.clipboard ? html`
                    <vaadin-button theme="tertiary small" ?disabled=${!this.shown} @click=${() => this.copy()}>
                        <vaadin-icon slot="prefix" src=${iconPath(this.copied ? 'check' : 'copy')}></vaadin-icon>
                        ${this.copied ? 'Copied' : 'Copy'}
                    </vaadin-button>
                ` : nothing}
                <vaadin-button theme="tertiary small" ?disabled=${!this.total} @click=${() => this.clear()}>
                    <vaadin-icon slot="prefix" src=${iconPath('eraser')}></vaadin-icon>
                    Clear
                </vaadin-button>
            </div>
            <div class="lines" ?data-empty=${!this.shown} tabindex="0" role="log" aria-live="off" aria-label="Log" @scroll=${() => { this.atEnd = this.isAtEnd(); }}></div>
        `;
    }

}

/**
 * A web address in a text, up to the first space or quote. What ends a sentence or closes a
 * bracket right after it is not part of it.
 */
const WEB_ADDRESS = /https?:\/\/[^\s<>"'`]+/g;

const TRAILING_PUNCTUATION = '.,;:!?)]}';

/**
 * A web address without what ends a sentence or closes a bracket right after it. The characters
 * are dropped one by one from the end, which takes no longer than the address is long, whatever
 * it contains.
 */
function withoutTrailingPunctuation(address) {
    let end = address.length;
    while (end > 0 && TRAILING_PUNCTUATION.includes(address[end - 1])) {
        end--;
    }
    return address.substring(0, end);
}

/**
 * Turns the web addresses in the text of an element into links, which open in a new tab. The
 * rest of the text is left as it is.
 */
export function linked(element) {
    const text = element.textContent;
    const parts = [];
    let end = 0;
    for (const match of text.matchAll(WEB_ADDRESS)) {
        const address = withoutTrailingPunctuation(match[0]);
        const link = document.createElement('a');
        link.href = address;
        link.textContent = address;
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        parts.push(text.substring(end, match.index), link);
        end = match.index + address.length;
    }
    if (parts.length) {
        element.replaceChildren(...parts, text.substring(end));
    }
    return element;
}

customElements.define('arconia-log-view', ArconiaLogView);
