package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import unimelb.bitbox.ServerMain;


public class Client extends ServerMain implements Runnable {
	private Socket socket;
	private DataInputStream input;
	private DataOutputStream output;

	public Client(String host, int port)  throws NumberFormatException, IOException, NoSuchAlgorithmException {
		try {
			socket = new Socket(host, port);
			System.out.println("Client: Connected to host: " + host + " Port: " + Integer.toString(port));

			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
			
			output.writeUTF("handshake");
			System.out.println(input.readUTF());
		} catch (IOException e) {
			System.out.println("Connection to: " + host + " Port: " + Integer.toString(port) + " FAILED");
			e.printStackTrace();
		}
	}

	public void run() {
		String line = "";
		// keep reading until "Over" is input
		while (true) {
			try {
				input.readUTF();
			} catch (IOException i) {
				System.out.println(i);
			}
		}

	}
}