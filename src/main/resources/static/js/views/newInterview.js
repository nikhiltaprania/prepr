// New Interview modal: choose type/difficulty/adaptive/#questions, create the session, generate the
// question plan, then enter the Interview Room. Opened from the topbar "Start interview" button and
// from the Report "Practice again" CTA (with prefill).

import { html, on } from "../core/dom.js";
import { getUserId } from "../core/store.js";
import { openModal, closeOverlay } from "../ui/overlay.js";
import { humanize } from "../ui/components.js";
import { toast } from "../ui/toast.js";
import * as interviews from "../api/interviews.js";

const TYPES = ["JAVA_BACKEND", "SPRING_BOOT", "MICROSERVICES", "SYSTEM_DESIGN", "BEHAVIORAL", "HR", "CUSTOM"];
const DIFFICULTIES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

export function openNewInterviewModal(prefill = {}) {
    let submitting = false;
    const handle = openModal({ title: "Start a new interview", body: form() });

    function option(value, selected) {
        return html`<option value="${value}" ${value === selected ? "selected" : ""}>${humanize(value)}</option>`;
    }

    function form() {
        return html`
            <form class="form" id="new-interview-form">
                <div class="grid-2">
                    <label class="field"><span>Type</span>
                        <select name="type">${TYPES.map((t) => option(t, prefill.type || "JAVA_BACKEND"))}</select>
                    </label>
                    <label class="field"><span>Difficulty</span>
                        <select name="difficulty">${DIFFICULTIES.map((d) => option(d, prefill.difficulty || "INTERMEDIATE"))}</select>
                    </label>
                </div>
                <label class="field"><span>Number of questions</span>
                    <input name="count" type="number" min="1" max="15" value="5"/>
                </label>
                <label class="check">
                    <input name="adaptive" type="checkbox"/>
                    <span>Adaptive follow-ups — the interviewer probes deeper based on your answers.</span>
                </label>
                ${submitting ? html`<p class="muted">Preparing your interview… generating questions can take a moment.</p>` : ""}
                <div class="save-bar">
                    <button class="btn btn--primary" type="submit" ${submitting ? "disabled" : ""}>
                        ${submitting ? "Preparing…" : "Begin interview"}
                    </button>
                </div>
            </form>`;
    }

    on(handle.root, "submit", "#new-interview-form", async (e) => {
        e.preventDefault();
        if (submitting) return;
        const fd = new FormData(e.target);
        submitting = true;
        handle.setBody(form());
        try {
            const created = await interviews.create({
                userId: getUserId(),
                type: fd.get("type"),
                difficulty: fd.get("difficulty"),
                adaptive: fd.get("adaptive") === "on",
            });
            const count = Number(fd.get("count")) || 5;
            await interviews.generatePlan(created.id, count);
            closeOverlay();
            location.hash = `#/interviews/${created.id}/room`;
        } catch (err) {
            submitting = false;
            handle.setBody(form());
            toast(err?.message || "Couldn't start the interview", "error");
        }
    });
}
