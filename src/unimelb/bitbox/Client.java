package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;


public class Client extends PeerConnection implements Runnable {

	public Client(Socket socket) {
		super(socket);
		try {

			input = new DataInputStream(this.socket.getInputStream());
			output = new DataOutputStream(this.socket.getOutputStream());
			
			output.writeUTF("handshake");
			System.out.println(input.readUTF());
		} catch (IOException e) {
			System.out.println("Connection to: FAILED");
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