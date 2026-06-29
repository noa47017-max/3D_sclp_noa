// dto/LoginRequest.java
package com.noa.packer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "אימייל הוא שדה חובה")
    @Email(message = "פורמט אימייל לא תקין")
    private String email;

    @NotBlank(message = "סיסמא היא שדה חובה")
    private String password;

    // Getters & Setters
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}
