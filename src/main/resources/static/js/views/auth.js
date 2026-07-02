// Auth screen — login / sign up against the real backend. Sets the session from the AuthResponse
// and routes to Home. Errors (bad credentials, duplicate email, validation) surface inline.

import { html, mount, on } from "../core/dom.js";
import { setSession } from "../core/store.js";
import { field, alert } from "../ui/components.js";
import * as authApi from "../api/auth.js";

export default function authView(outlet) {
    let mode = "login";          // "login" | "signup"
    let submitting = false;
    let error = "";
    let prefill = { username: "", email: "" }; // preserved across error re-renders (never the password)

    function template() {
        return html`
            <div class="auth">
                <div class="auth__card">
                    <div class="auth__brand">
                        <span class="auth__logo" aria-hidden="true">P</span>
                        <div class="auth__brand-text">
                            <div class="auth__name">Prepr</div>
                            <div class="auth__tagline muted">AI Interview Coaching</div>
                        </div>
                    </div>
                    <div class="segmented" role="tablist">
                        <button class="seg ${mode === "login" ? "is-active" : ""}" data-mode="login" type="button">Log in</button>
                        <button class="seg ${mode === "signup" ? "is-active" : ""}" data-mode="signup" type="button">Sign up</button>
                    </div>
                    ${alert(error)}
                    <form class="form" id="auth-form">
                        ${mode === "signup" ? field({ name: "username", label: "Username", value: prefill.username, required: true }) : ""}
                        ${field({ name: "email", label: "Email", type: "email", value: prefill.email, required: true })}
                        ${field({ name: "password", label: "Password", type: "password", required: true, autocomplete: mode === "signup" ? "new-password" : "current-password" })}
                        <button class="btn btn--primary btn--block" type="submit" ${submitting ? "disabled" : ""}>
                            ${submitting ? "Please wait…" : "Continue"}
                        </button>
                    </form>
                    <div class="auth__divider"><span>or</span></div>
                    <div class="oauth">
                        <a class="btn btn--secondary btn--block" href="/oauth2/authorization/google">Continue with Google</a>
                        <a class="btn btn--secondary btn--block" href="/oauth2/authorization/github">Continue with GitHub</a>
                    </div>
                    <p class="auth__note muted">
                        ${mode === "login" ? "New here? Choose Sign up to create an account." : "Your data stays private to your account."}
                    </p>
                </div>
            </div>`;
    }

    const paint = () => mount(outlet, template());
    paint();

    const offMode = on(outlet, "click", "[data-mode]", (_e, target) => {
        if (submitting) return;
        mode = target.dataset.mode;
        error = "";
        paint();
    });

    const offSubmit = on(outlet, "submit", "#auth-form", async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        prefill = { username: fd.get("username") || "", email: fd.get("email") || "" };
        submitting = true;
        error = "";
        paint();
        try {
            const payload = mode === "signup"
                ? { username: fd.get("username"), email: fd.get("email"), password: fd.get("password") }
                : { email: fd.get("email"), password: fd.get("password") };
            const res = mode === "signup" ? await authApi.register(payload) : await authApi.login(payload);
            setSession({ token: res.accessToken, userId: res.userId, role: res.role, name: res.username });
            location.hash = "#/";
        } catch (err) {
            error = err?.message || "Something went wrong. Please try again.";
            submitting = false;
            paint();
        }
    });

    return () => { offMode(); offSubmit(); };
}
