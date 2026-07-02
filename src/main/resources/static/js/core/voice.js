// Browser Web Speech API wrapper.
//
// Mic (speech-to-text): a CONTINUOUS, managed session. Once enabled it stays enabled until explicitly
// disabled — like camera access. It auto-restarts if the browser ends the session (silence/timeout),
// and can be briefly suspended (e.g. while the interviewer is speaking) without losing the enabled
// state. Each finalized utterance is delivered via onResult.
//
// TTS (text-to-speech): speak() accepts onStart/onEnd so the UI can drive the lip-syncing video.

export function createSpeech() {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const sttSupported = !!Recognition;

    let recognition = null;
    let enabled = false;     // user intent (button state)
    let suspended = false;   // temporary pause (e.g. during interviewer speech)
    let handlers = {};

    function ensure() {
        if (!sttSupported || recognition || !enabled || suspended) return;
        const rec = new Recognition();
        rec.lang = "en-US";
        rec.continuous = true;
        rec.interimResults = false;
        rec.maxAlternatives = 1;
        rec.onresult = (e) => {
            const last = e.results[e.results.length - 1];
            if (last && last.isFinal && handlers.onResult) handlers.onResult(last[0].transcript);
        };
        rec.onerror = (e) => { if (e.error !== "no-speech" && handlers.onError) handlers.onError(e); };
        rec.onend = () => { recognition = null; if (enabled && !suspended) ensure(); };  // auto-restart
        try { rec.start(); recognition = rec; } catch { /* already starting */ }
    }

    function kill() {
        if (recognition) { try { recognition.onend = null; recognition.stop(); } catch { /* ignore */ } recognition = null; }
    }

    /** Enable continuous mic. Stays on until disable(). */
    function enable(h = {}) { handlers = h; enabled = true; suspended = false; ensure(); }
    /** Disable the mic entirely (explicit off). */
    function disable() { enabled = false; suspended = false; kill(); }
    /** Temporarily pause without changing enabled state (mic button stays "on"). */
    function suspend() { suspended = true; kill(); }
    /** Resume after a suspend, if still enabled. */
    function resume() { suspended = false; ensure(); }
    function isEnabled() { return enabled; }

    function speak(text, { onStart, onEnd } = {}) {
        try {
            if (!("speechSynthesis" in window) || !text) { onEnd && onEnd(); return; }
            window.speechSynthesis.cancel();
            const u = new SpeechSynthesisUtterance(text);
            u.onstart = () => onStart && onStart();
            u.onend = () => onEnd && onEnd();
            u.onerror = () => onEnd && onEnd();
            window.speechSynthesis.speak(u);
        } catch { onEnd && onEnd(); }
    }

    function cancelSpeak() {
        try { window.speechSynthesis && window.speechSynthesis.cancel(); } catch { /* ignore */ }
    }

    return { sttSupported, enable, disable, suspend, resume, isEnabled, speak, cancelSpeak };
}
