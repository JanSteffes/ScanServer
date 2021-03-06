package server.helper;

import data.Config;

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
        if (Config.getDebug())
        {
            message = "[DEBUG] " + message;
        }
        System.out.println(message);
    }
}
