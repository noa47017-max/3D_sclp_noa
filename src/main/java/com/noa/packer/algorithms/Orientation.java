package com.noa.packer.algorithms;
public class Orientation {
    public int id;
//    public int type;// 1 עד 6
    public double l, w, h;
    public Orientation(int id, double l, double w, double h) {
        this.id = id;
//        this.type=type;
        this.l = l; this.w = w; this.h = h;
    }
    public String toShortString() {
        return String.format("%d", id);
    }
}
