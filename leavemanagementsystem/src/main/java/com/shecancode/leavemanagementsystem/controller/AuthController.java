package com.shecancode.leavemanagementsystem.controller;

import com.shecancode.leavemanagementsystem.dto.AuthDtos;
import com.shecancode.leavemanagementsystem.model.Role;
import com.shecancode.leavemanagementsystem.model.User;
import com.shecancode.leavemanagementsystem.repository.UserRepository;
import com.shecancode.leavemanagementsystem.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(Role.STAFF)
                .googleAvatarUrl("https://www.gravatar.com/avatar/" + Integer.toHexString(req.getEmail().hashCode()))
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        String token = jwtService.generateToken(user, claims);
        AuthDtos.AuthResponse resp = new AuthDtos.AuthResponse();
        resp.setToken(token);
        resp.setRole(user.getRole().name());
        resp.setFullName(user.getFullName());
        resp.setEmail(user.getEmail());
        resp.setAvatarUrl(user.getGoogleAvatarUrl());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        // 2FA stub: if enabled, ensure code is present (not verifying for brevity)
        if (user.isTwoFactorEnabled() && (req.getTwoFactorCode() == null || req.getTwoFactorCode().isBlank())) {
            return ResponseEntity.status(401).body(Map.of("error", "2FA code required"));
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        String token = jwtService.generateToken(user, claims);
        AuthDtos.AuthResponse resp = new AuthDtos.AuthResponse();
        resp.setToken(token);
        resp.setRole(user.getRole().name());
        resp.setFullName(user.getFullName());
        resp.setEmail(user.getEmail());
        resp.setAvatarUrl(user.getGoogleAvatarUrl());
        return ResponseEntity.ok(resp);
    }

}
