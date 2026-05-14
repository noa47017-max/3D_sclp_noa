package com.noa.packer.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PheromoneManager {

    private TreeMap<String, List<PheromoneNode>> pheromoneTree;
    // פרמטרים לפי Table 2 במאמר [3]
    private final double ALPHA = 1.0;
    private final double BETA = 3.0;
    private final double RHO = 0.7; // מקדם התנדפות קבוע
    private final double RHO = 0.15; // הופחת מ-0.7 כדי לאפשר למידה לטווח ארוך יותר
    private static final int MAX_TREE_SIZE = 50_000;

    public PheromoneManager() {
        this.pheromoneTree = new TreeMap<>();
    }

    public String generateStateWord(List<CargoType> allTypes) {
        StringBuilder typeDomain = new StringBuilder();
        StringBuilder quantityDomain = new StringBuilder();

        // Q מוגדר לפי הכמות המקסימלית האפשרית במכולה (למשל 10 ביטים לכל סוג)
        int Q = 7;

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
    public double getPheromone(String stateKey, int cargoId, int currentIteration) {
       List<PheromoneNode> nodes = pheromoneTree.get(stateKey); 
       if (nodes != null) {
     //    List<PheromoneNode> filteredList = nodes.stream()
     //    .filter(node -> node.getCargoId().equals(cargoId))
     //    .collect(Collectors.toList());
        PheromoneNode firstMatch = nodes.stream()
        .filter(node -> node.getCargoId()==cargoId).findFirst().orElse(null); 
        PheromoneNode firstMatch = null;
        // שימוש בלולאה רגילה במקום Stream לשיפור ביצועים באלגוריתם איטרטיבי
        for (PheromoneNode node : nodes) {
            if (node.getCargoId() == cargoId) {
                firstMatch = node;
                break;
            }
        }
        if (firstMatch != null) {
         int iterationsPassed = currentIteration - firstMatch.getTa();
         firstMatch.setTa(currentIteration); // עדכון זמן ה"טא" של הצומת כדי למדוד התנדxxx עתידית
          double decayFactor = Math.pow(1 - RHO, iterationsPassed);
           return Math.max(firstMatch.getConcentration() * decayFactor, 1e-6);        }
    }
    return 1.0; // ערך ראשוני [8, 9]
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
                                      double x, double y, double z, double currentVR, double vUR,int ta) {
        // תוקן: אם ה-tree גדול מדי — מחק את הרשומה הישנה ביותר
          if (pheromoneTree.size() >= MAX_TREE_SIZE) {
    String weakestKey = null;
    double lowestAvg = Double.MAX_VALUE;
        if (pheromoneTree.size() >= MAX_TREE_SIZE) {
            String weakestKey = null;
            double lowestAvg = Double.MAX_VALUE;
            for (Map.Entry<String, List<PheromoneNode>> entry : pheromoneTree.entrySet()) {
                double sum = 0;
                for (PheromoneNode n : entry.getValue()) sum += n.getConcentration();
                double avg = sum / entry.getValue().size();
                if (avg < lowestAvg) {
                    lowestAvg = avg;
                    weakestKey = entry.getKey();
                }
            }
            if (weakestKey != null) pheromoneTree.remove(weakestKey);
        }

    for (Map.Entry<String, List<PheromoneNode>> entry : pheromoneTree.entrySet()) {
        double avg = 0;
        List<PheromoneNode> nodes = entry.getValue();
        List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> new ArrayList<>());
        PheromoneNode node = null;
        for (PheromoneNode n : nodes) {
            avg += n.getConcentration();
            if (n.getCargoId() == cargoId) { node = n; break; }
        }
        avg = avg / nodes.size();

        if (avg < lowestAvg) {
            lowestAvg = avg;
            weakestKey = entry.getKey();
        }
    }

    if (weakestKey != null) {
        pheromoneTree.remove(weakestKey);
    }

}
        List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> new ArrayList<>());   
        if (nodes != null) {
     //    List<PheromoneNode> filteredList = nodes.stream()
     //    .filter(node -> node.getCargoId().equals(cargoId))
     //    .collect(Collectors.toList());
        PheromoneNode node = nodes.stream().filter(n -> n.getCargoId()==cargoId).findFirst().orElse(null); 
        if (node != null) {
            if (currentVR < node.getVr()) {
            node.setDirection(direction) ;
            node.setLeftBackBottom(new double[]{x, y, z});
            node.setVR(currentVR); ;
            }
        }
        else {
            node = new PheromoneNode(cargoId, direction, x, y, z, x, y, z, currentVR, ta,0);
            nodes.add(node);
        }

        // תוקן: וידוא שה-vUR תקין לפני העדכון
        if (Double.isNaN(vUR) || Double.isInfinite(vUR)) vUR = 0.0;
        // נוסחת עדכון: (1-rho)*tau + VUR 
        node.setConcentration((node.getConcentration())*(Math.pow(1-RHO,ta-node.getTa())) + vUR);
        node.setTa(ta);
        // תוקן: מניעת ריכוז שלילי או אפסי מאוד
        if (node.getConcentration() < 1e-6) node.setConcentration( 1e-6);
    }
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
         List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> new ArrayList<>());   
        if (nodes != null) {
     //    List<PheromoneNode> filteredList = nodes.stream()
     //    .filter(node -> node.getCargoId().equals(cargoId))
     //    .collect(Collectors.toList());
        PheromoneNode node = nodes.stream().filter(n -> n.getCargoId()==cargoId).findFirst().orElse(null); 
        if (node != null) {        // תוקן: שימוש ב-RHO_LOCAL נמוך — evaporation עדין שלא הורס מה שנלמד
           node.concentration = (1 - RHO_LOCAL) * node.concentration + RHO_LOCAL * 1.0;
           if (node.concentration < 1e-6) node.concentration = 1e-6;
        }
        else {
            node = new PheromoneNode(cargoId, 0, 0, 0, 0, 0, 0, 0, 1.0, 0,1.0);
            nodes.add(node);
        }
    }
}
    public void partialReset(double fraction) {
        Random rnd = new Random();
        for (List<PheromoneNode> nodes : pheromoneTree.values()) {
            for (PheromoneNode node : nodes) {
                if (rnd.nextDouble() < fraction) {
                    node.concentration = 1.0; // חזרה לערך ברירת מחדל
                }
            }
        }
    }
    // public static class PheromoneNode {
    //     double concentration;
    //     int bestCargoId, bestDirection;
    //     double[] bestCoords;
    //     double bestVR = Double.MAX_VALUE; // שואפים למינימום מרחב שיורי [14]
    //     public Object getCargoId() {
    //         // TODO Auto-generated method stub
    //         throw new UnsupportedOperationException("Unimplemented method 'getCargoId'");
    //     }
    // }

    public double getAlpha() { return ALPHA; }
    public double getBeta() { return BETA; }

  

    public void put(String stateKey, PheromoneNode newValue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'put'");
    }
}