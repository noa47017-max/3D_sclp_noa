package com.noa.packer.algorithms;

import java.util.*;
import java.util.stream.Collectors;

public class SpaceManager {

    private SpaceManager() {}

    public static List<Space> updateSpacesAfterPlacement(List<Space> allSpaces, double xMin, double yMin, double zMin,
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


    public static List<Space> mergeSpaces(List<Space> spaces) {
        if (spaces.size() <= 1) return new ArrayList<>(spaces);
        //  מיזוג מדויק (exact match)
        List<Space> result = exactMerge(spaces);
        //  מיזוג חפיפה — מוסיף חללים חדשים, לא מוחק ישנים
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
        return cleanSpaces(result);
    }

    // המיזוג המדויק
    private static List<Space> exactMerge(List<Space> spaces) {
        List<Space> result = new ArrayList<>(spaces);
        boolean merged = false;
        List<Space> newResult = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
                Space current = result.get(i);
                for (int j = i + 1; j < result.size(); j++) {
                    Space candidate = result.get(j);
                    if (canMergeLeftRight(current, candidate)) {
                        result.set(j,mergeLeftRight(current, candidate));
                        merged = true;
                    } else if (canMergeFrontRear(current, candidate)) {
                        result.set(j,mergeFrontRear(current, candidate));
                        merged = true;
                    } else if (canMergeTopBottom(current, candidate)) {
                        result.set(j, mergeTopBottom(current, candidate));
                        merged = true;
                    }
                }
                if (!merged){
                    newResult.add(current);
                }
                merged = false;
        }
        result = newResult;
        return result;
    }


    // מיזוג חפיפה  + שמירת מקוריים
    public static Space tryOverlapMerge(Space s1, Space s2) {

        // ניסיון מיזוג ב-Y
        if (isAdjacentY(s1, s2) && overlapsX(s1, s2) && overlapsZ(s1, s2)) {
            return mergeAlongY(s1, s2);
        }
        // ניסיון מיזוג ב-X
        if (isAdjacentX(s1, s2) && overlapsY(s1, s2) && overlapsZ(s1, s2)) {
            return mergeAlongX(s1, s2);
        }
        // ניסיון מיזוג ב-Z
        if (isAdjacentZ(s1, s2) && overlapsX(s1, s2) && overlapsY(s1, s2)) {
            return mergeAlongZ(s1, s2);
        }
        return null;
    }

    // --- בדיקות חפיפה
    private static boolean overlapsX(Space s1, Space s2) {
        return s1.x < s2.x + s2.L - 1e-9 && s2.x < s1.x + s1.L - 1e-9;
    }
    private static boolean overlapsY(Space s1, Space s2) {
        return s1.y < s2.y + s2.W - 1e-9 && s2.y < s1.y + s1.W - 1e-9;
    }
    private static boolean overlapsZ(Space s1, Space s2) {
        return s1.z < s2.z + s2.H - 1e-9 && s2.z < s1.z + s1.H - 1e-9;
    }

    //  בדיקות צמידות
    private static boolean isAdjacentY(Space s1, Space s2) {
        return Math.abs((s1.y + s1.W) - s2.y) < 1e-9 ||
                Math.abs((s2.y + s2.W) - s1.y) < 1e-9;
    }
    private static boolean isAdjacentX(Space s1, Space s2) {
        return Math.abs((s1.x + s1.L) - s2.x) < 1e-9 ||
                Math.abs((s2.x + s2.L) - s1.x) < 1e-9;
    }
    private static boolean isAdjacentZ(Space s1, Space s2) {
        return Math.abs((s1.z + s1.H) - s2.z) < 1e-9 ||
                Math.abs((s2.z + s2.H) - s1.z) < 1e-9;
    }

