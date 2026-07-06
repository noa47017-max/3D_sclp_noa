package com.noa.packer.repositories;

import com.noa.packer.models.Box;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BoxRepository {

    private final MongoTemplate mongoTemplate;

    public BoxRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Box save(Box box) {
        return mongoTemplate.save(box);
    }
}


