//package com.noa.packer.algorithms;
//
//import java.util.*;
//import com.noa.packer.models.Container;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Hybrid Multi-Strategy Loading System (ACO + Greedy)
// *
// * מימוש של מערכת הטעינה ההיברידית המתחברת בין שתי אסטרטגיות:
// * 1. GSM (Greedy Strategy Module) - בחירה לפי נפח מקסימלי
// * 2. ASM (Ant System Module) - בחירה לפי פרומונים
// *
// * המערכת מפעילה M איטרציות, בכל איטרציה m נמלים מבנים פתרון ללא תלות.
// * בסוף כל איטרציה, הפרומונים מתעדכנים לפי איכות הפתרונות שנמצאו.
// */
//public class HybridLoadingSystem_Clean {
//    // ========== פרמטרים עיקריים ==========
//    private static final int M = 100;                   // מספר איטרציות (ACO generations)
//    private static final int m = 30;                    // מספר נמלים (solution builders per iteration)
//
//    // ========== משאבים משותפים ==========
//    private final PheromoneManager pheromoneManager = new PheromoneManager();
//    private final GSMStrategy gsm = new GSMStrategy();
//    private final ASMStrategy asm = new ASMStrategy(pheromoneManager);
//    private final SpaceManager spaceManager = new SpaceManager();
//
//    private static final Logger logger = LoggerFactory.getLogger(HybridLoadingSystem_Clean.class);
//
//    // ========== נקודת כניסה ראשית ==========
//
//    /**
//     * מפעיל את אלגוריתם ACO היברידי להפתרת בעיית הטעינה
//     *
//     * @param container - המכולה להטעינה
//     * @param cargoTypes - רשימת סוגי הארגזים
//     * @return הפתרון הטוב ביותר שנמצא
//     */
//    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
//        // שלב 1: אתחול
//        initializeGlobalDimensions(cargoTypes);
//        LoadingChain bestGlobalSolution = null;
//
//        // שלב 2: לולאה על M איטרציות
//        for (int tau = 0; tau < M; tau++) {
//            // 2.1: הפעל איטרציה אחת - m נמלים יבנו פתרונות בתוך התורה הזו
//            LoadingChain bestIterationSolution = executeIteration(container, cargoTypes, tau);
//
//            if (bestIterationSolution != null) {
//                // 2.2: אידוי גלובלי - פרומונים ישנים מתחלשים
//                pheromoneManager.applyGlobalEvaporation();
//
//                // 2.3: חיזוק גלובלי - הפתרון הטוב מקבל ריח חזק
//                reinforcePheromoneFromSolution(bestIterationSolution, tau);
//
//                // 2.4: עדכון הטוב ביותר בכללי
//                bestGlobalSolution = updateGlobalBestIfNeeded(bestGlobalSolution, bestIterationSolution, tau);
//            }
//        }
//
//        // שלב 3: סיום וניקיון
//        cleanupGlobalState();
//        outputResults(bestGlobalSolution);
//        return bestGlobalSolution;
//    }
//
//    // ========== שלב איטרציה ==========
//
//    /**
//     * מפעיל איטרציה אחת - m נמלים בונים פתרונות בעצמאות
//     */
//    private LoadingChain executeIteration(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain bestIterationSolution = null;
//
//        for (int antId = 0; antId < m; antId++) {
//            // העתק את רשימת הארגזים (כל נמלה מקבל עותק חדש)
//            List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
//
//            // בנה פתרון בעזרת נמלה זו
//            LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
//
//            // עדכן הטוב ביותר באיטרציה זו
//            if (isBetterSolution(currentChain, bestIterationSolution)) {
//                bestIterationSolution = currentChain;
//            }
//        }
//
//        return bestIterationSolution;
//    }
//
//    // ========== שלב נמלה (Ant Loading) ==========
//
//    /**
//     * בונה פתרון אחד של טעינה עבור נמלה אחת
//     *
//     * המערכת מחלקת חללים לפי המאמר:
//     * - VU: חללים פעילים בשכבה הנוכחית (YOZ plane)
//     * - VR: שאריות ממוזגות בשכבה הנוכחית
//     * - VI: חללים לשכבות עתידיות (X direction)
//     */
//    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain chain = new LoadingChain(container);
//
//        List<Space> currentLayer = new ArrayList<>();
//        currentLayer.add(new Space(0, 0, 0,
//                container.getLength(), container.getWidth(), container.getHeight()));
//
//        List<Space> nextLayers = new ArrayList<>();
//
//        boolean isNewLayer = true;
//
//        while ((!currentLayer.isEmpty() || !nextLayers.isEmpty()) && hasCargo(cargoTypes)) {
//
//            // כאשר השכבה הנוכחית ריקה - פתח שכבה חדשה
//            if (currentLayer.isEmpty()) {
//                if (nextLayers.isEmpty()) break;
//                currentLayer.addAll(nextLayers);
//                nextLayers.clear();
//                isNewLayer = true;
//            }
//
//            // מיון חללים בשכבה הנוכחית (מלמטה למעלה, משמאל לימין)
//            currentLayer.sort(Comparator
//                    .comparingDouble((Space s) -> s.z)      // Z: מלמטה למעלה
//                    .thenComparingDouble(s -> s.y)          // Y: משמאל לימין
//                    .thenComparingDouble(s -> s.x));        // X: מאחור להביל
//
//            // בחר חלל מהשכבה הנוכחית
//            Space currentSpace = currentLayer.remove(0);
//
//            // בחר ארגז וכיוון
//            CargoSelectionResult selection = selectCargoAndOrientation(
//                    currentSpace, cargoTypes, tau, chain, isNewLayer
//            );
//
//            if (selection == null) {
//                // אין אפשרות - שמור את החלל ונמשיך
//                currentLayer.add(currentSpace);
//                continue;
//            }
//
//            // הנח את הארגז
//            chain.addStep(selection.cargo, selection.orientation, currentSpace, selection.stateKey);
//            selection.cargo.decreaseQuantity();
//            pheromoneManager.applyLocalEvaporation(selection.stateKey, selection.cargo.id);
//
//            // פיצול חללים לפי המאמר
//            double x = currentSpace.x, y = currentSpace.y, z = currentSpace.z;
//            double L = currentSpace.L, W = currentSpace.W, H = currentSpace.H;
//            double l = selection.orientation.l, w = selection.orientation.w, h = selection.orientation.h;
//
//            // T space (Top, Z+) → VR: נשאר בשכבה הנוכחית
//            if (H - h > 1e-6) {
//                currentLayer.add(new Space(x, y, z + h, l, w, H - h));
//            }
//
//            // R space (Right, Y+) → VR: נשאר בשכבה הנוכחית
//            if (W - w > 1e-6) {
//                currentLayer.add(new Space(x, y + w, z, l, W - w, H));
//            }
//
//            // F space (Front, X+) → VI: שכבה עתידית
//            if (L - l > 1e-6) {
//                nextLayers.add(new Space(x + l, y, z, L - l, W, H));
//            }
//
//            // מיזוג וניקוי חללים
//            currentLayer = spaceManager.mergeSpaces(currentLayer);
//            currentLayer = spaceManager.cleanAndOptimizeSpaces(currentLayer);
//            nextLayers = spaceManager.cleanAndOptimizeSpaces(nextLayers);
//
//            isNewLayer = false;
//        }
//
//        return chain;
//    }
//
//    // ========== בחירת ארגז וכיוון ==========
//
//    /**
//     * בחר ארגז וכיוון בעזרת GSM או ASM לפי השלב בו אנחנו
//     */
//    private CargoSelectionResult selectCargoAndOrientation(
//            Space currentSpace, List<CargoType> cargoTypes, int tau,
//            LoadingChain chain, boolean isNewLayer) {
//
//        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//
//        if (isNewLayer) {
//            // שכבה חדשה: GSM טהור - בחר לפי נפח מקסימלי
//            CargoType selected = cargoTypes.stream()
//                    .filter(c -> c.quantity > 0 && c.canFit(currentSpace))
//                    .max(Comparator.comparingDouble(CargoType::getVolume))
//                    .orElse(null);
//
//            if (selected == null) return null;
//
//            Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected);
//            if (bestOri == null) return null;
//
//            return new CargoSelectionResult(selected, bestOri, stateKey);
//        } else {
//            // בתוך שכבה: בחר בהסתברות בין GSM ל־ASM
//            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
//            boolean useGreedy = new Random().nextDouble() <= pTau;
//
//            if (useGreedy) {
//                // GSM: בחר לפי נפח
//                CargoType selected = cargoTypes.stream()
//                        .filter(c -> c.quantity > 0 && c.canFit(currentSpace))
//                        .max(Comparator.comparingDouble(CargoType::getVolume))
//                        .orElse(null);
//
//                if (selected == null) return null;
//
//                Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected);
//                if (bestOri == null) return null;
//
//                return new CargoSelectionResult(selected, bestOri, stateKey);
//            } else {
//                // ASM: בחר לפי פרומונים
//                CargoType selected = asm.selectCargo(currentSpace, cargoTypes, stateKey, tau);
//                if (selected == null) return null;
//
//                Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected);
//                if (bestOri == null) return null;
//
//                return new CargoSelectionResult(selected, bestOri, stateKey);
//            }
//        }
//    }
//
//    // ========== עדכון פרומונים ==========
//
//    /**
//     * חזק פרומונים לפי הפתרון שנמצא
//     */
//    private void reinforcePheromoneFromSolution(LoadingChain solution, int tau) {
//        if (solution == null || solution.getSteps() == null) {
//            return;
//        }
//
//        for (LoadingChain.PlacedBoxInfo step : solution.getSteps()) {
//            pheromoneManager.updateGlobalPheromone(
//                    step.stateKey,
//                    step.cargoType,
//                    step.direction,
//                    step.leftBackBottom[0],
//                    step.leftBackBottom[1],
//                    step.leftBackBottom[2],
//                    step.remainingSpace,
//                    solution.getVUR(),
//                    tau
//            );
//        }
//    }
//
//    /**
//     * עדכן הטוב ביותר בכללי אם הפתרון החדש יותר טוב
//     */
//    private LoadingChain updateGlobalBestIfNeeded(
//            LoadingChain current, LoadingChain newSolution, int tau) {
//
//        if (isBetterSolution(newSolution, current)) {
//            logger.info("Iteration " + tau + ": New Global Best VUR: " +
//                    String.format("%.2f", newSolution.getVUR() * 100) + "%");
//            return newSolution;
//        }
//        return current;
//    }
//
//    // ========== פונקציות עזר ==========
//
//    /**
//     * בדוק אם פתרון אחד טוב יותר מאחר
//     */
//    private boolean isBetterSolution(LoadingChain solution1, LoadingChain solution2) {
//        if (solution1 == null) return false;
//        if (solution2 == null) return true;
//        return solution1.getVUR() > solution2.getVUR();
//    }
//
//    /**
//     * הגדר את הממד המינימלי גלובלי
//     */
//    private void initializeGlobalDimensions(List<CargoType> cargoTypes) {
//        double minDimension = Double.MAX_VALUE;
//        for (CargoType cargo : cargoTypes) {
//            minDimension = Math.min(minDimension, cargo.l);
//            minDimension = Math.min(minDimension, cargo.w);
//            minDimension = Math.min(minDimension, cargo.h);
//        }
//        Global.MIN_DIM = minDimension;
//    }
//
//    /**
//     * נקה את המצב הגלובלי
//     */
//    private void cleanupGlobalState() {
//        Global.MIN_DIM = Double.MAX_VALUE;
//    }
//
//    /**
//     * הדפס תוצאות סופיות
//     */
//    private void outputResults(LoadingChain solution) {
//        if (solution != null) {
//            System.out.println("\n=== FINAL OPTIMIZATION RESULTS ===");
//            System.out.println("Final VUR: " + String.format("%.2f%%", solution.getVUR() * 100));
//            System.out.println("Total Items Loaded: " + solution.getSteps().size());
//            System.out.println("===================================\n");
//        } else {
//            System.out.println("\n❌ No solution found!\n");
//        }
//    }
//
//    /**
//     * בדוק אם יש עוד ארגזים להטענה
//     */
//    private boolean hasCargo(List<CargoType> list) {
//        return list.stream().anyMatch(c -> c.quantity > 0);
//    }
//
//    /**
//     * העתק עמוק רשימת ארגזים
//     */
//    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
//        List<CargoType> copy = new ArrayList<>();
//        for (CargoType ct : original) {
//            copy.add(new CargoType(ct));
//        }
//        return copy;
//    }
//
//    // ========== מחלקה עזר פנימית ==========
//
//    /**
//     * תוצאה של בחירת ארגז וכיוון
//     */
//    private static class CargoSelectionResult {
//        CargoType cargo;
//        Orientation orientation;
//        String stateKey;
//
//        CargoSelectionResult(CargoType cargo, Orientation orientation, String stateKey) {
//            this.cargo = cargo;
//            this.orientation = orientation;
//            this.stateKey = stateKey;
//        }
//    }
//}
//
