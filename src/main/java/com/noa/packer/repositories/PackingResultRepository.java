package com.noa.packer.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.noa.packer.config.MongoConnection;
import com.noa.packer.models.PackingResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

@Repository
public class PackingResultRepository {

    private final MongoCollection<Document> collection;

    public PackingResultRepository() {
        MongoDatabase db = MongoConnection.connect();
        this.collection  = db.getCollection("packing_results");
    }

    // ── המרה: PlacedBoxData → Document ───────────
    private Document placementToDocument(PackingResult.PlacedBoxData p) {
        return new Document()
            .append("cargoType",      p.getCargoType())
            .append("direction",      p.getDirection())
            .append("leftBackBottom", List.of(
                p.getLeftBackBottom()[0],
                p.getLeftBackBottom()[1],
                p.getLeftBackBottom()[2]))
            .append("rightFrontTop",  List.of(
                p.getRightFrontTop()[0],
                p.getRightFrontTop()[1],
                p.getRightFrontTop()[2]));
    }

    // ── המרה: Document → PlacedBoxData ───────────
    private PackingResult.PlacedBoxData documentToPlacement(Document doc) {
        List<Double> lbb = doc.getList("leftBackBottom", Double.class);
        List<Double> rft = doc.getList("rightFrontTop",  Double.class);
        return new PackingResult.PlacedBoxData(
            doc.getInteger("cargoType"),
            doc.getInteger("direction"),
            new double[]{lbb.get(0), lbb.get(1), lbb.get(2)},
            new double[]{rft.get(0), rft.get(1), rft.get(2)}
        );
    }

    // ── המרה: PackingResult → Document ───────────
    private Document resultToDocument(PackingResult r) {
        List<Document> placementDocs = new ArrayList<>();
        for (PackingResult.PlacedBoxData p : r.getPlacements())
            placementDocs.add(placementToDocument(p));

        return new Document()
            .append("userId",          r.getUserId())
            .append("containerId",     r.getContainerId())  // ← containerId בלבד
            .append("totalCargoVolume",r.getTotalCargoVolume())
            .append("vur",             r.getVur())
            .append("placements",      placementDocs)
            .append("createdAt",       r.getCreatedAt());
    }

    // ── המרה: Document → PackingResult ───────────
    private PackingResult documentToResult(Document doc) {
        List<Document> placementDocs = doc.getList("placements", Document.class);
        List<PackingResult.PlacedBoxData> placements = new ArrayList<>();
        if (placementDocs != null)
            for (Document p : placementDocs)
                placements.add(documentToPlacement(p));

        PackingResult r = new PackingResult();
        r.setId(doc.getObjectId("_id"));
        r.setUserId(doc.getObjectId("userId"));
        r.setContainerId(doc.getObjectId("containerId"));  // ← containerId בלבד
        r.setTotalCargoVolume(doc.getDouble("totalCargoVolume"));
        r.setVur(doc.getDouble("vur"));
        r.setPlacements(placements);
        r.setCreatedAt(doc.getDate("createdAt"));
        return r;
    }

    // ➕ שמירת תוצאה חדשה
    public ObjectId save(PackingResult result) {
        Document doc = resultToDocument(result);
        collection.insertOne(doc);
        System.out.println("✅ Packing result saved! VUR: " + result.getVur());
        return doc.getObjectId("_id");
    }

    // 📖 כל התוצאות של משתמש (לפי תאריך יורד)
    public List<PackingResult> getByUserId(ObjectId userId) {
        List<PackingResult> results = new ArrayList<>();
        for (Document doc : collection.find(eq("userId", userId))
                                      .sort(descending("createdAt")))
            results.add(documentToResult(doc));
        return results;
    }

    // 🔍 תוצאה בודדת לפי ID
    public PackingResult getById(ObjectId id) {
        Document doc = collection.find(eq("_id", id)).first();
        return doc != null ? documentToResult(doc) : null;
    }

    // 🗑️ מחיקת תוצאה
    public void delete(ObjectId id) {
        collection.deleteOne(eq("_id", id));
        System.out.println("🗑️ Packing result deleted!");
    }
}