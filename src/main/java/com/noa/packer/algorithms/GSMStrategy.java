package com.noa.packer.algorithms;

import ch.qos.logback.core.joran.sanity.Pair;
import java.util.List;
import static java.lang.Math.abs;

public class GSMStrategy {

    // הפונקציה לבחירת מטען
    public static CargoSelection selectCargo(Space currentSpace,
                                      List<CargoType> availableCargoes,
                                      List<LoadingChain.PlacedBoxInfo> placed) {
        CargoType bestCargo = null;
        Orientation bestOri = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (CargoType cargo : availableCargoes) {
            if (cargo.quantity <= 0) continue;

            Orientation ori = OrientationOptimizer.getBestOrientation(currentSpace, cargo);
            if (ori == null) continue;

            double score = calculateVisibility(cargo, ori, currentSpace);

            if (score > bestScore) {
                bestScore = score;
                bestCargo = cargo;
                bestOri = ori;
            }
        }
        if (bestCargo == null) return null;
        return new CargoSelection(bestCargo, bestOri);
    }
    //חישוב ציון לפי נפח
    private static double calculateVisibility(CargoType cargo, Orientation ori, Space space) {
        double usedVolume = ori.l * ori.w * ori.h;
        double spaceVolume = space.L * space.W * space.H;
        return usedVolume / spaceVolume;
    }

//    private double evaluate(Space s, CargoType c, Orientation o,
//                            List<LoadingChain.PlacedBoxInfo> placed) {
//
//        double U = c.getVolume() / s.getVolume();                            // 1. ניצול נפח
//        double D = 1 - (abs(s.L - o.l) + abs(s.W - o.w) + abs(s.H - o.h))    // 2. התאמה גיאומטרית
//                / (s.L + s.W + s.H);
//        double layer = 1 - abs(o.h - s.H) / s.H;                             // 3. התאמת שכבה
//        double contact = estimateContact(s, o, placed);                      // 4. מגע
//
//        double fragmentationPenalty = 0;
//        double remL = s.L % o.l;
//        double remW = s.W % o.w;
//        double remH = s.H % o.h;
//        if (remL > 1 && remL < Global.MIN_DIM) fragmentationPenalty += 0.03;
//        if (remW > 1 && remW < Global.MIN_DIM) fragmentationPenalty += 0.03;
//        if (remH > 1 && remH < Global.MIN_DIM) fragmentationPenalty += 0.03;
//
//        return 0.40 * U + 0.30 * contact + 0.20 * D + 0.10 * layer - fragmentationPenalty;
//    }
//
//    private double estimateContact(Space s, Orientation o,
//                                   List<LoadingChain.PlacedBoxInfo> placed) {
//        double contact = 0;
//
//        double x1 = s.x, y1 = s.y, z1 = s.z;
//        double x2 = x1 + o.l, y2 = y1 + o.w, z2 = z1 + o.h;
//
//        // 1. רצפה
//        if (z1 == 0) {
//            contact += o.l * o.w;
//        }
//
//        // 2. מגע עם ארגזים קיימים
//        for (LoadingChain.PlacedBoxInfo b : placed) {
//            double bx1 = b.leftBackBottom[0], by1 = b.leftBackBottom[1], bz1 = b.leftBackBottom[2];
//            double bx2 = b.rightFrontTop[0], by2 = b.rightFrontTop[1], bz2 = b.rightFrontTop[2];
//
//            boolean touchingTop = Math.abs(z1 - bz2) < Global.EPSILON;
//
//            if (touchingTop) {
//                double overlapX = overlapLength(x1, x2, bx1, bx2);
//                double overlapY = overlapLength(y1, y2, by1, by2);
//
//                if (overlapX > 0 && overlapY > 0) {
//                    contact += overlapX * overlapY;
//                }
//            }
//        }
//
//        // נרמול
//        double maxPossible = 2 * (o.l * o.w + o.l * o.h + o.w * o.h);
//        return contact / Math.max(Global.EPSILON, maxPossible);
//    }
//
//    private double overlapLength(double a1, double a2, double b1, double b2) {
//        return Math.max(0, Math.min(a2, b2) - Math.max(a1, b1));
//    }


}