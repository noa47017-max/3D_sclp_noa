package com.noa.packer.algorithms;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SpaceManager - ניהול חללים נקיים
 *
 * העיקרון: כל חלל ברשימת החללים הזמינים מובטח שלא חופף עם אף ארגז קיים.
 * לכן, כשמניחים ארגז בחלל - בלתי אפשרי שתהיה חפיפה.
 *
 * מבוסס על Section 3.4 + 3.5 מהמאמר (Three-space division + Merging)
 */
public class SpaceManager {

    // ============================================================
    //  CORE: עדכון חללים אחרי הנחת ארגז
    // ============================================================

    /**
     * עדכון כל החללים הזמינים אחרי הנחת ארגז חדש.
     *
     * כל חלל שחופף עם הארגז החדש - נחתך סביבו.
     * כל חלל שלא חופף - נשאר כמות שהוא.
     *
     * תוצאה: רשימת חללים נקיים שמובטח שלא חופפים עם שום ארגז.
     *
     * @param allSpaces כל החללים הזמינים (vrQueue + viQueue + legacyQueue)
     * @param placedBox הארגז שהונח (מיקום + כיוון)
     * @return רשימת חללים נקיים
     */
    public List<Space> updateSpacesAfterPlacement(List<Space> allSpaces, PlacedBox placedBox) {
        List<Space> updatedSpaces = new ArrayList<>();

        for (Space space : allSpaces) {
            if (spacesOverlap(space, placedBox)) {
                // ✅ החלל חופף עם הארגז - חתוך אותו סביב הארגז
                List<Space> fragments = cutSpaceAroundBox(space, placedBox);
                updatedSpaces.addAll(fragments);
            } else {
                // החלל לא חופף - שמור אותו
                updatedSpaces.add(space);
            }
        }

        // נקה חללים קטנים מדי וחללים מכוסים
        updatedSpaces = cleanSpaces(updatedSpaces);

        return updatedSpaces;
    }

    /**
     * בדוק אם חלל חופף עם ארגז שהונח
     */
    private boolean spacesOverlap(Space space, PlacedBox box) {
        return space.x < box.xMax && space.x + space.L > box.xMin &&
                space.y < box.yMax && space.y + space.W > box.yMin &&
                space.z < box.zMax && space.z + space.H > box.zMin;
    }

