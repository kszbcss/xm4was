package com.googlecode.xm4was.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;

public class LoggingServiceHandlerTest {
    @Test
    public void testNullMessage() {
        Logger logger = Logger.getLogger("test");
        LoggingServiceHandler handler = new LoggingServiceHandler();
        logger.addHandler(handler);
        try {
            logger.warning(null);
            // In 0.3.1 this would trigger a NullPointerException
            String[] messages = handler.getMessages(0);
            assertEquals(1, messages.length);
            assertTrue(messages[0].endsWith(":test:::]<null>"));
        } finally {
            logger.removeHandler(handler);
        }
    }
}
