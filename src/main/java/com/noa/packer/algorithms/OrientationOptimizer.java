package com.noa.packer.algorithms;

import java.util.List;

import com.noa.packer.models.Container;

public class OrientationOptimizer {

    private OrientationOptimizer(){}
    public static Orientation getBestOrientation(Space s, CargoType cargo) {
        Orientation best = null;
        double minWaste = Double.MAX_VALUE;
        for (Orientation o : cargo.getAllowedOrientations()) {
            if (!(o.l > s.L + Global.EPSILON || o.w > s.W + Global.EPSILON || o.h > s.H + Global.EPSILON)) {
            double wasteW = (s.W % o.w) * o.h;
            double wasteH = (s.H % o.h) * o.w;
            double waste  = wasteW + wasteH;
            if (waste < minWaste) { minWaste = waste; best = o; }
            }
        }
        return best;
    }
}
