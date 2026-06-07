package com.noa.packer.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class PheromoneManager {

    private TreeMap<String, List<PheromoneNode>> pheromoneTree;
    // פרמטרים לפי Table 2 במאמר [3]
    private final double ALPHA = 1.0;
    private final double BETA = 3.0;
    private final double RHO = 0.3; // מקדם התנדפות קבוע
    private final double RHO_LOCAL = 0.1;  // Local evaporation
    int Q = 7;
    private final double TAU0 = 1.0;
    private static final int MAX_TREE_SIZE = 50_000;

    public PheromoneManager() {
        this.pheromoneTree = new TreeMap<>();
    }

    public String generateStateWord(List<CargoType> allTypes) {
        StringBuilder typeDomain = new StringBuilder();
        StringBuilder quantityDomain = new StringBuilder();

        // Q מוגדר לפי הכמות המקסימלית האפשרית במכולה (למשל 10 ביטים לכל סוג)

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


    // ✅ LOCAL - בזמן הטעינה
    public void applyLocalEvaporation(String stateKey, int cargoId) {
        List<PheromoneNode> nodes = pheromoneTree
                .computeIfAbsent(stateKey, k -> new ArrayList<>());

        PheromoneNode node = nodes.stream()
                .filter(n -> n.getCargoId() == cargoId)
                .findFirst()
                .orElse(null);

        if (node != null) {
            // τ = (1 - ρ_local) × τ + ρ_local × τ_0
            node.setConcentration(
                   node.getConcentration() + RHO_LOCAL * 1.0
            );
        } else {
            // צור node חדש עם ערך ברירת מחדל
            PheromoneNode newNode = new PheromoneNode(
                    cargoId, 0, 0, 0, 0, 0, 0, 0, 1.0, 0,  TAU0
            );
            nodes.add(newNode);
        }
    }
    public void applyGlobalEvaporation() {
        for (List<PheromoneNode> nodes : pheromoneTree.values()) {
            for (PheromoneNode node : nodes) {
                // ✅ נדף מ-KOL NOD
               node.setConcentration((1 - RHO) * node.getConcentration() + RHO * TAU0);

            }
        }
    }
    // ✅ GLOBAL - אחרי איטרציה
    public void updateGlobalPheromone( String stateKey, int cargoId, int direction,
            double x, double y, double z,
            double currentVR, double delta, int ta) {

        List<PheromoneNode> nodes = pheromoneTree
                .computeIfAbsent(stateKey, k -> new ArrayList<>());

        PheromoneNode node = nodes.stream()
                .filter(n -> n.getCargoId() == cargoId && n.getD() == direction)
                .findFirst()
                .orElse(null);

        if (node != null) {
            // ✅ עדכון כלל ACO: τ = (1 - ρ) × τ_old + Δτ
            node.setConcentration(node.getConcentration() +  delta);
            node.setTa(ta);

            // שמור מטא-מידע
            if (currentVR < node.getVr()) {
                node.setVR(currentVR);
                node.setDirection(direction);
                node.setLeftBackBottom(new double[]{x, y, z});
            }
        } else {
            // צור node חדש
            PheromoneNode newNode = new PheromoneNode(
                    cargoId, direction, x, y, z, x, y, z, currentVR, ta, TAU0+ delta);
            nodes.add(newNode);
        }


    }

    // ✅ קריאה של פרומון (עם decay)
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
    // בתוך PheromoneManager.java
    public void updateLocalPheromone(String stateKey, int cargoId) {
        List<PheromoneNode> nodes = pheromoneTree.get(stateKey);
        if (nodes == null) return;

        PheromoneNode node = nodes.stream()
                .filter(n -> n.getCargoId() == cargoId)
                .findFirst()
                .orElse(null);

        if (node != null) {
            // עדכון מקומי: אנחנו מחלישים את הפרומון בנתיב הזה כדי שיהיה פחות אטרקטיבי
            // לנמלה הבאה, וזה מאלץ אותה לחפש נתיבים חדשים (Exploration)
            double current = node.getConcentration();
            node.setConcentration(current * (1 - RHO_LOCAL));
        }
    }
    /**
     * עדכון גלובלי - מחזק את הפרומונים לאורך המסלול של הפתרון הכי טוב.
     * @param bestSolution הפתרון הטוב ביותר שנמצא באיטרציה הנוכחית
     * @param delta כמות הפרומון להוסיף (למשל: Q * VUR)
     *//**
     *  * עדכון/הוספת פרומון מקומי מיד אחרי הנחה (GSM/ASM).
     *  * אם קיים node עבור stateKey+cargoId — מעדכן את הריכוז; אחרת יוצר node חדש.
     *  */


//    public void depositLocalPheromone(String stateKey,
//     int cargoId,
//     Orientation ori,
//     double x, double y, double z,
//     double amount) {
//
//     List<PheromoneNode> nodes = pheromoneTree.computeIfAbsent(stateKey, k -> new ArrayList<>());
//
//       // חפש node קיים שמתאים לסוג המטען + אותה אוריינטציה + מיקום מעוגל
//     PheromoneNode node = nodes.stream()
//                             .filter(n -> n.getCargoId() == cargoId
//                 && ori.toShortString().equals(n.getOri()) // אם שמרת שדה ori במבנה
//                 && Math.round(n.getLeftBackBottom()[0]) == Math.round(x)
//                 && Math.round(n.getLeftBackBottom()[1]) == Math.round(y)
//                 && Math.round(n.getLeftBackBottom()[2]) == Math.round(z))
//                             .findFirst()
//                .orElse(null);
//
//     if (node != null) {
//              // עדכון ריכוז מקומי: מוסיפים כמות קטנה (או ממזגים)
//     double newConc = node.getConcentration() + amount;
//     node.setConcentration(newConc);
//              // עדכן מטא-מידע אם צריך (שמור ta/vr אם רלוונטי)
//     } else {
//              // צור node חדש עם שדות בסיסיים
//     *PheromoneNode newNode = new PheromoneNode(
//                    cargoId,
//     0,              // direction (אם אין מספר כיוון, אפשר לשים 0 או להמיר ori ל-int)
//    x, y, z,        // leftBackBottom
//     x, y, z,        // some other coords placeholders (אם יש שדות אחרים)
//     1.0,            // vr (initial)
//    0,              // ta (iteration index) - caller יכול לעדכן מאוחר יותר
//     amount          // concentration initial
//                            );
//          // אם PheromoneNode תומך בשמירת אוריינטציה, הוסיפי:
//    newNode.setOri(ori.toShortString());
//     nodes.add(newNode);
//
//              // שמירה על גודל עץ סביר (מניעת זליגת זיכרון)
//    if (nodes.size() > MAX_TREE_SIZE) {
//                  // אסטרטגיה פשוטה: הסר את הישן ביותר (או לפי מדד)
//     nodes.remove(0);
//    }
//     }
//     }
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
