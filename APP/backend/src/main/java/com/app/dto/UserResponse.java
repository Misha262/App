package com.app.dto;

import com.app.model.User;
import lombok.Data;

@Data
public class UserResponse {

    private int id;
    private String name;
    private String email;
    private String bio;
    private String avatarPath;
    private String role;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getUserId();
        dto.name = user.getName();
        dto.email = user.getEmail();
        dto.bio = user.getBio();
        dto.avatarPath = user.getAvatarPath();
        dto.role = user.getRole() == null ? "USER" : user.getRole();
        return dto;
    }
}
