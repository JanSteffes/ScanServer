package testClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import data.PackageData;

// client tries to connect to server
// server (if no other client connected already) accepts connection
// client receives connection established and points out options:
// low quali (fast, may bad result), medium quali (decent, decent) and good quali (slow, good quali)
// will send chosen option to server (if taking longer than 30 sec, server will close connection)
// server tries to do chosen option
// server sends result



public class ClientTest {

	public static void main(String[] args) {
		Socket socket = new Socket();
		InetSocketAddress endpoint;
		try {
			System.out.println("conencting..");
			endpoint = new InetSocketAddress(InetAddress.getByName("raspberrypi"), 1234);
			socket.connect(endpoint);
			System.out.println("connected!");
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
			//Thread.sleep(5000);
			System.out.println("sending chosen option and fileName...");
			writer.writeObject(new PackageData(0, "hello2"));
			writer.flush();
			System.out.println("wait for result...");
			String read = reader.readLine();
			System.out.println("result: " + read);
			System.out.println("terminating connection...");
			socket.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}