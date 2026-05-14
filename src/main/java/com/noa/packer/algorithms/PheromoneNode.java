package com.noa.packer.algorithms;
public class PheromoneNode {
    int cargoId;              // סוג המטען
    int d;              // כיוון ההעמסה
    double[] leftBackBottom; // xB, yB, zB
    double xT, yT, zT;  // קואורדינטות פינה עליונה (אחרי התאמת תנוחה)
    double vr;  
    double concentration;        // נפח שנותר בשכבה (Remaining Space)
    int ta;

    public PheromoneNode(int cargoId, int d, double xB, double yB, double zB, 
                            double xT, double yT, double zT, double vr, int ta, double concentration) {
        this.cargoId = cargoId;
        this.d = d;
        this.leftBackBottom = new double[]{xB, yB, zB};
        this.xT = xT; this.yT = yT; this.zT = zT;
        this.vr = vr;
        this.concentration = concentration;
        this.ta = ta;
    }
    public int getCargoId(){return cargoId;}
    public int getD(){return d;}
    public double getVr(){return vr;}
    public double getConcentration(){return concentration;}
    public void setConcentration(double e) {this.concentration = e;}  
    public void setDirection(int direction) { this.d = direction; }
    public void setLeftBackBottom(double[] es) { this.leftBackBottom = es;}
    public void setVR(double currentVR) {this.vr = currentVR;}
    public int getTa(){return this.ta;}
    public void setTa(int ta) { this.ta = ta; }

}

