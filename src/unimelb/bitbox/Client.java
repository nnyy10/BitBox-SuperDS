package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.io.*;

public class Client implements Runnable {
	private Socket socket;
	private DataInputStream input;
	private DataOutputStream output;

	public Client(String host, int port) {
		try {
			socket = new Socket(host, port);
			System.out.println("Client: Connected to host");

			// takes input from terminal
			input = new DataInputStream(System.in);

			// sends output to the socket
			output = new DataOutputStream(socket.getOutputStream());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {

		// string to read message from input
		String line = "";
		// keep reading until "Over" is input
		while (true) {
			try {
				line = this.input.readLine();
				output.writeUTF(line);
			} catch (IOException i) {
				System.out.println(i);
			}
		}

	}
}