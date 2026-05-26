package com.noa.packer.algorithms;

import com.noa.packer.models.Container;

import java.util.*;

public class ASMStrategy {
    private PheromoneManager manager;
    private Random random = new Random();
    private OrientationOptimizer orientationOptimizer = new OrientationOptimizer();
    PheromoneManager pheromoneManager=new PheromoneManager();


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
public CargoType selectCargo(Space space, List<CargoType> cargoes, String stateKey, int currentIteration) {
    Map<CargoType, Double> weights = new HashMap<>();
    double totalWeight = 0;

    for (CargoType cargo : cargoes) {
        if (cargo.quantity > 0 && cargo.canFit(space)) {
            double pheromone = manager.getPheromone(stateKey, cargo.id);
            if (pheromone <= 0) pheromone = 1e-6;

            // ✅ משוואה 12: visibility = נפח קרגו / נפח חלל
            double visibility = cargo.getVolume() / space.getVolume();

            // ✅ משוואה 11: weight = pheromone^alpha × visibility^beta
            double weight = Math.pow(pheromone, manager.getAlpha()) *
                    Math.pow(visibility, manager.getBeta());

            weights.put(cargo, weight);
            totalWeight += weight;
        }
    }
    return rouletteWheelSelection(weights, totalWeight);
}
    public CargoType selectCargo7(Space space, List<CargoType> cargoes, String stateKey, int currentIteration) {
        Map<CargoType, Double> weights = new HashMap<>();
        double totalWeight = 0;

        for (CargoType cargo : cargoes) {
            // בדיקה בסיסית: האם נשאר מהארגז והאם הוא נכנס פיזית בחלל
            if (cargo.quantity > 0 && cargo.canFit(space)) {
                // 1. קבלת רמת הפרומון מהזיכרון של הנמלים
                double pheromone = manager.getPheromone(stateKey, cargo.id);
                if (pheromone <= 0) pheromone = 1e-6;

                 // 2. חישוב ה-Visibility (היוריסטיקה)
                 double spaceVolume = space.getVolume();
                 // תוקן: הגנה מפני חלוקה באפס
                 double volumeFit = (spaceVolume > 1e-9) ? (cargo.getVolume() / spaceVolume) : 0;
                 double visibility = volumeFit;

                 // --- בונוס התאמה לממדים (מניעת חורים בצדדים) ---
                 if (Math.abs(cargo.l - space.L) < 1.0) visibility *= 1.3;
                 if (Math.abs(cargo.w - space.W) < 1.0) visibility *= 1.3;
                 if (cargo.getVolume() < 200_000) {
                     visibility *= 1.4; // בונוס לארגזים קטנים שממלאים פינות
                 }
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
        // PheromoneNode newValue = new PheromoneNode(
        //     selectedCargo.id, selectedCargo.direction, pos.x, pos.y, pos.z, topPos.x, topPos.y, topPos.z, vr);

        // manager.put(stateKey, newValue); // הקשר הוא 1 ל-n [2]
        // בחירה מבוססת הסתברות מתוך כל האופציות שנמצאו
        return rouletteWheelSelection(weights, totalWeight);
    }
    public void executeASM(Space space, List<CargoType> cargoTypes, LoadingChain chain, Container container) {
        // 1. תנאי עצירה (כמו ב-GSM)
        if (space.getVolume() < 0.001) return;

        // 2. יצירת מפתח מצב (חשוב מאוד!)
        // המפתח הזה אומר לנו: "מה המצב הנוכחי של המכולה ואיזה ארגזים נותרו"
        String stateKey = chain.generateStateWord(cargoTypes);

        // 3. בחירת ארגז הסתברותית
        // כאן אנחנו משתמשים בלוגיקה של פורמונים + היוריסטיקה (Roulette Wheel)
        CargoType cargo = selectCargoProbabilistic(space, cargoTypes, stateKey);

        // אם לא נמצא ארגז (אולי המכולה מלאה או שאין ארגז מתאים), מפסיקים
        if (cargo == null) return;

        // 4. מציאת אוריינטציה (אותו הליך כמו ב-GSM)
        Orientation bestOri = orientationOptimizer.getBestOrientation(space, cargo);
        if (bestOri == null) return;
        pheromoneManager.updateLocalPheromone(stateKey, cargo.id);
        manager.updateLocalPheromone(stateKey, cargo.id);
        // 5. עדכון השרשרת
        chain.addStep(cargo, bestOri, space,stateKey);
        cargo.decreaseQuantity();

        // 6. רקורסיה: המשך למילוי המרחבים הנותרים
        executeASM(space.getTopResidual(bestOri), cargoTypes, chain, container);
        executeASM(space.getRightResidual(bestOri), cargoTypes, chain, container);
        executeASM(space.getFrontResidual(bestOri), cargoTypes, chain, container);
    }
    private CargoType selectCargoProbabilistic(Space space, List<CargoType> cargoTypes, String stateKey) {
        Map<CargoType, Double> weights = new HashMap<>();
        double totalWeight = 0;

        for (CargoType cargo : cargoTypes) {
            if (cargo.quantity > 0 && cargo.canFit(space)) {
                // שליפת פורמון (כמה פעמים הצלחת בעבר במצב הזה)
                double pheromone = manager.getPheromone(stateKey, cargo.id);

                // "נראות" (Visibility) - במאמר זה הנפח של הארגז או היעילות שלו
                double visibility = cargo.getVolume();

                // חישוב המשקל (אלפא ובטא הם הקבועים שהגדרת ב-PheromoneManager)
                double w = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());

                weights.put(cargo, w);
                totalWeight += w;
            }
        }

        // כאן אתה משתמש ב-rouletteWheelSelection שכבר יש לך בקוד
        return rouletteWheelSelection(weights, totalWeight);
    }
    //פונקציה לבחירה הסתברותית על פי משקלים
    private CargoType rouletteWheelSelection(Map<CargoType, Double> weights, double totalWeight) {
        // אין בכלל מועמדים
        if (weights == null || weights.isEmpty()) {
            return null;
        }

        // יש מועמדים אבל אין משקל הסתברותי - עדיין לא הבנתי
        if (totalWeight <= 0) {
            List<CargoType> keys = new ArrayList<>(weights.keySet());
            return keys.get(random.nextInt(keys.size())); // בטוח: size > 0
        }

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