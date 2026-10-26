package com.noa.packer.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import com.noa.packer.algorithms.CargoType;
import com.noa.packer.algorithms.HybridLoadingSystem;
import com.noa.packer.dto.HistoryItemDTO;
import com.noa.packer.dto.PackingRequestDTO;
import com.noa.packer.dto.PackingResultDTO;
import com.noa.packer.models.Box;
import com.noa.packer.repositories.BoxRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.noa.packer.algorithms.LoadingChain;
import com.noa.packer.models.Container;
import com.noa.packer.models.PackingResult;
import com.noa.packer.repositories.ContainerRepository;
import com.noa.packer.repositories.PackingResultRepository;

@Service
public class PackingService {
    private PackingResultRepository packingResultRepository;  // ← הזרקה
    private ContainerRepository containerRepository;  // ← הזרקה
    private BoxRepository boxRepository;
    private final HybridLoadingSystem hybridLoadingSystem;
    private final Executor userRequestsExecutor;

    @Autowired
    public PackingService(HybridLoadingSystem hybridLoadingSystem,
                          ContainerRepository containerRepository,
                          BoxRepository boxRepository, PackingResultRepository packingResultRepository,
                          @Qualifier("userRequestsExecutor") Executor userRequestsExecutor) {
        this.hybridLoadingSystem = hybridLoadingSystem;
        this.containerRepository = containerRepository;
        this.boxRepository = boxRepository;
        this.packingResultRepository = packingResultRepository;
        this.userRequestsExecutor = userRequestsExecutor;

    }

   // שמירת ה PackinkResult במסד הנתונים
   public ObjectId saveResult(LoadingChain chain) {
    List<PackingResult.PlacedBoxData> placements = new ArrayList<>();

    for (LoadingChain.PlacedBoxInfo step : chain.getSteps()) {
        placements.add(new PackingResult.PlacedBoxData(
            step.cargoType,
            step.direction,
            step.leftBackBottom,
            step.rightFrontTop
        ));
    }
    Container container = containerRepository.getById(chain.getContainerID());
    PackingResult result = new PackingResult(
    chain.getContainerID(),
    container.getVolume(),
    chain.getTotalCargoVolume(),
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

    public CompletableFuture<PackingResultDTO> solve(PackingRequestDTO request) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. חילוץ מידות המכולה
            double L = request.getContainer().get("l");
            double W = request.getContainer().get("w");
            double H = request.getContainer().get("h");

            ObjectId userId = new ObjectId(request.getUserId());
            Container container = new Container(userId, L, W, H);

            // 2 + 3. שמירה למסד נתונים - תבוצע אך ורק אם זה לא בנצ'מארק
            if (!request.getIsBenchmark()) {
                containerRepository.save(container);

                for (Map<String, Object> boxMap : request.getBoxes()) {
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
            List<CargoType> cargoTypes = convertToCargoTypes(request.getBoxes());

            // 5. הפעלת האלגוריתם
            LoadingChain result = this.hybridLoadingSystem.startLoadingProcess(container, cargoTypes);

            // 6. שמירת התוצאה במסד - רק אם זה לא בנצ'מארק
            if (!request.getIsBenchmark()) {
                saveResult(result);
            }
            List<PackingResult.PlacedBoxData> placements = new ArrayList<>();
            for (LoadingChain.PlacedBoxInfo step : result.getSteps()) {
                placements.add(new PackingResult.PlacedBoxData(
                        step.cargoType, step.direction,
                        step.leftBackBottom, step.rightFrontTop
                ));
            }
            PackingResult packingResult = new PackingResult(
                    container.getId(),
                    container.getVolume(),
                    result.getTotalCargoVolume(), placements
            );
            return new PackingResultDTO(packingResult, L,W,H);
        }, userRequestsExecutor);

    }


    //מחזיר את כל הסידורים של משתמש מסוים, מהחדש לישן
     public List<HistoryItemDTO> getHistory(ObjectId userId) {
         List<ObjectId> containerIds = containerRepository.getContainerIdsByUserId(userId);
         List<HistoryItemDTO> response = packingResultRepository.getByContainerIds(containerIds).stream().map(item -> {
             String containerSize = "";
             if (item.getContainerId() != null) {
                 Container c = containerRepository.getById(item.getContainerId());
                 if (c != null) {
                     containerSize = (int)c.getLength() + " × " + (int)c.getWidth() + " × " + (int)c.getHeight();
                 }
             }
             return new HistoryItemDTO(
                     item.getId() != null ? item.getId().toHexString() : null,
                     item.getVur(),
                     item.getCreatedAt(),
                     containerSize
             );
         }).collect(Collectors.toList());
         return response;
     }


    public PackingResultDTO getHistoryItem(String id) {
        // בדיקת תקינות לפני שניגשים למסד הנתונים
        if (id == null || id.length() != 24) {
            throw new IllegalArgumentException("מזהה (ID) חסר או שאינו באורך התקין");
        }

        try {
            PackingResult result = packingResultRepository.getById(new ObjectId(id));
            if (result == null) {
                // זריקת שגיאה במידה והפריט לא קיים במסד
                throw new RuntimeException("לא נמצאה תוצאת אריזה עבור המזהה המבוקש");
            }

            Container container = containerRepository.getById(result.getContainerId());
            return new PackingResultDTO(result, container.getLength(), container.getWidth(), container.getHeight());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("שגיאה בשליפת נתונים: " + e.getMessage(), e);
        }
    }
}
