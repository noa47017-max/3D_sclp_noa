package com.noa.packer.algorithms;

import com.noa.packer.models.Container;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ASMStrategy {

    private LoadingContext ctx;

    public ASMStrategy(LoadingContext ctx) {
        this.ctx = ctx;
    }

    public CargoSelection selectCargo(Space space, List<CargoType> cargoes, String stateKey) {
        Map<CargoSelection, Double> weights = new HashMap<>();
        double totalWeight = 0;
        for (CargoType cargo : cargoes) {
            // בדיקה בסיסית: האם נשאר מהארגז והאם הוא נכנס פיזית בחלל
            if (cargo.quantity > 0 && cargo.canFit(space)) {
                for (Orientation ori : cargo.getAllowedOrientations()) {
                    if (!(ori.l > space.L || ori.w > space.W || ori.h > space.H)) {
                        double pheromone = ctx.pheromones().getPheromone(stateKey, cargo.id, ori.id);
                        if (pheromone <= 0) pheromone = Global.EPSILON;
                        double visibility = calculateVisibility(cargo, ori, space);

                        double weight = Math.pow(pheromone, ctx.pheromones().getAlpha())
                                * Math.pow(visibility, ctx.pheromones().getBeta());

                        weights.put(new CargoSelection(cargo, ori), weight);
                        totalWeight += weight;
                    }
                }
            }
        }
        // בחירה מבוססת הסתברות מתוך כל האופציות שנמצאו
        CargoSelection result = rouletteWheelSelection(weights, totalWeight);
        return result;
    }


    private double calculateVisibility( CargoType cargo, Orientation ori, Space space) {
        // ששטח בסיס
        double usedVolume = ori.w * ori.h;
        double spaceVolume =space.W * space.H;
        double baseVisibility = usedVolume / spaceVolume;

        // חישוב השאריות שיישארו בכל ציר לאחר הנחת הארגז באוריינטציה הזו
        double remL = space.L - ori.l;
        double remW = space.W - ori.w;
        double remH = space.H - ori.h;


        double wasteW = (space.W % ori.w) * ori.h;
        double wasteH = (space.H % ori.h) * ori.w;
        double waste = wasteW + wasteH;

        double bonus = 1.0;
        double penalty = 1.0;


        // סף מינימלי לחלל שמיש - אם נשאר רווח קטן מזה (אבל גדול מ-0), הוא יישאר ריק לנצח
        double minUsableDim = ctx.minDim();

        // אם הארגז מתאים בדיוק לממד של החלל, זה מעולה כי לא נשארת "שארית זבל" בציר הזה
        if (remL < Global.EPSILON) bonus += 0.1;
        if (remW < Global.EPSILON) bonus += 0.1;
        if (remH < Global.EPSILON) bonus += 0.1;

        // אם נשאר רווח חיובי אבל הוא קטן מדי מכדי להכיל ארגז כלשהו, זהו נפח מבוזבז
        if (remL > Global.EPSILON*5 && remL < minUsableDim) penalty *= 0.9;
        if (remW > Global.EPSILON*5 && remW < minUsableDim) penalty *= 0.9;
        if (remH > Global.EPSILON*5 && remH < minUsableDim) penalty *= 0.9;

        //  שילוב הציון הסופי
        return (1.0/(1.0 + waste))*0.5+0.5*baseVisibility * bonus * penalty;
    }

    //פונקציה לבחירה הסתברותית על פי משקלים
    private static CargoSelection rouletteWheelSelection
    (Map<CargoSelection, Double> weights, double totalWeight) {
        // אין בכלל מועמדים
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        // יש מועמדים אבל אין משקל הסתברותי
        if (totalWeight <= 0) {
            List<CargoSelection> keys = new ArrayList<>(weights.keySet());
            return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));// בטוח: size > 0
        }

        double currentSum = 0.0;
        double threshold = ThreadLocalRandom.current().nextDouble() * totalWeight;
        for (Map.Entry<CargoSelection, Double> entry : weights.entrySet()) {
            currentSum += entry.getValue();
            if (currentSum >= threshold) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }
}