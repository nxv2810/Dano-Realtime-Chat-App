package com.dano.chatapp;

public class ChatMessage {
    private String sender;
    private String message;
    private long timestamp;
    private String senderPhotoUrl;

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String sender, String message, long timestamp, String senderPhotoUrl) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.senderPhotoUrl = senderPhotoUrl;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderPhotoUrl() {
        return senderPhotoUrl;
    }

    public void setSenderPhotoUrl(String senderPhotoUrl) {
        this.senderPhotoUrl = senderPhotoUrl;
    }
}
