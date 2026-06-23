package com.noa.packer.algorithms;
import java.util.Random;
import java.util.*;
import com.noa.packer.models.Container;

public class AntLoadingEngine {
    private final SpaceManager spaceManager;
    private final PheromoneManager pheromoneManager;
    private final GSMStrategy gsm;
    private final ASMStrategy asm;
    private final StrategySelector strategySelector;

    public AntLoadingEngine(SpaceManager spaceManager, PheromoneManager pheromoneManager,
                            GSMStrategy gsm, ASMStrategy asm) {
        this.spaceManager = spaceManager;
        this.pheromoneManager = pheromoneManager;
        this.gsm = gsm;
        this.asm = asm;
        strategySelector = new StrategySelector(gsm, asm, pheromoneManager);
    }

    //תהליך הטעינה העיקרי
    public LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
        LoadingChain chain = new LoadingChain(container);
        SpaceState spaceState = initializeSpaceState(container);
        boolean isNewLayer = true;
        while ((!spaceState.currentLayer.isEmpty() || !spaceState.nextLayers.isEmpty())
                && hasCargo(cargoTypes)) {
            // מעבר לשכבה הבאה אם גמרנו למלא את השכבה הנוכחית
            if (spaceState.currentLayer.isEmpty()) {

                spaceState.currentLayer = spaceManager.mergeRetryInto(
                        spaceState.nextLayers, spaceState.retrySpaces);
                spaceState.nextLayers.clear();
                spaceState.retrySpaces.clear();
                isNewLayer = true;
            }
            spaceState.sortSpacesByPosition();
            Space currentSpace = spaceState.currentLayer.remove(0);
            //בחירת ארגז
            CargoSelection choice = strategySelector.selectCargoAndOrientation(chain, cargoTypes, currentSpace,
                    isNewLayer, tau);
            if (choice.cargo() != null && choice.orientation() != null) {
                // הנחת הארגז
                placeCargoInSpace(choice, chain, cargoTypes, currentSpace, spaceState);
                // יצירת 3 החללים מסביב לארגז
                spaceState.addResidualSpaces(currentSpace, choice.orientation(), isNewLayer);
                isNewLayer = false;
                // עדכון החללים
                spaceState.updateSpacesAfterPlacement(
                        currentSpace.x, currentSpace.y, currentSpace.z,
                        currentSpace.x + choice.orientation().l, currentSpace.y + choice.orientation().w, currentSpace.z + choice.orientation().h );
            } else {
                // לא ניתן למלא - מוסיפים לרשימת ה VR
                spaceState.retrySpaces.add(currentSpace);
            }
        }
        return chain;
    }

    private SpaceState initializeSpaceState(Container container) {
        SpaceState state = new SpaceState();
        state.currentLayer.add(new Space(0, 0, 0,
                container.getLength(), container.getWidth(), container.getHeight()));
        return state;
    }

    // הנחת הארגז
    private void placeCargoInSpace(CargoSelection choice, LoadingChain chain, List<CargoType> cargoTypes,
                                              Space space, SpaceState spaceState) {
        //יצירת מילת מצב
        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
        chain.addStep(choice.cargo(), choice.orientation(), space, stateKey);
        // הורדה מכמות הארגזים מהסוג הנבחר
        choice.cargo().decreaseQuantity();
        // עדכון פורמון
        pheromoneManager.depositAndEvaporate(stateKey, choice.cargo().id, choice.orientation(), space,
                space.getVolume() - choice.cargo().getVolume());
    }

    private boolean hasCargo(List<CargoType> cargoTypes) {
        return cargoTypes.stream().anyMatch(c -> c.quantity > 0);
    }



    /**
     * Backfill: find best cargo for a space
     */
