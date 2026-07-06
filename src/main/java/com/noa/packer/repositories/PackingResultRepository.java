package com.noa.packer.repositories;

import com.noa.packer.models.PackingResult;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PackingResultRepository {

    private final MongoTemplate mongoTemplate;

    public PackingResultRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ObjectId save(PackingResult result) {
        PackingResult saved = mongoTemplate.save(result);
        return saved.getId();
    }

    public PackingResult getById(ObjectId id) {
        return mongoTemplate.findById(id, PackingResult.class);
    }

    public List<PackingResult> getByUserId(ObjectId userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, PackingResult.class);
    }

    public List<PackingResult> getByContainerIds(List<ObjectId> containerIds) {
        if (containerIds == null || containerIds.isEmpty()) {
            return List.of();
        }
        Query query = new Query(Criteria.where("containerId").in(containerIds))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, PackingResult.class);
    }
}