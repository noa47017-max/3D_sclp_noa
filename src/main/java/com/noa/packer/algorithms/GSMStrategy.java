package com.noa.packer.algorithms;

import ch.qos.logback.core.joran.sanity.Pair;
import java.util.List;
import static java.lang.Math.abs;

public class GSMStrategy {

    final OrientationOptimizer orientationOptimizer = new OrientationOptimizer();


    // גרסה קודמת/חלופית של בחירת מטען (מבוססת נפח מקסימלי בלבד)
    public CargoSelection selectCargo1(Space currentSpace,
                                       List<CargoType> availableCargoes,
                                       PheromoneManager pm,
                                       List<LoadingChain.PlacedBoxInfo> placed) {
        CargoType bestCargo = null;
        double maxVolume = -1.0;

        for (CargoType cargo : availableCargoes) {
            if (cargo.quantity > 0 && cargo.canFit(currentSpace)) {
                if (cargo.getVolume() > maxVolume) {
                    maxVolume = cargo.getVolume();
                    bestCargo = cargo;
                }
            }
        }
        Orientation bestOri = orientationOptimizer.getBestOrientation(currentSpace, bestCargo);
        String stateKey = currentCargoState(availableCargoes, pm);
        pm.depositAndEvaporate(stateKey, bestCargo.id, bestOri, currentSpace, currentSpace.getVolume() - bestCargo.getVolume());
        return new CargoSelection(bestCargo, bestOri);
    }

    private String currentCargoState(List<CargoType> allTypes, PheromoneManager pm) {
        return pm.generateStateWord(allTypes);
    }

    // הפונקציה המרכזית לבחירת מטען
    public CargoSelection selectCargo(Space currentSpace,
                                      List<CargoType> availableCargoes,
                                      List<LoadingChain.PlacedBoxInfo> placed) {
        CargoType bestCargo = null;
        Orientation bestOri = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (CargoType c : availableCargoes) {
            if (c.quantity <= 0) continue;

            Orientation ori = orientationOptimizer.getBestOrientation(currentSpace, c);
            if (ori == null) continue;

            double score = evaluate(currentSpace, c, ori, placed);

            if (score > bestScore) {
                bestScore = score;
                bestCargo = c;
                bestOri = ori;
            }
        }

        if (bestCargo == null) return null;
        return new CargoSelection(bestCargo, bestOri);
    }

    private double evaluate(Space s, CargoType c, Orientation o,
                            List<LoadingChain.PlacedBoxInfo> placed) {

        double U = c.getVolume() / s.getVolume();                            // 1. ניצול נפח
        double D = 1 - (abs(s.L - o.l) + abs(s.W - o.w) + abs(s.H - o.h))    // 2. התאמה גיאומטרית
                / (s.L + s.W + s.H);
        double layer = 1 - abs(o.h - s.H) / s.H;                             // 3. התאמת שכבה
        double contact = estimateContact(s, o, placed);                      // 4. מגע

        double fragmentationPenalty = 0;
        double remL = s.L % o.l;
        double remW = s.W % o.w;
        double remH = s.H % o.h;
        if (remL > 1 && remL < Global.MIN_DIM) fragmentationPenalty += 0.03;
        if (remW > 1 && remW < Global.MIN_DIM) fragmentationPenalty += 0.03;
        if (remH > 1 && remH < Global.MIN_DIM) fragmentationPenalty += 0.03;

        return 0.40 * U + 0.30 * contact + 0.20 * D + 0.10 * layer - fragmentationPenalty;
    }

    private double estimateContact(Space s, Orientation o,
                                   List<LoadingChain.PlacedBoxInfo> placed) {
        double contact = 0;

        double x1 = s.x, y1 = s.y, z1 = s.z;
        double x2 = x1 + o.l, y2 = y1 + o.w, z2 = z1 + o.h;

        // 1. רצפה
        if (z1 == 0) {
            contact += o.l * o.w;
        }

        // 2. מגע עם ארגזים קיימים
        for (LoadingChain.PlacedBoxInfo b : placed) {
            double bx1 = b.leftBackBottom[0], by1 = b.leftBackBottom[1], bz1 = b.leftBackBottom[2];
            double bx2 = b.rightFrontTop[0], by2 = b.rightFrontTop[1], bz2 = b.rightFrontTop[2];

            boolean touchingTop = Math.abs(z1 - bz2) < Global.EPSILON;

            if (touchingTop) {
                double overlapX = overlapLength(x1, x2, bx1, bx2);
                double overlapY = overlapLength(y1, y2, by1, by2);

                if (overlapX > 0 && overlapY > 0) {
                    contact += overlapX * overlapY;
                }
            }
        }

        // נרמול
        double maxPossible = 2 * (o.l * o.w + o.l * o.h + o.w * o.h);
        return contact / Math.max(Global.EPSILON, maxPossible);
    }

    private double overlapLength(double a1, double a2, double b1, double b2) {
        return Math.max(0, Math.min(a2, b2) - Math.max(a1, b1));
    }

    private String makePheromoneKey(String stateKey, int cargoId, Orientation ori, Space s) {
        int rx = (int)Math.round(s.x), ry = (int)Math.round(s.y), rz = (int)Math.round(s.z);
        return stateKey + "|" + cargoId + "|" + ori.toShortString() + "@" + rx + "," + ry + "," + rz;
    }

    private double localFillScore(CargoType c, Orientation ori, Space s) {
        double boxL = ori.l, boxW = ori.w, boxH = ori.h;
        double spaceY = s.W, spaceZ = s.H;

        double remY = spaceY % boxW;
        double remZ = spaceZ % boxH;

        double scoreY = remY / Math.max(Global.EPSILON, spaceY);
        double scoreZ = remZ / Math.max(Global.EPSILON, spaceZ);

        double wY = 0.5, wZ = 0.5;
        return wY * scoreY * boxH + wZ * scoreZ * boxW;
    }

    // פונקציה לבחירת מטען על בסיס שטח פנים בלבד (מניעת חורים בשכבות)
    public CargoType selectCargo7(Space currentSpace, List<CargoType> availableCargoes) {
        CargoType bestCargo = null;
        double maxArea = -1.0;

        for (CargoType cargo : availableCargoes) {
            if (cargo.quantity > 0 && cargo.canFit(currentSpace)) {
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
        double[][] orientations = {
                {cargo.l, cargo.w, cargo.h}, {cargo.l, cargo.h, cargo.w},
                {cargo.w, cargo.l, cargo.h}, {cargo.w, cargo.h, cargo.l},
                {cargo.h, cargo.l, cargo.w}, {cargo.h, cargo.w, cargo.l}
        };

        for (double[] ori : orientations) {
            double oL = ori[0], oW = ori[1], oH = ori[2];
            if (oL <= space.L + 1e-9 && oW <= space.W + 1e-9 && oH <= space.H + 1e-9) {
                double area = oL * oW;
                if (area > best) best = area;
            }
        }
        return best;
    }

    public void selectCargo3(Space currentSpace, CargoType c, boolean isNewLayer,
                             PheromoneManager pm, double layerHeight) {
        double U = c.getVolume() / currentSpace.getVolume();
        double D = 1 - (abs(currentSpace.L - c.l) + abs(currentSpace.W - c.w) + abs(currentSpace.H - c.h)) / (currentSpace.L + currentSpace.W + currentSpace.H);
    }
}