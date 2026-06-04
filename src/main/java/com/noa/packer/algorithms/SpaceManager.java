//package com.noa.packer.algorithms;
//import java.util.ArrayList;
//import java.util.List;
//
//public class SpaceManager {
//
//    public List<Space> splitIntoResiduals(Space current, Orientation o) {
//        List<Space> residuals = new ArrayList<>();
//
//        if (current.L - o.l > 1e-6) {
//            residuals.add(new Space(
//                    current.x + o.l, current.y, current.z,
//                    current.L - o.l, current.W, current.H
//            ));
//        }
//
//        if (current.W - o.w > 1e-6) {
//            residuals.add(new Space(
//                    current.x, current.y + o.w, current.z,
//                    o.l, current.W - o.w, current.H
//            ));
//        }
//
//        if (current.H - o.h > 1e-6) {
//            residuals.add(new Space(
//                    current.x, current.y, current.z + o.h,
//                    o.l, o.w, current.H - o.h
//            ));
//        }
//
//        return residuals;
//    }
//
//
//        public List<Space> splitAndMerge(Space currentSpace, Orientation bestOri) {
//            List<Space> nextSpaces = new ArrayList<>();
//
//            // חלל Right מורחב (כל העומק והגובה)
//            if (currentSpace.L > bestOri.l) {
//                nextSpaces.add(new Space(currentSpace.x + bestOri.l, currentSpace.y, currentSpace.z,
//                        currentSpace.L - bestOri.l, currentSpace.W, currentSpace.H));
//            }
//            // חלל Front מורחב (כל האורך והגובה)
//            if (currentSpace.W > bestOri.w) {
//                nextSpaces.add(new Space(currentSpace.x, currentSpace.y + bestOri.w, currentSpace.z,
//                        currentSpace.L, currentSpace.W - bestOri.w, currentSpace.H));
//            }
//            // חלל Top מורחב (כל האורך והעומק)
//            if (currentSpace.H > bestOri.h) {
//                nextSpaces.add(new Space(currentSpace.x, currentSpace.y, currentSpace.z + bestOri.h,
//                        currentSpace.L, currentSpace.W, currentSpace.H - bestOri.h));
//            }
//            return nextSpaces;
//        }
//        private Space tryCombine(Space s1, Space s2) {
//            // בגלל חישובי דאבל, נשתמש בטולרנס קטן (אפסילון)
//            double eps = 0.001;
//            // 1. ניסיון מיזוג לאורך ציר Y (רוחב - Width)
//            // תנאי: s1 ו-s2 צמודים ב-Y, ויש להם X, Z, L, H זהים לחלוטין
//            if (Math.abs(s1.y + s1.W - s2.y) < eps || Math.abs(s2.y + s2.W - s1.y) < eps) {
//                if (Math.abs(s1.x - s2.x) < eps && Math.abs(s1.z - s2.z) < eps &&
//                        Math.abs(s1.L - s2.L) < eps && Math.abs(s1.H - s2.H) < eps) {
//
//                    return new Space(s1.x, Math.min(s1.y, s2.y), s1.z,
//                            s1.L, s1.W + s2.W, s1.H);
//                }
//            }
//            // 2. ניסיון מיזוג לאורך ציר X (אורך - Length)
//            // תנאי: s1 ו-s2 צמודים ב-X, ויש להם Y, Z, W, H זהים לחלוטין
//            if (Math.abs(s1.x + s1.L - s2.x) < eps || Math.abs(s2.x + s2.L - s1.x) < eps) {
//                if (Math.abs(s1.y - s2.y) < eps && Math.abs(s1.z - s2.z) < eps &&
//                        Math.abs(s1.W - s2.W) < eps && Math.abs(s1.H - s2.H) < eps) {
//
//                    return new Space(Math.min(s1.x, s2.x), s1.y, s1.z,
//                            s1.L + s2.L, s1.W, s1.H);
//                }
//            }
//            // 3. ניסיון מיזוג לאורך ציר Z (גובה - Height)
//            // תנאי: s1 ו-s2 צמודים ב-Z, ויש להם X, Y, L, W זהים לחלוטין
//            if (Math.abs(s1.z + s1.H - s2.z) < eps || Math.abs(s2.z + s2.H - s1.z) < eps) {
//                if (Math.abs(s1.x - s2.x) < eps && Math.abs(s1.y - s2.y) < eps &&
//                        Math.abs(s1.L - s2.L) < eps && Math.abs(s1.W - s2.W) < eps) {
//
//                    return new Space(s1.x, s1.y, Math.min(s1.z, s2.z),
//                            s1.L, s1.W, s1.H + s2.H);
//                }
//            }
//            return null; // לא ניתן למזג באופן בטוח
//        }
//        public List<Space> mergeSpaces(List<Space> spaces) {
//            if (spaces.size() < 2) return spaces;
//            List<Space> merged = new ArrayList<>(spaces);
//            boolean found;
//            do {
//                found = false;
//                for (int i = 0; i < merged.size(); i++) {
//                    for (int j = i + 1; j < merged.size(); j++) {
//                        Space s1 = merged.get(i);
//                        Space s2 = merged.get(j);
//                        Space union = tryCombine(s1, s2);
//                        if (union != null) {
//                            merged.remove(j);
//                            merged.remove(i);
//                            merged.add(union);
//                            found = true;
//                            break;
//                        }
//                    }
//                    if (found) break;
//                }
//            } while (found);
//            return merged;
//        }
//        public List<Space> cleanAndOptimizeSpaces(List<Space> spaces) {
//            List<Space> result = new ArrayList<>();
//            for (int i = 0; i < spaces.size(); i++) {
//                Space s1 = spaces.get(i);
//                // התעלמות מחללים קטנים מדי (רעש חישובי)
//                if (s1.L < 1.0 || s1.W < 1.0 || s1.H < 1.0) continue;
//
//                boolean isContained = false;
//                for (int j = 0; j < spaces.size(); j++) {
//                    if (i != j && isContained(s1, spaces.get(j))) {
//                        isContained = true;
//                        break;
//                    }
//                }
//                if (!isContained) {
//                    result.add(s1);
//                }
//            }
//            return result;
//        }
//
//        private boolean isContained(Space small, Space large) {
//            return small.x >= large.x && small.y >= large.y && small.z >= large.z &&
//                    (small.x + small.L) <= (large.x + large.L) &&
//                    (small.y + small.W) <= (large.y + large.W) &&
//                    (small.z + small.H) <= (large.z + large.H);
//        }
//
//    }
package com.noa.packer.algorithms;                     // החבילה שבה המחלקה נמצאת

