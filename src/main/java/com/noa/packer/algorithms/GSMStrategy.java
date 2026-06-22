package com.noa.packer.algorithms;

import ch.qos.logback.core.joran.sanity.Pair;

import java.util.List;

import static java.lang.Math.abs;

public class GSMStrategy {
    /**
     * בחירת הארגז עם הנפח הגדול ביותר מתוך רשימת הארגזים הזמינים [5]
     */
    public static class Pair<A, B> {

        public final A first;

        public final B second;
        public Pair(A a, B b) {
            this.first = a;
            this.second = b;
        }

        public static <A, B> Pair<A, B> of(A a, B b) {
            return new Pair<>(a, b);
        }

    }

    private static final double EPS = 1e-9;
    final OrientationOptimizer orientationOptimizer = new OrientationOptimizer(); // שדה במחלקה

    private static final double MIN_VOLUME_THRESHOLD = 1e-6;
// אם אין לך ערך גלובלי, קבעי את maxCargoVolume לפי המקרה (או חשבי דינמית)

    private static final double maxCargoVolume = 1000.0; //
    public Pair<CargoType, Orientation> selectCargo1( Space currentSpace,
                                                     List<CargoType> availableCargoes,
                                                     PheromoneManager pm,
                                                     List<LoadingChain.PlacedBoxInfo> placed) {
        CargoType bestCargo = null;
        double maxVolume = -1.0;

        for (CargoType cargo : availableCargoes) {
            // בדיקה אם הארגז בכלל יכול להיכנס למרחב הנוכחי (לפחות באחד מ-6 כיוונים)
            if (cargo.quantity>0 && cargo.canFit(currentSpace)) {
                // כלל חמדן: בחירת הנפח הגדול ביותר [4]
                if (cargo.getVolume() > maxVolume) {
                    maxVolume = cargo.getVolume();
                    bestCargo = cargo;
                }
            }
        }
        Orientation bestOri = orientationOptimizer.getBestOrientation(currentSpace, bestCargo);
        String stateKey = currentCargoState(availableCargoes, pm);
        pm.depositAndEvaporate(stateKey, bestCargo.id, bestOri.id);
        return Pair.of(bestCargo, bestOri);
    }
private String currentCargoState(List<CargoType> allTypes, PheromoneManager pm) {
    // פשוט קורא למנהל הפרומונים כדי לייצר את מילת המצב
    return pm.generateStateWord(allTypes);
}

