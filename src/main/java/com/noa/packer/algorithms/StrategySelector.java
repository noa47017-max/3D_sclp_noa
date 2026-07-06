package com.noa.packer.algorithms;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StrategySelector {

    private final LoadingContext ctx;
    private final ASMStrategy asmStrategy;

    public StrategySelector(LoadingContext ctx) {
        this.ctx = ctx;
        this.asmStrategy = new ASMStrategy(ctx);
    }
    public CargoSelection selectCargoAndOrientation( LoadingChain chain, List<CargoType> cargoTypes,
                                          Space space, boolean isNewLayer, int tau, String stateKey) {
            if (isNewLayer) {
                return selectCargoForNewLayer(space, cargoTypes);
            } else {
                return selectCargoForLayer(chain, cargoTypes, space, tau, stateKey);
            }
        }
        private static CargoSelection selectCargoForNewLayer(Space space, List<CargoType> cargoTypes) {
            CargoType selected = cargoTypes.stream()
                    .filter(c -> c.quantity > 0 && c.canFit(space))
                    .max(Comparator.comparingDouble(CargoType::getVolume))
                    .orElse(null);

            if (selected == null) {
                return new CargoSelection(null, null);
            }
            Orientation bestOri = OrientationOptimizer.getBestOrientation(space, selected);
            return new CargoSelection(selected, bestOri);
        }

        private CargoSelection selectCargoForLayer(LoadingChain chain, List<CargoType> cargoTypes,
                                                   Space space, int tau, String stateKey) {
            //חישוב הסתברותי לפי התפלגות נורמלית בהתאם לאיטרציה הנוכחית
            double pTau = Math.exp(-Math.pow((double) tau / Global.M * 3.0, 2) / 2.0);
            boolean useGreedy = ThreadLocalRandom.current().nextDouble() <= pTau;

            CargoSelection choice = useGreedy ?
                    GSMStrategy.selectCargo(space, cargoTypes, chain.getSteps()) :
                    asmStrategy.selectCargo(space, cargoTypes, stateKey);

            if (choice != null) {
                return new CargoSelection(choice.cargo(), choice.orientation());
            }
            return new CargoSelection(null, null);
        }


    }