import java.util.ArrayList;                           // יבוא רשימת ArrayList
import java.util.List;                                // יבוא הממשק List

/**
 * SpaceManager היברידי:
 * 1. פיצול חלל לשלושה חללים (Right / Front / Top) — כמו במאמר.
 * 2. מיזוג חללים צמודים — Left–Right ו–Front–Rear.
 * 3. ניקוי חללים — הסרת חללים קטנים / מוכלים.
 */
public class SpaceManager {

    // ============================================================
    // 1. פיצול חלל לשלושה חללי שארית — Right / Front / Top
    // ============================================================

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
                    L,                                 // כל האורך
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
                    L,                                 // כל האורך
                    W,                                 // כל הרוחב
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
                    W,                                 // כל הרוחב
                    H - h                              // הגובה שנותר
            ));
        }

        return residuals;                              // מחזירים את שלושת חללי השארית
    }
    public Space splitIntoResidualsNewLayerVI(Space current, Orientation o) {

        double x = current.x;                          // שומרים את נקודת ההתחלה X
        double y = current.y;                          // שומרים את נקודת ההתחלה Y
        double z = current.z;                          // שומרים את נקודת ההתחלה Z
        double L = current.L;                          // אורך החלל
        double W = current.W;                          // רוחב החלל
        double H = current.H;                          // גובה החלל
        double l = o.l;                                // אורך הארגז בכיוון שנבחר
        double w = o.w;                                // רוחב הארגז בכיוון שנבחר
        double h = o.h;                                // גובה הארגז בכיוון שנבחר

        Space residuals =new Space(
                    x + l,                             // מתחיל אחרי הארגז בציר X
                    y,                                 // אותו Y
                    z,                                 // אותו Z
                    L - l,                             // האורך שנותר
                    W,                                 // כל הרוחב
                    H                                  // כל הגובה
            );
        return residuals;                              // מחזירים את שלושת חללי השארית
    }
    // ============================================================
    // 2. מיזוג חללים — Left–Right ו–Front–Rear
    // ============================================================

    private Space tryCombine(Space s1, Space s2) {

        double eps = 1e-3;                             // טולרנס לשגיאות double

        // ---------------------------------------------------------
        // LEFT–RIGHT MERGE — מיזוג בציר Y (רוחב)
        // ---------------------------------------------------------

        boolean touchY =
                Math.abs(s1.y + s1.W - s2.y) < eps ||  // האם s1 נוגע ב–s2 בציר Y
                        Math.abs(s2.y + s2.W - s1.y) < eps;

        boolean sameX_Z_L_H =
                Math.abs(s1.x - s2.x) < eps &&         // אותו X
                        Math.abs(s1.z - s2.z) < eps &&         // אותו Z
                        Math.abs(s1.L - s2.L) < eps &&         // אותו אורך
                        Math.abs(s1.H - s2.H) < eps;           // אותו גובה

        if (touchY && sameX_Z_L_H) {                   // אם צמודים ב–Y וזהים בשאר
            return new Space(
                    s1.x,                              // X נשאר
                    Math.min(s1.y, s2.y),              // Y המאוחד
                    s1.z,                              // Z נשאר
                    s1.L,                              // אותו אורך
                    s1.W + s2.W,                       // רוחב מאוחד
                    s1.H                               // אותו גובה
            );
        }

        // ---------------------------------------------------------
        // FRONT–REAR MERGE — מיזוג בציר X (אורך)
        // ---------------------------------------------------------

        boolean touchX =
                Math.abs(s1.x + s1.L - s2.x) < eps ||  // האם s1 נוגע ב–s2 בציר X
                        Math.abs(s2.x + s2.L - s1.x) < eps;

        boolean sameY_Z_W_H =
                Math.abs(s1.y - s2.y) < eps &&         // אותו Y
                        Math.abs(s1.z - s2.z) < eps &&         // אותו Z
                        Math.abs(s1.W - s2.W) < eps &&         // אותו רוחב
                        Math.abs(s1.H - s2.H) < eps;           // אותו גובה

        if (touchX && sameY_Z_W_H) {                   // אם צמודים ב–X וזהים בשאר
            return new Space(
                    Math.min(s1.x, s2.x),              // X המאוחד
                    s1.y,                              // Y נשאר
                    s1.z,                              // Z נשאר
                    s1.L + s2.L,                       // אורך מאוחד
                    s1.W,                              // אותו רוחב
                    s1.H                               // אותו גובה
            );
        }

        return null;                                   // לא ניתן למזג
    }

    public List<Space> mergeSpaces(List<Space> spaces) {

        if (spaces.size() < 2) return spaces;          // אם יש רק חלל אחד — אין מה למזג

        List<Space> merged = new ArrayList<>(spaces);  // יוצרים עותק כדי לא לפגוע במקור
        boolean found;                                 // דגל — האם מצאנו מיזוג

        do {
            found = false;                             // מתחילים סיבוב חדש
            for (int i = 0; i < merged.size(); i++) {
                for (int j = i + 1; j < merged.size(); j++) {

                    Space s1 = merged.get(i);          // חלל ראשון
                    Space s2 = merged.get(j);          // חלל שני

                    Space union = tryCombine(s1, s2);  // ניסיון מיזוג

                    if (union != null) {               // אם הצליח
                        merged.remove(j);              // מסירים את השני
                        merged.remove(i);              // מסירים את הראשון
                        merged.add(union);             // מוסיפים את המאוחד
                        found = true;                  // מסמנים שמצאנו מיזוג
                        break;                         // יוצאים מהלולאה
                    }
                }
                if (found) break;                      // מתחילים סיבוב חדש
            }
        } while (found);                               // ממשיכים עד שאין יותר מיזוגים

        return merged;                                 // מחזירים את הרשימה אחרי מיזוגים
    }
    public List<Space> SpacesVR(List<Space> spaces, double layerx) {
        List<Space> result = new ArrayList<>();        // רשימת תוצאה
        for (Space s : spaces) {
             s.L =s.L +layerx;
             result.add(s);
        }
        return result;
    }
    // ============================================================
    // 3. ניקוי חללים — הסרת חללים קטנים / מוכלים
    // ============================================================

    public List<Space> cleanAndOptimizeSpaces(List<Space> spaces) {

        List<Space> result = new ArrayList<>();        // רשימת תוצאה

        for (int i = 0; i < spaces.size(); i++) {

            Space s1 = spaces.get(i);                  // חלל נוכחי

            if (s1.L < 1e-4 || s1.W < Global.MIN_DIM || s1.H < Global.MIN_DIM)
                continue;                              // מסירים חללים קטנים מדי

            boolean contained = false;                 // דגל — האם החלל מוכל

            for (int j = 0; j < spaces.size(); j++) {
                if (i == j) continue;                  // לא משווים לעצמו

                Space s2 = spaces.get(j);              // חלל להשוואה

                if (isContained(s1, s2)) {             // אם s1 מוכל בתוך s2
                    contained = true;                  // מסמנים
                    break;                             // לא צריך לבדוק עוד
                }
            }

            if (!contained) result.add(s1);            // אם לא מוכל — מוסיפים
        }

        return result;                                 // מחזירים את הרשימה אחרי ניקוי
    }

    private boolean isContained(Space small, Space large) {

        return small.x >= large.x &&                   // small מתחיל אחרי large בציר X
                small.y >= large.y &&                   // small מתחיל אחרי large בציר Y
                small.z >= large.z &&                   // small מתחיל אחרי large בציר Z
                small.x + small.L <= large.x + large.L &&
                small.y + small.W <= large.y + large.W &&
                small.z + small.H <= large.z + large.H; // small נגמר לפני large
    }
}
