package com.noa.packer.algorithms;

import com.noa.packer.models.Container;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ASMStrategy {
    private PheromoneManager manager;
    private OrientationOptimizer orientationOptimizer = new OrientationOptimizer();


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
// public CargoType selectBestCargo(List<CargoType> availableCargoes, List<PheromoneNode> pheromoneHistory, Space vu) {
//     CargoType bestCargo = null;
//     double highestScore = -1.0;

//     for (CargoType cargo : availableCargoes) {
//         // 1. בדיקת התאמה בסיסית למרחב ה-VU (Unloaded Space)
//         // לפי המקורות, הטעינה מתבצעת בסדר צירים קבוע (z -> y -> x) [1, 4]
//         if (!cargo.canFit(vu)) {
//             continue;
//         }

//         // 2. חישוב ציון גיאומטרי (מבוסס על נפח יחסי וסדר הנחה) [1]
//         double geometricScore = calculateRelativeSizeScore(cargo, vu);

//         // 3. שילוב מידע הפרומון (אם קיים זיכרון מהעץ האדום-שחור)
//         double pheromoneScore = 0.0;
//         if (pheromoneHistory != null && !pheromoneHistory.isEmpty()) {
//             pheromoneScore = evaluatePheromoneSuccess(cargo, pheromoneHistory);
//         }

//         // ציון משולב: האלגוריתם משווה בין אפשרויות פתרון שונות בכל איטרציה [2]
//         double totalScore = geometricScore + pheromoneScore;

//         if (totalScore > highestScore) {
//             highestScore = totalScore;
//             bestCargo = cargo;
//         }
//     }  

    //     return bestCargo;
// }
//public CargoType selectCargo45(Space space, List<CargoType> cargoes, String stateKey, int currentIteration) {
//    Map<CargoType, Double> weights = new HashMap<>();
//    double totalWeight = 0;
//
//    for (CargoType cargo : cargoes) {
//        if (cargo.quantity > 0 && cargo.canFit(space)) {
//            double pheromone = manager.getPheromone(stateKey, cargo.id);
//            if (pheromone <= 0) pheromone = 1e-6;
//
//            // ✅ משוואה 12: visibility = נפח קרגו / נפח חלל
//            double visibility = cargo.getVolume() / space.getVolume();
//
//            // ✅ משוואה 11: weight = pheromone^alpha × visibility^beta
//            double weight = Math.pow(pheromone, manager.getAlpha()) *
//                    Math.pow(visibility, manager.getBeta());
//
//            weights.put(cargo, weight);
//            totalWeight += weight;
//        }
//    }
//    return rouletteWheelSelection(weights, totalWeight);
//}

    public CargoSelection selectCargo6(Space space, List<CargoType> cargoes, String stateKey, int currentIteration) {
        Map<CargoSelection, Double> weights = new HashMap<>();
        double totalWeight = 0;

        for (CargoType cargo : cargoes) {
            if (cargo.quantity <= 0) continue;

            // עוברים על כל האורייטציות האפשריות של הארגז
            for (Orientation ori : cargo.getAllowedOrientations()) {
                // האם הארגז בכיוון הזה נכנס בחלל?
                if (ori.l > space.L || ori.w > space.W || ori.h > space.H) continue;

                int d = ori.id;

                //  פרומון לזוג (סוג, כיוון)
                double pheromone = manager.getPheromone(stateKey, cargo.id, d);
                if (pheromone <= 0) pheromone = 1e-6;

                // ✅ נראות תלוית-כיוון: ככל שהשארית קטנה יותר, נראות גבוהה יותר
                // (הנפח לא משתנה עם הכיוון, לכן חייבים מדד מבוסס-שארית כמו משוואה 8)
                double leftover = (space.L - ori.l) + (space.W - ori.w) + (space.H - ori.h);
                double visibility = 1.0 / (1.0 + leftover);

                double weight = Math.pow(pheromone, manager.getAlpha()) *
                        Math.pow(visibility, manager.getBeta());

                weights.put(new CargoSelection(cargo, ori), weight);
                totalWeight += weight;
            }
        }
        return rouletteWheelSelection(weights, totalWeight);
    }
//    public CargoType selectCargo7(Space space, List<CargoType> cargoes, String stateKey, int currentIteration) {
//        Map<CargoType, Double> weights = new HashMap<>();
//        double totalWeight = 0;
//
//        for (CargoType cargo : cargoes) {
//            // בדיקה בסיסית: האם נשאר מהארגז והאם הוא נכנס פיזית בחלל
//            if (cargo.quantity > 0 && cargo.canFit(space)) {
//                // 1. קבלת רמת הפרומון מהזיכרון של הנמלים
//                double pheromone = manager.getPheromone(stateKey, cargo.id);
//                if (pheromone <= 0) pheromone = 1e-6;
//
//                 // 2. חישוב ה-Visibility (היוריסטיקה)
//                 double spaceVolume = space.getVolume();
//                 // תוקן: הגנה מפני חלוקה באפס
//                 double volumeFit = (spaceVolume > 1e-9) ? (cargo.getVolume() / spaceVolume) : 0;
//                 double visibility = volumeFit;
//
//                 // --- בונוס התאמה לממדים (מניעת חורים בצדדים) ---
//                 if (Math.abs(cargo.l - space.L) < 1.0) visibility *= 1.3;
//                 if (Math.abs(cargo.w - space.W) < 1.0) visibility *= 1.3;
//                 if (cargo.getVolume() < 200_000) {
//                     visibility *= 1.4; // בונוס לארגזים קטנים שממלאים פינות
//                 }
//                 // --- בונוס גובה קריטי (יישור שכבות) ---
//                 if (Math.abs(cargo.h - space.H) < 1.0) {
//                     visibility *= 2.5; // בונוס ענק לארגז שסוגר שכבה ישרה
//                 } else if (cargo.h > space.H * 0.8) {
//                     visibility *= 1.2; // בונוס קטן לארגז שקרוב לגובה השכבה
//                 }
//                // 3. חישוב המשקל הסופי לפי נוסחת ה-ACO
//                // Alpha שולט על חשיבות הפרומון, Beta שולט על חשיבות ה-Visibility
//                double weight = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());
//                weights.put(cargo, weight);
//                totalWeight += weight;
//            }
//
//        }
//        // PheromoneNode newValue = new PheromoneNode(
//        //     selectedCargo.id, selectedCargo.direction, pos.x, pos.y, pos.z, topPos.x, topPos.y, topPos.z, vr);
//
//        // manager.put(stateKey, newValue); // הקשר הוא 1 ל-n [2]
//        // בחירה מבוססת הסתברות מתוך כל האופציות שנמצאו
//        return rouletteWheelSelection(weights, totalWeight);
//    }
//    public void executeASM(Space space, List<CargoType> cargoTypes, LoadingChain chain, Container container) {
//        // 1. תנאי עצירה (כמו ב-GSM)
//        if (space.getVolume() < 0.001) return;
//
//        // 2. יצירת מפתח מצב (חשוב מאוד!)
//        // המפתח הזה אומר לנו: "מה המצב הנוכחי של המכולה ואיזה ארגזים נותרו"
//        String stateKey = chain.generateStateWord(cargoTypes);
//
//        // 3. בחירת ארגז הסתברותית
//        // כאן אנחנו משתמשים בלוגיקה של פורמונים + היוריסטיקה (Roulette Wheel)
//        CargoType cargo = selectCargoProbabilistic(space, cargoTypes, stateKey);
//
//        // אם לא נמצא ארגז (אולי המכולה מלאה או שאין ארגז מתאים), מפסיקים
//        if (cargo == null) return;
//
//        // 4. מציאת אוריינטציה (אותו הליך כמו ב-GSM)
//        Orientation bestOri = orientationOptimizer.getBestOrientation(space, cargo);
//        if (bestOri == null) return;
//        manager.updateLocalPheromone(stateKey, cargo.id);
//        manager.updateLocalPheromone(stateKey, cargo.id);
//        // 5. עדכון השרשרת
//        chain.addStep(cargo, bestOri, space,stateKey);
//        cargo.decreaseQuantity();
//
//        // 6. רקורסיה: המשך למילוי המרחבים הנותרים
//        executeASM(space.getTopResidual(bestOri), cargoTypes, chain, container);
//        executeASM(space.getRightResidual(bestOri), cargoTypes, chain, container);
//        executeASM(space.getFrontResidual(bestOri), cargoTypes, chain, container);
//    }
//    private CargoType selectCargoProbabilistic(Space space, List<CargoType> cargoTypes, String stateKey) {
//        Map<CargoType, Double> weights = new HashMap<>();
//        double totalWeight = 0;
//
//        for (CargoType cargo : cargoTypes) {
//            if (cargo.quantity > 0 && cargo.canFit(space)) {
//                // שליפת פורמון (כמה פעמים הצלחת בעבר במצב הזה)
//                double pheromone = manager.getPheromone(stateKey, cargo.id);
//
//                // "נראות" (Visibility) - במאמר זה הנפח של הארגז או היעילות שלו
//                double visibility = cargo.getVolume();
//
//                // חישוב המשקל (אלפא ובטא הם הקבועים שהגדרת ב-PheromoneManager)
//                double w = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());
//
//                weights.put(cargo, w);
//                totalWeight += w;
//            }
//        }


    public CargoSelection selectCargo(Space space, List<CargoType> cargoes, String stateKey) {
        Map<CargoSelection, Double> weights = new HashMap<>();
        double totalWeight = 0;
        for (CargoType cargo : cargoes) {
            // בדיקה בסיסית: האם נשאר מהארגז והאם הוא נכנס פיזית בחלל
            if (cargo.quantity > 0 && cargo.canFit(space)) {
                for (Orientation ori : cargo.getAllowedOrientations()) {
                    if (!(ori.l > space.L || ori.w > space.W || ori.h > space.H)) {
                        double pheromone = manager.getPheromone(stateKey, cargo.id, ori.id);
                        if (pheromone <= 0) pheromone = Global.EPSILON;
                        double visibility = calculateVisibility(cargo, ori, space);

                        double weight = Math.pow(pheromone, manager.getAlpha())
                                * Math.pow(visibility, manager.getBeta());


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
    private double calculateVisibility(CargoType cargo, Orientation ori, Space space) {
        // ששטח בסיס
        double usedVolume = ori.w * ori.h;
        double spaceVolume =space.W * space.H;
        double baseVisibility = usedVolume / spaceVolume;

        // 2. חישוב השאריות שיישארו בכל ציר לאחר הנחת הארגז באוריינטציה הזו
        double remL = space.L - ori.l;
        double remW = space.W - ori.w;
        double remH = space.H - ori.h;


        double wasteW = (space.W % ori.w) * ori.h;
        double wasteH = (space.H % ori.h) * ori.w;
        double waste = wasteW + wasteH;

        double bonus = 1.0;
        double penalty = 1.0;


        // סף מינימלי לחלל שמיש - אם נשאר רווח קטן מזה (אבל גדול מ-0), הוא יישאר ריק לנצח
        double minUsableDim = Global.MIN_DIM;

        // אם הארגז מתאים בדיוק לממד של החלל, זה מעולה כי לא נשארת "שארית זבל" בציר הזה
        if (remL < Global.EPSILON) bonus += 0.1;
        if (remW < Global.EPSILON) bonus += 0.1;
        if (remH < Global.EPSILON) bonus += 0.1;

        // אם נשאר רווח חיובי אבל הוא קטן מדי מכדי להכיל ארגז כלשהו, זהו נפח מבוזבז
        if (remL > Global.EPSILON*5 && remL < minUsableDim) penalty *= 0.9;
        if (remW > Global.EPSILON*5 && remW < minUsableDim) penalty *= 0.9;
        if (remH > Global.EPSILON*5 && remH < minUsableDim) penalty *= 0.9;

        // 3. שילוב הציון הסופי
        // כעת ה-Visibility שונה לחלוטין בין אוריינטציות שונות של אותו ארגז!
        return (1.0/(1.0 + waste))*0.5+0.5*baseVisibility * bonus * penalty;
    }

    //פונקציה לבחירה הסתברותית על פי משקלים
    private CargoSelection rouletteWheelSelection
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