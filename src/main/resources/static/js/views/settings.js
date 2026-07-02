// Settings — Profile (edit), Resume (upload + AI analysis), Account (identity + sign out).
// (User management lives in its own admin screen, #/admin.) Tab data is loaded lazily on first visit.

import { html, mount, on } from "../core/dom.js";
import { getSession, getUserId, clearSession, updateSession } from "../core/store.js";
import { field, textArea, button, skeleton, card } from "../ui/components.js";
import { toast } from "../ui/toast.js";
import * as usersApi from "../api/users.js";

export default function settingsView(outlet) {
    let tab = "profile";
    let user = null, saving = false;
    let resume, resumeUploading = false;     // resume: undefined=not loaded, null=none, object=analysis

    function tabs() {
        const list = [
            { id: "profile", label: "Profile" },
            { id: "resume", label: "Resume" },
            { id: "account", label: "Account" },
        ];
        return html`<div class="tabs" role="tablist">
            ${list.map((t) => html`<button class="tab ${tab === t.id ? "is-active" : ""}" data-tab="${t.id}" type="button">${t.label}</button>`)}
        </div>`;
    }

    /* ---- Profile ---- */
    function profilePanel() {
        if (!user) return skeleton(6);
        return html`
            <form class="form" id="profile-form">
                <div class="grid-2">
                    ${field({ name: "username", label: "Username", value: user.username, required: true })}
                    ${field({ name: "email", label: "Email", type: "email", value: user.email, required: true })}
                    ${field({ name: "targetRole", label: "Target role", value: user.targetRole })}
                    ${field({ name: "experienceYears", label: "Years of experience", type: "number", value: user.experienceYears ?? "" })}
                    ${field({ name: "currentCompany", label: "Current company", value: user.currentCompany })}
                    ${field({ name: "targetCompany", label: "Target company", value: user.targetCompany })}
                </div>
                ${textArea({ name: "bio", label: "Bio", value: user.bio, rows: 4 })}
                <div class="save-bar"><button class="btn btn--primary" type="submit" ${saving ? "disabled" : ""}>${saving ? "Saving…" : "Save changes"}</button></div>
            </form>`;
    }

    /* ---- Resume ---- */
    function resumePanel() {
        const section = (title, body) => html`<div class="subhead">${title}</div><div class="prose">${body || "—"}</div>`;
        return html`
            <form class="doc-form" id="resume-form">
                <input type="file" name="file" required/>
                <button class="btn btn--secondary btn--sm" type="submit" ${resumeUploading ? "disabled" : ""}>${resumeUploading ? "Analyzing…" : "Upload & analyze"}</button>
            </form>
            ${resume === undefined
                ? skeleton(5)
                : resume === null
                    ? html`<p class="muted">No resume yet. Upload one to get an AI analysis that tailors your interviews.</p>`
                    : html`
                        ${section("Summary", resume.candidateSummary)}
                        ${section("Skills", resume.skills)}
                        ${section("Experience", resume.experience)}
                        ${section("Projects", resume.projects)}
                        ${section("Education", resume.education)}
                        ${section("Strengths", resume.strengths)}`}`;
    }

    /* ---- Account ---- */
    function accountPanel() {
        const session = getSession();
        return html`
            <div class="stack">
                <div class="between"><div><div class="muted">Email</div><div>${user?.email ?? "—"}</div></div></div>
                <div class="between">
                    <div>
                        <div class="muted">Account role</div>
                        <div>${(session?.role || "ROLE_USER").replace("ROLE_", "")}</div>
                    </div>
                </div>
                <p class="muted" style="font-size:var(--fs-xs)">Account role is managed by administrators and can't be changed from your profile. (This is separate from your "Target role" career goal under Profile.)</p>
                <div>${button({ label: "Sign out", variant: "destructive", action: "logout" })}</div>
            </div>`;
    }

    function panel() {
        if (tab === "profile") return profilePanel();
        if (tab === "resume") return resumePanel();
        return accountPanel();
    }

    function render() {
        mount(outlet, html`
            <div class="page page--read">
                <h1 class="page__title">Settings</h1>
                ${tabs()}
                ${card(panel())}
            </div>`);
    }

    /* ---- data ---- */
    async function loadUser() {
        try { user = await usersApi.get(getUserId()); render(); }
        catch (err) { toast(err?.message || "Couldn't load your profile", "error"); }
    }
    async function loadResume() {
        try { resume = await usersApi.resume(getUserId()); }
        catch { resume = null; }
        render();
    }

    render();
    loadUser();

    /* ---- wire (delegated) ---- */
    const offTab = on(outlet, "click", "[data-tab]", (_e, t) => {
        tab = t.dataset.tab;
        render();
        if (tab === "resume" && resume === undefined) loadResume();
    });

    const offSave = on(outlet, "submit", "#profile-form", async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const years = fd.get("experienceYears");
        const body = {
            username: fd.get("username"), email: fd.get("email"),
            targetRole: fd.get("targetRole") || null,
            experienceYears: years ? Number(years) : null,
            currentCompany: fd.get("currentCompany") || null,
            targetCompany: fd.get("targetCompany") || null,
            bio: fd.get("bio") || null,
        };
        saving = true; render();
        try {
            user = await usersApi.update(getUserId(), body);
            updateSession({ name: user.username });   // refresh the header display name
            toast("Profile saved", "success");
        }
        catch (err) { toast(err?.message || "Save failed", "error"); }
        finally { saving = false; render(); }
    });

    const offResume = on(outlet, "submit", "#resume-form", async (e) => {
        e.preventDefault();
        const file = new FormData(e.target).get("file");
        if (!file || !file.name || resumeUploading) return;
        resumeUploading = true; render();
        try { resume = await usersApi.uploadResume(getUserId(), file); toast("Resume analyzed", "success"); }
        catch (err) { toast(err?.message || "Resume analysis failed", "error"); }
        finally { resumeUploading = false; render(); }
    });

    const offLogout = on(outlet, "click", '[data-action="logout"]', () => { clearSession(); location.hash = "#/login"; });

    return () => { offTab(); offSave(); offResume(); offLogout(); };
}
