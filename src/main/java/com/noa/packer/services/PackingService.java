package com.noa.packer.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.noa.packer.algorithms.CargoType;
import com.noa.packer.algorithms.HybridLoadingSystem;
import com.noa.packer.controllers.PackingController;
import com.noa.packer.dto.PackingRequestDTO;
import com.noa.packer.models.Box;
import com.noa.packer.repositories.BoxRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.noa.packer.algorithms.LoadingChain;
import com.noa.packer.models.Container;
import com.noa.packer.models.PackingResult;
import com.noa.packer.repositories.ContainerRepository;
import com.noa.packer.repositories.PackingResultRepository;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class PackingService {
    @Autowired
    private PackingResultRepository packingResultRepository;  // ← הזרקה
    @Autowired
    private ContainerRepository containerRepository;  // ← הזרקה
    @Autowired
    private BoxRepository boxRepository;

    public ObjectId saveResult(ObjectId userId, LoadingChain chain) {
    List<PackingResult.PlacedBoxData> placements = new ArrayList<>();

    for (LoadingChain.PlacedBoxInfo step : chain.getSteps()) {
        placements.add(new PackingResult.PlacedBoxData(
            step.cargoType,
            step.direction,
            step.leftBackBottom,
            step.rightFrontTop
            // stateKey ו-remainingSpace לא נשמרים
        ));
    }
    Container container = containerRepository.getById(chain.getContainerID());
    PackingResult result = new PackingResult(
    chain.getContainerID(),
    container.getVolume(),        // containerVolume
    chain.getTotalCargoVolume(),  // totalCargoVolume
    placements
    );
   

    return packingResultRepository.save(result);
}


    private List<CargoType> convertToCargoTypes(List<Map<String, Object>> boxes) {
        List<CargoType> cargoTypes = new ArrayList<>();
        for (Map<String, Object> boxMap : boxes) {
            int    typeId   = (int) boxMap.get("typeId");
            double l        = Double.parseDouble(boxMap.get("l").toString());
            double w        = Double.parseDouble(boxMap.get("w").toString());
            double h        = Double.parseDouble(boxMap.get("h").toString());
            int    quantity = (int) boxMap.get("quantity");

            // שליפת מערך ה-allowedRotations הדינמי מהבקשה
            List<Integer> allowedRotations = boxMap.containsKey("allowedRotations") ?
                    (List<Integer>) boxMap.get("allowedRotations") : Arrays.asList(0, 1, 2, 3, 4, 5);

            cargoTypes.add(new CargoType(typeId, l, w, h, quantity, allowedRotations));
        }
        return cargoTypes;
    }
    public ResponseEntity<?> solve(@RequestBody PackingRequestDTO request) {
        try {
            if (request.userId == null || request.userId.isEmpty()) {
                return ResponseEntity.status(400)
                        .body("שגיאה: userId חסר בבקשה");
            }

            // 1. חילוץ מידות המכולה
            double L = request.container.get("l");
            double W = request.container.get("w");
            double H = request.container.get("h");

            ObjectId userId = new ObjectId(request.userId);
            Container container = new Container(userId, L, W, H);

            // 2 + 3. שמירה למסד נתונים - תבוצע אך ורק אם זה לא בנצ'מארק
            if (!request.isBenchmark) {
                containerRepository.save(container);

                for (Map<String, Object> boxMap : request.boxes) {
                    // חילוץ מערך הסיבובים הדינמי שהגיע מהמשתמש, אם לא קיים שמים ברירת מחדל (0-5)
                    List<Integer> allowedRotations = boxMap.containsKey("allowedRotations") ?
                            (List<Integer>) boxMap.get("allowedRotations") : Arrays.asList(0, 1, 2, 3, 4, 5);

                    Box box = new Box(
                            userId,
                            Double.parseDouble(boxMap.get("l").toString()),
                            Double.parseDouble(boxMap.get("w").toString()),
                            Double.parseDouble(boxMap.get("h").toString()),
                            (int) boxMap.get("quantity"),
                            allowedRotations
                    );
                    boxRepository.save(box);
                }
            }

            // 4. המרת ארגזים (תוך שימוש במערך הסיבובים המותאם אישית)
            List<CargoType> cargoTypes = convertToCargoTypes(request.boxes);

            // 5. הפעלת האלגוריתם
            HybridLoadingSystem system = new HybridLoadingSystem();
            LoadingChain result = system.startLoadingProcess(container, cargoTypes);
            System.out.println("Best VUR: " + String.format("%.2f", result.getVUR() * 100) + "%");

            // 6. שמירת התוצאה במסד - רק אם זה לא בנצ'מארק
            if (!request.isBenchmark) {
                saveResult(userId, result);
            }

            // 7. החזרת התוצאה לפרונט
            return ResponseEntity.ok(result.getSteps());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in /api/solve: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body("שגיאה בחישוב: " + e.getMessage());
        }
    }
 /**
     * מחזיר את כל הסידורים של משתמש מסוים, מהחדש לישן.
     */
    public List<PackingResult> getHistory(ObjectId userId) {
        return packingResultRepository.getByUserId(userId);
    }
}
