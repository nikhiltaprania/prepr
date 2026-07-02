// Sources drawer — manage knowledge bases and their documents (the content that powers RAG).
// Opened from the Assistant. Selecting a KB here sets the Assistant's active knowledge base.

import { html, on } from "../core/dom.js";
import { openDrawer } from "../ui/overlay.js";
import { statusPill, skeleton } from "../ui/components.js";
import { toast } from "../ui/toast.js";
import * as assistant from "../api/assistant.js";

export function openSourcesDrawer({ selectedKbId = null, onSelect } = {}) {
    let kbs = [], docs = [], activeKbId = selectedKbId;
    let loadingKbs = true, loadingDocs = false, busy = false;

    const handle = openDrawer({ title: "Sources", body: skeleton(5) });

    function body() {
        return html`
            <div class="stack">
                <form class="form" id="kb-form">
                    <label class="field"><span>New knowledge base</span>
                        <input name="name" placeholder="e.g. Java Interview Notes" required/></label>
                    <button class="btn btn--secondary btn--sm" type="submit" ${busy ? "disabled" : ""}>Create</button>
                </form>

                <div>
                    <div class="subhead">Knowledge bases</div>
                    ${loadingKbs ? skeleton(3) : kbs.length
                        ? html`<div class="kb-list">${kbs.map(kbRow)}</div>`
                        : html`<p class="muted">No knowledge bases yet. Create one above.</p>`}
                </div>

                ${activeKbId ? docsSection() : ""}
            </div>`;
    }

    function kbRow(kb) {
        return html`<button class="kb-row ${kb.id === activeKbId ? "is-active" : ""}" data-kb="${kb.id}" type="button">${kb.name}</button>`;
    }

    function docsSection() {
        const kb = kbs.find((k) => k.id === activeKbId);
        return html`
            <div>
                <div class="subhead">Documents${kb ? ` · ${kb.name}` : ""}</div>
                <form class="doc-form" id="doc-form">
                    <input type="file" name="file" required/>
                    <button class="btn btn--secondary btn--sm" type="submit" ${busy ? "disabled" : ""}>${busy ? "Uploading…" : "Upload"}</button>
                </form>
                ${loadingDocs ? skeleton(2) : docs.length
                    ? html`<div class="doc-list">${docs.map(docRow)}</div>`
                    : html`<p class="muted">No documents yet.</p>`}
            </div>`;
    }

    function docRow(d) {
        return html`
            <div class="doc-row">
                <span class="doc-row__name">${d.fileName}</span>
                ${statusPill(d.status)}
                <span class="muted">${d.chunkCount ?? 0} chunks</span>
                <button class="icon-btn" data-del-doc="${d.id}" title="Delete" type="button">✕</button>
            </div>`;
    }

    const paint = () => handle.setBody(body());

    async function loadKbs() {
        loadingKbs = true; paint();
        try { kbs = await assistant.knowledgeBases(); }
        catch (e) { toast(e?.message || "Couldn't load knowledge bases", "error"); kbs = []; }
        loadingKbs = false; paint();
        if (activeKbId) loadDocs();
    }

    async function loadDocs() {
        loadingDocs = true; paint();
        try { docs = await assistant.documents(activeKbId); }
        catch { docs = []; }
        loadingDocs = false; paint();
    }

    function selectKb(id) {
        activeKbId = id;
        docs = [];
        onSelect && onSelect(id);
        loadDocs();
    }

    loadKbs();

    on(handle.root, "submit", "#kb-form", async (e) => {
        e.preventDefault();
        if (busy) return;
        const name = new FormData(e.target).get("name");
        busy = true; paint();
        try {
            const kb = await assistant.createKnowledgeBase({ name, description: "" });
            await loadKbs();
            selectKb(kb.id);          // auto-select the new KB (also updates the Assistant)
            toast("Knowledge base created", "success");
        } catch (err) {
            toast(err?.message || "Couldn't create knowledge base", "error");
        } finally {
            busy = false; paint();
        }
    });

    on(handle.root, "click", "[data-kb]", (_e, t) => selectKb(t.dataset.kb));

    on(handle.root, "submit", "#doc-form", async (e) => {
        e.preventDefault();
        if (busy || !activeKbId) return;
        const file = new FormData(e.target).get("file");
        if (!file || !file.name) return;
        busy = true; paint();
        try {
            await assistant.uploadDocument(activeKbId, file);
            await loadDocs();
            toast("Document ingested", "success");
        } catch (err) {
            toast(err?.message || "Upload failed", "error");
        } finally {
            busy = false; paint();
        }
    });

    on(handle.root, "click", "[data-del-doc]", async (_e, t) => {
        try { await assistant.deleteDocument(t.dataset.delDoc); await loadDocs(); }
        catch (err) { toast(err?.message || "Couldn't delete document", "error"); }
    });
}
