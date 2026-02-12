package com.noa.packer;
public class OrientationOptimizer {
    public Orientation getBestOrientation(Space omega, CargoType cargo, Container c) {
        Orientation best = null;
        double minResidualArea = Double.MAX_VALUE;

        for (Orientation o : cargo.getPossibleOrientations()) {
            // בדיקה שהארגז פיזית נכנס בחלל
            if (o.l <= omega.L && o.w <= omega.W && o.h <= omega.H) {
                // חישוב לפי משוואה 8: שטח שארית במישור YOZ (רוחב וגובה)
                double area = ((omega.W % o.w) * o.h) + ((omega.H % o.h) * o.w);
                if (area < minResidualArea) {
                    minResidualArea = area;
                    best = o;
                }
            }
        }
        if (best != null) {
            if(isWithinBounds(omega,best,c))
                return best;
        }
        return null;
    }
    public boolean isWithinBounds(Space s, Orientation o, Container c) {
        return (s.x + o.l <= c.L + 0.01) &&
                (s.y + o.w <= c.W + 0.01) &&
                (s.z + o.h <= c.H + 0.01);
    }
}
