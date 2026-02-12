package com.noa.packer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        // השורה הזו מפעילה את השרת על פורט 8080
        SpringApplication.run(Main.class, args);
    }
}

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
//public class Main {
//    public void exportAndShow(LoadingChain bestSolution) {
//        try {
//            // 1. יצירת תוכן הקובץ data.js
//            String content = "const mySteps = " + bestSolution.toJSON() + ";";
//
//            // 2. כתיבה לקובץ (נשמר בתיקיית הפרויקט)
//            FileWriter writer = new FileWriter("data.js");
//            writer.write(content);
//            writer.close();
//
//            System.out.println("נתונים יוצאו ל-data.js בהצלחה!");
//
//            // 3. פתיחת קובץ ה-HTML בדפדפן באופן אוטומטי
//            File htmlFile = new File("index.html");
//            if (Desktop.isDesktopSupported()) {
//                Desktop.getDesktop().browse(htmlFile.toURI());
//            } else {
//                System.out.println("לא ניתן לפתוח דפדפן אוטומטית, פתח את index.html ידנית.");
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    public static void main(String[] args) {
//        // 1. הגדרת המכולה (אורך, רוחב, גובה)
//
////        Container container = new Container(300, 200, 180);
////
////        List<CargoType> br1Test = new ArrayList<>();
////        // ID, L, W, H, Quantity
////        br1Test.add(new CargoType(1, 60, 70, 40, 12,Arrays.asList(0,1, 2, 3, 4,5)));
////        br1Test.add(new CargoType(2, 80, 50, 50, 10,Arrays.asList(0,1, 2, 3, 4,5)));
////        br1Test.add(new CargoType(3, 120, 100, 30, 16,Arrays.asList(0,1, 2, 3, 4,5)));
////        br1Test.add(new CargoType(4, 30, 30, 90, 15,Arrays.asList(0,1, 2, 3, 4,5)));
////        br1Test.add(new CargoType(5, 45, 45, 45, 20,Arrays.asList(0,1, 2, 3, 4,5)));
////        Container container4 = new Container(50, 22, 14);
////
////        List<CargoType> tsaiTest4 = new ArrayList<>();
////// ID, L, W, H, Quantity, Orientations
////        tsaiTest4.add(new CargoType(1, 25, 8, 6, 2, Arrays.asList(0,1,2,3,4,5)));
////        tsaiTest4.add(new CargoType(2, 20, 10, 5, 20, Arrays.asList(0,1,2,3,4,5)));
////        tsaiTest4.add(new CargoType(3, 16, 7, 3, 8, Arrays.asList(0,1,2,3,4,5)));
////        tsaiTest4.add(new CargoType(4, 15, 12, 6, 5, Arrays.asList(0,1,2,3,4,5)));
////        tsaiTest4.add(new CargoType(5, 22, 8, 3, 7, Arrays.asList(0,1,2,3,4,5)));
////        tsaiTest4.add(new CargoType(6, 20, 10, 4, 80, Arrays.asList(0,1,2,3,4,5)));
////        Container containerBicycle = new Container(1202, 235, 269);
////
////        List<CargoType> bicycleTest = new ArrayList<>();
////// ID, L, W, H, Quantity
////        bicycleTest.add(new CargoType(1, 120, 26, 48, 150, Arrays.asList(0,1,2,3,4,5))); // HB02
////        bicycleTest.add(new CargoType(3, 69, 31, 58, 100, Arrays.asList(0,1,2,3,4,5)));  // HB04
////        bicycleTest.add(new CargoType(4, 75, 18, 87, 250, Arrays.asList(0,1,2,3,4,5)));  // HB07 (Approx. 18.5cm)
////
////        Container container11 = new Container(500, 150, 150);
////
////        List<CargoType> desktopTest = new ArrayList<>();
////// ID, L, W, H, Quantity (5 units per type)
////        desktopTest.add(new CargoType(1, 57, 53, 24, 5, Arrays.asList(0,1,2,3,4,5))); // Host Case
////        desktopTest.add(new CargoType(2, 39, 34, 30, 85, Arrays.asList(0,1,2,3,4,5))); // Mouse/Wires
////        desktopTest.add(new CargoType(3, 51, 19, 4, 35, Arrays.asList(0,1,2,3,4,5)));  // Keyboard
////        desktopTest.add(new CargoType(4, 58, 12, 39, 5, Arrays.asList(0,1,2,3,4,5))); // 22" Monitor
////        desktopTest.add(new CargoType(5, 63, 12, 41, 45, Arrays.asList(0,1,2,3,4,5))); // 24" Monitor
////        desktopTest.add(new CargoType(6, 69, 16, 45, 5, Arrays.asList(0,1,2,3,4,5))); // 27" Monitor
////        desktopTest.add(new CargoType(7, 80, 13, 39, 45, Arrays.asList(0,1,2,3,4,5))); // 29" Monitor
////        desktopTest.add(new CargoType(8, 86, 51, 17, 15, Arrays.asList(0,1,2,3,4,5))); // 32" Monitor
////
////        HybridLoadingSystem system = new HybridLoadingSystem();
////        Container containerBR1 = new Container(587, 233, 220);
////
////        List<CargoType> BR1 = new ArrayList<>();
////
////        BR1.add(new CargoType(1, 100, 60, 40, 120, Arrays.asList(0,1,2,3,4,5)));
////        BR1.add(new CargoType(2, 102, 58, 42, 120, Arrays.asList(0,1,2,3,4,5)));
////        BR1.add(new CargoType(3, 98, 62, 38, 120, Arrays.asList(0,1,2,3,4,5)));
////        Container containerBR7 = new Container(587, 233, 220);
////
////        List<CargoType> BR7 = new ArrayList<>();
////
////        BR7.add(new CargoType(1, 20, 20, 15, 400, Arrays.asList(0,1,2,3,4,5)));
////        BR7.add(new CargoType(2, 40, 35, 25, 250, Arrays.asList(0,1,2,3,4,5)));
////        BR7.add(new CargoType(3, 60, 50, 40, 150, Arrays.asList(0,1,2,3,4,5)));
////        BR7.add(new CargoType(4, 90, 70, 55, 80, Arrays.asList(0,1,2,3,4,5)));
////        BR7.add(new CargoType(5, 130, 100, 75, 50, Arrays.asList(0,1,2,3,4,5)));
////        BR7.add(new CargoType(6, 170, 130, 95, 30, Arrays.asList(0,1,2,3,4,5)));
////        BR7.add(new CargoType(7, 220, 180, 120, 20, Arrays.asList(0,1,2,3,4,5)));
////        System.out.println("--- Starting Simulation ---");
////        system.startLoadingProcess(containerBR1, BR1);
////        System.out.println("--- Starting Simulation 7 ---");
////
////        system.startLoadingProcess(containerBR7, BR7);
////        Container containerBR6 = new Container(587, 233, 220);
////
////        List<CargoType> BR6 = new ArrayList<>();
////
////        BR6.add(new CargoType(1, 30, 30, 20, 200, Arrays.asList(0,1,2,3,4,5)));
////        BR6.add(new CargoType(2, 180, 120, 90, 40, Arrays.asList(0,1,2,3,4,5)));
////        BR6.add(new CargoType(3, 60, 45, 35, 150, Arrays.asList(0,1,2,3,4,5)));
////        BR6.add(new CargoType(4, 200, 150, 100, 30, Arrays.asList(0,1,2,3,4,5)));
////        BR6.add(new CargoType(5, 45, 35, 25, 180, Arrays.asList(0,1,2,3,4,5)));
////        System.out.println("--- Starting Simulation 6 ---");
////
////        system.startLoadingProcess(containerBR6, BR6);
////        Container containerBR5 = new Container(587, 233, 220);
////
////        List<CargoType> BR5 = new ArrayList<>();
////
////        BR5.add(new CargoType(1, 40, 30, 20, 300, Arrays.asList(0,1,2,3,4,5)));
////        BR5.add(new CargoType(2, 45, 35, 25, 250, Arrays.asList(0,1,2,3,4,5)));
////        BR5.add(new CargoType(3, 50, 40, 30, 200, Arrays.asList(0,1,2,3,4,5)));
////        BR5.add(new CargoType(4, 35, 25, 18, 350, Arrays.asList(0,1,2,3,4,5)));
////        System.out.println("--- Starting Simulation 5---");
////
////        system.startLoadingProcess(containerBR5, BR5);
////        Container containerBR4 = new Container(587, 233, 220);
////
////        List<CargoType> BR4 = new ArrayList<>();
////
////        BR4.add(new CargoType(1, 200, 120, 80, 40, Arrays.asList(0,1,2,3,4,5)));
////        BR4.add(new CargoType(2, 60, 40, 30, 150, Arrays.asList(0,1,2,3,4,5)));
////        BR4.add(new CargoType(3, 180, 100, 70, 50, Arrays.asList(0,1,2,3,4,5)));
////        BR4.add(new CargoType(4, 90, 60, 45, 100, Arrays.asList(0,1,2,3,4,5)));
////        BR4.add(new CargoType(5, 150, 90, 60, 60, Arrays.asList(0,1,2,3,4,5)));
////        System.out.println("--- Starting Simulation 4 ---");
////
////        system.startLoadingProcess(containerBR4, BR4);
////        Container containerBR3 = new Container(587, 233, 220);
////
////        List<CargoType> BR3 = new ArrayList<>();
////
////        BR3.add(new CargoType(1, 150, 100, 60, 60, Arrays.asList(0,1,2,3,4,5)));
////        BR3.add(new CargoType(2, 90, 70, 40, 100, Arrays.asList(0,1,2,3,4,5)));
////        BR3.add(new CargoType(3, 130, 90, 55, 70, Arrays.asList(0,1,2,3,4,5)));
////        BR3.add(new CargoType(4, 110, 60, 45, 80, Arrays.asList(0,1,2,3,4,5)));
////
////        System.out.println("--- Starting Simulation 3 ---");
////
////        system.startLoadingProcess(containerBR3, BR3);
//        Container containerBR2 = new Container(587, 233, 220);
//
//        List<CargoType> BR2 = new ArrayList<>();
//      HybridLoadingSystem system = new HybridLoadingSystem();
//
//        BR2.add(new CargoType(1, 120, 80, 50, 90, Arrays.asList(0,1,2,3,4,5)));
//        BR2.add(new CargoType(2, 110, 70, 45, 100, Arrays.asList(0,1,2,3,4,5)));
//        BR2.add(new CargoType(3, 130, 85, 55, 80, Arrays.asList(0,1,2,3,4,5)));
//        BR2.add(new CargoType(4, 115, 75, 48, 90, Arrays.asList(0,1,2,3,4,5)));
//        System.out.println("--- Starting Simulation 2 ---");
//        system.startLoadingProcess(containerBR2, BR2);
//        System.out.println("--- Simulation Finished ---");
//    }
//}
