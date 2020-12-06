import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class CookieServer {

	public static void main(String[] args)throws IOException {
		int portNum = Integer.parseInt(args[0]);
		
		ServerSocket server = new ServerSocket(portNum);
		System.out.println("Listening on port " + portNum + "...");
		Socket client = server.accept();
		System.out.println("Connection established");
		
		
		PrintWriter pw = new PrintWriter(client.getOutputStream());
		pw.print(randFor());
		System.out.println("Fortune sent");
		pw.flush();
		
		server.close();
		client.close();
		System.out.println("Exiting");
	}
	public static String randFor(){
		String Fortune1 = "Fortune 1";
		String Fortune2 = "Fortune 2";
		String Fortune3 = "Fortune 3";
		String Fortune4 = "Fortune 4";
		String Fortune5 = "Fortune 5";
		String Fortune6 = "Fortune 6";
		String Fortune7 = "Fortune 7";
		String Fortune8 = "Fortune 8";
		String Fortune9 = "Fortune 9";
		String Fortune10 = "Fortune 10";
		double x = Math.random();
		if (x<0.1) { return Fortune1; }
		if ((0.1 <= x) && (x < 0.2)) { return Fortune2; }
		if ((0.2 <= x) && (x < 0.3)) { return Fortune3; }
		if ((0.3 <= x) && (x < 0.4)) { return Fortune4; }
		if ((0.4 <= x) && (x < 0.5)) { return Fortune5; }
		if ((0.5 <= x) && (x < 0.6)) { return Fortune6; }
		if ((0.6 <= x) && (x < 0.7)) { return Fortune7; }
		if ((0.7 <= x) && (x < 0.8)) { return Fortune8; }
		if ((0.8 <= x) && (x < 0.9)) { return Fortune9; }
		if ((0.9 <= x) && (x < 1.0)) { return Fortune10;}
		return "out of bound";
	}
}
