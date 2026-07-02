package com.springrag.conversation.domain;

import jakarta.persistence.*;
import com.springrag.common.domain.BaseEntity;
import com.springrag.user.domain.User;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversation_user_id", columnList = "user_id")
})
public class Conversation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private boolean active = true;

    public Conversation() {
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
