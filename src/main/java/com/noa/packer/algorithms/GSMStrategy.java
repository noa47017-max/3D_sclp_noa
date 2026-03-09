package com.noa.packer.algorithms;

import java.util.List;
public class GSMStrategy {
    /**
     * בחירת הארגז עם הנפח הגדול ביותר מתוך רשימת הארגזים הזמינים [5]
     */
//    public CargoType selectCargo(Space currentSpace, List<CargoType> availableCargoes) {
//        CargoType bestCargo = null;
//        double maxVolume = -1.0;
//
//        for (CargoType cargo : availableCargoes) {
//            // בדיקה אם הארגז בכלל יכול להיכנס למרחב הנוכחי (לפחות באחד מ-6 כיוונים)
//            if (cargo.quantity>0 && cargo.canFit(currentSpace)) {
//                // כלל חמדן: בחירת הנפח הגדול ביותר [4]
//                if (cargo.getVolume() > maxVolume) {
//                    maxVolume = cargo.getVolume();
//                    bestCargo = cargo;
//                }
//            }
//        }
//        return bestCargo;
//    }
    public CargoType selectCargo(Space currentSpace, List<CargoType> availableCargoes) {
        CargoType bestCargo = null;
        double maxArea = -1.0;

        for (CargoType cargo : availableCargoes) {
            if (cargo.quantity > 0 && cargo.canFit(currentSpace)) {
                // שינוי: במקום getVolume, אנחנו מחפשים את שטח הבסיס הגדול ביותר
                // זה עוזר ליצור שכבות יציבות ושטוחות
                double bestAreaForCargo = getBestBaseArea(cargo, currentSpace);
                if (bestAreaForCargo > maxArea) {
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
}
