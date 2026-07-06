package com.noa.packer.repositories;

import com.noa.packer.models.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public class UserRepository {

    private final MongoTemplate mongoTemplate;

    public UserRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void addUser(User user) {
        mongoTemplate.save(user);
    }

    public User getUserByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, User.class);
    }

    public User getUserByUsername(String username) {
        Query query = new Query(Criteria.where("username").is(username));
        return mongoTemplate.findOne(query, User.class);
    }

    public void saveResetToken(String email, String token, Date expiry) {
        Query query = new Query(Criteria.where("email").is(email));
        Update update = new Update()
                .set("resetToken", token)
                .set("resetTokenExpiry", expiry);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    public User getUserByResetToken(String token) {
        Query query = new Query(Criteria.where("resetToken").is(token));
        return mongoTemplate.findOne(query, User.class);
    }

    public void updatePassword(String email, String hashedPassword) {
        Query query = new Query(Criteria.where("email").is(email));
        Update update = new Update()
                .set("password", hashedPassword)
                .set("resetToken", null)
                .set("resetTokenExpiry", null);
        mongoTemplate.updateFirst(query, update, User.class);
    }
}