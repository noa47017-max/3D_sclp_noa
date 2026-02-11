import java.util.ArrayList;
import java.util.List;

public class SpaceManager {

    /**
     * פונקציה שמקבלת חלל שהונח בו ארגז, ומייצרת חללים חדשים תוך ניסיון למזג אותם
     */
    public List<Space> splitAndMerge(Space currentSpace, Orientation bestOri) {
        List<Space> newSpaces = new ArrayList<>();

        // 1. יצירת שלושת החללים הבסיסיים (כמו שעשינו ב-Space)
        Space top = currentSpace.getTopResidual(bestOri);
        Space right = currentSpace.getRightResidual(bestOri);
        Space front = currentSpace.getFrontResidual(bestOri);

        // הוספה לרשימה זמנית רק אם יש להם נפח
        if (top.getVolume() > 0) newSpaces.add(top);
        if (right.getVolume() > 0) newSpaces.add(right);
        if (front.getVolume() > 0) newSpaces.add(front);

        // 2. לוגיקת המיזוג (Merging Logic)
        // כאן אנחנו בודקים אם חללים חדשים יכולים להתאחד עם חללים קיימים
        // במידה והם חופפים או צמודים לחלוטין באחת הפאות.
        return mergeSpaces(newSpaces);
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

    private Space tryCombine(Space s1, Space s2) {
        // בדיקת מיזוג על ציר X (גובה ורוחב זהים)
        if (s1.y == s2.y && s1.z == s2.z && s1.W == s2.W && s1.H == s2.H) {
            if (s1.x + s1.L == s2.x) return new Space(s1.x, s1.y, s1.z, s1.L + s2.L, s1.W, s1.H);
            if (s2.x + s2.L == s1.x) return new Space(s2.x, s2.y, s2.z, s2.L + s1.L, s1.W, s1.H);
        }
        // בדיקת מיזוג על ציר Y (אורך וגובה זהים)
        if (s1.x == s2.x && s1.z == s2.z && s1.L == s2.L && s1.H == s2.H) {
            if (s1.y + s1.W == s2.y) return new Space(s1.x, s1.y, s1.z, s1.L, s1.W + s2.W, s1.H);
            if (s2.y + s2.W == s1.y) return new Space(s1.x, s2.y, s1.z, s1.L, s2.W + s1.W, s1.H);
        }
        return null;
    }
}
