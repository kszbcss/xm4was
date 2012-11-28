package com.ibm.ws.bootstrap;

import java.util.logging.LogManager;

public class WsLogManager extends LogManager {
    public static boolean isConfigureByLoggingProperties() {
        return true;
    }
}
