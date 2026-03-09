package com.noa.packer.algorithms;

import com.noa.packer.models.Container;

import java.util.*;

public class Try {

    private PheromoneManager pheromoneManager = new PheromoneManager();
    private TreeMap<String, Double> pruningMatrix = new TreeMap<>();
    private int M = 150;
    private int m = 80;
    private ASMStrategy asm = new ASMStrategy(pheromoneManager);

    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
        LoadingChain bestGlobalSolution = null;
        int noImprovementCount = 0;
        final int NO_IMPROVE_LIMIT = 20;

        for (int tau = 0; tau < M; tau++) {
            boolean improvedThisTau = false;

            for (int i = 0; i < m; i++) {
                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
                LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);

                if (currentChain == null) continue;

                if (bestGlobalSolution == null
                        || currentChain.getVUR() > bestGlobalSolution.getVUR()) {

                    for (LoadingChain.PlacedBoxInfo step : currentChain.getSteps()) {
                        pheromoneManager.updateGlobalPheromone(
                                step.stateKey,
                                step.cargoType,
                                step.direction,
                                step.leftBackBottom[0],
                                step.leftBackBottom[1],
                                step.leftBackBottom[2],
                                step.remainingSpace,
                                currentChain.getVUR()
                        );
                    }
                    improvedThisTau = true;
                    bestGlobalSolution = currentChain;
                    System.out.println("Iteration " + tau + ", Ant " + i
                            + ": New Global Best VUR: "
                            + String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
                }
            }

            if (improvedThisTau) {
                noImprovementCount = 0;
            } else {
                noImprovementCount++;
            }

            if (noImprovementCount >= NO_IMPROVE_LIMIT) {
                pheromoneManager.partialReset(0.5);
                noImprovementCount = 0;
                System.out.println("🔄 Random Restart at iteration " + tau);
            }
        }

