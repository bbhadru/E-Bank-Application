package com.ebank.controller;

import com.ebank.dto.LoginRequest;
import com.ebank.dto.LoginResponse;
import com.ebank.model.User;
import com.ebank.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password, or account is locked.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<LoginResponse> loginApi(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> registerApi(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String role = payload.getOrDefault("role", "STAFF");

        User user = authService.registerUser(username, password, role);
        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "userId", user.getUserId(),
                "username", user.getUsername()
        ));
    }

    @PostMapping("/api/auth/unlock/{userId}")
    @ResponseBody
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        authService.unlockUser(userId);
        return ResponseEntity.ok(Map.of("message", "User account unlocked successfully"));
    }
}
