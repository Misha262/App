package com.app.model;

/**
 * Simple DTO that mirrors the USERS table.
 */
public class User {

    private int userId;
    private String name;
    private String email;
    private String avatarPath;
    private String bio;
    private String createdAt;
    private String passwordHash;
    private String role = "USER";

    public User() {
    }

    public User(int userId,
                String name,
                String email,
                String avatarPath,
                String bio,
                String createdAt,
                String passwordHash,
                String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.avatarPath = avatarPath;
        this.bio = bio;
        this.createdAt = createdAt;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
