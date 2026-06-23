package com.noa.packer.algorithms;

import java.util.*;
import java.util.stream.Collectors;

import com.noa.packer.models.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HybridLoadingSystem {

    // ========== משאבים משותפים ==========
    private final PheromoneManager pheromoneManager = new PheromoneManager();
    private final GSMStrategy gsm = new GSMStrategy();
    private final ASMStrategy asm = new ASMStrategy(pheromoneManager);
    private final SpaceManager spaceManager = new SpaceManager();
    private final AntLoadingEngine antLoadingEngine = new AntLoadingEngine(spaceManager, pheromoneManager, gsm, asm);
    private static final Logger logger = LoggerFactory.getLogger(HybridLoadingSystem.class);

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        // ===== שלב 1: אתחול =====
        initializeGlobalDimensions(cargoTypes);
        LoadingChain bestGlobalSolution = null;
        // ===== שלב 2: לולאה על M איטרציות =====
        for (int tau = 0; tau < Global.M; tau++) {
            // 2.1: הפעל איטרציה אחת (m נמלים)
            LoadingChain bestIterationSolution = executeIteration(container, cargoTypes, tau);
            if (bestIterationSolution != null) {
                // 2.2: אידוי פרומונים - הנמלים הישנים קהים
                pheromoneManager.applyGlobalEvaporation();
                // 2.3: חיזוק פרומונים - הנתיב הטוב מקבל ריח חזק
                reinforcePheromoneFromSolution(bestIterationSolution, tau);
                // 2.4: עדכון הפתרון הגלובלי
                bestGlobalSolution = updateGlobalBestIfNeeded(bestGlobalSolution, bestIterationSolution, tau);
            }
        }
        // ===== שלב 3: סיום =====
        cleanupGlobalState();
        outputResults(bestGlobalSolution);
        return bestGlobalSolution;
    }


    private LoadingChain executeIteration(Container container, List<CargoType> cargoTypes, int tau) {
        LoadingChain bestIterationSolution = null;
        // הפעל m נמלים בסדר רציף
        for (int antId = 0; antId < Global.m; antId++) {
            //העתקת כל רשימת הארגזים
            List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
            // ייצור תוכנית טעינה
            LoadingChain currentChain = antLoadingEngine.executeAntLoading(container, antCargoes, tau);
            // בחר את הטוב ביותר באיטרציה זו
            if (isBetterSolution(currentChain, bestIterationSolution)) {
                bestIterationSolution = currentChain;
            }
        }
        return bestIterationSolution;
    }

    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
        List<CargoType> copy = new ArrayList<>();
        for (CargoType ct : original) copy.add(new CargoType(ct));
        return copy;
    }

    private void reinforcePheromoneFromSolution(LoadingChain solution, int tau) {
        // בדוק שלא יש null
        if (solution == null || solution.getSteps() == null) {
            return;
        }
        double globalDelta = solution.getVUR() / solution.getSteps().size();
        // עבור כל צעד בשרשרת הטעינה
        for (LoadingChain.PlacedBoxInfo step : solution.getSteps()) {
            // חזק את הפרומון של בחירה זו
            pheromoneManager.updateGlobalPheromone(
                    step.stateKey,
                    step.cargoType,
                    step.direction,
                    step.leftBackBottom[0],
                    step.leftBackBottom[1],
                    step.leftBackBottom[2],
                    step.rightFrontTop[0],
                    step.rightFrontTop[1],
                    step.rightFrontTop[2],
                    step.remainingSpace,
                    globalDelta // כמות חיזוק פרופורציונלית ל-VUR
            );
        }
    }


    private LoadingChain updateGlobalBestIfNeeded(
            LoadingChain current,
            LoadingChain newSolution,
            int tau) {

        if (isBetterSolution(newSolution, current)) {
            // דוגמה הודעה: "Iteration 25: New Global Best VUR = 94.31%"
            logger.info("Iteration " + tau + ": New Global Best VUR: " +
                            String.format("%.2f", newSolution.getVUR() * 100) + "%");
            return newSolution;
        }
        return current;
    }


    private boolean isBetterSolution(LoadingChain solution1, LoadingChain solution2) {
        if (solution1 == null) {
            return false;  // solution1 גרוע (null)
        }
        if (solution2 == null) {
            return true;   // solution2 גרוע (null), אז solution1 טוב
        }
        // שניהם לא null - השווה לפי VUR
        return solution1.getVUR() > solution2.getVUR();
    }


    private void initializeGlobalDimensions(List<CargoType> cargoTypes) {
        double minDimension = Double.MAX_VALUE;
        // עברו על כל סוג מטען ותאפשו את הממד הקטן ביותר
        for (CargoType cargo : cargoTypes) {
            minDimension = Math.min(minDimension, cargo.l);  // אורך
            minDimension = Math.min(minDimension, cargo.w);  // רוחב
            minDimension = Math.min(minDimension, cargo.h);  // גובה
        }
        Global.MIN_DIM = minDimension;
    }


    private void cleanupGlobalState() {
        Global.MIN_DIM = Double.MAX_VALUE;
    }

