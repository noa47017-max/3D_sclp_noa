package com.noa.packer.models;

import org.bson.types.ObjectId;
import java.util.Date;
import java.util.List;

public class PackingResult {

    private ObjectId id;
    private ObjectId userId;
    private ObjectId containerId;
    private double   totalCargoVolume;
    private double   vur;
    private List<PlacedBoxData> placements;
    private Date     createdAt;

    public PackingResult() {}

    public PackingResult(ObjectId userId, ObjectId containerId,
                         double containerVolume, double totalCargoVolume,
                         List<PlacedBoxData> placements) {
        this.userId           = userId;
        this.containerId      = containerId;
        this.totalCargoVolume = totalCargoVolume;
        this.vur              = totalCargoVolume / containerVolume;
        this.placements       = placements;
        this.createdAt        = new Date();
    }

    // ── מחלקת עזר – רק השדות הנחוצים ────────────
    public static class PlacedBoxData {
        private int      cargoType;
        private int      direction;
        private double[] leftBackBottom;
        private double[] rightFrontTop;

        public PlacedBoxData() {}

        public PlacedBoxData(int cargoType, int direction,
                             double[] leftBackBottom, double[] rightFrontTop) {
            this.cargoType      = cargoType;
            this.direction      = direction;
            this.leftBackBottom = leftBackBottom;
            this.rightFrontTop  = rightFrontTop;
        }

        public int      getCargoType()              { return cargoType; }
        public void     setCargoType(int v)         { this.cargoType = v; }
        public int      getDirection()              { return direction; }
        public void     setDirection(int v)         { this.direction = v; }
        public double[] getLeftBackBottom()         { return leftBackBottom; }
        public void     setLeftBackBottom(double[] v) { this.leftBackBottom = v; }
        public double[] getRightFrontTop()          { return rightFrontTop; }
        public void     setRightFrontTop(double[] v)  { this.rightFrontTop = v; }
    }

    // Getters & Setters
    public ObjectId getId()                              { return id; }
    public void     setId(ObjectId id)                   { this.id = id; }
    public ObjectId getUserId()                          { return userId; }
    public void     setUserId(ObjectId userId)           { this.userId = userId; }
    public ObjectId getContainerId()           { return containerId; }
    public void     setContainerId(ObjectId v) { this.containerId = v; }
    public double   getTotalCargoVolume()                { return totalCargoVolume; }
    public void     setTotalCargoVolume(double v)        { this.totalCargoVolume = v; }
    public double   getVur()                             { return vur; }
    public void     setVur(double v)                     { this.vur = v; }
    public List<PlacedBoxData> getPlacements()           { return placements; }
    public void     setPlacements(List<PlacedBoxData> p) { this.placements = p; }
    public Date     getCreatedAt()                       { return createdAt; }
    public void     setCreatedAt(Date v)                 { this.createdAt = v; }
}