package com.notification.validation;

public class ValidationUtil {

    public static boolean hasConsecutiveRepeatedWords(String text, int maxConsecutive) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Split by whitespace
        String[] tokens = text.split("\\s+");
        int consecutiveCount = 1;
        String lastWord = "";

        for (String token : tokens) {
            // Extract the core word by stripping any leading or trailing non-alphanumeric characters
            String word = token.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "").toLowerCase();
            if (word.isEmpty()) {
                continue;
            }

            if (word.equals(lastWord)) {
                consecutiveCount++;
                if (consecutiveCount > maxConsecutive) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
                lastWord = word;
            }
        }

        return false;
    }
}
