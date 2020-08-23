/**
 * 
 */
package data.packages;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import data.Config;

/**
 * @author JanSt
 *
 */
public abstract class APackageData implements IPackageData 
{

	public String Execute()
	{
		try
		{
			Socket socket = new Socket();
			InetSocketAddress endpoint;
			System.out.println("conencting..");
			endpoint = new InetSocketAddress(InetAddress.getByName("localhost"), Config.SERVER_PORT);
			socket.connect(endpoint);
			System.out.println("connected!");
			var inputStream = socket.getInputStream();
			var outputStream = socket.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			ObjectOutputStream writer = new ObjectOutputStream(outputStream);
			System.out.println("sending list files request...");
			writer.writeObject(this);
			writer.flush();
			System.out.println("wait for result...");
			String read = reader.readLine();
			System.out.println("result: " + read);
			reader.close();
			writer.close();
			socket.close();
			System.out.println("terminating connection..");
			return read;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

}