package com.noa.packer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class PackingRequestDTO {

        @NotBlank(message = "userId חסר בבקשה")
        private String userId;

        @NotNull(message = "מידות מכולה חסרות")
        private Map<String, Double> container;

        @NotNull(message = "רשימת ארגזים חסרה")
        private List<Map<String, Object>> boxes;

        private boolean isBenchmark;

        public String getUserId()                         { return userId; }
        public void setUserId(String v)                   { this.userId = v; }
        public Map<String, Double> getContainer()         { return container; }
        public void setContainer(Map<String, Double> v)   { this.container = v; }
        public List<Map<String, Object>> getBoxes()       { return boxes; }
        public void setBoxes(List<Map<String, Object>> v) { this.boxes = v; }
        public boolean getIsBenchmark()                   { return isBenchmark; }
        public void setIsBenchmark(boolean v)             { this.isBenchmark = v; }
}