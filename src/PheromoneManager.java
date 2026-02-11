import java.util.TreeMap;
import java.util.List;

public class PheromoneManager {

    // עץ אדום-שחור לאחסון ריכוז פרומונים לפי מצב המערכת
    private TreeMap<String, PheromoneNode> pheromoneTree;

    // פרמטרים היוריסטיים
    private final double alpha = 1.0;
    private final double beta = 3.0;
    private final double rho = 0.7;
    private final int Q_BITS = 10;

    public PheromoneManager() {
        this.pheromoneTree = new TreeMap<>();
    }

    /**
     * יצירת מילת מצב בינארית המייצגת אילו ארגזים נטענו ובאיזו כמות.
     * זה משמש כמפתח הייחודי בעץ הפרומונים.
     */
    public String generateStateWord(List<CargoType> types) {
        StringBuilder sb = new StringBuilder();
        for (CargoType type : types) {
            // האם הסוג הזה נטען לפחות פעם אחת?
            int loadedCount = type.totalQuantity - type.quantity;
            sb.append(loadedCount > 0 ? "1" : "0");

            // המרת הכמות שנטענה לקוד בינארי באורך קבוע
            String binQty = Integer.toBinaryString(loadedCount);
            while (binQty.length() < Q_BITS) {
                binQty = "0" + binQty;
            }
            sb.append(binQty);
        }
        return sb.toString();
    }

    /**
     * שליפת ריכוז הפרומונים עבור מצב מסוים.
     */
    public double getPheromone(String stateKey) {
        // אם המצב לא קיים בעץ, נחזיר ערך התחלתי של 1.0
        PheromoneNode node = pheromoneTree.get(stateKey);
        return (node != null) ? node.concentration : 1.0;
    }

    /**
     * עדכון הפרומונים בסוף איטרציה (התנדבות + תוספת חדשה).
     */
    public void updatePheromones(String stateKey, double delta) {
        PheromoneNode node = pheromoneTree.getOrDefault(stateKey, new PheromoneNode());

        // נוסחת עדכון הריכוז: (1-rho) * ריכוז_ישן + דלתא (ה-VUR שהושג)
        node.concentration = (1 - rho) * node.concentration + delta;

        pheromoneTree.put(stateKey, node);
    }
    public void updateGlobalPheromones(String stateKey, double currentVUR, double rho) {
        // שליפת הצומת המתאים בעץ האדום-שחור [6]
        PheromoneNode node = pheromoneTree.getOrDefault(stateKey, new PheromoneNode());

        /**
         * יישום משוואה 10 מהמאמר:
         * (1 - rho) מייצג את מה שנשאר מהפרומון הישן לאחר ההתנדפות.
         * currentVUR הוא ה"דלתא" (המידע החדש מהנמלה הנוכחית).
         */
        node.concentration = (1 - rho) * node.concentration + currentVUR;

        // עדכון העץ בערך החדש
        pheromoneTree.put(stateKey, node);
    }

    /**
     * מחלקת עזר לייצוג הנתונים המאוחסנים בעץ.
     */
    public static class PheromoneNode {
        double concentration = 1.0;
        // ניתן להוסיף כאן את ה-bestStep אם תרצה לשמור את הצעד האופטימלי ספציפית
    }

    // Getters עבור ה-ASMStrategy
    public double getAlpha() { return alpha; }
    public double getBeta() { return beta; }
}








