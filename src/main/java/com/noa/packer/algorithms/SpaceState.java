package com.noa.packer.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpaceState {
    List<Space> currentLayer;
    List<Space> nextLayers;
    List<Space> retrySpaces;
    SpaceManager spaceManager;
    double layerXEnd;
    List<Space> pendingNew;

    public SpaceState() {
        this.currentLayer = new ArrayList<>();
        this.nextLayers = new ArrayList<>();
        this.retrySpaces = new ArrayList<>();
        this.spaceManager = new SpaceManager();
        this.layerXEnd = -1;
        this.pendingNew = new ArrayList<>();
    }
    // מיון
    public void sortSpacesByPosition() {
        currentLayer.sort(Comparator
                .comparingDouble((Space s) -> s.z)
                .thenComparingDouble(s -> s.y)
                .thenComparingDouble(s -> s.x));
    }

    // עדכון חללים לאחר הנחה
    public void updateSpacesAfterPlacement(double xMin, double yMin, double zMin,
                                           double xMax, double yMax, double zMax) {
        // 1. חיתוך — כולם
        currentLayer = spaceManager.updateSpacesAfterPlacement(currentLayer, xMin, yMin, zMin, xMax, yMax, zMax);
        nextLayers = spaceManager.updateSpacesAfterPlacement(nextLayers, xMin, yMin, zMin, xMax, yMax, zMax);
        retrySpaces = spaceManager.updateSpacesAfterPlacement(retrySpaces, xMin, yMin, zMin, xMax, yMax, zMax);

        // 2. מיזוג רק החדשים מול הקיימים
        if (!pendingNew.isEmpty()) {
            currentLayer = spaceManager.mergeNewInto(currentLayer, pendingNew);
            pendingNew.clear();
        }
    }




   // מיון לפי נפח
    public void sortSpacesByVolume(List<Space> spaces) {
        spaces.sort((a, b) -> Double.compare(b.getVolume(), a.getVolume()));
    }
    //הוספת 3 החללים לאחר הנחת ארגז
    public void addResidualSpaces(Space space, Orientation orientation, boolean isNewLayer) {
        double x = space.x, y = space.y, z = space.z;
        double L = space.L, W = space.W, H = space.H;
        double l = orientation.l, w = orientation.w, h = orientation.h;
        pendingNew.clear();
        // Top space
        if (H - h > Global.EPSILON) {
            pendingNew.add(new Space(x, y, z + h, l, w, H - h));
        }
        // Side space
        if (W - w > Global.EPSILON) {
            pendingNew.add(new Space(x, y + w, z, l, W - w, H));
        }
        // Front space
        if (L - l > Global.EPSILON) {
            double frontX = x + l;
            double frontL = L - l;

            if (isNewLayer) {
                layerXEnd = frontX;
                nextLayers.add(new Space(frontX, y, z, frontL, W, H));
            } else if (frontX >= layerXEnd - Global.EPSILON) {
                // כולו מעבר לגבול → שכבה הבאה
                nextLayers.add(new Space(frontX, y, z, frontL, W, H));
            } else if (frontX + frontL > layerXEnd + Global.EPSILON) {
                // חוצה → פיצול
                double insideL = layerXEnd - frontX;
                double outsideL = frontX + frontL - layerXEnd;
                pendingNew.add(new Space(frontX, y, z, insideL, W, H));
                nextLayers.add(new Space(layerXEnd, y, z, outsideL, W, H));
            } else {
                // כולו בפנים
                pendingNew.add(new Space(frontX, y, z, frontL, W, H));
            }
        }
    }

    public List<Space> collectAllRemainingSpaces() {
        List<Space> all = new ArrayList<>();
        all.addAll(currentLayer);
        all.addAll(nextLayers);
        all.addAll(retrySpaces);
        sortSpacesByVolume(all);
        return all;
    }
}
