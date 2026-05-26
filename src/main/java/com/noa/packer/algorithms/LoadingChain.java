package com.noa.packer.algorithms;

import com.noa.packer.models.Container;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class LoadingChain {
    // רשימת הארגזים שהונחו בשרשרת זו (משוואה 10 במקורות)
    private List<PlacedBoxInfo> steps = new ArrayList<>();
    private double totalCargoVolume = 0;
    private double containerVolume;
    private ObjectId containerID;

    public LoadingChain(Container container) {
        this.containerID = container.getId();
        this.containerVolume = container.getVolume();
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
        final int Q_BITS = 7; // לדוגמה, מאפשר כמות של עד 1023 יחידות מכל סוג [3]
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

    public void addStep(CargoType cargo, Orientation o, Space s, String stateKey) {
        double[] b = {s.x, s.y, s.z};
        double[] t = {s.x + o.l, s.y + o.w, s.z + o.h};

        // ✅ תיקון: VR צריך להיות סכום ה-residual spaces שנוצרו אחרי הנחת הארגז
        // (Right + Front + Top residuals)
        double vrRight = Math.max(0, s.L - o.l) * s.W * s.H;
        double vrFront = Math.max(0, s.W - o.w) * s.L * s.H;
        double vrTop = Math.max(0, s.H - o.h) * s.L * s.W;
        double vrTotal = vrRight + vrFront + vrTop;

        steps.add(new PlacedBoxInfo(cargo.id, o.id, b, t, vrTotal, stateKey));

        // תיקון חישוב הנפח - הנפח שהתווסף הוא נפח הארגז בפועל
        totalCargoVolume += (o.l * o.w * o.h);
    }
    public List<PlacedBoxInfo> getSteps() {
        return steps;
    }
    public class PlacedBoxInfo {
        public int cargoType; // k
        public int direction; // d
        public double[] leftBackBottom; // xB, yB, zB
        public double[] rightFrontTop;  // xT, yT, zT
        public double remainingSpace;   // VRi
        public String stateKey;

        public PlacedBoxInfo(int k, int d, double[] b, double[] t, double vr, String stateKey) {
            this.cargoType = k;
            this.direction = d;
            this.leftBackBottom = b;
            this.rightFrontTop = t;
            this.remainingSpace = vr;
            this.stateKey = stateKey;
        }
    }
    public ObjectId getContainerID() {
        return containerID;
    }

    public double getTotalCargoVolume() {
        return totalCargoVolume;
    }
}
