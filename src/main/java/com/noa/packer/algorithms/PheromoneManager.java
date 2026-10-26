
package com.noa.packer.algorithms;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PheromoneManager {

    // פרמטרים לפי Table 2 במאמר [3]
    private final double ALPHA = 1.0;
    private final double BETA = 3.0;
    private final double RHO = 0.7; // מקדם התנדפות קבוע
    private final double RHO_LOCAL = 0.1;  // Local evaporation
    private final int Q = 8;
    private final double TAU0 = 0.1;
    private static final int MAX_TREE_SIZE = 50_000;
    private final double DELTA_LOCAL = 0.2;
    private ConcurrentSkipListMap<String, List<PheromoneNode>> pheromoneTree;
    private final AtomicInteger treeSize;

    public PheromoneManager() {
        this.pheromoneTree = new ConcurrentSkipListMap<>();
        this.treeSize = new AtomicInteger(0); // אתחול המונה
    }

    public double getRho() { return RHO; }
    public double getTau0() { return TAU0; }
    public double getAlpha() { return ALPHA; }
    public double getBeta() { return BETA; }

    // יצירת מילת מצב
    public String generateStateWord(List<CargoType> allTypes) {
        StringBuilder stateBuilder = new StringBuilder(allTypes.size() * (1 + Q));
        StringBuilder quantityDomain = new StringBuilder();

        for (CargoType type : allTypes) {
            // 1. בניית ה-Type Domain: האם הסוג נטען לפחות פעם אחת?
            int loadedCount = type.getTotalQuantity() - type.getQuantity();
            stateBuilder.append(loadedCount > 0 ? "1" : "0");
            // 2. בניית ה-Quantity Domain: הכמות המדויקת שנטענה (בבינארי)
            String binQty = Integer.toBinaryString(loadedCount);
            // ריפוד אפסים
            int paddingNeeded = Q - binQty.length();
            for (int i = 0; i < paddingNeeded; i++) {
                quantityDomain.append('0');
            }
            quantityDomain.append(binQty);
        }
        // שרשור התחומים
        return stateBuilder.append(quantityDomain).toString();
    }

    // אידוי של כל הצמתים
    public void applyGlobalEvaporation() {
        for (List<PheromoneNode> nodes : pheromoneTree.values()) {
            synchronized (nodes) {
                for (PheromoneNode node : nodes) {
                    // נידוף
                    double decayed = (1 - RHO) * node.getConcentration()+RHO*TAU0;
                    node.setConcentration(decayed);
                }
            }
        }
        pruneTreeIfNeeded();
    }

    // חיתוך הרשימה אם עברה את הגודל המקסימלי
    private void pruneTreeIfNeeded() {
        if (treeSize.get() > MAX_TREE_SIZE) {
            int targetSize = (int) (MAX_TREE_SIZE * 0.95);
            int numToRemove = treeSize.get() - targetSize;

            List<Map.Entry<String, List<PheromoneNode>>> entries =
                    new ArrayList<>(pheromoneTree.entrySet());

            // חישוב הציון פעם אחת לכל מפתח (O(n)) במקום בכל השוואה (O(n log n) חישובים)
            record Scored(String key, double score) {}
            List<Scored> scored = new ArrayList<>(entries.size());
            for (Map.Entry<String, List<PheromoneNode>> entry : entries) {
                List<PheromoneNode> nodes = entry.getValue();
                double sc = 0.0;
                if (nodes != null && !nodes.isEmpty()) {
                    synchronized (nodes) {
                        for (PheromoneNode n : nodes) sc = Math.max(sc, n.getConcentration());
                    }
                }
                scored.add(new Scored(entry.getKey(), sc));
            }
            scored.sort(Comparator.comparingDouble(Scored::score));

            int removedCount = 0;
            for (int i = 0; i < scored.size() && removedCount < numToRemove; i++) {
                if (pheromoneTree.remove(scored.get(i).key()) != null) {
                    removedCount++;
                }
            }
            treeSize.addAndGet(-removedCount);
        }
    }


    //  GLOBAL - עדכון פורמונים לפתרון הטוב ביותר אחרי איטרציה
    public void updateGlobalPheromone(String stateKey, int cargoId, int direction,
                                      double x, double y, double z, double x2, double y2, double z2,
                                      double currentVR, double delta) {

        List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> {
            treeSize.incrementAndGet(); // הגדלת המונה רק כאשר נוצר מפתח חדש
            return Collections.synchronizedList(new ArrayList<>());
        });
        synchronized (nodes) {
            PheromoneNode node = nodes.stream()
                    .filter(n -> n.getCargoId() == cargoId && n.getD() == direction)
                    .findFirst()
                    .orElse(null);
            if (node != null) {
                node.setConcentration(node.getConcentration() + delta);
                if (currentVR < node.getVr()) {
                    node.setVR(currentVR);
                    node.setDirection(direction);
                    node.setLeftBackBottom(new double[]{x, y, z});
                }
            } else {
                PheromoneNode newNode = new PheromoneNode(
                        cargoId, direction, x, y, z, x2, y2, z2, currentVR, TAU0 + delta);
                nodes.add(newNode);
            }
        }
    }

    //  קריאה של פרומון (עם decay)
    public double getPheromone(String stateKey, int cargoId , int direction) {
        List<PheromoneNode> nodes = pheromoneTree.get(stateKey);
        if (nodes != null) {
            synchronized (nodes) {
                for (PheromoneNode node : nodes) {
                    if (node.getCargoId() == cargoId && node.getD() == direction) {
                        return node.getConcentration();
                    }
                }
            }
        }
        return TAU0; // ערך ראשוני
    }

    // עדכון פורמונים במהלך הריצה
    public void depositAndEvaporate(String stateKey, int cargoId,
                                    Orientation direction, Space space, double currentVR) {
        List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> {
            treeSize.incrementAndGet(); // הגדלת המונה רק כאשר נוצר מפתח חדש
            return Collections.synchronizedList(new ArrayList<>());
        });
        synchronized (nodes) {
            PheromoneNode node = nodes.stream()
                    .filter(n -> n.getCargoId() == cargoId && n.getD() == direction.id)
                    .findFirst()
                    .orElse(null);

            if (node != null) {
                node.setConcentration((1-RHO_LOCAL)*node.getConcentration() + RHO_LOCAL * TAU0);
            } else {
                double initialConcentration = TAU0 + DELTA_LOCAL;
                PheromoneNode newNode = new PheromoneNode(
                        cargoId, direction.id, space.x, space.y, space.z,
                        space.x + direction.l, space.y + direction.w,
                        space.z + direction.h, currentVR, initialConcentration);
                nodes.add(newNode);
            }
        }
    }
    public void reset() {
        pheromoneTree.clear();
        treeSize.set(0);
    }

}
