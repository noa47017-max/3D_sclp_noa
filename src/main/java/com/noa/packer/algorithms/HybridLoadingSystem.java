//package com.noa.packer;
//
//import java.util.*;
//public class HybridLoadingSystem {
//    private PheromoneManager pheromoneManager = new PheromoneManager();
//    private TreeMap<String, Double> pruningMatrix = new TreeMap<>(); // מפתח: stateKey, ערך: minVR [10, 13]
//    private int M = 100; // מקסימום איטרציות [3]
//    private int m = 30;  // מספר נמלים [3]
//    private ASMStrategy asm= new ASMStrategy(pheromoneManager);
//
//    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
//        LoadingChain bestGlobalSolution = null;
//
//        for (int tau = 0; tau < M; tau++) {
//            for (int i = 0; i < m; i++) {
//                // העתקה עמוקה חיונית: כל נמלה מתחילה עם מלאי מלא משלה
//                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
//                LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
//
//                if (currentChain != null && (bestGlobalSolution == null || currentChain.getVUR() > bestGlobalSolution.getVUR())) {
//                    bestGlobalSolution = currentChain;
//                    System.out.println("Iteration " + tau + ": New Best VUR: " + String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
//
//                }
//            }
//        }
//        outputResults(bestGlobalSolution);
//        return bestGlobalSolution;
//    }
//
//    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain chain = new LoadingChain(container.getVolume());
//        List<Space> availableSpaces = new ArrayList<>();
//        availableSpaces.add(new Space(0, 0, 0, container.L, container.W, container.H));
//
//        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {
//            Space currentSpace = availableSpaces.remove(0); // בחירת חלל לפי סדר Z-Y-X [17]
//
//            // בחירת אסטרטגיה לפי מודל הסתברותי (mu=0 מבטיח GSM בהתחלה) [18, 19]
//            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
//            boolean useGreedy = new Random().nextDouble() <= pTau;
//
//            String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//            CargoType selected = useGreedy ?
//                    new GSMStrategy().selectCargo(currentSpace, cargoTypes) :
//                    asm.selectCargo(currentSpace, cargoTypes, stateKey);
//
//            if (selected == null) continue;
//            else {
//
//            // אופטימיזציית כיוון: מינימום שטח שארית במישור YOZ (משוואה 8) [20, 21]
//            Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected,container);
//            if (bestOri == null) continue;
//            else {
//            // לוגיקת הגיזום (PMS): גזום נמלה אם המרחב השיורי שלה גדול מהאופטימלי שנשמר [13, 22, 23]
//            double currentVR = currentSpace.getVolume();
//            if (pruningMatrix.containsKey(stateKey) && currentVR > pruningMatrix.get(stateKey)) {
//                return null; // Give up the i-th ant [16]
//            }
//            pruningMatrix.put(stateKey, currentVR);
//
//            // ביצוע הנחה ועדכון פרומונים גלובלי [4, 24]
//            chain.addStep(selected, bestOri, currentSpace);
//            pheromoneManager.updateGlobalPheromone(stateKey, selected.id, bestOri.id,
//                    currentSpace.x, currentSpace.y, currentSpace.z, currentVR, chain.getVUR());
//
//            selected.decreaseQuantity();
//
//            // פיצול חללים ומיזוג לפי אלגוריתם 3.5 [25-27]
//            availableSpaces.addAll(new SpaceManager().splitAndMerge(currentSpace, bestOri));
//
//            // מיון חללים קריטי: קומה ראשונה (Z), עומק (Y) ואז רוחב (X) [17]
//            availableSpaces.sort(Comparator.comparingDouble((Space s) -> s.z)
//                    .thenComparingDouble(s -> s.y)
//                    .thenComparingDouble(s -> s.x));
//        }}}
//        if (chain.getSteps().isEmpty()) {
//            System.out.println("נמלה לא הצליחה לטעון כלום"); // Debug
//        }
//        return chain;
//    }
//
//    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
//        List<CargoType> copy = new ArrayList<>();
//        for (CargoType ct : original) copy.add(new CargoType(ct)); // מניח קיום Constructor מעתיק
//        return copy;
//    }
//
//    private boolean hasCargo(List<CargoType> list) {
//        return list.stream().anyMatch(c -> c.quantity > 0);
//
//    }
//
//    private void outputResults(LoadingChain solution) {
//        if (solution != null) {
//            System.out.println("\n======================================");
//            System.out.println("   FINAL OPTIMIZATION RESULTS");
//            System.out.println("======================================");
//            System.out.println("Final VUR: " + String.format("%.2f", solution.getVUR() * 100) + "%");
//            System.out.println("Total Items Loaded: " + solution.getSteps().size());
//            System.out.println("======================================\n");
//        } else {
//            System.out.println("No feasible solution was found.");
//        }
//    }
//
//    private boolean isPruned(String stateKey, double currentResidual) {
//        // אם המצב כבר קיים והשארית הנוכחית גדולה יותר (פחות יעיל) -> גזום
//        if (pruningMatrix.containsKey(stateKey) && currentResidual > pruningMatrix.get(stateKey)) {
//            return true;
//        }
//        pruningMatrix.put(stateKey, currentResidual);
//        return false;
//    }
//}
package com.noa.packer.algorithms;

