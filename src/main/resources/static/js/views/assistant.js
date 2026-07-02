// Assistant — one conversational surface. "General" = memory-first chat; "Knowledge" = grounded
// answers over a selected knowledge base, with citations. Sources (KB + docs) and Memory open as
// drawers. Operates on a single active conversation (no list endpoint), persisted client-side.

import { html, mount, on, el, announce } from "../core/dom.js";
import { getUserId, getActiveConversation, setActiveConversation, clearActiveConversation } from "../core/store.js";
import { chatBubble, citationChip, skeleton } from "../ui/components.js";
import { openDrawer } from "../ui/overlay.js";
import { toast } from "../ui/toast.js";
import { openSourcesDrawer } from "./sources.js";
import * as assistant from "../api/assistant.js";

const SUGGESTIONS = ["Give me 3 tips for the STAR method", "Explain dependency injection simply", "How do I prepare for a system design round?"];

export default function assistantView(outlet) {
    let mode = "general";          // "general" | "knowledge"
    let kbs = [], selectedKbId = null;
    let conversationId = getActiveConversation();
    let thread = [];               // { who, label, text, citations? }
    let sending = false, loadingThread = !!conversationId;

    /* ---- rendering ---- */
    function bar() {
        return html`
            <div class="assistant__bar">
                <div class="segmented" role="tablist">
                    <button class="seg ${mode === "general" ? "is-active" : ""}" data-mode="general" type="button">General</button>
                    <button class="seg ${mode === "knowledge" ? "is-active" : ""}" data-mode="knowledge" type="button">Knowledge</button>
                </div>
                ${mode === "knowledge" ? kbSelect() : ""}
                <div class="topbar__spacer"></div>
                <button class="btn btn--ghost btn--sm" data-action="sources" type="button">Sources</button>
                <button class="btn btn--ghost btn--sm" data-action="memory" type="button">Memory</button>
                <button class="btn btn--secondary btn--sm" data-action="new" type="button">New</button>
            </div>`;
    }

    function kbSelect() {
        if (!kbs.length) {
            return html`<button class="btn btn--ghost btn--sm" data-action="sources" type="button">+ Add a knowledge base</button>`;
        }
        return html`<select class="kb-select" id="kb-select">
            <option value="" ${!selectedKbId ? "selected" : ""} disabled>Choose a knowledge base…</option>
            ${kbs.map((k) => html`<option value="${k.id}" ${k.id === selectedKbId ? "selected" : ""}>${k.name}</option>`)}
        </select>`;
    }

    function threadPanel() {
        if (loadingThread) return html`<div class="assistant__thread">${skeleton(4)}</div>`;
        if (!thread.length) {
            return html`
                <div class="assistant__thread assistant__thread--empty">
                    <p class="muted">Ask anything. Switch to <strong>Knowledge</strong> to query your documents with citations.</p>
                    <div class="suggest">${SUGGESTIONS.map((s) => html`<button class="suggest__chip" data-suggest="${s}" type="button">${s}</button>`)}</div>
                </div>`;
        }
        return html`
            <div class="assistant__thread" id="athread">
                ${thread.map((m) => html`
                    ${chatBubble(m)}
                    ${m.citations && m.citations.length
                        ? html`<div class="cites">${m.citations.map((c) => citationChip(c))}</div>`
                        : ""}`)}
                ${sending ? html`<div class="bubble bubble--ai"><div class="bubble__who">Assistant</div><div class="typing"><span></span><span></span><span></span></div></div>` : ""}
            </div>`;
    }

    function render() {
        mount(outlet, html`
            <div class="assistant">
                ${bar()}
                ${threadPanel()}
                <form class="composer" id="achat">
                    <input class="composer__input" name="text" placeholder="${mode === "knowledge" ? "Ask the knowledge base…" : "Message the assistant…"}" autocomplete="off" ${sending ? "disabled" : ""}/>
                    <button class="btn btn--primary" type="submit" ${sending ? "disabled" : ""}>Send</button>
                </form>
            </div>`);
        scrollThread();
        const input = el(".composer__input", outlet);
        if (input && !sending) input.focus();
    }

    function scrollThread() {
        const t = el("#athread", outlet);
        if (t) t.scrollTop = t.scrollHeight;
    }

    /* ---- data ---- */
    async function init() {
        try { kbs = await assistant.knowledgeBases(); } catch { kbs = []; }
        if (conversationId) {
            try {
                const msgs = await assistant.messages(conversationId);
                thread = msgs.map(toThreadItem);
            } catch { thread = []; }
        }
        loadingThread = false;
        render();
    }

    function toThreadItem(m) {
        return m.role === "ASSISTANT"
            ? { who: "ai", label: "Assistant", text: m.content }
            : { who: "candidate", label: "You", text: m.content };
    }

    async function ensureConversation() {
        if (conversationId) return conversationId;
        const convo = await assistant.createConversation(getUserId(), "Assistant");
        conversationId = convo.conversationId;
        setActiveConversation(conversationId);
        return conversationId;
    }

    async function send(text) {
        const q = (text || "").trim();
        if (!q || sending) return;
        if (mode === "knowledge" && !selectedKbId) { toast("Choose a knowledge base first.", "error"); return; }

        thread.push({ who: "candidate", label: "You", text: q });
        sending = true; render();
        try {
            const cid = await ensureConversation();
            if (mode === "knowledge") {
                const res = await assistant.ask({ conversationId: cid, knowledgeBaseId: selectedKbId, question: q });
                thread.push({ who: "ai", label: "Assistant", text: res.answer, citations: res.citations || [] });
                announce(res.answer);
            } else {
                const res = await assistant.chat(cid, q);
                thread.push({ who: "ai", label: "Assistant", text: res.response });
                announce(res.response);
            }
        } catch (err) {
            toast(err?.message || "The assistant didn't respond. Please try again.", "error");
        } finally {
            sending = false; render();
        }
    }

    function openMemory() {
        const drawer = openDrawer({ title: "Memory", body: skeleton(4) });
        assistant.memoryStats(getUserId())
            .then((s) => drawer.setBody(html`
                <p class="muted">What the assistant remembers about you.</p>
                <div class="mem-grid">
                    ${memStat("Total", s.totalMemories)}
                    ${memStat("Personal", s.personal)}
                    ${memStat("Preference", s.preference)}
                    ${memStat("Fact", s.fact)}
                    ${memStat("Project", s.project)}
                    ${memStat("Goal", s.goal)}
                </div>`))
            .catch((e) => drawer.setBody(html`<p class="muted">Couldn't load memory: ${e?.message || "error"}</p>`));
    }

    function memStat(label, value) {
        return html`<div class="mem"><div class="mem__value">${value ?? 0}</div><div class="mem__label muted">${label}</div></div>`;
    }

    render();
    init();

    /* ---- wire (delegated) ---- */
    const offMode = on(outlet, "click", "[data-mode]", (_e, t) => { mode = t.dataset.mode; render(); });
    const offChange = on(outlet, "change", "#kb-select", (_e, t) => { selectedKbId = t.value || null; });
    const offSuggest = on(outlet, "click", "[data-suggest]", (_e, t) => send(t.dataset.suggest));
    const offSubmit = on(outlet, "submit", "#achat", (e) => {
        e.preventDefault();
        const input = el(".composer__input", outlet);
        send(input ? input.value : "");
    });
    const offActions = on(outlet, "click", "[data-action]", (_e, t) => {
        const action = t.dataset.action;
        if (action === "new") { clearActiveConversation(); conversationId = null; thread = []; render(); }
        else if (action === "memory") openMemory();
        else if (action === "sources") openSourcesDrawer({
            selectedKbId,
            onSelect: async (kbId) => {
                selectedKbId = kbId;
                mode = "knowledge";
                try { kbs = await assistant.knowledgeBases(); } catch { /* keep */ }
                render();
            },
        });
    });

    return () => { offMode(); offChange(); offSuggest(); offSubmit(); offActions(); };
}
