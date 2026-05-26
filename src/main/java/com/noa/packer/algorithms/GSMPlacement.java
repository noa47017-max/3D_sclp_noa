//package com.noa.packer.algorithms;
//
//import java.util.List;
//import com.noa.packer.models.Container; // תתאים את ה-Import למיקום אצלך
//
//public class GSMPlacement {
//    private GSMStrategy strategy = new GSMStrategy();
//    private OrientationOptimizer orientationOptimizer = new OrientationOptimizer();
//    PheromoneManager pheromoneManager=new PheromoneManager();
//
//    // זו נקודת הכניסה החדשה שלך
//    public void executeGSM(Space initialSpace, List<CargoType> cargoTypes, LoadingChain chain, Container container) {
//        tryFill(initialSpace, cargoTypes, chain, container);
//    }
//
//    private void tryFill(Space space, List<CargoType> cargoTypes, LoadingChain chain, Container container) {
//        // 1. תנאי עצירה: אם החלל קטן מדי (לפי המאמר, אם הנפח מתחת לסף מסוים, אין טעם להמשיך)
//        if (space.getVolume() < 0.001) return;
//        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//
//        // 2. בחירת הארגז הטוב ביותר לחלל הנוכחי (החלק החמדני - Greedy)
//        CargoType cargo = strategy.selectCargo(space, cargoTypes);
//        if (cargo == null) return; // לא נמצא ארגז מתאים, אין מה להמשיך ברקורסיה הזו
//
//        // 3. מציאת האוריינטציה האופטימלית
//        Orientation bestOri = orientationOptimizer.getBestOrientation(space, cargo, container);
//        if (bestOri == null) return; // לא נמצאה זווית מתאימה
//        pheromoneManager.updateLocalPheromone(stateKey, cargo.id);
//        // 4. ביצוע הטעינה בפועל
//        chain.addStep(cargo, bestOri, space,stateKey);
//        cargo.decreaseQuantity(); // עדכון המלאי
//
//        // 5. רקורסיה: יצירת החללים החדשים וקריאה אליהם בסדר הקריטי Top -> Right -> Front
//        // הסדר הזה הוא זה שמבטיח ש"נמלא" את המכולה בצורה יציבה ושלא יישארו חורים גדולים
//        tryFill(space.getTopResidual(bestOri), cargoTypes, chain, container);
//        tryFill(space.getRightResidual(bestOri), cargoTypes, chain, container);
//        tryFill(space.getFrontResidual(bestOri), cargoTypes, chain, container);
//    }
//}