import java.util.*;

import com.noa.packer.models.Container;

public class HybridLoadingSystem {
    private PheromoneManager pheromoneManager = new PheromoneManager();
    private TreeMap<String, Double> pruningMatrix = new TreeMap<>();
    private int M = 150;
    private int m = 80;
    private ASMStrategy asm = new ASMStrategy(pheromoneManager);

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        // 1. הגדרת המשתנה מחוץ לכל הלולאות
        LoadingChain bestGlobalSolution = null;
        for (int tau = 0; tau < M; tau++) {
            LoadingChain bestIterationSolution = null;
            for (int i = 0; i < m; i++) {
                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
                LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
                if (bestIterationSolution == null || currentChain.getVUR() > bestIterationSolution.getVUR()) {
                    bestIterationSolution = currentChain;
                }
                // 2. עדכון המנצח הגלובלי - רק אם הוא באמת טוב יותר!
                if (bestGlobalSolution == null || currentChain.getVUR() > bestGlobalSolution.getVUR()) {


                        bestGlobalSolution = currentChain;
                        // הדפסה בזמן אמת כשנמצא שיפור
                        System.out.println("Iteration " + tau + ": New Global Best VUR: " +
                                String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
                    }
                }
            if (bestIterationSolution != null) {
                for (LoadingChain.PlacedBoxInfo step : bestIterationSolution.getSteps()) {
                    pheromoneManager.updateGlobalPheromone(
                            step.stateKey, step.cargoType, step.direction,
                            step.leftBackBottom[0], step.leftBackBottom[1], step.leftBackBottom[2],
                            step.remainingSpace, bestIterationSolution.getVUR()
                    );
                }
            }
        }
        outputResults(bestGlobalSolution);
        return bestGlobalSolution;
    }

    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
        LoadingChain chain = new LoadingChain(container);
        List<Space> availableSpaces = new ArrayList<>();
        // חלל התחלתי: כל המכולה
        availableSpaces.add(new Space(0, 0, 0, container.getLength(), container.getWidth() , container.getHeight()));
        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {
            // 1. שליפת החלל הראשון מהרשימה הממוינת
            availableSpaces.sort(Comparator.comparingDouble((Space s) -> s.z)
                    .thenComparingDouble(s -> s.y)
                    .thenComparingDouble(s -> s.x));

            Space currentSpace = availableSpaces.remove(0);
            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
            boolean useGreedy = new Random().nextDouble() <= pTau;

            String stateKey = pheromoneManager.generateStateWord(cargoTypes);

            CargoType selected = useGreedy ?
                   new GSMStrategy().selectCargo(currentSpace, cargoTypes) :
                  asm.selectCargo(currentSpace, cargoTypes, stateKey);
            if (selected == null) continue;

            // 3. בחירת כיוון (Orientation)
            Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected, container);
            if (bestOri == null) continue;

            // 4. רישום ההנחה בשרשרת
            chain.addStep(selected, bestOri, currentSpace, stateKey);
            selected.decreaseQuantity();
           pheromoneManager.applyLocalEvaporation(stateKey, selected.id);

            // 5. עדכון חללים
            List<Space> nextIterationSpaces = new ArrayList<>();

            // א. הוספת החללים החדשים שנוצרו מהפיצול של החלל הנוכחי
            nextIterationSpaces.addAll(new SpaceManager().splitAndMerge(currentSpace, bestOri));

