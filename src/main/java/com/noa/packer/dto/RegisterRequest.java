// dto/RegisterRequest.java
package com.noa.packer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "שם משתמש הוא שדה חובה")
    private String username;

    @NotBlank(message = "אימייל הוא שדה חובה")
    @Email(message = "פורמט אימייל לא תקין")
    private String email;

    @NotBlank(message = "סיסמא היא שדה חובה")
    @Size(min = 6, message = "הסיסמא חייבת להכיל לפחות 6 תווים")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$",
            message = "הסיסמא חייבת להכיל אות גדולה, אות קטנה וספרה"
    )
    private String password;

    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}
