package com.shiv.shop;

public class SessionManager {
    private static String currentSessionId = "BILL1";

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static void setCurrentSessionId(String sessionId) {
        currentSessionId = sessionId;
    }
}