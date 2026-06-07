package com.noa.packer.controllers;

import com.noa.packer.models.PackingResult;
import com.noa.packer.repositories.PackingResultRepository;
import com.noa.packer.services.PackingService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired
    private PackingService packingService;

    @Autowired
    private PackingResultRepository resultRepository;

    // 📋 רשימת היסטוריה — ללא steps, עם id לכל פריט
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam String userId) {
        try {
            List<PackingResult> history = packingService.getHistory(new ObjectId(userId));

            List<Map<String, Object>> response = history.stream().map(item -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id",           item.getId() != null ? item.getId().toHexString() : null);
                dto.put("vur",          item.getVur());
                dto.put("ContainerId", item.getContainerId());
                dto.put("createdAt",    item.getCreatedAt());
                // ← אין steps כאן — מסמכים קטנים, ללא שגיאת זיכרון
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("HistoryController.getHistory error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // 🔍 סידור מלא לפי id — נקרא כשלוחצים "צפה"
    @GetMapping("/history/{id}")
    public ResponseEntity<?> getHistoryItem(@PathVariable String id) {
        try {
            PackingResult result = resultRepository.getById(new ObjectId(id));
            if (result == null) return ResponseEntity.notFound().build();

            Map<String, Object> dto = new HashMap<>();
            dto.put("placements",        result.getPlacements());
            dto.put("containerId", result.getContainerId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("HistoryController.getHistoryItem error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}