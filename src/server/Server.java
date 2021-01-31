
package server;

import data.Config;
import data.ServerAction;
import data.packages.*;
import data.packages.implementations.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * client tries to connect to server server (if no other client connected
 * already) accepts connection client receives connection established and points
 * out options: low quality (fast, may bad result), medium quality (decent, decent)
 * and good quality (slow, good quality) will send chosen option to server (if
 * taking longer than 30 sec, server will close connection) server tries to do
 * chosen option server sends result
 **/
public class Server {

	/**
	 * Set to true for connection debug purposes (e.g. debugging client to server connection / data handling)
	 */
	private final static boolean notReal = false;
	/**
	 * only used if notReal is set to return a list of fileNames
	 */
	
	private final static SimpleDateFormat LogDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	
	private static void log(String s)
	{
		s = LogDateFormat.format(new Date()) + " " + s;
		System.out.println(s);
	}
	
	public static void main(String[] args) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(Config.SERVER_PORT);
			log("Server running");

		} catch (Exception e) {
			log("Failure!");
			System.exit(1);
		}

		//noinspection InfiniteLoopStatement
		while (true) {
			Socket clientSocket = null;
			try {
				log("waiting for client..");
				clientSocket = ss.accept();
				log("Client connected!");
				// prepare streams
				log("prepare streams..");
				ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
				log("prepare reader...");
				var clientInputStream = clientSocket.getInputStream();
				log("got stream...");
				ObjectInputStream inReader = new ObjectInputStream(clientInputStream);
				log("reading data..");
				IPackageData data;
				try {
					data = (IPackageData) inReader.readObject();
				}
				catch(Exception e)
				{
					log("failed to read data!");
					log(e.toString());
					log("message:");
					log(e.getMessage());
					log("stacktrace:");
					e.printStackTrace();
					clientSocket.close();
					continue;
				}
				Object result = "failed";
				log("getting action...");
				ServerAction action = data.getAction();
				log("Action: " + action.name());
				switch (action) {
				case ReadFiles:
					result = readFiles();
					break;
				case MergeFiles:
					PackageDataMerge mergeFileData = (PackageDataMerge) data;
					result = mergeFiles(mergeFileData);
					break;
				case Scan:
					PackageDataScan scanFileData = (PackageDataScan) data;
					result = scanToFile(scanFileData);
					break;
				case DeleteFiles:
					PackageDataDelete deleteFileData = (PackageDataDelete) data;
					result = deleteFiles(deleteFileData);
					break;
				case CheckUpdate:
					PackageDataUpdate updateData = (PackageDataUpdate) data;
					result = updateCheck(updateData);
					break;
				case StreamFile:
					PackageDataGetFile getFileData = (PackageDataGetFile) data;
					result = getFile(getFileData);
				default:
					break;
				}
				log("returning result of action " + action + " ...");
				writer.writeObject(result);
				writer.flush();
				log("disconnecting");
				clientSocket.close();
			} catch (Exception e) {
				System.err.println("Exception happened: " + e.getMessage());
				try {
					if (clientSocket != null) {
						clientSocket.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private static byte[] getFile(PackageDataGetFile getFileData) throws Exception {
		var dirPath = GetTargetDirPath();
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
	 * @param mergeData contains which files to merge to which file in their current order
	 * @return boolean indicating if everything worked or not
	 */
	private static boolean mergeFiles(PackageDataMerge mergeData) {
		try {
			
			ArrayList<String> fileNames = mergeData.filesToMerge;

			String mergedFileName = mergeData.mergedFileName;
			if (notReal)
			{
				return true;
			}
			Path targetDirPath = GetTargetDirPath();
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

	private static byte[] updateCheck(PackageDataUpdate data){
		log("getting latest version..");
		var latestFileVersion = notReal ? "" : GetLatestAppVersion();
		log("latest version is: " + latestFileVersion);
		if (latestFileVersion == null)
		{
			return null;
		}
		var updateNeeded = notReal || CompareVersions(latestFileVersion, data.version) > 0;
		if (updateNeeded)
		{
			try {
				var fileData = GetLatestAppVersionApkData();
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
	private static int CompareVersions(int[] firstVersionParts, int[] secondVersionParts)
	{
		return CompareVersions(String.join(".", Arrays.stream(firstVersionParts).mapToObj(String::valueOf).toArray(String[]::new)), String.join(".", Arrays.stream(secondVersionParts).mapToObj(String::valueOf).toArray(String[]::new)));
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
	private static int CompareVersions(String first, String second)
	{
		log("Comparing versions "+ first + " and " + second);
		if (first.equals(second))
		{
			log("They are same, returning 0");
			// same
			return 0;
		}
		var firstSplit = GetAppFileNameVersionParts(first);
		var secondSplit = GetAppFileNameVersionParts(second);
		// max index is min of one of them, since we can't compare 1.2.3.4 to 1.2.3, need to handle that after comparing previous parts
		var maxIndex = Math.min(firstSplit.length, secondSplit.length);
		// handle differences in versions like 1.2.8 and 2.0.0
		for (var i = 0; i < maxIndex; i++)
		{
			var firstPart = firstSplit[i];
			var secondPart = secondSplit[i];
			if (firstPart > secondPart)
			{
				log("Part " + i + "(" + firstPart +") of first " + first + " is higher than ("+secondPart+") of second" + second);
				// any version part is greater than current, like 2.0.0 is newer than 1.0.0
				return 1;
			}
			else if (firstPart < secondPart)
			{
				log("Part " + i + "(" + firstPart +") of first " + first + " is lower than ("+secondPart+") of second" + second);
				// case unlike other else, like 1.0.0 is older than 2.0.0
				return -1;
			}
		}
		// till here, versions are the same, now see if there's a subversion in one of them
		if (firstSplit.length > secondSplit.length)
		{
			log("length of first " + first + " is longer than that of seconds "+ second);
			// first is newer, since it has a subversion
			return 1;
		}
		else
		{
			log("length of first " + first + " is lower than that of seconds "+ second);
			// second is newer, since it has subversion
			return -1;
		}
	}

	private static byte[] GetLatestAppVersionApkData() throws IOException {
		var latest = GetLatestAppVersionFile();
		if (notReal || latest == null)
		{
			return null;
		}
		var fileInputStream = new FileInputStream(latest.getAbsolutePath());
		var bytes = fileInputStream.readAllBytes();
		fileInputStream.close();
		return bytes;
	}

	private static File GetLatestAppVersionFile()
	{
		if (notReal)
		{
			return null;
		}
		log("get latest file..");
		var appDir = GetAppDirPath();
		var appDirFile = appDir.toFile();
		if (appDirFile.listFiles() == null)
		{
			return null;
		}
		var filesStream = Arrays.stream(Objects.requireNonNull(appDirFile.listFiles()));
		var orderedFiles = filesStream.sorted((o1, o2) -> CompareVersions(GetAppFileNameVersionParts(GetFileNameWithoutExtension(o2.getName())), GetAppFileNameVersionParts(GetFileNameWithoutExtension(o1.getName())))).collect(Collectors.toList());
		log("files: " + orderedFiles.stream().map(File::getName).collect(Collectors.joining(", ")));
		var latest = orderedFiles.get(0);
		log("latest: " + latest.getName());
		return latest;
	}

	private static String GetFileNameWithoutExtension(String fileName)
	{
		log("GetFileNameWithoutExtension of " + fileName);
		var fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
		log("GetFileNameWithoutExtension: " + fileNameWithoutExtension);
		return fileNameWithoutExtension;
	}

	/**
	 * fileName has to be without extensions
	 * @param fileNameWithoutExtension file name without extension like "ScanApp-1.2.3" instead of "ScanApp-1.2.3.apk"
	 * @return version part (1.2.3) as int array
	 */
	private static int[] GetAppFileNameVersionParts(String fileNameWithoutExtension)
	{
		log("Get version part of " + fileNameWithoutExtension);
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

	private static String GetLatestAppVersion() {
		var latest = GetLatestAppVersionFile();
		if (latest == null)
		{
			return null;
		}
		var fileNameWithoutExtension = GetFileNameWithoutExtension(latest.getName());
		return fileNameWithoutExtension.split("-")[1];
	}

	private static boolean deleteFiles(PackageDataDelete data) {
		if (notReal)
		{
			return true;
		}
		try
		{
			Path targetDirPath = GetTargetDirPath();
			File targetDir = targetDirPath.toFile();
			for(int fileIndex = 0; fileIndex < data.filesToDelete.size(); fileIndex++)
			{			
				String[] deleteCommands = {"rm", data.filesToDelete.get(fileIndex)};
				log("Will execute command: \"" + String.join(" ", deleteCommands) + "\"");
				ProcessBuilder pb = new ProcessBuilder();
				pb.directory(targetDir).command(deleteCommands).start();
			}
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Return list of files in current directory
	 * 
	 * @return String array of fileNames contained in current folder
	 */
	private static String[] readFiles() throws Exception {
		Path targetDirPath = GetTargetDirPath();
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
	 * @param scanData detailed data of how to scan to what file
	 * @return false if exception happened, true if not
	 */
	private static boolean scanToFile(PackageDataScan scanData) {
		try {
			// read options from stream
			int resolution = scanData.chosenOption;// * 150 + 150
			String fileName = scanData.chosenName;
			
			if (notReal)
			{
				return true;
			}
			
			Path targetDirPath = GetTargetDirPath();
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

	/**
	 * Return directory of current date. Creates if not existing.
	 * 
	 * @return path to current files
	 */
	private static Path GetTargetDirPath() throws Exception {
		// create target directory if not existing (folder with current date)
		// preparations
		Path targetDirPath = Paths.get(GetBaseFilesPath().toString(), "Scans",
				Config.dateFormat.format(new Date()));
		File targetDir = targetDirPath.toFile();
		if (!targetDir.exists()) {
			if (!targetDir.mkdir())
			{
				throw new Exception("Failed to create dir " + targetDir);
			}
		}
		return targetDirPath;
	}

	private static Path GetAppDirPath() {
		return Paths.get(GetBaseFilesPath().toString(), "Apps", "ScanApp");
	}

	private static Path GetBaseFilesPath()
	{
		Path path;
		if (System.getProperty("os.name").toLowerCase().contains("windows"))
		{
			path = Paths.get("Z:");
		}
		else {
			path = Paths.get(System.getProperty("user.home"), "pi-share");
		}
		return path;

	}
}
