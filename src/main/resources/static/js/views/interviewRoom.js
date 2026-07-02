// Interview Room — a balanced two-participant interview. The AI interviewer is a lip-syncing video
// that plays while the browser speaks the reply (TTS) and pauses when it finishes. The candidate has
// an identical webcam tile. Controls: Mic (continuous — stays on until turned off) and Camera; End.

import { html, mount, on, el, announce } from "../core/dom.js";
import { createSpeech } from "../core/voice.js";
import { pill, humanize, chatBubble, typingBubble } from "../ui/components.js";
import { toast } from "../ui/toast.js";
import * as interviews from "../api/interviews.js";

export default function interviewRoom(outlet, params) {
    const id = params.id;
    const speech = createSpeech();

    let interview = null;
    let phase = "loading";      // loading | live | error
    let sending = false;
    let completed = false;
    let micEnabled = false;
    let cameraOn = false;
    let mediaStream = null;
    const messages = [];        // { who: "ai" | "candidate", text }

    /* ---------- rendering ---------- */
    function render() {
        if (phase === "loading") { mount(outlet, html`<div class="room"><div class="room__stream"><p class="muted">Starting your interview…</p></div></div>`); return; }
        if (phase === "error") { mount(outlet, html`<div class="room"><div class="room__stream"><p class="muted">Couldn't load this interview. <a href="#/">Go home</a></p></div></div>`); return; }

        mount(outlet, html`
            <div class="room room--call">
                <header class="room__header">
                    <button class="btn btn--ghost btn--sm" data-action="back" type="button" title="Leave interview">← Back</button>
                    <div class="row" style="gap:var(--sp-2)">${pill(humanize(interview.type))}${pill(humanize(interview.difficulty))}</div>
                    <div class="topbar__spacer"></div>
                    <span class="muted">${completed ? "Completed" : "Interview in progress"}</span>
                </header>

                <div class="stage">
                    <div class="vtile vtile--ai">
                        <video id="ai-video" class="vtile__video" src="/assets/interviewer.mp4" loop muted playsinline preload="auto"></video>
                        <span class="vtile__label">AI Interviewer</span>
                    </div>
                    <div class="vtile vtile--me" data-camera="${cameraOn ? "on" : "off"}">
                        <video id="me-video" class="vtile__video" autoplay muted playsinline></video>
                        <div class="vtile__placeholder">Camera off</div>
                        <span class="vtile__label">You</span>
                    </div>
                </div>

                <div class="room__stream" id="stream">
                    ${messages.map((m) => chatBubble(m))}
                    ${sending ? typingBubble() : ""}
                </div>

                ${completed
                    ? html`<div class="room__done">
                            <div class="room__done-text">Interview complete. Your report is being prepared.</div>
                            <button class="btn btn--primary" data-action="view-report" type="button">View report</button>
                        </div>`
                    : html`
                        <div class="controls">
                            ${speech.sttSupported ? html`<button class="ctrl ${micEnabled ? "is-on" : ""}" data-action="mic" type="button" title="Answer by voice">🎤<span>Mic</span></button>` : ""}
                            <button class="ctrl ${cameraOn ? "is-on" : ""}" data-action="camera" type="button" title="Toggle camera">📷<span>Camera</span></button>
                            <button class="ctrl ctrl--end" data-action="end" type="button" title="End interview">⏹<span>End</span></button>
                        </div>
                        <form class="composer" id="composer">
                            <input class="composer__input" name="text" placeholder="Type your answer…" autocomplete="off" ${sending ? "disabled" : ""}/>
                            <button class="btn btn--primary" type="submit" ${sending ? "disabled" : ""}>Send</button>
                        </form>`}
            </div>`);

        if (cameraOn && mediaStream) attachCamera();
        scrollStream();
        const input = el(".composer__input", outlet);
        if (input && !sending && !completed) input.focus();
    }

    function scrollStream() { const s = el("#stream", outlet); if (s) s.scrollTop = s.scrollHeight; }
    function attachCamera() { const v = el("#me-video", outlet); if (v && mediaStream) v.srcObject = mediaStream; }

    function playAiVideo() { const v = el("#ai-video", outlet); if (v) v.play().catch(() => { /* no asset / autoplay */ }); }
    // On speech end: pause AND rewind to 0:00 so the next interviewer reply starts from the beginning.
    function resetAiVideo() { const v = el("#ai-video", outlet); if (v) { v.pause(); try { v.currentTime = 0; } catch { /* ignore */ } } }

    /* ---------- camera ---------- */
    async function startCamera() {
        try {
            mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
            cameraOn = true; render();
        } catch {
            cameraOn = false; render();
        }
    }
    function stopCamera() {
        if (mediaStream) { mediaStream.getTracks().forEach((t) => t.stop()); mediaStream = null; }
        cameraOn = false;
    }

    /* ---------- mic (continuous) ---------- */
    function toggleMic() {
        if (micEnabled) { micEnabled = false; speech.disable(); }
        else {
            micEnabled = true;
            speech.enable({
                onResult: (t) => { if (t && t.trim()) sendTurn(t); },
                onError: () => toast("Microphone unavailable or permission denied.", "error"),
            });
        }
        render();
    }

    /* ---------- interview turns ---------- */
    async function init() {
        try {
            interview = await interviews.get(id);
            if (interview.status === "COMPLETED") { location.hash = `#/interviews/${id}`; return; }
            if (interview.status === "CREATED") await interviews.start(id);

            let prior = [];
            try { prior = await interviews.transcript(id); } catch { prior = []; }
            phase = "live";
            if (prior && prior.length) {
                prior.forEach((e) => messages.push({ who: e.speaker === "AI" ? "ai" : "candidate", text: e.content }));
                render();
            } else {
                render();
                await sendTurn("");
            }
            // Mic and camera stay OFF by default — the candidate enables them explicitly.
        } catch (err) {
            phase = "error"; render();
            toast(err?.message || "Couldn't start the interview", "error");
        }
    }

    async function sendTurn(text) {
        if (sending || completed) return;
        const trimmed = (text || "").trim();
        if (trimmed) messages.push({ who: "candidate", text: trimmed });
        sending = true;
        speech.suspend();                 // don't capture the interviewer's voice while it responds
        render();
        try {
            const res = await interviews.respond(id, trimmed);
            completed = !!res.completed;
            if (res.reply) {
                messages.push({ who: "ai", text: res.reply });
                announce(res.reply);
                sending = false; render();
                // Speak the reply; the video plays during speech and pauses when done, then mic resumes.
                speech.speak(res.reply, {
                    onStart: playAiVideo,
                    onEnd: () => { resetAiVideo(); if (micEnabled && !completed) speech.resume(); },
                });
                return;
            }
        } catch (err) {
            toast(err?.message || "The interviewer didn't respond. Please try again.", "error");
        }
        sending = false; render();
        if (micEnabled && !completed) speech.resume();   // no reply spoken — resume mic now
    }

    /* ---------- wire (delegated) ---------- */
    const offSubmit = on(outlet, "submit", "#composer", (e) => {
        e.preventDefault();
        const i = el(".composer__input", outlet);
        if (i && i.value.trim()) sendTurn(i.value);
    });
    const offActions = on(outlet, "click", "[data-action]", (_e, target) => {
        const a = target.dataset.action;
        if (a === "mic") toggleMic();
        else if (a === "camera") (cameraOn ? (stopCamera(), render()) : startCamera());
        else if (a === "back") location.hash = "#/";   // leave safely — the interview persists and can be resumed
        else if (a === "end") { if (confirm("End the interview now? You can still view the report.")) location.hash = `#/interviews/${id}`; }
        else if (a === "view-report") location.hash = `#/interviews/${id}`;
    });

    render();
    init();

    return () => {
        offSubmit(); offActions();
        speech.disable(); speech.cancelSpeak();
        stopCamera();
    };
}
