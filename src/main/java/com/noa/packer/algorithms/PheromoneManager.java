package com.noa.packer.algorithms;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class PheromoneManager {

    private TreeMap<String, List<PheromoneNode>> pheromoneTree;
    // פרמטרים לפי Table 2 במאמר [3]
    private final double ALPHA = 1.0;
    private final double BETA = 3.0;
    private final double RHO = 0.7; // מקדם התנדפות קבוע
    private final double RHO_LOCAL = 0.1;  // Local evaporation
    int Q = 8;
    private final double TAU0 = 0.1;
    private static final int MAX_TREE_SIZE = 50_000;
    private final double DELTA_LOCAL = 0.2;
    public PheromoneManager() {
        this.pheromoneTree = new TreeMap<>();
    }

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

    public void applyGlobalEvaporation() {
        for (List<PheromoneNode> nodes : pheromoneTree.values()) {
            for (PheromoneNode node : nodes) {
                // נידוף
                double decayed = (1 - RHO) * node.getConcentration();
                node.setConcentration(Math.max(decayed, TAU0));
            }
        }
    }
    //  GLOBAL - אחרי איטרציה
    public void updateGlobalPheromone( String stateKey, int cargoId, int direction,
            double x, double y, double z, double x2, double y2, double z2, double currentVR, double delta) {

        List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> new ArrayList<>());
        PheromoneNode node = nodes.stream()
                .filter(n -> n.getCargoId() == cargoId && n.getD() == direction)
                .findFirst()
                .orElse(null);
        if (node != null) {
            // ✅ עדכון כלל ACO: τ = (1 - ρ) × τ_old + Δτ
            node.setConcentration(node.getConcentration() +  delta);
            // שמור מטא-מידע
            if (currentVR < node.getVr()) {
                node.setVR(currentVR);
                node.setDirection(direction);
                node.setLeftBackBottom(new double[]{x, y, z});
            }
        } else {
            // צור node חדש
            PheromoneNode newNode = new PheromoneNode(
                    cargoId, direction, x, y, z, x2, y2, z2, currentVR, TAU0+ delta);
            nodes.add(newNode);
        }
    }
    //  קריאה של פרומון (עם decay)
    public double getPheromone(String stateKey, int cargoId , int direction) {
        List<PheromoneNode> nodes = pheromoneTree.get(stateKey);

        if (nodes != null) {
            for (PheromoneNode node : nodes) {
                if (node.getCargoId() == cargoId && node.getD() == direction) {
                    return node.getConcentration();
                }
            }
        }
        return TAU0; // ערך ראשוני
    }
    public double getAlpha() { return ALPHA; }
    public double getBeta() { return BETA; }

    public void depositAndEvaporate(String stateKey, int cargoId, Orientation direction, Space space, double currentVR) {
        List<PheromoneNode> nodes = pheromoneTree
                .computeIfAbsent(stateKey, k -> new ArrayList<>());
        PheromoneNode node = nodes.stream()
                .filter(n -> n.getCargoId() == cargoId && n.getD() == direction.id)
                .findFirst()
                .orElse(null);

        if (node != null) {
            // שלב 1: deposit — הנמלה משאירה ריח
            node.setConcentration(node.getConcentration() + DELTA_LOCAL);
        } else {
            // node חדש: מתחיל עם TAU0 + deposit
            double initialConcentration = TAU0 + DELTA_LOCAL;
            PheromoneNode newNode = new PheromoneNode(
                    cargoId, direction.id, space.x, space.y, space.z, space.x+direction.l,
                    space.y+direction.w, space.z+direction.h, currentVR, initialConcentration);
            nodes.add(newNode);
        }
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
