package com.noa.packer;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PackingController {

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> creds) {
        Map<String, Object> response = new HashMap<>();
        // בדיקת דמה
        if ("admin".equals(creds.get("username"))) {
            response.put("status", "success");
            response.put("name", "נועה");
        } else {
            response.put("status", "fail");
        }
        return response;
    }

    // בתוך PackingController.java

    @PostMapping("/solve")
    public List<LoadingChain.PlacedBoxInfo> solve(@RequestBody PackingRequest request) {
        // 1. חילוץ המכולה
        double L = request.container.get("l");
        double W = request.container.get("w");
        double H = request.container.get("h");

        // 2. יצירת האובייקטים של האלגוריתם שלך
        Container container = new Container(L, W, H);
        List<CargoType> cargoTypes = convertToCargoTypes(request.boxes);

        // 3. הפעלת האלגוריתם
        HybridLoadingSystem system = new HybridLoadingSystem();
        return system.startLoadingProcess(container, cargoTypes).getSteps();
    }

    private List<CargoType> convertToCargoTypes(List<Map<String, Object>> boxes) {
        List<CargoType> cargoTypes = new ArrayList<>();

        for (Map<String, Object> boxMap : boxes) {
            // המרה בטוחה של הנתונים מה-Map
            int typeId = (int) boxMap.get("typeId");
            double l = Double.parseDouble(boxMap.get("l").toString());
            double w = Double.parseDouble(boxMap.get("w").toString());
            double h = Double.parseDouble(boxMap.get("h").toString());
            int quantity = (int) boxMap.get("quantity");

            // יצירת האובייקט והוספה לרשימה
            // ודאי שהקונסטרקטור של CargoType שלך תואם לסדר הזה
            cargoTypes.add(new CargoType(typeId, l, w, h, quantity,Arrays.asList(0,1,2,3,4,5)));
        }

        return cargoTypes;
    }

    // מחלקה פנימית לעזרה בקבלת הנתונים
    public static class PackingRequest {
        public Map<String, Double> container;
        public List<Map<String, Object>> boxes;
    }
}