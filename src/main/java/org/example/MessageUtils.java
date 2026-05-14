package org.example;

public class MessageUtils {
    public static String process(String message) {
        if (message.equalsIgnoreCase("exit")) {
            return "Goodbye!";
        }
        return "SERVER: " + message.toUpperCase() + " [" + System.currentTimeMillis() + "]";
    }
}
