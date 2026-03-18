package com.noa.packer.dto;

import com.noa.packer.models.PackingResult;
import com.noa.packer.models.PackingResult.PlacedBoxData;

import java.util.Date;
import java.util.List;

public class PackingResultDTO {

    private String             resultId;
    private String             containerId;
    private double             totalCargoVolume;
    private double             vur;
    private List<PlacedBoxData> placements;
    private Date               createdAt;

    public PackingResultDTO() {}

    public PackingResultDTO(PackingResult result) {
        this.resultId         = result.getId().toString();
        this.containerId      = result.getContainerId().toString();
        this.totalCargoVolume = result.getTotalCargoVolume();
        this.vur              = result.getVur();
        this.placements       = result.getPlacements();
        this.createdAt        = result.getCreatedAt();
    }

    public String              getResultId()                      { return resultId; }
    public void                setResultId(String v)              { this.resultId = v; }
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
}