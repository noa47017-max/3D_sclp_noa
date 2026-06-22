//package com.noa.packer.algorithms;
//
//import java.util.*;
//
//import com.noa.packer.models.Container;
//
//
//public class H {
//    private PheromoneManager pheromoneManager = new PheromoneManager();
//    private StrategySelector strategySelector = new StrategySelector(); // וודא שיש לך אובייקט כזה
//    private ASMStrategy asm = new ASMStrategy(pheromoneManager);
//
//    public LoadingChain startLoadingProcess(Container container, List<CargoType> cargoTypes) {
//
//        for (CargoType c : cargoTypes) {
//
//            Global.MIN_DIM = Math.min(Global.MIN_DIM, c.l);
//            Global.MIN_DIM = Math.min(Global.MIN_DIM, c.w);
//            Global.MIN_DIM = Math.min(Global.MIN_DIM, c.h);
//        }
//        LoadingChain bestGlobalSolution = null;
//
//        for (int tau = 0; tau < Global.M; tau++) {
//            LoadingChain bestIterationSolution = null;
//            LoadingChain[] arrChain = new LoadingChain[Global.m];
//
//            // 1. כל הנמלים בונות פתרונות
//            for (int i = 0; i < Global.m; i++) {
//                List<CargoType> antCargoes = deepCopyCargoes(cargoTypes);
//                LoadingChain currentChain = executeAntLoading(container, antCargoes, tau);
//                arrChain[i] = currentChain;
//
//                if (bestIterationSolution == null || currentChain.getVUR() > bestIterationSolution.getVUR()) {
//                    bestIterationSolution = currentChain;
//                }
//            }
//            // 2. אידוי גלובלי על כל הפרומונים (לפני החיזוקים)
//            pheromoneManager.applyGlobalEvaporation();
//            // 3. חיזוק גלובלי לפי כל הנמלים (Ant System)
//            // חישוב סכום VUR של כל הנמלים לשם משקל יחסי
//            double totalVUR = 0.0;
//            for (int i = 0; i < Global.m; i++) {
//                if (arrChain[i] != null) totalVUR += arrChain[i].getVUR();
//            }
//            if (totalVUR <= 0.0) totalVUR = 1e-9; // מניעת חלוקה באפס
//
//            // פרמטר כוונון לחיזוק הגלובלי
//            double q = 0.5; // ניתן לכוונן בניסויים
//
//            // עדכון פרומונים לכל נמלה לפי משקל היחסי שלה
//            for (int i = 0; i < Global.m; i++) {
//                LoadingChain chain = arrChain[i];
//                if (chain == null) continue;
//
//                double weight = chain.getVUR() / totalVUR; // משקל יחסי
//                double deltaBase = (weight)*7;             // delta פרמטרי
//                // delta פרמטרי
//                for (LoadingChain.PlacedBoxInfo step : chain.getSteps()) {
//                    pheromoneManager.updateGlobalPheromone(
//                            step.stateKey,
//                            step.cargoType,
//                            step.direction,
//                            step.leftBackBottom[0],
//                            step.leftBackBottom[1],
//                            step.leftBackBottom[2],
//                            step.remainingSpace,
//                            deltaBase,
//                            tau
//                    );
//                }
//            }
//
//            // 4. עדכון הטוב ביותר גלובלי
//            if (bestIterationSolution != null) {
//                if (bestGlobalSolution == null || bestIterationSolution.getVUR() > bestGlobalSolution.getVUR()) {
//                    bestGlobalSolution = bestIterationSolution;
//                    System.out.println("Iteration " + tau + ": New Global Best VUR: " +
//                            String.format("%.2f", bestGlobalSolution.getVUR() * 100) + "%");
//                }
//            }
//        }
//        Global.MIN_DIM = Double.MAX_VALUE;
//
//        outputResults(bestGlobalSolution);
//        return bestGlobalSolution;
//    }
//
//    // בדיקה אם החלל הנוכחי הוא תחילת שכבה חדשה
//    private boolean isNewLayer(Space current, List<Space> stack) {
//        // שכבה חדשה = אין חללים אחרים באותו X
//        return stack.stream()
//                .noneMatch(s -> Math.abs(s.x - current.x) < 1e-9);
//    }
//
//    private LoadingChain executeAntLoading(Container container, List<CargoType> cargoTypes, int tau) {
//        // יצירת שרשרת טעינה לנמלה הנוכחית
//        LoadingChain chain = new LoadingChain(container);
//
//        // רשימת החללים הפעילה (מכילה VU, VR, VI ביחד בתחילת האיטרציה)
//        List<Space> allSpaces = new ArrayList<>();
//        allSpaces.add(new Space(0, 0, 0, container.getLength(), container.getWidth(), container.getHeight()));
//
//        double currentLayerX = 0.0;                     // מעקב אחרי השכבה הנוכחית בציר X
//        double currentLayerLength=0.0 ;
//        GSMStrategy gsm = new GSMStrategy();            // אסטרטגיית GSM (גרידי)
//        SpaceManager spaceManager = new SpaceManager(); // מנהל חללים (split/merge/clean)
//        List<Space> VU = new ArrayList<>();
//        List<Space> VR = new ArrayList<>();
//        Space VI = new Space(0, 0, 0, container.getLength(), container.getWidth(), container.getHeight());
//
//        while ((VI.getVolume()>0 ||!VU.isEmpty()) && hasCargo(cargoTypes)) {
//
//            // 1. סיווג החללים ל־VU / VR / VI
//            double layerTol = 1e-6; // טולרנס אחיד לשכבה
//            double layerStartX = currentLayerX;
//            double layerEndX   = currentLayerX + currentLayerLength; // עומק השכבה
//            Space currentSpace = null;
//            boolean movedToNewLayer = false;
//
//            // 2. מיזוג VR בלבד (כדי למנוע מיזוג בין שכבות)
//            if (!VU.isEmpty()) {
//                VU = smartSortSpaces(VU, currentLayerX);
//                currentSpace = VU.remove(0);
//            }else {
//                // מיון VI לפי X בלבד (הכי שמאלי קודם)
//                currentSpace = VI;
//
//                currentLayerX = currentSpace.x;
//                movedToNewLayer = true;
//            }
//
//            double previousLayerX = currentLayerX;
//            // 5. בחירת קופסה (Hybrid GSM/ASM) לפי האם זו שכבה חדשה
//            // האם עברנו לשכבה חדשה ביחס ל־previousLayerX
//
//            String stateKey = pheromoneManager.generateStateWord(cargoTypes);
//            CargoType selected = null;
//            Orientation bestOri = null;
//
//            // Equation 12: P(τ) = שטח ה-Normal Distribution מ-τ עד M, מנורמל ל-[0,1]
//            // בתחילה (τ≈0): P≈1 → סיכוי גבוה ל-GSM  |  בסוף (τ≈M): P≈0 → ASM שולט
//            double x = ((double) tau / Global.M) * 3.0;
//            double pTau = 2.0 * (1.0 - normalCDF(x));
//            boolean useGreedy = new Random().nextDouble() <= pTau;
//
//            GSMStrategy.Pair<CargoType, Orientation> Choice =useGreedy?
//                    gsm.selectCargo(currentSpace, cargoTypes,pheromoneManager,chain.getSteps()):
//                    asm.selectCargo(currentSpace, cargoTypes, stateKey);
//            if (Choice != null) {
//                selected = Choice.first;
//                bestOri = Choice.second;
//            } else {
//                if (movedToNewLayer) {
//                    // כשלנו על VI עצמו — אין מה לעשות בחלל זה
//                    VI = new Space(0, 0, 0, 0, 0, 0); // מרוקנים את VI
//                } else {
//                    VR.add(currentSpace); // כשלנו על חלל מ-VU בלבד
//                }
//                continue; // טיפול במקרה של חוסר מועמד
//            }
//
//
//
//            // 8. רישום ההנחה והעדכון של הכמות
//            chain.addStep(selected, bestOri, currentSpace, stateKey);
//
//            selected.decreaseQuantity();
//            List<Space> nextIterationSpaces = new ArrayList<>();
//            if (movedToNewLayer) {
//                currentLayerLength = bestOri.l;
//                // 1. הוספת החללים החדשים (השאריות של הארגז עצמו)
//                nextIterationSpaces.addAll(spaceManager.splitIntoResidualsNewLayerVU(currentSpace, bestOri));
//                // 2. מתיחת חללי ה-VR קדימה בצורה בטוחה!
//                for (Space s : VR) {
//                    Space stretched = s;
//
//                    // נמתח את ה-VR פנימה רק אם הוא באמת מגיע עד קו התחלת השכבה החדשה (בקירוב של אפסילון)
//                    if (Math.abs((s.x + s.L) - currentSpace.x) < 1e-4) {
//                        stretched = new Space(s.x, s.y, s.z, s.L + currentLayerLength, s.W, s.H);
//                        // 3. חובה לבדוק: האם ה-VR המתוח מתנגש בארגז החדש שהרגע הנחנו במרכז השכבה?
//                        if (isIntersecting(stretched, currentSpace, bestOri)) {
//                            // אם כן, הפונקציה שלך תחתוך אותו סביב הארגז בצורה מושלמת בלי לייצר Overlap
//                            nextIterationSpaces.addAll(splitSpaceAroundBox(stretched, currentSpace, bestOri));
//                        } else {
//                            // אם לא מתנגש (למשל יושב רחוק בצד), פשוט נוסיף אותו
//                            nextIterationSpaces.add(stretched);
//                        }
//                    }
//                }
//
//                // 4. מיזוג וניקוי (הפעם יש לנו חללים רציפים וחוקיים)
//
//                VI = spaceManager.splitIntoResidualsNewLayerVI(currentSpace, bestOri);
//                VR.clear();
//            } else {
//                // 9. פיצול החלל הנוכחי לשלושת השאריות (Right / Front / Top)
//                nextIterationSpaces.addAll(spaceManager.splitIntoResiduals(currentSpace, bestOri));
//                for (Space s : VU) {
//                    if (isIntersecting(s, currentSpace, bestOri)) {
//                        nextIterationSpaces.addAll(splitSpaceAroundBox(s, currentSpace, bestOri));
//                    } else {
//                        nextIterationSpaces.add(s);
//                    }
//                }
//                List<Space> updatedVR = new ArrayList<>();
//                for (Space s : VR) {
//                    if (isIntersecting(s, currentSpace, bestOri)) {
//                        // אם הארגז החדש חותך חלל ב-VR, נפצל אותו כדי שיישאר נקי
//                        updatedVR.addAll(splitSpaceAroundBox(s, currentSpace, bestOri));
//                    } else {
//                        updatedVR.add(s);
//                    }
//                }
//                // ניקוי אופטימיזציה קל ל-VR כדי למנוע ריבוי חללים קטנים מדי
//                VR = spaceManager.cleanAndOptimizeSpaces(updatedVR);
//            }
//            // 11. מיזוג חללים שנוצרו (רק על החללים שנוצרו כרגע / VR המורחבים)
//            // 12. ניקוי ואופטימיזציה (הסרת חללים קטנים / מוכלים)
//            nextIterationSpaces = spaceManager.mergeSpaces(nextIterationSpaces);
//            nextIterationSpaces = spaceManager.cleanAndOptimizeSpaces(nextIterationSpaces);
//
//            VU = nextIterationSpaces;
//        }
//        return chain;
//    }
//
//    // ✅ זה הקוד שצריך להבין
//    private List<Space> smartSortSpaces(List<Space> spaces, double currentLayerX) {
//        double eps = 1e-4;
//        spaces.sort((a, b) -> {
//
//            // 2. מיון לפי X — שכבות (קטן → גדול)
//            // 1. מיון לפי X (עומק/שכבות) — קטן ← גדול
//
//            // 3. מיון לפי Y (רוחב) — Fill left → right
//            if (Math.abs(a.x - b.x) > eps) {
//                return Double.compare(a.x, b.x);
//            }
//            // 2. מיון לפי Z (גובה) — Load bottom → top
//
//            if (Math.abs(a.y - b.y) > eps) {
//                return Double.compare(a.y, b.y);
//            }
//            if (Math.abs(a.z - b.z) > eps) {
//                return Double.compare(a.z, b.z);
//            }
//
//
//            // 5. עדיפות לשטח בסיס גדול יותר (L×W) — מאמר: maximize base area
//            double baseA = a.L * a.W;
//            double baseB = b.L * b.W;
//
//            int baseComp = Double.compare(baseB, baseA); // גדול קודם
//            if (baseComp != 0) return baseComp;
//
//            // 6. אם הכל שווה — נפח גדול קודם (מאמר: reduce fragmentation)
//            return Double.compare(b.getVolume(), a.getVolume());
//        });
//
//        return spaces;
//    }
//    // פונקציה לבדיקה אם חלל נפגש עם ארגז
//
//    private boolean isIntersecting(
//            Space s,
//            Space bPos,
//            Orientation bOri
//    ) {
//
//        return
//                s.x < bPos.x + bOri.l &&
//                        s.x + s.L > bPos.x &&
//
//                        s.y < bPos.y + bOri.w &&
//                        s.y + s.W > bPos.y &&
//
//                        s.z < bPos.z + bOri.h &&
//                        s.z + s.H > bPos.z;
//    }
//    // פיצול חלל קיים סביב ארגז חדש כדי שלא תהיה חפיפה
//    private List<Space> splitSpaceAroundBoxold(Space s, Space bPos, Orientation bOri) {
//        List<Space> result = new ArrayList<>();
//        // חיתוך החלל s ל-6 חללים פוטנציאליים סביב התיבה המפריעה
//        if (bPos.x > s.x) result.add(new Space(s.x, s.y, s.z, bPos.x - s.x, s.W, s.H));
//        if (bPos.x + bOri.l < s.x + s.L) result.add(new Space(bPos.x + bOri.l, s.y, s.z, (s.x + s.L) - (bPos.x + bOri.l), s.W, s.H));
//        if (bPos.y > s.y) result.add(new Space(s.x, s.y, s.z, s.L, bPos.y - s.y, s.H));
//        if (bPos.y + bOri.w < s.y + s.W) result.add(new Space(s.x, bPos.y + bOri.w, s.z, s.L, (s.y + s.W) - (bPos.y + bOri.w), s.H));
//        if (bPos.z > s.z) result.add(new Space(s.x, s.y, s.z, s.L, s.W, bPos.z - s.z));
//        if (bPos.z + bOri.h < s.z + s.H) result.add(new Space(s.x, s.y, bPos.z + bOri.h, s.L, s.W, (s.z + s.H) - (bPos.z + bOri.h)));
//        return result;
//    }
//    private List<Space> splitSpaceAroundBox(
//            Space s,
//            Space bPos,
//            Orientation bOri
//    ) {
//
//        List<Space> result = new ArrayList<>();
//
//        double bx1 = bPos.x;
//        double by1 = bPos.y;
//        double bz1 = bPos.z;
//
//        double bx2 = bPos.x + bOri.l;
//        double by2 = bPos.y + bOri.w;
//        double bz2 = bPos.z + bOri.h;
//
//        double sx1 = s.x;
//        double sy1 = s.y;
//        double sz1 = s.z;
//
//        double sx2 = s.x + s.L;
//        double sy2 = s.y + s.W;
//        double sz2 = s.z + s.H;
//
//        // =========================================================
//        // LEFT
//        // =========================================================
//
//        if (bx1 > sx1 + 1e-6) {
//
//            result.add(
//                    new Space(
//                            sx1,
//                            sy1,
//                            sz1,
//                            bx1 - sx1,
//                            s.W,
//                            s.H
//                    )
//            );
//        }
//
//        // =========================================================
//        // RIGHT
//        // =========================================================
//
//        if (bx2 < sx2 - 1e-6) {
//
//            result.add(
//                    new Space(
//                            bx2,
//                            sy1,
//                            sz1,
//                            sx2 - bx2,
//                            s.W,
//                            s.H
//                    )
//            );
//        }
//
//        // =========================================================
//        // FRONT
//        // =========================================================
//
//        double ix1 = Math.max(sx1, bx1);
//        double ix2 = Math.min(sx2, bx2);
//
//        if (by1 > sy1 + 1e-6 &&
//                ix2 - ix1 > 1e-6) {
//
//            result.add(
//                    new Space(
//                            sx1,
//                            sy1,
//                            sz1,
//                            s.L,
//                            by1 - sy1,
//                            s.H
//                    )
//            );
//        }
//
//        // =========================================================
//        // BACK
//        // =========================================================
//
//        if (by2 < sy2 - 1e-6 &&
//                ix2 - ix1 > 1e-6) {
//
//            result.add(
//                    new Space(
//                            sx1,
//                            by2,
//                            sz1,
//                            s.L,
//                            sy2 - by2,
//                            s.H
//                    )
//            );
//        }
//
//        // =========================================================
//        // BOTTOM
//        // =========================================================
//
//        double iy1 = Math.max(sy1, by1);
//        double iy2 = Math.min(sy2, by2);
//
//        if (bz1 > sz1 + 1e-6 &&
//                ix2 - ix1 > 1e-6 &&
//                iy2 - iy1 > 1e-6) {
//
//            result.add(
//                    new Space(
//                            sx1,
//                            sy1,
//                            sz1,
//                            s.L,
//                            s.W,
//                            bz1 - sz1
//                    )
//            );
//        }
//
//        // =========================================================
//        // TOP
//        // =========================================================
//
//        if (bz2 < sz2 - 1e-6 &&
//                ix2 - ix1 > 1e-6 &&
//                iy2 - iy1 > 1e-6) {
//
//            result.add(
//                    new Space(
//                            sx1,
//                            sy1,
//                            bz2,
//                            s.L -(bx2-sx1),
//                            s.W -(by2-sy1),
//                            sz2 - bz2
//                    )
//            );
//        }
//
//        return result;
//    }
//    // Equation 12: CDF של התפלגות נורמלית סטנדרטית
//    private double normalCDF(double x) {
//        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
//    }
//
//    // קירוב של erf (שגיאה מקסימלית: 1.5×10⁻⁷) — Abramowitz & Stegun
//    private double erf(double x) {
//        double sign = x >= 0 ? 1.0 : -1.0;
//        x = Math.abs(x);
//        double t = 1.0 / (1.0 + 0.3275911 * x);
//        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t)
//                + 1.421413741) * t - 0.284496736) * t + 0.254829592)
//                * t * Math.exp(-x * x);
//        return sign * y;
//    }
//
//    private List<CargoType> deepCopyCargoes(List<CargoType> original) {
//        List<CargoType> copy = new ArrayList<>();
//        for (CargoType ct : original) copy.add(new CargoType(ct));
//        return copy;
//    }
//
//    private boolean hasCargo(List<CargoType> list) {
//        return list.stream().anyMatch(c -> c.quantity > 0);
//    }
//
//    private void outputResults(LoadingChain solution) {
//        if (solution != null) {
//            System.out.println("\n======================================");
//            System.out.println("   FINAL OPTIMIZATION RESULTS");
//            System.out.println("======================================");
//            System.out.println("Final VUR: " + String.format("%.2f", solution.getVUR() * 100) + "%");
//            System.out.println("Total Items Loaded: " + solution.getSteps().size());
//            System.out.println("======================================\n");
//        }
//    }
//}