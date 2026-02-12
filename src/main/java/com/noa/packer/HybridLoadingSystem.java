package com.noa.packer;

import java.util.*;
public class HybridLoadingSystem {
    private PheromoneManager pheromoneManager = new PheromoneManager();
    private TreeMap<String, Double> pruningMatrix = new TreeMap<>(); // מפתח: stateKey, ערך: minVR [10, 13]
    private int M = 100; // מקסימום איטרציות [3]
    private int m = 30;  // מספר נמלים [3]
    private ASMStrategy asm= new ASMStrategy(pheromoneManager);

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        LoadingChain bestGlobalSolution = null;

        for (int tau = 0; tau < M; tau++) {
            for (int i = 0; i < m; i++) {
                // העתקה עמוקה חיונית: כל נמלה מתחילה עם מלאי מלא משלה
                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
                LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);

                if (currentChain != null && (bestGlobalSolution == null || currentChain.getVUR() > bestGlobalSolution.getVUR())) {
                    bestGlobalSolution = currentChain;
                    System.out.println("Iteration " + tau + ": New Best VUR: " + String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");

                }
            }
        }
        outputResults(bestGlobalSolution);
        return bestGlobalSolution;
    }

    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
        LoadingChain chain = new LoadingChain(container.getVolume());
        List<Space> availableSpaces = new ArrayList<>();
        availableSpaces.add(new Space(0, 0, 0, container.L, container.W, container.H));

        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {
            Space currentSpace = availableSpaces.remove(0); // בחירת חלל לפי סדר Z-Y-X [17]

            // בחירת אסטרטגיה לפי מודל הסתברותי (mu=0 מבטיח GSM בהתחלה) [18, 19]
            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
            boolean useGreedy = new Random().nextDouble() <= pTau;

            String stateKey = pheromoneManager.generateStateWord(cargoTypes);
            CargoType selected = useGreedy ?
                    new GSMStrategy().selectCargo(currentSpace, cargoTypes) :
                    asm.selectCargo(currentSpace, cargoTypes, stateKey);

            if (selected == null) continue;
            else {
            // אופטימיזציית כיוון: מינימום שטח שארית במישור YOZ (משוואה 8) [20, 21]
            Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected,container);
            if (bestOri == null) continue;
            else {
            // לוגיקת הגיזום (PMS): גזום נמלה אם המרחב השיורי שלה גדול מהאופטימלי שנשמר [13, 22, 23]
            double currentVR = currentSpace.getVolume();
            if (pruningMatrix.containsKey(stateKey) && currentVR > pruningMatrix.get(stateKey)) {
                return null; // Give up the i-th ant [16]
            }
            pruningMatrix.put(stateKey, currentVR);

            // ביצוע הנחה ועדכון פרומונים גלובלי [4, 24]
            chain.addStep(selected, bestOri, currentSpace);
            pheromoneManager.updateGlobalPheromone(stateKey, selected.id, bestOri.id,
                    currentSpace.x, currentSpace.y, currentSpace.z, currentVR, chain.getVUR());

            selected.decreaseQuantity();

            // פיצול חללים ומיזוג לפי אלגוריתם 3.5 [25-27]
            availableSpaces.addAll(new SpaceManager().splitAndMerge(currentSpace, bestOri));

            // מיון חללים קריטי: קומה ראשונה (Z), עומק (Y) ואז רוחב (X) [17]
            availableSpaces.sort(Comparator.comparingDouble((Space s) -> s.z)
                    .thenComparingDouble(s -> s.y)
                    .thenComparingDouble(s -> s.x));
        }}}
        if (chain.getSteps().isEmpty()) {
            System.out.println("נמלה לא הצליחה לטעון כלום"); // Debug
        }
        return chain;
    }

    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
        List<CargoType> copy = new ArrayList<>();
        for (CargoType ct : original) copy.add(new CargoType(ct)); // מניח קיום Constructor מעתיק
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
        } else {
            System.out.println("No feasible solution was found.");
        }
    }

    private boolean isPruned(String stateKey, double currentResidual) {
        // אם המצב כבר קיים והשארית הנוכחית גדולה יותר (פחות יעיל) -> גזום
        if (pruningMatrix.containsKey(stateKey) && currentResidual > pruningMatrix.get(stateKey)) {
            return true;
        }
        pruningMatrix.put(stateKey, currentResidual);
        return false;
    }
}