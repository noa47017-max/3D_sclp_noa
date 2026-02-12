package com.noa.packer;
import java.util.ArrayList;
import java.util.List;

public class LoadingChain {
    // רשימת הארגזים שהונחו בשרשרת זו (משוואה 10 במקורות)
    private List<PlacedBoxInfo> steps = new ArrayList<>();
    private double totalCargoVolume = 0;
    private double containerVolume;

    public LoadingChain(double containerVolume) {
        this.containerVolume = containerVolume;
    }

    // הוספת ארגז לשרשרת ועדכון הנפח (Step 4 ב-GSM/ASM)


    // חישוב VUR - Volume Utilization Rate (משוואה 4 במקורות)
    public double getVUR() {
        return totalCargoVolume / containerVolume;
    }

    // יצירת מילת המצב (Status Word) לצורך גיזום (משוואה 9)
    // מורכב מסוגי הארגזים וכמותם הנוכחית בשרשרת
    public String generateStateWord(List<CargoType> allCargoTypes) {
        // אורך מילת המצב הוא K * (1 + Q) ביטים [2]
        // K הוא מספר סוגי המטענים, Q הוא מספר הביטים המוקצה לכמות [3, 4]
        final int Q_BITS = 10; // לדוגמה, מאפשר כמות של עד 1023 יחידות מכל סוג [3]
        StringBuilder state = new StringBuilder();

        for (CargoType type : allCargoTypes) {
            // שלב 1: קביעת דגל תחום הסוגים (Type Domain - T) [2, 3]
            // tk = 1 אם הוטען לפחות ארגז אחד מסוג זה, אחרת 0 [3]
            int loadedQuantity = type.totalQuantity - type.quantity;
            state.append(loadedQuantity > 0 ? "1" : "0");

            // שלב 2: קידוד תחום הכמויות (Quantity Domain - Q) [2, 3]
            // המרת הכמות שהוטענה לקוד בינארי באורך Q [3]
            String binaryQty = Integer.toBinaryString(loadedQuantity);

            // הוספת אפסים מובילים (Padding) כדי להבטיח אורך קבוע לכל מפתח בעץ [2]
            while (binaryQty.length() < Q_BITS) {
                binaryQty = "0" + binaryQty;
            }
            state.append(binaryQty);
        }

        return state.toString();
    }
    public void addStep(CargoType cargo, Orientation o, Space s) {
        double[] b = {s.x, s.y, s.z};
        double[] t = {s.x + o.l, s.y + o.w, s.z + o.h};
        steps.add(new PlacedBoxInfo(cargo.id, o.id, b, t, s.getVolume()));

        // תיקון חישוב הנפח:
        totalCargoVolume += (o.l * o.w * o.h);
    }
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < steps.size(); i++) {
            PlacedBoxInfo step = steps.get(i);
            json.append("  {")
                    .append("\"typeId\": ").append(step.cargoType).append(", ")
                    .append("\"xB\": ").append(step.leftBackBottom[0]).append(", ")
                    .append("\"yB\": ").append(step.leftBackBottom[1]).append(", ")
                    .append("\"zB\": ").append(step.leftBackBottom[2]).append(", ")
                    .append("\"xT\": ").append(step.rightFrontTop[0]).append(", ")
                    .append("\"yT\": ").append(step.rightFrontTop[1]).append(", ")
                    .append("\"zT\": ").append(step.rightFrontTop[2])
                    .append("}");
            if (i < steps.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        return json.toString();
    }
    public List<PlacedBoxInfo> getSteps() {
        return steps;
    }
    // מחלקת עזר פנימית לשמירת נתוני כל ארגז (משוואה 10)
    public static class PlacedBoxInfo {
        public int cargoType; // k
        public int direction; // d
        public double[] leftBackBottom; // xB, yB, zB
        public double[] rightFrontTop;  // xT, yT, zT
        public double remainingSpace;   // VRi

        public PlacedBoxInfo(int k, int d, double[] b, double[] t, double vr) {
            this.cargoType = k;
            this.direction = d;
            this.leftBackBottom = b;
            this.rightFrontTop = t;
            this.remainingSpace = vr;
        }
    }
}
