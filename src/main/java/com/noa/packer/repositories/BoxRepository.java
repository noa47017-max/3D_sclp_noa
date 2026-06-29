package com.noa.packer.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.noa.packer.config.MongoConnection;
import com.noa.packer.models.Box;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Repository
public class BoxRepository {

    private final MongoCollection<Document> collection;

    public BoxRepository() {
        MongoDatabase db = MongoConnection.connect();
        this.collection  = db.getCollection("boxes");
    }

    // ── המרה: Box → Document ─────────────────────
    private Document boxToDocument(Box b) {
        return new Document()
            .append("containerId",              b.getcontainerId())
            .append("l",                   b.getL())
            .append("w",                   b.getW())
            .append("h",                   b.getH())
            .append("quantity",            b.getQuantity())
            .append("allowedOrientations", b.getAllowedOrientations());
    }

    // ── המרה: Document → Box ─────────────────────
    private Box documentToBox(Document doc) {
        Box b = new Box();
        b.setId(doc.getObjectId("_id"));
        b.setcontainerId(doc.getObjectId("containerId"));
        b.setL(doc.getDouble("l"));
        b.setW(doc.getDouble("w"));
        b.setH(doc.getDouble("h"));
        b.setQuantity(doc.getInteger("quantity"));
        b.setAllowedOrientations(doc.getList("allowedOrientations", Integer.class));
        return b;
    }

    // ➕ שמירת ארגז אחד
    public ObjectId save(Box box) {
        Document doc = boxToDocument(box);
        collection.insertOne(doc);
        ObjectId newId = doc.getObjectId("_id");
        box.setId(newId);
        System.out.println("Box saved: " + newId);
        return newId;
    }

}

