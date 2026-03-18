package com.noa.packer.dto;

import com.noa.packer.models.User;

public class UserDTO {

    private String userId;
    private String username;
    private String email;
    private String subscriptionPlan;
    private String subscriptionStatus;
    private String billingCycle;

    public UserDTO() {}

    public UserDTO(User user) {
        this.userId             = user.getId().toString();
        this.username           = user.getUsername();
        this.email              = user.getEmail();
        this.subscriptionPlan   = user.getSubscriptionPlan();
        this.subscriptionStatus = user.getSubscriptionStatus();
        this.billingCycle       = user.getBillingCycle();
    }

    public String getUserId()                          { return userId; }
    public void   setUserId(String userId)             { this.userId = userId; }
    public String getUsername()                        { return username; }
    public void   setUsername(String username)         { this.username = username; }
    public String getEmail()                           { return email; }
    public void   setEmail(String email)               { this.email = email; }
    public String getSubscriptionPlan()                { return subscriptionPlan; }
    public void   setSubscriptionPlan(String v)        { this.subscriptionPlan = v; }
    public String getSubscriptionStatus()              { return subscriptionStatus; }
    public void   setSubscriptionStatus(String v)      { this.subscriptionStatus = v; }
    public String getBillingCycle()                    { return billingCycle; }
    public void   setBillingCycle(String v)            { this.billingCycle = v; }
}