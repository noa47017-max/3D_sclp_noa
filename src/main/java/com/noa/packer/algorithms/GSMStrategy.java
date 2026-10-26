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
}