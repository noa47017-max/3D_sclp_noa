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
            .append("userId",              b.getUserId())
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
        b.setUserId(doc.getObjectId("userId"));
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
        System.out.println("✅ Box saved: " + newId);
        return newId;
    }

    // ➕ שמירת רשימת ארגזים
    public List<ObjectId> saveAll(List<Box> boxes) {
        List<ObjectId> ids = new ArrayList<>();
        for (Box box : boxes)
            ids.add(save(box));
        return ids;
    }

    // 📖 כל הארגזים של משתמש
    public List<Box> getByUserId(ObjectId userId) {
        List<Box> list = new ArrayList<>();
        for (Document doc : collection.find(eq("userId", userId)))
            list.add(documentToBox(doc));
        return list;
    }

    // 🔍 ארגז לפי ID
    public Box getById(ObjectId id) {
        Document doc = collection.find(eq("_id", id)).first();
        return doc != null ? documentToBox(doc) : null;
    }

    // 🗑️ מחיקת ארגז
    public void delete(ObjectId id) {
        collection.deleteOne(eq("_id", id));
        System.out.println("🗑️ Box deleted!");
    }

    // 🔄 המרה: Box (DB) → CargoType (אלגוריתם)
    public com.noa.packer.algorithms.CargoType toCargoType(Box box, int index) {
        return new com.noa.packer.algorithms.CargoType(
            index,
            box.getL(),
            box.getW(),
            box.getH(),
            box.getQuantity(),
            box.getAllowedOrientations()
        );
    }
}