//    private BackfillResult fillSpaceWithBestCargo(LoadingChain chain, List<CargoType> cargoTypes,
//                                                  Space space, List<Space> allSpaces) {
//        CargoType bestCargo = null;
//        Orientation bestOri = null;
//        double bestScore = -1;
//
//        // Try all cargo types and orientations
//        for (CargoType cargo : cargoTypes) {
//            if (cargo.quantity <= 0) continue;
//
//            Orientation ori = new OrientationOptimizer().getBestOrientation(space, cargo);
//            if (ori == null) continue;
//
//            double score = calculateFillScore(ori, space);
//            if (score > bestScore) {
//                bestScore = score;
//                bestCargo = cargo;
//                bestOri = ori;
//            }
//        }
//
//        if (bestCargo == null) {
//            return new BackfillResult(false);
//        }
//
//        // Place the cargo
//        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//        chain.addStep(bestCargo, bestOri, space, stateKey);
//        bestCargo.decreaseQuantity();
//
//        // Update spaces
//        SpaceManager.PlacedBox placedBox = SpaceManager.PlacedBox
//                .fromSpaceAndOrientation(space, bestOri);
//        allSpaces = spaceManager.updateSpacesAfterPlacement(allSpaces, placedBox);
//
//        // Add residuals
//        addBackfillResiduals(space, bestOri, allSpaces);
//
//        allSpaces = spaceManager.cleanSpaces(allSpaces);
//        sortSpacesByVolume(allSpaces);
//
//        return new BackfillResult(true);
//    }
//
//    private void addBackfillResiduals(Space space, Orientation orientation, List<Space> spaces) {
//        double x = space.x, y = space.y, z = space.z;
//        double sL = space.L, sW = space.W, sH = space.H;
//        double ol = orientation.l, ow = orientation.w, oh = orientation.h;
//
//        if (sH - oh > Global.EPSILON) {
//            spaces.add(new Space(x, y, z + oh, ol, ow, sH - oh));
//        }
//        if (sW - ow > Global.EPSILON) {
//            spaces.add(new Space(x, y + ow, z, ol, sW - ow, sH));
//        }
//        if (sL - ol > Global.EPSILON) {
//            spaces.add(new Space(x + ol, y, z, sL - ol, sW, sH));
//        }
//    }
//
//    private double calculateFillScore(Orientation orientation, Space space) {
//        double cargoVolume = orientation.l * orientation.w * orientation.h;
//        return cargoVolume / space.getVolume();
//    }
//

//
//
//
//    /**
//     * Result of backfill attempt
//     */
//    private static class BackfillResult {
//        boolean wasPlaced;
//
//        BackfillResult(boolean wasPlaced) {
//            this.wasPlaced = wasPlaced;
//        }
//    }
//
//    private void loadBackfillPhase(LoadingChain chain, List<CargoType> cargoTypes,
//                                   SpaceState spaceState) {
//        List<Space> allRemaining = collectAllRemainingSpaces(spaceState);
//
//        boolean filledSomething = true;
//        while (filledSomething && hasCargo(cargoTypes)) {
//            filledSomething = false;
//
//            Iterator<Space> it = allRemaining.iterator();
//            while (it.hasNext() && hasCargo(cargoTypes)) {
//                Space space = it.next();
//
//                BackfillResult backfillResult = fillSpaceWithBestCargo(chain, cargoTypes, space,
//                        allRemaining);
//
//                if (backfillResult.wasPlaced) {
//                    filledSomething = true;
//                    it = allRemaining.iterator();  // Restart iteration
//                }
//            }
//        }
//    }
//    private CargoSelection selectCargoAndOrientation(LoadingChain chain,
//                                                     List<CargoType> cargoTypes,
//                                                     Space space, boolean isNewLayer, int tau) {
//        if (isNewLayer) {
//            return selectCargoForNewLayer(space, cargoTypes);
//        } else {
//            return selectCargoForLayer(chain, cargoTypes, space, tau);
//        }
//    }
//
//    /**
//     * New layer: pure GSM - select largest volume cargo
//     */
//    private CargoSelection selectCargoForNewLayer(Space space, List<CargoType> cargoTypes) {
//        CargoType selected = cargoTypes.stream()
//                .filter(c -> c.quantity > 0 && c.canFit(space))
//                .max(Comparator.comparingDouble(CargoType::getVolume))
//                .orElse(null);
//
//        if (selected == null) {
//            return new CargoSelection(null, null);
//        }
//
//        Orientation bestOri = new OrientationOptimizer().getBestOrientation(space, selected);
//        return new CargoSelection(selected, bestOri);
//    }
//
//    /**
//     * Within layer: choose between GSM/ASM based on pheromone strength
//     */
//    private CargoSelection selectCargoForLayer(LoadingChain chain, List<CargoType> cargoTypes,
//                                               Space space, int tau) {
//        double pTau = Math.exp(-Math.pow((double) tau / Global.M * 3.0, 2) / 2.0);
//        boolean useGreedy = random.nextDouble() <= pTau;
//
//        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//
//        CargoSelection choice = useGreedy ?
//                gsm.selectCargo(space, cargoTypes, chain.getSteps()) :
//                asm.selectCargo(space, cargoTypes, stateKey);
//
//        if (choice != null) {
//            return new CargoSelection(choice.cargo(), choice.orientation());
//        }
//        return new CargoSelection(null, null);
//    }
}