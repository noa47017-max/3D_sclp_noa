package com.noa.packer.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.noa.packer.config.MongoConnection;
import com.noa.packer.models.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
@Repository
public class UserRepository {

    private final MongoCollection<Document> collection;

    public UserRepository() {
        MongoDatabase db = MongoConnection.connect();
        this.collection  = db.getCollection("users");
    }

    // ── המרה: Document → User ─────────────────────
    private User documentToUser(Document doc) {
        User u = new User();
        u.setId(doc.getObjectId("_id"));
        u.setUsername(doc.getString("username"));
        u.setEmail(doc.getString("email"));
        u.setPassword(doc.getString("password"));
        u.setSubscriptionPlan(doc.getString("subscriptionPlan"));
        u.setSubscriptionStatus(doc.getString("subscriptionStatus"));
        u.setBillingCycle(doc.getString("billingCycle"));
        u.setSubscriptionStart(doc.getDate("subscriptionStart"));
        u.setSubscriptionEnd(doc.getDate("subscriptionEnd"));
        u.setPaymentMethod(doc.getString("paymentMethod"));
        u.setCreatedAt(doc.getDate("createdAt"));
        u.setResetToken(doc.getString("resetToken"));
        u.setResetTokenExpiry(doc.getDate("resetTokenExpiry"));
        return u;
    }

    // ── המרה: User → Document ─────────────────────
    private Document userToDocument(User u) {
        return new Document()
            .append("username",          u.getUsername())
            .append("email",             u.getEmail())
            .append("password",          u.getPassword())
            .append("subscriptionPlan",   u.getSubscriptionPlan())
            .append("subscriptionStatus", u.getSubscriptionStatus())
            .append("billingCycle",       u.getBillingCycle())
            .append("subscriptionStart",  u.getSubscriptionStart())
            .append("subscriptionEnd",    u.getSubscriptionEnd())
            .append("paymentMethod",      u.getPaymentMethod())
            .append("createdAt",          u.getCreatedAt());
    }

    // ➕ CREATE
    public void addUser(User user) {
        collection.insertOne(userToDocument(user));
        System.out.println("✅ User added: " + user.getUsername());
    }


    //  READ – לפי אימייל
    public User getUserByEmail(String email) {
        Document doc = collection.find(eq("email", email)).first();
        return doc != null ? documentToUser(doc) : null;
    }


    // 🔍 חיפוש לפי שם משתמש
public User getUserByUsername(String username) {
    Document doc = collection.find(eq("username", username)).first();
    return doc != null ? documentToUser(doc) : null;
}

// 🔑 שמירת Reset Token
public void saveResetToken(String email, String token, java.util.Date expiry) {
    collection.updateOne(
        eq("email", email),
        new Document("$set", new Document()
            .append("resetToken",       token)
            .append("resetTokenExpiry", expiry)
        )
    );
}

// 🔍 חיפוש לפי Reset Token
public User getUserByResetToken(String token) {
    Document doc = collection.find(eq("resetToken", token)).first();
    return doc != null ? documentToUser(doc) : null;
}

// 🔒 עדכון סיסמא חדשה + מחיקת Token
public void updatePassword(String email, String hashedPassword) {
    collection.updateOne(
        eq("email", email),
        new Document("$set", new Document()
            .append("password",         hashedPassword)
            .append("resetToken",       null)
            .append("resetTokenExpiry", null)
        )
    );
}
}
