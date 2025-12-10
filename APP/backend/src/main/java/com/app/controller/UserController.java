package com.app.controller;

import com.app.dto.ChangePasswordRequest;
import com.app.dto.UpdateProfileRequest;
import com.app.dto.UserResponse;
import com.app.model.User;
import com.app.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {

    private final UserService userService = new UserService();

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable int id) {
        return UserResponse.from(userService.getById(id));
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable int id, @RequestBody UpdateProfileRequest req) {
        User updated = userService.updateProfile(id, req.getName(), req.getBio(), req.getAvatarPath());
        return UserResponse.from(updated);
    }

    @PostMapping("/{id}/change-password")
    public void changePassword(@PathVariable int id, @RequestBody ChangePasswordRequest req) {
        userService.changePassword(id, req.getCurrentPassword(), req.getNewPassword());
    }
}
