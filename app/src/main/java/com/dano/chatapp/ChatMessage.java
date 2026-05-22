package com.dano.chatapp;

public class ChatMessage {
    private String messageId;
    private String sender;
    private String message;
    private String imageUrl;
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

    public ChatMessage(String sender, String message, String imageUrl, long timestamp, String senderPhotoUrl) {
        this.sender = sender;
        this.message = message;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.senderPhotoUrl = senderPhotoUrl;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