    // מיזוג
    private static Space mergeAlongY(Space s1, Space s2) {
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
    private static Space mergeAlongX(Space s1, Space s2) {
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
    private static Space mergeAlongZ(Space s1, Space s2) {
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

    // בדוק אם חלל חופף עם ארגז שהונח
    private static boolean spacesOverlap(Space space,double xMin, double yMin, double zMin,
                                  double xMax, double yMax, double zMax) {
        return space.x < xMax && space.x + space.L > xMin && //מתחיל לפני שנגמר הארגז ואחרי שהארגז מתחיל
                space.y < yMax && space.y + space.W > yMin &&
                space.z < zMax && space.z + space.H > zMin;
    }

    // חתוך חלל סביב ארגז - יוצר עד 6 חללים שיורים נקיים
    private static List<Space> cutSpaceAroundBox(Space space, double xMin, double yMin, double zMin,
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


    // מיזוג חללים שכנים לחללים גדולים יותר

    // ציר Y
    public static boolean canMergeLeftRight(Space s1, Space s2) {
        // אותו X range, אותו Z range, צמודים או חופפים ב-Y
        boolean sameX = Math.abs(s1.x - s2.x) < 1e-9 && Math.abs(s1.L - s2.L) < 1e-9;
        boolean sameZ = Math.abs(s1.z - s2.z) < 1e-9 && Math.abs(s1.H - s2.H) < 1e-9;
        boolean touchOrOverlapY = s1.y <= s2.y + s2.W + 1e-9 && s2.y <= s1.y + s1.W + 1e-9;

        return sameX && sameZ && touchOrOverlapY;
    }
    private static Space mergeLeftRight(Space s1, Space s2) {
        double newY = Math.min(s1.y, s2.y);
        double newW = Math.max(s1.y + s1.W, s2.y + s2.W) - newY;
        return new Space(s1.x, newY, s1.z, s1.L, newW, s1.H);
    }

    // ציר X

    public static boolean canMergeFrontRear(Space s1, Space s2) {
        boolean sameY = Math.abs(s1.y - s2.y) < 1e-9 && Math.abs(s1.W - s2.W) < 1e-9;
        boolean sameZ = Math.abs(s1.z - s2.z) < 1e-9 && Math.abs(s1.H - s2.H) < 1e-9;
        // צמודים או חופפים בציר X
        boolean touchOrOverlapX = s1.x <= s2.x + s2.L + 1e-9 && s2.x <= s1.x + s1.L + 1e-9;

        return sameY && sameZ && touchOrOverlapX;
    }
    private static Space mergeFrontRear(Space s1, Space s2) {
        double newX = Math.min(s1.x, s2.x);
        double newL = Math.max(s1.x + s1.L, s2.x + s2.L) - newX;
        return new Space(newX, s1.y, s1.z, newL, s1.W, s1.H);
    }

    // ציר Z

    public static boolean canMergeTopBottom(Space s1, Space s2) {
        boolean sameX = Math.abs(s1.x - s2.x) < 1e-9 && Math.abs(s1.L - s2.L) < 1e-9;
        boolean sameY = Math.abs(s1.y - s2.y) < 1e-9 && Math.abs(s1.W - s2.W) < 1e-9;
        // צמודים או חופפים ב-Z
        boolean touchOrOverlapZ = s1.z <= s2.z + s2.H + 1e-9 && s2.z <= s1.z + s1.H + 1e-9;

        return sameX && sameY && touchOrOverlapZ;
    }
    private static Space mergeTopBottom(Space s1, Space s2) {
        double newZ = Math.min(s1.z, s2.z);
        double newH = Math.max(s1.z + s1.H, s2.z + s2.H) - newZ;
        return new Space(s1.x, s1.y, newZ, s1.L, s1.W, newH);
    }


    // ניקוי חללים - הסר קטנים ומוכלים
    public static List<Space> cleanSpaces(List<Space> spaces) {
        // סנן חללים קטנים מדי
        List<Space> filtered = spaces.stream()
                .filter(s -> s.getVolume() > Global.EPSILON )
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
    //  בדיקה האם החלל מוכל
    private static boolean isContained(Space inner, Space outer) {
        return inner.x >= outer.x - 1e-9 && inner.y >= outer.y - 1e-9 && inner.z >= outer.z - 1e-9 &&
                inner.x + inner.L <= outer.x + outer.L + 1e-9 &&
                inner.y + inner.W <= outer.y + outer.W + 1e-9 &&
                inner.z + inner.H <= outer.z + outer.H + 1e-9;
    }

}