    /**
     * חתוך חלל סביב ארגז - יוצר עד 6 חללים שיורים נקיים
     *
     * דמיין קופסה גדולה (החלל) עם קופסה קטנה (הארגז) בתוכה.
     * אנחנו חותכים את הגדולה ל-6 חתיכות סביב הקטנה:
     *
     *   Left   (X-)  : מה שנשאר משמאל לארגז
     *   Right  (X+)  : מה שנשאר מימין לארגז
     *   Bottom (Y-)  : מה שנשאר מאחור לארגז
     *   Top    (Y+)  : מה שנשאר מאמם לארגז
     *   Below  (Z-)  : מה שנשאר מתחת לארגז
     *   Above  (Z+)  : מה שנשאר מעל לארגז
     */
    private List<Space> cutSpaceAroundBox(Space space, PlacedBox box) {
        List<Space> fragments = new ArrayList<>();

        double sxMin = space.x;
        double syMin = space.y;
        double szMin = space.z;
        double sxMax = space.x + space.L;
        double syMax = space.y + space.W;
        double szMax = space.z + space.H;

        // Left (X-): כל מה שמשמאל לארגז
        if (box.xMin > sxMin) {
            fragments.add(new Space(
                    sxMin, syMin, szMin,
                    box.xMin - sxMin, space.W, space.H
            ));
        }

        // Right (X+): כל מה שמימין לארגז
        if (box.xMax < sxMax) {
            fragments.add(new Space(
                    box.xMax, syMin, szMin,
                    sxMax - box.xMax, space.W, space.H
            ));
        }

        // Bottom (Y-): כל מה שמאחור לארגז (בתוך ציר X של הארגז)
        if (box.yMin > syMin) {
            fragments.add(new Space(
                    sxMin, syMin, szMin,
                    space.L, box.yMin - syMin, space.H
            ));
        }

        // Top (Y+): כל מה שמאמם לארגז
        if (box.yMax < syMax) {
            fragments.add(new Space(
                    sxMin, box.yMax, szMin,
                    space.L, syMax - box.yMax, space.H
            ));
        }

        // Below (Z-): כל מה שמתחת לארגז
        if (box.zMin > szMin) {
            fragments.add(new Space(
                    sxMin, syMin, szMin,
                    space.L, space.W, box.zMin - szMin
            ));
        }

        // Above (Z+): כל מה שמעל לארגז
        if (box.zMax < szMax) {
            fragments.add(new Space(
                    sxMin, syMin, box.zMax,
                    space.L, space.W, szMax - box.zMax
            ));
        }

        // סנן חללים קטנים מדי
        return fragments.stream()
                .filter(s -> s.getVolume() > 1e-6)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  MERGING: Section 3.5 - Remaining Space Merging Algorithm
    // ============================================================

    /**
     * מיזוג חללים שכנים לחללים גדולים יותר
     * Section 3.5 מהמאמר
     */
    public List<Space> mergeSpaces(List<Space> spaces) {
        if (spaces.size() <= 1) return new ArrayList<>(spaces);

        List<Space> result = new ArrayList<>(spaces);
        boolean merged = true;

        // חזור עד שאין מיזוגים נוספים
        while (merged) {
            merged = false;
            List<Space> newResult = new ArrayList<>();
            boolean[] used = new boolean[result.size()];

            for (int i = 0; i < result.size(); i++) {
                if (used[i]) continue;

                Space current = result.get(i);

                for (int j = i + 1; j < result.size(); j++) {
                    if (used[j]) continue;
                    Space candidate = result.get(j);

                    // Rule 1: Merge Left & Right (Equation 5)
                    if (canMergeLeftRight(current, candidate)) {
                        current = mergeLeftRight(current, candidate);
                        used[j] = true;
                        merged = true;
                    }
                    // Rule 2: Merge Front & Rear (Equation 6)
                    else if (canMergeFrontRear(current, candidate)) {
                        current = mergeFrontRear(current, candidate);
                        used[j] = true;
                        merged = true;
                    }
                    // Rule 3: Merge Top & Bottom (Z axis)
                    else if (canMergeTopBottom(current, candidate)) {
                        current = mergeTopBottom(current, candidate);
                        used[j] = true;
                        merged = true;
                    }
                }

                newResult.add(current);
                used[i] = true;
            }

            result = newResult;
        }

        return result;
    }

    // --- Rule 1: Left & Right (Y axis) ---

    private boolean canMergeLeftRight(Space s1, Space s2) {
        // אותו X range, אותו Z range, צמודים ב-Y
        boolean sameX = Math.abs(s1.x - s2.x) < 1e-9 && Math.abs(s1.L - s2.L) < 1e-9;
        boolean sameZ = Math.abs(s1.z - s2.z) < 1e-9 && Math.abs(s1.H - s2.H) < 1e-9;
        boolean adjacentY = Math.abs((s1.y + s1.W) - s2.y) < 1e-9 ||
                Math.abs((s2.y + s2.W) - s1.y) < 1e-9;

        return sameX && sameZ && adjacentY;
    }

    private Space mergeLeftRight(Space s1, Space s2) {
        double newY = Math.min(s1.y, s2.y);
        double newW = Math.max(s1.y + s1.W, s2.y + s2.W) - newY;
        return new Space(s1.x, newY, s1.z, s1.L, newW, s1.H);
    }

    // --- Rule 2: Front & Rear (X axis) ---

    private boolean canMergeFrontRear(Space s1, Space s2) {
        boolean sameY = Math.abs(s1.y - s2.y) < 1e-9 && Math.abs(s1.W - s2.W) < 1e-9;
        boolean sameZ = Math.abs(s1.z - s2.z) < 1e-9 && Math.abs(s1.H - s2.H) < 1e-9;
        boolean adjacentX = Math.abs((s1.x + s1.L) - s2.x) < 1e-9 ||
                Math.abs((s2.x + s2.L) - s1.x) < 1e-9;

        return sameY && sameZ && adjacentX;
    }

    private Space mergeFrontRear(Space s1, Space s2) {
        double newX = Math.min(s1.x, s2.x);
        double newL = Math.max(s1.x + s1.L, s2.x + s2.L) - newX;
        return new Space(newX, s1.y, s1.z, newL, s1.W, s1.H);
    }

    // --- Rule 3: Top & Bottom (Z axis) ---

    private boolean canMergeTopBottom(Space s1, Space s2) {
        boolean sameX = Math.abs(s1.x - s2.x) < 1e-9 && Math.abs(s1.L - s2.L) < 1e-9;
        boolean sameY = Math.abs(s1.y - s2.y) < 1e-9 && Math.abs(s1.W - s2.W) < 1e-9;
        boolean adjacentZ = Math.abs((s1.z + s1.H) - s2.z) < 1e-9 ||
                Math.abs((s2.z + s2.H) - s1.z) < 1e-9;

        return sameX && sameY && adjacentZ;
    }

    private Space mergeTopBottom(Space s1, Space s2) {
        double newZ = Math.min(s1.z, s2.z);
        double newH = Math.max(s1.z + s1.H, s2.z + s2.H) - newZ;
        return new Space(s1.x, s1.y, newZ, s1.L, s1.W, newH);
    }

    // ============================================================
    //  CLEANUP: ניקוי חללים
    // ============================================================

    /**
     * ניקוי חללים - הסר קטנים ומכוסים
     */
    public List<Space> cleanSpaces(List<Space> spaces) {
        // סנן חללים קטנים מדי
        List<Space> filtered = spaces.stream()
                .filter(s -> s.getVolume() > 1e-6)
                .sorted((a, b) -> Double.compare(b.getVolume(), a.getVolume()))
                .collect(Collectors.toList());

        // הסר חללים המכוסים בחללים אחרים
        List<Space> cleaned = new ArrayList<>();
        for (Space s : filtered) {
            boolean contained = false;
            for (Space big : cleaned) {
                if (isContained(s, big)) {
                    contained = true;
                    break;
                }
            }
            if (!contained) cleaned.add(s);
        }

        return cleaned;
    }

    private boolean isContained(Space inner, Space outer) {
        return inner.x >= outer.x - 1e-9 && inner.y >= outer.y - 1e-9 && inner.z >= outer.z - 1e-9 &&
                inner.x + inner.L <= outer.x + outer.L + 1e-9 &&
                inner.y + inner.W <= outer.y + outer.W + 1e-9 &&
                inner.z + inner.H <= outer.z + outer.H + 1e-9;
    }

    // ============================================================
    //  PlacedBox: מבנה עזר לארגז שהונח
    // ============================================================

    /**
     * מבנה עזר שמייצג ארגז שהונח במכולה
     */
    public static class PlacedBox {
        public double xMin, yMin, zMin;
        public double xMax, yMax, zMax;

        public PlacedBox(double xMin, double yMin, double zMin,
                         double xMax, double yMax, double zMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.zMin = zMin;
            this.xMax = xMax;
            this.yMax = yMax;
            this.zMax = zMax;
        }

        /**
         * יצירה מתוך חלל + כיוון (orientation)
         */
        public static PlacedBox fromSpaceAndOrientation(Space space, Orientation ori) {
            return new PlacedBox(
                    space.x, space.y, space.z,
                    space.x + ori.l, space.y + ori.w, space.z + ori.h
            );
        }
    }
    public List<Space> splitIntoResiduals(Space current, Orientation o) {

        List<Space> residuals = new ArrayList<>();     // רשימת חללים חדשה שנחזיר

        double x = current.x;                          // שומרים את נקודת ההתחלה X
        double y = current.y;                          // שומרים את נקודת ההתחלה Y
        double z = current.z;                          // שומרים את נקודת ההתחלה Z

        double L = current.L;                          // אורך החלל
        double W = current.W;                          // רוחב החלל
        double H = current.H;                          // גובה החלל

        double l = o.l;                                // אורך הארגז בכיוון שנבחר
        double w = o.w;                                // רוחב הארגז בכיוון שנבחר
        double h = o.h;                                // גובה הארגז בכיוון שנבחר

        // -------------------------
        // RIGHT SPACE — חלל ימני
        // -------------------------
        if (L - l > 1e-6) {                            // אם נשאר אורך מימין לארגז
            residuals.add(new Space(
                    x + l,                             // מתחיל אחרי הארגז בציר X
                    y,                                 // אותו Y
                    z,                                 // אותו Z
                    L - l,                             // האורך שנותר
                    W,                                 // כל הרוחב
                    H                                  // כל הגובה
            ));
        }

        // -------------------------
        // FRONT SPACE — חלל קדמי
        // -------------------------
        if (W - w > 1e-6) {                            // אם נשאר רוחב מלפנים
            residuals.add(new Space(
                    x,                                 // אותו X
                    y + w,                             // מתחיל אחרי הארגז בציר Y
                    z,                                 // אותו Z
                    l,                                 // כל האורך
                    W - w,                             // הרוחב שנותר
                    H                                  // כל הגובה
            ));
        }

        // -------------------------
        // TOP SPACE — חלל עליון
        // -------------------------
        if (H - h > 1e-6) {                            // אם נשאר גובה מעל הארגז
            residuals.add(new Space(
                    x,                                 // אותו X
                    y,                                 // אותו Y
                    z + h,                             // מתחיל מעל הארגז
                    l,                                 // כל האורך
                    w,                                 // כל הרוחב
                    H - h                              // הגובה שנותר
            ));
        }

        return residuals;                              // מחזירים את שלושת חללי השארית
    }


    public List<Space> splitIntoResidualsNewLayerVU(Space current, Orientation o) {

        List<Space> residuals = new ArrayList<>();     // רשימת חללים חדשה שנחזיר
        double x = current.x;                          // שומרים את נקודת ההתחלה X
        double y = current.y;                          // שומרים את נקודת ההתחלה Y
        double z = current.z;                          // שומרים את נקודת ההתחלה Z
        double L = current.L;                          // אורך החלל
        double W = current.W;                          // רוחב החלל
        double H = current.H;                          // גובה החלל
        double l = o.l;                                // אורך הארגז בכיוון שנבחר
        double w = o.w;                                // רוחב הארגז בכיוון שנבחר
        double h = o.h;                                // גובה הארגז בכיוון שנבחר


        // RIGHT SPACE — חלל ימני
        if (W - w > 1e-6) {                            // אם נשאר רוחב מלפנים
            residuals.add(new Space(
                    x,                                 // אותו X
                    y + w,                             // מתחיל אחרי הארגז בציר Y
                    z,                                 // אותו Z
                    l,                                 // כל האורך
                    W - w,                             // הרוחב שנותר
                    H                                  // כל הגובה
            ));
        }
        // TOP SPACE — חלל עליון
        if (H - h > 1e-6) {                            // אם נשאר גובה מעל הארגז
            residuals.add(new Space(
                    x,                                 // אותו X
                    y,                                 // אותו Y
                    z + h,                             // מתחיל מעל הארגז
                    l,                                 // כל האורך
                    w,                                 // כל הרוחב
                    H - h                              // הגובה שנותר
            ));
        }

        return residuals;                              // מחזירים את שלושת חללי השארית
    }

    }
