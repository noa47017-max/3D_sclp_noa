package com.noa.packer.dto;

import java.util.List;
import java.util.Map;

public class PackingRequestDTO {
        public String                    userId;    // ← חדש: מי שולח את הבקשה
        public Map<String, Double> container;
        public List<Map<String, Object>> boxes;
        public boolean                   isBenchmark;
}
