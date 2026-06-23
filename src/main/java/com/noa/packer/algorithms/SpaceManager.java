package com.noa.packer.algorithms;

import java.util.*;
import java.util.stream.Collectors;

public class SpaceManager {


    public List<Space> updateSpacesAfterPlacement(List<Space> allSpaces, double xMin, double yMin, double zMin,
                                                  double xMax, double yMax, double zMax) {
        List<Space> updatedSpaces = new ArrayList<>();
        for (Space space : allSpaces) {
            if (spacesOverlap(space, xMin, yMin, zMin,
             xMax, yMax, zMax)) {
                //  החלל חופף עם הארגז - חתוך אותו סביב הארגז
                List<Space> fragments = cutSpaceAroundBox(space, xMin,  yMin,  zMin,
                 xMax, yMax, zMax);
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
    public List<Space> mergeRetryInto(List<Space> base, List<Space> retrySpaces) {
        List<Space> result = new ArrayList<>(base);

        for (Space retry : retrySpaces) {
            boolean canMerge = false;
            for (Space existing : result) {
                if (canMergeLeftRight(retry, existing)
                        || canMergeFrontRear(retry, existing)
                        || canMergeTopBottom(retry, existing)
                        || tryOverlapMerge(retry, existing) != null) {
                    canMerge = true;
                    break;
                }
            }
            if (canMerge) {
                result.add(retry);
            }
            // לא מתמזג → נזרק
        }

        return mergeSpaces(result);
    }
    public List<Space> mergeNewInto(List<Space> existing, List<Space> newSpaces) {
        List<Space> result = new ArrayList<>(existing);

        for (Space ns : newSpaces) {
            Space current = ns;
            boolean mergedSomething = true;

            while (mergedSomething) {
                mergedSomething = false;
                for (int i = 0; i < result.size(); i++) {
                    Space other = result.get(i);
                    if (canMergeLeftRight(current, other)) {
                        current = mergeLeftRight(current, other);
                        result.remove(i);
                        mergedSomething = true;
                        break;
                    } else if (canMergeFrontRear(current, other)) {
                        current = mergeFrontRear(current, other);
                        result.remove(i);
                        mergedSomething = true;
                        break;
                    } else if (canMergeTopBottom(current, other)) {
                        current = mergeTopBottom(current, other);
                        result.remove(i);
                        mergedSomething = true;
                        break;
                    }
                }
            }
            result.add(current);
        }

        return cleanSpaces(result);
    }
    public List<Space> mergeSpaces(List<Space> spaces) {
        if (spaces.size() <= 1) return new ArrayList<>(spaces);
        // שלב 1: מיזוג מדויק כמו קודם (exact match)
        List<Space> result = exactMerge(spaces);
        // שלב 2: מיזוג חפיפה — מוסיף חללים חדשים, לא מוחק ישנים
        List<Space> overlapMerged = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                Space merged = tryOverlapMerge(result.get(i), result.get(j));
                if (merged != null) {
                    overlapMerged.add(merged);
                }
            }
        }
        result.addAll(overlapMerged);
        return cleanSpaces(result);  // cleanSpaces מוריד חללים מכוסים
    }

    // המיזוג המדויק הקיים — ללא שינוי
    private List<Space> exactMerge(List<Space> spaces) {
        List<Space> result = new ArrayList<>(spaces);
        boolean merged = true;
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
                    if (canMergeLeftRight(current, candidate)) {
                        current = mergeLeftRight(current, candidate);
                        used[j] = true;
                        merged = true;
                    } else if (canMergeFrontRear(current, candidate)) {
                        current = mergeFrontRear(current, candidate);
                        used[j] = true;
                        merged = true;
                    } else if (canMergeTopBottom(current, candidate)) {
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

    // מיזוג חפיפה — intersection + שמירת מקוריים
    private Space tryOverlapMerge(Space s1, Space s2) {

        // ניסיון מיזוג ב-Y (left-right, Eq 5)
        if (isAdjacentY(s1, s2) && overlapsX(s1, s2) && overlapsZ(s1, s2)) {
            return mergeAlongY(s1, s2);
        }
        // ניסיון מיזוג ב-X (front-rear, Eq 6)
        if (isAdjacentX(s1, s2) && overlapsY(s1, s2) && overlapsZ(s1, s2)) {
            return mergeAlongX(s1, s2);
        }
        // ניסיון מיזוג ב-Z (top-bottom)
        if (isAdjacentZ(s1, s2) && overlapsX(s1, s2) && overlapsY(s1, s2)) {
            return mergeAlongZ(s1, s2);
        }
        return null;
    }

// --- בדיקות חפיפה (overlap, לא exact) ---

    private boolean overlapsX(Space s1, Space s2) {
        return s1.x < s2.x + s2.L - 1e-9 && s2.x < s1.x + s1.L - 1e-9;
    }
    private boolean overlapsY(Space s1, Space s2) {
        return s1.y < s2.y + s2.W - 1e-9 && s2.y < s1.y + s1.W - 1e-9;
    }
    private boolean overlapsZ(Space s1, Space s2) {
        return s1.z < s2.z + s2.H - 1e-9 && s2.z < s1.z + s1.H - 1e-9;
    }

// --- בדיקות צמידות (adjacent) ---

    private boolean isAdjacentY(Space s1, Space s2) {
        return Math.abs((s1.y + s1.W) - s2.y) < 1e-9 ||
                Math.abs((s2.y + s2.W) - s1.y) < 1e-9;
    }
    private boolean isAdjacentX(Space s1, Space s2) {
        return Math.abs((s1.x + s1.L) - s2.x) < 1e-9 ||
                Math.abs((s2.x + s2.L) - s1.x) < 1e-9;
    }
    private boolean isAdjacentZ(Space s1, Space s2) {
        return Math.abs((s1.z + s1.H) - s2.z) < 1e-9 ||
                Math.abs((s2.z + s2.H) - s1.z) < 1e-9;
    }

// --- מיזוג עם intersection ---

    private Space mergeAlongY(Space s1, Space s2) {
        // Y = union, X ו-Z = intersection
        double newX = Math.max(s1.x, s2.x);
        double newL = Math.min(s1.x + s1.L, s2.x + s2.L) - newX;
        double newY = Math.min(s1.y, s2.y);
        double newW = Math.max(s1.y + s1.W, s2.y + s2.W) - newY;
        double newZ = Math.max(s1.z, s2.z);
        double newH = Math.min(s1.z + s1.H, s2.z + s2.H) - newZ;
        if (newL < 1e-6 || newW < 1e-6 || newH < 1e-6) return null;
        return new Space(newX, newY, newZ, newL, newW, newH);
    }

    private Space mergeAlongX(Space s1, Space s2) {
        // X = union, Y ו-Z = intersection
        double newX = Math.min(s1.x, s2.x);
        double newL = Math.max(s1.x + s1.L, s2.x + s2.L) - newX;
        double newY = Math.max(s1.y, s2.y);
        double newW = Math.min(s1.y + s1.W, s2.y + s2.W) - newY;
        double newZ = Math.max(s1.z, s2.z);
        double newH = Math.min(s1.z + s1.H, s2.z + s2.H) - newZ;
        if (newL < 1e-6 || newW < 1e-6 || newH < 1e-6) return null;
        return new Space(newX, newY, newZ, newL, newW, newH);
    }

    private Space mergeAlongZ(Space s1, Space s2) {
        // Z = union, X ו-Y = intersection
        double newX = Math.max(s1.x, s2.x);
        double newL = Math.min(s1.x + s1.L, s2.x + s2.L) - newX;
        double newY = Math.max(s1.y, s2.y);
        double newW = Math.min(s1.y + s1.W, s2.y + s2.W) - newY;
        double newZ = Math.min(s1.z, s2.z);
        double newH = Math.max(s1.z + s1.H, s2.z + s2.H) - newZ;
        if (newL < 1e-6 || newW < 1e-6 || newH < 1e-6) return null;
        return new Space(newX, newY, newZ, newL, newW, newH);
    }
    /**
     * בדוק אם חלל חופף עם ארגז שהונח
     */
    private boolean spacesOverlap(Space space,double xMin, double yMin, double zMin,
                                  double xMax, double yMax, double zMax) {
        return space.x < xMax && space.x + space.L > xMin &&
                space.y < yMax && space.y + space.W > yMin &&
                space.z < zMax && space.z + space.H > zMin;
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
    private List<Space> cutSpaceAroundBox(Space space, double xMin, double yMin, double zMin,
                                          double xMax, double yMax, double zMax) {
        List<Space> fragments = new ArrayList<>();

        double sxMin = space.x;
        double syMin = space.y;
        double szMin = space.z;
        double sxMax = space.x + space.L;
        double syMax = space.y + space.W;
        double szMax = space.z + space.H;

        // Left (X-): כל מה שמשמאל לארגז
        if (xMin > sxMin) {
            fragments.add(new Space(
                    sxMin, syMin, szMin,
                    xMin - sxMin, space.W, space.H
            ));
        }

        // Right (X+): כל מה שמימין לארגז
        if (xMax < sxMax) {
            fragments.add(new Space(
                    xMax, syMin, szMin,
                    sxMax - xMax, space.W, space.H
            ));
        }

        // Bottom (Y-): כל מה שמאחור לארגז (בתוך ציר X של הארגז)
        if (yMin > syMin) {
            fragments.add(new Space(
                    sxMin, syMin, szMin,
                    space.L, yMin - syMin, space.H
            ));
        }

        // Top (Y+): כל מה שמאמם לארגז
        if (yMax < syMax) {
            fragments.add(new Space(
                    sxMin, yMax, szMin,
                    space.L, syMax - yMax, space.H
            ));
        }

        // Below (Z-): כל מה שמתחת לארגז
        if (zMin > szMin) {
            fragments.add(new Space(
                    sxMin, syMin, szMin,
                    space.L, space.W, zMin - szMin
            ));
        }

        // Above (Z+): כל מה שמעל לארגז
        if (zMax < szMax) {
            fragments.add(new Space(
                    sxMin, syMin, zMax,
                    space.L, space.W, szMax - zMax
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
                .toList();
        // הסר חללים המכוסים בחללים אחרים
        List<Space> cleaned = new ArrayList<>();
        for (Space s : filtered) {
            boolean contained = cleaned.stream().anyMatch(big -> isContained(s, big));
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



}
