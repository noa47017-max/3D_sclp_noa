package com.noa.packer;

import java.util.List;

public class OrientationOptimizer {
//    public Orientation getBestOrientation(Space omega, CargoType cargo, Container c) {
//        Orientation best = null;
//        double minResidualArea = Double.MAX_VALUE;
//
//        for (Orientation o : cargo.getPossibleOrientations()) {
//            // בדיקה שהארגז פיזית נכנס בחלל
//            if (o.l <= omega.L && o.w <= omega.W && o.h <= omega.H) {
//                // חישוב לפי משוואה 8: שטח שארית במישור YOZ (רוחב וגובה)
//                double area = ((omega.W % o.w) * o.h) + ((omega.H % o.h) * o.w);
//                if (area < minResidualArea) {
//                    minResidualArea = area;
//                    best = o;
//                }
//            }
//        }
//        if (best != null) {
//            if(isWithinBounds(omega,best,c))
//                return best;
//        }
//        return null;
//    }
public Orientation getBestOrientation(Space space, CargoType cargo, Container container) {
    List<Orientation> possible = cargo.getPossibleOrientations();
    Orientation best = null;
    double maxScore = -1.0;

    for (Orientation ori : possible) {
        if (ori.l <= space.L && ori.w <= space.W && ori.h <= space.H) {
            // ניקוד המבוסס על שטח פנים (Area) חלקי הגובה
            // זה גורם לאלגוריתם להעדיף ארגזים שטוחים שתופסים הרבה רצפה
            double area = ori.l * ori.w;
            double heightPenalty = ori.h;
            double score = area / heightPenalty;

            // בונוס משמעותי אם הגובה מתאים בדיוק לגובה החלל הקיים (מילוי שכבה)
            if (Math.abs(ori.h - space.H) < 0.001) {
                score *= 2.0;
            }

            if (score > maxScore) {
                maxScore = score;
                best = ori;
            }
        }
    }
    return best;
}
    public boolean isWithinBounds(Space s, Orientation o, Container c) {
        return (s.x + o.l <= c.L + 0.01) &&
                (s.y + o.w <= c.W + 0.01) &&
                (s.z + o.h <= c.H + 0.01);
    }
}
