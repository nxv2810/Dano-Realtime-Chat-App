package com.dano.chatapp;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String profileImage;
    private String status;

    // Constructor trống cho Firebase
    public User() {
    }

    public User(String uid, String name, String email, String phone, String profileImage, String status) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImage = profileImage;
        this.status = status;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
