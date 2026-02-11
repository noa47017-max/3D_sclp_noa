public class OrientationOptimizer {
    public Orientation getBestOrientation(Space omega, CargoType cargo) {
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
        return best;
    }

}
