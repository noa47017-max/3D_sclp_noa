package com.noa.packer.algorithms;

import java.util.concurrent.ConcurrentSkipListMap;

public class PruningManager {
    private final ConcurrentSkipListMap<String, Double> pruningMatrix;

    public PruningManager() {
        this.pruningMatrix = new ConcurrentSkipListMap<>();
    }
    public boolean shouldPrune(String stateWord, double currentCumVR) {
        Double bestCumVR = pruningMatrix.get(stateWord);
        double tolerance = 1.15;

        return bestCumVR != null && currentCumVR > (bestCumVR * tolerance);
        //                                      ↑ יותר VR = יותר בזבוז = גזום
    }

    public void updateMatrix(String stateWord, double currentCumVR) {
        pruningMatrix.merge(stateWord, currentCumVR, Math::min);
        //                                           ↑ שומר את הנמוך ביותר
    }

    public void reset() { pruningMatrix.clear(); }
}