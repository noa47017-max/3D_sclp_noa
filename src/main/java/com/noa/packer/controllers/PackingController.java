package com.noa.packer.controllers;

import com.noa.packer.dto.PackingRequestDTO;
import com.noa.packer.dto.PackingResultDTO;
import com.noa.packer.services.PackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api")
public class PackingController {

   @Autowired
   private PackingService packingService;


//   @PostMapping("/solve")
//   public ResponseEntity<PackingResultDTO> solve(@RequestBody PackingRequestDTO request) {
//       try {
//           PackingResultDTO result = packingService.solve(request);
//           return ResponseEntity.ok(result);
//       } catch (Exception e) {
//           e.printStackTrace();
//           System.err.println("Error in /api/solve: " + e.getMessage());
//           return ResponseEntity.status(500).build();
//       }
//   }

   // קבלת מכולה וארגזים שמירה למסד והפעלת האלגוריתם
    @PostMapping("/solve")
    public CompletableFuture<ResponseEntity<PackingResultDTO>> solve(@RequestBody PackingRequestDTO request) {
        try {
            return packingService.solve(request)
                    .orTimeout(20, TimeUnit.MINUTES)   // גבול עליון, כולל המתנה בתור
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(ex -> {
                        Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                                ? ex.getCause() : ex;
                        if (cause instanceof TimeoutException) {
                            return ResponseEntity.status(504).build();
                        }
                        cause.printStackTrace();
                        System.err.println("Error in /api/solve: " + cause.getMessage());
                        return ResponseEntity.status(500).build();
                    });
        } catch (RejectedExecutionException busy) {
            // התור של solve מלא — "השרת עמוס" במקום קריסה
            return CompletableFuture.completedFuture(ResponseEntity.status(503).build());
        }
    }
}
