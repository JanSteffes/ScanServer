
package server;

import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import data.Config;
import data.packages.IPackageData;
import data.packages.PackageDataScan;

/**client tries to connect to server
 server (if no other client connected already) accepts connection
 client receives connection established and points out options:
 low quali (fast, may bad result), medium quali (decent, decent) and good quali (slow, good quali)
 will send chosen option to server (if taking longer than 30 sec, server will close connection)
 server tries to do chosen option
 server sends result
**/
public class Server_TEST {
	
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
				// APackageData packageData = (APackageData) inReader.readObject();
				var inputStream = clientSocket.getInputStream();
				var inReader = new ObjectInputStream(inputStream);
				var data = (IPackageData) inReader.readObject();
				var dataClass = data.getAction();
				switch(dataClass)
				{
				case MergeFiles:
					break;
				case ReadFiles:
					break;
				case Scan:
					var scanData = (PackageDataScan) data;
					System.out.println("Option: " + scanData.chosenOption);
					System.out.println("FileName: " + scanData.chosenName);
					break;
				default:
					break;					
				}
				System.out.println("Sending answer...");
				writer.write("" + 0);
				writer.newLine();
				writer.flush();
				System.out.println("disconnecting");
				clientSocket.close();
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
