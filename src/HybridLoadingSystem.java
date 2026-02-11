import java.util.*;

public class HybridLoadingSystem {
    // פרמטרים קבועים - כיול מחדש לשיפור התוצאות
    private static int M = 100;         // איטרציות
    private static int m = 30;          // נמלים
    private static double RHO = 0.1;    // מקדם התנדפות נמוך (0.1) לשמירת זיכרון

    private LoadingChain bestSolution = null;

    // מטריצת גיזום - שומרת את ה-VUR הכי טוב שהושג למצב מסוים
    private TreeMap<String, Double> pruningMatrix = new TreeMap<>();

    private PheromoneManager pheromoneManager = new PheromoneManager();
    private SpaceManager spaceManager = new SpaceManager();
    private ASMStrategy asm = new ASMStrategy(pheromoneManager);
    private GSMStrategy gsm = new GSMStrategy();
    private StrategySelector selector = new StrategySelector();

    public void startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        System.out.println("Starting Hybrid Heuristic Algorithm...");
        pruningMatrix.clear(); // איפוס הזיכרון בתחילת ריצה חדשה

        for (int tau = 0; tau < M; tau++) {
            List<LoadingChain> iterationSolutions = new ArrayList<>();

            for (int i = 0; i < m; i++) {
                List<CargoType> cargoT =resetCargoQuantities(cargoTypes);
                LoadingChain currentChain = executeAntLoading(container, cargoT, tau);

                if (currentChain != null) {
                    iterationSolutions.add(currentChain);

                    // עדכון הפתרון הגלובלי הטוב ביותר
                    if (bestSolution == null || currentChain.getVUR() > bestSolution.getVUR()) {
                        bestSolution = currentChain;
                        System.out.println("Iteration " + tau + ": New Best VUR found: " +
                                String.format("%.2f", bestSolution.getVUR() * 100) + "%");
                    }
                }
            }

            // עדכון פרומונים בסוף איטרציה
            for (LoadingChain solution : iterationSolutions) {
                pheromoneManager.updateGlobalPheromones(solution.generateStateWord(), solution.getVUR(),RHO);
            }
        }
        outputResults(bestSolution);
    }

    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
        LoadingChain chain = new LoadingChain(container.getVolume());
        List<Space> availableSpaces = new ArrayList<>();

        // אתחול עם המכולה כולה כחלל ראשון
        availableSpaces.add(new Space(0, 0, 0, container.L, container.W, container.H));

        while (!availableSpaces.isEmpty() && hasAvailableCargo(cargoTypes)) {
            // בחירת החלל הראשון (זה שתמיד יהיה הכי נמוך/אחורי/שמאלי בגלל המיון)
            Space currentSpace = availableSpaces.remove(0);

            // בחירת אסטרטגיה
            boolean useGreedy = selector.shouldRunGreedy(tau, M);

            CargoType selectedCargo = useGreedy ?
                    gsm.selectCargo(currentSpace, cargoTypes) :
                    asm.selectCargo(currentSpace, cargoTypes, chain.generateStateWord());

            if (selectedCargo == null || selectedCargo.quantity <= 0) continue;

            // אופטימיזציית סיבוב
            OrientationOptimizer optimizer = new OrientationOptimizer();
            Orientation bestOri = optimizer.getBestOrientation(currentSpace, selectedCargo);
            if (bestOri == null) continue;

            // --- שלב הגיזום (Pruning) המעודכן ---
            // אנחנו בודקים את המצב *לפני* ההנחה כדי להחליט אם להמשיך
            String stateKey = chain.generateStateWord();
            double currentVUR = chain.getVUR();

            if (pruningMatrix.containsKey(stateKey) && currentVUR < pruningMatrix.get(stateKey)) {
                return null; // גזום נמלה פחות יעילה
            }
            pruningMatrix.put(stateKey, currentVUR);

            // ביצוע הנחה
            chain.addStep(selectedCargo, bestOri, currentSpace);
            selectedCargo.decreaseQuantity();

            // יצירת חללים חדשים
            List<Space> nextSpaces = new ArrayList<>();
            nextSpaces.add(currentSpace.getTopResidual(bestOri));
            nextSpaces.add(currentSpace.getRightResidual(bestOri));
            nextSpaces.add(currentSpace.getFrontResidual(bestOri));

            // סינון חללים קטנים מדי (מתחת ל-1 ס"מ) והוספה לרשימה
            for (Space ns : nextSpaces) {
                if (ns.L > 1 && ns.W > 1 && ns.H > 1) {
                    availableSpaces.add(ns);
                }
            }

            // מיזוג חללים (אם קיים ב-SpaceManager שלך)
            availableSpaces = spaceManager.mergeSpaces(availableSpaces);

            // --- מיון חללים קריטי (Z -> Y -> X) ---
            availableSpaces.sort((s1, s2) -> {
                if (Math.abs(s1.z - s2.z) > 0.01) return Double.compare(s1.z, s2.z); // קומה ראשונה קודם
                if (Math.abs(s1.y - s2.y) > 0.01) return Double.compare(s1.y, s2.y); // עומק המכולה קודם
                return Double.compare(s1.x, s2.x); // צד שמאל קודם
            });
        }
        return chain;
    }

    private List<CargoType> resetCargoQuantities(List<CargoType> cargoTypes) {
        for (CargoType type : cargoTypes) {
            type.quantity = type.totalQuantity;
        }
        return cargoTypes;
    }

    private boolean hasAvailableCargo(List<CargoType> cargoTypes) {
        return cargoTypes.stream().anyMatch(c -> c.quantity > 0);
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