// Transient notifications. textContent only (no HTML injection). Auto-dismisses.

export function toast(message, type = "info", ms = 3200) {
    const root = document.getElementById("toast-root");
    if (!root) return;

    const node = document.createElement("div");
    node.className = "toast toast--" + type;
    node.setAttribute("role", "status");
    node.textContent = message;
    root.appendChild(node);

    requestAnimationFrame(() => node.classList.add("toast--in"));
    setTimeout(() => {
        node.classList.remove("toast--in");
        setTimeout(() => node.remove(), 200);
    }, ms);
}