            // ב. בדיקת כל שאר החללים ברשימה וקיצוץ שלהם אם הם חופפים לארגז החדש
            for (Space s : availableSpaces) {
                if (isIntersecting(s, currentSpace, bestOri)) {
                    nextIterationSpaces.addAll(splitSpaceAroundBox(s, currentSpace, bestOri));
                } else {
                    nextIterationSpaces.add(s);
                }
            }

            // 6. ניקוי ומיון
            availableSpaces = cleanAndOptimizeSpaces(new SpaceManager().mergeSpaces(nextIterationSpaces));

        }
        return chain;
    }

    // פונקציה לבדיקה אם חלל נפגש עם ארגז
    private boolean isIntersecting(Space s, Space bPos, Orientation bOri) {
        return s.x < bPos.x + bOri.l && s.x + s.L > bPos.x &&//בודקים שהחלל מתחיל לפני שהארגז מסתיים ושהארגז מתחיל לפני שהארגז מסתיים
                s.y < bPos.y + bOri.w && s.y + s.W > bPos.y &&
                s.z < bPos.z + bOri.h && s.z + s.H > bPos.z;
    }

    // פיצול חלל קיים סביב ארגז חדש כדי שלא תהיה חפיפה
    private List<Space> splitSpaceAroundBox(Space s, Space bPos, Orientation bOri) {
        List<Space> result = new ArrayList<>();
        // חיתוך החלל s ל-6 חללים פוטנציאליים סביב התיבה המפריעה
        if (bPos.x > s.x) result.add(new Space(s.x, s.y, s.z, bPos.x - s.x, s.W, s.H));
        if (bPos.x + bOri.l < s.x + s.L) result.add(new Space(bPos.x + bOri.l, s.y, s.z, (s.x + s.L) - (bPos.x + bOri.l), s.W, s.H));
        if (bPos.y > s.y) result.add(new Space(s.x, s.y, s.z, s.L, bPos.y - s.y, s.H));
        if (bPos.y + bOri.w < s.y + s.W) result.add(new Space(s.x, bPos.y + bOri.w, s.z, s.L, (s.y + s.W) - (bPos.y + bOri.w), s.H));
        if (bPos.z > s.z) result.add(new Space(s.x, s.y, s.z, s.L, s.W, bPos.z - s.z));
        if (bPos.z + bOri.h < s.z + s.H) result.add(new Space(s.x, s.y, bPos.z + bOri.h, s.L, s.W, (s.z + s.H) - (bPos.z + bOri.h)));
        return result;
    }

    private List<Space> cleanAndOptimizeSpaces(List<Space> spaces) {
        // מיון לפי נפח יורד - עוזר לנו לבדוק רק מול חללים גדולים יותר
        spaces.sort((a, b) -> Double.compare(b.getVolume(), a.getVolume()));
        List<Space> filtered = new ArrayList<>();
        for (int i = 0; i < spaces.size(); i++) {
            Space s1 = spaces.get(i);
            if (s1.getVolume() <= 0.001) continue;
            boolean contained = false;
            // בודקים רק מול חללים שכבר נכנסו ל-filtered (הם הגדולים יותר)
            for (Space sBig : filtered) {
                if (isContained(s1, sBig)) {
                    contained = true;
                    break;
                }
            }
            if (!contained) filtered.add(s1);
        }
        return filtered;
    }

    private boolean isContained(Space s1, Space s2) {
        return s1.x >= s2.x && s1.y >= s2.y && s1.z >= s2.z &&
                s1.x + s1.L <= s2.x + s2.L && s1.y + s1.W <= s2.y + s2.W && s1.z + s1.H <= s2.z + s2.H;
    }

    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
        List<CargoType> copy = new ArrayList<>();
        for (CargoType ct : original) copy.add(new CargoType(ct));
        return copy;
    }

    private boolean hasCargo(List<CargoType> list) {
        return list.stream().anyMatch(c -> c.quantity > 0);
    }

    private void outputResults(LoadingChain solution) {
        if (solution != null) {
            System.out.println("\n======================================");
            System.out.println("   FINAL OPTIMIZATION RESULTS");
            System.out.println("======================================");
            System.out.println("Final VUR: " + String.format("%.2f", solution.getVUR() * 100) + "%");
            System.out.println("Total Items Loaded: " + solution.getSteps().size());
            System.out.println("======================================\n");
        }
    }
}