package com.noa.packer.algorithms;

import java.util.*;

import com.noa.packer.models.Container;

public class HybridLoadingSystem {
    private PheromoneManager pheromoneManager = new PheromoneManager();
    private TreeMap<String, Double> pruningMatrix = new TreeMap<>();
    private int M = 100;
    private int m = 30;
    private ASMStrategy asm = new ASMStrategy(pheromoneManager);

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        // 1. הגדרת המשתנה מחוץ לכל הלולאות
        LoadingChain bestGlobalSolution = null;
        for (int tau = 0; tau < M; tau++) {
            LoadingChain bestIterationSolution = null;
            LoadingChain arrChain[]= new LoadingChain[m];
            for (int i = 0; i < m; i++) {
                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
                LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
                if (bestIterationSolution == null || currentChain.getVUR() > bestIterationSolution.getVUR()) {
                    bestIterationSolution = currentChain;
                }
                arrChain[i] = currentChain;
                }
                // 2. עדכון המנצח הגלובלי - רק אם הוא באמת טוב יותר!
            if (bestGlobalSolution == null || bestIterationSolution.getVUR() > bestGlobalSolution.getVUR()) {
                        bestGlobalSolution = bestIterationSolution;
                        // הדפסה בזמן אמת כשנמצא שיפור
                        System.out.println("Iteration " + tau + ": New Global Best VUR: " +
                                String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
                    }
             if (bestIterationSolution != null) {
                for (LoadingChain l : arrChain) {
                   for (LoadingChain.PlacedBoxInfo step : l.getSteps()) {
                    pheromoneManager.updateGlobalPheromone(
                            step.stateKey, step.cargoType, step.direction,
                            step.leftBackBottom[0], step.leftBackBottom[1], step.leftBackBottom[2],
                            step.remainingSpace, l.getVUR(),tau
                    );
                }
                }
                
            }
        }
        outputResults(bestGlobalSolution);
        return bestGlobalSolution;
    }

    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
        LoadingChain chain = new LoadingChain(container);
        List<Space> availableSpaces = new ArrayList<>();
       GSMStrategy gsm = new GSMStrategy();
        // חלל התחלתי: כל המכולה
        availableSpaces.add(new Space(0, 0, 0, container.getLength(), container.getWidth() , container.getHeight()));
        double currentLayerX = 0;// ✅ עוקב אחרי שכבה נוכחית לפי ציר X
        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {                 
        availableSpaces.sort(Comparator.comparingDouble((Space s) -> s.x)
                .thenComparingDouble(s -> s.z)
                .thenComparingDouble(s -> s.y));

        Space currentSpace = availableSpaces.remove(0);

        // ✅ זיהוי שכבה חדשה — האם ה-x של החלל גדול מהשכבה הנוכחית
        boolean isNewLayer = currentSpace.x > currentLayerX + 1e-9;
        if (isNewLayer) {
            currentLayerX = currentSpace.x;
        }
        CargoType selected;
        String stateKey = pheromoneManager.generateStateWord(cargoTypes);

       if (isNewLayer) {
            // ✅ שכבה חדשה — תמיד GSM עם נפח הכי גדול לפי שלב 2 במאמר
            selected = gsm.selectCargo(currentSpace, cargoTypes);
        } else {            

            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
            boolean useGreedy = new Random().nextDouble() <= pTau;

            selected = useGreedy ?
                   gsm.selectCargo(currentSpace, cargoTypes) :
                  asm.selectCargo(currentSpace, cargoTypes, stateKey, tau);
        }
            if (selected == null) continue;

            // 3. בחירת כיוון (Orientation)
            Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected, container);
            if (bestOri == null) continue;

            // 4. רישום ההנחה בשרשרת
            chain.addStep(selected, bestOri, currentSpace, stateKey,pheromoneManager);
            selected.decreaseQuantity();

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