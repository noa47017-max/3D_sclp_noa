
package com.noa.packer.algorithms;
import java.util.ArrayList;
import java.util.List;
public class CargoType {
    public int id;
    public double l, w, h;
    public int quantity;
    public int totalQuantity; // הכמות המקורית מההזמנה
    private List<Orientation> allowedOrientations;


    public CargoType(int id, double l, double w, double h, int q,List<Integer> allowedOrientations) {
        this.id = id; this.l = l; this.w = w; this.h = h; this.quantity = q;
        this.allowedOrientations = getPossibleOrientations(allowedOrientations);
        this.totalQuantity = q;
    }
    public CargoType(CargoType c) {
        this.id = c.id; this.l = c.l; this.w = c.w; this.h = c.h; this.quantity = c.quantity; this.allowedOrientations = c.allowedOrientations;
        this.totalQuantity = c.totalQuantity;
    }

    public double getVolume()//החזרת נפח הארגז
    { return l * w * h; }


    public boolean canFit(Space space) {
        return !getAllowedOrientations().stream()
                .filter(ori -> ori.l <= space.L + 1e-9
                        && ori.w <= space.W + 1e-9
                        && ori.h <= space.H + 1e-9)
                .findFirst()
                .isEmpty();
    }
    public List<Orientation> getAllowedOrientations(){
        return this.allowedOrientations;
    }
    public List<Orientation> getPossibleOrientations(List<Integer> allowedOrientations) {
        List<Orientation> available = new ArrayList<>();

        Orientation[] allSix = {
                new Orientation(0, l, w, h),  // רגיל
                new Orientation(1, l, h, w),  // סיבוב על X
                new Orientation(2, w, l, h),  // סיבוב על Z
                new Orientation(3, w, h, l),  // סיבוב על Z+X
                new Orientation(4, h, l, w),  // סיבוב על Y
                new Orientation(5, h, w, l)   // סיבוב על Y+Z
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
    public int getQuantity() { return quantity; }
    public int getTotalQuantity() { return totalQuantity; }
}
