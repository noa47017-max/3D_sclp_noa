package com.noa.packer.algorithms;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class PheromoneManager {
    private TreeMap<String, PheromoneNode> pheromoneTree;

    // פרמטרים לפי Table 2 במאמר [3]
    private final double ALPHA = 1.0;
    private final double BETA = 3.0;
    private final double RHO = 0.1; // מקדם התנדפות קבוע
    private final int Q_BITS = 10;  // 10 ביטים לייצוג כמות לפי משוואה 9 [6]
    private static final int MAX_TREE_SIZE = 50_000;

    public PheromoneManager() {
        this.pheromoneTree = new TreeMap<>();
    }


//    public String generateStateWord(List<CargoType> allTypes) {
//        StringBuilder sb = new StringBuilder();
//        for (CargoType type : allTypes) {
//            // T - האם הטענו לפחות אחד?
//            int loadedCount = type.totalQuantity - type.quantity;
//            sb.append(loadedCount > 0 ? "1" : "0");
//
//            // Q המעודכן - מייצג את "מצב המחסן" ב-3 ביטים בלבד (8 רמות)
//            // זה נותן מספיק דיוק כדי להבין מה נגמר, אבל מספיק כללי כדי שנמלים ייפגשו
//            double ratio = (double) type.quantity / type.totalQuantity;
//            int level = (int) (ratio * 7); // הופך לערך בין 0 ל-7
//            String binQty = Integer.toBinaryString(level);
//            while (binQty.length() < 3) binQty = "0" + binQty; // Padding ל-3 ביטים
//
//            sb.append(binQty);
//        }
//        return sb.toString();
//    }
    public String generateStateWord(List<CargoType> allTypes) {
        StringBuilder typeDomain = new StringBuilder();
        StringBuilder quantityDomain = new StringBuilder();

        // Q מוגדר לפי הכמות המקסימלית האפשרית במכולה (למשל 10 ביטים לכל סוג)
        int Q = 10;

        for (CargoType type : allTypes) {
            // 1. בניית ה-Type Domain: האם הסוג נטען לפחות פעם אחת?
            int loadedCount = type.totalQuantity - type.quantity;
            typeDomain.append(loadedCount > 0 ? "1" : "0");

            // 2. בניית ה-Quantity Domain: הכמות המדויקת שנטענה (בבינארי)
            String binQty = Integer.toBinaryString(loadedCount);
            // Padding ל-Q ביטים כפי שנדרש במשוואה 9
            while (binQty.length() < Q) binQty = "0" + binQty;
            quantityDomain.append(binQty);
        }

        // שרשור התחומים לפי איור 6
        return typeDomain.toString() + quantityDomain.toString();
    }
    public double getPheromone(String stateKey, int cargoId) {
        PheromoneNode node = pheromoneTree.get(stateKey + "-" + cargoId);
        return (node != null) ? node.concentration : 1.0; // ערך ראשוני [8, 9]
    }

    // עדכון גלובלי הכולל ריכוז פרומון ושמירת הצעד הטוב ביותר לפי VR [4, 10, 11]
//    public void updateGlobalPheromone(String stateKey, int cargoId, int direction,
//                                      double x, double y, double z,
//                                      double currentVR, double vUR) {
//
//        String decisionKey = stateKey + "-" + cargoId;
//        PheromoneNode node = pheromoneTree.getOrDefault(decisionKey, new PheromoneNode());
//
//        // נוסחת עדכון: (1-rho)*tau + VUR [11]
//        node.concentration = (1 - RHO) * node.concentration + vUR;
//
//        // שמירת הצעד הטוב ביותר למצב זה אם נמצא VR קטן (צפוף) יותר [12, 13]
//        if (currentVR < node.bestVR) {
//            node.bestCargoId = cargoId;
//            node.bestDirection = direction;
//            node.bestCoords = new double[]{x, y, z};
//            node.bestVR = currentVR;
//        }
//        pheromoneTree.put(decisionKey, node);
//    }
    public void updateGlobalPheromone(String stateKey, int cargoId, int direction,
                                      double x, double y, double z,
                                      double currentVR, double vUR) {

        // תוקן: אם ה-tree גדול מדי — מחק את הרשומה הישנה ביותר
        if (pheromoneTree.size() >= MAX_TREE_SIZE) {
            pheromoneTree.pollFirstEntry();
        }
        String decisionKey = stateKey + "-" + cargoId;
        PheromoneNode node = pheromoneTree.getOrDefault(decisionKey, new PheromoneNode());
        // תוקן: וידוא שה-vUR תקין לפני העדכון
        if (Double.isNaN(vUR) || Double.isInfinite(vUR)) vUR = 0.0;
        // נוסחת עדכון: (1-rho)*tau + VUR
        node.concentration = (1 - RHO) * node.concentration + vUR;
        // תוקן: מניעת ריכוז שלילי או אפסי מאוד
        if (node.concentration < 1e-6) node.concentration = 1e-6;
        // שמירת הצעד הטוב ביותר אם ה-VR קטן (צפוף) יותר
        if (currentVR < node.bestVR) {
            node.bestCargoId  = cargoId;
            node.bestDirection = direction;
            node.bestCoords   = new double[]{x, y, z};
            node.bestVR= currentVR;
        }
        pheromoneTree.put(decisionKey, node);
    }
//    public void applyLocalEvaporation(String stateKey, int cargoId) {
//        String decisionKey = stateKey + "-" + cargoId;
//        PheromoneNode node = pheromoneTree.get(decisionKey);
//        if (node != null) {
//            node.concentration = (1 - RHO) * node.concentration + RHO * 1.0; // tau_0 = 1.0
//            if (node.concentration < 1e-6) node.concentration = 1e-6;
//        }
//    }
private final double RHO_LOCAL = 0.1; // נמוך מ-RHO הגלובלי (0.7)

    public void applyLocalEvaporation(String stateKey, int cargoId) {
        String decisionKey = stateKey + "-" + cargoId;
        // תוקן: יצירת node אם לא קיים — כדי לרשום evaporation גם על החלטות חדשות
        PheromoneNode node = pheromoneTree.computeIfAbsent(decisionKey, k -> new PheromoneNode());
        // תוקן: שימוש ב-RHO_LOCAL נמוך — evaporation עדין שלא הורס מה שנלמד
        node.concentration = (1 - RHO_LOCAL) * node.concentration + RHO_LOCAL * 1.0;
        if (node.concentration < 1e-6) node.concentration = 1e-6;
    }
    public void partialReset(double fraction) {
        Random rnd = new Random();
        for (PheromoneNode node : pheromoneTree.values()) {
            if (rnd.nextDouble() < fraction) {
                node.concentration = 1.0; // חזרה לערך ברירת מחדל
            }
        }
    }
    public static class PheromoneNode {
        double concentration;
        int bestCargoId, bestDirection;
        double[] bestCoords;
        double bestVR = Double.MAX_VALUE; // שואפים למינימום מרחב שיורי [14]
    }

    public double getAlpha() { return ALPHA; }
    public double getBeta() { return BETA; }
}