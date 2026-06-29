// dto/HistoryItemDTO.java
package com.noa.packer.dto;

import java.util.Date;

public class HistoryItemDTO {

    private String id;
    private double vur;
    private Date   createdAt;
    private String containerSize;  // "587 × 233 × 220"

    public HistoryItemDTO(String id, double vur, Date createdAt, String containerSize) {
        this.id            = id;
        this.vur           = vur;
        this.createdAt     = createdAt;
        this.containerSize = containerSize;
    }

    public String getId()            { return id; }
    public double getVur()           { return vur; }
    public Date   getCreatedAt()     { return createdAt; }
    public String getContainerSize() { return containerSize; }
}