package com.noa.packer;

import java.util.List;
import java.util.TreeMap;

public class PheromoneManager {
    private TreeMap<String, PheromoneNode> pheromoneTree;

    // פרמטרים לפי Table 2 במאמר [3]
    private final double ALPHA = 1.0;
    private final double BETA = 3.0;
    private final double RHO = 0.7; // מקדם התנדפות קבוע
    private final int Q_BITS = 10;  // 10 ביטים לייצוג כמות לפי משוואה 9 [6]

    public PheromoneManager() {
        this.pheromoneTree = new TreeMap<>();
    }

    // יצירת מילת מצב בינארית (T|Q) עם Padding קבוע ל-10 ביטים [4, 6, 7]
    public String generateStateWord(List<CargoType> allTypes) {
        StringBuilder sb = new StringBuilder();
        for (CargoType type : allTypes) {
            int loadedCount = type.totalQuantity - type.quantity;
            sb.append(loadedCount > 0 ? "1" : "0"); // תחום T

            String binQty = Integer.toBinaryString(loadedCount);
            while (binQty.length() < Q_BITS) binQty = "0" + binQty; // תחום Q
            sb.append(binQty);
        }
        return sb.toString();
    }

    public double getPheromone(String stateKey, int cargoId) {
        PheromoneNode node = pheromoneTree.get(stateKey + "-" + cargoId);
        return (node != null) ? node.concentration : 1.0; // ערך ראשוני [8, 9]
    }

    // עדכון גלובלי הכולל ריכוז פרומון ושמירת הצעד הטוב ביותר לפי VR [4, 10, 11]
    public void updateGlobalPheromone(String stateKey, int cargoId, int direction,
                                      double x, double y, double z,
                                      double currentVR, double vUR) {

        String decisionKey = stateKey + "-" + cargoId;
        PheromoneNode node = pheromoneTree.getOrDefault(decisionKey, new PheromoneNode());

        // נוסחת עדכון: (1-rho)*tau + VUR [11]
        node.concentration = (1 - RHO) * node.concentration + vUR;

        // שמירת הצעד הטוב ביותר למצב זה אם נמצא VR קטן (צפוף) יותר [12, 13]
        if (currentVR < node.bestVR) {
            node.bestCargoId = cargoId;
            node.bestDirection = direction;
            node.bestCoords = new double[]{x, y, z};
            node.bestVR = currentVR;
        }
        pheromoneTree.put(decisionKey, node);
    }

    public static class PheromoneNode {
        double concentration = 1.0;
        int bestCargoId, bestDirection;
        double[] bestCoords;
        double bestVR = Double.MAX_VALUE; // שואפים למינימום מרחב שיורי [14]
    }

    public double getAlpha() { return ALPHA; }
    public double getBeta() { return BETA; }
}