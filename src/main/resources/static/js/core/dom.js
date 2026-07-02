// Tiny DOM + templating helpers. The whole rendering model lives here:
// components/views return Html (safe strings); the router/mount writes them into the DOM;
// interactivity is attached via delegated listeners (`on`).

/** Wrapper marking an already-safe HTML string so nested templates aren't re-escaped. */
class Html {
    constructor(s) { this.value = s; }
    toString() { return this.value; }
}

export function escapeHtml(v) {
    return String(v).replace(/[&<>"']/g, (c) => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}

/** Mark a trusted string (e.g. an inline SVG) as raw HTML. Use sparingly — never on API text. */
export function raw(s) { return new Html(String(s)); }

function part(v) {
    if (v == null || v === false) return '';
    if (v instanceof Html) return v.value;
    if (Array.isArray(v)) return v.map(part).join('');
    return escapeHtml(v);
}

/** Tagged template that auto-escapes interpolations. Returns Html. Nest with other html`` safely. */
export function html(strings, ...values) {
    let out = strings[0];
    for (let i = 0; i < values.length; i++) out += part(values[i]) + strings[i + 1];
    return new Html(out);
}

/** Write Html|string into a container. */
export function mount(root, content) { root.innerHTML = String(content); return root; }

export const el = (sel, root = document) => root.querySelector(sel);
export const els = (sel, root = document) => Array.from(root.querySelectorAll(sel));

/** Announce a message to assistive tech via the shared polite live region. */
export function announce(message) {
    const region = document.getElementById("sr-live");
    if (region) region.textContent = message;
}

/**
 * Delegated event listener: handles events from descendants matching `selector`.
 * Returns an unsubscribe function (call it in a view's teardown).
 */
export function on(root, event, selector, handler) {
    const fn = (e) => {
        const target = e.target.closest(selector);
        if (target && root.contains(target)) handler(e, target);
    };
    root.addEventListener(event, fn);
    return () => root.removeEventListener(event, fn);
}
