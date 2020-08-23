
package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import data.Config;
import data.packages.ServerAction;

/**client tries to connect to server
 server (if no other client connected already) accepts connection
 client receives connection established and points out options:
 low quali (fast, may bad result), medium quali (decent, decent) and good quali (slow, good quali)
 will send chosen option to server (if taking longer than 30 sec, server will close connection)
 server tries to do chosen option
 server sends result
**/
public class Server {
	
	public static void main(String[] args) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(Config.SERVER_PORT);
			System.out.println("Server running");

		} catch (Exception e) {
			System.out.println("Failure!");
			System.exit(1);
		}

		boolean running = true;
		while (running) {
			Socket clientSocket = null;
			try {
				System.out.println("waiting for client..");
				clientSocket = ss.accept();
				System.out.println("Client connected!");
				// prepare streams
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				//	ObjectInputStream inReader = new ObjectInputStream(clientSocket.getInputStream());
				// APackageData packageData = (APackageData) inReader.readObject();
				var inputStream = clientSocket.getInputStream();
				var dataInputStream = new DataInputStream(inputStream);
				var actionInt = dataInputStream.readInt();
				var action = ServerAction.values()[actionInt];
				var result = "failed";
				switch(action)
				{
					case ReadFiles:
						var fileNames = readFiles();
						result = String.join(";", fileNames);
						break;
					case MergeFiles:
						result = "" + mergeFiles(inputStream);
						break;
					case Scan:
						result = "" + scanToFile(inputStream);
						break;
				}		
				System.out.println("returning result...");
				writer.write("" + result);
				writer.newLine();
				writer.flush();
				System.out.println("disconnecting");
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
	
	private static boolean mergeFiles(InputStream inputStream)
	{
		try
		{
			var reader = new BufferedReader(new InputStreamReader(inputStream));
			var resultFileName = reader.readLine();
			var fileNames = reader.readLine().split(";");
			var targetDirPath = GetTargetDirPath();
			File targetDir = targetDirPath.toFile();
			var files = targetDir.listFiles();
			var filesToMerge = new String[fileNames.length];			
			boolean contains = Arrays.stream(files).filter(p => p.)("s"::equals);
			return fileNames;		
			
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	
	private static String[] readFiles()
	{
		var targetDirPath = GetTargetDirPath();
		File targetDir = targetDirPath.toFile();
		var files = targetDir.listFiles();
		var fileNames = new String[files.length];
		for(var fileIndex = 0; fileIndex < fileNames.length; fileIndex++)
		{
			fileNames[fileIndex] = files[fileIndex].getName();
		}			
		return fileNames;		
	}
	
	/**
	 * Scans and saves result to file. Will use some logic to minimize fileSize.
	 * @param inputStream
	 * @return false if exception happened, true if not
	 */
	private static boolean scanToFile(InputStream inputStream)
	{
		try
		{
			// read options from stream
		
		var resolution = -1;
		var fileName = "";
		resolution = new DataInputStream(inputStream).readInt();// * 150 + 150
		fileName = new BufferedReader(new InputStreamReader(inputStream)).readLine();
		
		var targetDirPath = GetTargetDirPath();
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
		System.out.println("Will execute command: \"" + String.join(" ", scanCommand) + "\"");
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
			var exitVal = scanProcess.waitFor();
			scanProcess.destroy();
			in.close();
		
		// write to tiff
		System.out.println("writing result to file...");
		File tempTiffFile = Paths.get(targetFilePath + "_temp.tiff").toFile();
		FileOutputStream fos = new FileOutputStream(tempTiffFile);
		fos.write(scanResultBytes);
		fos.flush();
		fos.close();
		// convert to pdf
		System.out.println("convertig file to pdf...");
		File targetTempPdfFile = new File(targetFilePath + "_temp.pdf");
		String[] pdfConvertCommand = { "tiff2pdf", "-o", targetTempPdfFile.getAbsolutePath(),
				tempTiffFile.getAbsolutePath() };
		System.out.println("Will execute command: \"" + String.join(" ", pdfConvertCommand) + "\"");
		Process convertProcess = pb.command(pdfConvertCommand).start();
		convertProcess.waitFor();
		// convert to postscript
		System.out.println("convertig to postscript...");
		File targetTempPsFile = Paths.get(targetFilePath + "_temp.ps").toFile();
		String[] pdfConvertToPsCommand = { "pdftops", targetTempPdfFile.getAbsolutePath(),
				targetTempPsFile.getAbsolutePath() };
		System.out.println("Will execute command: \"" + String.join(" ", pdfConvertToPsCommand) + "\"");
		Process convertToPsProcess = pb.command(pdfConvertToPsCommand).start();
		convertToPsProcess.waitFor();

		// convert to final file
		String[] psConvertToPdfCommand = { "ps2pdf", targetTempPsFile.getAbsolutePath(),
				targetFile.getAbsolutePath() };
		System.out.println("Will execute command: \"" + String.join(" ", psConvertToPdfCommand) + "\"");
		Process convertToPdfProcess = pb.command(psConvertToPdfCommand).start();
		convertToPdfProcess.waitFor();

		System.out.println("Deleting temp files...");
		targetTempPsFile.delete();
		targetTempPdfFile.delete();
		tempTiffFile.delete();
		return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Return directory of current date. Creates if not existing.
	 * @return
	 */
	private static Path GetTargetDirPath() {
		// create target directory if not existing (folder with current date)
		// preparations
		var targetDirPath = Paths.get(System.getProperty("user.home"), "pi-share", "Scans",
				Config.dateFormat.format(new Date()));
		File targetDir = targetDirPath.toFile();
		if (!targetDir.exists()) {
			targetDir.mkdir();
		}
		return targetDirPath;
	}
}
