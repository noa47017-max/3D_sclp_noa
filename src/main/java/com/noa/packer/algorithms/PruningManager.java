package com.noa.packer.algorithms;

import java.util.concurrent.ConcurrentSkipListMap;

public class PruningManager {
    private final ConcurrentSkipListMap<String, Double> pruningMatrix;
    private final ConcurrentSkipListMap<String, Integer> sampleCount;

    public PruningManager() {
        this.pruningMatrix = new ConcurrentSkipListMap<>();
        this.sampleCount = new ConcurrentSkipListMap<>();
    }
    public boolean shouldPrune(String stateWord, double currentCumVR) {
        Integer count = sampleCount.get(stateWord);
        if (count == null || count < 5) return false;  // מדגם קטן מדי
        Double bestCumVR = pruningMatrix.get(stateWord);
        double tolerance = 1.20;

        return bestCumVR != null && currentCumVR > (bestCumVR * tolerance ); //   יותר VR = יותר בזבוז = גזום
    }

    // שומר את הנמוך ביותר
    public void updateMatrix(String stateWord, double currentCumVR) {
        pruningMatrix.merge(stateWord, currentCumVR, Math::min);
        sampleCount.merge(stateWord, 1, Integer::sum);

    }

    public void reset() { pruningMatrix.clear();
    }
}