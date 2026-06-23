package com.noa.packer.controllers;

import com.noa.packer.models.User;
import com.noa.packer.repositories.UserRepository;
import com.noa.packer.security.CustomUserDetails;
import com.noa.packer.services.UserService;
import com.noa.packer.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController extends UserRepository {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService; // ← הזרקת שירות הטוקנים

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

    //  התחברות מעודכנת עם טוקן
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        Map<String, String> response = new HashMap<>();
        System.out.println("DEBUG: Entering login method");
        User user = userService.getUserAfterLogin(body.get("username"), body.get("password"));

        if (user != null) {
            // הגרלת טוקן עבור המשתמש שהתחבר בהצלחה
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String token = jwtService.generateToken(userDetails);

            response.put("status", "success");
            response.put("token", token); // ← הוספת הטוקן לתשובה שמתקבלת ב-Client
            response.put("userId",  user.getId().toString()); 
            response.put("username", user.getUsername());
        } else {
            response.put("status",  "fail");
            response.put("message", "שם משתמש או סיסמא שגויים");
        }
        return ResponseEntity.ok(response);
    }

    //  בקשת שחזור סיסמא (משתמש בטוקן פנימי של המערכת לשחזור, לא קשור ל-JWT)
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String result = userService.requestPasswordReset(body.get("email"));
        return ResponseEntity.ok(result);
    }

    //  איפוס סיסמא
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String result = userService.resetPassword(
            body.get("token"),
            body.get("newPassword")
        );
        return ResponseEntity.ok(result);
    }
}