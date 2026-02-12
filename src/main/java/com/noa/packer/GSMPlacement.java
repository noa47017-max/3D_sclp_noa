package com.noa.packer;

public class GSMPlacement {
    private GSMStrategy strategy = new GSMStrategy();
    private OrientationOptimizer orientationOptimizer = new OrientationOptimizer();

//    public void executeGSM(Space layerSpace, List<CargoType> cargoTypes, LoadingChain chain) {
//        // שלב 2 במאמר: אם זו שכבה חדשה, בחר ארגז מקסימלי [4]
//        CargoType firstCargo = strategy.selectCargo(layerSpace, cargoTypes);
//        if (firstCargo == null) return;
//
//        placeAndProcess(firstCargo, layerSpace, cargoTypes, chain);
//    }
//
//    private void placeAndProcess(CargoType cargo, Space space, List<CargoType> cargoTypes, LoadingChain chain,Container container) {
//        // קביעת כיוון אופטימלי לפי משוואה 8 (מינימום שטח שארית ב-YOZ) [8, 9]
//        Orientation bestOri = orientationOptimizer.getBestOrientation(space, cargo,container);
//
//        // עדכון שרשרת הטעינה ועדכון פרומונים בעץ (גם בשלב החמדן!) [4, 5]
//        chain.addStep(cargo, bestOri, space);
//        cargo.decreaseQuantity();
//
//        // שלב 3 במאמר: קביעת המרחבים הבאים לפי סדר Top-Right-Front [5, 7]
//        // המערכת מנסה למלא את החללים בסדר זה כדי להבטיח ניצול מקסימלי
//        Space topSpace = space.getTopResidual(bestOri);
//        Space rightSpace = space.getRightResidual(bestOri);
//        Space frontSpace = space.getFrontResidual(bestOri);
//
//        // קריאה רקורסיבית או לולאתית לפי סדר העדיפויות [7]
//        tryFill(topSpace, cargoTypes, chain);
//        tryFill(rightSpace, cargoTypes, chain);
//        tryFill(frontSpace, cargoTypes, chain);
//    }
//
//    private void tryFill(Space s, List<CargoType> types, LoadingChain chain) {
//        if (s.getVolume() <= 0) return;
//        CargoType next = strategy.selectCargo(s, types);
//        if (next != null) {
//            placeAndProcess(next, s, types, chain);
//        }
//    }
}
