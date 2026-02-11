import java.util.*;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // 1. הגדרת המכולה (אורך, רוחב, גובה)

        Container container = new Container(587, 233, 220);

        List<CargoType> br1Test = new ArrayList<>();
        // ID, L, W, H, Quantity
        br1Test.add(new CargoType(1, 108, 76, 30, 40,Arrays.asList(0,1, 2, 3, 4,5)));
        br1Test.add(new CargoType(2, 110, 43, 25, 33,Arrays.asList(0,1, 2, 3, 4,5)));
        br1Test.add(new CargoType(3, 92, 81, 55, 21,Arrays.asList(0,1, 2, 3, 4,5)));

        HybridLoadingSystem system = new HybridLoadingSystem();
        System.out.println("--- Starting Simulation ---");

        system.startLoadingProcess(container, br1Test);

        System.out.println("--- Simulation Finished ---");
    }
}
