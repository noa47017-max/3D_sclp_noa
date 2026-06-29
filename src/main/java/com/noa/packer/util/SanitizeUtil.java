// util/SanitizeUtil.java
package com.noa.packer.util;

public class SanitizeUtil {

    public static String clean(String input) {
        if (input == null) return null;
        return input
                .replaceAll("&", "&amp;")    // ← חייב להיות ראשון!
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .trim();
    }
}