package com.dano.chatapp;

import com.google.firebase.database.PropertyName;

public class RecentChat {
    private String userId;
    private String name;
    private String profileImage;
    private String lastMessage;
    private long timestamp;
    private int unreadCount;

    public RecentChat() {
    }

    @PropertyName("id")
    public String getUserId() {
        return userId;
    }

    @PropertyName("id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
