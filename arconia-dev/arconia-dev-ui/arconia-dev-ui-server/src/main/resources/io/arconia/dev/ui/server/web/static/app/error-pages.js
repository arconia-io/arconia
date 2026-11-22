/**
 * The only script of the error pages: the button that copies the stack trace of a failed
 * request. The pages are complete without it. The button is hidden until this script runs,
 * and stays hidden where the browser doesn't give access to the clipboard.
 */
const button = document.querySelector('.trace > .copy');

if (button && navigator.clipboard) {
    const label = button.getAttribute('aria-label');
    button.hidden = false;
    button.addEventListener('click', async () => {
        // When the stack trace can be read in two orders, the one on screen is copied.
        const trace = [...document.querySelectorAll('.trace > pre')].find((pre) => pre.offsetParent);
        try {
            await navigator.clipboard.writeText(trace.textContent);
        } catch {
            return;
        }
        button.classList.add('copied');
        button.setAttribute('aria-label', 'Copied');
        setTimeout(() => {
            button.classList.remove('copied');
            button.setAttribute('aria-label', label);
        }, 2000);
    });
}
