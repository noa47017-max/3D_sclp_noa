package com.noa.packer.algorithms;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StrategySelector {
    private Random rand = new Random();
    private final GSMStrategy gsm;
    private final ASMStrategy asm;
    private final PheromoneManager pheromoneManager;

        public StrategySelector(GSMStrategy gsm, ASMStrategy asm, PheromoneManager pheromoneManager) {
            this.gsm = gsm;
            this.asm = asm;
            this.pheromoneManager = pheromoneManager;
        }
        public CargoSelection selectCargoAndOrientation(LoadingChain chain, List<CargoType> cargoTypes,
                                          Space space, boolean isNewLayer, int tau) {
            if (isNewLayer) {
                return selectCargoForNewLayer(space, cargoTypes);
            } else {
                return selectCargoForLayer(chain, cargoTypes, space, tau);
            }
        }
        private CargoSelection selectCargoForNewLayer(Space space, List<CargoType> cargoTypes) {
            CargoType selected = cargoTypes.stream()
                    .filter(c -> c.quantity > 0 && c.canFit(space))
                    .max(Comparator.comparingDouble(CargoType::getVolume))
                    .orElse(null);

            if (selected == null) {
                return new CargoSelection(null, null);
            }
            Orientation bestOri = new OrientationOptimizer().getBestOrientation(space, selected);
            return new CargoSelection(selected, bestOri);
        }

        private CargoSelection selectCargoForLayer(LoadingChain chain, List<CargoType> cargoTypes,
                                                   Space space, int tau) {
            //חישוב הסתברותי לפי התפלגות נורמלית בהתאם לאיטרציה הנוכחית
            double pTau = Math.exp(-Math.pow((double) tau / Global.M * 3.0, 2) / 2.0);
            boolean useGreedy = ThreadLocalRandom.current().nextDouble() <= pTau;

            String stateKey = pheromoneManager.generateStateWord(cargoTypes);

            CargoSelection choice = useGreedy ?
                    gsm.selectCargo(space, cargoTypes, chain.getSteps()) :
                    asm.selectCargo(space, cargoTypes, stateKey);

            if (choice != null) {
                return new CargoSelection(choice.cargo(), choice.orientation());
            }
            return new CargoSelection(null, null);
        }


//    public boolean shouldRunGreedy(int currentIteration, int maxIterations) {
//        double rTau = rand.nextDouble(); // R(τ) [5]
//
//        // חישוב P(τ) לפי התפלגות נורמלית סטנדרטית (משוואה 12)
//        double pTau = calculateNormalProbability(currentIteration, maxIterations);
//        // החלטה: GSM אם R(τ) - P(τ) <= 0, אחרת ASM [5]
//        return (rTau - pTau) <= 0;
//    }
//
//    private double calculateNormalProbability(int tau, int M) {
//
//        double mu = 0;
//        double sigma = 1.0; // לפי המאמר [1]
//        // נרמול ה-tau לטווח שבין 0 ל-3 (כדי לכסות את רוב שטח ההתפלגות הנורמלית)
//        double x = (double) tau / M * 3.0;
//        return Math.exp(-Math.pow(x - mu, 2) / (2.0 * Math.pow(sigma, 2)));
//    }
}
