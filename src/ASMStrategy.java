import java.util.List;

public class ASMStrategy {
    private PheromoneManager manager;
    public ASMStrategy(PheromoneManager manager) {
        this.manager = manager;
    }
    public CargoType selectCargo(Space currentSpace, List<CargoType> availableCargoes, String currentPathKey) {
        CargoType bestCargo = null;
        double maxProbability = -1.0;

        for (CargoType cargo : availableCargoes) {
            if (cargo.canFit(currentSpace)) {
                // שליפת ריכוז הפרומון עבור השילוב הנוכחי מהעץ [1, 4]
                double pheromone = getPheromoneFromTree(currentPathKey, cargo.id);

                // הערך ההיוריסטי המקומי (בדרך כלל הנפח) [7, 8]
                double visibility = cargo.getVolume();

                // חישוב הסתברות הבחירה (שילוב של אלפא ובטא לפי Table 2) [5]
                double probability = Math.pow(pheromone, manager.getAlpha()) * Math.pow(visibility, manager.getBeta());

                if (probability > maxProbability) {
                    maxProbability = probability;
                    bestCargo = cargo;
                }
            }
        }
        return bestCargo;
    }
    private double getPheromoneFromTree(String path, int cargoId) {
        String key = path + "-" + cargoId;
        return manager.getPheromone(key);
    }

    // עדכון פרומון לאחר בחירת מטען

    public void updatePheromone(String pathKey, CargoType cargo, double delta) {
        manager.updatePheromones(pathKey + "-" + cargo.id, delta);
    }
}





