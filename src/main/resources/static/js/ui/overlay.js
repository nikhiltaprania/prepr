// Overlay primitive — a backdrop + panel. Variant "modal" (centered) now; "drawer" (side) reuses
// the same machinery in Milestone 6. One overlay at a time. Returns the root so callers can wire
// delegated events and re-render the body region.

import { html, mount, on } from "../core/dom.js";

let active = null;

export function closeOverlay() {
    if (!active) return;
    active.cleanup();
    active = null;
}

export function openOverlay({ title, body, variant = "modal" }) {
    closeOverlay();

    const previouslyFocused = document.activeElement;
    const root = document.createElement("div");
    root.className = "overlay";
    mount(root, html`
        <div class="overlay__backdrop" data-close></div>
        <div class="overlay__panel overlay__panel--${variant}" role="dialog" aria-modal="true" aria-label="${title}">
            <div class="overlay__header">
                <h3>${title}</h3>
                <button class="icon-btn" data-close aria-label="Close" type="button">✕</button>
            </div>
            <div class="overlay__body"></div>
        </div>`);
    document.body.appendChild(root);
    setBody(body);

    // Move focus into the overlay (first field, else the close button).
    const focusTarget = root.querySelector(".overlay__body input, .overlay__body select, .overlay__body textarea, .overlay__body button")
        || root.querySelector("[data-close]");
    focusTarget && focusTarget.focus();

    const offClose = on(root, "click", "[data-close]", () => closeOverlay());
    const onKey = (e) => { if (e.key === "Escape") closeOverlay(); };
    document.addEventListener("keydown", onKey);

    active = {
        root,
        cleanup: () => {
            offClose();
            document.removeEventListener("keydown", onKey);
            root.remove();
            if (previouslyFocused && previouslyFocused.focus) previouslyFocused.focus(); // restore focus
        },
    };
    return { root, setBody, close: closeOverlay };

    function setBody(content) { mount(root.querySelector(".overlay__body"), content); }
}

export const openModal = (opts) => openOverlay({ ...opts, variant: "modal" });
export const openDrawer = (opts) => openOverlay({ ...opts, variant: "drawer" });
