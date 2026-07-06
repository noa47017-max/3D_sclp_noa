package com.noa.packer.algorithms;

import java.util.*;

/**
 * כל ה-state המשותף של ריצת אופטימיזציה אחת.
 * נוצר פעם אחת ב-HybridLoadingSystem, משותף לכל הנמלים באותה ריצה,
 * ומושלך בסוף הריצה. ריצות שונות = context-ים שונים = בידוד מלא.
 */
public class LoadingContext {
    private final PheromoneManager pheromoneManager;
    private final PruningManager pruningManager;
    private final double minDim;

    public LoadingContext(List<CargoType> cargoTypes) {
        this.pheromoneManager = new PheromoneManager();
        this.pruningManager = new PruningManager();
        this.minDim = computeMinDim(cargoTypes);
    }

    private static double computeMinDim(List<CargoType> cargoTypes) {
        double min = Double.MAX_VALUE;
        for (CargoType c : cargoTypes) {
            min = Math.min(min, Math.min(c.l, Math.min(c.w, c.h)));
        }
        return min;
    }

    public PheromoneManager pheromones() { return pheromoneManager; }
    public PruningManager pruning() { return pruningManager; }
    public double minDim() { return minDim; }
}