        outputResults(bestGlobalSolution);
        return bestGlobalSolution;
    }

    private LoadingChain executeAntLoading(Container container,
                                           List<CargoType> cargoTypes, int tau) {
        LoadingChain chain = new LoadingChain(container);
        List<Space> availableSpaces = new ArrayList<>();
        availableSpaces.add(new Space(0, 0, 0,  container.getLength(), container.getWidth() , container.getHeight()));

        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {

            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
            boolean useGreedy = new Random().nextDouble() <= pTau;
            String stateKey = pheromoneManager.generateStateWord(cargoTypes);

            // שלב 1: בחירת ארגז — ללא קשר לחלל ספציפי, מתוך כל המלאי
            // GSM/ASM מקבלות את כל החלל הגדול ביותר רק כדי לסנן canFit,
            // אבל הבחירה היא על בסיס כל הארגזים הזמינים
            Space largestSpace = availableSpaces.stream()
                    .max(Comparator.comparingDouble(Space::getVolume))
                    .orElse(null);
            if (largestSpace == null) break;

            CargoType selectedCargo = useGreedy
                    ? new GSMStrategy().selectCargo(largestSpace, cargoTypes)
                    : asm.selectCargo(largestSpace, cargoTypes, stateKey);

            if (selectedCargo == null) break;

            // שלב 2: Best Fit — מצא את החלל הקטן ביותר שמכיל את הארגז הנבחר
            Space bestSpace = null;
            Orientation bestOri = null;
            double bestSpaceVolume = Double.MAX_VALUE;

            for (Space space : availableSpaces) {
                Orientation ori = new OrientationOptimizer()
                        .getBestOrientation(space, selectedCargo, container);
                if (ori == null) continue; // הארגז לא נכנס לחלל זה

                double spaceVol = space.getVolume();
                if (spaceVol < bestSpaceVolume) {
                    bestSpaceVolume = spaceVol;
                    bestSpace = space;
                    bestOri = ori;
                }
            }

            // אם לא נמצא חלל מתאים לארגז הנבחר — דלג עליו הפעם
            if (bestSpace == null) {
                // סמן את הארגז כ"לא ניתן להנחה כעת" ונסה ארגז אחר
                // על ידי הסרה זמנית מהרשימה ומיון מחדש
                cargoTypes.stream()
                        .filter(c -> c.id == selectedCargo.id)
                        .findFirst()
                        .ifPresent(c -> c.quantity = 0); // זמני — לא מעדכן totalQuantity
                continue;
            }

            // שלב 3: הנחת הארגז בחלל הנבחר
            availableSpaces.remove(bestSpace);
            chain.addStep(selectedCargo, bestOri, bestSpace, stateKey);
            selectedCargo.decreaseQuantity();
            pheromoneManager.applyLocalEvaporation(stateKey, selectedCargo.id);

            // שלב 4: עדכון החללים
            List<Space> nextIterationSpaces = new ArrayList<>();
            nextIterationSpaces.addAll(new SpaceManager().splitAndMerge(bestSpace, bestOri));

            for (Space s : availableSpaces) {
                if (isIntersecting(s, bestSpace, bestOri)) {
                    nextIterationSpaces.addAll(splitSpaceAroundBox(s, bestSpace, bestOri));
                } else {
                    nextIterationSpaces.add(s);
                }
            }

            availableSpaces = cleanAndOptimizeSpaces(
                    new SpaceManager().mergeSpaces(nextIterationSpaces));
        }

        return chain;
    }

    private boolean isIntersecting(Space s, Space bPos, Orientation bOri) {
        return s.x < bPos.x + bOri.l && s.x + s.L > bPos.x &&
                s.y < bPos.y + bOri.w && s.y + s.W > bPos.y &&
                s.z < bPos.z + bOri.h && s.z + s.H > bPos.z;
    }

    private List<Space> splitSpaceAroundBox(Space s, Space bPos, Orientation bOri) {
        List<Space> result = new ArrayList<>();
        double bx1 = bPos.x, bx2 = bPos.x + bOri.l;
        double by1 = bPos.y, by2 = bPos.y + bOri.w;
        double bz1 = bPos.z, bz2 = bPos.z + bOri.h;
        double sx2 = s.x + s.L, sy2 = s.y + s.W, sz2 = s.z + s.H;

        if (bx1 > s.x) result.add(new Space(s.x, s.y, s.z, bx1 - s.x, s.W, s.H));
        if (bx2 < sx2)  result.add(new Space(bx2, s.y, s.z, sx2 - bx2, s.W, s.H));
        if (by1 > s.y)  result.add(new Space(s.x, s.y, s.z, s.L, by1 - s.y, s.H));
        if (by2 < sy2)  result.add(new Space(s.x, by2, s.z, s.L, sy2 - by2, s.H));
        if (bz1 > s.z)  result.add(new Space(s.x, s.y, s.z, s.L, s.W, bz1 - s.z));
        if (bz2 < sz2)  result.add(new Space(s.x, s.y, bz2, s.L, s.W, sz2 - bz2));
        return result;
    }

    private List<Space> cleanAndOptimizeSpaces(List<Space> spaces) {
        spaces.sort((a, b) -> Double.compare(b.getVolume(), a.getVolume()));
        List<Space> filtered = new ArrayList<>();
        for (Space s1 : spaces) {
            if (s1.getVolume() <= 0.001) continue;
            boolean contained = false;
            for (Space sBig : filtered) {
                if (isContained(s1, sBig)) { contained = true; break; }
            }
            if (!contained) filtered.add(s1);
        }
        return filtered;
    }

    private boolean isContained(Space s1, Space s2) {
        return s1.x >= s2.x && s1.y >= s2.y && s1.z >= s2.z &&
                s1.x + s1.L <= s2.x + s2.L &&
                s1.y + s1.W <= s2.y + s2.W &&
                s1.z + s1.H <= s2.z + s2.H;
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
        System.out.println("\n======================================");
        System.out.println("   FINAL OPTIMIZATION RESULTS");
        System.out.println("======================================");
        if (solution != null) {
            System.out.println("Final VUR: "
                    + String.format("%.2f", solution.getVUR() * 100) + "%");
            System.out.println("Total Items Loaded: " + solution.getSteps().size());
        } else {
            System.out.println("No valid solution found.");
        }
        System.out.println("======================================\n");
    }
}
