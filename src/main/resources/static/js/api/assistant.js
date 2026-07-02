// Assistant endpoints: memory-first chat, knowledge-grounded ask, conversation + messages,
// memory stats, and knowledge-base / document management (Sources).

import { api } from "../core/api.js";

/* Chat & RAG */
export function chat(conversationId, message) { return api.post("/chat", { conversationId, message }); }
export function ask({ conversationId, knowledgeBaseId, question }) {
    return api.post("/ask", { conversationId, knowledgeBaseId, question });
}

/* Conversation (no list endpoint — caller keeps one active id) */
export function createConversation(userId, title) { return api.post("/conversations", { userId, title }); }
export function messages(conversationId) { return api.get(`/conversations/${conversationId}/messages`); }

/* Memory (counts only) */
export function memoryStats(userId) { return api.get(`/memories/statistics/${userId}`); }

/* Knowledge bases + documents (Sources) */
export function knowledgeBases() { return api.get("/knowledge-bases"); }
export function createKnowledgeBase(body) { return api.post("/knowledge-bases", body); }
export function documents(kbId) { return api.get(`/documents/knowledge-base/${kbId}`); }
export function uploadDocument(kbId, file) {
    const fd = new FormData();
    fd.append("file", file);
    return api.upload(`/documents/upload?knowledgeBaseId=${kbId}`, fd);
}
export function deleteDocument(docId) { return api.del(`/documents/${docId}`); }
