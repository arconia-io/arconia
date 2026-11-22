import { html, nothing } from 'lit';
import { ArconiaElement } from './arconia-element.js';
import { UNREACHABLE, WAITING_HEADING } from './api.js';

/**
 * A message shown in place of content: nothing to show, something not found, or a failure.
 * A plain message has no panel and no mark, for what is only shown for a moment. A waiting
 * message says that the application cannot be reached and is asked again.
 */
class ArconiaMessage extends ArconiaElement {

    static properties = {
        heading: { type: String },
        text: { type: String },
        error: { type: Boolean, reflect: true },
        plain: { type: Boolean, reflect: true },
        waiting: { type: Boolean },
    };

    render() {
        if (this.waiting) {
            return html`<h2>${WAITING_HEADING}</h2><p>${UNREACHABLE} Trying again...</p>`;
        }
        return html`
            ${this.heading ? html`<h2>${this.heading}</h2>` : nothing}
            <p>${this.text}</p>
        `;
    }

}

customElements.define('arconia-message', ArconiaMessage);
