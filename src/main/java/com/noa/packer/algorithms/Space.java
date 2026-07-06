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
}

