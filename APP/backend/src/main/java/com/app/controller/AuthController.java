package com.app.controller;

import com.app.model.User;
import com.app.security.JwtUtil;
import com.app.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService = new AuthService();

    /** -------------------------
     *  POST /api/auth/register
     *  ------------------------- */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            User user = authService.register(name, email, password);

            String token = JwtUtil.generateToken(
                    user.getUserId(),
                    user.getEmail(),
                    60L * 60L * 24L * 7L // 7 days
            );

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    /** -------------------------
     *  POST /api/auth/login
     *  ------------------------- */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        try {
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            User user = authService.login(email, password);
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid email or password"));
            }

            String token = JwtUtil.generateToken(
                    user.getUserId(),
                    user.getEmail(),
                    60L * 60L * 24L * 7L // 7 days
            );

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** -------------------------
     *  POST /api/auth/change-password
     *  ------------------------- */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestAttribute(value = "userId", required = false) Integer userId,
                                            @RequestBody Map<String, Object> body) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            String current = (String) body.get("currentPassword");
            String next = (String) body.get("newPassword");
            authService.changePassword(userId, current, next);
            return ResponseEntity.ok(Map.of("message", "Password updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** -------------------------
     *  POST /api/auth/change-email
     *  ------------------------- */
    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestAttribute(value = "userId", required = false) Integer userId,
                                         @RequestBody Map<String, Object> body) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            String email = (String) body.get("email");
            User updated = authService.changeEmail(userId, email);
            return ResponseEntity.ok(Map.of("user", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
