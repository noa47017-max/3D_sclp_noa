
package com.noa.packer;
import java.util.ArrayList;
import java.util.List;
public class CargoType {
    public int id;
    public double l, w, h;
    public int quantity;
    public int totalQuantity; // הכמות המקורית מההזמנה
    private List<Integer> allowedOrientations;


    public CargoType(int id, double l, double w, double h, int q,List<Integer> allowedOrientations) {
        this.id = id; this.l = l; this.w = w; this.h = h; this.quantity = q; this.allowedOrientations = allowedOrientations;
        this.totalQuantity = q;
    }
    public CargoType(CargoType c) {
        this.id = c.id; this.l = c.l; this.w = c.w; this.h = c.h; this.quantity = c.quantity; this.allowedOrientations = c.allowedOrientations;
        this.totalQuantity = c.quantity;
    }

    public double getVolume()//החזרת נפח הארגז
    { return l * w * h; }


    public boolean canFit(Space space) {
        // בדיקה גיאומטרית בסיסית מול ממדי המרחב
        return this.getMinDimension() <= Math.min(space.L, Math.min(space.W, space.H));
    }

    private double getMinDimension() {
        return Math.min(this.l, this.w);
    }
    public List<Orientation> getPossibleOrientations() {
        List<Orientation> available = new ArrayList<>();

        Orientation[] allSix = {
                new Orientation(1, l, h, w), // משוואה 2.1
                new Orientation(2, l, w, h), // משוואה 2.2
                new Orientation(3, w, l, h), // משוואה 2.3
                new Orientation(4, h, w, l), // משוואה 2.4
                new Orientation(5, l, w, h), // משוואה 2.5
                new Orientation(6, w, h, l)  // משוואה 2.6
        };

        // סינון רק לפי מה שהוגדר במשתנה allowedOrientations
        for (int index : allowedOrientations)
             available.add(allSix[index]);
        return available;
    }
    public void decreaseQuantity() {
        if (this.quantity > 0) {
            this.quantity = this.quantity - 1; // או בקיצור: quantity--;
        }
    }
}
