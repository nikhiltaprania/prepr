// Entry point: render the persistent shell, register routes, start the router.

import { renderShell } from "./ui/shell.js";
import { register, start } from "./core/router.js";
import authView from "./views/auth.js";
import homeView from "./views/home.js";
import growthView from "./views/growth.js";
import assistantView from "./views/assistant.js";
import settingsView from "./views/settings.js";
import adminView from "./views/admin.js";
import adminQuestionsView from "./views/adminQuestions.js";
import interviewRoom from "./views/interviewRoom.js";
import reportView from "./views/report.js";
import { setSessionFromToken } from "./core/store.js";

// Apply the saved theme before first paint.
const savedTheme = localStorage.getItem("prepr.theme");
if (savedTheme) document.documentElement.dataset.theme = savedTheme;

// OAuth2 redirect lands on /?token=<jwt> — adopt the session, then clean the URL.
const oauthToken = new URLSearchParams(location.search).get("token");
if (oauthToken) {
    setSessionFromToken(oauthToken);
    history.replaceState(null, "", location.pathname + (location.hash || "#/"));
}

renderShell(document.getElementById("app"));

register([
    { path: "/login", view: authView, public: true },

    { path: "/", nav: "home", view: homeView },
    { path: "/growth", nav: "growth", view: growthView },
    { path: "/assistant", nav: "assistant", view: assistantView },
    { path: "/settings", nav: "settings", view: settingsView },
    { path: "/admin", nav: "admin-users", view: adminView },
    { path: "/admin/questions", nav: "admin-questions", view: adminQuestionsView },

    { path: "/interviews/:id/room", focus: true, view: interviewRoom },
    { path: "/interviews/:id", view: reportView },
]);

start();
