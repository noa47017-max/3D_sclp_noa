package com.noa.packer.controllers;

import com.noa.packer.dto.HistoryItemDTO;
import com.noa.packer.dto.PackingResultDTO;
import com.noa.packer.models.Container;
import com.noa.packer.models.PackingResult;
import com.noa.packer.repositories.ContainerRepository;
import com.noa.packer.repositories.PackingResultRepository;
import com.noa.packer.services.PackingService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired
    private PackingService packingService;

    @Autowired
    private PackingResultRepository resultRepository;

    @Autowired
    private ContainerRepository containerRepository;

    // רשימת היסטוריה — ללא placements
    @GetMapping("/history")
    public ResponseEntity<List<HistoryItemDTO>> getHistory(@RequestParam String userId) {
        if (userId == null || userId.length() != 24) {
            return ResponseEntity.badRequest().build();
        }

        List<PackingResult> history = packingService.getHistory(new ObjectId(userId));

        List<HistoryItemDTO> response = history.stream().map(item -> {
            String containerSize = "";
            if (item.getContainerId() != null) {
                Container c = containerRepository.getById(item.getContainerId());
                if (c != null) {
                    containerSize = (int)c.getL() + " × " + (int)c.getW() + " × " + (int)c.getH();
                }
            }
            return new HistoryItemDTO(
                    item.getId() != null ? item.getId().toHexString() : null,
                    item.getVur(),
                    item.getCreatedAt(),
                    containerSize
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // סידור מלא לפי id — כשלוחצים "צפה"
    @GetMapping("/history/{id}")
    public ResponseEntity<PackingResultDTO> getHistoryItem(@PathVariable String id) {
        if (id == null || id.length() != 24) {
            return ResponseEntity.badRequest().build();
        }

        PackingResult result = resultRepository.getById(new ObjectId(id));
        if (result == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(new PackingResultDTO(result));
    }
}