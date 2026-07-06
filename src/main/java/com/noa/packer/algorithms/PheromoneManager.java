//package com.noa.packer.algorithms;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentSkipListMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class PheromoneManager {
//
//    // פרמטרים לפי Table 2 במאמר [3]
//    private final double ALPHA = 1.0;
//    private final double BETA = 3.0;
//    private final double RHO = 0.7; // מקדם התנדפות קבוע
//    private final double RHO_LOCAL = 0.1;  // Local evaporation
//    private final int Q = 8;
//    private final double TAU0 = 0.1;
//    private static final int MAX_TREE_SIZE = 50_000;
//    private final double DELTA_LOCAL = 0.2;
//    ConcurrentSkipListMap<String, ConcurrentHashMap<Long, PheromoneNode>> pheromoneTree;
//    private final AtomicInteger treeSize;
//
//    public PheromoneManager() {
//        this.pheromoneTree = new ConcurrentSkipListMap<>();
//        this.treeSize = new AtomicInteger(0); // אתחול המונה
//    }
//
//    public double getRho() { return RHO; }
//    public double getTau0() { return TAU0; }
//    public double getAlpha() { return ALPHA; }
//    public double getBeta() { return BETA; }
//
//
//    public String generateStateWord(List<CargoType> allTypes) {
//        StringBuilder stateBuilder = new StringBuilder(allTypes.size() * (1 + Q));
//        StringBuilder quantityDomain = new StringBuilder();
//
//        for (CargoType type : allTypes) {
//            // 1. בניית ה-Type Domain: האם הסוג נטען לפחות פעם אחת?
//            int loadedCount = type.getTotalQuantity() - type.getQuantity();
//            stateBuilder.append(loadedCount > 0 ? "1" : "0");
//            // 2. בניית ה-Quantity Domain: הכמות המדויקת שנטענה (בבינארי)
//            String binQty = Integer.toBinaryString(loadedCount);
//            // ריפוד אפסים
//            int paddingNeeded = Q - binQty.length();
//            for (int i = 0; i < paddingNeeded; i++) {
//                quantityDomain.append('0');
//            }
//            quantityDomain.append(binQty);
//        }
//        // שרשור התחומים
//        return stateBuilder.append(quantityDomain).toString();
//    }
//
//    private static long nodeKey(int cargoId, int direction) {
//        return ((long) cargoId << 32) | (direction & 0xFFFFFFFFL);
//    }
//    public void applyGlobalEvaporation() {
//        for (ConcurrentHashMap<Long, PheromoneNode> inner : pheromoneTree.values()) {
//            for (PheromoneNode node : inner.values()) {
//                double decayed = (1 - RHO) * node.getConcentration() + RHO * TAU0;
//                node.setConcentration(decayed);
//            }
//        }
//        pruneTreeIfNeeded();
//    }
//
//    private void pruneTreeIfNeeded() {
//        int currentSize = treeSize.get();
//        if (currentSize <= MAX_TREE_SIZE) {
//            return;
//        }
//
//        int numToRemove = currentSize - (int) (MAX_TREE_SIZE * 0.95);
//
//        PriorityQueue<Map.Entry<String, ConcurrentHashMap<Long, PheromoneNode>>> worstNodes =
//                new PriorityQueue<>((a, b) -> {
//                    double maxA = getMaxConcentration(a.getValue());
//                    double maxB = getMaxConcentration(b.getValue());
//                    return Double.compare(maxB, maxA);
//                });
//
//        for (Map.Entry<String, ConcurrentHashMap<Long, PheromoneNode>> entry
//                : pheromoneTree.entrySet()) {
//            worstNodes.offer(entry);
//            if (worstNodes.size() > numToRemove) {
//                worstNodes.poll();
//            }
//        }
//
//        int removedCount = 0;
//        while (!worstNodes.isEmpty()) {
//            if (pheromoneTree.remove(worstNodes.poll().getKey()) != null) {
//                removedCount++;
//            }
//        }
//        treeSize.addAndGet(-removedCount);
//    }
//
//    // פונקציית עזר קטנה לחילוץ הריכוז המקסימלי
//    private double getMaxConcentration(ConcurrentHashMap<Long, PheromoneNode> inner) {
//        if (inner == null || inner.isEmpty()) return 0.0;
//        double max = 0.0;
//        for (PheromoneNode node : inner.values()) {
//            double c = node.getConcentration();
//            if (c > max) max = c;
//        }
//        return max;
//    }
//
//    //  GLOBAL - אחרי איטרציה
//    public void updateGlobalPheromone(String stateKey, int cargoId, int direction,
//                                      double x, double y, double z,
//                                      double x2, double y2, double z2,
//                                      double currentVR, double delta) {
//        ConcurrentHashMap<Long, PheromoneNode> inner =
//                pheromoneTree.computeIfAbsent(stateKey, k -> {
//                    treeSize.incrementAndGet();
//                    return new ConcurrentHashMap<>();
//                });
//
//        long key = nodeKey(cargoId, direction);
//
//        inner.compute(key, (k, existing) -> {
//            if (existing != null) {
//                existing.setConcentration(existing.getConcentration() + delta);
//                // שמור את המיקום עם ה-VR הנמוך ביותר
//                if (currentVR < existing.getVr()) {
//                    existing.setVR(currentVR);
//                    existing.setDirection(direction);
//                    existing.setLeftBackBottom(new double[]{x, y, z});
//                }
//                return existing;
//            } else {
//                return new PheromoneNode(
//                        cargoId, direction,
//                        x, y, z, x2, y2, z2,
//                        currentVR, TAU0 + delta);
//            }
//        });
//    }
//    public double getPheromone(String stateKey, int cargoId, int direction) {
//        var inner = pheromoneTree.get(stateKey);         // O(1)
//        if (inner != null) {
//            PheromoneNode node = inner.get(nodeKey(cargoId, direction));  // O(1)
//            if (node != null) return node.getConcentration();
//        }
//        return TAU0;
//    }
//
//
//    public void depositAndEvaporate(String stateKey, int cargoId,
//                                    Orientation direction, Space space, double currentVR) {
//        var inner = pheromoneTree.computeIfAbsent(stateKey, k -> new ConcurrentHashMap<>());
//        long key = nodeKey(cargoId, direction.id);
//
//        inner.compute(key, (k, existing) -> {
//            if (existing != null) {
//                existing.setConcentration(
//                        (1 - RHO_LOCAL) * existing.getConcentration() + RHO_LOCAL * DELTA_LOCAL);
//                return existing;
//            }
//            return new PheromoneNode(cargoId, direction.id,
//                    space.x, space.y, space.z,
//                    space.x + direction.l, space.y + direction.w, space.z + direction.h,
//                    currentVR, TAU0 + DELTA_LOCAL);
//        });
//    }
//
//
//    public void reset() {
//        pheromoneTree.clear();
//        treeSize.set(0);
//    }
//
//}
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
    int Q = 8;
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
    public void shakeUpPheromones() {
        // פקטור ניעור: נשאיר רק 20% מהפרומון הקיים כדי לשבור את הלופ
        double shakeUpFactor = 0.2;

        // מעבר על כל מפת/עץ הפרומונים שלך:
        // (הנחתי שככה נראית בערך הלולאה אצלך, שנה לפי מבנה הנתונים שלך)
        for (List<PheromoneNode> nodes : pheromoneTree.values()) {
            synchronized (nodes) {
                for (PheromoneNode node : nodes) {
                    double currentPhero = node.getConcentration();
                    // אידוי אגרסיבי במיוחד
                    node.setConcentration(currentPhero * shakeUpFactor);
                }
            }
        }
    }
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

    private void pruneTreeIfNeeded() {
        if (treeSize.get() > MAX_TREE_SIZE) {
            // המטרה: להסיר כ-20% מהצמתים החלשים ביותר כדי לפנות מקום
            int targetSize = (int) (MAX_TREE_SIZE * 0.95);
            int numToRemove = treeSize.get() - targetSize;

            // איסוף כל הערכים מתוך המפה
            List<Map.Entry<String, List<PheromoneNode>>> entries = new ArrayList<>(pheromoneTree.entrySet());

            // מיון הרשימה: המצבים עם ריכוז הפרומונים המקסימלי הנמוך ביותר יופיעו בהתחלה
            entries.sort(Comparator.comparingDouble(entry -> {
                List<PheromoneNode> nodes = entry.getValue();
                if (nodes == null || nodes.isEmpty()) return 0.0;
                synchronized (nodes) {
                    return nodes.stream()
                            .mapToDouble(PheromoneNode::getConcentration)
                            .max()
                            .orElse(0.0);
                }
            }));

            // מחיקת המצבים החלשים ביותר
            int removedCount = 0;
            for (int i = 0; i < entries.size() && removedCount < numToRemove; i++) {
                if (pheromoneTree.remove(entries.get(i).getKey()) != null) {
                    removedCount++;
                }
            }

            // עדכון המונה באופן מדויק
            treeSize.addAndGet(-removedCount);
        }
    }
    //  GLOBAL - אחרי איטרציה
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
    public double getAlpha() { return ALPHA; }
    public double getBeta() { return BETA; }

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
    public void applyGlobalUpdate(LoadingChain bestSolution, double delta) {
        if (bestSolution == null || bestSolution.getSteps() == null) return;

        // עוברים על כל "צעד" (ארגז שהונח) בפתרון הטוב ביותר
        for (LoadingChain.PlacedBoxInfo step : bestSolution.getSteps()) {

            // 1. מחלצים את מילת המצב שהובילה להחלטה הזו
            String stateKey = step.stateKey;

            // 2. מוצאים את הצומת המתאים בעץ הפרומונים
            List<PheromoneNode> nodes = pheromoneTree.get(stateKey);

            if (nodes != null) {
                // 3. מחפשים את הצומת הספציפי שמתייחס לסוג הארגז הזה
                for (PheromoneNode node : nodes) {
                    if (node.getCargoId() == step.cargoType) {
                        // 4. חיזוק הפרומון: מוסיפים את ה-delta
                        double newConcentration = node.getConcentration() + delta;
                        node.setConcentration(newConcentration);
                    }
                }
            }
        }
    }
}
