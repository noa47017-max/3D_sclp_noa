package com.noa.packer.algorithms;

import java.util.*;

import com.noa.packer.models.Container;


    public class HybridLoadingSystem {
        private PheromoneManager pheromoneManager = new PheromoneManager();
        private StrategySelector strategySelector = new StrategySelector(); // וודא שיש לך אובייקט כזה
        private ASMStrategy asm = new ASMStrategy(pheromoneManager);
        private int M = 1; // מספר איטרציות
        private int m = 1;  // מספר נמלים

//        public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
//            LoadingChain bestGlobalSolution = null;
//
//            for (int tau = 0; tau < M; tau++) {
//                for (int i = 0; i < m; i++) {
//                    // יצירת עותק נקי של הארגזים לכל נמלה
//                    List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
//
//                    // כאן אנחנו קוראים לפונקציה שמנהלת את הנמלה
//                    LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
//
//                    // עדכון הפתרון הטוב ביותר שנמצא
//                    if (bestGlobalSolution == null || currentChain.getVUR() > bestGlobalSolution.getVUR()) {
//                        bestGlobalSolution = currentChain;
//                        System.out.println("Iteration " + tau + ": New Global Best VUR: " +
//                                String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
//                    }
//
//                }
//                // עדכון פרומונים גלובלי מתבצע רק אחרי שכל ה"דור" סיים
//                if (bestGlobalSolution != null) {
//                    // חיזוק הפרומונים של הנתיב הכי טוב שנמצא עד כה
//                    double q = 100.0; // פרמטר התאמה
//                    double delta = q * bestGlobalSolution.getVUR();
//                    pheromoneManager.applyGlobalUpdate(bestGlobalSolution, delta);
//
//                    // אידוי גלובלי של כל הפרומונים במערכת
//                    pheromoneManager.applyGlobalEvaporation();
//                }            }
//            return bestGlobalSolution;
//        }

//        private LoadingChain executeAntLoading(Container container, List<CargoType> antCargoes, int tau) {
//            // 1. החלטה אסטרטגית לנמלה זו בלבד (GSM או ASM)
//            // זה קורה פעם אחת בתחילת המסלול!
//            boolean useGreedy = strategySelector.shouldRunGreedy(tau, M);
//
//            LoadingChain chain = new LoadingChain(container);
//            Space initialSpace = new Space(0, 0, 0, container.getLength(), container.getWidth(), container.getHeight());
//
//            // 2. הפעלת האלגוריתם המתאים
//            if (useGreedy) {
//                gsmPlacement.executeGSM(initialSpace, antCargoes, chain, container);
//            } else {
//                // כאן ASM יפעיל בחירה מבוססת פרומונים לכל אורך הדרך
//                asmPlacement.executeASM(initialSpace, antCargoes, chain, container);
//            }
//
//            return chain;
//        }
public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {

    LoadingChain bestGlobalSolution = null;

    for (int tau = 0; tau < M; tau++) {
        LoadingChain bestIterationSolution = null;
        LoadingChain[] arrChain = new LoadingChain[m];

        // 1. כל הנמלים בונות פתרונות
        for (int i = 0; i < m; i++) {
            List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
            LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
            arrChain[i] = currentChain;

            if (bestIterationSolution == null || currentChain.getVUR() > bestIterationSolution.getVUR()) {
                bestIterationSolution = currentChain;
            }
        }

        // 2. אידוי גלובלי על כל הפרומונים (לפני החיזוקים)
        pheromoneManager.applyGlobalEvaporation();

        // 3. חיזוק גלובלי לפי כל הנמלים (Ant System)
        // חישוב סכום VUR של כל הנמלים לשם משקל יחסי
        double totalVUR = 0.0;
        for (int i = 0; i < m; i++) {
            if (arrChain[i] != null) totalVUR += arrChain[i].getVUR();
        }
        if (totalVUR <= 0.0) totalVUR = 1e-9; // מניעת חלוקה באפס

        // פרמטר כוונון לחיזוק הגלובלי
        double q = 0.5; // ניתן לכוונן בניסויים

        // עדכון פרומונים לכל נמלה לפי משקל היחסי שלה
        for (int i = 0; i < m; i++) {
            LoadingChain chain = arrChain[i];
            if (chain == null) continue;

            double weight = chain.getVUR() / totalVUR; // משקל יחסי
            double deltaBase = q * weight;             // delta פרמטרי

            for (LoadingChain.PlacedBoxInfo step : chain.getSteps()) {
                pheromoneManager.updateGlobalPheromone(
                        step.stateKey,
                        step.cargoType,
                        step.direction,
                        step.leftBackBottom[0],
                        step.leftBackBottom[1],
                        step.leftBackBottom[2],
                        step.remainingSpace,
                        deltaBase,
                        tau
                );
            }
        }

        // 4. עדכון הטוב ביותר גלובלי
        if (bestIterationSolution != null) {
            if (bestGlobalSolution == null || bestIterationSolution.getVUR() > bestGlobalSolution.getVUR()) {
                bestGlobalSolution = bestIterationSolution;
                System.out.println("Iteration " + tau + ": New Global Best VUR: " +
                        String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
            }
        }
    }

    outputResults(bestGlobalSolution);
    return bestGlobalSolution;
}
//    private LoadingChain executeAntLoading2(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain chain = new LoadingChain(container);
//        List<Space> availableSpaces = new ArrayList<>();
//        GSMStrategy gsm = new GSMStrategy();
//
//        availableSpaces.add(new Space(0, 0, 0, container.getLength(), container.getWidth(), container.getHeight()));
//        double currentLayerX = 0;
//
//        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {
//            // ✅ Sort spaces smartly: prioritize by layer distance, then Z, then Y, then volume
//            availableSpaces = smartSortSpaces(availableSpaces, currentLayerX);
//            Space currentSpace = availableSpaces.get(0);
//            availableSpaces.remove(currentSpace);
//
//            // זיהוי שכבה חדשה
//            boolean isNewLayer =  Math.abs(currentSpace.x - currentLayerX) > 1e-9;
//            if (isNewLayer) {
//                currentLayerX = currentSpace.x;
//            }
//
//            CargoType selected;
//            String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//            Orientation bestOri;
//
//                double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
//                boolean useGreedy = new Random().nextDouble() <= pTau;
//                GSMStrategy.Pair.of(selected, bestOri) = useGreedy ?
//                        gsm.selectCargo(currentSpace, cargoTypes,isNewLayer,pheromoneManager) :
//                        (asm.selectCargo(currentSpace, cargoTypes, stateKey, tau),new OrientationOptimizer().getBestOrientation(currentSpace, selected));
//
//
//            if (selected == null) {
//                continue;
//            }
//
//
//
//            // Local Evaporation
//            pheromoneManager.applyLocalEvaporation(stateKey, selected.id);
//
//            // בחירת כיוון
//            if (bestOri == null) {
//
//                continue;
//            }
//
//            // טעינה
//            chain.addStep(selected, bestOri, currentSpace, stateKey);
//            selected.decreaseQuantity();
//
//            // עדכון חללים - סדר הצעדים חשוב!
//            // עדכון חללים - סדר הצעדים חשוב!
//            List<Space> nextIterationSpaces = new ArrayList<>();
//
//            List<Space> residuals = new SpaceManager().splitIntoResiduals(currentSpace, bestOri);
//            nextIterationSpaces.addAll(residuals);
//
//            for (Space s : availableSpaces) {
//                if (isIntersecting(s, currentSpace, bestOri)) {
//                    nextIterationSpaces.addAll(splitSpaceAroundBox(s, currentSpace, bestOri));
//                } else {
//                    nextIterationSpaces.add(s);
//                }
//            }
//
//            // תיקון חשוב:
//            availableSpaces = new SpaceManager().mergeSpaces(nextIterationSpaces);
//            availableSpaces = new SpaceManager().cleanAndOptimizeSpaces(availableSpaces);
//        }
//
//        return chain;
//    }
    // בדיקה אם החלל הנוכחי הוא תחילת שכבה חדשה
    private boolean isNewLayer(Space current, List<Space> stack) {
        // שכבה חדשה = אין חללים אחרים באותו X
        return stack.stream()
                .noneMatch(s -> Math.abs(s.x - current.x) < 1e-9);
    }
//    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
//        LoadingChain chain = new LoadingChain(container);
//        List<Space> availableSpaces = new ArrayList<>();
//       GSMStrategy gsm = new GSMStrategy();
//        // חלל התחלתי: כל המכולה
//        availableSpaces.add(new Space(0, 0, 0, container.getLength(), container.getWidth() , container.getHeight()));
//        double currentLayerX = 0;// ✅ עוקב אחרי שכבה נוכחית לפי ציר X
//        while (!availableSpaces.isEmpty() && hasCargo(cargoTypes)) {
//
//
//            availableSpaces = smartSortSpaces(availableSpaces, currentLayerX);
//            Space currentSpace = availableSpaces.remove(0);
//
//        // ✅ זיהוי שכבה חדשה — האם ה-x של החלל גדול מהשכבה הנוכחית
//        boolean isNewLayer = currentSpace.x > currentLayerX + 1e-9;
//        if (isNewLayer) {
//            currentLayerX = currentSpace.x;
//        }
//        CargoType selected;
//        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//
//       if (isNewLayer) {
//            // ✅ שכבה חדשה — תמיד GSM עם נפח הכי גדול לפי שלב 2 במאמר
//            selected = gsm.selectCargo(currentSpace, cargoTypes);
//        } else {
//
//            double pTau = Math.exp(-Math.pow((double) tau / M * 3.0, 2) / 2.0);
//            boolean useGreedy = new Random().nextDouble() <= pTau;
//
//            selected = useGreedy ?
//                   gsm.selectCargo(currentSpace, cargoTypes) :
//                  asm.selectCargo(currentSpace, cargoTypes, stateKey, tau);
//        }
//            if (selected == null) continue;
//            pheromoneManager.applyLocalEvaporation(stateKey, selected.id);
//
//            // 3. בחירת כיוון (Orientation)
//            Orientation bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected, container);
//            if (bestOri == null) continue;
//
//            // 4. רישום ההנחה בשרשרת
//            chain.addStep(selected, bestOri, currentSpace, stateKey,pheromoneManager);
//            selected.decreaseQuantity();
//
//            // 5. עדכון חללים
//            List<Space> nextIterationSpaces = new ArrayList<>();
//
//            // א. הוספת החללים החדשים שנוצרו מהפיצול של החלל הנוכחי
//            List<Space> newSpaces = new SpaceManager().splitAndMerge(currentSpace, bestOri);
//            nextIterationSpaces.addAll(newSpaces);
//            // ב. בדיקת כל שאר החללים ברשימה וקיצוץ שלהם אם הם חופפים לארגז החדש
//            for (Space s : availableSpaces) {
//                if (isIntersecting(s, currentSpace, bestOri)) {
//                    nextIterationSpaces.addAll(splitSpaceAroundBox(s, currentSpace, bestOri));
//                } else {
//                    nextIterationSpaces.add(s);
//                }
//            }
//
//            // 6. ניקוי ומיון
//            availableSpaces = cleanAndOptimizeSpaces(
//                    new SpaceManager().mergeSpaces(nextIterationSpaces)
//            );
//        }
//        return chain;
//    }
private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
    // יצירת שרשרת טעינה לנמלה הנוכחית
    LoadingChain chain = new LoadingChain(container);

    // רשימת החללים הפעילה (מכילה VU, VR, VI ביחד בתחילת האיטרציה)
    List<Space> allSpaces = new ArrayList<>();
    allSpaces.add(new Space(0, 0, 0, container.getLength(), container.getWidth(), container.getHeight()));

    double currentLayerX = 0.0;                     // מעקב אחרי השכבה הנוכחית בציר X
    double currentLayerLength=0.0 ;
    GSMStrategy gsm = new GSMStrategy();            // אסטרטגיית GSM (גרידי)
    SpaceManager spaceManager = new SpaceManager(); // מנהל חללים (split/merge/clean)

    while (!allSpaces.isEmpty() && hasCargo(cargoTypes)) {

        // -----------------------------
        // 1. סיווג החללים ל־VU / VR / VI
        // -----------------------------
        double layerTol = 1e-6; // טולרנס אחיד לשכבה

        List<Space> VU = new ArrayList<>();
        List<Space> VR = new ArrayList<>();
        List<Space> VI = new ArrayList<>();

        double layerStartX = currentLayerX;
        double layerEndX   = currentLayerX + currentLayerLength; // עומק השכבה

        for (Space s : allSpaces) {
            if (s.x + 1e-6 >= layerStartX && s.x <= layerEndX + 1e-6) {
                VU.add(s);   // בתוך השכבה
            } else if (s.x < layerStartX - 1e-6) {
                VR.add(s);   // שמאלה
            } else {
                VI.add(s);   // ימינה → שכבה חדשה
            }
        }

        // -----------------------------
        // 2. מיזוג VR בלבד (כדי למנוע מיזוג בין שכבות)
        // -----------------------------
        if (!VR.isEmpty()) {
            VR = spaceManager.mergeSpaces(VR);
            VR = spaceManager.cleanAndOptimizeSpaces(VR);
        }


        // -----------------------------
        // 3. מיון VU בלבד בעזרת smartSortSpaces (עדיפות לשכבה הנוכחית)
        // -----------------------------
        // מיון VU בעזרת smartSortSpaces (עדיפות לשכבה הנוכחית)
        VU = smartSortSpaces(VU, currentLayerX);

// בחירת החלל הבא: קודם VU, אם ריק — VR, אם ריק — VI (ולאחר מכן עדכון שכבה)
        Space currentSpace = null;
        double previousLayerX = currentLayerX;
        boolean movedToNewLayer = false;

        if (!VU.isEmpty()) {
            currentSpace = VU.remove(0);
        } else if (!VR.isEmpty()) {
            currentSpace = VR.remove(0);
            // לא מעדכנים currentLayerX כאן — VR שייכים לאותה שכבה פיזית
        } else if (!VI.isEmpty()) {
            // מיון VI לפי X בלבד (הכי שמאלי קודם)
            VI.sort(Comparator.comparingDouble(s -> s.x));
            currentSpace = VI.remove(0);
            // עכשיו עוברים לשכבה חדשה
            currentLayerX = currentSpace.x;
            movedToNewLayer = true;
        } else {
            break;
        }


        // הרכבת רשימת כל החללים מחדש (VU + VR + VI) ללא currentSpace
        List<Space> remaining = new ArrayList<>();
        remaining.addAll(VU);
        remaining.addAll(VR);
        remaining.addAll(VI);

        // -----------------------------
        // 5. בחירת קופסה (Hybrid GSM/ASM) לפי האם זו שכבה חדשה
        // -----------------------------
// האם עברנו לשכבה חדשה ביחס ל־previousLayerX
        boolean isNewLayer = movedToNewLayer; // true רק אם לקחנו חלל מ‑VI

        String stateKey = pheromoneManager.generateStateWord(cargoTypes);
        CargoType selected = null;
        Orientation bestOri = null;

        // בתוך אותה שכבה — שילוב גרידי ו‑ASM
        double pTau = Math.exp(-Math.pow(((double) tau / M) * 3.0, 2) / 2.0);
        boolean useGreedy = new Random().nextDouble() <= pTau;
//        if (useGreedy)
//        {
            GSMStrategy.Pair<CargoType, Orientation> gsmChoice =gsm.selectCargo(currentSpace, cargoTypes,isNewLayer,pheromoneManager);
            if (gsmChoice != null) {
                selected = gsmChoice.first;
                bestOri = gsmChoice.second;
            } else {
                // טיפול במקרה של חוסר מועמד
                allSpaces = remaining;
                continue;
            }
//        }else {
//            selected =asm.selectCargo(currentSpace, cargoTypes, stateKey, tau);
//            if (selected == null) {
//                // אין קופסה מתאימה לחלל זה — נשמר את החלל ברשימת remaining ונמשיך
//                allSpaces = remaining;
//                continue;
//            }
//            bestOri = new OrientationOptimizer().getBestOrientation(currentSpace, selected);
//
//        }






        // -----------------------------
        // 7. בחירת אוריינטציה מתאימה
        // -----------------------------
        if (bestOri == null) {
            // לא נמצאה אוריינטציה מתאימה — נשמר החלל ונמשיך
            allSpaces = remaining;
            continue;
        }
if (isNewLayer) {
    currentLayerLength = bestOri.l;
}
    // -----------------------------
        // 8. רישום ההנחה והעדכון של הכמות
        // -----------------------------
        chain.addStep(selected, bestOri, currentSpace, stateKey);

        selected.decreaseQuantity();

        // -----------------------------
        // 9. פיצול החלל הנוכחי לשלושת השאריות (Right / Front / Top)
        // -----------------------------
        List<Space> nextIterationSpaces = new ArrayList<>();
        nextIterationSpaces.addAll(spaceManager.splitIntoResiduals(currentSpace, bestOri));

        // -----------------------------
        // 10. חיתוך חללים חופפים ברשימת remaining סביב הארגז החדש
        // -----------------------------
        for (Space s : remaining) {
            if (isIntersecting(s, currentSpace, bestOri)) {
                nextIterationSpaces.addAll(splitSpaceAroundBox(s, currentSpace, bestOri));
            } else {
                nextIterationSpaces.add(s);
            }
        }

        // -----------------------------
        // 11. מיזוג חללים שנוצרו (רק על החללים שנוצרו כרגע / VR המורחבים)
        // -----------------------------
        nextIterationSpaces = spaceManager.mergeSpaces(nextIterationSpaces);

        // -----------------------------
        // 12. ניקוי ואופטימיזציה (הסרת חללים קטנים / מוכלים)
        // -----------------------------
        nextIterationSpaces = spaceManager.cleanAndOptimizeSpaces(nextIterationSpaces);

        // -----------------------------
        // 13. עדכון כל החללים לולאה הבאה
        // -----------------------------
        allSpaces = nextIterationSpaces;
    }

    return chain;
}

        // ✅ זה הקוד שצריך להבין
private List<Space> smartSortSpaces(List<Space> spaces, double currentLayerX) {

    spaces.sort((a, b) -> {

        // ---------------------------------------------------------
        // 1. עדיפות לחללים ששייכים לשכבה הנוכחית (מאמר: Layered Loading)
        // ---------------------------------------------------------
        boolean aIsCurrent = Math.abs(a.x - currentLayerX) < 1e-9;
        boolean bIsCurrent = Math.abs(b.x - currentLayerX) < 1e-9;

        if (aIsCurrent && !bIsCurrent) return -1;   // a קודם
        if (!aIsCurrent && bIsCurrent) return 1;    // b קודם

        // ---------------------------------------------------------
        // 2. מיון לפי X — שכבות (קטן → גדול)
        // ---------------------------------------------------------
        int layerComp = Double.compare(a.x, b.x);
        if (layerComp != 0) return layerComp;

        // ---------------------------------------------------------
        // 3. מיון לפי Z — גובה (מאמר: Load bottom → top)
        // ---------------------------------------------------------
        int zComp = Double.compare(a.z, b.z);
        if (zComp != 0) return zComp;

        // ---------------------------------------------------------
        // 4. מיון לפי Y — שמאל → ימין (מאמר: Fill left → right)
        // ---------------------------------------------------------
        int yComp = Double.compare(a.y, b.y);
        if (yComp != 0) return yComp;

        // ---------------------------------------------------------
        // 5. עדיפות לשטח בסיס גדול יותר (L×W) — מאמר: maximize base area
        // ---------------------------------------------------------
        double baseA = a.L * a.W;
        double baseB = b.L * b.W;

        int baseComp = Double.compare(baseB, baseA); // גדול קודם
        if (baseComp != 0) return baseComp;

        // ---------------------------------------------------------
        // 6. אם הכל שווה — נפח גדול קודם (מאמר: reduce fragmentation)
        // ---------------------------------------------------------
        return Double.compare(b.getVolume(), a.getVolume());
    });

    return spaces;
}




    // פונקציה לבדיקה אם חלל נפגש עם ארגז
    private boolean isIntersecting(Space s, Space bPos, Orientation bOri) {
        return s.x < bPos.x + bOri.l && s.x + s.L > bPos.x &&//בודקים שהחלל מתחיל לפני שהארגז מסתיים ושהארגז מתחיל לפני שהארגז מסתיים
                s.y < bPos.y + bOri.w && s.y + s.W > bPos.y &&
                s.z < bPos.z + bOri.h && s.z + s.H > bPos.z;
    }

    // פיצול חלל קיים סביב ארגז חדש כדי שלא תהיה חפיפה
    private List<Space> splitSpaceAroundBox(Space s, Space bPos, Orientation bOri) {
        List<Space> result = new ArrayList<>();
        // חיתוך החלל s ל-6 חללים פוטנציאליים סביב התיבה המפריעה
        if (bPos.x > s.x) result.add(new Space(s.x, s.y, s.z, bPos.x - s.x, s.W, s.H));
        if (bPos.x + bOri.l < s.x + s.L) result.add(new Space(bPos.x + bOri.l, s.y, s.z, (s.x + s.L) - (bPos.x + bOri.l), s.W, s.H));
        if (bPos.y > s.y) result.add(new Space(s.x, s.y, s.z, s.L, bPos.y - s.y, s.H));
        if (bPos.y + bOri.w < s.y + s.W) result.add(new Space(s.x, bPos.y + bOri.w, s.z, s.L, (s.y + s.W) - (bPos.y + bOri.w), s.H));
        if (bPos.z > s.z) result.add(new Space(s.x, s.y, s.z, s.L, s.W, bPos.z - s.z));
        if (bPos.z + bOri.h < s.z + s.H) result.add(new Space(s.x, s.y, bPos.z + bOri.h, s.L, s.W, (s.z + s.H) - (bPos.z + bOri.h)));
        return result;
    }

    private List<Space> cleanAndOptimizeSpaces(List<Space> spaces) {
        // מיון לפי נפח יורד - עוזר לנו לבדוק רק מול חללים גדולים יותר
        spaces.sort((a, b) -> Double.compare(b.getVolume(), a.getVolume()));
        List<Space> filtered = new ArrayList<>();
        for (int i = 0; i < spaces.size(); i++) {
            Space s1 = spaces.get(i);
            if (s1.getVolume() <= 0.001) continue;
            boolean contained = false;
            // בודקים רק מול חללים שכבר נכנסו ל-filtered (הם הגדולים יותר)
            for (Space sBig : filtered) {
                if (isContained(s1, sBig)) {
                    contained = true;
                    break;
                }
            }
            if (!contained) filtered.add(s1);
        }
        return filtered;
    }

    private boolean isContained(Space s1, Space s2) {
        return s1.x >= s2.x && s1.y >= s2.y && s1.z >= s2.z &&
                s1.x + s1.L <= s2.x + s2.L && s1.y + s1.W <= s2.y + s2.W && s1.z + s1.H <= s2.z + s2.H;
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
        if (solution != null) {
            System.out.println("\n======================================");
            System.out.println("   FINAL OPTIMIZATION RESULTS");
            System.out.println("======================================");
            System.out.println("Final VUR: " + String.format("%.2f", solution.getVUR() * 100) + "%");
            System.out.println("Total Items Loaded: " + solution.getSteps().size());
            System.out.println("======================================\n");
        }
    }
}