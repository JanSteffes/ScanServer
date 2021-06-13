package server.actions;

import server.helper.LogHelper;

/**
 * Base of action classes. Contains helping methods, so action classes do not have to implement them again.
 */
public class BaseAction {

    /**
     * Log message
     * @param message message to log
     */
    protected static void log(String message)
    {
        LogHelper.log(message);
    }
}
