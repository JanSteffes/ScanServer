package server.helper;

/**
 * Helper for handling files
 */
public class FileHelper {

    /**
     * Return fileName without extension (very simple logic, might fail for some files)
     * @param fileName fileName/path to remove extension from
     * @return fileName/path without extension
     */
    public static String getFileNameWithoutExtension(String fileName)
    {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }
}
