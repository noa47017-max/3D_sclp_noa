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
    public String generateStateWord() {
        // המאמר מציין קוד בינארי המייצג T (סוגים) ו-Q (כמויות)
        // במימוש Java נוכל להשתמש במחרוזת ייחודית כמפתח לעץ
        StringBuilder state = new StringBuilder();
        // לוגיקה לקידוד T ו-Q לפי רשימת ה-steps
        return state.toString();
    }
    public void addStep(CargoType cargo, Orientation o, Space s) {
        double[] b = {s.x, s.y, s.z};
        double[] t = {s.x + o.l, s.y + o.w, s.z + o.h};
        steps.add(new PlacedBoxInfo(cargo.id, o.id, b, t, s.getVolume()));

        // תיקון חישוב הנפח:
        totalCargoVolume += (o.l * o.w * o.h);
    }
    public List<PlacedBoxInfo> getSteps() {
        return steps;
    }
    // מחלקת עזר פנימית לשמירת נתוני כל ארגז (משוואה 10)
    public static class PlacedBoxInfo {
        int cargoType; // k
        int direction; // d
        double[] leftBackBottom; // xB, yB, zB
        double[] rightFrontTop;  // xT, yT, zT
        double remainingSpace;   // VRi

        public PlacedBoxInfo(int k, int d, double[] b, double[] t, double vr) {
            this.cargoType = k;
            this.direction = d;
            this.leftBackBottom = b;
            this.rightFrontTop = t;
            this.remainingSpace = vr;
        }
    }
}
