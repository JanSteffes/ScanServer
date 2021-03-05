package server.helper;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

/**
 * Helper to retrieve paths
 */
public class PathHelper {


    /**
     * Return main working directory
     * @return return basePath based on current environment.
     */
    public static Path getBaseFilesPath()
    {
        Path path;
        if (EnvironmentHelper.isDebug())
        {
            path = Paths.get("Z:");
        }
        else {
            path = Paths.get(System.getProperty("user.home"), "pi-share");
        }
        return path;

    }

    /**
     * Overload method.
     * @see #getTargetDirPath(String)
     * @return Path to directory of current data.
     * @throws Exception if directory didn't exist and creation failed
     */
    public static Path getTargetDirPath() throws Exception {
        return getTargetDirPath(null);
    }

    /**
     * Return directory of current date. Create target directory if not existing (folder with current date)
     * @param folderName folder to get, can be null or empty
     * @return path to
     * @throws Exception if directory didn't exist and creation failed
     */
    public static Path getTargetDirPath(String folderName) throws Exception {
        Path targetDirPath = Paths.get(getBaseFilesPath().toString(), "Scans");
        if (folderName != null)
        {
            targetDirPath = Paths.get(targetDirPath.toString(), folderName);
        }
        File targetDir = targetDirPath.toFile();
        if (!targetDir.exists()) {
            if (!targetDir.mkdir())
            {
                throw new Exception("Failed to create dir " + targetDir);
            }
        }
        return targetDirPath;
    }


    /**
     * Return directory containing app files
     * @return Path to app apk folder
     */
    public static Path getAppDirPath() {
        return Paths.get(getBaseFilesPath().toString(), "Apps", "ScanApp");
    }

    /**
     * Returns latest folder in scan data
     * @return latest folder in scan data folder
     * @throws Exception @see {@link #getTargetDirPath()}
     */
    public static String getLatestFolder() throws Exception {
        var folders = readFolders();
        if (folders == null || folders.length == 0)
        {
            throw new Exception("could not retrieve any folders!");
        }
        return folders[0];
    }

    /**
     * Read folders of scan directory.
     * @return folders of dates when scans were done
     * @throws Exception @see {@link #getTargetDirPath()}
     */
    public static String[] readFolders() throws Exception {
        var foldersPath = getTargetDirPath();
        var folderEntries = foldersPath.toFile().listFiles();
        if (folderEntries == null)
        {
            return null;
        }
        var array = Arrays.stream(folderEntries).filter(File::isDirectory).map(File::getName).toArray(String[]::new);
        Arrays.sort(array, Collections.reverseOrder());
        return array;
    }

}
