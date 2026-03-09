package com.noa.packer.controllers;
import com.noa.packer.models.User;

import com.noa.packer.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // ➕ הרשמה
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        String result = userService.registerUser(
            body.get("username"),
            body.get("email"),
            body.get("password")
        );
        return ResponseEntity.ok(result);
    }

    // 🔐 התחברות
   @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        Map<String, String> response = new HashMap<>();

        User user = userService.getUserAfterLogin(body.get("username"), body.get("password"));

        if (user != null) {
            response.put("status", "success");
            response.put("userId",  user.getId().toString()); // ← חדש
            response.put("username", user.getUsername());
        } else {
            response.put("status",  "fail");
            response.put("message", "שם משתמש או סיסמא שגויים");
        }
        return ResponseEntity.ok(response);
    }

    // 📧 בקשת שחזור סיסמא
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String result = userService.requestPasswordReset(body.get("email"));
        return ResponseEntity.ok(result);
    }

    // 🔒 איפוס סיסמא
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String result = userService.resetPassword(
            body.get("token"),
            body.get("newPassword")
        );
        return ResponseEntity.ok(result);
    }
}

