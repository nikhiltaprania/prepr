package com.springrag.conversation.web;

import com.springrag.conversation.dto.CachedMessage;
import com.springrag.conversation.service.RecentMessageStore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memory")
public class RecentMessageController {

    private final RecentMessageStore recentMessageStore;

    public RecentMessageController(RecentMessageStore recentMessageStore) {
        this.recentMessageStore = recentMessageStore;
    }

    @GetMapping("/{conversationId}")
    @PreAuthorize("@access.ownsConversation(#conversationId) or hasRole('ADMIN')")
    public List<CachedMessage> getMemory(@PathVariable UUID conversationId) {
        return recentMessageStore.getRecentMessages(conversationId);
    }

    @DeleteMapping("/{conversationId}")
    @PreAuthorize("@access.ownsConversation(#conversationId) or hasRole('ADMIN')")
    public void clearMemory(@PathVariable UUID conversationId) {
        recentMessageStore.clearMemory(conversationId);
    }
}
