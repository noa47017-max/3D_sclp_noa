package com.noa.packer.algorithms;
public class PheromoneNode {
    int cargoId;              // סוג המטען
    int d;              // כיוון ההעמסה
    double[] leftBackBottom; // xB, yB, zB
    double xT, yT, zT; // קואורדינטות פינה עליונה (אחרי התאמת תנוחה)
    double vr;
    double concentration;        // נפח שנותר בשכבה (Remaining Space)

    public PheromoneNode(int cargoId, int d, double xB, double yB, double zB,
                            double xT, double yT, double zT, double vr,  double concentration) {
        this.cargoId = cargoId;
        this.d = d;
        this.leftBackBottom = new double[]{xB, yB, zB};
        this.xT = xT; this.yT = yT; this.zT = zT;
        this.vr = vr;
        this.concentration = concentration;
    }
    // בנאי ממוקד עבור עדכונים מקומיים - ללא מיקום קשיח
    public PheromoneNode(int cargoId, int direction, double initialConcentration) {
        this.cargoId = cargoId;
        this.d = direction;
        this.concentration = initialConcentration;
        this.vr = Double.MAX_VALUE;
        this.leftBackBottom = new double[]{-1.0, -1.0, -1.0};
        this.xT = -1.0; this.yT = -1.0; this.zT = -1.0;

    }
    public int getCargoId(){return cargoId;}
    public int getD(){return d;}
    public double getVr(){return vr;}
    public double getConcentration(){return concentration;}
    public void setConcentration(double e) {this.concentration = e;}
    public void setDirection(int direction) { this.d = direction; }
    public void setLeftBackBottom(double[] es) { this.leftBackBottom = es;}
    public void setVR(double currentVR) {this.vr = currentVR;}

}

