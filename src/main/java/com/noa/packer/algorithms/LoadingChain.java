package com.noa.packer.algorithms;

import com.noa.packer.models.Container;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class LoadingChain {
    // רשימת הארגזים שהונחו בשרשרת זו
    private List<PlacedBoxInfo> steps = new ArrayList<>();
    private double totalCargoVolume = 0;
    private double containerVolume;
//    private double cumulativeVR = 0;
    private ObjectId containerID;

    public LoadingChain(Container container) {
        this.containerID = container.getId();
        this.containerVolume = container.getVolume();
    }

    public void addStep(CargoType cargo, Orientation o, Space s, String stateKey) {
        double[] b = {s.x, s.y, s.z};
        double[] t = {s.x + o.l, s.y + o.w, s.z + o.h};
         // חישוב הremainingSpace
        double vrTop   = o.l * o.w * Math.max(0, s.H - o.h);       // l×w, לא L×W
        double vrSide  = o.l * Math.max(0, s.W - o.w) * s.H;       // l, לא L
        double vrFront = Math.max(0, s.L - o.l) * s.W * s.H;       // זה נשאר — תואם
        double vrTotal = vrTop + vrSide + vrFront;
        //הוספת הצעד לרשימה
        steps.add(new PlacedBoxInfo(cargo.id, o.id, b, t, vrTotal, stateKey));
//        cumulativeVR += vrTotal;
        totalCargoVolume += (o.l * o.w * o.h);

//        return cumulativeVR;
    }

    public class PlacedBoxInfo {
        public int cargoType; // k
        public int direction; // d
        public double[] leftBackBottom; // xB, yB, zB
        public double[] rightFrontTop;  // xT, yT, zT
        public double remainingSpace;   // VRi
        public String stateKey;

        public PlacedBoxInfo(int k, int d, double[] b, double[] t, double vr, String stateKey) {
            this.cargoType = k;
            this.direction = d;
            this.leftBackBottom = b;
            this.rightFrontTop = t;
            this.remainingSpace = vr;
            this.stateKey = stateKey;
        }
    }

    public List<PlacedBoxInfo> getSteps() {
        return steps;
    }
    public double getVUR() {
        return totalCargoVolume / containerVolume;
    }
    public ObjectId getContainerID() {
        return containerID;
    }
//    public double getCumulativeVR() { return cumulativeVR; }
    public double getTotalCargoVolume() {
        return totalCargoVolume;
    }
}
