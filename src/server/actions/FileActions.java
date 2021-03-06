package server.actions;

import data.packages.implementations.PackageData.*;
import data.packages.interfaces.IPackageData;
import server.helper.EnvironmentHelper;
import server.helper.PathHelper;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * All actions regarding files
 */
public class FileActions  extends BaseAction {

    /**
     * If server is real or not
     * @see EnvironmentHelper#isDebug()
     * @return server is real (can perform fileActions) or not
     */
    private static boolean isReal()
    {
        return !EnvironmentHelper.isDebug();
    }

    /**
     * Returns fileData of file
     * @param data file to retrieve
     * @return data of file to retrieve
     * @throws Exception if no latest folder or path given not found
     */
    public static byte[] getFile(IPackageData data) throws Exception {
        var getFileData = (PackageDataGetFile) data;
        if (getFileData.folderName == null || getFileData.folderName.length() < 1)
        {
            getFileData.folderName = PathHelper.getLatestFolder();
        }
        var dirPath = PathHelper.getTargetDirPath(getFileData.folderName);
        var filePath = Paths.get(dirPath.toString(), getFileData.fileName);
        var file = filePath.toFile();
        var inputStream = new FileInputStream(file);
        var fileData = inputStream.readAllBytes();
        inputStream.close();
        return fileData;
    }