    // -----------------------------
// גרסה מתוקנת של selectCargo (חתימה מעט שונה)
// שימי לב: הוספתי פרמטר allTypes כדי ליצור stateKey
// -----------------------------
    public Pair<CargoType, Orientation> selectCargo(
            Space currentSpace,
            List<CargoType> availableCargoes,
            PheromoneManager pm,
            List<LoadingChain.PlacedBoxInfo> placed
            ) {
        double localDeposit= 0.005;
        CargoType bestCargo = null;
        Orientation bestOri = null;

        // אם שכבה חדשה: בחר לפי נפח מקסימלי (GSM לפי המאמר)
//        if (isNewLayer) {
//            double bestVol = -1;
//            for (CargoType c : availableCargoes) {
//                if (c.quantity <= 0) continue;
//                // קבל את האוריינטציה הטובה ביותר עבור החלל (אם יש לך OrientationOptimizer)
//                Orientation ori = new OrientationOptimizer().getBestOrientation(currentSpace, c);
//                if (ori == null) continue;
//                double vol = c.getVolume();
//                if (vol > bestVol) {
//                    bestVol = vol;
//                    bestCargo = c;
//                    bestOri = ori;
//                }
//            }
//        } else {
            // בתוך אותה שכבה: דירוג לפי localFillScore + נפח (נמוך = טוב)
            double wArea = 0.6, wVol = 0.4;
            double bestScore = Double.NEGATIVE_INFINITY;;

            for (CargoType c : availableCargoes) {
                if (c.quantity <= 0) continue;
                Orientation ori = orientationOptimizer.getBestOrientation(currentSpace, c);

                if (ori == null) continue;
                double local = localFillScore(c, ori, currentSpace); // נמוך = טוב
                double volNorm = 1.0 - (c.getVolume() / Math.max(EPS, currentSpace.getVolume())); // נורמליזציה
//                double score = wArea * local + wVol * volNorm;
                double score = evaluate(currentSpace, c, ori, placed);

                if (score > bestScore) {
                    bestScore = score;
                    bestCargo = c;
                    bestOri = ori;
                }
            }
//        }

        if (bestCargo == null) return null;

        // עדכון פרומון מקומי מיד אחרי בחירה (GSM תורם ל‑ASM)
        String stateKey = currentCargoState(availableCargoes, pm);
        pm.depositAndEvaporate(stateKey, bestCargo.id, bestOri.id);


        return Pair.of(bestCargo, bestOri);
    }
    private double evaluate(Space s, CargoType c, Orientation o,
                            List<LoadingChain.PlacedBoxInfo> placed) {

        double U = c.getVolume() / s.getVolume();                            // 1. ניצול נפח
        double D = 1 - (abs(s.L - o.l) + abs(s.W - o.w) + abs(s.H - o.h))  // 2. התאמה גיאומטרית
                / (s.L + s.W + s.H);
        double layer = 1 - abs(o.h - s.H) / s.H;                            // 3. התאמת שכבה
        double contact = estimateContact(s, o, placed);                      // 4. מגע

        // ===== חדש: פנליטי ריכוז =====
        // אם כבר השתמשנו ביותר מ-60% מהכמות של סוג זה, פנליטי
        // זה מעודד את האלגוריתם לשמור ארגזים גדולים לשכבות מאוחרות


        // פנליטי פרגמנטציה (כבר קיים)
        double fragmentationPenalty = 0;
        double remL = s.L % o.l;
        double remW = s.W % o.w;
        double remH = s.H % o.h;
        if (remL > 1 && remL < Global.MIN_DIM) fragmentationPenalty += 0.03;
        if (remW > 1 && remW < Global.MIN_DIM) fragmentationPenalty += 0.03;
        if (remH > 1 && remH < Global.MIN_DIM) fragmentationPenalty += 0.03;

        return 0.40 * U +
                0.30 * contact +
                0.20 * D +
                0.10 * layer - fragmentationPenalty ;
    }
    private double estimateContact(
            Space s,
            Orientation o,
            List<LoadingChain.PlacedBoxInfo> placed
    ) {

        double contact = 0;

        // מיקום הארגז החדש
        double x1 = s.x;
        double y1 = s.y;
        double z1 = s.z;

        double x2 = x1 + o.l;
        double y2 = y1 + o.w;
        double z2 = z1 + o.h;

        // =========================
        // 1. רצפה
        // =========================
        if (z1 == 0) {
            contact += o.l * o.w;
        }

        // =========================
        // 2. מגע עם ארגזים קיימים
        // =========================
        for (LoadingChain.PlacedBoxInfo b : placed) {

            double bx1 = b.leftBackBottom[0];
            double by1 = b.leftBackBottom[1];
            double bz1 = b.leftBackBottom[2];

            double bx2 = b.rightFrontTop[0];
            double by2 = b.rightFrontTop[1];
            double bz2 = b.rightFrontTop[2];

            // =====================================
            // הארגז החדש יושב על הארגז הקיים
            // =====================================
            boolean touchingTop =
                    Math.abs(z1 - bz2) < EPS;

            if (touchingTop) {

                double overlapX =
                        overlapLength(x1, x2, bx1, bx2);

                double overlapY =
                        overlapLength(y1, y2, by1, by2);

                if (overlapX > 0 && overlapY > 0) {

                    contact += overlapX * overlapY;
                }
            }
        }

        // =========================
        // נרמול
        // =========================
        double maxPossible =
                2 * (o.l * o.w +
                        o.l * o.h +
                        o.w * o.h);

        return contact / Math.max(EPS, maxPossible);
    }
    private double overlapLength(
            double a1, double a2,
            double b1, double b2
    ) {
        return Math.max(
                0,
                Math.min(a2, b2) - Math.max(a1, b1)
        );
    }
    private String makePheromoneKey(String stateKey, int cargoId, Orientation ori, Space s) {
        int rx = (int)Math.round(s.x);
        int ry = (int)Math.round(s.y);
        int rz = (int)Math.round(s.z);
        return stateKey + "|" + cargoId + "|" + ori.toShortString() + "@" + rx + "," + ry + "," + rz;
    }
    private double localFillScore(CargoType c, Orientation ori, Space s) {
        // קבל מידות הקופסה לפי האוריינטציה
        double boxL = ori.l;
        double boxW = ori.w;
        double boxH = ori.h;

        // מידות החלל
        double spaceY = s.W;   // רוחב (y)
        double spaceZ = s.H;  // גובה (z)

        // שאריות אחרי הנחה
        double remY = spaceY %boxW;
        double remZ = spaceZ % boxH;

        // נורמליזציה
        double scoreY = remY / Math.max(EPS, spaceY);
        double scoreZ = remZ / Math.max(EPS, spaceZ);

        // משקלות לפי נוסחה 8: אפשר לכוונן
        double wY = 0.5;
        double wZ = 0.5;

        // אפשר לשקלל גם לפי גודל השארית הממשי (כפי שהמאמר מציע)
        double result = wY * scoreY * boxH + wZ * scoreZ * boxW;

        return result;
    }
    public CargoType selectCargo7(Space currentSpace, List<CargoType> availableCargoes) {
        CargoType bestCargo = null;
        double maxArea = -1.0;

        for (CargoType cargo : availableCargoes) {
            if (cargo.quantity > 0 && cargo.canFit(currentSpace)) {
                // ✅ בחר לפי base area (L×W) בלבד - זה ממלא שכבות וממונע "ארגזים באוויר"
                // זה עוזר ליצור שכבות יציבות ושטוחות ללא חורים
                double bestAreaForCargo = getBestBaseArea(cargo, currentSpace);
                if (bestAreaForCargo > maxArea) {
                    // ✅ תיקון חשוב: עדכן את base area, לא volume!
                    maxArea = bestAreaForCargo;
                    bestCargo = cargo;
                }
            }
        }
        return bestCargo;
    }

