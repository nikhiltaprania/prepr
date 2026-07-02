// Interview Report — overall score, narrative, per-question breakdown (with async-score polling),
// and the transcript. Also the detail view for any past interview. Report generation is automatic
// on completion; if it isn't ready, the user can trigger it.

import { html, mount, on } from "../core/dom.js";
import { poll } from "../core/api.js";
import { fmtDate } from "../core/format.js";
import { scoreRing, pill, humanize, card, skeleton, button, emptyState, chatBubble } from "../ui/components.js";
import { toast } from "../ui/toast.js";
import { openNewInterviewModal } from "./newInterview.js";
import * as interviews from "../api/interviews.js";
import * as reports from "../api/reports.js";

export default function reportView(outlet, params) {
    const id = params.id;
    let interview = null, rep = null, questions = [], answers = [], transcript = [];
    let loading = true, missing = false, generating = false, tab = "breakdown";

    const scoreVariant = (s) => (s == null ? "neutral" : s >= 8 ? "good" : s >= 5 ? "warn" : "bad");
    const scoreTag = (s) => pill(s == null ? "…" : s.toFixed(1), scoreVariant(s));
    const answerFor = (q) => answers.find((a) => a.questionId === q.id);

    function head() {
        return html`
            <div class="report__head">
                <div>
                    <div class="row" style="gap:var(--sp-2)">
                        ${interview ? pill(humanize(interview.type)) : ""}
                        ${interview ? pill(humanize(interview.difficulty)) : ""}
                        ${interview?.completedAt ? html`<span class="muted">${fmtDate(interview.completedAt)}</span>` : ""}
                    </div>
                    <h1 class="page__title" style="margin-top:var(--sp-3)">Interview report</h1>
                </div>
                ${rep ? scoreRing(rep.overallScore) : ""}
            </div>`;
    }

    function breakdown() {
        return html`
            ${rep?.summary ? card(html`<div class="prose">${rep.summary}</div>`, { title: "Summary" }) : ""}
            <div class="grid-3">
                ${card(html`<div class="prose">${rep?.strengths || "—"}</div>`, { title: "Strengths" })}
                ${card(html`<div class="prose">${rep?.weaknesses || "—"}</div>`, { title: "Weaknesses" })}
                ${card(html`<div class="prose">${rep?.recommendations || "—"}</div>`, { title: "Recommendations" })}
            </div>
            <h3 style="margin:var(--sp-6) 0 var(--sp-3)">Question breakdown</h3>
            ${questions.length ? questions.map(qaItem) : html`<p class="muted">No questions recorded.</p>`}`;
    }

    function qaItem(q, i) {
        const a = answerFor(q);
        return html`
            <details class="qa">
                <summary>
                    <span class="qa__q">Q${q.sequenceNumber || i + 1}. ${q.question}</span>
                    ${a ? scoreTag(a.score) : pill("No answer", "neutral")}
                </summary>
                <div class="qa__body">
                    ${a
                        ? html`
                            <p class="qa__answer">${a.answer}</p>
                            ${a.score == null
                                ? html`<p class="muted">Scoring…</p>`
                                : html`
                                    <div class="qa__detail"><strong>Strengths.</strong> <span class="prose">${a.strengths || "—"}</span></div>
                                    <div class="qa__detail"><strong>Weaknesses.</strong> <span class="prose">${a.weaknesses || "—"}</span></div>
                                    <div class="qa__detail"><strong>Improve.</strong> <span class="prose">${a.improvementSuggestions || "—"}</span></div>`}`
                        : html`<p class="muted">Not answered.</p>`}
                </div>
            </details>`;
    }

    function transcriptPanel() {
        if (!transcript.length) return html`<p class="muted">No transcript available.</p>`;
        return html`<div class="room__stream room__stream--static">
            ${transcript.map((t) => chatBubble({ who: t.speaker === "AI" ? "ai" : "candidate", text: t.content }))}
        </div>`;
    }

    function render() {
        if (loading) { mount(outlet, html`<div class="page page--read"><h1 class="page__title">Interview report</h1>${card(skeleton(6))}</div>`); return; }

        if (missing) {
            mount(outlet, html`
                <div class="page page--read">
                    ${head()}
                    ${card(emptyState({
                        title: "Report not ready yet",
                        body: "The report is generated when the interview finishes. You can generate it now.",
                        actionLabel: generating ? "Generating…" : "Generate report",
                        action: "generate",
                    }))}
                </div>`);
            return;
        }

        mount(outlet, html`
            <div class="page page--read">
                ${head()}
                <div class="tabs" role="tablist">
                    <button class="tab ${tab === "breakdown" ? "is-active" : ""}" data-tab="breakdown" type="button">Breakdown</button>
                    <button class="tab ${tab === "transcript" ? "is-active" : ""}" data-tab="transcript" type="button">Transcript</button>
                </div>
                ${tab === "breakdown" ? breakdown() : transcriptPanel()}
                <div class="report__cta">
                    ${button({ label: "Update skills & coaching", href: "#/growth", variant: "secondary" })}
                    ${button({ label: "Practice this topic again", action: "practice" })}
                </div>
            </div>`);
    }

    async function load() {
        try { interview = await interviews.get(id); } catch { /* shown via report state */ }
        try { rep = await reports.get(id); missing = false; }
        catch (err) { if (err?.status === 404) missing = true; else toast(err?.message || "Couldn't load the report", "error"); }
        try {
            const [q, a, t] = await Promise.all([interviews.questions(id), interviews.answers(id), interviews.transcript(id)]);
            questions = q; answers = a; transcript = t;
        } catch { /* non-fatal */ }
        loading = false;
        render();
        pollScores();
    }

    async function pollScores() {
        if (!answers.length || !answers.some((a) => a.score == null)) return;
        await poll(async () => {
            try { answers = await interviews.answers(id); render(); } catch { /* ignore */ }
            return answers;
        }, { intervalMs: 2500, timeoutMs: 30000, until: (a) => !a.some((x) => x.score == null) });
    }

    async function doGenerate() {
        generating = true; render();
        try {
            rep = await reports.generate(id);
            missing = false;
            answers = await interviews.answers(id);
        } catch (err) {
            toast(err?.message || "Couldn't generate the report", "error");
        } finally {
            generating = false; render(); pollScores();
        }
    }

    render();
    load();

    const offTab = on(outlet, "click", "[data-tab]", (_e, t) => { tab = t.dataset.tab; render(); });
    const offActions = on(outlet, "click", "[data-action]", (_e, target) => {
        const action = target.dataset.action;
        if (action === "generate") doGenerate();
        else if (action === "practice") openNewInterviewModal({ type: interview?.type, difficulty: interview?.difficulty });
    });

    return () => { offTab(); offActions(); };
}
