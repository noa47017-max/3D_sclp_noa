package com.noa.packer.repositories;

import com.noa.packer.models.Container;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ContainerRepository {

    private final MongoTemplate mongoTemplate;

    public ContainerRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Container save(Container container) {
        return mongoTemplate.save(container);
    }

    public Container getById(ObjectId id) {
        return mongoTemplate.findById(id, Container.class);
    }

    public List<ObjectId> getContainerIdsByUserId(ObjectId userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        List<Container> containers = mongoTemplate.find(query, Container.class);
        return containers.stream()
                .map(Container::getId)
                .collect(Collectors.toList());
    }
}