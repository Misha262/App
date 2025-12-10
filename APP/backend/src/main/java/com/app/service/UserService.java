package com.app.service;

import com.app.model.User;
import com.app.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

/**
 * High-level operations around users.
 */
public class UserService {

    private final UserRepository userRepository = new UserRepository();

    public User getById(int id) {
        try {
            User user = userRepository.findById(id);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load user: " + e.getMessage(), e);
        }
    }

    public User getByEmail(String email) {
        try {
            User user = userRepository.findByEmail(email.toLowerCase().trim());
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load user: " + e.getMessage(), e);
        }
    }

    public User updateProfile(int id, String name, String bio, String avatarPath) {
        try {
            User current = getById(id);

            String newName = (name == null || name.isBlank()) ? current.getName() : name.trim();
            String newBio = (bio == null) ? current.getBio() : bio;
            String newAvatar = (avatarPath == null) ? current.getAvatarPath() : avatarPath;

            return userRepository.updateProfile(id, newName, newBio, newAvatar);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    public void changePassword(int id, String oldPw, String newPw) {
        try {
            if (newPw == null || newPw.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters");
            }

            String currentHash = userRepository.getPasswordHashById(id);
            if (currentHash == null) {
                throw new IllegalArgumentException("User not found");
            }

            if (oldPw == null || !BCrypt.checkpw(oldPw, currentHash)) {
                throw new IllegalArgumentException("Current password incorrect");
            }

            String newHash = BCrypt.hashpw(newPw, BCrypt.gensalt());
            userRepository.updatePasswordHash(id, newHash);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage(), e);
        }
    }
}
