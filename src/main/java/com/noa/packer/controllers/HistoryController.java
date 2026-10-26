package com.noa.packer.controllers;

import com.noa.packer.dto.HistoryItemDTO;
import com.noa.packer.dto.PackingResultDTO;
import com.noa.packer.services.PackingService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HistoryController {

    @Autowired
    private PackingService packingService;


    // רשימת היסטוריה — ללא placements
    @GetMapping("/history")
    public ResponseEntity<List<HistoryItemDTO>> getHistory(@RequestParam String userId) {
        if (userId == null || userId.length() != 24) {
            return ResponseEntity.badRequest().build();
        }

        List<HistoryItemDTO> response = packingService.getHistory(new ObjectId(userId));
        return ResponseEntity.ok(response);
    }

    // סידור מלא לפי id — כשלוחצים "צפה"
    @GetMapping("/history/{id}")
    public ResponseEntity<PackingResultDTO> getHistoryItem(@PathVariable String id) {
        try {
            PackingResultDTO result = packingService.getHistoryItem(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in /api/history/{id}: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}