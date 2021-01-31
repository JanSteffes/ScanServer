
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
import java.util.Base64;
import java.util.Date;


/**
 * client tries to connect to server server (if no other client connected
 * already) accepts connection client receives connection established and points
 * out options: low quali (fast, may bad result), medium quali (decent, decent)
 * and good quali (slow, good quali) will send chosen option to server (if
 * taking longer than 30 sec, server will close connection) server tries to do
 * chosen option server sends result
 **/
public class Server {

	/**
	 * Set to true for connection debug purposes (e.g. debugging client to server connection / data handling)
	 */
	private static boolean notReal = false;
	/**
	 * only used if notReal is set to return a list of fileNames
	 */
	private static int currentCounter = 1;
	
	private static SimpleDateFormat LogDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	
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

		boolean running = true;
		while (running) {
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
					String[] readFilesResult = readFiles();
					result = readFilesResult;
					break;
				case MergeFiles:
					PackageDataMerge mergeFileData = (PackageDataMerge) data;
					boolean mergeResult = mergeFiles(mergeFileData);
					result = mergeResult;
					break;
				case Scan:
					PackageDataScan scanFileData = (PackageDataScan) data;
					boolean scanResult = scanToFile(scanFileData);
					result = scanResult;
					break;
				case DeleteFiles:
					PackageDataDelete deleteFileData = (PackageDataDelete) data;
					var deleteFileResult = deleteFiles(deleteFileData);
					result = deleteFileResult;
					break;
				case CheckUpdate:
					PackageDataUpdate updateData = (PackageDataUpdate) data;
					var updateCheckResult = updateCheck(updateData);
					result = updateCheckResult;
					break;
				case StreamFile:
					PackageDataGetFile getFileData = (PackageDataGetFile) data;
					var getFileResult = getFile(getFileData);
					result = getFileResult;
				default:
					break;
				}
				log("returning result of action " + action + " ...");
				writer.writeObject(result);
				writer.flush();
				log("disconnecting");
				clientSocket.close();
			} catch (Exception e) {
				System.err.println("Exeption happend: " + e.getMessage());
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

	private static byte[] getFile(PackageDataGetFile getFileData) throws IOException {
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
	 * @param mergeData
	 * @return
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
			
			// Mergen:
			// pdftk page1.pdf page2.pdf ... cat output result.pdf
			ArrayList<String> mergeCommandsList = new ArrayList<String>();
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

	private static String updateCheck(PackageDataUpdate data){
		var latestFileVersion = notReal ? "" : GetLatestAppVersion();
		var updateNeeded = notReal || CompareVersions(latestFileVersion, data.version);
		if (updateNeeded)
		{
			try {
				var base64ByteString = GetLatestAppVersionApkData();
				return base64ByteString;
			}
			catch(Exception e)
			{
				log("Error while reading apk data. Returning null.");
				return null;
			}
		}
		else {
			return null;
		}
	}

	private static boolean CompareVersions(String latest, String current)
	{
		var latestSplit = latest.split(".");
		var currentSplit = current.split(".");
		var maxIndex = Math.min(latestSplit.length, currentSplit.length);
		// handle differences in versions like 1.2 and 2.0 or something like that
		for (var i = 0; i < maxIndex; i++)
		{
			if (Integer.parseInt(latestSplit[i]) > Integer.parseInt(currentSplit[i]))
			{
				// any version part is greater than current
				return true;
			}
		}
		// only other case is if there's a new subversion, like 1.0.1 for latest and 1.0 for current
		var newSubVersion = latestSplit.length > currentSplit.length;
		return newSubVersion;
	}

	private static String GetLatestAppVersionApkData() throws IOException {
		var latest = GetLatestAppVersionFile();
		if (notReal)
		{
			return "test";
		}
		var fileInputStream = new FileInputStream(latest.getAbsolutePath());
		var bytes = fileInputStream.readAllBytes();
		fileInputStream.close();
		var base64String = Base64.getEncoder().encodeToString(bytes);
		return base64String;
	}

	private static File GetLatestAppVersionFile()
	{
		if (notReal)
		{
			return null;
		}
		var appDir = GetAppDirPath();
		var appDirFile = appDir.toFile();
		var orderedFiles = Arrays.stream(appDirFile.listFiles()).sorted();
		var latest = orderedFiles.findFirst().get();
		return latest;
	}

	private static String GetLatestAppVersion() {
		var latest = GetLatestAppVersionFile();
		var version = latest.getName().split(".")[0].split("-")[1];
		return version;
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
	 * @return
	 */
	private static String[] readFiles() {
		
		if (notReal)
		{
			String[] resultArray = new String[currentCounter++];
			for (int i = 0; i < resultArray.length; i++)
			{
				resultArray[i] = "Test_" + i;
			}
			return resultArray;
		}
		
		Path targetDirPath = GetTargetDirPath();
		log("trying to read from '" + targetDirPath.toString() + "' ...");
		File targetDir = targetDirPath.toFile();
		File[] files = targetDir.listFiles();
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
	 * @param scanData
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
			int bytesRead = 0;
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
			log("convertig file to pdf...");
			File targetTempPdfFile = new File(targetFilePath + "_temp.pdf");
			String[] pdfConvertCommand = { "tiff2pdf", "-o", targetTempPdfFile.getAbsolutePath(),
					tempTiffFile.getAbsolutePath() };
			log("Will execute command: \"" + String.join(" ", pdfConvertCommand) + "\"");
			Process convertProcess = pb.command(pdfConvertCommand).start();
			convertProcess.waitFor();
			// convert to postscript
			log("convertig to postscript...");
			File targetTempPsFile = Paths.get(targetFilePath + "_temp.ps").toFile();
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
			targetTempPsFile.delete();
			targetTempPdfFile.delete();
			tempTiffFile.delete();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Return directory of current date. Creates if not existing.
	 * 
	 * @return
	 */
	private static Path GetTargetDirPath() {
		// create target directory if not existing (folder with current date)
		// preparations
		Path targetDirPath = Paths.get(System.getProperty("user.home"), "pi-share", "Scans",
				Config.dateFormat.format(new Date()));
		File targetDir = targetDirPath.toFile();
		if (!targetDir.exists()) {
			targetDir.mkdir();
		}
		return targetDirPath;
	}

	private static Path GetAppDirPath() {
		var appDirPaths = Paths.get(System.getProperty("user.home"), "pi-share", "Apps", "ScanClient");
		return appDirPaths;
	}
}
