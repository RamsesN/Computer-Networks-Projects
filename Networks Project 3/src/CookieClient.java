import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class CookieClient {
	public static void main(String[] args)throws IOException {
		String cookieClient = args[0];
		int portNum = Integer.parseInt(args[1]);
		System.out.println("Connecting to " + cookieClient + ":" + portNum + "...");
		
		Socket client = new Socket(cookieClient, portNum);
		System.out.println("Connection established");
		
		InputStreamReader input = new InputStreamReader(client.getInputStream());
		BufferedReader read = new BufferedReader(input);
		
		String myFortune = read.readLine();
		System.out.println("Your fortune: "+ myFortune);
		
		client.close();
		System.out.println("Exiting");
	}
}
