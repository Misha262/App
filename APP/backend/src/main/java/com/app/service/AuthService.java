package com.app.service;

import com.app.model.User;
import com.app.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Сервис аутентификации и работы с паролями.
 */
public class AuthService {

    private final UserRepository userRepo = new UserRepository();

    /**
     * Регистрация нового пользователя.
     * @throws IllegalArgumentException если данные некорректны
     */
    public User register(String name, String email, String password) throws Exception {
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("All fields are required");
        }

        email = email.trim().toLowerCase();
        name = name.trim();

        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userRepo.emailExists(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        return userRepo.createUser(name, email, hash);
    }

    /**
     * Логин пользователя.
     * @return User либо null, если email/пароль неверны
     */
    public User login(String email, String password) throws Exception {
        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return null;
        }

        email = email.trim().toLowerCase();

        String hash = userRepo.getPasswordHashByEmail(email);
        if (hash == null) {
            return null; // user not found
        }

        if (!BCrypt.checkpw(password, hash)) {
            return null; // wrong password
        }

        return userRepo.findByEmail(email);
    }

    /**
     * Смена пароля авторизованным пользователем.
     *
     * @throws IllegalArgumentException если текущий пароль неверный
     */
    public void changePassword(int userId, String currentPassword, String newPassword) throws Exception {
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Passwords must not be empty");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        String currentHash = userRepo.getPasswordHashById(userId);
        if (currentHash == null || !BCrypt.checkpw(currentPassword, currentHash)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userRepo.updatePasswordHash(userId, newHash);
    }

    public User changeEmail(int userId, String newEmail) throws Exception {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("Email must not be empty");
        }
        newEmail = newEmail.trim().toLowerCase();

        if (userRepo.emailExists(newEmail)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        userRepo.updateEmail(userId, newEmail);
        return userRepo.findById(userId);
    }
}
