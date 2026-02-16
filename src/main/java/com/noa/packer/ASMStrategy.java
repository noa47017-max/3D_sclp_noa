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

//    public CargoType selectCargo(Space space, List<CargoType> cargoes, String stateKey) {
//        Map<CargoType, Double> weights = new HashMap<>();
//        double totalWeight = 0;
//
//        for (CargoType cargo : cargoes) {
//            if (cargo.quantity > 0 && cargo.canFit(space)) {
//                // התיקון הקריטי: מפתח שמשלב מצב + סוג ארגז
//
//                // שליפת פרומון ספציפי להחלטה הזו
//                double pheromone = manager.getPheromone(stateKey,cargo.id);
//
//                // ערך היוריסטי (נפח)
//                double visibility = cargo.getVolume();
//
//                // חישוב משקל (אלפא=1, בטא=3)
//                double w = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());
//
//                weights.put(cargo, w);
//                totalWeight += w;
//            }
//        }
//
//        return rouletteWheelSelection(weights, totalWeight);
//    }
public CargoType selectCargo(Space space, List<CargoType> cargoes, String stateKey) {
    Map<CargoType, Double> weights = new HashMap<>();
    double totalWeight = 0;

    for (CargoType cargo : cargoes) {
        // בדיקה בסיסית: האם נשאר מהארגז והאם הוא נכנס פיזית בחלל
        if (cargo.quantity > 0 && cargo.canFit(space)) {

            // 1. קבלת רמת הפרומון מהזיכרון של הנמלים
            double pheromone = manager.getPheromone(stateKey, cargo.id);

            // 2. חישוב ה-Visibility (היוריסטיקה)
            double visibility = cargo.getVolume();

            // --- בונוס התאמה לממדים (מניעת חורים בצדדים) ---
            if (Math.abs(cargo.l - space.L) < 1.0) visibility *= 1.3;
            if (Math.abs(cargo.w - space.W) < 1.0) visibility *= 1.3;

            // --- בונוס גובה קריטי (יישור שכבות) ---
            if (Math.abs(cargo.h - space.H) < 1.0) {
                visibility *= 2.5; // בונוס ענק לארגז שסוגר שכבה ישרה
            } else if (cargo.h > space.H * 0.8) {
                visibility *= 1.2; // בונוס קטן לארגז שקרוב לגובה השכבה
            }

            // 3. חישוב המשקל הסופי לפי נוסחת ה-ACO
            // Alpha שולט על חשיבות הפרומון, Beta שולט על חשיבות ה-Visibility
            double weight = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());

            weights.put(cargo, weight);
            totalWeight += weight;
        }
    }

    // בחירה מבוססת הסתברות מתוך כל האופציות שנמצאו
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