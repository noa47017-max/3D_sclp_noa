package com.noa.packer.dto;

import com.noa.packer.models.PackingResult;
import com.noa.packer.models.PackingResult.PlacedBoxData;

import java.util.Date;
import java.util.List;

public class PackingResultDTO {

    private String             containerId;
    private double             containerL;
    private double             containerW;
    private double             containerH;
    private double             totalCargoVolume;
    private double             vur;
    private List<PlacedBoxData> placements;
    private Date               createdAt;


    public PackingResultDTO(PackingResult result, double l, double w, double h) {
        this.containerId      = result.getContainerId() != null ? result.getContainerId().toHexString() : null;
        this.totalCargoVolume = result.getTotalCargoVolume();
        this.vur              = result.getVur();
        this.placements       = result.getPlacements();
        this.createdAt        = result.getCreatedAt();
        this.containerL       = l;
        this.containerW       = w;
        this.containerH       = h;
    }


    public String              getContainerId()                   { return containerId; }
    public void                setContainerId(String v)           { this.containerId = v; }
    public double              getTotalCargoVolume()              { return totalCargoVolume; }
    public void                setTotalCargoVolume(double v)      { this.totalCargoVolume = v; }
    public double              getVur()                           { return vur; }
    public void                setVur(double v)                   { this.vur = v; }
    public List<PlacedBoxData> getPlacements()                    { return placements; }
    public void                setPlacements(List<PlacedBoxData> v) { this.placements = v; }
    public Date                getCreatedAt()                     { return createdAt; }
    public void                setCreatedAt(Date v)               { this.createdAt = v; }
    public double getContainerL() { return containerL; }
    public double getContainerW() { return containerW; }
    public double getContainerH() { return containerH; }
}