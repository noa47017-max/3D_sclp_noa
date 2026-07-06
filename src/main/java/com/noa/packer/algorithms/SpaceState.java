package com.noa.packer.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpaceState {
    List<Space> currentLayer; // חללים בשכבה נוכחית - VU
    List<Space> nextLayers;// חלל לשכבה הבאה - VI
    List<Space> retrySpaces;// חללים קטנים למיזוג לשכבות הבאות - VR
    double layerXEnd;
    double sumVR;

    public SpaceState() {
        this.currentLayer = new ArrayList<>();
        this.nextLayers = new ArrayList<>();
        this.retrySpaces = new ArrayList<>();
        this.layerXEnd = -1;
        this.sumVR = 0;

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
        currentLayer = SpaceManager.updateSpacesAfterPlacement(currentLayer, xMin, yMin, zMin, xMax, yMax, zMax);
        retrySpaces = SpaceManager.updateSpacesAfterPlacement(retrySpaces, xMin, yMin, zMin, xMax, yMax, zMax);

        currentLayer = SpaceManager.mergeSpaces(currentLayer);
    }



    //הוספת 3 החללים לאחר הנחת ארגז
    public void addResidualSpaces(Space space, Orientation orientation, boolean isNewLayer) {

        double x = space.x, y = space.y, z = space.z;
        double L = space.L, W = space.W, H = space.H;
        double l = orientation.l, w = orientation.w, h = orientation.h;
        // Top space
        if (H - h > Global.EPSILON) {
            currentLayer.add(new Space(x, y, z + h, l, w, H - h));
        }
        // Side space
        if (W - w > Global.EPSILON) {
            currentLayer.add(new Space(x, y + w, z, l, W - w, H));
        }
        // Front space
        if (L - l > Global.EPSILON) {
            double frontX = x + l;
            double frontL = L - l;

            if (isNewLayer) {
                layerXEnd = frontX;
                nextLayers.add(new Space(frontX, y, z, frontL, W, H));
                currentLayer = mergeRetryInto();
                retrySpaces.clear();
            } else if (frontX >= layerXEnd - Global.EPSILON) {
                // כולו מעבר לגבול → שכבה הבאה
            } else if (frontX + frontL > layerXEnd + Global.EPSILON) {
                // חוצה → פיצול
                double insideL = layerXEnd - frontX;
                currentLayer.add(new Space(frontX, y, z, insideL, W, H));
            } else {
                // כולו בפנים
                currentLayer.add(new Space(frontX, y, z, frontL, W, H));
            }
        }
    }
    private List<Space> mergeRetryInto() {

        List<Space> result = new ArrayList<>(this.currentLayer);
        for (Space retry : this.retrySpaces) {
            boolean canMerge = false;
            for (Space existing : result) {
                if (SpaceManager.canMergeLeftRight(retry, existing)
                        || SpaceManager.canMergeFrontRear(retry, existing)
                        || SpaceManager.canMergeTopBottom(retry, existing)
                        || SpaceManager.tryOverlapMerge(retry, existing) != null) {
                    canMerge = true;
                    break;
                }
            }
            if (canMerge) {
                result.add(retry);
            }
            else {
                sumVR+=retry.getVolume();
            }
        }
        return SpaceManager.mergeSpaces(result);
    }
}
