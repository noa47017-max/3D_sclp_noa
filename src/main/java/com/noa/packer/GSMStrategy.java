package com.noa.packer;

import java.util.List;
public class GSMStrategy {
    /**
     * בחירת הארגז עם הנפח הגדול ביותר מתוך רשימת הארגזים הזמינים [5]
     */
    public CargoType selectCargo(Space currentSpace, List<CargoType> availableCargoes) {
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
        return bestCargo;
    }


}
