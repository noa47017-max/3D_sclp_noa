package com.noa.packer.controllers;

import com.noa.packer.dto.LoginRequest;
import com.noa.packer.dto.LoginResponse;
import com.noa.packer.dto.RegisterRequest;
import com.noa.packer.models.User;
import com.noa.packer.security.CustomUserDetails;
import com.noa.packer.services.UserService;
import com.noa.packer.security.JwtService;
import com.noa.packer.util.SanitizeUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService; // ← הזרקת שירות הטוקנים

    //הרשמה
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String result = userService.registerUser(
                SanitizeUtil.clean(request.getUsername()),
                SanitizeUtil.clean(request.getEmail()),
                request.getPassword()
        );
        return ResponseEntity.ok(result);
    }

    //  התחברות מעודכנת עם טוקן
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getUserAfterLogin(SanitizeUtil.clean(request.getEmail()),
                request.getPassword());

        if (user != null) {
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            return ResponseEntity.ok(new LoginResponse(token, user.getId().toString(), user.getUsername()));
        }
        return ResponseEntity.ok(new LoginResponse("אימייל או סיסמא שגויים"));
    }
}