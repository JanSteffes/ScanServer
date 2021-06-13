package server.helper;

/**
 * Helper determinate current environment.
 */
public class EnvironmentHelper {

    /**
     * Backing field.
     */
    private static Boolean _isDebug;

    /**
     * Debug = windows, live = not windows
     * @return can perform fileActions or not
     */
    public static boolean isDebug() {
        if (_isDebug == null)
        {
            _isDebug = System.getProperty("os.name").toLowerCase().contains("windows");
        }
        return _isDebug;
    }
}