    /**
     * Merge multiple pdfs to one file.
     *
     * @param data contains which files to merge to which file in their current order
     * @return boolean indicating if everything worked or not
     */
    public static boolean mergeFiles(IPackageData data) {
        var mergeData = (PackageDataMerge) data;
        try {

            ArrayList<String> fileNames = mergeData.filesToMerge;

            String mergedFileName = mergeData.mergedFileName;
            if (!isReal())
            {
                return true;
            }
            Path targetDirPath = PathHelper.getTargetDirPath(mergeData.folderName);
            File targetDir = targetDirPath.toFile();

            // prepare files
            String targetFilePath = Paths.get(targetDirPath.toString(), mergedFileName).toString();
            File targetFile = new File(targetFilePath + ".pdf");

            // if file exist, rename
            if (targetFile.exists()) {
                int counter = 0;
                while (targetFile.exists()) {
                    targetFile = new File(targetFilePath + "_" + counter + ".pdf");
                    counter++;
                }
            }

            // Merge:
            // pdftk page1.pdf page2.pdf ... cat output result.pdf
            ArrayList<String> mergeCommandsList = new ArrayList<>();
            mergeCommandsList.add("pdftk");
            mergeCommandsList.addAll(fileNames);
            mergeCommandsList.add("cat");
            mergeCommandsList.add("output");
            mergeCommandsList.add(targetFile.getName());
            String[] mergeCommands =mergeCommandsList.toArray(new String[0]);

            log("Will execute command: \"" + String.join(" ", mergeCommands) + "\"");
            ProcessBuilder pb = new ProcessBuilder();
            Process mergeProcess = pb.directory(targetDir).command(mergeCommands).start();
            BufferedReader errorReader = new BufferedReader(new
                    InputStreamReader(mergeProcess.getErrorStream()));
            // Read any errors from the attempted command
            log("Here is the standard error of the command (if any):\n");
            String s;
            while ((s = errorReader.readLine()) != null) {
                log(s);
            }

            mergeProcess.waitFor();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    /**
     * Deletes files.
     * @param data folder (if set) and fileNames
     * @return if delete worked for all files
     */
    public static boolean deleteFiles(IPackageData data) throws Exception {
        var result = true;
        PackageDataDelete deleteFileData = (PackageDataDelete) data;
        if (deleteFileData.folderName == null || deleteFileData.folderName.length() < 1)
        {
            deleteFileData.folderName = PathHelper.getLatestFolder();
        }
        try
        {
            for(int fileIndex = 0; fileIndex < deleteFileData.filesToDelete.size(); fileIndex++)
            {
                var currentFilePath = Paths.get(PathHelper.getTargetDirPath(deleteFileData.folderName).toString(), deleteFileData.filesToDelete.get(fileIndex));
                var currentFile = currentFilePath.toFile();
                result &= currentFile.delete();
                if (!result)
                {
                    log("Failed to delete file " + currentFilePath);
                }
            }
            return result;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Return list of files in current directory
     *@return String array of fileNames contained in current folder
     */
    public static String[] readFiles(IPackageData data) throws Exception {
        PackageDataListFiles listFileData = (PackageDataListFiles) data;
        if (listFileData.folderName == null || listFileData.folderName.length() < 1)
        {
            listFileData.folderName = PathHelper.getLatestFolder();
        }
        log("folderName: " + listFileData.folderName);
        Path targetDirPath = PathHelper.getTargetDirPath(listFileData.folderName);
        log("trying to read from '" + targetDirPath.toString() + "' ...");
        File targetDir = targetDirPath.toFile();
        File[] files = targetDir.listFiles();
        if (files == null)
        {
            log("no files found!");
            return new String[0];
        }
        String[] fileNames = new String[files.length];
        for (int fileIndex = 0; fileIndex < fileNames.length; fileIndex++) {
            fileNames[fileIndex] = files[fileIndex].getName();
        }
        log("Found " + fileNames.length + " files: " + String.join(", ", fileNames));
        return fileNames;
    }

    /**
     * Scans and saves result to file. Will use some logic to minimize fileSize.
     *
     * @param data detailed data of how to scan to what file
     * @return false if exception happened, true if not
     */
    public static boolean scanToFile(IPackageData data) throws Exception {
        PackageDataScan scanData = (PackageDataScan) data;
        if (scanData.folderName == null || scanData.folderName.length() < 1)
        {
            scanData.folderName = PathHelper.getLatestFolder();
        }
        try {
            // read options from stream
            int resolution = scanData.chosenOption;// * 150 + 150
            String fileName = scanData.chosenName;

            if (EnvironmentHelper.isDebug())
            {
                return true;
            }

            Path targetDirPath = PathHelper.getTargetDirPath(scanData.folderName);
            File targetDir = targetDirPath.toFile();

            // prepare files
            String targetFilePath = Paths.get(targetDirPath.toString(), fileName).toString();
            File targetFile = new File(targetFilePath + ".pdf");

            // if file exist, rename
            if (targetFile.exists()) {
                int counter = 0;
                while (targetFile.exists()) {
                    targetFile = new File(targetFilePath + "_" + counter + ".pdf");
                    counter++;
                }
            }

            // scan
            @SuppressWarnings("SpellCheckingInspection")
            String[] scanCommand = { "scanimage", "--format=tiff", "--resolution", "" + resolution };
            log("Will execute command: \"" + String.join(" ", scanCommand) + "\"");
            ProcessBuilder pb = new ProcessBuilder();
            Process scanProcess;
            scanProcess = pb.directory(targetDir).command(scanCommand).start();
            // write result to file
            InputStream in = scanProcess.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] scanResultBytes;
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            scanResultBytes = out.toByteArray();
            out.close();
            scanProcess.waitFor();
            scanProcess.destroy();
            in.close();

            // write to tiff
            log("writing result to file...");
            File tempTiffFile = Paths.get(targetFilePath + "_temp.tiff").toFile();
            FileOutputStream fos = new FileOutputStream(tempTiffFile);
            fos.write(scanResultBytes);
            fos.flush();
            fos.close();

            // convert to pdf
            log("converting file to pdf...");
            File targetTempPdfFile = new File(targetFilePath + "_temp.pdf");
            String[] pdfConvertCommand = { "tiff2pdf", "-o", targetTempPdfFile.getAbsolutePath(),
                    tempTiffFile.getAbsolutePath() };
            log("Will execute command: \"" + String.join(" ", pdfConvertCommand) + "\"");
            Process convertProcess = pb.command(pdfConvertCommand).start();
            convertProcess.waitFor();
            // convert to postscript
            log("converting to postscript...");
            File targetTempPsFile = Paths.get(targetFilePath + "_temp.ps").toFile();
            @SuppressWarnings("SpellCheckingInspection")
            String[] pdfConvertToPsCommand = { "pdftops", targetTempPdfFile.getAbsolutePath(),
                    targetTempPsFile.getAbsolutePath() };
            log("Will execute command: \"" + String.join(" ", pdfConvertToPsCommand) + "\"");
            Process convertToPsProcess = pb.command(pdfConvertToPsCommand).start();
            convertToPsProcess.waitFor();

            // convert to final file
            String[] psConvertToPdfCommand = { "ps2pdf", targetTempPsFile.getAbsolutePath(),
                    targetFile.getAbsolutePath() };
            log("Will execute command: \"" + String.join(" ", psConvertToPdfCommand) + "\"");
            Process convertToPdfProcess = pb.command(psConvertToPdfCommand).start();
            convertToPdfProcess.waitFor();

            log("Deleting temp files...");
            boolean deletion = targetTempPsFile.delete();
            deletion = deletion & targetTempPdfFile.delete();
            deletion = deletion & tempTiffFile.delete();
            if (!deletion)
            {
                log("At least one file failed to delete!");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
