package server.helper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper to log messages.
 */
public class LogHelper {

    /**
     * Format for date and time used in log method.
     * @see #log(String)
     */
    private final static SimpleDateFormat LogDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    /**
     * Log message formatted with current date and time
     * @param message message to log
     */
    public static void log(String message)
    {
        message = LogDateFormat.format(new Date()) + " " + message;
        if (EnvironmentHelper.isDebug())
        {
            message = "[DEBUG] " + message;
        }
        System.out.println(message);
    }
}
