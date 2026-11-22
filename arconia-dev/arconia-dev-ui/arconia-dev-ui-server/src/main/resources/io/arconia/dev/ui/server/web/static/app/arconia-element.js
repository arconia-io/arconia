import { LitElement } from 'lit';

/**
 * The base of the elements of the Dev UI.
 * <p>
 * An element renders into the page itself, without a shadow root, so that the theme of the
 * Dev UI applies to the Vaadin components it contains: the theme is a stylesheet of the page,
 * which doesn't reach inside a shadow root. The styles of the elements are in app.css, where
 * each rule starts with the name of the element it belongs to.
 */
export class ArconiaElement extends LitElement {

    createRenderRoot() {
        return this;
    }

}
