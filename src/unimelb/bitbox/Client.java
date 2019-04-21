package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;


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
	        	try{
		        	this.input.close();
		        	this.output.close();
		        	this.socket.close();
		        	break;
		        } catch(Exception e1){
		        	
		        	System.out.println(e1);
		        }
	            //report exception somewhere.
	            i.printStackTrace();
			}
		}

	}
}