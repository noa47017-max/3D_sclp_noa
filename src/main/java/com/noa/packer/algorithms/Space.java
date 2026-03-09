package com.noa.packer.algorithms;
public class Space {
    public double x, y, z; // קואורדינטות הפינה השמאלית-אחורית-תחתונה [4]
    public double L, W, H; // ממדי החלל

    public Space(double x, double y, double z, double l, double w, double h) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.L = l;
        this.W = w;
        this.H = h;
    }

    public double getVolume() { return L * W * H; }


    // יצירת חלל עליון (Top)
    public Space getTopResidual(Orientation o) {
        return new Space(x, y, z + o.h, o.l, o.w, H - o.h);
    }

    // יצירת חלל ימני (Right)
    public Space getRightResidual(Orientation o) {
        return new Space(x, y + o.w, z, o.l, W - o.w, H);
    }

    // יצירת חלל קדמי (Front)
    public Space getFrontResidual(Orientation o) {
        return new Space(x + o.l, y, z, L - o.l, W, H);
    }
}