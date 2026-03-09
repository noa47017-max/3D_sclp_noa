package com.noa.packer.models;

import org.bson.types.ObjectId;

public class Container {

    private ObjectId id;
    private ObjectId userId;
    private double   length;
    private double   width;
    private double   height;
    private double   volume;     // מחושב אוטומטית

    public Container() {}

    public Container(ObjectId userId, double length, double width, double height) {
        this.userId    = userId;
        this.length    = length;
        this.width     = width;
        this.height    = height;
        this.volume    = length * width * height;
    }

    // Getters & Setters
    public ObjectId getId()                  { return id; }
    public void     setId(ObjectId id)       { this.id = id; }
    public ObjectId getUserId()              { return userId; }
    public void     setUserId(ObjectId v)    { this.userId = v; }   
    public double   getLength()             { return length; }
    public void     setLength(double v)      { this.length = v; recalculateVolume(); }
    public double   getWidth()              { return width; }
    public void     setWidth(double v)       { this.width = v; recalculateVolume();}
    public double   getHeight()             { return height; }
    public void     setHeight(double v)      { this.height = v; recalculateVolume();}
    public double   getVolume()             { return volume; }
    public void     setVolume(double v)      { this.volume = v; }
 
    // עדכון נפח אוטומטי
    public void recalculateVolume() {
        this.volume = this.length * this.width * this.height;
    }
}
