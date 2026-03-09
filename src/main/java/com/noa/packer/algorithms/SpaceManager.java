package com.noa.packer.algorithms;
import java.util.ArrayList;
import java.util.List;

//public class SpaceManager {
//
//    /**
//     * פונקציה שמקבלת חלל שהונח בו ארגז, ומייצרת חללים חדשים תוך ניסיון למזג אותם
//     */
//    public List<Space> splitAndMerge(Space currentSpace, Orientation bestOri) {
//        List<Space> newSpaces = new ArrayList<>();
//
//        // 1. יצירת שלושת החללים הבסיסיים (כמו שעשינו ב-Space)
//        Space top = currentSpace.getTopResidual(bestOri);
//        Space right = currentSpace.getRightResidual(bestOri);
//        Space front = currentSpace.getFrontResidual(bestOri);
//
//        // הוספה לרשימה זמנית רק אם יש להם נפח
//        if (top.getVolume() > 0) newSpaces.add(top);
//        if (right.getVolume() > 0) newSpaces.add(right);
//        if (front.getVolume() > 0) newSpaces.add(front);
//
//        // 2. לוגיקת המיזוג (Merging Logic)
//        // כאן אנחנו בודקים אם חללים חדשים יכולים להתאחד עם חללים קיימים
//        // במידה והם חופפים או צמודים לחלוטין באחת הפאות.
//        return mergeSpaces(newSpaces);
//    }
//
//
//

//}
public class SpaceManager {

    public List<Space> splitAndMerge(Space currentSpace, Orientation bestOri) {
        List<Space> nextSpaces = new ArrayList<>();

        // חלל Right מורחב (כל העומק והגובה)
        if (currentSpace.L > bestOri.l) {
            nextSpaces.add(new Space(currentSpace.x + bestOri.l, currentSpace.y, currentSpace.z,
                    currentSpace.L - bestOri.l, currentSpace.W, currentSpace.H));
        }
        // חלל Front מורחב (כל האורך והגובה)
        if (currentSpace.W > bestOri.w) {
            nextSpaces.add(new Space(currentSpace.x, currentSpace.y + bestOri.w, currentSpace.z,
                    currentSpace.L, currentSpace.W - bestOri.w, currentSpace.H));
        }
        // חלל Top מורחב (כל האורך והעומק)
        if (currentSpace.H > bestOri.h) {
            nextSpaces.add(new Space(currentSpace.x, currentSpace.y, currentSpace.z + bestOri.h,
                    currentSpace.L, currentSpace.W, currentSpace.H - bestOri.h));
        }
        return nextSpaces;
    }
        private Space tryCombine(Space s1, Space s2) {
        // בגלל חישובי דאבל, נשתמש בטולרנס קטן (אפסילון)
        double eps = 0.001;
        // 1. ניסיון מיזוג לאורך ציר Y (רוחב - Width)
        // תנאי: s1 ו-s2 צמודים ב-Y, ויש להם X, Z, L, H זהים לחלוטין
        if (Math.abs(s1.y + s1.W - s2.y) < eps || Math.abs(s2.y + s2.W - s1.y) < eps) {
            if (Math.abs(s1.x - s2.x) < eps && Math.abs(s1.z - s2.z) < eps &&
                    Math.abs(s1.L - s2.L) < eps && Math.abs(s1.H - s2.H) < eps) {

                return new Space(s1.x, Math.min(s1.y, s2.y), s1.z,
                        s1.L, s1.W + s2.W, s1.H);
            }
        }
        // 2. ניסיון מיזוג לאורך ציר X (אורך - Length)
        // תנאי: s1 ו-s2 צמודים ב-X, ויש להם Y, Z, W, H זהים לחלוטין
        if (Math.abs(s1.x + s1.L - s2.x) < eps || Math.abs(s2.x + s2.L - s1.x) < eps) {
            if (Math.abs(s1.y - s2.y) < eps && Math.abs(s1.z - s2.z) < eps &&
                    Math.abs(s1.W - s2.W) < eps && Math.abs(s1.H - s2.H) < eps) {

                return new Space(Math.min(s1.x, s2.x), s1.y, s1.z,
                        s1.L + s2.L, s1.W, s1.H);
            }
        }
        // 3. ניסיון מיזוג לאורך ציר Z (גובה - Height)
        // תנאי: s1 ו-s2 צמודים ב-Z, ויש להם X, Y, L, W זהים לחלוטין
        if (Math.abs(s1.z + s1.H - s2.z) < eps || Math.abs(s2.z + s2.H - s1.z) < eps) {
            if (Math.abs(s1.x - s2.x) < eps && Math.abs(s1.y - s2.y) < eps &&
                    Math.abs(s1.L - s2.L) < eps && Math.abs(s1.W - s2.W) < eps) {

                return new Space(s1.x, s1.y, Math.min(s1.z, s2.z),
                        s1.L, s1.W, s1.H + s2.H);
            }
        }
        return null; // לא ניתן למזג באופן בטוח
    }
    public List<Space> mergeSpaces(List<Space> spaces) {
        if (spaces.size() < 2) return spaces;
        List<Space> merged = new ArrayList<>(spaces);
        boolean found;
        do {
            found = false;
            for (int i = 0; i < merged.size(); i++) {
                for (int j = i + 1; j < merged.size(); j++) {
                    Space s1 = merged.get(i);
                    Space s2 = merged.get(j);
                    Space union = tryCombine(s1, s2);
                    if (union != null) {
                        merged.remove(j);
                        merged.remove(i);
                        merged.add(union);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        } while (found);
        return merged;
    }
    public List<Space> cleanAndOptimizeSpaces(List<Space> spaces) {
        List<Space> result = new ArrayList<>();
        for (int i = 0; i < spaces.size(); i++) {
            Space s1 = spaces.get(i);
            // התעלמות מחללים קטנים מדי (רעש חישובי)
            if (s1.L < 1.0 || s1.W < 1.0 || s1.H < 1.0) continue;

            boolean isContained = false;
            for (int j = 0; j < spaces.size(); j++) {
                if (i != j && isContained(s1, spaces.get(j))) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                result.add(s1);
            }
        }
        return result;
    }

    private boolean isContained(Space small, Space large) {
        return small.x >= large.x && small.y >= large.y && small.z >= large.z &&
                (small.x + small.L) <= (large.x + large.L) &&
                (small.y + small.W) <= (large.y + large.W) &&
                (small.z + small.H) <= (large.z + large.H);
    }

    // ... כאן נכנסת פונקציית ה-mergeSpaces וה-tryCombine המקורית שלך
}