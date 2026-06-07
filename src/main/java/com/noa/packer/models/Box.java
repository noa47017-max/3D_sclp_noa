package com.noa.packer.models;

import org.bson.types.ObjectId;
import java.util.List;

public class Box {

    private ObjectId    id;
    private ObjectId    containerId;;
    private double      l;
    private double      w;
    private double      h;
    private int         quantity;
    private List<Integer> allowedOrientations;

    public Box() {}

    public Box(ObjectId containerId, double l, double w, double h,
               int quantity, List<Integer> allowedOrientations) {
        this.containerId            = containerId;
        this.l                   = l;
        this.w                   = w;
        this.h                   = h;
        this.quantity            = quantity;
        this.allowedOrientations = allowedOrientations;
    }

    // Getters & Setters
    public ObjectId      getId()                              { return id; }
    public void          setId(ObjectId id)                   { this.id = id; }
    public ObjectId      getcontainerId()                          { return containerId; }
    public void          setcontainerId(ObjectId v)                { this.containerId = v; }
    public double        getL()                               { return l; }
    public void          setL(double v)                       { this.l = v; }
    public double        getW()                               { return w; }
    public void          setW(double v)                       { this.w = v; }
    public double        getH()                               { return h; }
    public void          setH(double v)                       { this.h = v; }
    public int           getQuantity()                        { return quantity; }
    public void          setQuantity(int v)                   { this.quantity = v; }
    public List<Integer> getAllowedOrientations()             { return allowedOrientations; }
    public void          setAllowedOrientations(List<Integer> v) { this.allowedOrientations = v; }
}
