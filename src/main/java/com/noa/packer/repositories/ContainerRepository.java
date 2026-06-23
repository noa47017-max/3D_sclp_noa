package com.noa.packer.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.noa.packer.config.MongoConnection;
import com.noa.packer.models.Container;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Repository
public class ContainerRepository {

    private final MongoCollection<Document> collection;

    public ContainerRepository() {
        MongoDatabase db = MongoConnection.connect();
        this.collection  = db.getCollection("containers");
    }

    // ── המרות ────────────────────────────────────
    private Document containerToDocument(Container c) {
        return new Document()
            .append("userId",    c.getUserId())
            .append("length",    c.getLength())
            .append("width",     c.getWidth())
            .append("height",    c.getHeight())
            .append("volume",    c.getVolume());
    }

    private Container documentToContainer(Document doc) {
        Container c = new Container();
        c.setId(doc.getObjectId("_id"));
        c.setUserId(doc.getObjectId("userId"));
        c.setLength(doc.getDouble("length"));
        c.setWidth(doc.getDouble("width"));
        c.setHeight(doc.getDouble("height"));
        c.setVolume(doc.getDouble("volume"));
        return c;
    }

    // ➕ שמירת מכולה חדשה
    public ObjectId save(Container container) {
        Document doc = containerToDocument(container);
        collection.insertOne(doc);
        ObjectId newId = doc.getObjectId("_id");
        container.setId(newId);
        System.out.println("✅ Container saved: ");
        return newId;
    }



    // 🔍 מכולה לפי ID
    public Container getById(ObjectId id) {
        Document doc = collection.find(eq("_id", id)).first();
        return doc != null ? documentToContainer(doc) : null;
    }

}