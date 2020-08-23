
package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import data.Config;
import data.ServerAction;
import data.packages.IPackageData;
import data.packages.implementations.PackageDataDelete;
import data.packages.implementations.PackageDataMerge;
import data.packages.implementations.PackageDataScan;

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
	private static int currentCounter = 0;
	
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
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				ObjectInputStream inReader = new ObjectInputStream(clientSocket.getInputStream());
				IPackageData data = (IPackageData) inReader.readObject();
				String result = "failed";
				ServerAction action = data.getAction();
				switch (action) {
				case ReadFiles:
					String[] readFilesResult = readFiles();
					result = String.join(";", readFilesResult);
					break;
				case MergeFiles:
					PackageDataMerge mergeFileData = (PackageDataMerge) data;
					boolean mergeResult = mergeFiles(mergeFileData);
					result = "" + mergeResult;
					break;
				case Scan:
					PackageDataScan scanFileData = (PackageDataScan) data;
					result = "" + scanToFile(scanFileData);
					break;
				case DeleteFiles:
					PackageDataDelete deleteFileData = (PackageDataDelete) data;
					result = "" + deleteFiles(deleteFileData);
					break;
				default:
					break;
				}
				log("returning result of action " + action + " ...");
				writer.write("" + result);
				writer.newLine();
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
			
//			String[] mergeCommand = { "pdftk", String.join(" ", fileNames), "cat output", targetFile.getName()};
			log("Will execute command: \"" + String.join(" ", mergeCommands) + "\"");
			ProcessBuilder pb = new ProcessBuilder();
			Process mergeProcess = pb.directory(targetDir).command(mergeCommands).start();
			BufferedReader errorReader = new BufferedReader(new 
				     InputStreamReader(mergeProcess.getErrorStream()));
			// Read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):\n");
			String s;
			while ((s = errorReader.readLine()) != null) {
			    System.out.println(s);
			}
			// write result to file
//			InputStream in = mergeProcess.getInputStream();
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//			byte[] mergeResultBytes;
//			byte[] buffer = new byte[8 * 1024];
//			int bytesRead = 0;
//			while ((bytesRead = in.read(buffer)) != -1) {
//				out.write(buffer, 0, bytesRead);
//			}
//			mergeResultBytes = out.toByteArray();
//			out.close();
			mergeProcess.waitFor();
//			mergeProcess.destroy();
//			in.close();
//			log("writing result to file...");
//			File mergeResultFile = Paths.get(targetFile.getAbsolutePath()).toFile();
//			FileOutputStream fos = new FileOutputStream(mergeResultFile);
//			fos.write(mergeResultBytes);
//			fos.flush();
//			fos.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

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
		File targetDir = targetDirPath.toFile();
		File[] files = targetDir.listFiles();
		String[] fileNames = new String[files.length];
		for (int fileIndex = 0; fileIndex < fileNames.length; fileIndex++) {
			fileNames[fileIndex] = files[fileIndex].getName();
		}
		
		return fileNames;
	}

	/**
	 * Scans and saves result to file. Will use some logic to minimize fileSize.
	 * 
	 * @param inputStream
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
}
