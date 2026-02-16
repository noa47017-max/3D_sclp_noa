package com.noa.packer;

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
                double area = cargo.l * cargo.w;
                if (area > maxArea) {
                    maxArea = area;
                    bestCargo = cargo;
                }
            }
        }
        return bestCargo;
    }

}
