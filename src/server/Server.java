
package server;

import data.Config;
import data.ServerAction;
import data.packages.interfaces.IPackageData;
import server.actions.FileActions;
import server.actions.UpdateActions;
import server.helper.LogHelper;
import server.helper.PathHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;


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
	 * Log messages
	 * @param message message to log
	 */
	private static void log(String message)
	{
		LogHelper.log(message);
	}

	/**
	 * Main execution point.
	 * @param args args to start server with. Only one allowed currently: if server should be in debug or not debug mode. Default is debug.
	 */
	public static void main(String[] args) {
		log("Arguments: " + String.join(", ", args));
		if (args.length > 0)
		{
			if (args[0].toLowerCase(Locale.ROOT).equals("debug"))
			{
				Config.switchDebug();
			}
		}
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(Config.getPort());
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
				log("Client connected: " + clientSocket.getInetAddress().getCanonicalHostName());
				// prepare streams
				//log("prepare streams..");
				ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
				//log("prepare reader...");
				var clientInputStream = clientSocket.getInputStream();
				//log("got stream...");
				ObjectInputStream inReader = new ObjectInputStream(clientInputStream);
				//log("reading data..");
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
				//log("getting action...");
				ServerAction action = data.getAction();
				log("Action: " + action.name());
				switch (action) {
					case ReadFolders:
						result = PathHelper.readFolders();
						break;
					case CheckUpdate:
						result = UpdateActions.updateCheck(data);
						break;
					case GetUpdate:
						result = UpdateActions.getUpdate(data);
						break;
					case ReadFiles:
						result = FileActions.readFiles(data);
						break;
					case MergeFiles:
						result = FileActions.mergeFiles(data);
						break;
					case Scan:
						result = FileActions.scanToFile(data);
						break;
					case DeleteFiles:
						result = FileActions.deleteFiles(data);
						break;
					case StreamFile:
						result = FileActions.getFile(data);
						break;
					default:
						break;
				}
				//log("returning result of action " + action + " ...");
				var size = getSize(result);
				//log("with size " + size);
				writer.writeInt(size);
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

	/**
	 * get size of file in bytes
	 * @param object object to get byte size from
	 * @return byteSize of object
	 * @throws IOException if streams don't work out
	 */
	private static int getSize(Object object) throws IOException {
		if (object instanceof byte[])
		{
			byte[] a = (byte[]) object;
			return a.length;
		}
		var byteArrayStream = new ByteArrayOutputStream();
		var objectStream = new ObjectOutputStream(byteArrayStream);
		objectStream.writeObject(object);
		objectStream.flush();
		return byteArrayStream.size();
	}
}
