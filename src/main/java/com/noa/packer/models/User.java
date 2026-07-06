package com.noa.packer.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
@Document(collection = "users")
public class User {
    @Id
    private ObjectId id;

    // פרטי התחברות
    private String username;
    private String email;
    private String password;

    // מנוי
    private String subscriptionPlan;    // basic / pro / enterprise
    private String subscriptionStatus;  // active / expired / cancelled
    private String billingCycle;        // monthly / yearly
    private Date   subscriptionStart;
    private Date   subscriptionEnd;
    private String paymentMethod;

    // מטא
    private Date createdAt;
    
    // שחזור סיסמא
    private String resetToken;
    private Date   resetTokenExpiry;


    // ── Constructors ──────────────────────────────
    public User() {}

    public User(String username, String email, String password) {
        this.username          = username;
        this.email             = email;
        this.password          = password;
        this.subscriptionPlan   = "basic";
        this.subscriptionStatus = "active";
        this.billingCycle       = "monthly";
        this.subscriptionStart  = new Date();
        this.createdAt          = new Date();
    }

    // ── Getters & Setters ─────────────────────────
    public ObjectId getId()                        { return id; }
    public void     setId(ObjectId id)             { this.id = id; }

    public String getUsername()                    { return username; }
    public void   setUsername(String username)     { this.username = username; }

    public String getEmail()                       { return email; }
    public void   setEmail(String email)           { this.email = email; }

    public String getPassword()                    { return password; }
    public void   setPassword(String password)     { this.password = password; }

    public String getSubscriptionPlan()                        { return subscriptionPlan; }
    public void   setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public String getSubscriptionStatus()                          { return subscriptionStatus; }
    public void   setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public String getBillingCycle()                      { return billingCycle; }
    public void   setBillingCycle(String billingCycle)   { this.billingCycle = billingCycle; }

    public Date getSubscriptionStart()                       { return subscriptionStart; }
    public void setSubscriptionStart(Date subscriptionStart) { this.subscriptionStart = subscriptionStart; }

    public Date getSubscriptionEnd()                     { return subscriptionEnd; }
    public void setSubscriptionEnd(Date subscriptionEnd) { this.subscriptionEnd = subscriptionEnd; }

    public String getPaymentMethod()                       { return paymentMethod; }
    public void   setPaymentMethod(String paymentMethod)   { this.paymentMethod = paymentMethod; }

    public Date getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(Date createdAt)     { this.createdAt = createdAt; }
    public String getResetToken()                    { return resetToken; }
    public void   setResetToken(String resetToken)   { this.resetToken = resetToken; }

    public Date getResetTokenExpiry()                        { return resetTokenExpiry; }
    public void setResetTokenExpiry(Date resetTokenExpiry)   { this.resetTokenExpiry = resetTokenExpiry; }
}