package com.noa.packer.controllers;

import com.noa.packer.dto.PackingRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.noa.packer.algorithms.CargoType;
import com.noa.packer.algorithms.HybridLoadingSystem;
import com.noa.packer.algorithms.LoadingChain;
import com.noa.packer.models.Box;
import com.noa.packer.models.Container;
import com.noa.packer.repositories.BoxRepository;
import com.noa.packer.repositories.ContainerRepository;
import com.noa.packer.services.PackingService;
import org.bson.types.ObjectId;
import java.util.*;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "*")
public class PackingController {

   @Autowired
   private PackingService packingService;        // ← חדש: שמירה למסד

   @Autowired
   private ContainerRepository containerRepository; // ← חדש: שמירת מכולה

   @PostMapping("/solve")
   public ResponseEntity<?> solve(@RequestBody PackingRequestDTO request) {
       try {
           if (request.userId == null || request.userId.isEmpty()) {
               return ResponseEntity.status(400)
                   .body("שגיאה: userId חסר בבקשה");
           }
           return packingService.solve(request);
       } catch (Exception e) {
           e.printStackTrace();
           System.err.println("Error in /api/solve: " + e.getMessage());
           return ResponseEntity.status(500)
               .body("שגיאה בחישוב: " + e.getMessage());
       }
   }

}