//    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain chain = new LoadingChain(container);
//
//        // ===== חלוקה לפי המאמר (Section 3.4, Figure 2) =====
//        //
//        // currentLayer = VU + VR (חללים בשכבה הנוכחית - מישור YOZ)
//        // nextLayers   = VI      (חללים לשכבות עתידיות - כיוון X)
//        //
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
//            // ===== מעבר שכבה =====
//            // כשהשכבה הנוכחית ריקה → פתח שכבה חדשה מ-VI
//            // (Section 3.4: "After filling each layer space on the YOZ plane,
//            //  cargoes are placed on the next layer in VI")
//            if (currentLayer.isEmpty()) {
//                if (nextLayers.isEmpty()) break;
//                currentLayer.addAll(nextLayers);
//                nextLayers.clear();
//                isNewLayer = true;
//            }
//
//            // ===== מיון בתוך השכבה הנוכחית =====
//            // (Section 3.4: "from bottom to top (z-axis), from left to right (y-axis)")
//            // (Section 4.3 Step 3: "based on the order of top-right-front")
//            currentLayer.sort(Comparator
//                    .comparingDouble((Space s) -> s.z)      // Z: מלמטה למעלה (bottom to top)
//                    .thenComparingDouble(s -> s.y)           // Y: משמאל לימין (left to right)
//                    .thenComparingDouble(s -> s.x));         // X: מאחור להביל (back to front)
//
//            // ====== שלב 1: בחר חלל מהשכבה הנוכחית ======
//            Space currentSpace = currentLayer.remove(0);
//
//            // ====== שלב 2: בחר ארגז וכיוון ======
//            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
//            boolean useGreedy = new Random().nextDouble() <= pTau;
//
//            CargoType selected = null;
//            Orientation bestOri = null;
//            String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//
//            if (isNewLayer) {
//                // שכבה חדשה: GSM טהור - בחר נפח מקסימלי
//                // (Section 4.3 Step 2: "select a cargo with the largest volume")
//                selected = cargoTypes.stream()
//                        .filter(c -> c.quantity > 0 && c.canFit(currentSpace))
//                        .max(Comparator.comparingDouble(CargoType::getVolume))
//                        .orElse(null);
//
//                if (selected == null) continue;
//
//                bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected);
//                if (bestOri == null) continue;
//
//                isNewLayer = false;
//            } else {
//                // בתוך שכבה: בחר לפי הסתברות GSM/ASM
//                // (Section 4.4: R(τ) - P(τ) ≤ 0 → GSM, else → ASM)
//                GSMStrategy.Pair<CargoType, Orientation> choice = useGreedy ?
//                        gsm.selectCargo(currentSpace, cargoTypes, pheromoneManager, chain.getSteps()) :
//                        asm.selectCargo(currentSpace, cargoTypes, stateKey);
//
//                if (choice != null) {
//                    selected = choice.first;
//                    bestOri = choice.second;
//                } else {
//                    continue;
//                }
//            }
//
//            // ====== שלב 3: הנח את הארגז ======
//            chain.addStep(selected, bestOri, currentSpace, stateKey);
//            selected.decreaseQuantity();
//            pheromoneManager.applyLocalEvaporation(stateKey, selected.id, bestOri.id);
//
//            // ====== שלב 4: פיצול חללים לפי המאמר (Section 3.4, Figure 2) ======
//            //
//            //  אחרי הנחת ארגז בגודל (l, w, h) בחלל בגודל (L, W, H):
//            //
//            //  ┌──────────────────────────┐
//            //  │         Front (X+)       │ → VI (שכבה עתידית)
//            //  │         F space          │
//            //  ├────────┬─────────────────┤
//            //  │  Box   │  Right (Y+)     │ → VR (שכבה נוכחית)
//            //  │        │  R space        │
//            //  │        ├─────────────────┤
//            //  │        │  Top (Z+)       │ → VR (שכבה נוכחית)
//            //  │        │  T space        │
//            //  └────────┴─────────────────┘
//            //
//            double x = currentSpace.x, y = currentSpace.y, z = currentSpace.z;
//            double L = currentSpace.L, W = currentSpace.W, H = currentSpace.H;
//            double l = bestOri.l, w = bestOri.w, h = bestOri.h;
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
//            // ====== שלב 5: חיתוך, מיזוג וניקוי ======
//            SpaceManager.PlacedBox placedBox = SpaceManager.PlacedBox
//                    .fromSpaceAndOrientation(currentSpace, bestOri);
//
//            // חתוך את כל החללים כנגד הארגז החדש (מונע חפיפות)
//            currentLayer = spaceManager.updateSpacesAfterPlacement(currentLayer, placedBox);
//            nextLayers = spaceManager.updateSpacesAfterPlacement(nextLayers, placedBox);
//
//            // מיזוג חללים שכנים בשכבה הנוכחית (Section 3.5)
//            currentLayer = spaceManager.mergeSpaces(currentLayer);
//
//            // ניקוי חללים קטנים ומכוסים
//            currentLayer = spaceManager.cleanSpaces(currentLayer);
//            nextLayers = spaceManager.cleanSpaces(nextLayers);
//        }
//
//        return chain;
//    }
    // ==========================================
    // PRIVATE - קיימות כבר (executeAntLoading וכו')
    // ==========================================

