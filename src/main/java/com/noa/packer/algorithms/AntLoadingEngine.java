package com.noa.packer.algorithms;
import java.util.Random;
import java.util.*;
import com.noa.packer.models.Container;

public class AntLoadingEngine {
    private final LoadingContext ctx;
    private final Container container;
    private final List<CargoType> cargoTypes;
    private final int tau;
    private final StrategySelector strategySelector;
    public AntLoadingEngine(LoadingContext ctx, Container container,
                            List<CargoType> cargoTypes, int tau) {
        this.ctx = ctx;
        this.container = container;
        this.cargoTypes = cargoTypes;
        this.tau = tau;
        this.strategySelector = new StrategySelector(ctx);
    }
    //תהליך הטעינה העיקרי
    public LoadingChain executeAntLoading() {
        LoadingChain chain = new LoadingChain(container);
        SpaceState spaceState = initializeSpaceState(container);
        boolean isNewLayer = true;
        while ((!spaceState.currentLayer.isEmpty() || !spaceState.nextLayers.isEmpty()) && hasCargo(cargoTypes)) {
            Space currentSpace;

            // מעבר לשכבה הבאה אם גמרנו למלא את השכבה הנוכחית
            if (spaceState.currentLayer.isEmpty()) {
                    isNewLayer = true;
                    currentSpace = spaceState.nextLayers.remove(0);
                    spaceState.nextLayers.clear();
            }
            else{
            spaceState.sortSpacesByPosition();
            currentSpace = spaceState.currentLayer.remove(0);
            }
            if (attemptPlacement(currentSpace, chain, cargoTypes, spaceState, isNewLayer)) {
                isNewLayer = false;
            } else {
                spaceState.retrySpaces.add(currentSpace);
            }
        }
        //חללים למיזוג בשכבה האחרונה
        if (spaceState.currentLayer.isEmpty() && spaceState.nextLayers.isEmpty() && !spaceState.retrySpaces.isEmpty()) {
            spaceState.currentLayer = SpaceManager.mergeSpaces(spaceState.retrySpaces);

            while (!spaceState.currentLayer.isEmpty() && hasCargo(cargoTypes)) {
                spaceState.sortSpacesByPosition();
                Space currentSpace = spaceState.currentLayer.remove(0);
                attemptPlacement(currentSpace, chain, cargoTypes, spaceState, false);
            }
        }
        return chain;
    }

    //נסיון שימוש בחלל הנוכחי
    private boolean attemptPlacement( Space currentSpace, LoadingChain chain, List<CargoType> cargoTypes,
                                     SpaceState spaceState, boolean isNewLayer) {


        String stateKey = ctx.pheromones().generateStateWord(cargoTypes);
        // בחירת ארגז
        CargoSelection choice = strategySelector.selectCargoAndOrientation(chain, cargoTypes,
                currentSpace, isNewLayer, tau, stateKey);

        if (choice.cargo() != null && choice.orientation() != null) {
            // הנחת הארגז
            placeCargoInSpace(choice, chain, currentSpace,  stateKey, spaceState.sumVR);
            // יצירת 3 החללים מסביב לארגז
            spaceState.addResidualSpaces(currentSpace, choice.orientation(), isNewLayer);

            // עדכון החללים
            spaceState.updateSpacesAfterPlacement(
                    currentSpace.x, currentSpace.y, currentSpace.z,
                    currentSpace.x + choice.orientation().l,
                    currentSpace.y + choice.orientation().w,
                    currentSpace.z + choice.orientation().h
            );

            // תהליך גיזום (Pruning)
            if (ctx.pruning() != null && chain.getSteps().size() > 10) {
                String pruneKey = ctx.pheromones().generateStateWord(cargoTypes);
                if (ctx.pruning().shouldPrune(pruneKey, spaceState.sumVR / chain.getSteps().size())) {
                    spaceState.currentLayer.clear();
                    spaceState.nextLayers.clear();
                    spaceState.retrySpaces.clear();
                } else {
                    ctx.pruning().updateMatrix(pruneKey, spaceState.sumVR /chain.getSteps().size());
                }
            }
            return true;
        }
        return false;
    }

    // אתחול החלל הראשון
    private static SpaceState initializeSpaceState(Container container) {
        SpaceState state = new SpaceState();
        state.nextLayers.add(new Space(0, 0, 0,
                container.getLength(), container.getWidth(), container.getHeight()));
        return state;
    }

    // הנחת הארגז
    private void placeCargoInSpace(CargoSelection choice, LoadingChain chain,
                                              Space space, String stateKey, double sumVR) {
        chain.addStep(choice.cargo(), choice.orientation(), space, stateKey);
        choice.cargo().decreaseQuantity();
        // עדכון פורמון
        ctx.pheromones().depositAndEvaporate(stateKey, choice.cargo().id, choice.orientation(), space,
                sumVR/chain.getSteps().size());
    }
    private static boolean hasCargo(List<CargoType> cargoTypes) {
        return cargoTypes.stream().anyMatch(c -> c.quantity > 0);
    }
}