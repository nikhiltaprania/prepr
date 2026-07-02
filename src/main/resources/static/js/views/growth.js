// Growth — "am I improving, and what should I fix?" Skill matrix + score trend + on-demand AI
// coaching plan. Coaching is an LLM call, so it's generated on click (not on load).

import { html, mount, on } from "../core/dom.js";
import { getUserId } from "../core/store.js";
import { skillBar, miniTrendChart, card, skeleton, button, pill, humanize, emptyState } from "../ui/components.js";
import { openNewInterviewModal } from "./newInterview.js";
import { toast } from "../ui/toast.js";
import * as growth from "../api/growth.js";
import * as interviews from "../api/interviews.js";

// Map a skill category to the closest interview type (for the "Practice" prefill).
const SKILL_TO_TYPE = {
    JAVA: "JAVA_BACKEND", SPRING_BOOT: "SPRING_BOOT", MICROSERVICES: "MICROSERVICES",
    SYSTEM_DESIGN: "SYSTEM_DESIGN", BEHAVIORAL: "BEHAVIORAL",
};

export default function growthView(outlet) {
    let skills = [], trend = [], loading = true;
    let plan = null, planning = false;

    const weakest = () => (skills.length
        ? skills.reduce((acc, s) => ((s.averageScore ?? 0) < (acc.averageScore ?? 0) ? s : acc))
        : null);

    function practiceWeakest() {
        const w = weakest();
        openNewInterviewModal(w ? { type: SKILL_TO_TYPE[w.skillCategory] } : {});
    }

    function skillMatrix() {
        return card(
            skills.length
                ? html`<div class="skills">${skills.map((s) => skillBar({ label: humanize(s.skillCategory), score: s.averageScore, count: s.assessmentCount }))}</div>`
                : html`<p class="muted">No skill data yet — complete an interview to start tracking.</p>`,
            { title: "Skills" }
        );
    }

    function trendCard() {
        return card(
            trend.length >= 2
                ? miniTrendChart(trend)
                : html`<p class="muted">Complete a few interviews to see your score trend.</p>`,
            { title: "Score trend" }
        );
    }

    function coachingCard() {
        const body = html`
            <div class="between">
                <h3 class="card__title" style="margin:0">Coaching plan</h3>
                <button class="btn btn--secondary btn--sm" data-action="generate" type="button" ${planning ? "disabled" : ""}>
                    ${planning ? "Generating…" : plan ? "Refresh plan" : "Generate plan"}
                </button>
            </div>
            ${planning
                ? skeleton(4)
                : plan
                    ? html`
                        ${plan.estimatedDuration ? html`<div style="margin:var(--sp-3) 0">${pill("Estimated: " + plan.estimatedDuration, "info")}</div>` : ""}
                        <div class="subhead">Focus areas</div><div class="prose">${plan.focusAreas || "—"}</div>
                        <div class="subhead">Recommendations</div><div class="prose">${plan.recommendations || "—"}</div>
                        <div class="subhead">Practice plan</div><div class="prose">${plan.practicePlan || "—"}</div>
                        <div style="margin-top:var(--sp-4)">${button({ label: "Practice now", action: "practice", variant: "secondary" })}</div>`
                    : html`<p class="muted">Generate a personalized plan based on your skill scores.</p>`}`;
        return html`<section class="card">${body}</section>`;
    }

    function render() {
        if (loading) { mount(outlet, html`<div class="page"><h1 class="page__title">Growth</h1>${card(skeleton(6))}</div>`); return; }

        if (!skills.length && trend.length < 2) {
            mount(outlet, html`
                <div class="page">
                    <h1 class="page__title">Growth</h1>
                    ${emptyState({
                        title: "Your growth map is empty",
                        body: "Complete an interview to unlock skill tracking and a personalized coaching plan.",
                        actionLabel: "Start interview",
                        action: "practice",
                    })}
                </div>`);
            return;
        }

        mount(outlet, html`
            <div class="page">
                <h1 class="page__title">Growth</h1>
                <div class="growth__grid">${skillMatrix()}${trendCard()}</div>
                <div style="margin-top:var(--sp-4)">${coachingCard()}</div>
            </div>`);
    }

    async function load() {
        const uid = getUserId();
        // Also load the PERSISTED active plan so it survives refreshes/sessions (404 = none yet).
        const [s, l, p] = await Promise.allSettled([
            growth.skills(uid), interviews.listByUser(uid), growth.plan(uid),
        ]);
        skills = s.status === "fulfilled" ? s.value : [];
        const list = l.status === "fulfilled" ? l.value : [];
        plan = p.status === "fulfilled" ? p.value : null;   // rejected (404) → no plan yet
        // listByUser is newest-first; reverse to chronological for the trend.
        trend = list.filter((i) => i.status === "COMPLETED" && i.overallScore != null)
            .map((i) => i.overallScore).reverse();
        loading = false;
        render();
    }

    async function generate() {
        planning = true; render();
        try {
            plan = await growth.generatePlan(getUserId());   // POST → replaces the persisted plan
        } catch (err) {
            toast(err?.message || "Couldn't generate the coaching plan", "error");
        } finally {
            planning = false; render();
        }
    }

    render();
    load();

    const offActions = on(outlet, "click", "[data-action]", (_e, target) => {
        if (target.dataset.action === "generate") generate();
        else if (target.dataset.action === "practice") practiceWeakest();
    });

    return () => { offActions(); };
}
