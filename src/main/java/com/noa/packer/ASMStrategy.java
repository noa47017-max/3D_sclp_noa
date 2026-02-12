package com.noa.packer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
public class ASMStrategy {
    private PheromoneManager manager;
    private Random random = new Random();

    public ASMStrategy(PheromoneManager manager) {
        this.manager = manager;
    }

    public CargoType selectCargo(Space space, List<CargoType> cargoes, String stateKey) {
        Map<CargoType, Double> weights = new HashMap<>();
        double totalWeight = 0;

        for (CargoType cargo : cargoes) {
            if (cargo.quantity > 0 && cargo.canFit(space)) {
                // התיקון הקריטי: מפתח שמשלב מצב + סוג ארגז

                // שליפת פרומון ספציפי להחלטה הזו
                double pheromone = manager.getPheromone(stateKey,cargo.id);

                // ערך היוריסטי (נפח)
                double visibility = cargo.getVolume();

                // חישוב משקל (אלפא=1, בטא=3)
                double w = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());

                weights.put(cargo, w);
                totalWeight += w;
            }
        }

        return rouletteWheelSelection(weights, totalWeight);
    }

    private CargoType rouletteWheelSelection(Map<CargoType, Double> weights, double totalWeight) {
        if (totalWeight <= 0 || weights.isEmpty()) return null;

        double threshold = random.nextDouble() * totalWeight;
        double currentSum = 0.0;

        for (Map.Entry<CargoType, Double> entry : weights.entrySet()) {
            currentSum += entry.getValue();
            if (currentSum >= threshold) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }
}