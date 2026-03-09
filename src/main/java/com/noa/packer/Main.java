package com.noa.packer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        // הפעלת השרת על פורט 8443
      SpringApplication.run(Main.class, args);
    }
}
