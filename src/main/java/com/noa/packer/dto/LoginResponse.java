// dto/LoginResponse.java
package com.noa.packer.dto;

public class LoginResponse {

    private String status;
    private String token;
    private String userId;
    private String username;
    private String message;

    // Constructor הצלחה
    public LoginResponse(String token, String userId, String username) {
        this.status   = "success";
        this.token    = token;
        this.userId   = userId;
        this.username = username;
    }

    // Constructor כישלון
    public LoginResponse(String message) {
        this.status  = "fail";
        this.message = message;
    }

    // Getters
    public String getStatus()   { return status; }
    public String getToken()    { return token; }
    public String getUserId()   { return userId; }
    public String getUsername()  { return username; }
    public String getMessage()  { return message; }
}