//    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain chain = new LoadingChain(container);
//        List<Space> availableSpaces = spaceManager.initializeAvailableSpaces(container);
//
//        boolean isNewLayer = true;
//        double currentLayerX = 0;
//
//        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {
//
//            // שלב 1: מיון חללים
//            availableSpaces = sortSpacesByLayering(availableSpaces);
//
//            // שלב 2: בחר חלל
//            Space currentSpace = availableSpaces.remove(0);
//
//            // שלב 3: בדוק אם שכבה חדשה
//            if (isNewLayer(currentSpace.x, currentLayerX)) {
//                isNewLayer = true;
//                currentLayerX = currentSpace.x;
//            }
//
//            // שלב 4: בחר ארגז וכיוון
//            SelectedCargoAndOrientation selection = selectCargoAndOrientation(
//                    currentSpace, cargoTypes, tau, chain, isNewLayer
//            );
//            if (selection == null) continue;
//
//            // שלב 5: הנח את הארגז
//            processPlacedBox(chain, selection, currentSpace, cargoTypes);
//
//            // שלב 6: עדכן חללים
//            availableSpaces = spaceManager.updateAvailableSpacesAfterPlacement(
//                    availableSpaces, currentSpace, selection.orientation
//            );
//
//            isNewLayer = false;
//        }
//
//        return chain;
//    }
//
//    private SelectedCargoAndOrientation selectCargoAndOrientation(
//            Space currentSpace,
//            List<CargoType> cargoTypes,
//            int tau,
//            LoadingChain chain,
//            boolean isNewLayer) {
//
//        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//
//        if (isNewLayer) {
//            // בשכבה חדשה: תמיד GSM
//            return selectCargoForNewLayer(currentSpace, cargoTypes);
//        } else {
//            // בתוך שכבה: בחר לפי הסתברות
//            return selectCargoForExistingLayer(currentSpace, cargoTypes, tau, chain, stateKey);
//        }
//    }
//
//
//    private SelectedCargoAndOrientation selectCargoForNewLayer(
//            Space space, List<CargoType> cargoTypes) {
//
//        CargoType selected = cargoTypes.stream()
//                .filter(c -> c.quantity > 0 && c.canFit(space))
//                .max(Comparator.comparingDouble(CargoType::getVolume))
//                .orElse(null);
//
//        if (selected == null) return null;
//
//        Orientation orientation = new OrientationOptimizer().getBestOrientation(space, selected);
//        if (orientation == null) return null;
//
//        return new SelectedCargoAndOrientation(selected, orientation);
//    }
//
//
//    private SelectedCargoAndOrientation selectCargoForExistingLayer(
//            Space space,
//            List<CargoType> cargoTypes,
//            int tau,
//            LoadingChain chain,
//            String stateKey) {
//
//        // חישוב הסתברות
//        double pTau = calculatePTau(tau);
//        boolean useGreedy = Math.random() <= pTau;
//
//        if (useGreedy) {
//            // GSM: בחר לפי ניקוד משולב
//            return gsmStrategy.selectCargo(space, cargoTypes, pheromoneManager, chain.getSteps());
//        } else {
//            // ASM: בחר לפי פרומונים
//            return asmStrategy.selectCargo(space, cargoTypes, stateKey);
//        }
//    }
//
//    private double calculatePTau(int tau) {
//        double normalized = (double) tau / M * 3.0;
//        return Math.exp(-Math.pow(normalized, 2) / 2.0);
//    }
//
//    // ==========================================
//    // PRIVATE - עזר כללי
//    // ==========================================
//
//    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
//        return original.stream()
//                .map(CargoType::deepCopy)
//                .collect(Collectors.toList());
//    }

    private void outputResults(LoadingChain solution) {
        if (solution != null) {
            System.out.println("===== Final Results =====");
            System.out.println("VUR: " + String.format("%.2f%%", solution.getVUR() * 100));
            System.out.println("Boxes Loaded: " + solution.getSteps().size());
        } else {
            System.out.println("No solution found!");
        }
    }

}