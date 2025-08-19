package com.shecancode.leavemanagementsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthDtos {
    @Data
    public static class RegisterRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String fullName;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String email;
        @NotBlank
        private String password;
        private String twoFactorCode; // stubbed
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String role;
        private String fullName;
        private String email;
        private String avatarUrl;
    }
}
