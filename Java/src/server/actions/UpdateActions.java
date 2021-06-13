package server.actions;

import data.packages.implementations.PackageData.PackageDataUpdateCheck;
import data.packages.implementations.PackageData.PackageDataUpdate;
import data.packages.interfaces.IPackageData;
import server.helper.FileHelper;
import server.helper.PathHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * All actions/logic regarding updates
 */
public class UpdateActions extends BaseAction {

    public static boolean updateCheck(IPackageData data) {
        PackageDataUpdateCheck updateCheckData = (PackageDataUpdateCheck) data;
        var latestFileVersion = getLatestAppVersion();
        log("updateCheck for version "+ updateCheckData.version + ", latest version is: " + latestFileVersion);
        if (latestFileVersion == null)
        {
            return false;
        }
        return compareVersions(latestFileVersion, updateCheckData.version) > 0;
    }

    /**
     * retrieve fileData for update
     * @param data information about current app version
     * @return file data of apk file
     */
    public static byte[] getUpdate(IPackageData data) {
        var updateData = (PackageDataUpdate) data;
        if (updateCheck(new PackageDataUpdateCheck(updateData.version)))
        {
            try {
                var fileData = getLatestAppVersionApkData();
                if (fileData == null)
                {
                    log("fileData is null, returning null..");
                    return null;
                }
                log("returning fileData: " + fileData.length + " bytes");
                return fileData;
            }
            catch(Exception e)
            {
                log("Error while reading apk data. Returning null.");
                return null;
            }
        }
        else {
            log("no update needed!");
            return null;
        }
    }

    /**
     * Overload for CompareVersions.
     * Has overhead, since it converts the int arrays to strings first ([1,2,3] to "1.2.3")
     * @param firstVersionParts first version(parts) to compare
     * @param secondVersionParts second version(parts) to compare
     * @return int value indicating if first or second is newer or both the same
     */
    private static int compareVersions(int[] firstVersionParts, int[] secondVersionParts)
    {
        return compareVersions(String.join(".", Arrays.stream(firstVersionParts).mapToObj(String::valueOf).toArray(String[]::new)), String.join(".", Arrays.stream(secondVersionParts).mapToObj(String::valueOf).toArray(String[]::new)));
    }

    /**
     * Compare version strings.
     * Return 0 if same.
     * Return 1 if first is newer than second
     * Return -1 if first is older than second
     * @param first first to compare
     * @param second second to compare
     * @return int-value indicating result
     */
    private static int compareVersions(String first, String second)
    {
        //log("Comparing versions "+ first + " and " + second);
        if (first.equals(second))
        {
            //log("They are same, returning 0");
            // same
            return 0;
        }
        var firstSplit = getAppFileNameVersionParts(first);
        var secondSplit = getAppFileNameVersionParts(second);
        // max index is min of one of them, since we can't compare 1.2.3.4 to 1.2.3, need to handle that after comparing previous parts
        var maxIndex = Math.min(firstSplit.length, secondSplit.length);
        // handle differences in versions like 1.2.8 and 2.0.0
        for (var i = 0; i < maxIndex; i++)
        {
            var firstPart = firstSplit[i];
            var secondPart = secondSplit[i];
            if (firstPart > secondPart)
            {
                //log("Part " + i + "(" + firstPart +") of first " + first + " is higher than ("+secondPart+") of second" + second);
                // any version part is greater than current, like 2.0.0 is newer than 1.0.0
                return 1;
            }
            else if (firstPart < secondPart)
            {
                //log("Part " + i + "(" + firstPart +") of first " + first + " is lower than ("+secondPart+") of second" + second);
                // case unlike other else, like 1.0.0 is older than 2.0.0
                return -1;
            }
        }
        // till here, versions are the same, now see if there's a subversion in one of them
        if (firstSplit.length > secondSplit.length)
        {
            //log("length of first " + first + " is longer than that of seconds "+ second);
            // first is newer, since it has a subversion
            return 1;
        }
        else
        {
            //log("length of first " + first + " is lower than that of seconds "+ second);
            // second is newer, since it has subversion
            return -1;
        }
    }

    private static byte[] getLatestAppVersionApkData() throws IOException {
        var latest = getLatestAppVersionFile();
        if (latest == null)
        {
            return null;
        }
        var fileInputStream = new FileInputStream(latest.getAbsolutePath());
        log("loading data..");
        var bytes = fileInputStream.readAllBytes();
        fileInputStream.close();
        log("finished loading data!");
        return bytes;
    }

    /**
     * Return newest apk
     * @return file information about newest apk file
     */
    private static File getLatestAppVersionFile()
    {
        log("get latest file..");
        var appDir = PathHelper.getAppDirPath();
        var appDirFile = appDir.toFile();
        if (appDirFile.listFiles() == null)
        {
            return null;
        }
        var filesStream = Arrays.stream(Objects.requireNonNull(appDirFile.listFiles()));
        var orderedFiles = filesStream.sorted((o1, o2) -> compareVersions(getAppFileNameVersionParts(FileHelper.getFileNameWithoutExtension(o2.getName())), getAppFileNameVersionParts(FileHelper.getFileNameWithoutExtension(o1.getName())))).collect(Collectors.toList());
        var latest = orderedFiles.get(0);
        log("latest: " + latest.getName());
        return latest;
    }



    /**
     * fileName has to be without extensions
     * @param fileNameWithoutExtension file name without extension like "ScanApp-1.2.3" instead of "ScanApp-1.2.3.apk"
     * @return version part (1.2.3) as int array
     */
    private static int[] getAppFileNameVersionParts(String fileNameWithoutExtension)
    {
        String[] splitBySeparator = fileNameWithoutExtension.split("-");
        String fileNameVersionParts;
        if (splitBySeparator.length > 1)
        {
            fileNameVersionParts = splitBySeparator[1];
        }
        else
        {
            fileNameVersionParts = splitBySeparator[0];
        }
        return Arrays.stream(fileNameVersionParts.split("\\.")).mapToInt(Integer::parseInt).toArray();

    }

    /**
     * Retrieve latest version as string
     * @return null if none found (see {@link #getLatestAppVersionFile()} or latest version as string
     */
    private static String getLatestAppVersion() {
        var latest = getLatestAppVersionFile();
        if (latest == null)
        {
            return null;
        }
        var fileNameWithoutExtension = FileHelper.getFileNameWithoutExtension(latest.getName());
        return fileNameWithoutExtension.split("-")[1];
    }
}