    private double getBestBaseArea(CargoType cargo, Space space) {
        double best = -1.0;

        // כל 6 האוריינטציות: (l,w,h), (l,h,w), (w,l,h), (w,h,l), (h,l,w), (h,w,l)
        double[][] orientations = {
                {cargo.l, cargo.w, cargo.h},
                {cargo.l, cargo.h, cargo.w},
                {cargo.w, cargo.l, cargo.h},
                {cargo.w, cargo.h, cargo.l},
                {cargo.h, cargo.l, cargo.w},
                {cargo.h, cargo.w, cargo.l}
        };

        for (double[] ori : orientations) {
            double oL = ori[0], oW = ori[1], oH = ori[2];
            // בדיקה שהאוריינטציה נכנסת פיזית לחלל
            if (oL <= space.L + 1e-9 && oW <= space.W + 1e-9 && oH <= space.H + 1e-9) {
                double area = oL * oW;
                if (area > best) best = area;
            }
        }
        return best;
    }
    public void selectCargo3(
            Space currentSpace,
            CargoType c, boolean isNewLayer,
            PheromoneManager pm,
            double layerHeight
    ) {

      double  U = c.getVolume() / currentSpace.getVolume();
//      double  A = 1 - abs(c.l - layerHeight) / layerHeight;
      double  D = 1 - (abs(currentSpace.L-c.l) + abs(currentSpace.W-c.w) + abs(currentSpace.H-c.h)) / (currentSpace.L+currentSpace.W+currentSpace.H);

    }

}
